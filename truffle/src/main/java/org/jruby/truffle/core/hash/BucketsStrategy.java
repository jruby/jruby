/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyGuards;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class BucketsStrategy {

    // If the size is more than this fraction of the number of buckets, resize
    public static final double LOAD_FACTOR = 0.75;

    // Create this many more buckets than there are entries when resizing or creating from scratch
    public static final int OVERALLOCATE_FACTOR = 4;

    public static final int SIGN_BIT_MASK = ~(1 << 31);

    private static final int MRI_PRIMES[] = {
                    8 + 3, 16 + 3, 32 + 5, 64 + 3, 128 + 3, 256 + 27, 512 + 9, 1024 + 9, 2048 + 5, 4096 + 3,
                    8192 + 27, 16384 + 43, 32768 + 3, 65536 + 45, 131072 + 29, 262144 + 3, 524288 + 21, 1048576 + 7,
                    2097152 + 17, 4194304 + 15, 8388608 + 9, 16777216 + 43, 33554432 + 35, 67108864 + 15,
                    134217728 + 29, 268435456 + 3, 536870912 + 11, 1073741824 + 85
    };

    private static final int[] CAPACITIES = MRI_PRIMES;

    @TruffleBoundary
    public static DynamicObject create(RubyContext context, Collection<KeyValue> entries, boolean byIdentity) {
        int actualSize = entries.size();

        final int bucketsCount = capacityGreaterThan(entries.size()) * OVERALLOCATE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry firstInSequence = null;
        Entry lastInSequence = null;

        for (KeyValue entry : entries) {
            Object key = entry.getKey();

            if (!byIdentity && RubyGuards.isRubyString(key)) {
                key = context.send(context.send(key, "dup", null), "freeze", null);
            }

            final int hashed = hashKey(context, key);
            Entry newEntry = new Entry(hashed, key, entry.getValue());

            final int index = BucketsStrategy.getBucketIndex(hashed, newEntries.length);
            Entry bucketEntry = newEntries[index];

            if (bucketEntry == null) {
                newEntries[index] = newEntry;
            } else {
                Entry previousInBucket = null;

                while (bucketEntry != null) {
                    if (hashed == bucketEntry.getHashed()
                            && areKeysEqual(context, bucketEntry.getKey(), key, byIdentity)) {
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

        return Layouts.HASH.createHash(context.getCoreLibrary().getHashFactory(), newEntries, actualSize, firstInSequence, lastInSequence, null, null, false);
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

    public static void addNewEntry(RubyContext context, DynamicObject hash, int hashed, Object key, Object value) {
        assert HashGuards.isBucketHash(hash);
        assert HashOperations.verifyStore(context, hash);

        final Entry[] buckets = (Entry[]) Layouts.HASH.getStore(hash);

        final Entry entry = new Entry(hashed, key, value);

        if (Layouts.HASH.getFirstInSequence(hash) == null) {
            Layouts.HASH.setFirstInSequence(hash, entry);
        } else {
            Layouts.HASH.getLastInSequence(hash).setNextInSequence(entry);
            entry.setPreviousInSequence(Layouts.HASH.getLastInSequence(hash));
        }

        Layouts.HASH.setLastInSequence(hash, entry);

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

        Layouts.HASH.setSize(hash, Layouts.HASH.getSize(hash) + 1);

        assert HashOperations.verifyStore(context, hash);
    }

    @TruffleBoundary
    public static void resize(RubyContext context, DynamicObject hash) {
        assert HashGuards.isBucketHash(hash);
        assert HashOperations.verifyStore(context, hash);

        final int bucketsCount = capacityGreaterThan(Layouts.HASH.getSize(hash)) * OVERALLOCATE_FACTOR;
        final Entry[] newEntries = new Entry[bucketsCount];

        Entry entry = Layouts.HASH.getFirstInSequence(hash);

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

        int size = Layouts.HASH.getSize(hash);
        Entry firstInSequence = Layouts.HASH.getFirstInSequence(hash);
        Entry lastInSequence = Layouts.HASH.getLastInSequence(hash);
        assert HashOperations.verifyStore(context, newEntries, size, firstInSequence, lastInSequence);
        Layouts.HASH.setStore(hash, newEntries);
        Layouts.HASH.setSize(hash, size);
        Layouts.HASH.setFirstInSequence(hash, firstInSequence);
        Layouts.HASH.setLastInSequence(hash, lastInSequence);

        assert HashOperations.verifyStore(context, hash);
    }

    public static Iterator<KeyValue> iterateKeyValues(final Entry firstInSequence) {
        return new Iterator<KeyValue>() {

            private Entry entry = firstInSequence;

            @Override
            public boolean hasNext() {
                return entry != null;
            }

            @Override
            public KeyValue next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final KeyValue entryResult = new KeyValue(entry.getKey(), entry.getValue());

                entry = entry.getNextInSequence();

                return entryResult;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public static Iterable<KeyValue> iterableKeyValues(final Entry firstInSequence) {
        return () -> iterateKeyValues(firstInSequence);
    }

    public static void copyInto(RubyContext context, DynamicObject from, DynamicObject to) {
        assert RubyGuards.isRubyHash(from);
        assert HashGuards.isBucketHash(from);
        assert HashOperations.verifyStore(context, from);
        assert RubyGuards.isRubyHash(to);
        assert HashOperations.verifyStore(context, to);

        final Entry[] newEntries = new Entry[((Entry[]) Layouts.HASH.getStore(from)).length];

        Entry firstInSequence = null;
        Entry lastInSequence = null;

        Entry entry = Layouts.HASH.getFirstInSequence(from);

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

        int size = Layouts.HASH.getSize(from);
        assert HashOperations.verifyStore(context, newEntries, size, firstInSequence, lastInSequence);
        Layouts.HASH.setStore(to, newEntries);
        Layouts.HASH.setSize(to, size);
        Layouts.HASH.setFirstInSequence(to, firstInSequence);
        Layouts.HASH.setLastInSequence(to, lastInSequence);

    }

    private static int hashKey(RubyContext context, Object key) {
        final Object hashValue = context.send(key, "hash", null);

        if (hashValue instanceof Integer) {
            return (int) hashValue;
        } else if (hashValue instanceof Long) {
            return (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean areKeysEqual(RubyContext context, Object a, Object b, boolean byIdentity) {
        final String method;

        if (byIdentity) {
            method = "equal?";
        } else {
            method = "eql?";
        }

        final Object equalityResult = context.send(a, method, null, b);

        if (equalityResult instanceof Boolean) {
            return (boolean) equalityResult;
        }

        throw new UnsupportedOperationException();
    }

}
