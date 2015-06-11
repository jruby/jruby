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
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;

import java.util.*;

public abstract class BucketsStrategy {

    // If the size is more than this fraction of the number of buckets, resize
    public static final double LOAD_FACTOR = 0.75;

    // Create this many more buckets than there are entries when resizing or creating from scratch
    public static final int RESIZE_FACTOR = 4;

    public static final int SIGN_BIT_MASK = ~(1 << 31);

    private static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);

    public static RubyBasicObject create(RubyClass hashClass, Collection<Map.Entry<Object, Object>> entries, boolean byIdentity) {
        int actualSize = entries.size();

        final int bucketsCount = capacityGreaterThan(entries.size()) * RESIZE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry firstInSequence = null;
        Entry lastInSequence = null;

        for (Map.Entry<Object, Object> entry : entries) {
            Object key = entry.getKey();

            if (!byIdentity && RubyGuards.isRubyString(key)) {
                key = DebugOperations.send(hashClass.getContext(), DebugOperations.send(hashClass.getContext(), key, "dup", null), "freeze", null);
            }

            final int hashed = HashOperations.hashKey(hashClass.getContext(), key);
            Entry newEntry = new Entry(hashed, key, entry.getValue());

            final int index = BucketsStrategy.getBucketIndex(hashed, newEntries.length);
            Entry bucketEntry = newEntries[index];

            if (bucketEntry == null) {
                newEntries[index] = newEntry;
            } else {
                Entry previousInBucket = null;

                while (bucketEntry != null) {
                    if (hashed == bucketEntry.getHashed()
                            && HashOperations.areKeysEqual(hashClass.getContext(), bucketEntry.getKey(), key, byIdentity)) {
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

    public static void addNewEntry(RubyBasicObject hash, int hashed, Object key, Object value) {
        assert HashNodes.getStore(hash) instanceof Entry[];

        final Entry[] buckets = (Entry[]) HashNodes.getStore(hash);

        final Entry entry = new Entry(hashed, key, value);

        if (HashNodes.getFirstInSequence(hash) == null) {
            HashNodes.setFirstInSequence(hash, entry);
        } else {
            HashNodes.getLastInSequence(hash).setNextInSequence(entry);
            entry.setPreviousInSequence(HashNodes.getLastInSequence(hash));
        }

        HashNodes.setLastInSequence(hash, entry);

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

        HashNodes.setSize(hash, HashNodes.getSize(hash) + 1);

        assert HashOperations.verifyStore(hash);
    }

    @TruffleBoundary
    public static void resize(RubyBasicObject hash) {
        assert HashOperations.verifyStore(hash);

        final int bucketsCount = capacityGreaterThan(HashNodes.getSize(hash)) * RESIZE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry entry = HashNodes.getFirstInSequence(hash);

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

        HashNodes.setStore(hash, newEntries, HashNodes.getSize(hash), HashNodes.getFirstInSequence(hash), HashNodes.getLastInSequence(hash));

        assert HashOperations.verifyStore(hash);
    }

    @TruffleBoundary
    public static Iterator<Map.Entry<Object, Object>> iterateKeyValues(final Entry firstInSequence) {
        return new Iterator<Map.Entry<Object, Object>>() {

            private Entry entry = firstInSequence;

            @Override
            public boolean hasNext() {
                return entry != null;
            }

            @Override
            public Map.Entry<Object, Object> next() {
                if (entry == null) {
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

        };
    }

    @TruffleBoundary
    public static Iterable<Map.Entry<Object, Object>> iterableKeyValues(final Entry firstInSequence) {
        return new Iterable<Map.Entry<Object, Object>>() {

            @Override
            public Iterator<Map.Entry<Object, Object>> iterator() {
                return iterateKeyValues(firstInSequence);
            }

        };
    }

}
