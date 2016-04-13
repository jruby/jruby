package org.jruby.java.invokers;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.collections.NonBlockingHashMapLong;

import static org.jruby.util.CodegenUtils.prettyParams;

public abstract class RubyToJavaInvoker<T extends JavaCallable> extends JavaMethod {
    // implements CallableCache<T> {

    static final NonBlockingHashMapLong NULL_CACHE = new NullHashMapLong();

    protected final T javaCallable; /* null if multiple callable members */
    protected final T[][] javaCallables; /* != null if javaCallable == null */
    protected final T[] javaVarargsCallables; /* != null if any var args callables */

    // in case multiple callables (overloaded Java method - same name different args)
    // for the invoker exists  CallableSelector caches resolution based on args here
    final NonBlockingHashMapLong<T> cache;

    private final Ruby runtime;

    @SuppressWarnings("unchecked") // NULL_CACHE
    RubyToJavaInvoker(RubyModule host, Member member) {
        super(host, Visibility.PUBLIC);
        this.runtime = host.getRuntime();

        final T callable;
        T[] varargsCallables = null;
        int minVarArgsArity = -1;

        callable = createCallable(runtime, member);
        int minArity = callable.getArity();
        if ( callable.isVarArgs() ) { // TODO does it need to happen?
            varargsCallables = createCallableArray(callable);
            minVarArgsArity = getMemberArity(member) - 1;
        }

        cache = NULL_CACHE; // if there's a single callable - matching (and thus the cache) won't be used

        this.javaCallable = callable;
        this.javaCallables = null;
        this.javaVarargsCallables = varargsCallables;

        setArity(minArity, minArity, minVarArgsArity);
        setupNativeCall();
    }

    @SuppressWarnings("unchecked") // NULL_CACHE
    RubyToJavaInvoker(RubyModule host, Member[] members) {
        super(host, Visibility.PUBLIC);
        this.runtime = host.getRuntime();

        // initialize all the callables for this method
        final T callable;
        final T[][] callables;
        T[] varargsCallables = null;
        int minVarArgsArity = -1; int maxArity, minArity;

        final int length = members.length;
        if ( length == 1 ) {
            callable = createCallable(runtime, members[0]);
            maxArity = minArity = callable.getArity();
            if ( callable.isVarArgs() ) {
                varargsCallables = createCallableArray(callable);
                minVarArgsArity = getMemberArity(members[0]) - 1;
            }
            callables = null;

            cache = NULL_CACHE; // if there's a single callable - matching (and thus the cache) won't be used
        }
        else {
            callable = null; maxArity = -1; minArity = Integer.MAX_VALUE;

            IntHashMap<ArrayList<T>> arityMap = new IntHashMap<ArrayList<T>>(length, 1);

            ArrayList<T> varArgs = null;
            for ( int i = 0; i < length; i++ ) {
                final Member method = members[i];
                final int currentArity = getMemberArity(method);
                maxArity = Math.max(currentArity, maxArity);
                minArity = Math.min(currentArity, minArity);

                final T javaMethod = createCallable(runtime, method);

                ArrayList<T> methodsForArity = arityMap.get(currentArity);
                if (methodsForArity == null) {
                    // most calls have 2-3 callables length (a.k.a. overrides)
                    // using capacity of length is a win-win here - will (likely)
                    // use small internal [len] + no resizing even in worst case
                    methodsForArity = new ArrayList<T>(length);
                    arityMap.put(currentArity, methodsForArity);
                }
                methodsForArity.add(javaMethod);

                if ( javaMethod.isVarArgs() ) {
                    final int usableArity = currentArity - 1;
                    // (String, Object...) has usable arity == 1 ... (String)
                    if ((methodsForArity = arityMap.get(usableArity)) == null) {
                        methodsForArity = new ArrayList<T>(length);
                        arityMap.put(usableArity, methodsForArity);
                    }
                    methodsForArity.add(javaMethod);

                    if (varArgs == null) varArgs = new ArrayList<T>(length);
                    varArgs.add(javaMethod);

                    if ( minVarArgsArity == -1 ) minVarArgsArity = Integer.MAX_VALUE;
                    minVarArgsArity = Math.min(usableArity, minVarArgsArity);
                }
            }

            callables = createCallableArrayArray(maxArity + 1);
            for (IntHashMap.Entry<ArrayList<T>> entry : arityMap.entrySet()) {
                ArrayList<T> methodsForArity = entry.getValue();

                T[] methodsArray = methodsForArity.toArray(createCallableArray(methodsForArity.size()));
                callables[ entry.getKey() /* int */ ] = methodsArray;
            }

            if (varArgs != null /* && varargsMethods.size() > 0 */) {
                varargsCallables = (T[]) varArgs.toArray( createCallableArray(varArgs.size()) );
            }
            // NOTE: tested (4, false); with opt_for_space: false but does not
            // seem to give  the promised ~10% improvement in map's speed ...
            cache = new NonBlockingHashMapLong<>(4, true); // 8 still uses MIN_SIZE_LOG == 4
        }

        this.javaCallable = callable;
        this.javaCallables = callables;
        this.javaVarargsCallables = varargsCallables;

        setArity(minArity, maxArity, minVarArgsArity);
        setupNativeCall();
    }

    private void setArity(final int minArity,  final int maxArity, final int minVarArgsArity) {
        if ( minVarArgsArity == -1 ) { // no var-args
            if ( minArity == maxArity ) {
                setArity( Arity.fixed(minArity) );
            }
            else { // multiple overloads
                setArity(Arity.required(minArity)); // but <= maxArity
            }
        }
        else {
            setArity( Arity.required(minVarArgsArity < minArity ? minVarArgsArity : minArity) );
        }
    }

    final void setupNativeCall() { // if it's not overloaded, set up a NativeCall
        if (javaCallable != null) {
            // no constructor support yet
            if (javaCallable instanceof org.jruby.javasupport.JavaMethod) {
                setNativeCallIfPublic(((org.jruby.javasupport.JavaMethod) javaCallable).getValue());
            }
        } else { // use the lowest-arity non-overload
            for ( int i = 0; i< javaCallables.length; i++ ) {
                final JavaCallable[] callablesForArity = javaCallables[i];
                if ( callablesForArity == null || callablesForArity.length != 1 ) continue;
                if ( callablesForArity[0] instanceof org.jruby.javasupport.JavaMethod ) {
                    final Method method = ((org.jruby.javasupport.JavaMethod) callablesForArity[0]).getValue();
                    if ( setNativeCallIfPublic( method ) ) break;
                }
            }
        }
    }

    private boolean setNativeCallIfPublic(final Method method) {
        final int mod = method.getModifiers(); // only public, since non-public don't bind
        if ( Modifier.isPublic(mod) && Modifier.isPublic( method.getDeclaringClass().getModifiers() ) ) {
            setNativeCall(method.getDeclaringClass(), method.getName(), method.getReturnType(), method.getParameterTypes(), Modifier.isStatic(mod), true);
            return true;
        }
        return false;
    }

    /**
     * Internal API
     * @param signatureCode
     * @return callable
     */
    public final T getSignature(int signatureCode) {
        return cache.get(signatureCode);
    }

    /**
     * Internal API
     * @param signatureCode
     * @param callable
     */
    public final void putSignature(int signatureCode, T callable) {
        cache.put(signatureCode, callable);
    }

    protected abstract T createCallable(Ruby runtime, Member member);

    protected abstract T[] createCallableArray(T callable);

    protected abstract T[] createCallableArray(int size);

    protected abstract T[][] createCallableArrayArray(int size);

    protected abstract Class[] getMemberParameterTypes(Member member);

    @Deprecated // no longer used!
    protected abstract boolean isMemberVarArgs(Member member);

    final int getMemberArity(Member member) {
        return getMemberParameterTypes(member).length;
    }

    public static Object[] convertArguments(final ParameterTypes method, final IRubyObject[] args) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Object[] javaArgs; final int len = args.length;

        if ( method.isVarArgs() ) {
            final int last = paramTypes.length - 1;
            javaArgs = new Object[ last + 1 ];
            for ( int i = 0; i < last; i++ ) {
                javaArgs[i] = args[i].toJava(paramTypes[i]);
            }
            javaArgs[ last ] = convertVarArgumentsOnly(paramTypes[ last ], last, args);
        }
        else {
            javaArgs = new Object[len];
            for ( int i = 0; i < len; i++ ) {
                javaArgs[i] = args[i].toJava(paramTypes[i]);
            }
        }
        return javaArgs;
    }

    private static Object convertVarArgumentsOnly(final Class<?> varArrayType,
        final int varStart, final IRubyObject[] args) {
        final int varCount = args.length - varStart;

        if ( args.length == 0 || varCount <= 0 ) {
            return Array.newInstance(varArrayType.getComponentType(), 0);
        }

        if ( varCount == 1 && args[varStart] instanceof ArrayJavaProxy ) {
            // we may have a pre-created array to pass; try that first
            return args[varStart].toJava(varArrayType);
        }

        final Class<?> compType = varArrayType.getComponentType();
        final Object varArgs = Array.newInstance(compType, varCount);
        for ( int i = 0; i < varCount; i++ ) {
            Array.set(varArgs, i, args[varStart + i].toJava(compType));
        }
        return varArgs;
    }

    static JavaProxy castJavaProxy(final IRubyObject self) {
        assert self instanceof JavaProxy : "Java methods can only be invoked on Java objects";
        return (JavaProxy) self;
    }

    static <T extends AccessibleObject> T setAccessible(T accessible) {
        if ( ! Ruby.isSecurityRestricted() ) {
            try { accessible.setAccessible(true); }
            catch (SecurityException e) {}
        }
        return accessible;
    }

    static <T extends AccessibleObject> T[] setAccessible(T[] accessibles) {
        if ( ! Ruby.isSecurityRestricted() ) {
            try { AccessibleObject.setAccessible(accessibles, true); }
            catch (SecurityException e) {}
        }
        return accessibles;
    }

    protected T findCallable(IRubyObject self, String name, IRubyObject[] args, final int arity) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            final T[] callablesForArity;
            if ( arity >= javaCallables.length || (callablesForArity = javaCallables[arity]) == null ) {
                if ( ( callable = matchVarArgsCallableArityN(self, args) ) == null ) {
                    throw runtime.newArgumentError(args.length, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityN(runtime, this, callablesForArity, args);
            if ( callable == null ) {
                if ( ( callable = matchVarArgsCallableArityN(self, args) ) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity, args);
                }
            }
        }
        else {
            if ( ! callable.isVarArgs() ) checkCallableArity(callable, args.length);
        }
        return callable;
    }

    private T matchVarArgsCallableArityN(IRubyObject self, IRubyObject[] args) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityN(runtime, this, varArgsCallables, args);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, args);
            }
            return callable;
        }
        return null;
    }

    protected final T findCallableArityZero(IRubyObject self, String name) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length == 0 || (callablesForArity = javaCallables[0]) == null ) {
                throw newErrorDueNoMatchingCallable(self, name);
            }
            callable = callablesForArity[0];
        }
        else {
            checkCallableArity(callable, 0);
        }
        return callable;
    }

    protected final T findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 1 || (callablesForArity = javaCallables[1]) == null ) {
                throw runtime.newArgumentError(1, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityOne(runtime, this, callablesForArity, arg0);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0);
            }
        }
        else {
            checkCallableArity(callable, 1);
        }
        return callable;
    }

    protected final T findCallableArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 2 || (callablesForArity = javaCallables[2]) == null ) {
                throw runtime.newArgumentError(2, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityTwo(runtime, this, callablesForArity, arg0, arg1);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1);
            }
        }
        else {
            checkCallableArity(callable, 2);
        }
        return callable;
    }

    protected final T findCallableArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 3 || (callablesForArity = javaCallables[3]) == null ) {
                throw runtime.newArgumentError(3, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityThree(runtime, this, callablesForArity, arg0, arg1, arg2);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2);
            }
        }
        else {
            checkCallableArity(callable, 3);
        }
        return callable;
    }

    protected final T findCallableArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 4 || (callablesForArity = javaCallables[4]) == null ) {
                throw runtime.newArgumentError(4, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityFour(runtime, this, callablesForArity, arg0, arg1, arg2, arg3);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2, arg3);
            }
        }
        else {
            checkCallableArity(callable, 4);
        }
        return callable;
    }

    private void checkCallableArity(final T callable, final int expected) {
        final int arity = callable.getArity();
        if ( arity != expected ) throw runtime.newArgumentError(expected, arity);
    }

    private T someCallable() {
        if ( javaCallable == null ) {
            for ( int i = 0; i < javaCallables.length; i++ ) {
                T[] callables = javaCallables[i];
                if ( callables != null && callables.length > 0 ) {
                    for ( int j = 0; j < callables.length; j++ ) {
                        if ( callables[j] != null ) return callables[j];
                    }
                }
            }
            return null; // not expected to happen ...
        }
        return javaCallable;
    }

    private boolean isConstructor() {
        return someCallable() instanceof JavaConstructor;
    }

    RaiseException newErrorDueArgumentTypeMismatch(final IRubyObject receiver,
        final T[] methods, IRubyObject... args) {

        final Class[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = getClass( args[i] );
        }

        final StringBuilder error = new StringBuilder(64);

        error.append("no ");
        if ( isConstructor() ) error.append("constructor");
        else {
            org.jruby.javasupport.JavaMethod method = (org.jruby.javasupport.JavaMethod) methods[0];
            error.append("method '").append( method.getValue().getName() ).append("'");
        }
        error.append(" for arguments ");
        prettyParams(error, argTypes);
        error.append(" on ").append( formatReceiver(receiver) );

        if ( methods.length > 1 ) {
            error.append("\n  available overloads:");
            for (ParameterTypes method : methods) {
                Class<?>[] paramTypes = method.getParameterTypes();
                error.append("\n    "); prettyParams( error, paramTypes );
            }
        }

        // TODO should have been ArgumentError - might break users to refactor at this point
        return runtime.newNameError(error.toString(), null);
    }

    private RaiseException newErrorDueNoMatchingCallable(final IRubyObject receiver, final String name) {
        final StringBuilder error = new StringBuilder(48);

        error.append("no ");
        if ( isConstructor() ) error.append("constructor");
        else {
            error.append("method '").append( name ).append("'");
        }
        error.append(" (for zero arguments) on ").append( formatReceiver(receiver) );
        return runtime.newArgumentError( error.toString() );
    }

    private static Class<?> getClass(final IRubyObject object) {
        if (object == null) return void.class;

        if (object instanceof ConcreteJavaProxy) {
            return ((ConcreteJavaProxy) object).getJavaClass();
        }
        return object.getClass();
    }

    private static String formatReceiver(final IRubyObject object) {
        if ( object instanceof RubyModule ) {
            return ((RubyModule) object).getName();
        }
        return object.getMetaClass().getRealClass().getName();
    }

    private static class NullHashMapLong<V> extends NonBlockingHashMapLong<V> {

        NullHashMapLong() { super(0, false); }

        @Override
        public V put( long key, V val) { return null; }

        @Override
        public V putIfAbsent( long key, V val ) { return null; }

        // public final V get( long key )

        @Override
        public V get(Object key) { return null; }

    }

}
