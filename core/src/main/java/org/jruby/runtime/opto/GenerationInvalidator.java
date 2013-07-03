package org.jruby.runtime.opto;

import java.util.List;
import org.jruby.RubyModule;

public class GenerationInvalidator implements Invalidator {
    private final RubyModule module;
    public GenerationInvalidator(RubyModule module) {
        this.module = module;
    }
    
    public void invalidate() {
        module.updateGeneration();
    }
    
    public void invalidateAll(List<Invalidator> invalidators) {
        for (Invalidator invalidator : invalidators) {
            invalidator.invalidate();
        }
    }

    public Object getData() {
        return module.getGenerationObject();
    }
    
}
