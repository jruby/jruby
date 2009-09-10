package org.jruby.java.dispatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.javasupport.JavaMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.javasupport.proxy.JavaProxyConstructor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Method selection logic for calling from Ruby to Java.
 */
public class CallableSelector {
    public static ParameterTypes matchingCallableArityN(IRubyObject recv, Map cache, ParameterTypes[] methods, IRubyObject[] args, int argsLength) {
        int signatureCode = argsHashCode(args);
        ParameterTypes method = (ParameterTypes)cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(recv, cache, signatureCode, methods, args);
        }
        return method;
    }

    // NOTE: The five match methods are arity-split to avoid the cost of boxing arguments
    // when there's already a cached match. Do not condense them into a single
    // method.
    public static JavaCallable matchingCallableArityN(IRubyObject recv, Map cache, JavaCallable[] methods, IRubyObject[] args, int argsLength) {
        int signatureCode = argsHashCode(args);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(recv, cache, signatureCode, methods, args);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityOne(IRubyObject recv, Map cache, JavaCallable[] methods, IRubyObject arg0) {
        int signatureCode = argsHashCode(arg0);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(recv, cache, signatureCode, methods, arg0);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityTwo(IRubyObject recv, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1) {
        int signatureCode = argsHashCode(arg0, arg1);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(recv, cache, signatureCode, methods, arg0, arg1);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityThree(IRubyObject recv, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        int signatureCode = argsHashCode(arg0, arg1, arg2);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(recv, cache, signatureCode, methods, arg0, arg1, arg2);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityFour(IRubyObject recv, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(recv, cache, signatureCode, methods, arg0, arg1, arg2, arg3);
        }
        return method;
    }

    private static ParameterTypes findMatchingCallableForArgs(IRubyObject recv, Map cache, int signatureCode, ParameterTypes[] methods, IRubyObject... args) {
        ParameterTypes method = findCallable(methods, Exact, args);
        if (method == null) {
            method = findCallable(methods, AssignableAndPrimitivable, args);
        }
        if (method == null) {
            method = findCallable(methods, AssignableOrDuckable, args);
        }
        if (method == null) {
            throw argTypesDoNotMatch(recv.getRuntime(), recv, methods, (Object[])args);
        } else {
            cache.put(signatureCode, method);
            return method;
        }
    }

    private static ParameterTypes findCallable(ParameterTypes[] callables, CallableAcceptor acceptor, IRubyObject... args) {
        ParameterTypes bestCallable = null;
        int bestScore = -1;
        for (int k = 0; k < callables.length; k++) {
            ParameterTypes callable = callables[k];
            Class<?>[] types = callable.getParameterTypes();

            if (acceptor.accept(types, args)) {
                int currentScore = getExactnessScore(types, args);
                if (currentScore > bestScore) {
                    bestCallable = callable;
                    bestScore = currentScore;
                }
            }
        }
        return bestCallable;
    }

    private static int getExactnessScore(Class<?>[] types, IRubyObject[] args) {
        int count = 0;
        for (int i = 0; i < args.length; i++) {
            if (types[i].equals(argClass(args[i]))) {
                count++;
            }
        }
        return count;
    }

    private static interface CallableAcceptor {

        public boolean accept(Class<?>[] types, IRubyObject[] args);
    }
    private static final CallableAcceptor Exact = new CallableAcceptor() {

        public boolean accept(Class<?>[] types, IRubyObject[] args) {
            return exactMatch(types, args);
        }
    };
    private static final CallableAcceptor AssignableAndPrimitivable = new CallableAcceptor() {

        public boolean accept(Class<?>[] types, IRubyObject[] args) {
            return assignableAndPrimitivable(types, args);
        }
    };
    private static final CallableAcceptor AssignableOrDuckable = new CallableAcceptor() {

        public boolean accept(Class<?>[] types, IRubyObject[] args) {
            return assignableOrDuckable(types, args);
        }
    };

    private static boolean exactMatch(Class[] types, IRubyObject... args) {
        for (int i = 0; i < types.length; i++) {
            if (!types[i].equals(argClass(args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableAndPrimitivable(Class[] types, IRubyObject... args) {
        for (int i = 0; i < types.length; i++) {
            if (!(assignable(types[i], args[i]) && primitivable(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableOrDuckable(Class[] types, IRubyObject... args) {
        for (int i = 0; i < types.length; i++) {
            if (!(assignable(types[i], args[i]) || duckable(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignable(Class type, IRubyObject arg) {
        return JavaClass.assignable(type, argClass(arg));
    }

    /**
     * This method checks whether an argument can be *directly* converted into
     * the target primitive, i.e. without changing from integral to floating-point.
     *
     * @param type The target type
     * @param arg The argument to convert
     * @return Whether the argument can be directly converted to the target primitive type
     */
    private static boolean primitivable(Class type, IRubyObject arg) {
        Class argClass = argClass(arg);
        if (type.isPrimitive()) {
            // TODO: This is where we would want to do precision checks to see
            // if it's non-destructive to coerce a given type into the target
            // integral primitive
            if (type == Integer.TYPE || type == Long.TYPE || type == Short.TYPE || type == Character.TYPE) {
                return argClass == long.class || // long first because it's what Fixnum claims to be
                        argClass == byte.class ||
                        argClass == short.class ||
                        argClass == char.class ||
                        argClass == int.class ||
                        argClass == Long.class ||
                        argClass == Byte.class ||
                        argClass == Short.class ||
                        argClass == Character.class ||
                        argClass == Integer.class;
            } else if (type == Float.TYPE || type == Double.TYPE) {
                return argClass == double.class || // double first because it's what float claims to be
                        argClass == float.class ||
                        argClass == Float.class ||
                        argClass == Double.class;
            } else if (type == Boolean.TYPE) {
                return argClass == boolean.class ||
                        argClass == Boolean.class;
            }
        }
        return false;
    }

    private static boolean duckable(Class type, IRubyObject arg) {
        return JavaUtil.isDuckTypeConvertable(argClass(arg), type);
    }

    private static int argsHashCode(IRubyObject a0) {
        return 31 + classHashCode(a0);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1) {
        return 31 * argsHashCode(a0) + classHashCode(a1);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2) {
        return 31 * argsHashCode(a0, a1) + classHashCode(a2);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2, IRubyObject a3) {
        return 31 * argsHashCode(a0, a1, a2) + classHashCode(a3);
    }

    private static int argsHashCode(IRubyObject[] a) {
        if (a == null) {
            return 0;
        }

        int result = 1;

        for (IRubyObject element : a) {
            result = 31 * result + classHashCode(element);
        }

        return result;
    }

    private static int classHashCode(IRubyObject o) {
        return o == null ? 0 : o.getJavaClass().hashCode();
    }

    private static Class argClass(IRubyObject a) {
        if (a == null) {
            return void.class;
        }

        return a.getJavaClass();
    }

    private static RaiseException argTypesDoNotMatch(Ruby runtime, IRubyObject receiver, Object[] methods, Object... args) {
        ArrayList<Class<?>> argTypes = new ArrayList<Class<?>>(args.length);

        for (Object o : args) {
            argTypes.add(argClassTypeError(o));
        }

        return argumentError(runtime.getCurrentContext(), methods[0], receiver, argTypes);
    }

    private static Class argClassTypeError(Object object) {
        if (object == null) {
            return void.class;
        }
        if (object instanceof ConcreteJavaProxy) {
            return ((ConcreteJavaProxy)object).getJavaClass();
        }

        return object.getClass();
    }

    private static RaiseException argumentError(ThreadContext context, Object method, IRubyObject receiver, List<Class<?>> argTypes) {
        String methodName = (method instanceof JavaConstructor || method instanceof JavaProxyConstructor) ? "constructor" : ((JavaMethod)method).name().toString();

        return context.getRuntime().newNameError("no " + methodName + " with arguments matching " +
                argTypes + " on object " + receiver.callMethod(context, "inspect"), null);
    }
}
