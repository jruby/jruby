package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public final class StaticCallback implements Callback {
    final IStaticCallable callable;
    final int index;
    final int arity;

    /**
     * Constructor for StaticCallback.
     */
    private StaticCallback(IStaticCallable callable, int index, int arity) {
        this.callable = callable;
        this.index = index;
        this.arity = arity;
    }
    
    /**
     * Create a callback with a minimal # of arguments
     */
    public static StaticCallback createOptional(IStaticCallable callable, int index, int required) {
        return new StaticCallback(callable, index, -(1 + required));
    }


    /**
     * Create a callback with an optional # of arguments
     */
    public static StaticCallback createOptional(IStaticCallable callable, int index) {
        return new StaticCallback(callable, index, -1);
    }


    /**
     * Create a callback with a fixed # of arguments
     */
    public static StaticCallback create(IStaticCallable callable, int index, int arity) {
        return new StaticCallback(callable, index, arity);
    }


    /**
     * @see org.jruby.runtime.Callback#execute(IRubyObject, IRubyObject[])
     */
    public IRubyObject execute(IRubyObject receiver, IRubyObject[] args) {
        checkArity(receiver.getRuntime(), args);
        return callable.callIndexed(index, receiver, args);
    }

    private void checkArity(Ruby ruby, IRubyObject[] args) {
        if (arity >= 0) {
            if (arity != args.length) {
                throw new ArgumentError(ruby,
                                        "wrong # of arguments(" + args.length + " for " + arity + ")");
            }
        } else {
            int required = -(1 + arity);
            if (args.length < required) {
                throw new ArgumentError(ruby, "wrong # of arguments(at least " + required + ")");
            }
        }
    }

    /**
     * @see org.jruby.runtime.Callback#getArity()
     */
    public int getArity() {
        return arity;
    }
}