    package org.jruby.runtime.callback;

import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.jruby.exceptions.RaiseException;
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
public class CglibCallback extends AbstractCallback {
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
        FastClass fastClass = fastClass(klass);
        if (isStaticMethod) {
            return new CglibStaticCallType(fastClass);
        } else {
            return new CglibInstanceCallType(fastClass);
        }
    }

    private class CglibStaticCallType extends CallType {
        private final FastMethod fastMethod;

        public CglibStaticCallType(FastClass fastClass) {
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
            Object[] result = new Object[arguments.length + 1];
            System.arraycopy(arguments, 0, result, 1, arguments.length);
            result[0] = recv;
            try {
                return (IRubyObject) fastMethod.invoke(null, result);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ClassCastException) {
                    throw new RaiseException(recv.getRuntime(), "TypeError", e.getCause().getMessage());
                } else {
                    throw e;
                }
            }
        }

        public Class[] reflectionArgumentTypes() {
            Class[] result = new Class[argumentTypes.length + 1];
            System.arraycopy(argumentTypes, 0, result, 1, argumentTypes.length);
            result[0] = IRubyObject.class;
            return result;
        }
    }

    private class CglibInstanceCallType extends CallType {
        private final FastMethod fastMethod;

        public CglibInstanceCallType(FastClass fastClass) {
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
            try {
                return (IRubyObject) fastMethod.invoke(recv, arguments);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ClassCastException) {
                    throw new RaiseException(recv.getRuntime(), "TypeError", e.getCause().getMessage());
                } else {
                    throw e;
                }
            }
        }

        public Class[] reflectionArgumentTypes() {
            return argumentTypes;
        }
    }

    private FastClass fastClass(Class klass) {
        FastClass result = (FastClass) fastClassCache.get(klass.getName());
        if (result == null) {
            result = FastClass.create(klass);
            fastClassCache.put(klass.getName(), result);
        }
        return result;
    }
}
