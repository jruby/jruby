/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.util.cli.Options;

/**
 * Represents the Ruby {@code Hash} class.
 */
public class RubyHash extends RubyBasicObject {

    // TODO(CS): I think we need to salt the hash somehow - there have been DOS attacks on Ruby for that in the past

    public static final int HASHES_SMALL = Options.TRUFFLE_HASHES_SMALL.load();

    public static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);

    private static final int SIGN_BIT_MASK = ~(1 << 31);

    /**
     * The class from which we create the object that is {@code Hash}. A subclass of
     * {@link org.jruby.truffle.runtime.core.RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyHash} rather than a normal {@link org.jruby.truffle.runtime.core.RubyBasicObject}.
     */
    public static class RubyHashClass extends RubyClass {

        public RubyHashClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Hash");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyHash(this, null, null, null, 0, null);
        }

    }

    private RubyProc defaultBlock;
    private Object defaultValue;
    private Object store;
    private int storeSize;
    public Bucket firstInSequence;
    public Bucket lastInSequence;

    public RubyHash(RubyClass rubyClass, RubyProc defaultBlock, Object defaultValue, Object store, int storeSize, Bucket firstInSequence) {
        super(rubyClass);

        final boolean isObjectArray = store instanceof Object[] && !(store instanceof RubyHash.Bucket[]);
        assert store == null || store instanceof Object[] || store instanceof Bucket[] : store.getClass();
        assert !isObjectArray || ((Object[]) store).length == HASHES_SMALL * 2 : store.getClass();
        assert !isObjectArray || storeSize <= HASHES_SMALL : store.getClass();
        assert !isObjectArray || (firstInSequence == null) : store.getClass();

        this.defaultBlock = defaultBlock;
        this.defaultValue = defaultValue;
        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
    }

    public RubyProc getDefaultBlock() {
        return defaultBlock;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object getStore() {
        return store;
    }

    public int getStoreSize() {
        return storeSize;
    }

    public void setDefaultBlock(RubyProc defaultBlock) {
        this.defaultBlock = defaultBlock;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setStore(Object store, int storeSize, Bucket firstInSequence, Bucket lastInSequence) {
        final boolean isObjectArray = store instanceof Object[] && !(store instanceof RubyHash.Bucket[]);
        assert store == null || store instanceof Object[] || store instanceof Bucket[] : store.getClass();
        assert !isObjectArray || ((Object[]) store).length == HASHES_SMALL * 2 : store.getClass();
        assert !isObjectArray || storeSize <= HASHES_SMALL : store.getClass();
        assert !isObjectArray || (firstInSequence == null) : store.getClass();

        this.store = store;
        this.storeSize = storeSize;
        this.firstInSequence = firstInSequence;
        this.lastInSequence = lastInSequence;
    }

    public void setStoreSize(int storeSize) {
        this.storeSize = storeSize;
    }

    public List<Entry> verySlowToEntries() {
        final List<Entry> entries = new ArrayList<>();

        if (store instanceof Bucket[]) {
            Bucket bucket = firstInSequence;

            while (bucket != null) {
                entries.add(new Entry(bucket.key, bucket.value));
                bucket = bucket.nextInSequence;
            }
        } else if (store instanceof Object[]) {
            for (int n = 0; n < storeSize; n++) {
                entries.add(new Entry(((Object[]) store)[n * 2], ((Object[]) store)[n * 2 + 1]));
            }
        } else if (store != null) {
            throw new UnsupportedOperationException();
        }

        return entries;
    }

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (RubyHash.Entry entry : verySlowToEntries()) {
            if (entry.getKey() instanceof RubyBasicObject) {
                ((RubyBasicObject) entry.getKey()).visitObjectGraph(visitor);
            }

            if (entry.getValue() instanceof RubyBasicObject) {
                ((RubyBasicObject) entry.getValue()).visitObjectGraph(visitor);
            }
        }
    }

    public static class BucketAndIndex {

        private final Bucket bucket;
        private final int index;
        private final Bucket endOfLookupChain;

        public BucketAndIndex(Bucket bucket, int index, Bucket endOfLookupChain) {
            this.bucket = bucket;
            this.index = index;
            this.endOfLookupChain = endOfLookupChain;
        }

        public Bucket getBucket() {
            return bucket;
        }

        public int getIndex() {
            return index;
        }

        public Bucket getEndOfLookupChain() {
            return endOfLookupChain;
        }
    }

    public static class Entry {

        private final Object key;
        private final Object value;

        public Entry(Object key, Object value) {
            assert key != null;
            assert value != null;

            this.key = key;
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public Object getKey() {
            return key;
        }

    }

    public static class Bucket {

        public Object key;
        public Object value;

        public Bucket previousInLookup;
        public Bucket nextInLookup;

        public Bucket previousInSequence;
        public Bucket nextInSequence;

    }

    public static int capacityGreaterThan(int size) {
        for (int capacity : CAPACITIES) {
            if (capacity > size) {
                return capacity;
            }
        }

        return CAPACITIES[CAPACITIES.length - 1];
    }

    @CompilerDirectives.SlowPath
    public BucketAndIndex verySlowFindBucket(Object key) {
        final Bucket[] buckets = (Bucket[]) store;

        // Hash

        // TODO: cast

        final Object hashValue = DebugOperations.send(getContext(), key, "hash", null);

        final int hashed;

        if (hashValue instanceof Integer) {
            hashed = (int) hashValue;
        } else if (hashValue instanceof Long) {
            hashed = (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }

        final int bucketIndex = (hashed & SIGN_BIT_MASK) % buckets.length;

        // Find the initial bucket

        Bucket bucket = buckets[bucketIndex];

        // Go through the chain of buckets to see if we're going to overwrite a key or append a new bucket

        Bucket endOfLookupChain = null;

        while (bucket != null) {
            // TODO: cast

            if ((boolean) DebugOperations.send(getContext(), key, "eql?", null, bucket.key)) {
                return new BucketAndIndex(bucket, bucketIndex, bucket);
            }

            endOfLookupChain = bucket;
            bucket = bucket.nextInLookup;
        }

        return new BucketAndIndex(null, bucketIndex, endOfLookupChain);
    }

    @CompilerDirectives.SlowPath
    public void verySlowSetAtBucket(BucketAndIndex bucketAndIndex, Object key, Object value) {
        if (bucketAndIndex.getBucket() == null) {
            final Bucket bucket = new RubyHash.Bucket();
            bucket.key = key;
            bucket.value = value;

            if (firstInSequence == null) {
                firstInSequence = bucket;
                lastInSequence = bucket;
            } else {
                lastInSequence.nextInSequence = bucket;
                bucket.previousInSequence = lastInSequence;
                lastInSequence = bucket;
            }

            if (bucketAndIndex.getEndOfLookupChain() == null) {
                ((Bucket[]) store)[bucketAndIndex.getIndex()] = bucket;
            } else {
                bucketAndIndex.getEndOfLookupChain().nextInLookup = bucket;
                bucket.previousInLookup = bucketAndIndex.getEndOfLookupChain();
            }
        } else {
            final Bucket bucket = bucketAndIndex.getBucket();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            bucket.key = key;
            bucket.value = value;
        }
    }

    @CompilerDirectives.SlowPath
    public boolean verySlowSetInBuckets(Object key, Object value) {
        if (key instanceof RubyString) {
            key = DebugOperations.send(getContext(), DebugOperations.send(getContext(), key, "dup", null), "freeze", null);
        }

        final BucketAndIndex bucketAndIndex = verySlowFindBucket(key);
        verySlowSetAtBucket(bucketAndIndex, key, value);
        return bucketAndIndex.getBucket() == null;
    }

    @CompilerDirectives.SlowPath
    public static RubyHash verySlowFromEntries(RubyContext context, List<Entry> entries) {
        RubyNode.notDesignedForCompilation();

        final RubyHash hash = new RubyHash(context.getCoreLibrary().getHashClass(), null, null, null, 0, null);
        hash.verySlowSetEntries(entries);
        return hash;
    }

    @CompilerDirectives.SlowPath
    public void verySlowSetEntries(List<Entry> entries) {
        final int size = entries.size();
        setStore(new Bucket[RubyHash.capacityGreaterThan(size)], 0, null, null);

        int actualSize = 0;

        for (Entry entry : entries) {
            if (verySlowSetInBuckets(entry.getKey(), entry.getValue())) {
                actualSize++;
            }
        }

        setStoreSize(actualSize);
    }

    public void dump() {
        final StringBuilder builder = new StringBuilder();

        builder.append("(");

        for (Bucket bucket : (Bucket[]) store) {
            builder.append("(");

            while (bucket != null) {
                builder.append("[");
                builder.append(bucket.key);
                builder.append(",");
                builder.append(bucket.value);
                builder.append("]");
                bucket = bucket.nextInLookup;
            }

            builder.append(")");
        }

        builder.append(")~>(");

        Bucket bucket = firstInSequence;

        while (bucket != null) {
            builder.append("[");
            builder.append(bucket.key);
            builder.append(",");
            builder.append(bucket.value);
            builder.append("]");
            bucket = bucket.nextInSequence;
        }

        builder.append(")<~(");

        bucket = lastInSequence;

        while (bucket != null) {
            builder.append("[");
            builder.append(bucket.key);
            builder.append(",");
            builder.append(bucket.value);
            builder.append("]");
            bucket = bucket.previousInSequence;
        }

        builder.append(")");

        System.err.println(builder);
    }

}
