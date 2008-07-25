package org.jruby.javasupport.methods;

import org.jruby.javasupport.*;
import java.util.Arrays;
import java.util.Map;
import org.jruby.RubyClass;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class RubyToJavaInvoker extends org.jruby.internal.runtime.methods.JavaMethod {
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];
    protected JavaCallable javaCallable;
    protected JavaCallable[][] javaCallables;
    protected Map cache;
    protected volatile boolean initialized;
    
    RubyToJavaInvoker(RubyClass host) {
        super(host, Visibility.PUBLIC);
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
            if (arity > javaCallables.length || (callablesForArity = javaCallables[arity]) == null) {
                raiseNoMatchingCallableError(name, self, args, 0);
            }
            callable = Java.matchingCallableArityN(self, cache, callablesForArity, args, arity);
        }
        return callable;
    }

    protected JavaCallable findCallableArityZero(IRubyObject self, String name) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length == 0 || (callablesForArity = javaCallables[0]) == null) {
                raiseNoMatchingCallableError(name, self, IRubyObject.NULL_ARRAY, 0);
            }
            callable = callablesForArity[0];
        }
        return callable;
    }

    protected JavaCallable findCallableArityOne(IRubyObject self, String name, IRubyObject arg0) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length < 1 || (callablesForArity = javaCallables[1]) == null) {
                raiseNoMatchingCallableError(name, self, arg0);
            }
            callable = Java.matchingCallableArityOne(self, cache, callablesForArity, arg0);
        }
        return callable;
    }

    protected JavaCallable findCallableArityTwo(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 2 || (callablesForArity = javaCallables[2]) == null) {
                raiseNoMatchingCallableError(name, self, arg0, arg1);
            }
            callable = Java.matchingCallableArityTwo(self, cache, callablesForArity, arg0, arg1);
        }
        return callable;
    }

    protected JavaCallable findCallableArityThree(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 3 || (callablesForArity = javaCallables[3]) == null) {
                raiseNoMatchingCallableError(name, self, arg0, arg1, arg2);
            }
            callable = Java.matchingCallableArityThree(self, cache, callablesForArity, arg0, arg1, arg2);
        }
        return callable;
    }

    protected JavaCallable findCallableArityFour(IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        JavaCallable callable;
        if ((callable = javaCallable) == null) {
            // TODO: varargs?
            JavaCallable[] callablesForArity = null;
            if (javaCallables.length <= 4 || (callablesForArity = javaCallables[4]) == null) {
                raiseNoMatchingCallableError(name, self, arg0, arg1, arg2, arg3);
            }
            callable = Java.matchingCallableArityFour(self, cache, callablesForArity, arg0, arg1, arg2, arg3);
        }
        return callable;
    }
}