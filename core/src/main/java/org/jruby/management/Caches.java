package org.jruby.management;

import java.util.concurrent.atomic.AtomicLong;

public class Caches implements CachesMBean {
    private final AtomicLong methodInvalidations = new AtomicLong(0);
    private final AtomicLong constantInvalidations = new AtomicLong(0);

    @Override
    public long getMethodInvalidationCount() {
        return methodInvalidations.get();
    }

    @Override
    public long getConstantInvalidationCount() {
        return constantInvalidations.get();
    }

    public long incrementMethodInvalidations() {
        return methodInvalidations.incrementAndGet();
    }

    public long incrementConstantInvalidations() {
        return constantInvalidations.incrementAndGet();
    }
}
