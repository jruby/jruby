package org.jruby.runtime.opto;

import org.jruby.RubyModule;

public class GenerationAndSwitchPointInvalidator implements Invalidator {
    private final GenerationInvalidator generationInvalidator;
    private final SwitchPointInvalidator switchPointInvalidator;
    
    public GenerationAndSwitchPointInvalidator(RubyModule module) {
        generationInvalidator = new GenerationInvalidator(module);
        switchPointInvalidator = new SwitchPointInvalidator();
    }

    public void invalidate() {
        // does not need to be atomic
        generationInvalidator.invalidate();
        switchPointInvalidator.invalidate();
    }

    public Object getData() {
        return switchPointInvalidator.getData();
    }
}
