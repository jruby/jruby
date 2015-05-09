package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.javasupport.JavaClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

/**
* Created by headius on 2/26/15.
*/
public class InterfaceInitializer extends Initializer {

    public InterfaceInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    @Override
    public RubyModule initialize(RubyModule proxy) {
        final State state = new State(runtime, null);

        Field[] fields = JavaClass.getDeclaredFields(javaClass);

        for (int i = fields.length; --i >= 0; ) {
            final Field field = fields[i];
            if ( javaClass != field.getDeclaringClass() ) continue;

            if ( ConstantField.isConstant(field) ) {
                state.constantFields.add(new ConstantField(field));
            }

            final int mod = field.getModifiers();
            if ( Modifier.isStatic(mod) ) {
                addField(state.staticInstallers, state.staticNames, field, Modifier.isFinal(mod), true);
            }
        }

        setupInterfaceMethods(javaClass, state);

        // Add in any Scala singleton methods
        handleScalaSingletons(javaClass, state);

        // Now add all aliases for the static methods (fields) as appropriate
        for (Map.Entry<String, NamedInstaller> entry : state.staticInstallers.entrySet()) {
            final NamedInstaller installer = entry.getValue();
            if (installer.type == NamedInstaller.STATIC_METHOD && installer.hasLocalMethod()) {
                assignAliases((MethodInstaller) installer, state.staticNames);
            }
        }

        runtime.getJavaSupport().getStaticAssignedNames().get(javaClass).putAll(state.staticNames);
        runtime.getJavaSupport().getInstanceAssignedNames().get(javaClass).clear();

        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);

        installClassFields(proxy, state);
        installClassStaticMethods(proxy, state);
        installClassClasses(javaClass, proxy);

        proxy.getName(); // trigger calculateName()

        return proxy;
    }

    private static void setupInterfaceMethods(Class<?> javaClass, Initializer.State state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        final List<Method> methods = getMethods(javaClass);

        for ( int i = methods.size(); --i >= 0; ) {
            // Java 8 introduced static methods on interfaces, so we just look for those
            Method method = methods.get(i);
            String name = method.getName();

            if ( ! Modifier.isStatic( method.getModifiers() ) ) continue;

            prepareStaticMethod(javaClass, state, method, name);
        }

        // now iterate over all installers and make sure they also have appropriate aliases
        assignStaticAliases(state);
    }

}
