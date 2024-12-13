package org.jruby.java.dispatch;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.java.invokers.RubyToJavaInvoker;
import org.jruby.java.util.ClassUtils;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntHashMap;

import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.util.CodegenUtils.getBoxType;
import static org.jruby.util.CodegenUtils.prettyParams;
import static org.jruby.javasupport.Java.getFunctionalInterfaceMethod;

/**
 * Method selection logic for calling from Ruby to Java.
 */
public class CallableSelector {

    private CallableSelector() { /* no-instances */ }

    //private static final boolean DEBUG = true;

    public static <T extends ParameterTypes> T matchingCallableArityN(Ruby runtime, CallableCache<T> cache, T[] methods, IRubyObject[] args) {
        final int signatureCode = argsHashCode(args);
        T method = cache.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, args);
            if (method != null) cache.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends ParameterTypes> T matchingCallableArityOne(Ruby runtime, CallableCache<T> cache, T[] methods, IRubyObject arg0) {
        final int signatureCode = argsHashCode(arg0);
        T method = cache.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0);
            if (method != null) cache.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends ParameterTypes> T matchingCallableArityTwo(Ruby runtime, CallableCache<T> cache, T[] methods, IRubyObject arg0, IRubyObject arg1) {
        final int signatureCode = argsHashCode(arg0, arg1);
        T method = cache.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1);
            if (method != null) cache.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends ParameterTypes> T matchingCallableArityThree(Ruby runtime, CallableCache<T> cache, T[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2);
        T method = cache.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2);
            if (method != null) cache.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends ParameterTypes> T matchingCallableArityFour(Ruby runtime, CallableCache<T> cache, T[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        T method = cache.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2, arg3);
            if (method != null) cache.putSignature(signatureCode, method);
        }
        return method;
    }

    // RubyToJavaInvoker

    public static <T extends JavaCallable> T matchingCallableArityN(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods, IRubyObject[] args) {
        final int signatureCode = argsHashCode(args);
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, args);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends JavaCallable> T matchingCallableArityZero(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods) {
        final int signatureCode = 0;
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends JavaCallable> T matchingCallableArityOne(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods, IRubyObject arg0) {
        final int signatureCode = argsHashCode(arg0);
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends JavaCallable> T matchingCallableArityTwo(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods, IRubyObject arg0, IRubyObject arg1) {
        final int signatureCode = argsHashCode(arg0, arg1);
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends JavaCallable> T matchingCallableArityThree(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2);
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    public static <T extends JavaCallable> T matchingCallableArityFour(Ruby runtime, RubyToJavaInvoker<T> invoker, T[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        T method = invoker.getSignature(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2, arg3);
            if (method != null) invoker.putSignature(signatureCode, method);
        }
        return method;
    }

    private static <T extends ParameterTypes> T findMatchingCallableForArgs(final Ruby runtime,
        final T[] methods, final IRubyObject... args) {
        T method = null;

        // try the new way first
        final List<T> candidates = findCallableCandidates(methods, args);
        final int size = candidates.size();

        if ( size > 0 ) {
            // new way found one, so let's go with that
            if ( size == 1 ) method = candidates.get(0);
            else { // narrow to most specific version (or first version, if none are more specific)
                T mostSpecific = candidates.get(0);
                Class<?>[] msTypes = mostSpecific.getParameterTypes();
                boolean ambiguous = false;

                final IRubyObject lastArg = args.length > 0 ? args[ args.length - 1 ] : null;
                int mostSpecificArity = Integer.MIN_VALUE;

                final int procArity;
                if ( lastArg instanceof RubyProc ) {
                    final Method implMethod; final int last = msTypes.length - 1;
                    if ( last >= 0 && msTypes[last].isInterface() &&
                        ( implMethod = getFunctionalInterfaceMethod(msTypes[last]) ) != null ) {
                        mostSpecificArity = implMethod.getParameterTypes().length;
                    }
                    procArity = procArityValue(lastArg);
                }
                else {
                    procArity = Integer.MIN_VALUE;
                }

                /* OUTER: */
                for ( int c = 1; c < size; c++ ) {
                    final T candidate = candidates.get(c);
                    final Class<?>[] cTypes = candidate.getParameterTypes();

                    // TODO still need to handle var-args better Class<?> lastType;

                    final boolean lastArgProc = procArity != Integer.MIN_VALUE;
                    final Boolean moreSpecific = moreSpecificTypes(msTypes, cTypes, lastArgProc);
                    if ( moreSpecific == Boolean.TRUE ) {
                        mostSpecific = candidate; msTypes = cTypes;
                        ambiguous = false; continue /* OUTER */;
                    }
                    else { // if ( (Object) moreSpecific == Boolean.FALSE ) {
                        // none more specific; check for ambiguities
                        for ( int i = 0; i < msTypes.length; i++ ) {
                            // TODO if lastArgProc (and we're not dealing with RubyProc.class)
                            // then comparing last arg should not be needed, right?
                            // ... same applies for moreSpecificTypes method ...
                            final Class<?> msType = msTypes[i], cType = cTypes[i];
                            if ( msType == cType || msType.isAssignableFrom(cType) || cType.isAssignableFrom(msType) ) {
                                ambiguous = false; break; // continue OUTER;
                            }
                            else if ( cType.isPrimitive() && msType.isAssignableFrom(getBoxType(cType)) ) {
                                ambiguous = false; break; // continue OUTER;
                            }
                            else {
                                ambiguous = true;
                            }
                        }
                    }

                    // special handling if we're dealing with Proc#impl :
                    if ( lastArgProc ) {  // lastArg instanceof RubyProc
                        // cases such as (both ifaces - differ in arg count) :
                        // java.io.File#listFiles(java.io.FileFilter) ... accept(File)
                        // java.io.File#listFiles(java.io.FilenameFilter) ... accept(File, String)
                        final Method implMethod; final int last = cTypes.length - 1;
                        if ( last >= 0 && cTypes[last].isInterface() && ( implMethod = getFunctionalInterfaceMethod(cTypes[last]) ) != null ) {
                            // we're sure to have an interface in the end - match arg count :
                            // NOTE: implMethod.getParameterCount() on Java 8 would do ...
                            final int methodArity = implMethod.getParameterTypes().length;
                            if ( methodArity == procArity ) {
                                if ( mostSpecificArity == methodArity ) ambiguous = true; // 2 with same arity
                                // TODO we could try to match parameter types with arg types
                                else {
                                    mostSpecific = candidate; msTypes = cTypes;
                                    mostSpecificArity = procArity; ambiguous = false;
                                }
                                continue; /* OUTER */ // we want to check all
                            }
                            else if ( mostSpecificArity != procArity ) {
                                if ( methodArity < procArity ) { // not ideal but still usable
                                    if ( mostSpecificArity == methodArity ) ambiguous = true; // 2 with same arity
                                    else if ( mostSpecificArity < methodArity ) { // candidate is "better" match
                                        mostSpecific = candidate; msTypes = cTypes;
                                        mostSpecificArity = methodArity; ambiguous = false;
                                    }
                                    continue; /* OUTER */
                                }
                                else if ( procArity < 0 && methodArity >= -(procArity + 1) ) { // *splat that fits
                                    if ( mostSpecificArity == methodArity ) ambiguous = true; // 2 with same arity
                                    else {
                                        final int msa = mostSpecificArity + procArity;
                                        final int ma = methodArity + procArity;
                                        if ( ( msa < 0 && ma < 0 && msa < ma ) ||
                                             ( msa >= 0 && ma >= 0 && msa > ma ) ||
                                             ( msa > ma ) ) {
                                            mostSpecific = candidate; msTypes = cTypes;
                                            mostSpecificArity = methodArity; // ambiguous = false;
                                        }
                                        ambiguous = false;
                                    }
                                    continue; /* OUTER */
                                }
                            }
                            else { // we're not a match and if there's something else matched than it's not really ambiguous
                                ambiguous = false; /* continue; /* OUTER */
                            }
                        }
                    }

                    // somehow we can still decide e.g. if we got a RubyFixnum
                    // then (int) constructor should be preferred over (float)
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
                    var context = runtime.getCurrentContext();

                    if (Options.JI_AMBIGUOUS_CALLS_DEBUG.load()) {
                        runtimeError(context,
                                "multiple Java methods found, dumping backtrace and choosing "
                                        + ((Member) ((JavaCallable) method).accessibleObject()).getName()
                                        + prettyParams(msTypes)
                        ).printStackTrace(runtime.getErr());
                    } else {
                        warn(context,
                                "multiple Java methods found, use -X" + Options.JI_AMBIGUOUS_CALLS_DEBUG.propertyName() + " for backtrace. Choosing "
                                        + ((Member) ((JavaCallable) method).accessibleObject()).getName()
                                        + prettyParams(msTypes));
                    }
                }
            }
        }

        // fall back on old ways
        if (method == null) {
            method = findMatchingCallableForArgsFallback(runtime, methods, args);
        }

        return method;
    }

    private static Boolean moreSpecificTypes(final Class[] msTypes, final Class[] cTypes,
        final boolean lastArgProc) {

        final int last = msTypes.length - 1;
        int moreSpecific = 0; Class<?> msType, cType;
        for ( int i = 0; i < last; i++ ) {
             msType = msTypes[i]; cType = cTypes[i];
            if ( msType == cType ) ;
            else if ( msType.isAssignableFrom(cType) ) {
                moreSpecific++; /* continue; */
            }
            else if ( cType.isAssignableFrom(msType) ) {
                moreSpecific--; /* continue; */
            }
            /* return false; */
        }

        if ( last >= 0 ) { // last argument :
            msType = msTypes[last]; cType = cTypes[last];
            if ( lastArgProc ) {
                if ( cType.isAssignableFrom(RubyProc.class) ) {
                    if ( ! msType.isAssignableFrom(RubyProc.class) ) moreSpecific++;
                    // return moreSpecific > 0;
                }
                else {
                    // NOTE: maybe this needs some implMethod arity matching here?
                    return null; // interface matching logic (can not decide)
                }
            }
            else {
                if ( msType == cType ) ;
                else if ( msType.isAssignableFrom(cType) ) {
                    moreSpecific++;
                }
                else if ( cType.isAssignableFrom(msType) ) {
                    moreSpecific--;
                }
            }
        }
        return moreSpecific > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    private static <T extends ParameterTypes> T findMatchingCallableForArgsFallback(final Ruby runtime,
        final T[] methods, final IRubyObject... args) {
        T method = findCallable(methods, Exact, args);
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
        return method;
    }

    private static <T extends ParameterTypes> T findCallable(T[] callables, CallableAcceptor acceptor, IRubyObject[] args) {
        T bestCallable = null;
        int bestScore = -1;
        for ( int i = 0; i < callables.length; i++ ) {
            final T callable = callables[i];

            if ( acceptor.accept(callable, args) ) {
                int currentScore = calcExactnessScore(callable, args);
                if (currentScore > bestScore) {
                    bestCallable = callable; bestScore = currentScore;
                }
            }
        }
        return bestCallable;
    }

    @SuppressWarnings("unchecked")
    private static <T extends ParameterTypes> List<T> findCallableCandidates(final T[] callables,
        final IRubyObject[] args) {
        // in case of an exact match prefer to return it early :
        for ( int c = 0; c < callables.length; c++ ) {
            final T callable = callables[c];
            if ( exactMatch(callable, args ) ) return Collections.singletonList(callable);
        }

        final ArrayList<T> retained = new ArrayList<T>(callables.length);
        ParameterTypes[] incoming = callables.clone();

        for ( int i = 0; i < args.length; i++ ) {
            retained.clear(); // non-exact match sequence :
            // PRIMITIVABLE :
            for ( int c = 0; c < incoming.length; c++ ) {
                ParameterTypes callable = incoming[c];
                if ( callable == null ) continue; // removed (matched)

                Class[] types = callable.getParameterTypes();

                if ( PRIMITIVABLE.match( types[i], args[i] ) ) {
                    retained.add((T) callable);
                    incoming[c] = null; // retaining - remove
                }
            }
            // ASSIGNABLE :
            for ( int c = 0; c < incoming.length; c++ ) {
                ParameterTypes callable = incoming[c];
                if ( callable == null ) continue; // removed (matched)

                Class[] types = callable.getParameterTypes();

                if ( ASSIGNABLE.match( types[i], args[i] ) ) {
                    retained.add((T) callable);
                    incoming[c] = null; // retaining - remove
                }
            }
            if ( retained.isEmpty() ) {
                // DUCKABLE :
                for ( int c = 0; c < incoming.length; c++ ) {
                    ParameterTypes callable = incoming[c];
                    if ( callable == null ) continue; // removed (matched)

                    Class[] types = callable.getParameterTypes();

                    if ( DUCKABLE.match( types[i], args[i] ) ) {
                        retained.add((T) callable);
                        //incoming[c] = null; // retaining - remove
                    }
                }
            }
            incoming = retained.toArray( new ParameterTypes[retained.size()] );
        }

        // final step rule out argument length mismatch :
        int j = 0; for ( int i = 0; i < retained.size(); i++ ) {
            T callable = retained.get(i);
            if ( callable.isVarArgs() ) {
                if ( callable.getArity() > args.length - 1 ) continue;
            }
            else {
                if ( callable.getArity() != args.length ) continue;
            }
            retained.set(j++, callable);
        }

        return j < retained.size() ? retained.subList(0, j) : retained;
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
            return type == argClass || (type.isPrimitive() && getBoxType(type) == argClass);
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

    private static boolean exactMatch(ParameterTypes paramTypes, IRubyObject... args) {
        final Class[] types = paramTypes.getParameterTypes();

        if (args.length != types.length) return false;

        for (int i = 0; i < types.length; i++) {
            if (!EXACT.match(types[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableAndPrimitivable(ParameterTypes paramTypes, IRubyObject... args) {
        final Class[] types = paramTypes.getParameterTypes();

        if (args.length != types.length) return false;

        for (int i = 0; i < types.length; i++) {
            if (!(ASSIGNABLE.match(types[i], args[i]) && PRIMITIVABLE.match(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableOrDuckable(ParameterTypes paramTypes, IRubyObject... args) {
        final Class[] types = paramTypes.getParameterTypes();

        if (args.length != types.length) return false;

        for (int i = 0; i < types.length; i++) {
            if (!(ASSIGNABLE.match(types[i], args[i]) || DUCKABLE.match(types[i], args[i]))) {
                return false;
            }
        }
        return true;
    }

    private static boolean assignableAndPrimitivableWithVarargs(ParameterTypes paramTypes, IRubyObject... args) {
        if ( ! paramTypes.isVarArgs() ) return false; // bail out if this is not a varargs method

        final Class[] types = paramTypes.getParameterTypes();

        // if there's no args, we only match when there's just varargs
        if ( args.length == 0 ) return types.length <= 1;

        final int last = types.length - 1;

        if ( args.length < last ) return false; // can't match - paramTypes method is not usable!
        for ( int i = 0; i < last; i++ ) { // first check non-vararg argument types match
            if (!(ASSIGNABLE.match(types[i], args[i]) || PRIMITIVABLE.match(types[i], args[i]))) {
                return false;
            }
        }

        final Class varArgType = types[last].getComponentType();
        // dig out as many trailing args as will fit, ensuring they match varargs type
        for ( int i = last; i < args.length; i++ ) {
            if (!(ASSIGNABLE.match(varArgType, args[i]) || PRIMITIVABLE.match(varArgType, args[i]))) {
                return false;
            }
        }

        return true;
    }

    private static boolean assignable(Class<?> type, final IRubyObject arg) {
        return ClassUtils.assignable(type, getJavaClass(arg)) ||
                // handle 'native' signatures e.g. method with a (org.jruby.RubyArray arg)
                ( arg != null && type.isAssignableFrom(arg.getClass()) );
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

        if ( primitive ) type = getBoxType(type);

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
        else if ( arg instanceof RubySymbol ) {
            if ( type == String.class ) return 10;
            if ( CharSequence.class.isAssignableFrom(type) ) return 7;
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
        return 31 + javaClassOrProcHashCode(a0);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1) {
        return 17 * ( 31 + javaClassHashCode(a0) ) +
                javaClassOrProcHashCode(a1);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2) {
        return 17 * ( 17 * ( 31 + javaClassHashCode(a0) ) + javaClassHashCode(a1) ) +
                javaClassOrProcHashCode(a2);
    }

    private static int argsHashCode(IRubyObject a0, IRubyObject a1, IRubyObject a2, IRubyObject a3) {
        return 17 * ( 17 * ( 17 * ( 31 + javaClassHashCode(a0) ) + javaClassHashCode(a1) ) + javaClassHashCode(a2) ) +
                javaClassOrProcHashCode(a3);
    }

    private static int argsHashCode(final IRubyObject[] args) {
        final int last = args.length - 1;
        if ( last == -1 ) return 0;

        int result = 31;
        for ( int i = 0; i < last; i++ ) {
            result = 17 * ( result + javaClassHashCode( args[i] ) );
        }

        return result + javaClassOrProcHashCode( args[last] );
    }

    private static int javaClassHashCode(final IRubyObject arg) {
        return arg.getJavaClass().hashCode();
    }

    private static int javaClassOrProcHashCode(final IRubyObject arg) {
        final Class<?> javaClass = arg.getJavaClass();
        return javaClass == RubyProc.class ? 11 * procArityValue(arg) : javaClass.hashCode();
    }

    private static int procArityValue(final IRubyObject proc) {
        return ((RubyProc) proc).getBlock().getSignature().arityValue();
    }

    private static Class<?> getJavaClass(final IRubyObject arg) {
        return arg != null ? arg.getJavaClass() : void.class;
    }

    /**
     * A cache of "callables" based on method signature hash.
     * @param <T> java callable type
     */
    public static interface CallableCache<T extends ParameterTypes> {

        T getSignature(int signatureCode) ;
        void putSignature(int signatureCode, T callable) ;

    }

    /**
     * Internal helper to allocate a callable map to cache argument method matches.
     * @param <T> the callable type
     * @return cache usable with {@link CallableSelector}
     */
    @Deprecated
    public static <T extends ParameterTypes> IntHashMap<T> newCallableCache() {
        return new IntHashMap<T>(8);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static ParameterTypes matchingCallableArityN(Ruby runtime, Map cache, ParameterTypes[] methods, IRubyObject[] args) {
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
    @SuppressWarnings("unchecked")
    @Deprecated
    public static JavaCallable matchingCallableArityN(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject[] args) {
        final int signatureCode = argsHashCode(args);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, args);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    @Deprecated
    public static JavaCallable matchingCallableArityOne(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0) {
        final int signatureCode = argsHashCode(arg0);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    @Deprecated
    public static JavaCallable matchingCallableArityTwo(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1) {
        final int signatureCode = argsHashCode(arg0, arg1);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    @Deprecated
    public static JavaCallable matchingCallableArityThree(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

    @Deprecated
    public static JavaCallable matchingCallableArityFour(Ruby runtime, Map cache, JavaCallable[] methods, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final int signatureCode = argsHashCode(arg0, arg1, arg2, arg3);
        JavaCallable method = (JavaCallable) cache.get(signatureCode);
        if (method == null) {
            method = findMatchingCallableForArgs(runtime, methods, arg0, arg1, arg2, arg3);
            if (method != null) cache.put(signatureCode, method);
        }
        return method;
    }

}
