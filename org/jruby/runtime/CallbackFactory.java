package org.jruby.runtime;

import org.jruby.*;

/**
 * Helper class to build Callback method.
 * This impements method corresponding to the signature of method most often found in
 * the Ruby library, for methods with other signature the appropriate Callback object
 * will need to be explicitly created.
 **/
public final class CallbackFactory {
    /**
     * gets an instance method with no arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method);
    }

    /**
     * gets an instance method with 1 argument.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, arg1);
    }

    /**
     * gets an instance method with 1 argument.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, arg2 });
    }

    /**
     * gets a singleton (class) method without arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getSingletonMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, false, true);
    }

    /**
     * gets a singleton (class) method with 1 argument.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, arg1, false, true);
    }

    /**
     * gets a singleton (class) method with 2 arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getSingletonMethod(Class type, String method, Class arg1, Class arg2) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, arg2 }, false, true);
    }

    public static Callback getBlockMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, new Class[] { RubyObject.class, RubyObject.class }, false, true);
    }
    


    
    /**
     * gets a singleton (class) method with 1 mandatory argument and some optional arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param arg1 the class of the only mandatory argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getOptSingletonMethod(Class type, String method, Class arg1) {
        return new ReflectionCallbackMethod(type, method, new Class[] { arg1, RubyObject[].class }, true, true);
    }
    
    /**
     * gets a singleton (class) method with several mandatory argument and some optional arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param args an array of java class of the mandatory arguments (NOTE: this must include 
     * the appropriate rest argument class (usually a RubyObject[].class))
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getOptSingletonMethod(Class type, String method, Class[] args) {
        return new ReflectionCallbackMethod(type, method, args, true, true);
    }

     /**
     * gets a singleton (class) method with no mandatory argument and some optional arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getOptSingletonMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, true, true);
    }

     /**
     * gets an instance method with no mandatory argument and some optional arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public static Callback getOptMethod(Class type, String method) {
        return new ReflectionCallbackMethod(type, method, true);
    }

     /**
     * gets an instance method with 1 mandatory argument and some optional arguments.
     * @param type java class where the method is implemented
     * @param method name of the method
     * @param arg1 the class of the only mandatory argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
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
