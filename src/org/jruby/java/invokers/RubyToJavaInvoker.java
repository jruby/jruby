package org.jruby.java.invokers;

import org.jruby.javasupport.*;
import java.util.Arrays;
import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.java.dispatch.CallableSelector;
import org.jruby.java.proxies.JavaProxy;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class RubyToJavaInvoker extends org.jruby.internal.runtime.methods.JavaMethod {
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    protected JavaCallable javaCallable;
    protected JavaCallable[][] javaCallables;
    protected Map cache;
    protected volatile boolean initialized;
    
    RubyToJavaInvoker(RubyModule host) {
        super(host, Visibility.PUBLIC);
        // we set all Java methods to optional, since many/most have overloads
        setArity(Arity.OPTIONAL);
    }

    static Object convertArg(ThreadContext context, IRubyObject arg, JavaCallable method, int index) {
        return arg.toJava(method.getParameterTypes()[index]);
    }

    static JavaProxy castJavaProxy(IRubyObject self) {
        if (!(self instanceof JavaProxy)) {
            throw self.getRuntime().newTypeError("Java methods can only be invoked on Java objects");
        }
        JavaProxy proxy = (JavaProxy)self;
        return proxy;
    }

    void raiseNoMatchingCallableError(String name, IRubyObject proxy, Object... args) {
        int len = args.length;
        Class[] argTypes = new Class[args.length];
        for (int i = 0; i < len; i++) {
            argTypes[i] = args[i].getClass();
        }
        throw proxy.getRuntime().newNameError("no " + name + " with arguments matching " + Arrays.toString(argTypes) + " on object " + proxy.getMetaClass(), null);
    }

    protected JavaCallable findCallable(IRubyObject self, String name, IRubyObject[] args, int arity) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (arity >= javaCallables.length || (callablesForArity = javaCallables[arity]) == null) {
                throw self.getRuntime().newArgumentError(args.length, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityN(self, cache, callablesForArity, args, arity);
        } else {
            if (callable.getParameterTypes().length != args.length) {
                throw self.getRuntime().newArgumentError(args.length, callable.getParameterTypes().length);
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
                raiseNoMatchingCallableError(name, self, EMPTY_OBJECT_ARRAY);
            }
            callable = callablesForArity[0];
        } else {
            if (callable.getParameterTypes().length != 0) {
                throw self.getRuntime().newArgumentError(0, callable.getParameterTypes().length);
            }
        }
        return callable;
    }

    protected JavaCallable findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length < 1 || (callablesForArity = javaCallables[1]) == null) {
                throw self.getRuntime().newArgumentError(1, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityOne(self, cache, callablesForArity, arg0);
        } else {
            if (callable.getParameterTypes().length != 1) {
                throw self.getRuntime().newArgumentError(1, callable.getParameterTypes().length);
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
                throw self.getRuntime().newArgumentError(2, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityTwo(self, cache, callablesForArity, arg0, arg1);
        } else {
            if (callable.getParameterTypes().length != 2) {
                throw self.getRuntime().newArgumentError(2, callable.getParameterTypes().length);
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
                throw self.getRuntime().newArgumentError(3, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityThree(self, cache, callablesForArity, arg0, arg1, arg2);
        } else {
            if (callable.getParameterTypes().length != 3) {
                throw self.getRuntime().newArgumentError(3, callable.getParameterTypes().length);
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
                throw self.getRuntime().newArgumentError(4, javaCallables.length - 1);
            }
            callable = CallableSelector.matchingCallableArityFour(self, cache, callablesForArity, arg0, arg1, arg2, arg3);
        } else {
            if (callable.getParameterTypes().length != 4) {
                throw self.getRuntime().newArgumentError(4, callable.getParameterTypes().length);
            }
        }
        return callable;
    }
}