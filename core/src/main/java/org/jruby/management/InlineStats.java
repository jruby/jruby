package org.jruby.management;

import java.util.concurrent.atomic.AtomicLong;

public class InlineStats implements InlineStatsMBean {
    final AtomicLong inlineSuccessCount = new AtomicLong(0);
    final AtomicLong inlineFailedCount = new AtomicLong(0);

    @Override
    public long getInlineSuccessCount() {
        return inlineSuccessCount.get();
    }

    @Override
    public long getInlineFailedCount() {
        return inlineFailedCount.get();
    }

    public void incrementInlineSuccessCount() {
        inlineSuccessCount.incrementAndGet();
    }

    public void incrementInlineFailedCount() {
        inlineFailedCount.incrementAndGet();
    }

}
