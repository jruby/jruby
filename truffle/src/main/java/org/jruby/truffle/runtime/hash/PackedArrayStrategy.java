/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.util.cli.Options;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class PackedArrayStrategy {

    public static final int MAX_ENTRIES = Options.TRUFFLE_HASH_PACKED_ARRAY_MAX.load();
    public static final int ELEMENTS_PER_ENTRY = 3;
    public static final int MAX_ELEMENTS = MAX_ENTRIES * ELEMENTS_PER_ENTRY;

    public static Object[] createStore(int hashed, Object key, Object value) {
        final Object[] store = createStore();
        setHashedKeyValue(store, 0, hashed, key, value);
        return store;
    }

    public static Object[] createStore() {
        return new Object[MAX_ELEMENTS];
    }

    public static Object[] copyStore(Object[] store) {
        final Object[] copied = createStore();
        System.arraycopy(store, 0, copied, 0, MAX_ELEMENTS);
        return copied;
    }

    public static int getHashed(Object[] store, int n) {
        return (int) store[n * ELEMENTS_PER_ENTRY];
    }

    public static Object getKey(Object[] store, int n) {
        return store[n * ELEMENTS_PER_ENTRY + 1];
    }

    public static Object getValue(Object[] store, int n) {
        return store[n * ELEMENTS_PER_ENTRY + 2];
    }

    public static void setHashed(Object[] store, int n, int hashed) {
        store[n * ELEMENTS_PER_ENTRY] = hashed;
    }

    public static void setKey(Object[] store, int n, Object key) {
        store[n * ELEMENTS_PER_ENTRY + 1] = key;
    }

    public static void setValue(Object[] store, int n, Object value) {
        store[n * ELEMENTS_PER_ENTRY + 2] = value;
    }

    public static void setHashedKeyValue(Object[] store, int n, int hashed, Object key, Object value) {
        setHashed(store, n, hashed);
        setKey(store, n, key);
        setValue(store, n, value);
    }

    public static void removeEntry(Object[] store, int n) {
        for (int i = 0; i < MAX_ELEMENTS; i += ELEMENTS_PER_ENTRY) {
            assert store[i] == null || store[i] instanceof Integer;
        }

        final int index = n * ELEMENTS_PER_ENTRY;
        System.arraycopy(store, index + ELEMENTS_PER_ENTRY, store, index, MAX_ELEMENTS - ELEMENTS_PER_ENTRY - index);

        for (int i = 0; i < MAX_ELEMENTS; i += ELEMENTS_PER_ENTRY) {
            assert store[i] == null || store[i] instanceof Integer;
        }
    }

    @TruffleBoundary
    public static void promoteToBuckets(RubyBasicObject hash, Object[] store, int size) {
        final Entry[] buckets = new Entry[BucketsStrategy.capacityGreaterThan(size)];

        Entry firstInSequence = null;
        Entry previousInSequence = null;
        Entry lastInSequence = null;

        for (int n = 0; n < size; n++) {
            final int hashed = getHashed(store, n);
            final Entry entry = new Entry(hashed, getKey(store, n), getValue(store, n));

            if (previousInSequence == null) {
                firstInSequence = entry;
            } else {
                previousInSequence.setNextInSequence(entry);
                entry.setPreviousInSequence(previousInSequence);
            }

            previousInSequence = entry;
            lastInSequence = entry;

            final int bucketIndex = BucketsStrategy.getBucketIndex(hashed, buckets.length);

            Entry previousInLookup = buckets[bucketIndex];

            if (previousInLookup == null) {
                buckets[bucketIndex] = entry;
            } else {
                while (previousInLookup.getNextInLookup() != null) {
                    previousInLookup = previousInLookup.getNextInLookup();
                }

                previousInLookup.setNextInLookup(entry);
            }
        }

        HashNodes.setStore(hash, buckets, size, firstInSequence, lastInSequence);

        assert HashNodes.verifyStore(hash);
    }

    @TruffleBoundary
    public static Iterator<Map.Entry<Object, Object>> iterateKeyValues(final Object[] store, final int size) {
        return new Iterator<Map.Entry<Object, Object>>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public Map.Entry<Object, Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final int finalIndex = index;

                final Map.Entry<Object, Object> entryResult = new Map.Entry<Object, Object>() {

                    @Override
                    public Object getKey() {
                        return PackedArrayStrategy.getKey(store, finalIndex);
                    }

                    @Override
                    public Object getValue() {
                        return PackedArrayStrategy.getValue(store, finalIndex);
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }

                };

                index++;

                return entryResult;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

}
