package org.jruby.runtime;

import org.jruby.util.Asserts;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class StaticCallback implements Callback {
    final IStaticCallable callable;
    final int index;
    final Arity arity;

    /**
     * Constructor for StaticCallback.
     */
    private StaticCallback(IStaticCallable callable, int index, Arity arity) {
        this.callable = callable;
        this.index = index;
        this.arity = arity;
    }
    
    /**
     * Create a callback with a minimal # of arguments
     */
    public static StaticCallback createOptional(IStaticCallable callable, int index, int minimum) {
        Asserts.isTrue(minimum >= 0);
        return new StaticCallback(callable, index, Arity.required(minimum));
    }


    /**
     * Create a callback with an optional # of arguments
     */
    public static StaticCallback createOptional(IStaticCallable callable, int index) {
        return new StaticCallback(callable, index, Arity.optional());
    }


    /**
     * Create a callback with a fixed # of arguments
     */
    public static StaticCallback create(IStaticCallable callable, int index, int arity) {
        return new StaticCallback(callable, index, Arity.fixed(arity));
    }


    /**
     * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
     */
    public IRubyObject execute(IRubyObject receiver, IRubyObject[] args) {
        arity.checkArity(receiver.getRuntime(), args);
        return callable.callIndexed(index, receiver, args);
    }

    public Arity getArity() {
        return arity;
    }
}