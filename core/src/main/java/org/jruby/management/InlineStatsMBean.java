package org.jruby.management;

public interface InlineStatsMBean {
    long getInlineSuccessCount();
    long getInlineFailedCount();
}
