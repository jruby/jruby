package org.jruby.javasupport.binding;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Collections;

import static org.jruby.runtime.Visibility.PUBLIC;

/**
* Created by headius on 2/26/15.
*/
public class ClassInitializer extends Initializer {
    public ClassInitializer(Ruby runtime, Class<?> javaClass) {
        super(runtime, javaClass);
    }

    public synchronized void initialize(JavaClass javaClassObject, RubyModule proxy) {
        if (hasRun) return;
        hasRun = true;

        InitializerState state = new InitializerState(runtime, javaClass.getSuperclass());

        setupClassFields(javaClass, state);
        setupClassMethods(javaClass, state);
        setupClassConstructors(javaClass);

        staticAssignedNames = Collections.unmodifiableMap(state.staticNames);
        instanceAssignedNames = Collections.unmodifiableMap(state.instanceNames);
        staticInstallers = Collections.unmodifiableMap(state.staticCallbacks);
        instanceInstallers = Collections.unmodifiableMap(state.instanceCallbacks);
        constantFields = Collections.unmodifiableList(state.constantFields);

        RubyClass proxyClass = (RubyClass)proxy;
        proxy.addMethod("__jsend!", new org.jruby.internal.runtime.methods.JavaMethod.JavaMethodNBlock(proxy, PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
                String callName = args[0].asJavaString();

                DynamicMethod method = self.getMetaClass().searchMethod(callName);
                int v = method.getArity().getValue();

                IRubyObject[] newArgs = new IRubyObject[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);

                if(v < 0 || v == (newArgs.length)) {
                    return Helpers.invoke(context, self, callName, newArgs, Block.NULL_BLOCK);
                } else {
                    RubyClass superClass = self.getMetaClass().getSuperClass();
                    return Helpers.invokeAs(context, superClass, self, callName, newArgs, Block.NULL_BLOCK);
                }
            }
        });

        final Class<?> javaClass = javaClassObject.javaClass();

        proxyClass.setReifiedClass(javaClass);

        assert javaClassObject.proxyClass == null;
        javaClassObject.unfinishedProxyClass = proxyClass;
        if (javaClass.isArray() || javaClass.isPrimitive()) {
            // see note below re: 2-field kludge
            javaClassObject.proxyClass = proxyClass;
            javaClassObject.proxyModule = proxy;
            return;
        }

        installClassFields(proxyClass);
        installClassMethods(proxyClass);
        installClassConstructors(proxyClass);
        installClassClasses(javaClass, proxy);

        // flag the class as a Java class proxy.
        proxy.setJavaProxy(true);
        proxy.getSingletonClass().setJavaProxy(true);

        // set the Java class name and package
        proxy.setBaseName(javaClass.getSimpleName());

        // set parent to either package module or outer class
        RubyModule parent;
        if (javaClass.getEnclosingClass() != null) {
            parent = Java.getProxyClass(javaClassObject.getRuntime(), javaClass.getEnclosingClass());
        } else {
            parent = Java.getJavaPackageModule(javaClassObject.getRuntime(), javaClass.getPackage());
        }
        proxy.setParent(parent);

        // FIXME: bit of a kludge here (non-interface classes assigned to both
        // class and module fields). simplifies proxy extender code, will go away
        // when JI is overhauled (and proxy extenders are deprecated).
        javaClassObject.proxyClass = proxyClass;
        javaClassObject.proxyModule = proxy;

        javaClassObject.applyProxyExtenders();

        // TODO: we can probably release our references to the constantFields
        // array and static/instance callback hashes at this point.
    }
}
