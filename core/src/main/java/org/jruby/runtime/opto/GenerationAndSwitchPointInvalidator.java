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
        if (invalidators.isEmpty()) return;

        SwitchPoint[] switchPoints = new SwitchPoint[invalidators.size()];
        
        for (int i = 0; i < invalidators.size(); i++) {
            Invalidator invalidator = invalidators.get(i);
            assert invalidator instanceof SwitchPointInvalidator;
            SwitchPointInvalidator switchPointInvalidator = (SwitchPointInvalidator) invalidator;
            switchPoints[i] = switchPointInvalidator.replaceSwitchPoint();
        }
        SwitchPoint.invalidateAll(switchPoints);
    }

    public Object getData() {
        return switchPointInvalidator.getData();
    }

    public void addIfUsed(RubyModule.InvalidatorList invalidators) {
        // invalidate generation immediately and only add SP invalidator
        generationInvalidator.invalidate();
        switchPointInvalidator.addIfUsed(invalidators);
    }
}
