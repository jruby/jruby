package org.jruby.runtime.builtin.definitions;

import org.jruby.RubyModule;
import org.jruby.runtime.IStaticCallable;
import org.jruby.runtime.StaticCallback;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class ModuleFunctionsContext {
    private final IStaticCallable callable;
    private final RubyModule module;

    /**
     * Constructor for SingletonMethodContext.
     * @param runtime
     */
    public ModuleFunctionsContext(RubyModule singleton, IStaticCallable callable) {
        this.callable = callable;
        this.module = singleton;
    }

    public void create(String name, int index, int arity) {
        module.defineModuleFunction(name, StaticCallback.create(callable, index, arity));
    }

    public void createOptional(String name, int index) {
        module.defineModuleFunction(name, StaticCallback.createOptional(callable, index));
    }

    public void createOptional(String name, int index, int required) {
        module.defineModuleFunction(name, StaticCallback.createOptional(callable, index, required));
    }
}