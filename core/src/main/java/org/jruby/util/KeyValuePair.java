package org.jruby.util;

/**
 * Simple key-value pair object.
 */
public class KeyValuePair<K,V> {
    private K key;
    private V value;

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
