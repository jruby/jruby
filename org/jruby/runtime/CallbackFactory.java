package org.jruby.runtime;

import org.jruby.*;

public final class CallbackFactory {
    public static Callback getMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method);
    }

    public static Callback getMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, arg1);
    }

    public static Callback getMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, arg2 });
    }

    public static Callback getSingletonMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, false, true);
    }

    public static Callback getSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, arg1, false, true);
    }

    public static Callback getSingletonMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, arg2 }, false, true);
    }

    public static Callback getBlockMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, new Class[] { RubyObject.class, RubyObject.class }, false, true);
    }
    
    public static Callback getOptSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, RubyObject[].class }, true, true);
    }
    
    public static Callback getOptSingletonMethod(Class type, String method, Class[] args) {
        return new ReflectionCallbackMethod(type, method, args, true, true);
    }

    public static Callback getOptSingletonMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, true, true);
    }

    public static Callback getOptMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, true);
    }

    public static Callback getOptMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, new Class[]{arg1, RubyObject[].class}, true);
    }
    
    public static Callback getFalseMethod() {
        return new Callback() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return ruby.getFalse();
            }
        };
    }
    
    public static Callback getTrueMethod() {
        return new Callback() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return ruby.getTrue();
            }
        };
    }
    
    public static Callback getNilMethod() {
        return new Callback() {
            public RubyObject execute(RubyObject recv, RubyObject[] args, Ruby ruby) {
                return ruby.getNil();
            }
        };
    }
}