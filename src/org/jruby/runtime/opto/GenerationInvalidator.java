package org.jruby.runtime.opto;

import org.jruby.RubyModule;

public class GenerationInvalidator implements Invalidator {
    private final RubyModule module;
    public GenerationInvalidator(RubyModule module) {
        this.module = module;
    }
    
    public void invalidate() {
        module.updateGeneration();
    }

    public Object getData() {
        return module.getGeneration();
    }
    
}
