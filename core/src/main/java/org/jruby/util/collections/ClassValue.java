package org.jruby.util.collections;

import org.jruby.util.cli.Options;

/**
 * Represents a cache or other mechanism for getting the Ruby-level proxy classes
 * for a given Java class.
 */
@SuppressWarnings("unchecked")
public abstract class ClassValue<T> {

    public ClassValue(ClassValueCalculator<T> calculator) {
        this.calculator = calculator;
    }

    public abstract T get(Class cls);

    protected final ClassValueCalculator<T> calculator;

    public static <T> ClassValue<T> newInstance(ClassValueCalculator<T> calculator) {
        if ( JAVA7_CLASS_VALUE ) return newJava7Instance(calculator);
        return new MapBasedClassValue<T>(calculator);
    }

    private static <T> ClassValue<T> newJava7Instance(ClassValueCalculator<T> calculator) {
        return new Java7ClassValue<T>(calculator);
    }

    private static final boolean JAVA7_CLASS_VALUE;

    static {
        boolean java7ClassValue = false;
        if ( Options.INVOKEDYNAMIC_CLASS_VALUES.load() ) {
            try {
                Class.forName("java.lang.ClassValue");
                Class.forName("org.jruby.util.collections.Java7ClassValue");
                java7ClassValue = true;
            }
            catch (Exception ex) {
                // fall through to Map version
            }
        }
        JAVA7_CLASS_VALUE = java7ClassValue;
    }

}
