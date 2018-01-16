package org.jruby.runtime.opto;

import org.jruby.management.Caches;

import java.util.List;

/**
 * A validator specific to how we manage Ruby constants.
 */
public class ConstantInvalidator extends SwitchPointInvalidator {
    private final Caches caches;

    public ConstantInvalidator(Caches caches) {
        this.caches = caches;
    }

    @Override
    public void invalidate() {
        // order is important; invalidate before increment
        super.invalidate();
        caches.incrementConstantInvalidations();
    }

    @Override
    public void invalidateAll(List<Invalidator> invalidators) {
        // order is important; invalidate before increment
        super.invalidateAll(invalidators);
        caches.incrementConstantInvalidations();
    }
}
