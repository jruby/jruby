package org.jruby.util.collections;

/**
 * Calculate a value based on an incoming class. Used by ClassValue.
 */
public interface ClassValueCalculator<T> {
    public T computeValue(Class<?> cls);
}
