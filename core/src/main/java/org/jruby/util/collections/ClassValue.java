package org.jruby.util.collections;

import org.jruby.util.cli.Options;

import java.util.function.Function;

/**
 * Represents a cache or other mechanism for getting the Ruby-level proxy classes
 * for a given Java class.
 * @param <T> value
 */
@SuppressWarnings({"unchecked", "deprecation"})
public abstract class ClassValue<T> {

    public enum Type {
        /**
         * A ClassValue based on java.lang.ClassValue but with stable, once-only computation.
         */
        STABLE(StableClassValue::new),

        /**
         * A ClassValue based on a hard-referencing, concurrent Map implementation with once-only computation.
         *
         * @see MapBasedClassValue
         */
        HARD_MAP(MapBasedClassValue::new),

        /**
         * A ClassValue based on java.lang.ClassValue but all values are strongly referenced until the ClassValue is
         * dereferenced. No guarantee of once-only computation.
         *
         * @see Java7ClassValue
         */
        @Deprecated
        HARD_VALUES(Java7ClassValue::new);

        Type(Function<ClassValueCalculator, ClassValue> function) {
            this.function = function;
        }

        final Function<ClassValueCalculator, ClassValue> function;
    }

    public ClassValue(ClassValueCalculator<T> calculator) {
        this.calculator = calculator;
    }

    public abstract T get(Class<?> cls);

    protected final ClassValueCalculator<T> calculator;

    public static <T> ClassValue<T> newInstance(ClassValueCalculator<T> calculator) {
        if (Options.INVOKEDYNAMIC_CLASS_VALUES.load()) return newJava7Instance(calculator);
        return Options.JI_CLASS_VALUES.load().function.apply(calculator);
    }

    @Deprecated
    private static <T> ClassValue<T> newJava7Instance(ClassValueCalculator<T> calculator) {
        return new Java7ClassValue<>(calculator);
    }

}
