
package org.jruby.runtime.callback;

import org.jruby.Ruby;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Asserts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CompiledReflectionCallback implements Callback {
    private Ruby runtime;
    private String methodName;
    private String className;
    private int arity;
    private ClassLoader classLoader;
    private Method method = null;

    public CompiledReflectionCallback(Ruby runtime, String className, String methodName, int arity, ClassLoader classLoader) {
        this.runtime = runtime;
        this.className = className;
        this.methodName = methodName;
        this.arity = arity;
        this.classLoader = classLoader;
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
        Asserts.isTrue(arity == args.length);
        Object[] arguments = new Object[2 + args.length];
        arguments[0] = runtime;
        arguments[1] = recv;
        System.arraycopy(args, 0, arguments, 2, args.length);
        try {
            return (IRubyObject) getMethod().invoke(null, arguments);
        } catch (IllegalAccessException e) {
            Asserts.notReached(e.toString());
            return null;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                Asserts.notReached(e.getCause().toString());
                return null;
            }
        }
    }

    public Arity getArity() {
        return Arity.fixed(arity);
    }

    private Method getMethod() {
        if (method != null) {
            return method;
        }
        try {
            Class javaClass = getJavaClass();
            Class[] args = new Class[2 + arity];
            args[0] = Ruby.class;
            args[1] = IRubyObject.class;
            for (int i = 2; i < args.length; i++) {
                args[i] = IRubyObject.class;
            }
            method = javaClass.getMethod(methodName, args);
            return method;

        } catch (NoSuchMethodException e) {
            Asserts.notReached("method not found: " + methodName);
            return null;
        }
    }

    private Class getJavaClass() {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Asserts.notReached("class not found: " + className);
            return null;
        }
    }
}
