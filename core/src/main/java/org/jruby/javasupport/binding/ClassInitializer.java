package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
public class ClassInitializer extends Initializer {
    public ClassInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyModule initialize(RubyModule proxy) {
        RubyClass proxyClass = (RubyClass)proxy;
        Class<?> superclass = javaClass.getSuperclass();

        final State state = new State(runtime, superclass);

        super.initializeBase(proxy);

        proxyClass.setReifiedClass(javaClass);

        runtime.getJavaSupport().getUnfinishedProxyClassCache().get(javaClass).set(proxyClass);

        if ( javaClass.isArray() || javaClass.isPrimitive() ) {
            return proxy;
        }

        setupClassFields(javaClass, state);
        setupClassMethods(javaClass, state);
        setupClassConstructors(javaClass, state);

        runtime.getJavaSupport().getStaticAssignedNames().get(javaClass).putAll(state.staticNames);
        runtime.getJavaSupport().getInstanceAssignedNames().get(javaClass).putAll(state.instanceNames);

        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);

        // set parent to either package module or outer class
        final RubyModule parent;
        final Class<?> enclosingClass = javaClass.getEnclosingClass();
        if ( enclosingClass != null ) {
            parent = Java.getProxyClass(runtime, enclosingClass);
        } else {
            parent = Java.getJavaPackageModule(runtime, javaClass.getPackage());
        }
        proxy.setParent(parent);

        // set the Java class name and package
        if ( javaClass.isAnonymousClass() ) {
            String baseName = ""; // javaClass.getSimpleName() returns "" for anonymous
            if ( enclosingClass != null ) {
                // instead of an empty name anonymous classes will have a "conforming"
                // although not valid (by Ruby semantics) RubyClass name e.g. :
                // 'Java::JavaUtilConcurrent::TimeUnit::1' for $1 anonymous enum class
                // NOTE: if this turns out suitable shall do the same for method etc.
                final String className = javaClass.getName();
                final int length = className.length();
                final int offset = enclosingClass.getName().length();
                if ( length > offset && className.charAt(offset) != '$' ) {
                    baseName = className.substring( offset );
                }
                else if ( length > offset + 1 ) { // skip '$'
                    baseName = className.substring( offset + 1 );
                }
            }
            proxy.setBaseName( baseName );
        }
        else {
            proxy.setBaseName( javaClass.getSimpleName() );
        }

        installClassFields(proxyClass, state);
        installClassInstanceMethods(proxyClass, state);
        installClassConstructors(proxyClass, state);
        installClassClasses(javaClass, proxyClass);

        proxy.getName(); // trigger calculateName()

        return proxy;
    }

    private static void installClassInstanceMethods(final RubyClass proxy, final Initializer.State state) {
        installClassStaticMethods(proxy, state);
        //assert state.instanceInstallers != null;
        for ( Map.Entry<String, NamedInstaller> entry : state.instanceInstallers.entrySet() ) {
            entry.getValue().install(proxy);
        }
    }

    private static void setupClassFields(Class<?> javaClass, Initializer.State state) {
        Field[] fields = JavaClass.getFields(javaClass);

        for (int i = fields.length; --i >= 0;) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;

            if (ConstantField.isConstant(field)) {
                state.constantFields.add(new ConstantField(field));
                continue;
            }

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                addField(state.staticInstallers, state.staticNames, field, Modifier.isFinal(modifiers), true);
            } else {
                addField(state.instanceInstallers, state.instanceNames, field, Modifier.isFinal(modifiers), false);
            }
        }
    }

    private void setupClassMethods(Class<?> javaClass, State state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        final List<Method> methods = getMethods(javaClass);

        for ( int i = methods.size(); --i >= 0; ) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            Method method = methods.get(i);
            String name = method.getName();

            if ( Modifier.isStatic( method.getModifiers() ) ) {
                prepareStaticMethod(javaClass, state, method, name);
            } else {
                prepareInstanceMethod(javaClass, state, method, name);
            }
        }

        // try to wire up Scala singleton logic if present
        handleScalaSingletons(javaClass, state);

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases(state);
        assignInstanceAliases(state);
    }

    private void setupClassConstructors(final Class<?> javaClass, final State state) {
        // TODO: protected methods.  this is going to require a rework
        // of some of the mechanism.
        final Constructor[] constructors = JavaClass.getConstructors(javaClass);

        // create constructorInstaller; if there are no constructors, it will disable construction
        ConstructorInvokerInstaller constructorInstaller = new ConstructorInvokerInstaller("__jcreate!");

        for ( int i = constructors.length; --i >= 0; ) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            constructorInstaller.addConstructor(constructors[i], javaClass);
        }

        state.constructorInstaller = constructorInstaller;
    }

    private void prepareInstanceMethod(Class<?> javaClass, State state, Method method, String name) {
        AssignedName assignedName = state.instanceNames.get(name);

        // For JRUBY-4505, restore __method methods for reserved names
        if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
            setupInstanceMethods(state.instanceInstallers, javaClass, method, name + METHOD_MANGLE);
            return;
        }

        if (assignedName == null) {
            state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                state.instanceInstallers.remove(name);
                state.instanceInstallers.remove(name + '=');
                state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        setupInstanceMethods(state.instanceInstallers, javaClass, method, name);
    }

    private static void setupInstanceMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new InstanceMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    private static void assignInstanceAliases(State state) {
        for (Map.Entry<String, NamedInstaller> entry : state.instanceInstallers.entrySet()) {
            if (entry.getValue().type == NamedInstaller.INSTANCE_METHOD) {
                MethodInstaller methodInstaller = (MethodInstaller)entry.getValue();

                // no aliases for __method methods
                if (entry.getKey().endsWith("__method")) continue;

                if (methodInstaller.hasLocalMethod()) {
                    assignAliases(methodInstaller, state.instanceNames);
                }

                // JRUBY-6967: Types with java.lang.Comparable were using Ruby Comparable#== instead of dispatching directly to
                // java.lang.Object.equals. We force an alias here to ensure we always use equals.
                if (entry.getKey().equals("equals")) {
                    // we only install "local" methods, but need to force this so it will bind
                    methodInstaller.setLocalMethod(true);
                    methodInstaller.addAlias("==");
                }
            }
        }
    }

    private static void installClassConstructors(final RubyModule proxy, final State state) {
        if ( state.constructorInstaller != null ) state.constructorInstaller.install(proxy);
    }

}
