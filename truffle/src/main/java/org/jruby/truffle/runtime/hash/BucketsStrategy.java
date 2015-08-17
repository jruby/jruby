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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.hash.HashGuards;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.DebugOperations;

import java.util.*;

public abstract class BucketsStrategy {

    // If the size is more than this fraction of the number of buckets, resize
    public static final double LOAD_FACTOR = 0.75;

    // Create this many more buckets than there are entries when resizing or creating from scratch
    public static final int OVERALLOCATE_FACTOR = 4;

    public static final int SIGN_BIT_MASK = ~(1 << 31);

    private static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);

    public static DynamicObject create(DynamicObject hashClass, int capacity) {
        final int bucketsCount = capacityGreaterThan(capacity) * OVERALLOCATE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        return HashNodes.createHash(hashClass, null, null, newEntries, 0, null, null);
    }

    public static DynamicObject create(DynamicObject hashClass, Collection<Map.Entry<Object, Object>> entries, boolean byIdentity) {
        int actualSize = entries.size();

        final int bucketsCount = capacityGreaterThan(entries.size()) * OVERALLOCATE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry firstInSequence = null;
        Entry lastInSequence = null;

        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();

            if (!byIdentity && RubyGuards.isRubyString(key)) {
                key = DebugOperations.send(BasicObjectNodes.getContext(hashClass), DebugOperations.send(BasicObjectNodes.getContext(hashClass), key, "dup", null), "freeze", null);
            }

            final int hashed = HashNodes.slowHashKey(BasicObjectNodes.getContext(hashClass), key);
            Entry newEntry = new Entry(hashed, key, entry.getValue());

            final int index = BucketsStrategy.getBucketIndex(hashed, newEntries.length);
            Entry bucketEntry = newEntries[index];

            if (bucketEntry == null) {
                newEntries[index] = newEntry;
            } else {
                Entry previousInBucket = null;

                while (bucketEntry != null) {
                    if (hashed == bucketEntry.getHashed()
                            && HashNodes.slowAreKeysEqual(BasicObjectNodes.getContext(hashClass), bucketEntry.getKey(), key, byIdentity)) {
                        bucketEntry.setValue(entry.getValue());

                        actualSize--;

                        if (bucketEntry.getPreviousInSequence() != null) {
                            bucketEntry.getPreviousInSequence().setNextInSequence(bucketEntry.getNextInSequence());
                        }

                        if (bucketEntry.getNextInSequence() != null) {
                            bucketEntry.getNextInSequence().setPreviousInSequence(bucketEntry.getPreviousInSequence());
                        }

                        if (bucketEntry == lastInSequence) {
                            lastInSequence = bucketEntry.getPreviousInSequence();
                        }

                        // We wasted by allocating newEntry, but never mind
                        newEntry = bucketEntry;
                        previousInBucket = null;

                        break;
                    }

                    previousInBucket = bucketEntry;
                    bucketEntry = bucketEntry.getNextInLookup();
                }

                if (previousInBucket != null) {
                    previousInBucket.setNextInLookup(newEntry);
                }
            }

            if (firstInSequence == null) {
                firstInSequence = newEntry;
            }

            if (lastInSequence != null) {
                lastInSequence.setNextInSequence(newEntry);
            }

            newEntry.setPreviousInSequence(lastInSequence);
            newEntry.setNextInSequence(null);

            lastInSequence = newEntry;
        }

        return HashNodes.createHash(hashClass, null, null, newEntries, actualSize, firstInSequence, lastInSequence);
    }

    public static int capacityGreaterThan(int size) {
        for (int capacity : CAPACITIES) {
            if (capacity > size) {
                return capacity;
            }
        }

        return CAPACITIES[CAPACITIES.length - 1];
    }

    public static int getBucketIndex(int hashed, int bucketsCount) {
        return (hashed & SIGN_BIT_MASK) % bucketsCount;
    }

    public static void addNewEntry(DynamicObject hash, int hashed, Object key, Object value) {
        assert HashGuards.isBucketHash(hash);
        assert HashNodes.verifyStore(hash);

        final Entry[] buckets = (Entry[]) HashNodes.HASH_LAYOUT.getStore(hash);

        final Entry entry = new Entry(hashed, key, value);

        if (HashNodes.HASH_LAYOUT.getFirstInSequence(hash) == null) {
            HashNodes.HASH_LAYOUT.setFirstInSequence(hash, entry);
        } else {
            HashNodes.HASH_LAYOUT.getLastInSequence(hash).setNextInSequence(entry);
            entry.setPreviousInSequence(HashNodes.HASH_LAYOUT.getLastInSequence(hash));
        }

        HashNodes.HASH_LAYOUT.setLastInSequence(hash, entry);

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

        HashNodes.HASH_LAYOUT.setSize(hash, HashNodes.HASH_LAYOUT.getSize(hash) + 1);

        assert HashNodes.verifyStore(hash);
    }

    @TruffleBoundary
    public static void resize(DynamicObject hash) {
        assert HashGuards.isBucketHash(hash);
        assert HashNodes.verifyStore(hash);

        final int bucketsCount = capacityGreaterThan(HashNodes.HASH_LAYOUT.getSize(hash)) * OVERALLOCATE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry entry = HashNodes.HASH_LAYOUT.getFirstInSequence(hash);

        while (entry != null) {
            final int bucketIndex = getBucketIndex(entry.getHashed(), bucketsCount);
            Entry previousInLookup = newEntries[bucketIndex];

            if (previousInLookup == null) {
                newEntries[bucketIndex] = entry;
            } else {
                while (previousInLookup.getNextInLookup() != null) {
                    previousInLookup = previousInLookup.getNextInLookup();
                }

                previousInLookup.setNextInLookup(entry);
            }

            entry.setNextInLookup(null);
            entry = entry.getNextInSequence();
        }

        HashNodes.setStore(hash, newEntries, HashNodes.HASH_LAYOUT.getSize(hash), HashNodes.HASH_LAYOUT.getFirstInSequence(hash), HashNodes.HASH_LAYOUT.getLastInSequence(hash));

        assert HashNodes.verifyStore(hash);
    }

    public static Iterator<Map.Entry<Object, Object>> iterateKeyValues(final Entry firstInSequence) {
        return new Iterator<Map.Entry<Object, Object>>() {

            private Entry entry = firstInSequence;

            @Override
            public boolean hasNext() {
                return entry != null;
            }

            @Override
            public Map.Entry<Object, Object> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final Entry finalEntry = entry;

                final Map.Entry<Object, Object> entryResult = new Map.Entry<Object, Object>() {

                    @Override
                    public Object getKey() {
                        return finalEntry.getKey();
                    }

                    @Override
                    public Object getValue() {
                        return finalEntry.getValue();
                    }

                    @Override
                    public Object setValue(Object value) {
                        throw new UnsupportedOperationException();
                    }

                };

                entry = entry.getNextInSequence();

                return entryResult;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public static Iterable<Map.Entry<Object, Object>> iterableKeyValues(final Entry firstInSequence) {
        return new Iterable<Map.Entry<Object, Object>>() {

            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                return iterateKeyValues(firstInSequence);
            }

        };
    }

    public static void copyInto(DynamicObject from, DynamicObject to) {
        assert RubyGuards.isRubyHash(from);
        assert HashGuards.isBucketHash(from);
        assert HashNodes.verifyStore(from);
        assert RubyGuards.isRubyHash(to);
        assert HashNodes.verifyStore(to);

        final Entry[] newEntries = new Entry[((Entry[]) HashNodes.HASH_LAYOUT.getStore(from)).length];

        Entry firstInSequence = null;
        Entry lastInSequence = null;

        Entry entry = HashNodes.HASH_LAYOUT.getFirstInSequence(from);

        while (entry != null) {
            final Entry newEntry = new Entry(entry.getHashed(), entry.getKey(), entry.getValue());

            final int index = BucketsStrategy.getBucketIndex(entry.getHashed(), newEntries.length);

            newEntry.setNextInLookup(newEntries[index]);
            newEntries[index] = newEntry;

            if (firstInSequence == null) {
                firstInSequence = newEntry;
            }

            if (lastInSequence != null) {
                lastInSequence.setNextInSequence(newEntry);
                newEntry.setPreviousInSequence(lastInSequence);
            }

            lastInSequence = newEntry;

            entry = entry.getNextInSequence();
        }

        HashNodes.setStore(to, newEntries, HashNodes.HASH_LAYOUT.getSize(from), firstInSequence, lastInSequence);
    }

}
