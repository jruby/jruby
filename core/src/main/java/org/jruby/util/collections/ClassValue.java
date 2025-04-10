package org.jruby.util.collections;

import org.jruby.util.cli.Options;

/**
 * Represents a cache or other mechanism for getting the Ruby-level proxy classes
 * for a given Java class.
 * @param <T> value
 */
@SuppressWarnings("unchecked")
public abstract class ClassValue<T> {

    public ClassValue(ClassValueCalculator<T> calculator) {
        this.calculator = calculator;
    }

    public abstract T get(Class<?> cls);

    protected final ClassValueCalculator<T> calculator;

    public static <T> ClassValue<T> newInstance(ClassValueCalculator<T> calculator) {
        if ( CLASS_VALUE ) return newJava7Instance(calculator);
        return new MapBasedClassValue<>(calculator);
    }

    private static <T> ClassValue<T> newJava7Instance(ClassValueCalculator<T> calculator) {
        return new Java7ClassValue<>(calculator);
    }

    private static final boolean CLASS_VALUE = Options.INVOKEDYNAMIC_CLASS_VALUES.load();

}
