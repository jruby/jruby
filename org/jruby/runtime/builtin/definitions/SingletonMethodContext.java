package org.jruby.runtime.builtin.definitions;

import org.jruby.RubyObject;
import org.jruby.runtime.IStaticCallable;
import org.jruby.runtime.StaticCallback;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class SingletonMethodContext {
    private final IStaticCallable callable;
    private final RubyObject singleton;

    /**
     * Constructor for SingletonMethodContext.
     * @param runtime
     */
    public SingletonMethodContext(RubyObject singleton, IStaticCallable callable) {
        this.callable = callable;
        this.singleton = singleton;
    }

    public void create(String name, int index, int arity) {
        singleton.defineSingletonMethod(name, StaticCallback.create(callable, index, arity));
    }

    public void createOptional(String name, int index) {
        singleton.defineSingletonMethod(name, StaticCallback.createOptional(callable, index));
    }

    public void createOptional(String name, int index, int required) {
        singleton.defineSingletonMethod(name, StaticCallback.createOptional(callable, index, required));
    }
}