package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static org.jruby.runtime.Visibility.PUBLIC;

/**
* Created by headius on 2/26/15.
*/
public abstract class Initializer {
    protected final Ruby runtime;
    protected final JavaSupport javaSupport;
    protected final Class javaClass;

    private static final Logger LOG = LoggerFactory.getLogger("Initializer");

    private static final int ACC_BRIDGE    = 0x00000040;

    public static final boolean DEBUG_SCALA = false;

    public static final String METHOD_MANGLE = "__method";

    private static final Map<String, String> SCALA_OPERATORS;

    // TODO: other reserved names?
    private static final Map<String, AssignedName> RESERVED_NAMES = new HashMap<String, AssignedName>();
    static {
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
    }
    protected static final Map<String, AssignedName> STATIC_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }
    protected static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES = new HashMap<String, AssignedName>(RESERVED_NAMES);
    static {
        // only possible for "getClass" to be an instance method in Java
        INSTANCE_RESERVED_NAMES.put("class", new AssignedName("class", Priority.RESERVED));
        // "initialize" has meaning only for an instance (as opposed to a class)
        INSTANCE_RESERVED_NAMES.put("initialize", new AssignedName("initialize", Priority.RESERVED));
    }

    public Initializer(Ruby runtime, Class javaClass) {
        this.runtime = runtime;
        this.javaSupport = runtime.getJavaSupport();
        this.javaClass = javaClass;
    }

    public static RubyModule setupProxyClass(Ruby runtime, final Class<?> javaClass, final RubyClass proxy) {
        setJavaClassFor(javaClass, proxy);

        return new ClassInitializer(runtime, javaClass).initialize(proxy);
    }

    public static RubyModule setupProxyModule(Ruby runtime, final Class<?> javaClass, final RubyModule proxy) {
        setJavaClassFor(javaClass, proxy);

        assert javaClass.isInterface();

        return new InterfaceInitializer(runtime, javaClass).initialize(proxy);
    }

    protected static void addField(
            final Map<String, NamedInstaller> callbacks,
            final Map<String, AssignedName> names,
            final Field field,
            final boolean isFinal,
            final boolean isStatic) {

        final String name = field.getName();

        if ( Priority.FIELD.lessImportantThan( names.get(name) ) ) return;

        names.put(name, new AssignedName(name, Priority.FIELD));
        callbacks.put(name, isStatic ? new StaticFieldGetterInstaller(name, field) :
                new InstanceFieldGetterInstaller(name, field));

        if (!isFinal) {
            String setName = name + '=';
            callbacks.put(setName, isStatic ? new StaticFieldSetterInstaller(setName, field) :
                    new InstanceFieldSetterInstaller(setName, field));
        }
    }

    protected static void prepareStaticMethod(Class<?> javaClass, State state, Method method, String name) {
        AssignedName assignedName = state.staticNames.get(name);

        // For JRUBY-4505, restore __method methods for reserved names
        if (STATIC_RESERVED_NAMES.containsKey(method.getName())) {
            setupStaticMethods(state.staticInstallers, javaClass, method, name + METHOD_MANGLE);
            return;
        }

        if (assignedName == null) {
            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                state.staticInstallers.remove(name);
                state.staticInstallers.remove(name + '=');
                state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        setupStaticMethods(state.staticInstallers, javaClass, method, name);
    }

    private static void setupStaticMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new StaticMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    protected static void assignStaticAliases(final State state) {
        for (Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet()) {
            // no aliases for __method methods
            if (entry.getKey().endsWith("__method")) continue;

            if (entry.getValue().type == NamedInstaller.STATIC_METHOD && entry.getValue().hasLocalMethod()) {
                assignAliases((MethodInstaller) entry.getValue(), state.staticNames);
            }
        }
    }

    protected static void assignAliases(MethodInstaller installer, Map<String, AssignedName> assignedNames) {
        String name = installer.name;
        String rubyCasedName = JavaUtil.getRubyCasedName(name);
        addUnassignedAlias(rubyCasedName, assignedNames, installer);

        String javaPropertyName = JavaUtil.getJavaPropertyName(name);
        String rubyPropertyName = null;

        for (Method method: installer.methods) {
            Class<?>[] argTypes = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            int argCount = argTypes.length;

            // Add scala aliases for apply/update to roughly equivalent Ruby names
            if (rubyCasedName.equals("apply")) {
                addUnassignedAlias("[]", assignedNames, installer);
            }
            if (rubyCasedName.equals("update") && argCount == 2) {
                addUnassignedAlias("[]=", assignedNames, installer);
            }

            // Scala aliases for $ method names
            if (name.startsWith("$")) {
                addUnassignedAlias(ClassInitializer.fixScalaNames(name), assignedNames, installer);
            }

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyCasedName.startsWith("get_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 0 ||                                // getFoo      => foo
                            argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)

                        addUnassignedAlias(javaPropertyName, assignedNames, installer);
                        addUnassignedAlias(rubyPropertyName, assignedNames, installer);
                    }
                } else if (rubyCasedName.startsWith("set_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                        addUnassignedAlias(javaPropertyName + '=', assignedNames, installer);
                        addUnassignedAlias(rubyPropertyName + '=', assignedNames, installer);
                    }
                } else if (rubyCasedName.startsWith("is_")) {
                    rubyPropertyName = rubyCasedName.substring(3);
                    if (resultType == boolean.class) {                  // isFoo() => foo, isFoo(*) => foo(*)
                        addUnassignedAlias(javaPropertyName, assignedNames, installer);
                        addUnassignedAlias(rubyPropertyName, assignedNames, installer);
                    }
                }
            }

            // Additionally add ?-postfixed aliases to any boolean methods and properties.
            if (resultType == boolean.class) {
                // is_something?, contains_thing?
                addUnassignedAlias(rubyCasedName + '?', assignedNames, installer);
                if (rubyPropertyName != null) {
                    // something?
                    addUnassignedAlias(rubyPropertyName + '?', assignedNames, installer);
                }
            }
        }
    }

    private static void addUnassignedAlias(String name, Map<String, AssignedName> assignedNames,
                                           MethodInstaller installer) {
        if (name == null) return;

        AssignedName assignedName = assignedNames.get(name);
        // TODO: missing additional logic for dealing with conflicting protected fields.
        if (Priority.ALIAS.moreImportantThan(assignedName)) {
            installer.addAlias(name);
            assignedNames.put(name, new AssignedName(name, Priority.ALIAS));
        } else if (Priority.ALIAS.asImportantAs(assignedName)) {
            installer.addAlias(name);
        }
    }

    protected static String fixScalaNames(final String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }
        return s;
    }

    static {
        HashMap<String, String> scalaOperators = new HashMap<String, String>();
        scalaOperators.put("\\$plus", "+");
        scalaOperators.put("\\$minus", "-");
        scalaOperators.put("\\$colon", ":");
        scalaOperators.put("\\$div", "/");
        scalaOperators.put("\\$eq", "=");
        scalaOperators.put("\\$less", "<");
        scalaOperators.put("\\$greater", ">");
        scalaOperators.put("\\$bslash", "\\\\");
        scalaOperators.put("\\$hash", "#");
        scalaOperators.put("\\$times", "*");
        scalaOperators.put("\\$bang", "!");
        scalaOperators.put("\\$at", "@");
        scalaOperators.put("\\$percent", "%");
        scalaOperators.put("\\$up", "^");
        scalaOperators.put("\\$amp", "&");
        scalaOperators.put("\\$tilde", "~");
        scalaOperators.put("\\$qmark", "?");
        scalaOperators.put("\\$bar", "|");
        SCALA_OPERATORS = Collections.unmodifiableMap(scalaOperators);
    }

    protected static void handleScalaSingletons(final Class<?> javaClass, final State state) {
        // check for Scala companion object
        try {
            final ClassLoader loader = javaClass.getClassLoader();
            if ( loader == null ) return; //this is a core class, bail

            Class<?> companionClass = loader.loadClass(javaClass.getName() + '$');
            final Field field = companionClass.getField("MODULE$");
            final Object singleton = field.get(null);
            if ( singleton == null ) return;

            final List<Method> scalaMethods = getMethods(companionClass);
            for ( int j = scalaMethods.size() - 1; j >= 0; j-- ) {
                final Method method = scalaMethods.get(j);
                String name = method.getName();

                if (DEBUG_SCALA) LOG.debug("Companion object method {} for {}", name, companionClass);

                if ( name.indexOf('$') >= 0 ) name = fixScalaNames(name);

                if ( ! Modifier.isStatic(method.getModifiers()) ) {
                    AssignedName assignedName = state.staticNames.get(name);
                    // For JRUBY-4505, restore __method methods for reserved names
                    if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                        if (DEBUG_SCALA) LOG.debug("in reserved " + name);
                        setupSingletonMethods(state.staticInstallers, javaClass, singleton, method, name + METHOD_MANGLE);
                        continue;
                    }
                    if (assignedName == null) {
                        state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                        if (DEBUG_SCALA) LOG.debug("Assigned name is null");
                    } else {
                        if (Priority.METHOD.lessImportantThan(assignedName)) {
                            if (DEBUG_SCALA) LOG.debug("Less important");
                            continue;
                        }
                        if (!Priority.METHOD.asImportantAs(assignedName)) {
                            state.staticInstallers.remove(name);
                            state.staticInstallers.remove(name + '=');
                            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                        }
                    }
                    if (DEBUG_SCALA) LOG.debug("Installing {} {} {}", name, method, singleton);
                    setupSingletonMethods(state.staticInstallers, javaClass, singleton, method, name);
                }
                else {
                    if (DEBUG_SCALA) LOG.debug("Method {} is sadly static", method);
                }
            }
        }
        catch (ClassNotFoundException e) { /* there's no companion object */ }
        catch (NoSuchFieldException e) { /* no MODULE$ field in companion */ }
        catch (Exception e) {
            if (DEBUG_SCALA) LOG.debug("Failed with {}", e);
        }
    }

    private static void setupSingletonMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Object singleton, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new SingletonMethodInvokerInstaller(name, singleton);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    protected static void installClassFields(final RubyModule proxy, final State state) {
        //assert state.constantFields != null;
        for (ConstantField field : state.constantFields) {
            field.install(proxy);
        }
    }

    protected static void installClassStaticMethods(final RubyModule proxy, final State state) {
        //assert state.staticInstallers != null;
        for ( Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet() ) {
            entry.getValue().install(proxy);
        }
    }

    protected static void installClassClasses(final Class<?> javaClass, final RubyModule proxy) {
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
                    proxy.getSingletonClass().addMethod(simpleName, new JavaMethod.JavaMethodZero(proxy.getSingletonClass(), PUBLIC) {
                        @Override
                        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                            return innerProxy;
                        }
                    });
                }
            }
        }
    }

    private static void setJavaClassFor(final Class<?> javaClass, final RubyModule proxy) {
        proxy.setInstanceVariable("@java_class", proxy.getRuntime().getJavaSupport().getJavaClassFromCache(javaClass));
        proxy.dataWrapStruct(javaClass);
    }

    public abstract RubyModule initialize(RubyModule proxy);

    public static class State {

        final Map<String, AssignedName> staticNames;
        final Map<String, AssignedName> instanceNames;
        final Map<String, NamedInstaller> staticInstallers = new HashMap<String, NamedInstaller>();
        final Map<String, NamedInstaller> instanceInstallers = new HashMap<String, NamedInstaller>();
        final List<ConstantField> constantFields = new ArrayList<ConstantField>();

        ConstructorInvokerInstaller constructorInstaller;

        State(final Ruby runtime, final Class superClass) {
            if (superClass == null) {
                staticNames = new HashMap<String, AssignedName>();
                instanceNames = new HashMap<String, AssignedName>();
            } else {
                staticNames = new HashMap<String, AssignedName>(runtime.getJavaSupport().getStaticAssignedNames().get(superClass));
                instanceNames = new HashMap<String, AssignedName>(runtime.getJavaSupport().getInstanceAssignedNames().get(superClass));
            }
            staticNames.putAll(STATIC_RESERVED_NAMES);
            instanceNames.putAll(INSTANCE_RESERVED_NAMES);
        }

    }

    static List<Method> getMethods(final Class<?> javaClass) {
        HashMap<String, List<Method>> nameMethods = new HashMap<String, List<Method>>(32);

        // to better size the final ArrayList below
        int totalMethods = 0;

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
                    totalMethods += addNewMethods(nameMethods, klass.getDeclaredMethods(), klass == javaClass, true);
                }
                catch (SecurityException e) { /* ignored */ }
            }

            // then do the same for each interface
            for ( Class iface : klass.getInterfaces() ) {
                try {
                    // add methods, not including static (should be none on
                    // interfaces anyway) and not replacing child methods with
                    // parent methods
                    totalMethods += addNewMethods(nameMethods, iface.getMethods(), false, false);
                }
                catch (SecurityException e) { /* ignored */ }
            }
        }

        // now only bind the ones that remain
        ArrayList<Method> finalList = new ArrayList<Method>(totalMethods);

        for ( Map.Entry<String, List<Method>> entry : nameMethods.entrySet() ) {
            finalList.addAll( entry.getValue() );
        }

        return finalList;
    }

    private static boolean methodsAreEquivalent(Method child, Method parent) {
        return parent.getDeclaringClass().isAssignableFrom(child.getDeclaringClass())
                && child.getReturnType() == parent.getReturnType()
                && child.isVarArgs() == parent.isVarArgs()
                && Modifier.isPublic(child.getModifiers()) == Modifier.isPublic(parent.getModifiers())
                && Modifier.isProtected(child.getModifiers()) == Modifier.isProtected(parent.getModifiers())
                && Modifier.isStatic(child.getModifiers()) == Modifier.isStatic(parent.getModifiers())
                && Arrays.equals(child.getParameterTypes(), parent.getParameterTypes());
    }

    private static int addNewMethods(
            final HashMap<String, List<Method>> nameMethods,
            final Method[] methods,
            final boolean includeStatic,
            final boolean removeDuplicate) {

        int added = 0;

        Methods: for (final Method method : methods) {
            final int mod = method.getModifiers();
            // Skip private methods, since they may mess with dispatch
            if ( Modifier.isPrivate(mod) ) continue;

            // ignore bridge methods because we'd rather directly call methods that this method
            // is bridging (and such methods are by definition always available.)
            if ( ( mod & ACC_BRIDGE ) != 0 ) continue;

            if ( ! includeStatic && Modifier.isStatic(mod) ) {
                // Skip static methods if we're not suppose to include them.
                // Generally for superclasses; we only bind statics from the actual
                // class.
                continue;
            }

            List<Method> childMethods = nameMethods.get(method.getName());
            if (childMethods == null) {
                // first method of this name, add a collection for it
                childMethods = new ArrayList<Method>(4);
                childMethods.add(method); added++;
                nameMethods.put(method.getName(), childMethods);
            }
            else {
                // we have seen other methods; check if we already have an equivalent one
                final ListIterator<Method> childMethodsIterator = childMethods.listIterator();
                while ( childMethodsIterator.hasNext() ) {
                    final Method current = childMethodsIterator.next();
                    if ( methodsAreEquivalent(current, method) ) {
                        if (removeDuplicate) {
                            // Replace the existing method, since the super call is more general
                            // and virtual dispatch will call the subclass impl anyway.
                            // Used for instance methods, for which we usually want to use the highest-up
                            // callable implementation.
                            childMethodsIterator.set(method);
                        } else {
                            // just skip the new method, since we don't need it (already found one)
                            // used for interface methods, which we want to add unconditionally
                            // but only if we need them
                        }
                        continue Methods;
                    }
                }
                // no equivalent; add it
                childMethods.add(method); added++;
            }
        }
        return added;
    }

}
