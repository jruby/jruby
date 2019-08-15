package org.jruby.javasupport.binding;

import com.headius.backport9.modules.Modules;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jruby.runtime.Visibility.PUBLIC;
import static org.jruby.util.StringSupport.startsWith;

/**
* Created by headius on 2/26/15.
*/
public abstract class Initializer {

    static final Logger LOG = LoggerFactory.getLogger(Initializer.class);

    public static final boolean DEBUG_SCALA = false;

    protected final Ruby runtime;
    protected final JavaSupport javaSupport;
    protected final Class javaClass;

    private static final int ACC_BRIDGE = 0x00000040;

    public static final String METHOD_MANGLE = "__method";

    private static final Map<String, String> SCALA_OPERATORS;

    // TODO: other reserved names?
    private static Map<String, AssignedName> newReservedNamesMap(final int size) {
        HashMap<String, AssignedName> RESERVED_NAMES = new HashMap<>(size + 4, 1);
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
        return RESERVED_NAMES;
    }

    protected static final Map<String, AssignedName> STATIC_RESERVED_NAMES;
    static {
        STATIC_RESERVED_NAMES = newReservedNamesMap(1);
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }
    protected static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES;
    static {
        INSTANCE_RESERVED_NAMES = newReservedNamesMap(2);
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

    public static RubyModule setupProxyClass(Ruby runtime, final Class<?> javaClass, RubyClass proxy) {
        setJavaClassFor(javaClass, proxy);

        proxy.setReifiedClass((Class) javaClass);

        if ( javaClass.isArray() ) {
            flagAsJavaProxy(proxy); return proxy;
        }

        if ( javaClass.isPrimitive() ) {
            final RubyClass proxySingleton = proxy.getSingletonClass();
            proxySingleton.undefineMethod("new"); // remove ConcreteJavaProxy class method 'new'
            if ( javaClass == Void.TYPE ) {
                // special treatment ... while Java::int[4] is OK Java::void[2] is NOT!
                proxySingleton.undefineMethod("[]"); // from JavaProxy
                proxySingleton.undefineMethod("new_array"); // from JavaProxy
            }
            flagAsJavaProxy(proxy); return proxy;
        }

        proxy = new ClassInitializer(runtime, javaClass).initialize(proxy);
        flagAsJavaProxy(proxy); return proxy;
    }

    public static RubyModule setupProxyModule(Ruby runtime, final Class<?> javaClass, RubyModule proxy) {
        setJavaClassFor(javaClass, proxy);

        assert javaClass.isInterface();

        proxy = new InterfaceInitializer(runtime, javaClass).initialize(proxy);
        flagAsJavaProxy(proxy); return proxy;
    }

    private static void flagAsJavaProxy(final RubyModule proxy) {
        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);
    }

    protected static void addField(
            final Map<String, NamedInstaller> callbacks,
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

    protected static void assignStaticAliases(final State state) {
        state.getStaticInstallers().forEach((name, value) -> {
            // no aliases for __method methods
            if (name.endsWith(METHOD_MANGLE)) return;

            if (value.type == NamedInstaller.STATIC_METHOD && value.hasLocalMethod()) {
                assignAliases((MethodInstaller) value, state.staticNames);
            }
        });
    }

    static void assignAliases(final MethodInstaller installer,
        final Map<String, AssignedName> assignedNames) {

        final String name = installer.name;
        String rubyCasedName = JavaUtil.getRubyCasedName(name);
        addUnassignedAlias(rubyCasedName, assignedNames, installer, Priority.ALIAS);

        String javaPropertyName = JavaUtil.getJavaPropertyName(name);

        final List<Method> methods = installer.methods;

        for ( int i = 0; i < methods.size(); i++ ) {
            final Method method = methods.get(i);
            Class<?>[] argTypes = method.getParameterTypes();
            Class<?> resultType = method.getReturnType();
            int argCount = argTypes.length;

            // Add scala aliases for apply/update to roughly equivalent Ruby names
            if (name.equals("apply")) {
                addUnassignedAlias("[]", assignedNames, installer, Priority.ALIAS);
            } else if (argCount == 2 && name.equals("update")) {
                addUnassignedAlias("[]=", assignedNames, installer, Priority.ALIAS);
            } else if (startsWith(name, '$')) { // Scala aliases for $ method names
                addUnassignedAlias(ClassInitializer.fixScalaNames(name), assignedNames, installer, Priority.ALIAS);
            }

            String rubyPropertyName = null;

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyCasedName.startsWith("get_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 0) {  // getFoo      => foo
                        addUnassignedAlias(javaPropertyName, assignedNames, installer, Priority.GET_ALIAS);
                        addUnassignedAlias(rubyPropertyName, assignedNames, installer, Priority.GET_ALIAS);
                    }
                } else if (rubyCasedName.startsWith("set_")) {
                    rubyPropertyName = rubyCasedName.substring(4); // TODO do not add foo? for setFoo (returning boolean)
                    if (argCount == 1 && resultType == void.class) {  // setFoo(Foo) => foo=(Foo)
                        addUnassignedAlias(javaPropertyName + '=', assignedNames, installer, Priority.ALIAS);
                        addUnassignedAlias(rubyPropertyName + '=', assignedNames, installer, Priority.ALIAS);
                    }
                } else if (rubyCasedName.startsWith("is_")) {
                    rubyPropertyName = rubyCasedName.substring(3);
                    // TODO (9.2) should be another check here to make sure these are only for getters
                    // ... e.g. isFoo() and not arbitrary isFoo(param) see GH-4432
                    if (resultType == boolean.class) {  // isFoo() => foo, isFoo(*) => foo(*)
                        addUnassignedAlias(javaPropertyName, assignedNames, installer, Priority.IS_ALIAS);
                        addUnassignedAlias(rubyPropertyName, assignedNames, installer, Priority.IS_ALIAS);
                        // foo? is added bellow
                    }
                }
            }

            // Additionally add ?-postfixed aliases to any boolean methods and properties.
            if (resultType == boolean.class) {
                // isFoo -> isFoo?, contains -> contains?
                addUnassignedAlias(rubyCasedName + '?', assignedNames, installer, Priority.ALIAS);
                if (rubyPropertyName != null) { // isFoo -> foo?
                    addUnassignedAlias(rubyPropertyName + '?', assignedNames, installer, Priority.ALIAS);
                }
            }
        }
    }

    private static boolean addUnassignedAlias(final String name,
        final Map<String, AssignedName> assignedNames, final MethodInstaller installer,
        final Priority aliasType) {

        AssignedName assignedName = assignedNames.get(name);

        if (aliasType.moreImportantThan(assignedName)) {
            installer.addAlias(name);
            assignedNames.put(name, new AssignedName(name, aliasType));
            return true;
        }
        if (aliasType.asImportantAs(assignedName)) {
            installer.addAlias(name);
            return true;
        }

        // TODO: missing additional logic for dealing with conflicting protected fields.

        return false;
    }

    protected static String fixScalaNames(final String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }
        return s;
    }

    static {
        HashMap<String, String> scalaOperators = new HashMap<>(24, 1);
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

    private static final ClassValue<Boolean> IS_SCALA = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            boolean scalaAnno = false;
            for (Annotation anno : type.getDeclaredAnnotations()) {
                Package pkg = anno.annotationType().getPackage();
                if (pkg != null && pkg.getName() != null && pkg.getName().startsWith("scala.")) {
                    scalaAnno = true;
                    break;
                }
            }
            return scalaAnno;
        }
    };

    protected static void handleScalaSingletons(final Class<?> javaClass, final State state) {
        // check for Scala companion object
        try {
            final ClassLoader loader = javaClass.getClassLoader();
            if ( loader == null ) return; //this is a core class, bail

            if (!IS_SCALA.get(javaClass)) return;

            Class<?> companionClass = loader.loadClass(javaClass.getName() + '$');
            final Field field = companionClass.getField("MODULE$");
            final Object singleton = field.get(null);
            if ( singleton == null ) return;

            getMethods(companionClass).forEach((name, methods) -> {
                for (int j = 0; j < methods.size(); j++) {
                    final Method method = methods.get(j);

                    if (DEBUG_SCALA) LOG.debug("Companion object method {} for {}", name, companionClass);

                    if (name.indexOf('$') >= 0) name = fixScalaNames(name);

                    if (!Modifier.isStatic(method.getModifiers())) {
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
                    } else {
                        if (DEBUG_SCALA) LOG.debug("Method {} is sadly static", method);
                    }
                }
            });
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
        state.constantFields.forEach(field -> field.install(proxy));
    }

    protected static void installClassStaticMethods(final RubyModule proxy, final State state) {
        state.getStaticInstallers().forEach(($, value) -> value.install(proxy));
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

    private static void setJavaClassFor(final Class<?> javaClass, final RubyModule proxy) {
        proxy.setInstanceVariable("@java_class", proxy.getRuntime().getJavaSupport().getJavaClassFromCache(javaClass));
        proxy.dataWrapStruct(javaClass);
    }

    public abstract RubyModule initialize(RubyModule proxy);

    public static class State {

        final Map<String, AssignedName> staticNames;
        final Map<String, AssignedName> instanceNames;
        private Map<String, NamedInstaller> staticInstallers = Collections.EMPTY_MAP;
        private Map<String, NamedInstaller> instanceInstallers = Collections.EMPTY_MAP;
        final List<ConstantField> constantFields = new ArrayList<>();

        ConstructorInvokerInstaller constructorInstaller;

        State(final Ruby runtime, final Class superClass) {
            if (superClass == null) {
                staticNames = new HashMap<>(STATIC_RESERVED_NAMES);
                instanceNames = new HashMap<>(INSTANCE_RESERVED_NAMES);
            } else {
                JavaSupport javaSupport = runtime.getJavaSupport();

                Map<String, AssignedName> staticAssignedNames = javaSupport.getStaticAssignedNames().get(superClass);
                staticNames = new HashMap<>(staticAssignedNames.size() + STATIC_RESERVED_NAMES.size());

                Map<String, AssignedName> instanceAssignedNames = javaSupport.getInstanceAssignedNames().get(superClass);
                instanceNames = new HashMap<>(instanceAssignedNames.size() + INSTANCE_RESERVED_NAMES.size());

                staticNames.putAll(STATIC_RESERVED_NAMES);
                staticNames.putAll(staticAssignedNames);
                instanceNames.putAll(INSTANCE_RESERVED_NAMES);
                instanceNames.putAll(instanceAssignedNames);
            }
        }

        protected void prepareStaticMethod(Class<?> javaClass, Method method, String name) {
            // lazy instantiation
            Map<String, NamedInstaller> staticInstallers = getStaticInstallersForWrite();

            // For JRUBY-4505, restore __method methods for reserved names
            if (STATIC_RESERVED_NAMES.containsKey(method.getName())) {
                name = name + METHOD_MANGLE;
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

            MethodInstaller invoker = (MethodInstaller) staticInstallers.get(name);
            if (invoker == null) {
                invoker = new StaticMethodInvokerInstaller(name);
                staticInstallers.put(name, invoker);
            }
            invoker.addMethod(method, javaClass);
        }

        protected void prepareInstanceMethod(Class<?> javaClass, Method method, String name) {
            // lazy instantiation
            Map<String, NamedInstaller> instanceInstallers = getInstanceInstallersForWrite();

            // For JRUBY-4505, restore __method methods for reserved names
            if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                name = name + METHOD_MANGLE;
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

            MethodInstaller invoker = (MethodInstaller) instanceInstallers.get(name);
            if (invoker == null) {
                invoker = new InstanceMethodInvokerInstaller(name);
                instanceInstallers.put(name, invoker);
            }
            invoker.addMethod(method, javaClass);
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

    }

    public static final ClassValue<Method[]> DECLARED_METHODS = new ClassValue<Method[]>() {
        @Override
        public Method[] computeValue(Class cls) {
            return cls.getDeclaredMethods();
        }
    };

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
    }

    public static final ClassValue<PartitionedMethods> FILTERED_DECLARED_METHODS = new ClassValue<PartitionedMethods>() {
        @Override
        public PartitionedMethods computeValue(Class cls) {
            return new PartitionedMethods(DECLARED_METHODS.get(cls));
        }
    };

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

    public static final java.lang.ClassValue<Class<?>[]> INTERFACES = new java.lang.ClassValue<Class<?>[]>() {
        @Override
        public Class<?>[] computeValue(Class cls) {
            return cls.getInterfaces();
        }
    };

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

}
