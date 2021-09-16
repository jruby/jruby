/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.java.invokers;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaCallable;
import org.jruby.javasupport.JavaConstructor;
import org.jruby.javasupport.ParameterTypes;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Signature;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.collections.NonBlockingHashMapLong;

import static org.jruby.util.CodegenUtils.prettyParams;

public abstract class RubyToJavaInvoker<T extends JavaCallable> extends JavaMethod {
    static final NonBlockingHashMapLong NULL_CACHE = new NullHashMapLong();

    protected T javaCallable; /* null if multiple callable members */
    protected T[][] javaCallables; /* != null if javaCallable == null */
    protected T[] javaVarargsCallables; /* != null if any var args callables */

    // in case multiple callables (overloaded Java method - same name different args)
    // for the invoker exists  CallableSelector caches resolution based on args here
    NonBlockingHashMapLong<T> cache;

    volatile boolean initialized;

    private final Ruby runtime;
    private final Supplier<Member[]> members;

    @SuppressWarnings("unchecked") // NULL_CACHE
    RubyToJavaInvoker(RubyModule host, Supplier<Member[]> members, String name) {
        super(host, Visibility.PUBLIC, name);

        this.runtime = host.getRuntime();
        this.members = members;

        initialize();
    }

    void initialize() {
        if (initialized) return;

        synchronized (this) {
            if (initialized) return;

            // initialize all the callables for this method
            final T callable;
            final T[][] callables;
            T[] varargsCallables = null;
            int minVarArgsArity = -1;
            int maxArity, minArity;

            Member[] members = this.members.get();

            final int length = members.length;
            if (length == 1) {
                callable = createCallable(runtime, members[0]);
                maxArity = minArity = callable.getArity();
                if (callable.isVarArgs()) {
                    varargsCallables = createCallableArray(callable);
                    minVarArgsArity = getMemberArity(members[0]) - 1;
                }
                callables = null;

                cache = NULL_CACHE; // if there's a single callable - matching (and thus the cache) won't be used
            } else {
                callable = null;
                maxArity = -1;
                minArity = Integer.MAX_VALUE;

                IntHashMap<ArrayList<T>> arityMap = new IntHashMap<>(length, 1);

                ArrayList<T> varArgs = null;
                for (int i = 0; i < length; i++) {
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

                    if (javaMethod.isVarArgs()) {
                        final int usableArity = currentArity - 1;
                        // (String, Object...) has usable arity == 1 ... (String)
                        if ((methodsForArity = arityMap.get(usableArity)) == null) {
                            methodsForArity = new ArrayList<T>(length);
                            arityMap.put(usableArity, methodsForArity);
                        }
                        methodsForArity.add(javaMethod);

                        if (varArgs == null) varArgs = new ArrayList<T>(length);
                        varArgs.add(javaMethod);

                        if (minVarArgsArity == -1) minVarArgsArity = Integer.MAX_VALUE;
                        minVarArgsArity = Math.min(usableArity, minVarArgsArity);
                    }
                }

                callables = createCallableArrayArray(maxArity + 1);
                for (IntHashMap.Entry<ArrayList<T>> entry : arityMap.entrySet()) {
                    ArrayList<T> methodsForArity = entry.getValue();

                    T[] methodsArray = methodsForArity.toArray(createCallableArray(methodsForArity.size()));
                    callables[entry.getKey() /* int */] = methodsArray;
                }

                if (varArgs != null /* && varargsMethods.size() > 0 */) {
                    varargsCallables = (T[]) varArgs.toArray(createCallableArray(varArgs.size()));
                }
                // NOTE: tested (4, false); with opt_for_space: false but does not
                // seem to give  the promised ~10% improvement in map's speed ...
                cache = new NonBlockingHashMapLong<>(4, true); // 8 still uses MIN_SIZE_LOG == 4
            }

            this.javaCallable = callable;
            this.javaCallables = callables;
            this.javaVarargsCallables = varargsCallables;

            setSignature(minArity, maxArity, minVarArgsArity);
            setupNativeCall();

            initialized = true;
        }
    }

    private void setSignature(final int minArity, final int maxArity, final int minVarArgsArity) {
        if ( minVarArgsArity == -1 ) { // no var-args
            if ( minArity == maxArity ) {
                setSignature(Signature.from(minArity, 0, 0, 0, 0, Signature.Rest.NONE, -1));
            }
            else { // multiple overloads
                setSignature(Signature.from(minArity, maxArity - minArity, 0, 0, 0, Signature.Rest.NONE, -1));
            }
        }
        else {
            setSignature(Signature.from(minVarArgsArity < minArity ? minVarArgsArity : minArity, 0, 0, 0, 0, Signature.Rest.NORM, -1));
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
        return convertArguments(method, args, 0); // 0 - no additional space
    }

    public static Object[] convertArguments(final ParameterTypes method, final IRubyObject[] args, final int addSpace) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Object[] javaArgs; final int len = args.length;

        if ( method.isVarArgs() ) {
            final int last = paramTypes.length - 1;
            javaArgs = new Object[ last + 1 + addSpace ];
            for ( int i = 0; i < last; i++ ) {
                javaArgs[i] = args[i].toJava(paramTypes[i]);
            }
            javaArgs[ last ] = convertVarArgumentsOnly(paramTypes[ last ], last, args);
        }
        else {
            javaArgs = new Object[ len + addSpace ];
            for ( int i = 0; i < len; i++ ) {
                javaArgs[i] = args[i].toJava(paramTypes[i]);
            }
        }
        return javaArgs;
    }

    // specialized case of above convertArguments(IRubyObject...)
    public static Object[] convertArguments(final ParameterTypes method, final IRubyObject arg0, final int addSpace) {
        final Class<?>[] paramTypes = method.getParameterTypes();
        final Object[] javaArgs;

        if ( method.isVarArgs() ) {
            javaArgs = new Object[ 1 + addSpace ];
            javaArgs[0] = convertVarArgumentsOnly(paramTypes[0], arg0);
        }
        else {
            javaArgs = new Object[ 1 + addSpace ];
            javaArgs[0] = arg0.toJava(paramTypes[0]);
        }
        return javaArgs;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object convertVarArgumentsOnly(final Class<T> varArrayType,
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
        if (compType.isPrimitive()) {
            for (int i = 0; i < varCount; i++) {
                Array.set(varArgs, i, args[varStart + i].toJava(compType));
            }
        } else { // 10x speedup avoiding Array.set
            T[] base = (T[]) varArgs;
            for (int i = 0; i < varCount; i++) {
                base[i] = (T) (args[varStart + i].toJava(compType));
            }
        }
        return varArgs;
    }

    // specialized case of above convertVarArgumentsOnly
    private static Object convertVarArgumentsOnly(final Class<?> varArrayType,
        /* final int varStart = 0, */ final IRubyObject arg0) {

        if ( arg0 instanceof ArrayJavaProxy ) {
            // we may have a pre-created array to pass; try that first
            return arg0.toJava(varArrayType);
        }

        final Class<?> compType = varArrayType.getComponentType();
        final Object varArgs = Array.newInstance(compType, 1);
        Array.set(varArgs, 0, arg0.toJava(compType));
        return varArgs;
    }

    static JavaProxy castJavaProxy(final IRubyObject self) {
        assert self instanceof JavaProxy : "Java methods can only be invoked on Java objects";
        return (JavaProxy) self;
    }

    static Object unwrapIfJavaProxy(final IRubyObject object) {
        if (object instanceof JavaProxy) {
            return ((JavaProxy) object).getObject();
        }
        // special case when target is a plain-old Ruby object
        // e.g. in case of a `class Ruby; include java.some.Interface end`
        // Interface's default methods will be set up as InstanceMethodInvokers
        return object;
    }

    static <T extends AccessibleObject & Member> T setAccessible(T accessible) {
        // TODO: Replace flag that's false on 9 with proper module checks
        if (!Java.isAccessible(accessible) &&
                !Ruby.isSecurityRestricted() &&
                Options.JI_SETACCESSIBLE.load() &&
                accessible instanceof Member) {
            try { Java.trySetAccessible(accessible); }
            catch (SecurityException e) {}
            catch (RuntimeException re) {
                rethrowIfNotInaccessibleObject(re);
            }
        }
        return accessible;
    }

    static <T extends AccessibleObject & Member> T[] setAccessible(T[] accessibles) {
        // TODO: Replace flag that's false on 9 with proper module checks
        if (!Ruby.isSecurityRestricted() &&
                Options.JI_SETACCESSIBLE.load()) {
            try {
                for (T accessible : accessibles) {
                    if (Java.isAccessible(accessible)) continue;
                    if (!(accessible instanceof Member)) continue;

                    Java.trySetAccessible(accessible);
                }
            }
            catch (SecurityException e) {}
            catch (RuntimeException re) {
                rethrowIfNotInaccessibleObject(re);
            }
        }
        return accessibles;
    }

    private static void rethrowIfNotInaccessibleObject(RuntimeException re) {
        // Mega gross, but how else are we supposed to catch this and support Java 8?
        if (re.getClass().getName().equals("java.lang.reflect.InaccessibleObjectException")) {
            // ok, leave it inaccessible
        } else {
            // throw all other RuntimeException
            throw re;
        }
    }

    /**
     * Find the matching callable object given the target proxy wrapper, method name, arguments, and actual arity.
     *
     * @param self the proxy wrapper
     * @param name the method name
     * @param args the arguments
     * @param arity the actual arity
     * @return a suitable callable, or else raises an argument or name error
     */
    public T findCallable(IRubyObject self, String name, IRubyObject[] args, final int arity) {
        switch (arity) {
            case 0:
                return findCallableArityZero(self, name);
            case 1:
                return findCallableArityOne(self, name, args[0]);
            case 2:
                return findCallableArityTwo(self, name, args[0], args[1]);
            case 3:
                return findCallableArityThree(self, name, args[0], args[1], args[2]);
            case 4:
                return findCallableArityFour(self, name, args[0], args[1], args[2], args[3]);
        }
        return findCallableArityN(self, name, args, arity);
    }

    protected final T findCallableArityZero(IRubyObject self, String name) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            final T[] callablesForArity;
            if ( javaCallables.length == 0 || (callablesForArity = javaCallables[0]) == null ) {
                if ( ( callable = matchVarArgsCallableArityZero(self) ) == null ) {
                    throw newErrorDueNoMatchingCallable(self, name);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityZero(runtime, this, callablesForArity);
            if ( callable == null ) {
                if ((callable = matchVarArgsCallableArityZero(self)) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity);
                }
            }
        }
        else {
            if (!callable.isVarArgs()) checkCallableArity(callable, 0);
        }
        return callable;
    }

    protected final T findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 1 || (callablesForArity = javaCallables[1]) == null ) {
                if ((callable = matchVarArgsCallableArityOne(self, arg0)) == null) {
                    throw runtime.newArgumentError(1, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityOne(runtime, this, callablesForArity, arg0);
            if ( callable == null ) {
                if ((callable = matchVarArgsCallableArityOne(self, arg0)) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0);
                }
            }
        } else {
            if (!callable.isVarArgs()) checkCallableArity(callable, 1);
        }
        return callable;
    }

    protected final T findCallableArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 2 || (callablesForArity = javaCallables[2]) == null ) {
                if ((callable = matchVarArgsCallableArityTwo(self, arg0, arg1)) == null ) {
                    throw runtime.newArgumentError(2, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityTwo(runtime, this, callablesForArity, arg0, arg1);
            if ( callable == null ) {
                if ((callable = matchVarArgsCallableArityTwo(self, arg0, arg1)) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1);
                }
            }
        } else {
            if (!callable.isVarArgs()) checkCallableArity(callable, 2);
        }
        return callable;
    }

    protected final T findCallableArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 3 || (callablesForArity = javaCallables[3]) == null ) {
                if ( ( callable = matchVarArgsCallableArityThree(self, arg0, arg1, arg2) ) == null ) {
                    throw runtime.newArgumentError(3, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityThree(runtime, this, callablesForArity, arg0, arg1, arg2);
            if ( callable == null ) {
                if ( ( callable = matchVarArgsCallableArityThree(self, arg0, arg1, arg2) ) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2);
                }
            }
        } else {
            if (!callable.isVarArgs()) checkCallableArity(callable, 3);
        }
        return callable;
    }

    protected final T findCallableArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        T callable = this.javaCallable;
        if ( callable == null ) {
            // TODO: varargs?
            final T[] callablesForArity;
            if ( javaCallables.length <= 4 || (callablesForArity = javaCallables[4]) == null ) {
                if ( ( callable = matchVarArgsCallableArityFour(self, arg0, arg1, arg2, arg3) ) == null ) {
                    throw runtime.newArgumentError(4, javaCallables.length - 1);
                }
                return callable;
            }
            callable = CallableSelector.matchingCallableArityFour(runtime, this, callablesForArity, arg0, arg1, arg2, arg3);
            if ( callable == null ) {
                if ( ( callable = matchVarArgsCallableArityFour(self, arg0, arg1, arg2, arg3) ) == null ) {
                    throw newErrorDueArgumentTypeMismatch(self, callablesForArity, arg0, arg1, arg2, arg3);
                }
            }
        } else {
            if (!callable.isVarArgs()) checkCallableArity(callable, 4);
        }
        return callable;
    }

    private T findCallableArityN(IRubyObject self, String name, IRubyObject[] args, int arity) {
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
            if (!callable.isVarArgs()) checkCallableArity(callable, args.length);
        }
        return callable;
    }

    private T matchVarArgsCallableArityZero(IRubyObject self) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityZero(runtime, this, varArgsCallables);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables);
            }
            return callable;
        }
        return null;
    }

    private T matchVarArgsCallableArityOne(IRubyObject self, IRubyObject arg0) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityOne(runtime, this, varArgsCallables, arg0);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, arg0);
            }
            return callable;
        }
        return null;
    }

    private T matchVarArgsCallableArityTwo(IRubyObject self, IRubyObject arg0, IRubyObject arg1) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityTwo(runtime, this, varArgsCallables, arg0, arg1);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, arg0, arg1);
            }
            return callable;
        }
        return null;
    }

    private T matchVarArgsCallableArityThree(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityThree(runtime, this, varArgsCallables, arg0, arg1, arg2);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, arg0, arg1, arg2);
            }
            return callable;
        }
        return null;
    }

    private T matchVarArgsCallableArityFour(IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        final T[] varArgsCallables = this.javaVarargsCallables;
        if ( varArgsCallables != null ) {
            T callable = CallableSelector.matchingCallableArityFour(runtime, this, varArgsCallables, arg0, arg1, arg2, arg3);
            if ( callable == null ) {
                throw newErrorDueArgumentTypeMismatch(self, varArgsCallables, arg0, arg1, arg2, arg3);
            }
            return callable;
        }
        return null;
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
        return runtime.newNameError(error.toString(), (String) null);
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
