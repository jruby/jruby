package org.jruby.util;

/**
 * Simple key-value pair object.
 * @param <K> key
 * @param <V> value
 */
public class KeyValuePair<K,V> {
    private final K key;
    private final V value;

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + "=>" + value;
    }
}
