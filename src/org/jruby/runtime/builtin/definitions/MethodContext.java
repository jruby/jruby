package org.jruby.runtime.builtin.definitions;

import org.jruby.RubyModule;
import org.jruby.runtime.IndexedCallback;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class MethodContext {
    private final RubyModule module;

    /**
     * Constructor for MethodContext.
     */
    public MethodContext(RubyModule module) {
        super();
        this.module = module;
    }

    public void create(String name, int index, int arity) {
        module.defineMethod(name, IndexedCallback.create(index, arity));
    }

    public void createOptional(String name, int index) {
        module.defineMethod(name, IndexedCallback.createOptional(index));
    }

    public void createOptional(String name, int index, int required) {
        module.defineMethod(name, IndexedCallback.createOptional(index, required));
    }
    
    public void createAlias(String name, String definition) {
        module.defineAlias(name, definition);
    }
}