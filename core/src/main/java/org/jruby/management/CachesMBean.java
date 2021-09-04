package org.jruby.management;

public interface CachesMBean {
    public long getMethodInvalidationCount();
    public long getConstantInvalidationCount();
}
