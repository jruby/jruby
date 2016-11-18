/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyBaseNode;

public class LookupEntryNode extends RubyBaseNode {

    @Child HashNode hashNode = new HashNode();
    @Child CompareHashKeysNode compareHashKeysNode = new CompareHashKeysNode();

    private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

    public HashLookupResult lookup(VirtualFrame frame, DynamicObject hash, Object key) {
        final boolean compareByIdentity = byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash));
        int hashed = hashNode.hash(frame, key, compareByIdentity);

        final Entry[] entries = (Entry[]) Layouts.HASH.getStore(hash);
        final int index = BucketsStrategy.getBucketIndex(hashed, entries.length);
        Entry entry = entries[index];

        Entry previousEntry = null;

        while (entry != null) {
            if (equalKeys(frame, compareByIdentity, key, hashed, entry.getKey(), entry.getHashed())) {
                return new HashLookupResult(hashed, index, previousEntry, entry);
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashLookupResult(hashed, index, previousEntry, null);
    }

    protected boolean equalKeys(VirtualFrame frame, boolean compareByIdentity, Object key, int hashed, Object otherKey, int otherHashed) {
        return compareHashKeysNode.equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed);
    }

}
