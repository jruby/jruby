package org.jruby.javasupport.test;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * A test for ambiguous warning when same interface method is declared twice:
 * <ul>
 *     <li><link>{@link java.util.Map#compute(Object, BiFunction)}</link></li>
 *     <li><link>{@link java.util.concurrent.ConcurrentMap#compute(Object, BiFunction)}</link></li>
 * <ul/>
 *
 * @param <K>
 * @param <V>
 */
class TestConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    private final ConcurrentMap<K, V> delegate = new ConcurrentHashMap<>();

    @Override
    public V get(Object key) {
        if (key == null) {
            return null;
        }
        return delegate.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return false;
        }
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        return delegate.containsValue(value);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if (key == null) {
            return null;
        }
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException("remove(2)");
    }

    @Override
    public V replace(K key, V value) {
        throw new UnsupportedOperationException("replace(2)");
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        throw new UnsupportedOperationException("replace(3)");
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> function) {
        return delegate.compute(key, function);
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }
}
