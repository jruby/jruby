package org.jruby.util.collections;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * An implementation of JRuby's ClassValue, using java.lang.ClassValue directly but ensuring computation of each value
 * occurs at most once.
 */
final class StableClassValue<T> extends ClassValue<T> {
    private final ReentrantLock lock = new ReentrantLock();
    private final WeakHashMap<StableValue<Class<?>, T>, Object> stableValues = new WeakHashMap<>();

    public StableClassValue(ClassValueCalculator<T> calculator) {
        super(calculator);
    }

    public T get(Class<?> cls) {
        // We don't check for null on the WeakReference since the
        // value is strongly referenced by proxy's list
        return proxy.get(cls).get(cls);
    }

    public void clear() {
        synchronized (stableValues) {
            for (StableValue<Class<?>, T> value : stableValues.keySet()) {
                value.clear();
            }
            stableValues.clear();
        }
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
        private final ReentrantLock lock;
        private volatile Function<Input, Result> calculator;
        private volatile Result result;
        StableValue(ReentrantLock lock, Function<Input, Result> calculator) {
            this.lock = lock;
            this.calculator = calculator;
        }
        Result get(Input input) {
            Result result = this.result;

            if (result != null) return result;

            // Use shared lock to ensure only one StableValue can initialize at a time
            lock.lock();
            try {
                result = this.result;

                if (result != null) return result;

                result = this.calculator.apply(input);
                this.result = result;
            } finally {
                lock.unlock();
            }

            return result;
        }

        void clear() {
            this.calculator = null;
            this.result = null;
        }
    }

    private final java.lang.ClassValue<StableValue<Class<?>, T>> proxy = createStableValueProxy(lock, calculator, stableValues);

    private static <U> java.lang.ClassValue<StableValue<Class<?>, U>> createStableValueProxy(ReentrantLock lock, Function<Class<?>, U> calculator, WeakHashMap<StableValue<Class<?>, U>, Object> stableValues){
        return new java.lang.ClassValue<>() {
            @Override
            protected StableValue<Class<?>, U> computeValue(Class<?> type) {
                StableValue<Class<?>, U> stableValue = new StableValue<>(lock, calculator);
                synchronized (stableValues) {
                    stableValues.put(stableValue, null);
                }
                return stableValue;
            }
        };
    }
}
