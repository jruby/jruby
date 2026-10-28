package org.jruby.runtime.opto;

import java.lang.invoke.SwitchPoint;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SwitchPointInvalidatorTest {

    @Test
    public void invalidateDuringInvalidateAllInvalidatesTheSwitchPointAlreadyHandedOut() {
        SwitchPointInvalidator target = new SwitchPointInvalidator();
        SwitchPointInvalidator other = new SwitchPointInvalidator();
        SwitchPoint handedOut = (SwitchPoint) target.getData();
        other.getData();

        boolean[] hookRan = {false};
        boolean[] invalidWhenInvalidateReturned = {false};
        List<Invalidator> batch = new ArrayList<>(List.of(target, other)) {
            @Override
            public Invalidator get(int index) {
                if (index == 1 && !hookRan[0]) {
                    hookRan[0] = true;
                    target.invalidate();
                    invalidWhenInvalidateReturned[0] = handedOut.hasBeenInvalidated();
                }
                return super.get(index);
            }
        };

        target.invalidateAll(batch);

        assertTrue("hook inside invalidateAll did not run", hookRan[0]);
        assertTrue("invalidate() returned while the SwitchPoint it handed out was still valid", invalidWhenInvalidateReturned[0]);
    }
}
