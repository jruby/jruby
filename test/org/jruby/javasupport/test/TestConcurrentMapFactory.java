package org.jruby.javasupport.test;

import java.util.concurrent.ConcurrentMap;

public abstract class TestConcurrentMapFactory {
    public static <K, V> ConcurrentMap<K, V> newMap() {
        return new TestConcurrentMap<>();
    }
}
