package org.jruby.runtime;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callback.Callback;
import org.jruby.runtime.callback.ReflectionCallbackFactory;

/**
 * Helper class to build Callback method.
 * This impements method corresponding to the signature of method most often found in
 * the Ruby library, for methods with other signature the appropriate Callback object
 * will need to be explicitly created.
 **/
public abstract class CallbackFactory {
    public static final Class[] NULL_CLASS_ARRAY = new Class[0];
    
    /**
     * gets an instance method with no arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getMethod(String method);

    /**
     * gets an instance method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getMethod(String method, Class arg1);

    /**
     * gets an instance method with two arguments.
     * @param method name of the method
     * @param arg1 the java class of the first argument for this method
     * @param arg2 the java class of the second argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getMethod(String method, Class arg1, Class arg2);

    /**
     * gets a singleton (class) method without arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getSingletonMethod(String method);

    /**
     * gets a singleton (class) method with 1 argument.
     * @param method name of the method
     * @param arg1 the class of the only argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getSingletonMethod(String method, Class arg1);

    /**
     * gets a singleton (class) method with 2 arguments.
     * @param method name of the method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getSingletonMethod(String method, Class arg1, Class arg2);

    public abstract Callback getBlockMethod(String method);

    /**
     * gets a singleton (class) method with 1 mandatory argument and some optional arguments.
     * @param method name of the method
     * @param arg1 the class of the only mandatory argument for this method
     * @return a CallBack object corresponding to the appropriate method
     **/
    public abstract Callback getOptSingletonMethod(String method, Class arg1);

    /**
    * gets a singleton (class) method with no mandatory argument and some optional arguments.
     * @param method name of the method
    * @return a CallBack object corresponding to the appropriate method
    **/
    public abstract Callback getOptSingletonMethod(String method);

    /**
    * gets an instance method with no mandatory argument and some optional arguments.
     * @param method name of the method
    * @return a CallBack object corresponding to the appropriate method
    **/
    public abstract Callback getOptMethod(String method);

    /**
    * gets an instance method with 1 mandatory argument and some optional arguments.
     * @param method name of the method
     * @param arg1 the class of the only mandatory argument for this method
    * @return a CallBack object corresponding to the appropriate method
    **/
    public abstract Callback getOptMethod(String method, Class arg1);

    public Callback getFalseMethod(final int arity) {
        return new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return recv.getRuntime().getFalse();
            }

            public Arity getArity() {
                return Arity.createArity(arity);
            }
        };
    }

    public Callback getTrueMethod(final int arity) {
        return new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return recv.getRuntime().getTrue();
            }

            public Arity getArity() {
                return Arity.createArity(arity);
            }
        };
    }

    public Callback getNilMethod(final int arity) {
        return new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return recv.getRuntime().getNil();
            }

            public Arity getArity() {
                return Arity.createArity(arity);
            }
        };
    }

    public Callback getSelfMethod(final int arity) {
        return new Callback() {
            public IRubyObject execute(IRubyObject recv, IRubyObject[] args) {
                return recv;
            }

            public Arity getArity() {
                return Arity.createArity(arity);
            }
        };
    }

    public static CallbackFactory createFactory(Class type) {
        /* Removed cglib for now
        try {
            // Check if we have CGLIB support compiled in.
            Class factoryClass = Class.forName("org.jruby.runtime.callback.CglibCallbackFactory");
            // Check if CGLIB is available.
            Class.forName("net.sf.cglib.reflect.FastClass");
            try {
                return (CallbackFactory) factoryClass.newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } catch (ClassNotFoundException e) {
            return new ReflectionCallbackFactory();
        }
        */
        
        return new ReflectionCallbackFactory(type);
    }
}
