package org.jruby.runtime.callback;

import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class ReflectionCallbackFactory extends CallbackFactory {
	
	private final Class type;
	
	public ReflectionCallbackFactory(Class type) {
		this.type = type;
	}
	
    public Callback getMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, false, Arity.noArguments());
    }

    public Callback getMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, false, Arity.singleArgument());
    }

    public Callback getMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, false, Arity.fixed(2));
    }

    public Callback getSingletonMethod(String method) {
        return new ReflectionCallback(type, method, NULL_CLASS_ARRAY, false, true, Arity.noArguments());
    }

    public Callback getSingletonMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1 }, false, true, Arity.singleArgument());
    }

    public Callback getSingletonMethod(String method, Class arg1, Class arg2) {
        return new ReflectionCallback(type, method, new Class[] { arg1, arg2 }, false, true, Arity.fixed(2));
    }

    public Callback getBlockMethod(String method) {
        return new ReflectionCallback(
            type,
            method,
            new Class[] { IRubyObject.class, IRubyObject.class },
            false,
            true,
            Arity.fixed(2));
    }

    public Callback getOptSingletonMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1, IRubyObject[].class }, true, true, Arity.optional());
    }
    
    public Callback getOptSingletonMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, true, Arity.optional());
    }

    public Callback getOptMethod(String method) {
        return new ReflectionCallback(type, method, new Class[] { IRubyObject[].class }, true, false, Arity.optional());
    }

    public Callback getOptMethod(String method, Class arg1) {
        return new ReflectionCallback(type, method, new Class[] { arg1, IRubyObject[].class }, true, false, Arity.optional());
    }
}
