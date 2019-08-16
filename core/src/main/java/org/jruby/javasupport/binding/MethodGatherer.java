package org.jruby.javasupport.binding;

import com.headius.backport9.modules.Modules;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.invokers.ConstructorInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.jruby.runtime.Visibility.PUBLIC;

public class MethodGatherer {
    private static final int ACC_BRIDGE = 0x00000040;

    final Map<String, AssignedName> staticNames;
    final Map<String, AssignedName> instanceNames;
    private Map<String, NamedInstaller> staticInstallers = Collections.EMPTY_MAP;
    private Map<String, NamedInstaller> instanceInstallers = Collections.EMPTY_MAP;
    final List<ConstantField> constantFields = new ArrayList<>();
    final Ruby runtime;
    public static final String CONSTRUCTOR_NAME = "__jcreate!";

    MethodGatherer(final Ruby runtime, final Class superClass) {
        this.runtime = runtime;
        
        if (superClass == null) {
            staticNames = new HashMap<>(Initializer.STATIC_RESERVED_NAMES);
            instanceNames = new HashMap<>(Initializer.INSTANCE_RESERVED_NAMES);
        } else {
            JavaSupport javaSupport = runtime.getJavaSupport();

            Map<String, AssignedName> staticAssignedNames = javaSupport.getStaticAssignedNames().get(superClass);
            staticNames = new HashMap<>(staticAssignedNames.size() + Initializer.STATIC_RESERVED_NAMES.size());

            Map<String, AssignedName> instanceAssignedNames = javaSupport.getInstanceAssignedNames().get(superClass);
            instanceNames = new HashMap<>(instanceAssignedNames.size() + Initializer.INSTANCE_RESERVED_NAMES.size());

            staticNames.putAll(Initializer.STATIC_RESERVED_NAMES);
            staticNames.putAll(staticAssignedNames);
            instanceNames.putAll(Initializer.INSTANCE_RESERVED_NAMES);
            instanceNames.putAll(instanceAssignedNames);
        }
    }

    void initializeClass(Class<?> javaClass, RubyModule proxy) {
        setupClassFields(javaClass);
        setupClassMethods(javaClass);

        runtime.getJavaSupport().getStaticAssignedNames().get(javaClass).putAll(staticNames);
        runtime.getJavaSupport().getInstanceAssignedNames().get(javaClass).putAll(instanceNames);

        installClassFields(proxy);
        installClassStaticMethods(proxy);
        installClassInstanceMethods(proxy);
        installClassConstructors(javaClass, proxy);
        installClassClasses(javaClass, proxy);
    }

    void initializeInterface(Class javaClass, RubyModule proxy) {
        setupInterfaceFields(javaClass);
        setupInterfaceMethods(javaClass);

        // Add in any Scala singleton methods
        handleScalaSingletons(javaClass);

        // Now add all aliases for the static methods (fields) as appropriate
        getStaticInstallers().forEach(($, installer) -> {
            if (installer.type == NamedInstaller.STATIC_METHOD && installer.hasLocalMethod()) {
                ((MethodInstaller) installer).assignAliases(staticNames);
            }
        });

        runtime.getJavaSupport().getStaticAssignedNames().get(javaClass).putAll(staticNames);
        runtime.getJavaSupport().getInstanceAssignedNames().get(javaClass).clear();

        installClassFields(proxy);
        installClassStaticMethods(proxy);
        installClassClasses(javaClass, proxy);
    }

    static Map<String, List<Method>> getMethods(final Class<?> javaClass) {
        HashMap<String, List<Method>> nameMethods = new HashMap<>(32);

        // we scan all superclasses, but avoid adding superclass methods with
        // same name+signature as subclass methods (see JRUBY-3130)
        for ( Class<?> klass = javaClass; klass != null; klass = klass.getSuperclass() ) {
            // only add class's methods if it's public or we can set accessible
            // (see JRUBY-4799)
            if (Modifier.isPublic(klass.getModifiers()) || JavaUtil.CAN_SET_ACCESSIBLE) {
                // for each class, scan declared methods for new signatures
                try {
                    // add methods, including static if this is the actual class,
                    // and replacing child methods with equivalent parent methods
                    PartitionedMethods filteredMethods = FILTERED_DECLARED_METHODS.get(klass);

                    addNewMethods(nameMethods, filteredMethods.instanceMethods, true);

                    if (klass == javaClass) {
                        addNewMethods(nameMethods, filteredMethods.staticMethods, true);
                    }
                }
                catch (SecurityException e) { /* ignored */ }
            }

            // then do the same for each interface
            for ( Class iface : INTERFACES.get(klass) ) {
                try {
                    // add methods, not including static (should be none on
                    // interfaces anyway) and not replacing child methods with
                    // parent methods
                    PartitionedMethods filteredMethods = FILTERED_METHODS.get(klass);

                    addNewMethods(nameMethods, filteredMethods.instanceMethods, false);
                }
                catch (SecurityException e) { /* ignored */ }
            }
        }

        return nameMethods;
    }

    private static boolean methodsAreEquivalent(Method child, Method parent) {
        int childModifiers, parentModifiers;

        return parent.getDeclaringClass().isAssignableFrom(child.getDeclaringClass())
                && child.getReturnType() == parent.getReturnType()
                && child.isVarArgs() == parent.isVarArgs()
                && Modifier.isPublic(childModifiers = child.getModifiers()) == Modifier.isPublic(parentModifiers = parent.getModifiers())
                && Modifier.isProtected(childModifiers) == Modifier.isProtected(parentModifiers)
                && Modifier.isStatic(childModifiers) == Modifier.isStatic(parentModifiers)
                && Arrays.equals(child.getParameterTypes(), parent.getParameterTypes());
    }

    private static void addNewMethods(
            final HashMap<String, List<Method>> nameMethods,
            final Method[] methods,
            final boolean removeDuplicate) {

        Methods: for (Method method : methods) {
            List<Method> childMethods = nameMethods.get(method.getName());
            if (childMethods == null) {
                // first method of this name, add a collection for it
                childMethods = new ArrayList<>(4);
                childMethods.add(method);
                nameMethods.put(method.getName(), childMethods);
            }
            else {
                // we have seen other methods; check if we already have an equivalent one
                for (int i = 0; i < childMethods.size(); i++) {
                    final Method current = childMethods.get(i);
                    if ( methodsAreEquivalent(current, method) ) {
                        if (removeDuplicate) {
                            // Replace the existing method, since the super call is more general
                            // and virtual dispatch will call the subclass impl anyway.
                            // Used for instance methods, for which we usually want to use the highest-up
                            // callable implementation.
                            childMethods.set(i, method);
                        } else {
                            // just skip the new method, since we don't need it (already found one)
                            // used for interface methods, which we want to add unconditionally
                            // but only if we need them
                        }
                        continue Methods;
                    }
                }
                // no equivalent; add it
                childMethods.add(method);
            }
        }
    }

    public static final ClassValue<PartitionedMethods> FILTERED_DECLARED_METHODS = new ClassValue<PartitionedMethods>() {
        @Override
        public PartitionedMethods computeValue(Class cls) {
            return new PartitionedMethods(Initializer.DECLARED_METHODS.get(cls));
        }
    };
    public static final ClassValue<Method[]> METHODS = new ClassValue<Method[]>() {
        @Override
        public Method[] computeValue(Class cls) {
            return cls.getMethods();
        }
    };
    public static final ClassValue<PartitionedMethods> FILTERED_METHODS = new ClassValue<PartitionedMethods>() {
        @Override
        public PartitionedMethods computeValue(Class cls) {
            return new PartitionedMethods(METHODS.get(cls));
        }
    };
    public static final ClassValue<Class<?>[]> INTERFACES = new ClassValue<Class<?>[]>() {
        @Override
        public Class<?>[] computeValue(Class cls) {
            return cls.getInterfaces();
        }
    };

    protected void installClassClasses(final Class<?> javaClass, final RubyModule proxy) {
        // setup constants for public inner classes
        Class<?>[] classes = JavaClass.getDeclaredClasses(javaClass);

        final Ruby runtime = proxy.getRuntime();

        for ( int i = classes.length; --i >= 0; ) {
            final Class<?> clazz = classes[i];
            if ( javaClass != clazz.getDeclaringClass() ) continue;

            // no non-public inner classes
            if ( ! Modifier.isPublic(clazz.getModifiers()) ) continue;

            final String simpleName = JavaClass.getSimpleName(clazz);
            if ( simpleName.length() == 0 ) continue;

            final RubyModule innerProxy = Java.getProxyClass(runtime, JavaClass.get(runtime, clazz));

            if ( IdUtil.isConstant(simpleName) ) {
                if (proxy.getConstantAt(simpleName) == null) {
                    proxy.const_set(runtime.newString(simpleName), innerProxy);
                }
            }
            else { // lower-case name
                if ( ! proxy.respondsTo(simpleName) ) {
                    // define a class method
                    proxy.getSingletonClass().addMethod(simpleName, new JavaMethod.JavaMethodZero(proxy.getSingletonClass(), PUBLIC, simpleName) {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                            return innerProxy;
                        }
                    });
                }
            }
        }
    }

    void setupInterfaceMethods(Class<?> javaClass) {
        getMethods(javaClass).forEach((name, methods) -> {
            for (int i = methods.size(); --i >= 0; ) {
                // Java 8 introduced static methods on interfaces, so we just look for those
                Method method = methods.get(i);

                if (!Modifier.isStatic(method.getModifiers())) continue;

                prepareStaticMethod(javaClass, method, name);
            }
        });

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases();
    }

    protected void handleScalaSingletons(final Class<?> javaClass) {
        // check for Scala companion object
        try {
            final ClassLoader loader = javaClass.getClassLoader();
            if ( loader == null ) return; //this is a core class, bail

            if (!Initializer.IS_SCALA.get(javaClass)) return;

            Class<?> companionClass = loader.loadClass(javaClass.getName() + '$');
            final Field field = companionClass.getField("MODULE$");
            final Object singleton = field.get(null);
            if ( singleton == null ) return;

            getMethods(companionClass).forEach((name, methods) -> {
                for (int j = 0; j < methods.size(); j++) {
                    final Method method = methods.get(j);

                    if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Companion object method {} for {}", name, companionClass);

                    if (name.indexOf('$') >= 0) name = Initializer.fixScalaNames(name);

                    if (!Modifier.isStatic(method.getModifiers())) {
                        AssignedName assignedName = staticNames.get(name);
                        // For JRUBY-4505, restore __method methods for reserved names
                        if (Initializer.INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                            if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("in reserved " + name);
                            setupSingletonMethods(staticInstallers, javaClass, singleton, method, name + Initializer.METHOD_MANGLE);
                            continue;
                        }
                        if (assignedName == null) {
                            staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Assigned name is null");
                        } else {
                            if (Priority.METHOD.lessImportantThan(assignedName)) {
                                if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Less important");
                                continue;
                            }
                            if (!Priority.METHOD.asImportantAs(assignedName)) {
                                staticInstallers.remove(name);
                                staticInstallers.remove(name + '=');
                                staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            }
                        }
                        if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Installing {} {} {}", name, method, singleton);
                        setupSingletonMethods(staticInstallers, javaClass, singleton, method, name);
                    } else {
                        if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Method {} is sadly static", method);
                    }
                }
            });
        }
        catch (ClassNotFoundException e) { /* there's no companion object */ }
        catch (NoSuchFieldException e) { /* no MODULE$ field in companion */ }
        catch (Exception e) {
            if (Initializer.DEBUG_SCALA) Initializer.LOG.debug("Failed with {}", e);
        }
    }

    protected void installClassFields(final RubyModule proxy) {
        constantFields.forEach(field -> field.install(proxy));
    }

    protected void installClassStaticMethods(final RubyModule proxy) {
        getStaticInstallers().forEach(($, value) -> value.install(proxy));
    }

    private void assignInstanceAliases() {
        getInstanceInstallers().forEach((name, value) -> {
            if (value.type == NamedInstaller.INSTANCE_METHOD) {
                MethodInstaller methodInstaller = (MethodInstaller) value;

                // no aliases for __method methods
                if (name.endsWith(Initializer.METHOD_MANGLE)) return;

                if (methodInstaller.hasLocalMethod()) {
                    methodInstaller.assignAliases(instanceNames);
                }

                // JRUBY-6967: Types with java.lang.Comparable were using Ruby Comparable#== instead of dispatching directly to
                // java.lang.Object.equals. We force an alias here to ensure we always use equals.
                if (name.equals("equals")) {
                    // we only install "local" methods, but need to force this so it will bind
                    methodInstaller.setLocalMethod(true);
                    methodInstaller.addAlias("==");
                }
            }
        });
    }

    void installClassConstructors(Class<?> javaClass, final RubyModule proxy) {
        Constructor[] constructors = JavaClass.getConstructors(javaClass);

        boolean localConstructor = false;
        for (Constructor constructor : constructors) {
            localConstructor |= javaClass == constructor.getDeclaringClass();
        }

        if (localConstructor) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            proxy.addMethod(CONSTRUCTOR_NAME, new ConstructorInvoker(proxy, javaClass::getConstructors, CONSTRUCTOR_NAME));
        } else {
            // if there's no constructor, we must prevent construction
            proxy.addMethod(CONSTRUCTOR_NAME, new NoConstructorMethod(proxy, CONSTRUCTOR_NAME));
        }
    }

    static class NoConstructorMethod extends JavaMethod {
        NoConstructorMethod(RubyModule proxy, String name) {
            super(proxy, PUBLIC, name);
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            throw context.runtime.newTypeError("no public constructors for " + clazz);
        }
    }

    protected void prepareStaticMethod(Class<?> javaClass, Method method, String name) {
        // lazy instantiation
        Map<String, NamedInstaller> staticInstallers = getStaticInstallersForWrite();

        // For JRUBY-4505, restore __method methods for reserved names
        if (Initializer.STATIC_RESERVED_NAMES.containsKey(method.getName())) {
            name = name + Initializer.METHOD_MANGLE;
        } else {
            AssignedName assignedName = staticNames.get(name);
            if (assignedName == null) {
                staticNames.put(name, new AssignedName(name, Priority.METHOD));
            } else {
                if (Priority.METHOD.lessImportantThan(assignedName)) return;
                if (!Priority.METHOD.asImportantAs(assignedName)) {
                    staticInstallers.remove(name);
                    staticInstallers.remove(name + '=');
                    staticNames.put(name, new AssignedName(name, Priority.METHOD));
                }
            }
        }

        NamedInstaller invoker = staticInstallers.get(name);
        if (invoker == null) {
            invoker = new StaticMethodInvokerInstaller(name);
            staticInstallers.put(name, invoker);
        }
        ((MethodInstaller) invoker).addMethod(method, javaClass);
    }

    protected void prepareInstanceMethod(Class<?> javaClass, Method method, String name) {
        // lazy instantiation
        Map<String, NamedInstaller> instanceInstallers = getInstanceInstallersForWrite();

        // For JRUBY-4505, restore __method methods for reserved names
        if (Initializer.INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
            name = name + Initializer.METHOD_MANGLE;
        } else {
            AssignedName assignedName = instanceNames.get(name);

            if (assignedName == null) {
                instanceNames.put(name, new AssignedName(name, Priority.METHOD));
            } else {
                if (Priority.METHOD.lessImportantThan(assignedName)) return;
                if (!Priority.METHOD.asImportantAs(assignedName)) {
                    instanceInstallers.remove(name);
                    instanceInstallers.remove(name + '=');
                    instanceNames.put(name, new AssignedName(name, Priority.METHOD));
                }
            }
        }

        NamedInstaller invoker = instanceInstallers.get(name);
        if (invoker == null) {
            invoker = new InstanceMethodInvokerInstaller(name);
            instanceInstallers.put(name, invoker);
        }
        ((MethodInstaller) invoker).addMethod(method, javaClass);
    }

    Map<String, NamedInstaller> getStaticInstallers() {
        return staticInstallers;
    }

    Map<String, NamedInstaller> getStaticInstallersForWrite() {
        Map<String, NamedInstaller> staticInstallers = this.staticInstallers;
        return staticInstallers == Collections.EMPTY_MAP ? this.staticInstallers = new HashMap() : staticInstallers;
    }

    Map<String, NamedInstaller> getInstanceInstallers() {
        return instanceInstallers;
    }

    Map<String, NamedInstaller> getInstanceInstallersForWrite() {
        Map<String, NamedInstaller> instanceInstallers = this.instanceInstallers;
        return instanceInstallers == Collections.EMPTY_MAP ? this.instanceInstallers = new HashMap() : instanceInstallers;
    }

    void setupClassFields(Class<?> javaClass) {
        Field[] fields = JavaClass.getFields(javaClass);

        for (int i = fields.length; --i >= 0;) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;

            if (ConstantField.isConstant(field)) {
                constantFields.add(new ConstantField(field));
                continue;
            }

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) {
                addField(getStaticInstallersForWrite(), staticNames, field, Modifier.isFinal(modifiers), true, false);
            } else {
                addField(getInstanceInstallersForWrite(), instanceNames, field, Modifier.isFinal(modifiers), false, false);
            }
        }
    }

    void setupClassMethods(Class<?> javaClass) {
        getMethods(javaClass).forEach((name, methods) -> {
            for (int i = methods.size(); --i >= 0; ) {
                // we need to collect all methods, though we'll only
                // install the ones that are named in this class
                Method method = methods.get(i);

                if (Modifier.isStatic(method.getModifiers())) {
                    prepareStaticMethod(javaClass, method, name);
                } else {
                    prepareInstanceMethod(javaClass, method, name);
                }
            }
        });

        // try to wire up Scala singleton logic if present
        handleScalaSingletons(javaClass);

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases();
        assignInstanceAliases();
    }

    private void setupSingletonMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Object singleton, Method method, String name) {
        NamedInstaller invoker = methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new SingletonMethodInvokerInstaller(name, singleton);
            methodCallbacks.put(name, invoker);
        }
        ((MethodInstaller) invoker).addMethod(method, javaClass);
    }

    protected void assignStaticAliases() {
        getStaticInstallers().forEach((name, value) -> {
            // no aliases for __method methods
            if (name.endsWith(Initializer.METHOD_MANGLE)) return;

            if (value.type == NamedInstaller.STATIC_METHOD && value.hasLocalMethod()) {
                ((MethodInstaller) value).assignAliases(staticNames);
            }
        });
    }

    protected static void addField(
            final Map callbacks,
            final Map<String, AssignedName> names,
            final Field field,
            final boolean isFinal,
            final boolean isStatic,
            final boolean isConstant) {

        final String name = field.getName();

        if ( Priority.FIELD.lessImportantThan( names.get(name) ) ) return;

        names.put(name, new AssignedName(name, Priority.FIELD));
        callbacks.put(name, isStatic ? new StaticFieldGetterInstaller(name, field, isConstant) :
                new InstanceFieldGetterInstaller(name, field));

        if (!isFinal) {
            String setName = name + '=';
            callbacks.put(setName, isStatic ? new StaticFieldSetterInstaller(setName, field) :
                    new InstanceFieldSetterInstaller(setName, field));
        }
    }

    void installClassInstanceMethods(final RubyModule proxy) {
        getInstanceInstallers().forEach(($, value) -> value.install(proxy));
    }

    private void setupInterfaceFields(Class javaClass) {
        Field[] fields = JavaClass.getDeclaredFields(javaClass);

        for (int i = fields.length; --i >= 0; ) {
            final Field field = fields[i];
            if ( javaClass != field.getDeclaringClass() ) continue;

            boolean isConstant = ConstantField.isConstant(field);
            if (isConstant) {
                constantFields.add(new ConstantField(field));
            }

            final int mod = field.getModifiers();
            if ( Modifier.isStatic(mod) ) {
                // If we already are adding it as a constant, make the accessors warn about deprecated behavior.
                // See jruby/jruby#5730.
                addField(getStaticInstallersForWrite(), staticNames, field, Modifier.isFinal(mod), true, isConstant);
            }
        }
    }

    private static class PartitionedMethods {
        final Method[] instanceMethods;
        final Method[] staticMethods;

        PartitionedMethods(Method[] methods) {
            List<Method> instanceMethods = Collections.EMPTY_LIST;
            List<Method> staticMethods = Collections.EMPTY_LIST;

            for (Method m : methods) {
                int modifiers = m.getModifiers();
                if (filterAccessible(m, modifiers)) {
                    if (Modifier.isStatic(modifiers)) {
                        if (staticMethods == Collections.EMPTY_LIST) staticMethods = new ArrayList<>();
                        staticMethods.add(m);
                    } else {
                        if (instanceMethods == Collections.EMPTY_LIST) instanceMethods = new ArrayList<>();
                        instanceMethods.add(m);
                    }
                }
            }

            this.instanceMethods = instanceMethods.toArray(new Method[instanceMethods.size()]);
            this.staticMethods = staticMethods.toArray(new Method[staticMethods.size()]);
        }

        private static boolean filterAccessible(Method method, int mod) {
            // Skip private methods, since they may mess with dispatch
            if (Modifier.isPrivate(mod)) return false;

            // Skip protected methods if we can't set accessible
            if (!Modifier.isPublic(mod) && !Modules.trySetAccessible(method, Java.class)) return false;

            // ignore bridge methods because we'd rather directly call methods that this method
            // is bridging (and such methods are by definition always available.)
            if ((mod & ACC_BRIDGE) != 0) return false;

            return true;
        }
    }
}
