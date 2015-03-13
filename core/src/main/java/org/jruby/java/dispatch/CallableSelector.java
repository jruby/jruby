package org.jruby.java.dispatch;

import java.lang.reflect.Member;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyString;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

/**
 * Method selection logic for calling from Ruby to Java.
 */
public class CallableSelector {

    private CallableSelector() { /* no-instances */ }

    //private static final boolean DEBUG = true;

    public static ParameterTypes matchingCallableArityN(Ruby runtime, Map cache, ParameterTypes[] methods, IRubyObject[] args, int argsLength) {
        final int signatureCode = argsHashCode(args);
        ParameterTypes method = (ParameterTypes) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, args);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    // NOTE: The five match methods are arity-split to avoid the cost of boxing arguments
    // when there's already a cached match. Do not condense them into a single
    // method.
    public static JavaCallable matchingCallableArityN(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject[] args, int argsLength) {
        final int signatureCode = argsHashCode(args);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable) findMatchingCallableForArgs(runtime, methods, args);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityOne(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0) {
        final int signatureCode = argsHashCode(arg0);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable) findMatchingCallableForArgs(runtime, methods, arg0);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityTwo(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1) {
        final int signatureCode = argsHashCode(arg0, arg1);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable) findMatchingCallableForArgs(runtime, methods, arg0, arg1);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityThree(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable) findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    public static JavaCallable matchingCallableArityFour(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = (JavaCallable) findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2, arg3);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    private static ParameterTypes findMatchingCallableForArgs(final Ruby runtime,
        final ParameterTypes[] methods, final IRubyObject... args) {
        ParameterTypes method = null;

        // try the new way first
        final List<ParameterTypes> candidates = findCallableCandidates(methods, args);
        final int size = candidates.size();

        if ( size > 0 ) {
            // new way found one, so let's go with that
            if ( size == 1 ) method = candidates.get(0);
            else { // narrow to most specific version (or first version, if none are more specific)
                ParameterTypes mostSpecific = candidates.get(0);
                Class<?>[] msTypes = mostSpecific.getParameterTypes();
                boolean ambiguous = false;

                OUTER: for ( int c = 1; c < size; c++ ) {
                    final ParameterTypes candidate = candidates.get(c);
                    final Class<?>[] cTypes = candidate.getParameterTypes();

                    for ( int i = 0; i < msTypes.length; i++ ) {
                        final Class<?> msType = msTypes[i], cType = cTypes[i];
                        if ( msType != cType && msType.isAssignableFrom(cType) ) {
                            mostSpecific = candidate;
                            msTypes = cTypes;
                            ambiguous = false; continue OUTER;
                        }
                    }
                    // none more specific; check for ambiguities
                    for ( int i = 0; i < msTypes.length; i++ ) {
                        final Class<?> msType = msTypes[i], cType = cTypes[i];
                        if ( msType == cType || msType.isAssignableFrom(cType) || cType.isAssignableFrom(msType) ) {
                            ambiguous = false; continue OUTER;
                        } else {
                            ambiguous = true;
                        }
                    }
                    // somehow we can still decide e.g. if we got a RubyFixnum
                    // then (int) constructor shoudl be preffered over (float)
                    if ( ambiguous ) {
                        int msPref = 0, cPref = 0;
                        for ( int i = 0; i < msTypes.length; i++ ) {
                            final Class<?> msType = msTypes[i], cType = cTypes[i];
                            msPref += calcTypePreference(msType, args[i]);
                            cPref += calcTypePreference(cType, args[i]);
                        }
                        // for backwards compatibility we do not switch to cType as
                        // the better fit - we seem to lack tests on this front ...
                        if ( msPref > cPref ) ambiguous = false; // continue OUTER;
                    }
                }
                method = mostSpecific;

                if ( ambiguous ) {
                    runtime.getWarnings().warn("ambiguous Java methods found, using " + ((Member) ((JavaCallable) method).accessibleObject()).getName() + CodegenUtils.prettyParams(msTypes));
                }
            }
        }

        // fall back on old ways
        if (method == null) {
            method = findCallable(methods, Exact, args);
            if (method == null) {
                method = findCallable(methods, AssignableAndPrimitivable, args);
                if (method == null) {
                    method = findCallable(methods, AssignableOrDuckable, args);
                    if (method == null) {
                        method = findCallable(methods, AssignableOrDuckable, args);
                        if (method == null) {
                            method = findCallable(methods, AssignableAndPrimitivableWithVarargs, args);
                        }
                    }
                }
            }
        }

        return method;
    }

    private static ParameterTypes findCallable(ParameterTypes[] callables, CallableAcceptor acceptor, IRubyObject[] args) {
        ParameterTypes bestCallable = null;
        int bestScore = -1;
        for ( int i = 0; i < callables.length; i++ ) {
            ParameterTypes callable = callables[i];

            if ( acceptor.accept(callable, args) ) {
                int currentScore = calcExactnessScore(callable, args);
                if (currentScore > bestScore) {
                    bestCallable = callable; bestScore = currentScore;
                }
            }
        }
        return bestCallable;
    }

    private static List<ParameterTypes> findCallableCandidates(final ParameterTypes[] callables,
        final IRubyObject[] args) {
        // in case of an exact match prefer to return it early :
        for ( int c = 0; c < callables.length; c++ ) {
            final ParameterTypes callable = callables[c];
            if ( exactMatch(callable, args ) ) return Collections.singletonList(callable);
        }

        final ArrayList<ParameterTypes> retained = new ArrayList<ParameterTypes>(callables.length);
        ParameterTypes[] incoming = callables.clone();

        for ( int i = 0; i < args.length; i++ ) {
            retained.clear();
            for ( final Matcher matcher : NON_EXACT_MATCH_SEQUENCE ) {
                for ( int c = 0; c < incoming.length; c++ ) {
                    ParameterTypes callable = incoming[c];
                    if ( callable == null ) continue; // removed (matched)

                    Class[] types = callable.getParameterTypes();

                    if ( matcher.match( types[i], args[i] ) ) {
                        retained.add(callable);
                        incoming[c] = null; // retaining - remove
                    }
                }
            }
            incoming = retained.toArray( new ParameterTypes[retained.size()] );
        }

        return retained;
    }

    private static int calcExactnessScore(final ParameterTypes callable, final IRubyObject[] args) {
        final Class[] types = callable.getParameterTypes();
        int count = 0;

        if ( callable.isVarArgs() ) {
            // varargs exactness gives the last N args as +1 since they'll already
            // have been determined to fit

            // dig out as many trailing args as possible that match varargs type
            final int nonVarargs = types.length - 1;

            count += 1; // add one for vararg

            // check remaining args
            for (int i = 0; i < nonVarargs && i < args.length; i++) {
                if ( types[i] == getJavaClass(args[i]) ) count++;
            }
        }
        else {
            for (int i = 0; i < args.length; i++) {
                if ( types[i] == getJavaClass(args[i]) ) count++;
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

    private static interface Matcher {
        public boolean match(final Class<?> type, final IRubyObject arg);
    }

    private static final Matcher EXACT = new Matcher() {
        public boolean match(final Class<?> type, final IRubyObject arg) {
            final Class<?> argClass = getJavaClass(arg);
            return type == argClass || (type.isPrimitive() && CodegenUtils.getBoxType(type) == argClass);
        }
        @Override public String toString() { return "EXACT"; } // for debugging
    };

    private static final Matcher ASSIGNABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return assignable(type, arg);
        }
        @Override public String toString() { return "ASSIGNABLE"; } // for debugging
    };

    private static final Matcher PRIMITIVABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return primitivable(type, arg);
        }
        @Override public String toString() { return "PRIMITIVABLE"; } // for debugging
    };

    private static final Matcher DUCKABLE = new Matcher() {
        public boolean match(Class type, IRubyObject arg) {
            return duckable(type, arg);
        }
        @Override public String toString() { return "DUCKABLE"; } // for debugging
    };

    //private static final Matcher[] MATCH_SEQUENCE = new Matcher[] { EXACT, PRIMITIVABLE, ASSIGNABLE, DUCKABLE };
    private static final Matcher[] NON_EXACT_MATCH_SEQUENCE = new Matcher[] { PRIMITIVABLE, ASSIGNABLE, DUCKABLE };

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
        if ( args.length == 0 ) return types.length <= 1;

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

    private static boolean assignable(Class<?> type, final IRubyObject arg) {
        return JavaClass.assignable(type, getJavaClass(arg));
    }

    /**
     * This method checks whether an argument can be *directly* converted into
     * the target primitive, i.e. without changing from integral to floating-point.
     *
     * @param type The target type
     * @param arg The argument to convert
     * @return Whether the argument can be directly converted to the target primitive type
     */
    private static boolean primitivable(final Class<?> type, final IRubyObject arg) {
        final Class<?> argClass = getJavaClass(arg);
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
            }
            if (type == Float.TYPE || type == Double.TYPE) {
                return argClass == double.class || // double first because it's what float claims to be
                       argClass == float.class ||
                       argClass == Float.class ||
                       argClass == Double.class;
            }
            if (type == Boolean.TYPE) {
                return argClass == boolean.class ||
                       argClass == Boolean.class;
            }
        }
        return false;
    }

    private static int calcTypePreference(Class<?> type, final IRubyObject arg) {
        final boolean primitive = type.isPrimitive();

        if ( primitive ) type = CodegenUtils.getBoxType(type);

        if ( Number.class.isAssignableFrom(type) || Character.class == type ) {
            if ( arg instanceof RubyFixnum ) {
                if ( type == Long.class ) return 10;
                if ( type == Integer.class ) return 8;
                if ( type == BigInteger.class ) return 7;
                if ( type == Short.class ) return 6;
                if ( type == Byte.class ) return 4;
                if ( type == Float.class ) return 3;
                if ( type == Double.class ) return 2;
                //if ( type == Character.class ) return 1;
                return 1;
            }
            if ( arg instanceof RubyBignum ) {
                if ( type == BigInteger.class ) return 10;
                if ( type == Long.class ) return 4;
                //if ( type == Integer.class ) return 6;
                //if ( type == Short.class ) return 5;
                //if ( type == Byte.class ) return 4;
                if ( type == Double.class ) return 6;
                if ( type == Float.class ) return 5;
                //if ( type == Character.class ) return 1;
                return 1;
            }
            if ( arg instanceof RubyInteger ) {
                if ( type == Long.class ) return 10;
                if ( type == Integer.class ) return 8;
                //if ( type == Short.class ) return 6;
                //if ( type == Byte.class ) return 4;
                if ( type == Float.class ) return 3;
                if ( type == Double.class ) return 2;
                //if ( type == Character.class ) return 1;
                return 1;
            }
            if ( arg instanceof RubyFloat ) {
                if ( type == Double.class ) return 10;
                if ( type == Float.class ) return 8;
                if ( type == BigDecimal.class ) return 6;
                if ( type == Long.class ) return 4;
                if ( type == Integer.class ) return 3;
                if ( type == Short.class ) return 2;
                //if ( type == Character.class ) return 1;
                return 1;
            }
        }
        else if ( arg instanceof RubyString ) {
            if ( type == String.class ) return 10;
            if ( type == byte[].class ) return 8;
            if ( CharSequence.class.isAssignableFrom(type) ) return 7;
            if ( type == Character.class ) return 1;
        }
        else if ( arg instanceof RubyBoolean ) {
            if ( type == Boolean.class ) return 10;
            //if ( type == Byte.class ) return 2;
            //if ( type == Character.class ) return 1;
        }

        return 0;
    }

    private static boolean duckable(final Class<?> type, final IRubyObject arg) {
        return JavaUtil.isDuckTypeConvertable(getJavaClass(arg), type);
    }

    private static int argsHashCode(IRubyObject a0) {
        return 31 + javaClassHashCode(a0);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1) {
        return 31 * argsHashCode(a0) + javaClassHashCode(a1);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2) {
        return 31 * argsHashCode(a0, a1) + javaClassHashCode(a2);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2, IRubyObject a3) {
        return 31 * argsHashCode(a0, a1, a2) + javaClassHashCode(a3);
    }

    private static int argsHashCode(IRubyObject[] args) {
        if ( args == null ) return 0;

        int result = 1;

        for ( int i = 0; i < args.length; i++ ) {
            result = 31 * result + javaClassHashCode(args[i]);
        }

        return result;
    }

    private static int javaClassHashCode(final IRubyObject arg) {
        return arg == null ? 0 : arg.getJavaClass().hashCode();
    }

    private static Class<?> getJavaClass(final IRubyObject arg) {
        return arg != null ? arg.getJavaClass() : void.class;
    }

}
