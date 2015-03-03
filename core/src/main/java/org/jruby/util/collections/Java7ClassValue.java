package org.jruby.util.collections;

/**
 * A proxy cache that uses Java 7's ClassValue.
 */
public class Java7ClassValue<T> extends ClassValue<T> {

    public Java7ClassValue(ClassValueCalculator<T> calculator) {
        super(calculator);
    }

    public T get(Class cls) {
        return proxy.get(cls);
    }

    private final java.lang.ClassValue<T> proxy = new java.lang.ClassValue<T>() {
        @Override
        protected T computeValue(Class<?> type) {
            return calculator.computeValue(type);
        }
    };
}
