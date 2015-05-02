/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;

import java.util.ArrayList;
import java.util.List;

public class HashOperations {

    public static RubyHash verySlowFromEntries(RubyContext context, List<KeyValue> entries, boolean byIdentity) {
        return verySlowFromEntries(context.getCoreLibrary().getHashClass(), entries, byIdentity);
    }

    @CompilerDirectives.TruffleBoundary
    public static RubyHash verySlowFromEntries(RubyClass hashClass, List<KeyValue> entries, boolean byIdentity) {
        final RubyHash hash = new RubyHash(hashClass, null, null, null, 0, null);
        verySlowSetKeyValues(hash, entries, byIdentity);
        assert HashOperations.verifyStore(hash);
        return hash;
    }

    public static void dump(RubyHash hash) {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        builder.append(hash.getSize());
        builder.append("](");

        for (Entry entry : (Entry[]) hash.getStore()) {
            builder.append("(");

            while (entry != null) {
                builder.append("[");
                builder.append(entry.getKey());
                builder.append(",");
                builder.append(entry.getValue());
                builder.append("]");
                entry = entry.getNextInLookup();
            }

            builder.append(")");
        }

        builder.append(")~>(");

        Entry entry = hash.getFirstInSequence();

        while (entry != null) {
            builder.append("[");
            builder.append(entry.getKey());
            builder.append(",");
            builder.append(entry.getValue());
            builder.append("]");
            entry = entry.getNextInSequence();
        }

        builder.append(")<~(");

        entry = hash.getLastInSequence();

        while (entry != null) {
            builder.append("[");
            builder.append(entry.getKey());
            builder.append(",");
            builder.append(entry.getValue());
            builder.append("]");
            entry = entry.getPreviousInSequence();
        }

        builder.append(")");

        System.err.println(builder);
    }

    @CompilerDirectives.TruffleBoundary
    public static List<KeyValue> verySlowToKeyValues(RubyHash hash) {
        final List<KeyValue> keyValues = new ArrayList<>();

        if (hash.getStore() instanceof Entry[]) {
            Entry entry = hash.getFirstInSequence();

            while (entry != null) {
                keyValues.add(new KeyValue(entry.getKey(), entry.getValue()));
                entry = entry.getNextInSequence();
            }
        } else if (hash.getStore() instanceof Object[]) {
            final Object[] store = (Object[]) hash.getStore();
            for (int n = 0; n < hash.getSize(); n++) {
                keyValues.add(new KeyValue(PackedArrayStrategy.getKey(store, n), PackedArrayStrategy.getValue(store, n)));
            }
        } else if (hash.getStore() != null) {
            throw new UnsupportedOperationException();
        }

        return keyValues;
    }

    @CompilerDirectives.TruffleBoundary
    public static HashSearchResult verySlowFindBucket(RubyHash hash, Object key, boolean byIdentity) {
        final Object hashValue = DebugOperations.send(hash.getContext(), key, "hash", null);

        final int hashed;

        if (hashValue instanceof Integer) {
            hashed = (int) hashValue;
        } else if (hashValue instanceof Long) {
            hashed = (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }

        final Entry[] entries = (Entry[]) hash.getStore();
        final int bucketIndex = (hashed & BucketsStrategy.SIGN_BIT_MASK) % entries.length;
        Entry entry = entries[bucketIndex];

        Entry previousEntry = null;

        while (entry != null) {
            // TODO: cast
            
            final String method;
            
            if (byIdentity) {
                method = "equal?";
            } else {
                method = "eql?";
            }

            if ((boolean) DebugOperations.send(hash.getContext(), key, method, null, entry.getKey())) {
                return new HashSearchResult(hashed, bucketIndex, previousEntry, entry);
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashSearchResult(hashed, bucketIndex, previousEntry, null);
    }

    @CompilerDirectives.TruffleBoundary
    public static void verySlowSetAtBucket(RubyHash hash, HashSearchResult hashSearchResult, Object key, Object value) {
        assert verifyStore(hash);

        assert hash.getStore() instanceof Entry[];

        if (hashSearchResult.getEntry() == null) {
            BucketsStrategy.addNewEntry(hash, hashSearchResult.getHashed(), key, value);

            assert verifyStore(hash);
        } else {
            final Entry entry = hashSearchResult.getEntry();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            entry.setHashed(hashSearchResult.getHashed());
            entry.setKey(key);
            entry.setValue(value);

            assert verifyStore(hash);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static boolean verySlowSetInBuckets(RubyHash hash, Object key, Object value, boolean byIdentity) {
        assert verifyStore(hash);

        if (!byIdentity && key instanceof RubyString) {
            key = DebugOperations.send(hash.getContext(), DebugOperations.send(hash.getContext(), key, "dup", null), "freeze", null);
        }

        final HashSearchResult hashSearchResult = verySlowFindBucket(hash, key, byIdentity);
        verySlowSetAtBucket(hash, hashSearchResult, key, value);

        assert verifyStore(hash);

        return hashSearchResult.getEntry() == null;
    }

    @CompilerDirectives.TruffleBoundary
    public static void verySlowSetKeyValues(RubyHash hash, List<KeyValue> keyValues, boolean byIdentity) {
        assert verifyStore(hash);

        final int size = keyValues.size();
        hash.setStore(new Entry[BucketsStrategy.capacityGreaterThan(size)], 0, null, null);

        int actualSize = 0;

        for (KeyValue keyValue : keyValues) {
            if (verySlowSetInBuckets(hash, keyValue.getKey(), keyValue.getValue(), byIdentity)) {
                actualSize++;
            }
        }

        hash.setSize(actualSize);

        assert verifyStore(hash);
    }

    public static boolean verifyStore(RubyHash hash) {
        return verifyStore(hash.getStore(), hash.getSize(), hash.getFirstInSequence(), hash.getLastInSequence());
    }

    public static boolean verifyStore(Object store, int size, Entry firstInSequence, Entry lastInSequence) {
        assert store == null || store instanceof Object[] || store instanceof Entry[];

        if (store == null) {
            assert size == 0;
            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        if (store instanceof Entry[]) {
            assert lastInSequence == null || lastInSequence.getNextInSequence() == null;

            final Entry[] entryStore = (Entry[]) store;

            Entry foundFirst = null;
            Entry foundLast = null;
            int foundSizeBuckets = 0;

            for (int n = 0; n < entryStore.length; n++) {
                Entry entry = entryStore[n];

                while (entry != null) {
                    foundSizeBuckets++;

                    if (entry == firstInSequence) {
                        assert foundFirst == null;
                        foundFirst = entry;
                    }

                    if (entry == lastInSequence) {
                        assert foundLast == null;
                        foundLast = entry;
                    }

                    entry = entry.getNextInLookup();
                }
            }

            assert foundSizeBuckets == size;
            assert firstInSequence == foundFirst;
            assert lastInSequence == foundLast;

            int foundSizeSequence = 0;
            Entry entry = firstInSequence;

            while (entry != null) {
                foundSizeSequence++;

                if (entry.getNextInSequence() == null) {
                    assert entry == lastInSequence;
                } else {
                    assert entry.getNextInSequence().getPreviousInSequence() == entry;
                }

                entry = entry.getNextInSequence();

                assert entry != firstInSequence;
            }

            assert foundSizeSequence == size : String.format("%d %d", foundSizeSequence, size);
        } else if (store instanceof Object[]) {
            assert ((Object[]) store).length == PackedArrayStrategy.MAX_ENTRIES * PackedArrayStrategy.ELEMENTS_PER_ENTRY : ((Object[]) store).length;

            final Object[] packedStore = (Object[]) store;

            for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
                if (n < size) {
                    assert packedStore[n * 2] != null;
                    assert packedStore[n * 2 + 1] != null;
                }
            }

            assert firstInSequence == null;
            assert lastInSequence == null;
        }

        return true;
    }

}
