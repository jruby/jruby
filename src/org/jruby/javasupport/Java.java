package org.jruby.javasupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyProc;

public class Java {
    public static RubyModule createJavaModule(Ruby runtime) {
        RubyModule javaModule = runtime.defineModule("Java");

        javaModule.defineModuleFunction("define_exception_handler", CallbackFactory.getOptSingletonMethod(Java.class, "define_exception_handler"));
        javaModule.defineModuleFunction("primitive_to_java", CallbackFactory.getSingletonMethod(Java.class, "primitive_to_java", IRubyObject.class));
        javaModule.defineModuleFunction("java_to_primitive", CallbackFactory.getSingletonMethod(Java.class, "java_to_primitive", IRubyObject.class));
        javaModule.defineModuleFunction("new_proxy_instance", CallbackFactory.getOptSingletonMethod(Java.class, "new_proxy_instance"));

        JavaClass.createJavaClassClass(runtime, javaModule);
        JavaMethod.createJavaMethodClass(runtime, javaModule);
        JavaConstructor.createJavaConstructorClass(runtime, javaModule);
        JavaField.createJavaFieldClass(runtime, javaModule);

        return javaModule;
    }

	// Java methods
    public static IRubyObject define_exception_handler(IRubyObject recv, IRubyObject[] args) {
        String name = args[0].toString();
        RubyProc handler = null;
        if (args.length > 1) {
            handler = (RubyProc)args[1];
        } else {
            handler = RubyProc.newProc(recv.getRuntime());
        }
        recv.getRuntime().getJavaSupport().defineExceptionHandler(name, handler);

        return recv;
    }

    public static IRubyObject primitive_to_java(IRubyObject recv, IRubyObject object) {
        Ruby runtime = recv.getRuntime();
        Object javaObject = JavaUtil.convertRubyToJava(runtime, object);
        return new JavaObject(runtime, javaObject);
    }

    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        } else {
            return object;
        }
    }

    public static IRubyObject new_proxy_instance(final IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1) {
            throw new ArgumentError(recv.getRuntime(), "wrong # of arguments(" + args.length + " for 1)");
        }

        final RubyProc proc = args[args.length - 1] instanceof RubyProc ? (RubyProc)args[args.length - 1] : RubyProc.newProc(recv.getRuntime());
        int size = args[args.length - 1] instanceof RubyProc ? args.length - 1 : args.length;

        Class[] interfaces = new Class[size];
        for (int i = 0; i < size; i++) {
            if (!(args[i] instanceof JavaClass) || !((JavaClass)args[i]).interface_p().isTrue()) {
                throw new ArgumentError(recv.getRuntime(), "Java interface expected.");
            }
            interfaces[i] = args[i].getJavaClass();
        }

        return new JavaObject(recv.getRuntime(), Proxy.newProxyInstance(recv.getRuntime().getJavaSupport().getJavaClassLoader(), interfaces, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                IRubyObject rubyArgs[] = new IRubyObject[args.length + 2];
                rubyArgs[0] = new JavaObject(recv.getRuntime(), proxy);
                rubyArgs[1] = new JavaMethod(recv.getRuntime(), method);
                for (int i = 0, length = args.length; i < length; i++) {
                    rubyArgs[i + 2] = new JavaObject(recv.getRuntime(), args[i]);
                }
                return JavaUtil.convertArgument(proc.call(rubyArgs), method.getReturnType());
            }
        }));
    }
}