package org.jruby.core;

import org.jruby.*;

public class CallbackFactory {
	public static Callback getMethod(Class type, String method) {
	    return new ReflectionCallbackMethod(type, method);
	}

	public static Callback getMethod(Class type, String method, Class arg1) {
	    return new ReflectionCallbackMethod(type, method, arg1);
	}
	
	public static Callback getSingletonMethod(Class type, String method) {
	    return new ReflectionCallbackMethod(type, method, false, true);
	}
	
	public static Callback getSingletonMethod(Class type, String method, Class arg1) {
	    return new ReflectionCallbackMethod(type, method, arg1, false, true);
	}
	
	public static Callback getOptSingletonMethod(Class type, String method) {
	    return new ReflectionCallbackMethod(type, method, true, true);
	}
	
	public static Callback getOptMethod(Class type, String method) {
	    return new ReflectionCallbackMethod(type, method, true);
	}

	public static Callback getTrueMethod() {
	    return new Callback() {
	        public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
	            return ruby.getTrue();
	        }
	    };
	}
}