package org.jruby.javasupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class Java {
    public static RubyModule createJavaModule(Ruby runtime) {
        RubyModule javaModule = runtime.defineModule("Java");
        CallbackFactory callbackFactory = runtime.callbackFactory();
        javaModule.defineModuleFunction("define_exception_handler", callbackFactory.getOptSingletonMethod(Java.class, "define_exception_handler"));
        javaModule.defineModuleFunction("primitive_to_java", callbackFactory.getSingletonMethod(Java.class, "primitive_to_java", IRubyObject.class));
        javaModule.defineModuleFunction("java_to_primitive", callbackFactory.getSingletonMethod(Java.class, "java_to_primitive", IRubyObject.class));
        javaModule.defineModuleFunction("new_proxy_instance", callbackFactory.getOptSingletonMethod(Java.class, "new_proxy_instance"));

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
        if (object instanceof JavaObject) {
            return object;
        }
        Ruby runtime = recv.getRuntime();
        Object javaObject;
        if (object.isNil()) {
            javaObject = null;
        } else if (object instanceof RubyFixnum) {
            javaObject = new Long(((RubyFixnum) object).getLongValue());
        } else if (object instanceof RubyBignum) {
            javaObject = ((RubyBignum) object).getValue();
        } else if (object instanceof RubyFloat) {
            javaObject = new Double(((RubyFloat) object).getValue());
        } else if (object instanceof RubyString) {
            javaObject = ((RubyString) object).getValue();
        } else if (object instanceof RubyBoolean) {
            javaObject = Boolean.valueOf(object.isTrue());
        } else {
            javaObject = object;
        }
        return JavaObject.wrap(runtime, javaObject);
    }

    public static IRubyObject java_to_primitive(IRubyObject recv, IRubyObject object) {
        if (object instanceof JavaObject) {
            return JavaUtil.convertJavaToRuby(recv.getRuntime(), ((JavaObject) object).getValue());
        }
		return object;
    }

    public static IRubyObject new_proxy_instance(final IRubyObject recv, IRubyObject[] args) {
        if (args.length < 1) {
            throw recv.getRuntime().newArgumentError("wrong # of arguments(" + args.length + " for 1)");
        }

        final RubyProc proc = args[args.length - 1] instanceof RubyProc ? (RubyProc)args[args.length - 1] : RubyProc.newProc(recv.getRuntime());
        int size = args[args.length - 1] instanceof RubyProc ? args.length - 1 : args.length;

        Class[] interfaces = new Class[size];
        for (int i = 0; i < size; i++) {
            if (!(args[i] instanceof JavaClass) || !((JavaClass)args[i]).interface_p().isTrue()) {
                throw recv.getRuntime().newArgumentError("Java interface expected.");
            }
            interfaces[i] = ((JavaClass) args[i]).javaClass();
        }

        return JavaObject.wrap(recv.getRuntime(), Proxy.newProxyInstance(recv.getRuntime().getJavaSupport().getJavaClassLoader(), interfaces, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("toString") && method.getParameterTypes().length == 0) {
                    return proxy.getClass().getName();
                } else if (method.getName().equals("hashCode") && method.getParameterTypes().length == 0) {
                    return new Integer(proxy.getClass().hashCode());
                } else if (method.getName().equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(Object.class)) {
                    return Boolean.valueOf(proxy == args[1]);
                }
                int length = args == null ? 0 : args.length;
                IRubyObject[] rubyArgs = new IRubyObject[length + 2];
                rubyArgs[0] = JavaObject.wrap(recv.getRuntime(), proxy);
                rubyArgs[1] = new JavaMethod(recv.getRuntime(), method);
                for (int i = 0; i < length; i++) {
                    rubyArgs[i + 2] = JavaObject.wrap(recv.getRuntime(), args[i]);
                }
                return JavaUtil.convertArgument(proc.call(rubyArgs), method.getReturnType());
            }
        }));
    }
}