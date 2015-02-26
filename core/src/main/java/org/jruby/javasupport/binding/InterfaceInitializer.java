package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
public class InterfaceInitializer extends Initializer {
    public InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    public synchronized void initialize(JavaClass javaClassObject, RubyModule module) {
        if (hasRun) return;
        hasRun = true;

        InitializerState state = new InitializerState(runtime, null);

        Field[] fields = JavaClass.getDeclaredFields(javaClass);

        for (int i = fields.length; --i >= 0; ) {
            Field field = fields[i];
            if (javaClass != field.getDeclaringClass()) continue;
            if (ConstantField.isConstant(field)) state.constantFields.add(new ConstantField(field));

            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)) addField(state.staticCallbacks, state.staticNames, field, Modifier.isFinal(modifiers), true);
        }

        setupInterfaceMethods(javaClass, state);

        // Add in any Scala singleton methods
        handleScalaSingletons(javaClass, state);

        // Now add all aliases for the static methods (fields) as appropriate
        for (Map.Entry<String, NamedInstaller> entry : state.staticCallbacks.entrySet()) {
            if (entry.getValue().type == NamedInstaller.STATIC_METHOD && entry.getValue().hasLocalMethod()) {
                assignAliases((MethodInstaller)entry.getValue(), state.staticNames);
            }
        }

        staticAssignedNames = Collections.unmodifiableMap(state.staticNames);
        staticInstallers = Collections.unmodifiableMap(state.staticCallbacks);
        constantFields = Collections.unmodifiableList(state.constantFields);
        assert javaClass.isInterface();

        javaClassObject.unfinishedProxyModule = module;
        Class<?> javaClass = javaClassObject.javaClass();
        for (ConstantField field: constantFields) {
            field.install(module);
        }
        for (NamedInstaller installer : staticInstallers.values()) {
            installer.install(module);
        }

        installClassClasses(javaClass, module);

        // flag the class as a Java class proxy.
        module.setJavaProxy(true);
        module.getSingletonClass().setJavaProxy(true);

        javaClassObject.proxyModule.compareAndSet(null, module);
        javaClassObject.applyProxyExtenders();
    }

    private void setupInterfaceMethods(Class<?> javaClass, InitializerState state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        Method[] methods = JavaClass.getMethods(javaClass);

        for (int i = methods.length; --i >= 0;) {
            // Java 8 introduced static methods on interfaces, so we just look for those
            Method method = methods[i];
            String name = method.getName();

            if (!Modifier.isStatic(method.getModifiers())) continue;

            prepareStaticMethod(javaClass, state, method, name);
        }

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases(state);
    }
}
