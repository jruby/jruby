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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.cli.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class HashOperations {

    public static final int SMALL_HASH_SIZE = Options.TRUFFLE_HASHES_SMALL.load();

    private static final int[] CAPACITIES = Arrays.copyOf(org.jruby.RubyHash.MRI_PRIMES, org.jruby.RubyHash.MRI_PRIMES.length - 1);
    private static final int SIGN_BIT_MASK = ~(1 << 31);

    public static int capacityGreaterThan(int size) {
        for (int capacity : CAPACITIES) {
            if (capacity > size) {
                return capacity;
            }
        }

        return CAPACITIES[CAPACITIES.length - 1];
    }

    public static RubyHash verySlowFromEntries(RubyContext context, List<KeyValue> entries, boolean byIdentity) {
        return verySlowFromEntries(context.getCoreLibrary().getHashClass(), entries, byIdentity);
    }

    @CompilerDirectives.TruffleBoundary
    public static RubyHash verySlowFromEntries(RubyClass hashClass, List<KeyValue> entries, boolean byIdentity) {
        RubyNode.notDesignedForCompilation();

        final RubyHash hash = new RubyHash(hashClass, null, null, null, 0, null);
        verySlowSetKeyValues(hash, entries, byIdentity);
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
            for (int n = 0; n < hash.getSize(); n++) {
                keyValues.add(new KeyValue(((Object[]) hash.getStore())[n * 2], ((Object[]) hash.getStore())[n * 2 + 1]));
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
        final int bucketIndex = (hashed & SIGN_BIT_MASK) % entries.length;
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
                return new HashSearchResult(bucketIndex, previousEntry, entry);
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashSearchResult(bucketIndex, previousEntry, null);
    }

    public static void setAtBucket(RubyHash hash, HashSearchResult hashSearchResult, Object key, Object value) {
        if (hashSearchResult.getEntry() == null) {
            final Entry entry = new Entry(key, value);

            if (hashSearchResult.getPreviousEntry() == null) {
                ((Entry[]) hash.getStore())[hashSearchResult.getIndex()] = entry;
            } else {
                hashSearchResult.getPreviousEntry().setNextInLookup(entry);
            }

            if (hash.getFirstInSequence() == null) {
                hash.setFirstInSequence(entry);
                hash.setLastInSequence(entry);
            } else {
                hash.getLastInSequence().setNextInSequence(entry);
                entry.setPreviousInSequence(hash.getLastInSequence());
                hash.setLastInSequence(entry);
            }
        } else {
            final Entry entry = hashSearchResult.getEntry();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            entry.setKey(key);
            entry.setValue(value);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static boolean verySlowSetInBuckets(RubyHash hash, Object key, Object value, boolean byIdentity) {
        if (!byIdentity && key instanceof RubyString) {
            key = DebugOperations.send(hash.getContext(), DebugOperations.send(hash.getContext(), key, "dup", null), "freeze", null);
        }

        final HashSearchResult hashSearchResult = verySlowFindBucket(hash, key, byIdentity);
        setAtBucket(hash, hashSearchResult, key, value);
        return hashSearchResult.getEntry() == null;
    }

    @CompilerDirectives.TruffleBoundary
    public static void verySlowSetKeyValues(RubyHash hash, List<KeyValue> keyValues, boolean byIdentity) {
        final int size = keyValues.size();
        hash.setStore(new Entry[capacityGreaterThan(size)], 0, null, null);

        int actualSize = 0;

        for (KeyValue keyValue : keyValues) {
            if (verySlowSetInBuckets(hash, keyValue.getKey(), keyValue.getValue(), byIdentity)) {
                actualSize++;
            }
        }

        hash.setSize(actualSize);
    }

    public static int getIndex(int hashed, int entriesLength) {
        return (hashed & HashOperations.SIGN_BIT_MASK) % entriesLength;
    }
}
