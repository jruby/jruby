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

import org.jruby.truffle.runtime.core.RubyHash;

import java.util.Arrays;

public abstract class BucketsStrategy {

    public static final int SIGN_BIT_MASK = ~(1 << 31);

    private static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);

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

    public static void addNewEntry(RubyHash hash, int hashed, Object key, Object value) {
        assert hash.getStore() instanceof Entry[];

        final Entry[] buckets = (Entry[]) hash.getStore();

        final Entry entry = new Entry(hashed, key, value);

        if (hash.getFirstInSequence() == null) {
            hash.setFirstInSequence(entry);
        } else {
            hash.getLastInSequence().setNextInSequence(entry);
            entry.setPreviousInSequence(hash.getLastInSequence());
        }

        hash.setLastInSequence(entry);

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

        hash.setSize(hash.getSize() + 1);

        assert HashOperations.verifyStore(hash);
    }

}
