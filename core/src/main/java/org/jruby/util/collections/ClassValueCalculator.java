package org.jruby.util.collections;

import java.util.function.Function;

/**
 * Calculate a value based on an incoming class. Used by ClassValue.
 */
public interface ClassValueCalculator<T> extends Function<Class<?>, T> {
    T computeValue(Class<?> cls);
    default T apply(Class<?> cls) {
        return computeValue(cls);
    }
}
