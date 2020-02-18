package org.jruby.runtime.opto;

import java.util.List;

public class SimpleInvalidator implements Invalidator {
    private volatile Object serial;

    @Override
    public void invalidate() {
        serial = new Object();
    }

    @Override
    public void invalidateAll(List<Invalidator> invalidators) {
        invalidators.forEach((invalidator) -> invalidator.invalidate());
    }

    @Override
    public Object getData() {
        return serial;
    }
}
