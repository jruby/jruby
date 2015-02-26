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

    public void initialize(JavaClass javaClassObject, RubyModule proxy) {
        final State state = new State(runtime, null);

        super.initializeBase(proxy);

        javaClassObject.unfinishedProxyModule = proxy;

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

        javaClassObject.staticAssignedNames = Collections.unmodifiableMap(state.staticNames);
        javaClassObject.instanceAssignedNames = Collections.emptyMap();

        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);

        installClassFields(proxy, state);
        installClassStaticMethods(proxy, state);
        installClassClasses(javaClass, proxy);

        javaClassObject.setProxyModule(proxy); // this.proxyModule = proxy

        javaClassObject.applyProxyExtenders();
    }

    private static void setupInterfaceMethods(Class<?> javaClass, Initializer.State state) {
        // TODO: protected methods.  this is going to require a rework of some of the mechanism.
        Method[] methods = getMethods(javaClass);

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
