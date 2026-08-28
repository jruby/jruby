package org.jruby.internal.runtime.methods;

/**
 * Indicates this method implementation can be used to generate a cacheable method
 */
public interface CacheableMethod {
    public DynamicMethod getMethodForCaching();
}
