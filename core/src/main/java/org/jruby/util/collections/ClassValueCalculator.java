package org.jruby.util.collections;

/**
 * Calculate a value based on an incoming class. Used by ClassValue.
 * @param <T> value
 */
public interface ClassValueCalculator<T> {
    T computeValue(Class<?> cls);
}
