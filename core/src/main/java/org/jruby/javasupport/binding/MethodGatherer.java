package org.jruby.javasupport.binding;

import com.headius.backport9.modules.Modules;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.invokers.ConstructorInvoker;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaSupport;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.jruby.runtime.Visibility.PUBLIC;

public class MethodGatherer {
    private static final boolean DEBUG_SCALA = false;
    private static final String METHOD_MANGLE = "__method";
    private static final String CONSTRUCTOR_NAME = "__jcreate!";
    private static final Method[] EMPTY_METHODS = new Method[0];
    private static final int ACC_BRIDGE = 0x00000040;

    private static final Map<String, String> SCALA_OPERATORS;

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

    private final Map<String, AssignedName> staticNames;
    private final Map<String, AssignedName> instanceNames;

    private static final Map<String, AssignedName> STATIC_RESERVED_NAMES;
    private static final Map<String, AssignedName> INSTANCE_RESERVED_NAMES;

    static {
        STATIC_RESERVED_NAMES = newReservedNamesMap(1);
        STATIC_RESERVED_NAMES.put("new", new AssignedName("new", Priority.RESERVED));
    }

    static {
        INSTANCE_RESERVED_NAMES = newReservedNamesMap(2);
        // only possible for "getClass" to be an instance method in Java
        INSTANCE_RESERVED_NAMES.put("class", new AssignedName("class", Priority.RESERVED));
        // "initialize" has meaning only for an instance (as opposed to a class)
        INSTANCE_RESERVED_NAMES.put("initialize", new AssignedName("initialize", Priority.RESERVED));
        // "equal?" should not be overridden (GH-5990)
        INSTANCE_RESERVED_NAMES.put("equal?", new AssignedName("equal?", Priority.RESERVED));
    }

    // TODO: other reserved names?
    private static Map<String, AssignedName> newReservedNamesMap(final int size) {
        HashMap<String, AssignedName> RESERVED_NAMES = new HashMap<>(size + 4, 1);
        RESERVED_NAMES.put("__id__", new AssignedName("__id__", Priority.RESERVED));
        RESERVED_NAMES.put("__send__", new AssignedName("__send__", Priority.RESERVED));
        // JRUBY-5132: java.awt.Component.instance_of?() expects 2 args
        RESERVED_NAMES.put("instance_of?", new AssignedName("instance_of?", Priority.RESERVED));
        return RESERVED_NAMES;
    }

    private Map<String, NamedInstaller> staticInstallers = Collections.EMPTY_MAP;
    private Map<String, NamedInstaller> instanceInstallers = Collections.EMPTY_MAP;
    private Map<String, ConstantField> constantFields = Collections.EMPTY_MAP;
    final Ruby runtime;

    MethodGatherer(final Ruby runtime, final Class superClass) {
        this.runtime = runtime;
        
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

    void initialize(Class<?> javaClass, RubyModule proxy) {
        setupFieldsAndConstants(javaClass);
        setupMethods(javaClass);
        setupScalaSingleton(javaClass);

        assignStaticAliases();

        JavaSupport javaSupport = runtime.getJavaSupport();

        javaSupport.getStaticAssignedNames().get(javaClass).putAll(staticNames);

        Map<String, AssignedName> instanceAssignedNames = javaSupport.getInstanceAssignedNames().get(javaClass);
        if (javaClass.isInterface()) {
            instanceAssignedNames.clear();
            installInstanceMethods(proxy);
        } else {
            assignInstanceAliases();

            instanceAssignedNames.putAll(instanceNames);

            installInstanceMethods(proxy);
            installConstructors(javaClass, proxy);
        }

        installConstants(proxy);
        installClassMethods(proxy);
        installInnerClasses(javaClass, proxy);
    }

    static Map<String, List<Method>> getMethods(final Class<?> javaClass) {
        HashMap<String, List<Method>> nameMethods = new HashMap<>(32);

        eachAccessibleMethod(
                javaClass,
                (classMethods) -> { addNewMethods(nameMethods, classMethods, true); return true; },
                (interfaceMethods) -> { addNewMethods(nameMethods, interfaceMethods, false); return true; });

        return nameMethods;
    }

    public static void eachAccessibleMethod(final Class<?> javaClass, Predicate<Method[]> classProcessor, Predicate<Method[]> interfaceProcessor) {
        boolean isPublic = Modifier.isPublic(javaClass.getModifiers());

        // we scan all superclasses, but avoid adding superclass methods with
        // same name+signature as subclass methods (see JRUBY-3130)
        for ( Class<?> klass = javaClass; klass != null; klass = klass.getSuperclass() ) {
            // only add if target class is public or source class is public, and package is exported
            if (Modules.isExported(klass, Java.class) &&
                    (isPublic || Modifier.isPublic(klass.getModifiers()))) {
                // for each class, scan declared methods for new signatures
                try {
                    // add methods, including static if this is the actual class,
                    // and replacing child methods with equivalent parent methods
                    PartitionedMethods filteredMethods = FILTERED_DECLARED_METHODS.get(klass);

                    if (!classProcessor.test(filteredMethods.instanceMethods)) return;

                    if (klass == javaClass) {
                        if (!classProcessor.test(filteredMethods.staticMethods)) return;
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
                    PartitionedMethods filteredMethods = FILTERED_METHODS.get(iface);

                    if (!interfaceProcessor.test(filteredMethods.instanceMethods)) return;
                }
                catch (SecurityException e) { /* ignored */ }
            }
        }
    }

    private static boolean methodsAreEquivalent(Method child, Method parent) {
        int childModifiers, parentModifiers;

        return parent.getDeclaringClass().isAssignableFrom(child.getDeclaringClass())
                && parent.getReturnType().isAssignableFrom(child.getReturnType())
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
                            // GH-6199: Only replace child method if parent class is public.
                            if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
                                childMethods.set(i, method);
                            }
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

    public static final ClassValue<Method[]> DECLARED_METHODS = new ClassValue<Method[]>() {
        @Override
        public Method[] computeValue(Class cls) {
            try {
                return cls.getDeclaredMethods();
            } catch (SecurityException se) {
                return EMPTY_METHODS;
            }
        }
    };

    private static final ClassValue<PartitionedMethods> FILTERED_DECLARED_METHODS = new ClassValue<PartitionedMethods>() {
        @Override
        public PartitionedMethods computeValue(Class cls) {
            return new PartitionedMethods(DECLARED_METHODS.get(cls));
        }
    };
    private static final ClassValue<Method[]> METHODS = new ClassValue<Method[]>() {
        @Override
        public Method[] computeValue(Class cls) {
            try {
                return cls.getMethods();
            } catch (SecurityException se) {
                return EMPTY_METHODS;
            }
        }
    };
    private static final ClassValue<PartitionedMethods> FILTERED_METHODS = new ClassValue<PartitionedMethods>() {
        @Override
        public PartitionedMethods computeValue(Class cls) {
            return new PartitionedMethods(METHODS.get(cls));
        }
    };
    private static final ClassValue<Class<?>[]> INTERFACES = new ClassValue<Class<?>[]>() {
        @Override
        public Class<?>[] computeValue(Class cls) {
            // Expand each interface's parent interfaces using a set
            Set<Class<?>> interfaceSet = new HashSet<>();

            addAllInterfaces(interfaceSet, cls);

            return interfaceSet.toArray(new Class<?>[interfaceSet.size()]);
        }

        void addAllInterfaces(Set<Class<?>> set, Class<?> ifc) {
            for (Class<?> i : ifc.getInterfaces()) {
                set.add(i);
                addAllInterfaces(set, i);
            }
        }
    };

    private static final ClassValue<Boolean> IS_SCALA = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            if (type.isInterface()) return false;

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

    protected void installInnerClasses(final Class<?> javaClass, final RubyModule proxy) {
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

            if (constantFields.containsKey(simpleName)) {
                /*
                 If we already have a static final field of the same name, don't define the inner class constant.
                 Typically this is used for singleton patterns where the field is an instance of the inner class, and
                 in languages like Kotlin the class itself is not what users intend to access. See GH-6196.
                 */
                runtime.getWarnings().warning("inner class \"" + javaClass.getName() + "::" + simpleName + "\" conflicts with field of same name");
                continue;
            }

            final RubyModule innerProxy = Java.getProxyClass(runtime, clazz);

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

    protected void setupScalaSingleton(final Class<?> javaClass) {
        if (javaClass.isInterface()) return;

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

                    if (DEBUG_SCALA) Initializer.LOG.debug("Companion object method {} for {}", name, companionClass);

                    if (name.indexOf('$') >= 0) name = fixScalaNames(name);

                    if (!Modifier.isStatic(method.getModifiers())) {
                        AssignedName assignedName = staticNames.get(name);
                        // For JRUBY-4505, restore __method methods for reserved names
                        if (INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                            if (DEBUG_SCALA) Initializer.LOG.debug("in reserved " + name);
                            setupSingletonMethods(staticInstallers, javaClass, singleton, method, name + METHOD_MANGLE);
                            continue;
                        }
                        if (assignedName == null) {
                            staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            if (DEBUG_SCALA) Initializer.LOG.debug("Assigned name is null");
                        } else {
                            if (Priority.METHOD.lessImportantThan(assignedName)) {
                                if (DEBUG_SCALA) Initializer.LOG.debug("Less important");
                                continue;
                            }
                            if (!Priority.METHOD.asImportantAs(assignedName)) {
                                staticInstallers.remove(name);
                                staticInstallers.remove(name + '=');
                                staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            }
                        }
                        if (DEBUG_SCALA) Initializer.LOG.debug("Installing {} {} {}", name, method, singleton);
                        setupSingletonMethods(staticInstallers, javaClass, singleton, method, name);
                    } else {
                        if (DEBUG_SCALA) Initializer.LOG.debug("Method {} is sadly static", method);
                    }
                }
            });
        }
        catch (ClassNotFoundException e) { /* there's no companion object */ }
        catch (NoSuchFieldException e) { /* no MODULE$ field in companion */ }
        catch (Exception e) {
            if (DEBUG_SCALA) Initializer.LOG.debug("Failed with {}", e);
        }
    }

    protected static String fixScalaNames(final String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }
        return s;
    }

    protected void installConstants(final RubyModule proxy) {
        constantFields.forEach((name, field) -> field.install(proxy));
    }

    protected void installClassMethods(final RubyModule proxy) {
        getStaticInstallers().forEach(($, value) -> value.install(proxy));
    }

    void installConstructors(Class<?> javaClass, final RubyModule proxy) {
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
        prepareMethod(javaClass, method, name, getStaticInstallersForWrite(), STATIC_RESERVED_NAMES, staticNames, StaticMethodInvokerInstaller::new);
    }

    protected void prepareInstanceMethod(Class<?> javaClass, Method method, String name) {
        prepareMethod(javaClass, method, name, getInstanceInstallersForWrite(), INSTANCE_RESERVED_NAMES, instanceNames, InstanceMethodInvokerInstaller::new);
    }

    protected void prepareMethod(Class<?> javaClass, Method method, String name, Map<String, NamedInstaller> installers, Map<String, AssignedName> reservedNames, Map<String, AssignedName> names, Function<String, NamedInstaller> constructor) {
        // For JRUBY-4505, restore __method methods for reserved names
        if (reservedNames.containsKey(method.getName())) {
            name = name + METHOD_MANGLE;
        } else {
            if (lowerPriority(name, installers, names)) return;
        }

        NamedInstaller invoker = installers.get(name);
        if (invoker == null) {
            invoker = constructor.apply(name);
            installers.put(name, invoker);
        }
        ((MethodInstaller) invoker).addMethod(method, javaClass);
    }

    private boolean lowerPriority(String name, Map<String, NamedInstaller> installers, Map<String, AssignedName> names) {
        AssignedName assignedName = names.get(name);
        if (assignedName == null) {
            names.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return true;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                installers.remove(name);
                installers.remove(name + '=');
                names.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        return false;
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

    void setupFieldsAndConstants(Class<?> javaClass) {
        boolean isInterface = javaClass.isInterface();
        Field[] fields = JavaClass.getDeclaredFields(javaClass);

        for (Field field : fields) {
            if (javaClass != field.getDeclaringClass()) continue;

            int modifiers = field.getModifiers();

            boolean isPublic = Modifier.isPublic(modifiers);

            if (!isPublic) continue;

            boolean isStatic = Modifier.isStatic(modifiers);
            boolean isFinal = Modifier.isFinal(modifiers);

            boolean constant = isPublic && isStatic && isFinal && Character.isUpperCase(field.getName().charAt(0));
            if (constant) {
                addConstantField(field);

                // If we are adding it as a constant,  do not add an accessor (jruby/jruby#5730)
                continue;
            }

            if (isStatic) {
                addField(getStaticInstallersForWrite(), staticNames, field, isFinal, true);
            } else {
                addField(getInstanceInstallersForWrite(), instanceNames, field, isFinal, false);
            }
        }
    }

    private void addConstantField(Field field) {
        Map<String, ConstantField> constantFields = this.constantFields;
        if (constantFields == Collections.EMPTY_MAP) {
            constantFields = this.constantFields = new HashMap<>();
        }
        constantFields.put(field.getName(), new ConstantField(field));
    }

    void setupMethods(Class<?> javaClass) {
        boolean isInterface = javaClass.isInterface();

        getMethods(javaClass).forEach((name, methods) -> {
            for (int i = methods.size(); --i >= 0; ) {
                // Java 8 introduced static methods on interfaces, so we just look for those
                Method method = methods.get(i);

                if (Modifier.isStatic(method.getModifiers())) {
                    prepareStaticMethod(javaClass, method, name);
                } else if (!isInterface || method.isDefault()) {
                    prepareInstanceMethod(javaClass, method, name);
                }
            }
        });
    }

    private void setupSingletonMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Object singleton, Method method, String name) {
        NamedInstaller invoker = methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new SingletonMethodInvokerInstaller(name, singleton);
            methodCallbacks.put(name, invoker);
        }
        ((MethodInstaller) invoker).addMethod(method, javaClass);
    }

    private void assignStaticAliases() {
        getStaticInstallers().forEach((name, installer) -> {
            if (installer.type == NamedInstaller.STATIC_METHOD && installer.hasLocalMethod()) {
                // no aliases for __method methods
                if (name.endsWith(METHOD_MANGLE)) return;

                MethodInstaller methodInstaller = (MethodInstaller) installer;

                methodInstaller.assignAliases(staticNames);
            }
        });
    }

    private void assignInstanceAliases() {
        getInstanceInstallers().forEach((name, installer) -> {
            if (installer.type == NamedInstaller.INSTANCE_METHOD) {
                // no aliases for __method methods
                if (name.endsWith(METHOD_MANGLE)) return;

                MethodInstaller methodInstaller = (MethodInstaller) installer;

                if (installer.hasLocalMethod()) {
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

    protected static void addField(
            final Map callbacks,
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

    void installInstanceMethods(final RubyModule proxy) {
        getInstanceInstallers().forEach(($, value) -> value.install(proxy));
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
            if (!Modifier.isPublic(mod) && !Java.trySetAccessible(method)) return false;

            // ignore bridge methods because we'd rather directly call methods that this method
            // is bridging (and such methods are by definition always available.)
            if ((mod & ACC_BRIDGE) != 0) return false;

            return true;
        }
    }
}
