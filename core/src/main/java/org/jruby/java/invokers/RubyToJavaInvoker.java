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
import org.jruby.internal.runtime.methods.CallConfiguration;
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
import static org.jruby.java.dispatch.CallableSelector.newCallableCache;
import static org.jruby.util.CodegenUtils.prettyParams;

public abstract class RubyToJavaInvoker extends JavaMethod {

    static final IntHashMap<JavaCallable> NULL_CACHE = IntHashMap.nullMap();

    protected final JavaCallable javaCallable; /* null if multiple callable members */
    protected final JavaCallable[][] javaCallables; /* != null if javaCallable == null */
    protected final JavaCallable[] javaVarargsCallables; /* != null if any var args callables */
    protected final int minVarargsArity;

    // in case multiple callables (overloaded Java method - same name different args)
    // for the invoker exists  CallableSelector caches resolution based on args here
    final IntHashMap<JavaCallable> cache;

    private final Ruby runtime;
    private final Member[] members;

    RubyToJavaInvoker(RubyModule host, Member[] members) {
        super(host, Visibility.PUBLIC, CallConfiguration.FrameNoneScopeNone);
        this.members = members;
        this.runtime = host.getRuntime();
        // we set all Java methods to optional, since many/most have overloads
        setArity(Arity.OPTIONAL);

        // initialize all the callables for this method
        final JavaCallable callable;
        final JavaCallable[][] callables;
        JavaCallable[] varargsCallables = null;
        int varArgsArity = Integer.MAX_VALUE;

        final int length = members.length;
        if ( length == 1 ) {
            callable = createCallable(runtime, members[0]);
            if ( callable.isVarArgs() ) {
                varargsCallables = createCallableArray(callable);
            }
            callables = null;

            cache = NULL_CACHE; // if there's a single callable - matching (and thus the cache) won't be used
        }
        else {
            callable = null;

            IntHashMap<ArrayList<JavaCallable>> arityMap = new IntHashMap<ArrayList<JavaCallable>>(length, 1);

            ArrayList<JavaCallable> varArgs = null; int maxArity = 0;
            for ( int i = 0; i < length; i++ ) {
                final Member method = members[i];
                final int currentArity = getMemberParameterTypes(method).length;
                maxArity = Math.max(currentArity, maxArity);

                ArrayList<JavaCallable> methodsForArity = arityMap.get(currentArity);
                if (methodsForArity == null) {
                    // most calls have 2-3 callables length (a.k.a. overrides)
                    // using capacity of length is a win-win here - will (likely)
                    // use small internal [len] + no resizing even in worst case
                    methodsForArity = new ArrayList<JavaCallable>(length);
                    arityMap.put(currentArity, methodsForArity);
                }

                final JavaCallable javaMethod = createCallable(runtime, method);
                methodsForArity.add(javaMethod);

                if ( isMemberVarArgs(method) ) {
                    varArgsArity = Math.min(currentArity - 1, varArgsArity);
                    if (varArgs == null) varArgs = new ArrayList<JavaCallable>(length);
                    varArgs.add(javaMethod);
                }
            }

            callables = createCallableArrayArray(maxArity + 1);
            for (IntHashMap.Entry<ArrayList<JavaCallable>> entry : arityMap.entrySet()) {
                ArrayList<JavaCallable> methodsForArity = entry.getValue();

                JavaCallable[] methodsArray = methodsForArity.toArray(createCallableArray(methodsForArity.size()));
                callables[ entry.getKey() /* int */ ] = methodsArray;
            }

            if (varArgs != null /* && varargsMethods.size() > 0 */) {
                varargsCallables = varArgs.toArray( createCallableArray(varArgs.size()) );
            }

            cache = newCallableCache();
        }

        this.javaCallable = callable;
        this.javaCallables = callables;
        this.javaVarargsCallables = varargsCallables;
        this.minVarargsArity = varArgsArity;

        // if it's not overloaded, set up a NativeCall
        if (javaCallable != null) {
            // no constructor support yet
            if (javaCallable instanceof org.jruby.javasupport.JavaMethod) {
                setNativeCallIfPublic( ((org.jruby.javasupport.JavaMethod) javaCallable).getValue() );
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

    protected final Member[] getMembers() {
        return members;
    }

    protected AccessibleObject[] getAccessibleObjects() {
        return (AccessibleObject[]) getMembers();
    }

    protected abstract JavaCallable createCallable(Ruby runtime, Member member);

    protected abstract JavaCallable[] createCallableArray(JavaCallable callable);

    protected abstract JavaCallable[] createCallableArray(int size);

    protected abstract JavaCallable[][] createCallableArrayArray(int size);

    protected abstract Class[] getMemberParameterTypes(Member member);

    protected abstract boolean isMemberVarArgs(Member member);

    //final int getMemberArity(Member member) {
    //    return getMemberParameterTypes(member).length;
    //}

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

    static void trySetAccessible(AccessibleObject... accesibles) {
        if ( ! Ruby.isSecurityRestricted() ) {
            try { AccessibleObject.setAccessible(accesibles, true); }
            catch(SecurityException e) {}
        }
    }

    protected JavaCallable findCallable(IRubyObject self, String name, IRubyObject[] args, final int arity) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            final JavaCallable[] callablesForArity;
            if ( arity >= javaCallables.length || (callablesForArity = javaCallables[arity]) == null ) {
                if ( ( callable = matchVarArgsCallableArityN(self, args) ) == null ) {
                    throw runtime.newArgumentError(args.length, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityN(runtime, cache, callablesForArity, args);
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

    private JavaCallable matchVarArgsCallableArityN(IRubyObject self, IRubyObject[] args) {
        final JavaCallable[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            JavaCallable callable = CallableSelector.matchingCallableArityN(runtime, cache, varArgsCallables, args);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, args);
            }
            return callable;
        }
        return null;
    }

    protected final JavaCallable findCallableArityZero(IRubyObject self, String name) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final JavaCallable[] callablesForArity;
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

    protected final JavaCallable findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final JavaCallable[] callablesForArity;
            if ( javaCallables.length <= 1 || (callablesForArity = javaCallables[1]) == null ) {
                throw runtime.newArgumentError(1, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityOne(runtime, cache, callablesForArity, arg0);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0);
            }
        }
        else {
            checkCallableArity(callable, 1);
        }
        return callable;
    }

    protected final JavaCallable findCallableArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final JavaCallable[] callablesForArity;
            if ( javaCallables.length <= 2 || (callablesForArity = javaCallables[2]) == null ) {
                throw runtime.newArgumentError(2, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityTwo(runtime, cache, callablesForArity, arg0, arg1);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1);
            }
        }
        else {
            checkCallableArity(callable, 2);
        }
        return callable;
    }

    protected final JavaCallable findCallableArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final JavaCallable[] callablesForArity;
            if ( javaCallables.length <= 3 || (callablesForArity = javaCallables[3]) == null ) {
                throw runtime.newArgumentError(3, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityThree(runtime, cache, callablesForArity, arg0, arg1, arg2);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2);
            }
        }
        else {
            checkCallableArity(callable, 3);
        }
        return callable;
    }

    protected final JavaCallable findCallableArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        JavaCallable callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final JavaCallable[] callablesForArity;
            if ( javaCallables.length <= 4 || (callablesForArity = javaCallables[4]) == null ) {
                throw runtime.newArgumentError(4, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityFour(runtime, cache, callablesForArity, arg0, arg1, arg2, arg3);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2, arg3);
            }
        }
        else {
            checkCallableArity(callable, 4);
        }
        return callable;
    }

    private void checkCallableArity(final JavaCallable callable, final int expected) {
        final int arity = callable.getArity();
        if ( arity != expected ) throw runtime.newArgumentError(expected, arity);
    }

    private JavaCallable someCallable() {
        if ( javaCallable == null ) {
            for ( int i = 0; i < javaCallables.length; i++ ) {
                JavaCallable[] callables = javaCallables[i];
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
        final JavaCallable[] methods, IRubyObject... args) {

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

}