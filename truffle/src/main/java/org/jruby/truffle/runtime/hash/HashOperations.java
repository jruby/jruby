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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class HashOperations {

    public static RubyBasicObject verySlowFromEntries(RubyContext context, List<KeyValue> entries, boolean byIdentity) {
        return verySlowFromEntries(context.getCoreLibrary().getHashClass(), entries, byIdentity);
    }

    @TruffleBoundary
    public static RubyBasicObject verySlowFromEntries(RubyClass hashClass, List<KeyValue> entries, boolean byIdentity) {
        final RubyBasicObject hash = HashNodes.createEmptyHash(hashClass);
        verySlowSetKeyValues(hash, entries, byIdentity);
        assert HashOperations.verifyStore(hash);
        return hash;
    }

    @TruffleBoundary
    public static HashLookupResult verySlowFindBucket(RubyBasicObject hash, Object key, boolean byIdentity) {
        final Object hashValue = DebugOperations.send(hash.getContext(), key, "hash", null);

        final int hashed;

        if (hashValue instanceof Integer) {
            hashed = (int) hashValue;
        } else if (hashValue instanceof Long) {
            hashed = (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }

        final Entry[] entries = (Entry[]) HashNodes.getStore(hash);
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
                return new HashLookupResult(hashed, bucketIndex, previousEntry, entry);
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashLookupResult(hashed, bucketIndex, previousEntry, null);
    }

    @TruffleBoundary
    public static void verySlowSetAtBucket(RubyBasicObject hash, HashLookupResult hashLookupResult, Object key, Object value) {
        assert verifyStore(hash);

        assert HashNodes.getStore(hash) instanceof Entry[];

        if (hashLookupResult.getEntry() == null) {
            BucketsStrategy.addNewEntry(hash, hashLookupResult.getHashed(), key, value);

            assert verifyStore(hash);
        } else {
            final Entry entry = hashLookupResult.getEntry();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            entry.setHashed(hashLookupResult.getHashed());
            entry.setKey(key);
            entry.setValue(value);

            assert verifyStore(hash);
        }
    }

    @TruffleBoundary
    public static boolean verySlowSetInBuckets(RubyBasicObject hash, Object key, Object value, boolean byIdentity) {
        assert verifyStore(hash);

        if (!byIdentity && key instanceof RubyString) {
            key = DebugOperations.send(hash.getContext(), DebugOperations.send(hash.getContext(), key, "dup", null), "freeze", null);
        }

        final HashLookupResult hashLookupResult = verySlowFindBucket(hash, key, byIdentity);
        verySlowSetAtBucket(hash, hashLookupResult, key, value);

        assert verifyStore(hash);

        return hashLookupResult.getEntry() == null;
    }

    @TruffleBoundary
    public static void verySlowSetKeyValues(RubyBasicObject hash, Iterable<Map.Entry<Object, Object>> keyValues, boolean byIdentity) {
        final List<KeyValue> converted = new ArrayList<>();

        for (Map.Entry<Object, Object> keyValue : keyValues) {
            converted.add(new KeyValue(keyValue.getKey(), keyValue.getValue()));
        }

        verySlowSetKeyValues(hash, converted, byIdentity);
    }

    @TruffleBoundary
    public static void verySlowSetKeyValues(RubyBasicObject hash, List<KeyValue> keyValues, boolean byIdentity) {
        assert verifyStore(hash);

        final int size = keyValues.size();
        HashNodes.setStore(hash, new Entry[BucketsStrategy.capacityGreaterThan(size)], 0, null, null);

        int actualSize = 0;

        for (KeyValue keyValue : keyValues) {
            if (verySlowSetInBuckets(hash, keyValue.getKey(), keyValue.getValue(), byIdentity)) {
                actualSize++;
            }
        }

        HashNodes.setSize(hash, actualSize);

        assert verifyStore(hash);
    }

    public static boolean verifyStore(RubyBasicObject hash) {
        return verifyStore(HashNodes.getStore(hash), HashNodes.getSize(hash), HashNodes.getFirstInSequence(hash), HashNodes.getLastInSequence(hash));
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
