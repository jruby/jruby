package org.jruby.java.dispatch;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
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
import org.jruby.util.CodegenUtils;

/**
 * Method selection logic for calling from Ruby to Java.
 */
public class CallableSelector {
    public static ParameterTypes matchingCallableArityN(Ruby runtime, Map cache, ParameterTypes[] methods, IRubyObject[] args, int argsLength) {
        int signatureCode = argsHashCode(args);
        ParameterTypes method = (ParameterTypes)cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, cache, signatureCode, methods, args);
        }
        return method;
    }

    // NOTE: The five match methods are arity-split to avoid the cost of boxing arguments
    // when there's already a cached match. Do not condense them into a single
    // method.
    public static JavaCallable matchingCallableArityN(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject[] args, int argsLength) {
        int signatureCode = argsHashCode(args);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(runtime, cache, signatureCode, methods, args);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityOne(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0) {
        int signatureCode = argsHashCode(arg0);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(runtime, cache, signatureCode, methods, arg0);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityTwo(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1) {
        int signatureCode = argsHashCode(arg0, arg1);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(runtime, cache, signatureCode, methods, arg0, arg1);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityThree(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        int signatureCode = argsHashCode(arg0, arg1, arg2);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(runtime, cache, signatureCode, methods, arg0, arg1, arg2);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityFour(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        JavaCallable method = (JavaCallable)cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable)findMatchingCallableForArgs(runtime, cache, signatureCode, methods, arg0, arg1, arg2, arg3);
        }
        return method;
    }

    private static final boolean DEBUG = true;

    private static ParameterTypes findMatchingCallableForArgs(Ruby runtime, Map cache, int signatureCode, ParameterTypes[] methods, IRubyObject... args) {
        ParameterTypes method = null;

        // try the new way first
        List<ParameterTypes> newFinds = findCallable(methods, args);
        if (newFinds.size() > 0) {
            // new way found one, so let's go with that
            if (newFinds.size() == 1) {
                method = newFinds.get(0);
            } else {
                // narrow to most specific version (or first version, if none are more specific
                ParameterTypes mostSpecific = null;
                Class[] msTypes = null;
                boolean ambiguous = false;
                OUTER: for (ParameterTypes candidate : newFinds) {
                    if (mostSpecific == null) {
                        mostSpecific = candidate;
                        msTypes = mostSpecific.getParameterTypes();
                        continue;
                    }

                    Class[] cTypes = candidate.getParameterTypes();

                    for (int i = 0; i < msTypes.length; i++) {
                        if (msTypes[i] != cTypes[i] && msTypes[i].isAssignableFrom(cTypes[i])) {
                            mostSpecific = candidate;
                            msTypes = cTypes;
                            ambiguous = false;
                            continue OUTER;
                        }
                    }

                    // none more specific; check for ambiguities
                    for (int i = 0; i < msTypes.length; i++) {
                        if (msTypes[i] != cTypes[i] && !msTypes[i].isAssignableFrom(cTypes[i]) && !cTypes[i].isAssignableFrom(msTypes[i])) {
                            ambiguous = true;
                        } else {
                            ambiguous = false;
                            continue OUTER;
                        }
                    }
                }
                method = mostSpecific;

                if (ambiguous) {
                    runtime.getWarnings().warn("ambiguous Java methods found, using " + ((Member) ((JavaCallable) method).accessibleObject()).getName() + CodegenUtils.prettyParams(msTypes));
                }
            }
        }

        // fall back on old ways
        if (method == null) {
            method = findCallable(methods, Exact, args);
        }
        if (method == null) {
            method = findCallable(methods, AssignableAndPrimitivable, args);
        }
        if (method == null) {
            method = findCallable(methods, AssignableOrDuckable, args);
        }
        if (method == null) {
            method = findCallable(methods, AssignableAndPrimitivableWithVarargs, args);
        }
        
        // cache found result
        if (method != null) cache.put(signatureCode, method);
        
        return method;
    }

    private static void warnMultipleMatches(IRubyObject[] args, List<ParameterTypes> newFinds) {
        RubyClass[] argTypes = new RubyClass[args.length];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = args[i].getMetaClass();
        }
        StringBuilder builder = new StringBuilder("multiple Java methods for arguments (");
        boolean first = true;
        for (RubyClass argType : argTypes) {
            if (!first) {
                builder.append(",");
            }
            first = false;
            builder.append(argType);
        }
        builder.append("), using first:");
        for (ParameterTypes types : newFinds) {
            builder.append("\n  ").append(types);
        }
        args[0].getRuntime().getWarnings().warn(builder.toString());
    }

    private static ParameterTypes findCallable(ParameterTypes[] callables, CallableAcceptor acceptor, IRubyObject... args) {
        ParameterTypes bestCallable = null;
        int bestScore = -1;
        for (int k = 0; k < callables.length; k++) {
            ParameterTypes callable = callables[k];

            if (acceptor.accept(callable, args)) {
                int currentScore = getExactnessScore(callable, args);
                if (currentScore > bestScore) {
                    bestCallable = callable;
                    bestScore = currentScore;
                }
            }
        }
        return bestCallable;
    }

    private static List<ParameterTypes> findCallable(ParameterTypes[] callables, IRubyObject... args) {
        List<ParameterTypes> retainedCallables = new ArrayList<ParameterTypes>(callables.length);
        List<ParameterTypes> incomingCallables = new ArrayList<ParameterTypes>(Arrays.asList(callables));
        
        for (int currentArg = 0; currentArg < args.length; currentArg++) {
            retainedCallables.clear();
            for (Matcher matcher : MATCH_SEQUENCE) {
                for (Iterator<ParameterTypes> callableIter = incomingCallables.iterator(); callableIter.hasNext();) {
                    ParameterTypes callable = callableIter.next();
                    Class[] types = callable.getParameterTypes();

                    if (matcher.match(types[currentArg], args[currentArg])) {
                        callableIter.remove();
                        retainedCallables.add(callable);
                    }
                }
            }
            incomingCallables.clear();
            incomingCallables.addAll(retainedCallables);
        }

        return retainedCallables;
    }

    private static int getExactnessScore(ParameterTypes paramTypes, IRubyObject[] args) {
        Class[] types = paramTypes.getParameterTypes();
        int count = 0;

        if (paramTypes.isVarArgs()) {
            // varargs exactness gives the last N args as +1 since they'll already
            // have been determined to fit

            // dig out as many trailing args as possible that match varargs type
            int nonVarargs = types.length - 1;
            
            // add one for vararg
            count += 1;

            // check remaining args
            for (int i = 0; i < nonVarargs && i < args.length; i++) {
                if (types[i].equals(argClass(args[i]))) {
                    count++;
                }
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                if (types[i].equals(argClass(args[i]))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static interface CallableAcceptor {

        public boolean accept(ParameterTypes types, IRubyObject[] args);
    }
    private static final CallableAcceptor Exact = new CallableAcceptor() {

        public boolean accept(ParameterTypes types, IRubyObject[] args) {
            return exactMatch(types, args);
        }
    };
    private static final CallableAcceptor AssignableAndPrimitivable = new CallableAcceptor() {

        public boolean accept(ParameterTypes types, IRubyObject[] args) {
            return assignableAndPrimitivable(types, args);
        }
    };
    private static final CallableAcceptor AssignableOrDuckable = new CallableAcceptor() {

        public boolean accept(ParameterTypes types, IRubyObject[] args) {
            return assignableOrDuckable(types, args);
        }
    };
    private static final CallableAcceptor AssignableAndPrimitivableWithVarargs = new CallableAcceptor() {

        public boolean accept(ParameterTypes types, IRubyObject[] args) {
            return assignableAndPrimitivableWithVarargs(types, args);
        }
    };

    private interface Matcher {
        public boolean match(Class type, IRubyObject arg);
    }

    private static boolean exactMatch(ParameterTypes paramTypes, IRubyObject... args) {
        Class[] types = paramTypes.getParameterTypes();
        
        if (args.length != types.length) return false;
        
        for (int i = 0; i < types.length; i++) {
            if (!EXACT.match(types[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static Matcher EXACT = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return type.equals(argClass(arg))
                    || (type.isPrimitive() && CodegenUtils.getBoxType(type) == argClass(arg));
        }
    };

    private static Matcher ASSIGNABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return assignable(type, arg);
        }
    };

    private static Matcher PRIMITIVABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return primitivable(type, arg);
        }
    };

    private static Matcher DUCKABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return duckable(type, arg);
        }
    };

    private static final Matcher[] MATCH_SEQUENCE = new Matcher[] {EXACT, PRIMITIVABLE, ASSIGNABLE, DUCKABLE};

    private static boolean assignableAndPrimitivable(ParameterTypes paramTypes, IRubyObject... args) {
        Class[] types = paramTypes.getParameterTypes();
        
        if (args.length != types.length) return false;
        
        for (int i = 0; i < types.length; i++) {
            if (!(ASSIGNABLE.match(types[i], args[i]) && PRIMITIVABLE.match(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableOrDuckable(ParameterTypes paramTypes, IRubyObject... args) {
        Class[] types = paramTypes.getParameterTypes();
        
        if (args.length != types.length) return false;
        
        for (int i = 0; i < types.length; i++) {
            if (!(ASSIGNABLE.match(types[i], args[i]) || DUCKABLE.match(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableAndPrimitivableWithVarargs(ParameterTypes paramTypes, IRubyObject... args) {
        // bail out if this is not a varargs method
        if (!paramTypes.isVarArgs()) return false;
        
        Class[] types = paramTypes.getParameterTypes();

        Class varArgArrayType = types[types.length - 1];
        Class varArgType = varArgArrayType.getComponentType();
        
        // if there's no args, we only match when there's just varargs
        if (args.length == 0) {
            return types.length <= 1;
        }

        // dig out as many trailing args as will fit, ensuring they match varargs type
        int nonVarargs = types.length - 1;
        for (int i = args.length - 1; i >= nonVarargs; i--) {
            if (!(ASSIGNABLE.match(varArgType, args[i]) || PRIMITIVABLE.match(varArgType, args[i]))) {
                return false;
            }
        }

        // check remaining args
        for (int i = 0; i < nonVarargs; i++) {
            if (!(ASSIGNABLE.match(types[i], args[i]) || PRIMITIVABLE.match(types[i], args[i]))) {
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

    public static RaiseException argTypesDoNotMatch(Ruby runtime, IRubyObject receiver, JavaCallable[] methods, Object... args) {
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = argClassTypeError(args[i]);
        }

        return argumentError(runtime.getCurrentContext(), methods, receiver, argTypes);
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

    private static RaiseException argumentError(ThreadContext context, ParameterTypes[] methods, IRubyObject receiver, Class[] argTypes) {
        boolean constructor = methods[0] instanceof JavaConstructor || methods[0] instanceof JavaProxyConstructor;
        
        StringBuffer fullError = new StringBuffer();
        fullError.append("no ");
        if (constructor) {
            fullError.append("constructor");
        } else {
            fullError.append("method '")
                    .append(((JavaMethod)methods[0]).name().toString())
                    .append("' ");
        }
        fullError.append("for arguments ")
                .append(CodegenUtils.prettyParams(argTypes))
                .append(" on ");
        if (receiver instanceof RubyModule) {
            fullError.append(((RubyModule)receiver).getName());
        } else {
            fullError.append(receiver.getMetaClass().getRealClass().getName());
        }
        
        if (methods.length > 1) {
            fullError.append("\n  available overloads:");
            for (ParameterTypes method : methods) {
                fullError.append("\n    " + CodegenUtils.prettyParams(method.getParameterTypes()));
            }
        }
        
        return context.runtime.newNameError(fullError.toString(), null);
    }
}
