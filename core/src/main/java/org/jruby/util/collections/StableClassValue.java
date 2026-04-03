package org.jruby.util.collections;

import java.util.function.Function;

/**
 * An implementation of JRuby's ClassValue, using java.lang.ClassValue directly but ensuring computation of each value
 * occurs at most once.
 */
final class StableClassValue<T> extends ClassValue<T> {
    final Object lock = new Object();

    public StableClassValue(ClassValueCalculator<T> calculator) {
        super(calculator);
    }

    public T get(Class<?> cls) {
        // We don't check for null on the WeakReference since the
        // value is strongly referenced by proxy's list
        return proxy.get(cls).get(cls);
    }

    /**
     * Represents a stable value, which is computed at most once and then stored forever.
     *
     * This should be replaced with a JDK StableValue once it becomes available in Java 25. The double-checked locking
     * implementation cannot be folded by the JVM and will not have the same performance characteristics.
     *
     * @param <Input> input of the computation
     * @param <Result> result of the computation
     */
    private static class StableValue<Input, Result> {
        private Function<Input, Result> calculator;
        private volatile Result result;
        private final Object lock;
        StableValue(Object lock, Function<Input, Result> calculator) {
            this.calculator = calculator;
            this.lock = lock;
        }
        Result get(Input input) {
            Result result = this.result;

            if (result != null) return result;

            // lock on the StableClassValue so there are not multiple locks potentially in different orders
            synchronized (lock) {
                result = this.result;

                if (result != null) return result;

                result = this.calculator.apply(input);
                this.result = result;
            }

            return result;
        }
    }

    private final java.lang.ClassValue<StableValue<Class<?>, T>> proxy = new java.lang.ClassValue<StableValue<Class<?>, T>>() {
        @Override
        protected StableValue<Class<?>, T> computeValue(Class<?> type) {
            return new StableValue<>(lock, calculator);
        }
    };
}
