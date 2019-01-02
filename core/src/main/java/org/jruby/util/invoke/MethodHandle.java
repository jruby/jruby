package org.jruby.util.invoke;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodHandle {

    private static abstract class Callable {
        public abstract Object invoke(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException;
    }

    private static class CallableConstructor extends Callable {
        private Constructor constructor;

        public CallableConstructor(Constructor constructor) {
            this.constructor = constructor;
        }

        public Object invoke(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return constructor.newInstance(args);
        }
    }

    private static class CallableMethod extends Callable {
        private Object receiver;
        private Method method;

        public CallableMethod(Object receiver, Method method) {
            this.receiver = receiver;
            this.method = method;
        }

        public Object invoke(Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            return method.invoke(receiver, args);
        }
    }

    private Callable callable;
    private MethodType castType = null;

    protected MethodHandle(Object receiver, Method method) {
        this.callable = (Callable) new CallableMethod(receiver, method);
    }

    protected MethodHandle(Constructor constructor) {
        this.callable = (Callable) new CallableConstructor(constructor);
    }

    private MethodHandle(Callable original, MethodType type) {
        this.callable = original;
        this.castType = type;
    }

    public MethodHandle asType(MethodType type) {
        return new MethodHandle(callable, type);
    }

    public Object invokeExact(Object... args) throws Throwable {
        Object result =  callable.invoke(args);
        if (castType != null) {
            return castType.returnType().cast(result);
        } else {
            return result;
        }
    }

    public Object invoke(Object... args) throws Throwable {
        return invokeExact(args);
    }
}
