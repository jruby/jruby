package org.jruby.runtime.opto;

import java.lang.invoke.SwitchPoint;
import java.util.List;
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
    
    public void invalidateAll(List<Invalidator> invalidators) {
        SwitchPoint[] switchPoints = new SwitchPoint[invalidators.size()];
        
        for (int i = 0; i < invalidators.size(); i++) {
            Invalidator invalidator = invalidators.get(i);
            assert invalidator instanceof GenerationAndSwitchPointInvalidator;
            GenerationAndSwitchPointInvalidator gsInvalidator = (GenerationAndSwitchPointInvalidator)invalidator;
            gsInvalidator.generationInvalidator.invalidate();
            switchPoints[i] = gsInvalidator.switchPointInvalidator.replaceSwitchPoint();
        }
        SwitchPoint.invalidateAll(switchPoints);
    }

    public Object getData() {
        return switchPointInvalidator.getData();
    }
}
