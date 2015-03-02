package org.jruby.java.invokers;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.JavaMethod;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.ArrayJavaProxy;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.javasupport.JavaCallable;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.collections.IntHashMap;

public abstract class RubyToJavaInvoker extends JavaMethod {

    protected final JavaCallable javaCallable; /* null if multiple callable members */
    protected final JavaCallable[][] javaCallables; /* != null if javaCallable == null */
    protected final JavaCallable[] javaVarargsCallables; /* != null if any var args callables */
    protected final int minVarargsArity;

    // initialize cache of parameter types to method
    // FIXME: No real reason to use CHM, is there?
    protected final Map cache = new ConcurrentHashMap(0, 0.75f, 1);

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
        int varargsArity = Integer.MAX_VALUE;

        if (members.length == 1) {
            callable = createCallable(runtime, members[0]);
            if ( callable.isVarArgs() ) {
                varargsCallables = createCallableArray(callable);
            }
            callables = null;
        }
        else {
            callable = null;

            IntHashMap<List<JavaCallable>> arityMap = new IntHashMap<List<JavaCallable>>(members.length);
            ArrayList<JavaCallable> varArgs = null;
            int maxArity = 0;
            for ( final Member method : members ) {
                final int currentArity = getMemberParameterTypes(method).length;
                maxArity = Math.max(currentArity, maxArity);

                List<JavaCallable> methodsForArity = arityMap.get(currentArity);
                if (methodsForArity == null) {
                    methodsForArity = new ArrayList<JavaCallable>(8);
                    arityMap.put(currentArity, methodsForArity);
                }

                final JavaCallable javaMethod = createCallable(runtime, method);
                methodsForArity.add(javaMethod);

                if ( isMemberVarArgs(method) ) {
                    varargsArity = Math.min(currentArity - 1, varargsArity);
                    if (varArgs == null) varArgs = new ArrayList<JavaCallable>();
                    varArgs.add(javaMethod);
                }
            }

            callables = createCallableArrayArray(maxArity + 1);
            for (IntHashMap.Entry<List<JavaCallable>> entry : arityMap.entrySet()) {
                List<JavaCallable> methodsForArity = entry.getValue();

                JavaCallable[] methodsArray = methodsForArity.toArray(createCallableArray(methodsForArity.size()));
                callables[ entry.getKey() ] = methodsArray;
            }

            if (varArgs != null /* && varargsMethods.size() > 0 */) {
                varargsCallables = varArgs.toArray( createCallableArray(varArgs.size()) );
            }
        }

        this.javaCallable = callable;
        this.javaCallables = callables;
        this.javaVarargsCallables = varargsCallables;
        this.minVarargsArity = varargsArity;

        // if it's not overloaded, set up a NativeCall
        if (javaCallable != null) {
            // no constructor support yet
            if (javaCallable instanceof org.jruby.javasupport.JavaMethod) {
                setNativeCallIfPublic( (Method) ((org.jruby.javasupport.JavaMethod) javaCallable).getValue() );
            }
        } else { // use the lowest-arity non-overload
            for (JavaCallable[] callablesForArity : javaCallables) {
                if ( callablesForArity == null || callablesForArity.length != 1 ) continue;
                if ( callablesForArity[0] instanceof org.jruby.javasupport.JavaMethod ) {
                    setNativeCallIfPublic( (Method) ((org.jruby.javasupport.JavaMethod) callablesForArity[0]).getValue() );
                }
            }
        }
    }

    private void setNativeCallIfPublic(final Method method) {
        final int mod = method.getModifiers(); // only public, since non-public don't bind
        if ( Modifier.isPublic(mod) && Modifier.isPublic(method.getDeclaringClass().getModifiers()) ) {
            setNativeCall(method.getDeclaringClass(), method.getName(), method.getReturnType(), method.getParameterTypes(), Modifier.isStatic(mod), true);
        }
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

    static Object convertArg(IRubyObject arg, JavaCallable method, int index) {
        return arg.toJava(method.getParameterTypes()[index]);
    }

    static Object convertVarargs(IRubyObject[] args, JavaCallable method) {
        Class[] types = method.getParameterTypes();
        Class varargArrayType = types[types.length - 1];
        Class varargType = varargArrayType.getComponentType();
        int varargsStart = types.length - 1;
        int varargsCount = args.length - varargsStart;

        Object varargs;
        if (args.length == 0) {
            return Array.newInstance(varargType, 0);
        } else if (varargsCount == 1 && args[varargsStart] instanceof ArrayJavaProxy) {
            // we may have a pre-created array to pass; try that first
            varargs = args[varargsStart].toJava(varargArrayType);
        } else {
            varargs = Array.newInstance(varargType, varargsCount);

            for (int i = 0; i < varargsCount; i++) {
                Array.set(varargs, i, args[varargsStart + i].toJava(varargType));
            }
        }
        return varargs;
    }

    static JavaProxy castJavaProxy(IRubyObject self) {
        assert self instanceof JavaProxy : "Java methods can only be invoked on Java objects";
        return (JavaProxy)self;
    }

    static void trySetAccessible(AccessibleObject[] accObjs) {
        if (!Ruby.isSecurityRestricted()) {
            try {
                AccessibleObject.setAccessible(accObjs, true);
            } catch(SecurityException e) {}
        }
    }

    void raiseNoMatchingCallableError(String name, IRubyObject proxy, Object... args) {
        final int len = args.length;
        Class[] argTypes = new Class[len];
        for ( int i = 0; i < len; i++ ) {
            argTypes[i] = args[i].getClass();
        }
        throw runtime.newArgumentError("no " + name + " with arguments matching " + Arrays.toString(argTypes) + " on object " + proxy.getMetaClass());
    }

    protected JavaCallable findCallable(IRubyObject self, String name, IRubyObject[] args, int arity) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            JavaCallable[] callablesForArity = null;
            if (arity >= javaCallables.length || (callablesForArity = javaCallables[arity]) == null) {
                if (javaVarargsCallables != null) {
                    callable = CallableSelector.matchingCallableArityN(runtime, cache, javaVarargsCallables, args, arity);
                    if (callable == null) {
                        throw CallableSelector.argTypesDoNotMatch(runtime, self, javaVarargsCallables, (Object[])args);
                    }
                    return callable;
                } else {
                    throw runtime.newArgumentError(args.length, javaCallables.length - 1);
                }
            }
            callable = CallableSelector.matchingCallableArityN(runtime, cache, callablesForArity, args, arity);
            if (callable == null && javaVarargsCallables != null) {
                callable = CallableSelector.matchingCallableArityN(runtime, cache, javaVarargsCallables, args, arity);
                if (callable == null) {
                    throw CallableSelector.argTypesDoNotMatch(runtime, self, javaVarargsCallables, (Object[])args);
                }
                return callable;
            }
            if (callable == null) {
                throw CallableSelector.argTypesDoNotMatch(runtime, self, callablesForArity, (Object[])args);
            }
        } else {
            if (!callable.isVarArgs() && callable.getParameterTypes().length != args.length) {
                throw runtime.newArgumentError(args.length, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityZero(IRubyObject self, String name) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length == 0 || (callablesForArity = javaCallables[0]) == null) {
                raiseNoMatchingCallableError(name, self);
            }
            callable = callablesForArity[0];
        } else {
            if (callable.getParameterTypes().length != 0) {
                throw runtime.newArgumentError(0, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 1 || (callablesForArity = javaCallables[1]) == null) {
                throw runtime.newArgumentError(1, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityOne(runtime, cache, callablesForArity, arg0);
            if (callable == null) {
                throw CallableSelector.argTypesDoNotMatch(runtime, self, callablesForArity, arg0);
            }
        } else {
            if (callable.getParameterTypes().length != 1) {
                throw runtime.newArgumentError(1, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 2 || (callablesForArity = javaCallables[2]) == null) {
                throw runtime.newArgumentError(2, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityTwo(runtime, cache, callablesForArity, arg0, arg1);
            if (callable == null) {
                throw CallableSelector.argTypesDoNotMatch(runtime, self, callablesForArity, arg0, arg1);
            }
        } else {
            if (callable.getParameterTypes().length != 2) {
                throw runtime.newArgumentError(2, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 3 || (callablesForArity = javaCallables[3]) == null) {
                throw runtime.newArgumentError(3, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityThree(runtime, cache, callablesForArity, arg0, arg1, arg2);
            if (callable == null) {
                throw CallableSelector.argTypesDoNotMatch(runtime, self, callablesForArity, arg0, arg1, arg2);
            }
        } else {
            if (callable.getParameterTypes().length != 3) {
                throw runtime.newArgumentError(3, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 4 || (callablesForArity = javaCallables[4]) == null) {
                throw runtime.newArgumentError(4, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityFour(runtime, cache, callablesForArity, arg0, arg1, arg2, arg3);
            if (callable == null) {
                throw CallableSelector.argTypesDoNotMatch(runtime, self, callablesForArity, arg0, arg1, arg2, arg3);
            }
        } else {
            if (callable.getParameterTypes().length != 4) {
                throw runtime.newArgumentError(4, callable.getParameterTypes().length);
            }
        }
        return callable;
    }
}