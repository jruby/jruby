package org.jruby.util.collections;

import org.jruby.Ruby;

/**
 * Represents a cache or other mechanism for getting the Ruby-level proxy classes
 * for a given Java class.
 */
public abstract class ClassValue<T> {
    public ClassValue(ClassValueCalculator<T> calculator) {
        this.calculator = calculator;
    }
    
    public abstract T get(Class cls);
    
    protected final ClassValueCalculator<T> calculator;
}
