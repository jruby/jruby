/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.hash;

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.cli.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashOperations {

    public static final int SMALL_HASH_SIZE = Options.TRUFFLE_HASHES_SMALL.load();
    public static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);
    public static final int SIGN_BIT_MASK = ~(1 << 31);

    public static int capacityGreaterThan(int size) {
        for (int capacity : CAPACITIES) {
            if (capacity > size) {
                return capacity;
            }
        }

        return CAPACITIES[CAPACITIES.length - 1];
    }

    @CompilerDirectives.SlowPath
    public static RubyHash verySlowFromEntries(RubyContext context, List<Entry> entries) {
        RubyNode.notDesignedForCompilation();

        final RubyHash hash = new RubyHash(context.getCoreLibrary().getHashClass(), null, null, null, 0, null);
        verySlowSetEntries(hash, entries);
        return hash;
    }

    public static void dump(RubyHash hash) {
        final StringBuilder builder = new StringBuilder();

        builder.append("(");

        for (Bucket bucket : (Bucket[]) hash.getStore()) {
            builder.append("(");

            while (bucket != null) {
                builder.append("[");
                builder.append(bucket.getKey());
                builder.append(",");
                builder.append(bucket.getValue());
                builder.append("]");
                bucket = bucket.getNextInLookup();
            }

            builder.append(")");
        }

        builder.append(")~>(");

        Bucket bucket = hash.getFirstInSequence();

        while (bucket != null) {
            builder.append("[");
            builder.append(bucket.getKey());
            builder.append(",");
            builder.append(bucket.getValue());
            builder.append("]");
            bucket = bucket.getNextInSequence();
        }

        builder.append(")<~(");

        bucket = hash.getLastInSequence();

        while (bucket != null) {
            builder.append("[");
            builder.append(bucket.getKey());
            builder.append(",");
            builder.append(bucket.getValue());
            builder.append("]");
            bucket = bucket.getPreviousInSequence();
        }

        builder.append(")");

        System.err.println(builder);
    }

    @CompilerDirectives.SlowPath
    public static List<Entry> verySlowToEntries(RubyHash hash) {
        final List<Entry> entries = new ArrayList<>();

        if (hash.getStore() instanceof Bucket[]) {
            Bucket bucket = hash.getFirstInSequence();

            while (bucket != null) {
                entries.add(new Entry(bucket.getKey(), bucket.getValue()));
                bucket = bucket.getNextInSequence();
            }
        } else if (hash.getStore() instanceof Object[]) {
            for (int n = 0; n < hash.getStoreSize(); n++) {
                entries.add(new Entry(((Object[]) hash.getStore())[n * 2], ((Object[]) hash.getStore())[n * 2 + 1]));
            }
        } else if (hash.getStore() != null) {
            throw new UnsupportedOperationException();
        }

        return entries;
    }

    @CompilerDirectives.SlowPath
    public static BucketSearchResult verySlowFindBucket(RubyHash hash, Object key) {
        final Object hashValue = DebugOperations.send(hash.getContext(), key, "hash", null);

        final int hashed;

        if (hashValue instanceof Integer) {
            hashed = (int) hashValue;
        } else if (hashValue instanceof Long) {
            hashed = (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }

        final Bucket[] buckets = (Bucket[]) hash.getStore();
        final int bucketIndex = (hashed & SIGN_BIT_MASK) % buckets.length;
        Bucket bucket = buckets[bucketIndex];

        Bucket endOfLookupChain = null;

        while (bucket != null) {
            // TODO: cast

            if ((boolean) DebugOperations.send(hash.getContext(), key, "eql?", null, bucket.getKey())) {
                return new BucketSearchResult(bucketIndex, bucket, bucket);
            }

            endOfLookupChain = bucket;
            bucket = bucket.getNextInLookup();
        }

        return new BucketSearchResult(bucketIndex, endOfLookupChain, null);
    }

    public static void setAtBucket(RubyHash hash, BucketSearchResult bucketSearchResult, Object key, Object value) {
        if (bucketSearchResult.getBucket() == null) {
            final Bucket bucket = new Bucket(key, value);

            if (hash.getFirstInSequence() == null) {
                hash.setFirstInSequence(bucket);
                hash.setLastInSequence(bucket);
            } else {
                hash.getLastInSequence().setNextInSequence(bucket);
                bucket.setPreviousInSequence(hash.getLastInSequence());
                hash.setLastInSequence(bucket);
            }

            if (bucketSearchResult.getEndOfLookupChain() == null) {
                ((Bucket[]) hash.getStore())[bucketSearchResult.getIndex()] = bucket;
            } else {
                bucketSearchResult.getEndOfLookupChain().setNextInLookup(bucket);
                bucket.setPreviousInLookup(bucketSearchResult.getEndOfLookupChain());
            }
        } else {
            final Bucket bucket = bucketSearchResult.getBucket();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            bucket.setKey(key);
            bucket.setValue(value);
        }
    }

    @CompilerDirectives.SlowPath
    public static boolean verySlowSetInBuckets(RubyHash hash, Object key, Object value) {
        if (key instanceof RubyString) {
            key = DebugOperations.send(hash.getContext(), DebugOperations.send(hash.getContext(), key, "dup", null), "freeze", null);
        }

        final BucketSearchResult bucketSearchResult = verySlowFindBucket(hash, key);
        setAtBucket(hash, bucketSearchResult, key, value);
        return bucketSearchResult.getBucket() == null;
    }

    @CompilerDirectives.SlowPath
    public static void verySlowSetEntries(RubyHash hash, List<Entry> entries) {
        final int size = entries.size();
        hash.setStore(new Bucket[capacityGreaterThan(size)], 0, null, null);

        int actualSize = 0;

        for (Entry entry : entries) {
            if (verySlowSetInBuckets(hash, entry.getKey(), entry.getValue())) {
                actualSize++;
            }
        }

        hash.setStoreSize(actualSize);
    }
}
