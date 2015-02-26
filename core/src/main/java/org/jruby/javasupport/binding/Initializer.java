package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.IdUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
public abstract class Initializer {
    protected Map<String, AssignedName> staticAssignedNames;
    protected Map<String, AssignedName> instanceAssignedNames;
    protected Map<String, NamedInstaller> staticInstallers;
    protected Map<String, NamedInstaller> instanceInstallers;
    protected ConstructorInvokerInstaller constructorInstaller;
    protected List<ConstantField> constantFields;
    protected final Ruby runtime;
    protected final Class javaClass;
    protected volatile boolean hasRun = false;

    public Initializer(Ruby runtime, Class javaClass) {
        this.runtime = runtime;
        this.javaClass = javaClass;
    }

    public Initializer() {
        // only for null initializer
        this(null, null);
    }

    public abstract void initialize(JavaClass javaClassObject, RubyModule proxy);

    protected synchronized void installClassConstructors(final RubyClass proxy) {
        if (constructorInstaller != null) {
            constructorInstaller.install(proxy);
            constructorInstaller = null;
        }
    }

    protected synchronized void installClassFields(final RubyClass proxy) {
        assert constantFields != null;
        for (ConstantField field : constantFields) {
            field.install(proxy);
        }
        constantFields = null;
    }

    protected synchronized void installClassMethods(final RubyClass proxy) {
        assert staticInstallers != null;
        for (NamedInstaller installer : staticInstallers.values()) {
            installer.install(proxy);
        }
        staticInstallers = null;

        assert instanceInstallers != null;
        for (NamedInstaller installer : instanceInstallers.values()) {
            installer.install(proxy);
        }
        instanceInstallers = null;
    }

    protected void setupClassConstructors(Class<?> javaClass) {
        // TODO: protected methods.  this is going to require a rework
        // of some of the mechanism.
        Constructor[] clsConstructors = JavaClass.getConstructors(javaClass);

        // create constructorInstaller; if there are no constructors, it will disable construction
        constructorInstaller = new ConstructorInvokerInstaller("__jcreate!");

        for (int i = clsConstructors.length; --i >= 0;) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            Constructor ctor = clsConstructors[i];
            constructorInstaller.addConstructor(ctor, javaClass);
        }
    }

    protected void setupClassMethods(Class<?> javaClass, InitializerState state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        Method[] methods = JavaClass.getMethods(javaClass);

        for (int i = methods.length; --i >= 0;) {
            // we need to collect all methods, though we'll only
            // install the ones that are named in this class
            Method method = methods[i];
            String name = method.getName();

            if (Modifier.isStatic(method.getModifiers())) {
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

    protected void prepareInstanceMethod(Class<?> javaClass, InitializerState state, Method method, String name) {
        AssignedName assignedName = state.instanceNames.get(name);

        // For JRUBY-4505, restore __method methods for reserved names
        if (InitializerState.INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
            installInstanceMethods(state.instanceCallbacks, javaClass, method, name + JavaClass.METHOD_MANGLE);
            return;
        }

        if (assignedName == null) {
            state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                state.instanceCallbacks.remove(name);
                state.instanceCallbacks.remove(name + '=');
                state.instanceNames.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        installInstanceMethods(state.instanceCallbacks, javaClass, method, name);
    }

    protected void prepareStaticMethod(Class<?> javaClass, InitializerState state, Method method, String name) {
        AssignedName assignedName = state.staticNames.get(name);

        // For JRUBY-4505, restore __method methods for reserved names
        if (InitializerState.STATIC_RESERVED_NAMES.containsKey(method.getName())) {
            installStaticMethods(state.staticCallbacks, javaClass, method, name + JavaClass.METHOD_MANGLE);
            return;
        }

        if (assignedName == null) {
            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
        } else {
            if (Priority.METHOD.lessImportantThan(assignedName)) return;
            if (!Priority.METHOD.asImportantAs(assignedName)) {
                state.staticCallbacks.remove(name);
                state.staticCallbacks.remove(name + '=');
                state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
            }
        }
        installStaticMethods(state.staticCallbacks, javaClass, method, name);
    }

    protected void installInstanceMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new InstanceMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    protected void installStaticMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new StaticMethodInvokerInstaller(name);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    protected void installSingletonMethods(Map<String, NamedInstaller> methodCallbacks, Class<?> javaClass, Object singleton, Method method, String name) {
        MethodInstaller invoker = (MethodInstaller) methodCallbacks.get(name);
        if (invoker == null) {
            invoker = new SingletonMethodInvokerInstaller(name, singleton);
            methodCallbacks.put(name, invoker);
        }
        invoker.addMethod(method, javaClass);
    }

    protected void handleScalaSingletons(Class<?> javaClass, InitializerState state) {
        // check for Scala companion object
        try {
            ClassLoader cl = javaClass.getClassLoader();
            if (cl == null) {
                //this is a core class, bail
                return;
            }

            Class<?> companionClass = cl.loadClass(javaClass.getName() + "$");
            Field field = companionClass.getField("MODULE$");
            Object singleton = field.get(null);
            if (singleton != null) {
                Method[] sMethods = JavaClass.getMethods(companionClass);
                for (int j = sMethods.length - 1; j >= 0; j--) {
                    Method method = sMethods[j];
                    String name = method.getName();
                    if (JavaClass.DEBUG_SCALA) {
                        JavaClass.LOG.debug("Companion object method {} for {}", name, companionClass);
                    }
                    if (name.indexOf("$") >= 0) {
                        name = fixScalaNames(name);
                    }
                    if (!Modifier.isStatic(method.getModifiers())) {
                        AssignedName assignedName = state.staticNames.get(name);
                        // For JRUBY-4505, restore __method methods for reserved names
                        if (InitializerState.INSTANCE_RESERVED_NAMES.containsKey(method.getName())) {
                            if (JavaClass.DEBUG_SCALA) {
                                JavaClass.LOG.debug("in reserved " + name);
                            }
                            installSingletonMethods(state.staticCallbacks, javaClass, singleton, method, name + JavaClass.METHOD_MANGLE);
                            continue;
                        }
                        if (assignedName == null) {
                            state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            if (JavaClass.DEBUG_SCALA) {
                                JavaClass.LOG.debug("Assigned name is null");
                            }
                        } else {
                            if (Priority.METHOD.lessImportantThan(assignedName)) {
                                if (JavaClass.DEBUG_SCALA) {
                                    JavaClass.LOG.debug("Less important");
                                }
                                continue;
                            }
                            if (!Priority.METHOD.asImportantAs(assignedName)) {
                                state.staticCallbacks.remove(name);
                                state.staticCallbacks.remove(name + '=');
                                state.staticNames.put(name, new AssignedName(name, Priority.METHOD));
                            }
                        }
                        if (JavaClass.DEBUG_SCALA) {
                            JavaClass.LOG.debug("Installing {} {} {}", name, method, singleton);
                        }
                        installSingletonMethods(state.staticCallbacks, javaClass, singleton, method, name);
                    } else {
                        if (JavaClass.DEBUG_SCALA) {
                            JavaClass.LOG.debug("Method {} is sadly static", method);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // ignore... there's no companion object
        }
    }

    protected void assignStaticAliases(InitializerState state) {
        for (Map.Entry<String, NamedInstaller> entry : state.staticCallbacks.entrySet()) {
            // no aliases for __method methods
            if (entry.getKey().endsWith("__method")) continue;

            if (entry.getValue().type == NamedInstaller.STATIC_METHOD && entry.getValue().hasLocalMethod()) {
                assignAliases((MethodInstaller) entry.getValue(), state.staticNames);
            }
        }
    }

    protected void assignInstanceAliases(InitializerState state) {
        for (Map.Entry<String, NamedInstaller> entry : state.instanceCallbacks.entrySet()) {
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

    protected static void assignAliases(MethodInstaller installer, Map<String, AssignedName> assignedNames) {
        String name = installer.name;
        String rubyCasedName = JavaUtil.getRubyCasedName(name);
        addUnassignedAlias(rubyCasedName,assignedNames,installer);

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
                addUnassignedAlias(fixScalaNames(name), assignedNames, installer);
            }

            // Add property name aliases
            if (javaPropertyName != null) {
                if (rubyCasedName.startsWith("get_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 0 ||                                // getFoo      => foo
                            argCount == 1 && argTypes[0] == int.class) {    // getFoo(int) => foo(int)

                        addUnassignedAlias(javaPropertyName,assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName,assignedNames,installer);
                    }
                } else if (rubyCasedName.startsWith("set_")) {
                    rubyPropertyName = rubyCasedName.substring(4);
                    if (argCount == 1 && resultType == void.class) {    // setFoo(Foo) => foo=(Foo)
                        addUnassignedAlias(javaPropertyName+'=',assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName+'=',assignedNames,installer);
                    }
                } else if (rubyCasedName.startsWith("is_")) {
                    rubyPropertyName = rubyCasedName.substring(3);
                    if (resultType == boolean.class) {                  // isFoo() => foo, isFoo(*) => foo(*)
                        addUnassignedAlias(javaPropertyName,assignedNames,installer);
                        addUnassignedAlias(rubyPropertyName,assignedNames,installer);
                    }
                }
            }

            // Additionally add ?-postfixed aliases to any boolean methods and properties.
            if (resultType == boolean.class) {
                // is_something?, contains_thing?
                addUnassignedAlias(rubyCasedName+'?',assignedNames,installer);
                if (rubyPropertyName != null) {
                    // something?
                    addUnassignedAlias(rubyPropertyName+'?',assignedNames,installer);
                }
            }
        }
    }

    protected static void addUnassignedAlias(String name, Map<String, AssignedName> assignedNames,
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

    protected void installClassClasses(final Class<?> javaClass, final RubyModule proxy) {
        // setup constants for public inner classes
        Class<?>[] classes = JavaClass.getDeclaredClasses(javaClass);

        for (int i = classes.length; --i >= 0; ) {
            if (javaClass == classes[i].getDeclaringClass()) {
                Class<?> clazz = classes[i];

                // no non-public inner classes
                if (!Modifier.isPublic(clazz.getModifiers())) continue;

                String simpleName = JavaClass.getSimpleName(clazz);
                if (simpleName.length() == 0) continue;

                final IRubyObject innerProxy = Java.get_proxy_class(runtime.getJavaSupport().getJavaUtilitiesModule(), JavaClass.get(runtime, clazz));

                if (IdUtil.isConstant(simpleName)) {
                    if (proxy.getConstantAt(simpleName) == null) {
                        proxy.const_set(runtime.newString(simpleName), innerProxy);
                    }
                } else {
                    // lower-case name
                    if (!proxy.respondsTo(simpleName)) {
                        // define a class method
                        proxy.getSingletonClass().addMethod(simpleName, new JavaMethod.JavaMethodZero(proxy.getSingletonClass(), Visibility.PUBLIC) {
                            @Override
                            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                                return innerProxy;
                            }
                        });
                    }
                }
            }
        }
    }

    protected void addField(Map <String, NamedInstaller> callbacks, Map<String, AssignedName> names,
                          Field field, boolean isFinal, boolean isStatic) {
        String name = field.getName();

        if (Priority.FIELD.lessImportantThan(names.get(name))) return;

        names.put(name, new AssignedName(name, Priority.FIELD));
        callbacks.put(name, isStatic ? new StaticFieldGetterInstaller(name, field) :
                new InstanceFieldGetterInstaller(name, field));

        if (!isFinal) {
            String setName = name + '=';
            callbacks.put(setName, isStatic ? new StaticFieldSetterInstaller(setName, field) :
                    new InstanceFieldSetterInstaller(setName, field));
        }
    }

    protected void setupClassFields(Class<?> javaClass, InitializerState state) {
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
                addField(state.staticCallbacks, state.staticNames, field, Modifier.isFinal(modifiers), true);
            } else {
                addField(state.instanceCallbacks, state.instanceNames, field, Modifier.isFinal(modifiers), false);
            }
        }
    }

    protected static String fixScalaNames(String name) {
        String s = name;
        for (Map.Entry<String, String> entry : SCALA_OPERATORS.entrySet()) {
            s = s.replaceAll(entry.getKey(), entry.getValue());
        }

        return s;
    }

    protected static final Map<String, String> SCALA_OPERATORS;
    static {
        Map<String, String> tmp = new HashMap();
        tmp.put("\\$plus", "+");
        tmp.put("\\$minus", "-");
        tmp.put("\\$colon", ":");
        tmp.put("\\$div", "/");
        tmp.put("\\$eq", "=");
        tmp.put("\\$less", "<");
        tmp.put("\\$greater", ">");
        tmp.put("\\$bslash", "\\\\");
        tmp.put("\\$hash", "#");
        tmp.put("\\$times", "*");
        tmp.put("\\$bang", "!");
        tmp.put("\\$at", "@");
        tmp.put("\\$percent", "%");
        tmp.put("\\$up", "^");
        tmp.put("\\$amp", "&");
        tmp.put("\\$tilde", "~");
        tmp.put("\\$qmark", "?");
        tmp.put("\\$bar", "|");
        SCALA_OPERATORS = Collections.unmodifiableMap(tmp);
    }
}
