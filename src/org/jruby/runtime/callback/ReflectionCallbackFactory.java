package org.jruby.runtime.callback;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectionCallbackFactory extends CallbackFactory {

    public Callback getMethod(Class type, String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, false, Arity.noArguments());
    }

    public Callback getMethod(Class type, String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, false, Arity.singleArgument());
    }

    public Callback getMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, false, Arity.fixed(2));
    }

    public Callback getSingletonMethod(Class type, String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, true, Arity.noArguments());
    }

    public Callback getSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, true, Arity.singleArgument());
    }

    public Callback getSingletonMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, true, Arity.fixed(2));
    }

    public Callback getBlockMethod(Class type, String method) {
        return new ReflectionCallback(
            type,
            method,
            new Class[] { IRubyObject.class, IRubyObject.class },
            false,
            true,
            Arity.fixed(2));
    }

    public Callback getOptSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1, IRubyObject[].class }, true, true, Arity.optional());
    }

    public Callback getOptSingletonMethod(Class type, String method, Class[] args) {
        return new ReflectionCallback(type, method, args, true, true, Arity.optional());
    }

    public Callback getOptSingletonMethod(Class type, String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, true, Arity.optional());
    }

    public Callback getOptMethod(Class type, String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, false, Arity.optional());
    }

    public Callback getOptMethod(Class type, String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1, IRubyObject[].class }, true, false, Arity.optional());
    }
}
