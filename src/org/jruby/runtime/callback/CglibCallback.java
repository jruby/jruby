package org.jruby.runtime.callback;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.builtin.IRubyObject;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Similar to ReflectionCallback, but uses CGLIB's reflection optimizer
 * instead of Java's native reflection for instance calls.
 *
 * @see org.jruby.runtime.callback.ReflectionCallback
 */
public class CglibCallback extends ReflectionCallback {
    private static final Map fastClassCache = new HashMap();

    public CglibCallback(
        Class klass,
        String methodName,
        Class[] args,
        boolean isRestArgs,
        boolean isStaticMethod,
        Arity arity)
    {
        super(klass, methodName, args, isRestArgs, isStaticMethod, arity);
    }

    protected CallType callType(boolean isStaticMethod) {
        if (isStaticMethod) {
            return new ReflectionCallback.StaticCallType();
        } else {
            return new CglibInstanceCallType();
        }
    }

    private class CglibInstanceCallType extends CallType {
        private FastClass fastClass;
        private final FastMethod fastMethod;

        public CglibInstanceCallType() {
            this.fastClass = (FastClass) fastClassCache.get(klass.getName());
            if (fastClass == null) {
                this.fastClass = FastClass.create(klass);
                fastClassCache.put(klass.getName(), fastClass);
            }
            try {
                fastMethod = fastClass.getMethod(methodName, reflectionArgumentTypes());
            } catch (SecurityException e) {
                throw new RuntimeException(
                    "SecurityException: Cannot get method \""
                        + methodName
                        + "\" in class \""
                        + klass.getName()
                        + "\" by Reflection.");
            }
        }

        public IRubyObject invokeMethod(IRubyObject recv, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException
        {
            if (isRestArgs) {
                arguments = packageRestArgumentsForReflection(arguments);
            }
            return (IRubyObject) fastMethod.invoke(recv, arguments);
        }

        public Class[] reflectionArgumentTypes() {
            return argumentTypes;
        }
    }
}
