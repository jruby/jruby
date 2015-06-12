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
import org.jruby.truffle.nodes.core.hash.HashNodes;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HashOperations {

    @TruffleBoundary
    public static HashLookupResult verySlowFindBucket(RubyBasicObject hash, Object key, boolean byIdentity) {
        final int hashed = HashNodes.slowHashKey(hash.getContext(), key);

        final Entry[] entries = (Entry[]) HashNodes.getStore(hash);
        final int bucketIndex = (hashed & BucketsStrategy.SIGN_BIT_MASK) % entries.length;
        Entry entry = entries[bucketIndex];

        Entry previousEntry = null;

        while (entry != null) {
            if (HashNodes.slowAreKeysEqual(hash.getContext(), key, entry.getKey(), byIdentity)) {
                return new HashLookupResult(hashed, bucketIndex, previousEntry, entry);
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashLookupResult(hashed, bucketIndex, previousEntry, null);
    }

    @TruffleBoundary
    public static boolean verySlowSetInBuckets(RubyBasicObject hash, Object key, Object value, boolean byIdentity) {
        assert HashNodes.verifyStore(hash);

        if (!byIdentity && key instanceof RubyString) {
            key = DebugOperations.send(hash.getContext(), DebugOperations.send(hash.getContext(), key, "dup", null), "freeze", null);
        }

        final HashLookupResult hashLookupResult = verySlowFindBucket(hash, key, byIdentity);
        assert HashNodes.verifyStore(hash);

        assert HashNodes.getStore(hash) instanceof Entry[];

        if (hashLookupResult.getEntry() == null) {
            BucketsStrategy.addNewEntry(hash, hashLookupResult.getHashed(), key, value);

            assert HashNodes.verifyStore(hash);
        } else {
            final Entry entry = hashLookupResult.getEntry();

            // The bucket stays in the same place in the sequence

            // Update the key (it overwrites even it it's eql?) and value

            entry.setHashed(hashLookupResult.getHashed());
            entry.setKey(key);
            entry.setValue(value);

            assert HashNodes.verifyStore(hash);
        }

        assert HashNodes.verifyStore(hash);

        return hashLookupResult.getEntry() == null;
    }

}
