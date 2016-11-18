/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(HashGuards.class)
@NodeChildren({
        @NodeChild(value = "hash", type = RubyNode.class),
        @NodeChild(value = "key", type = RubyNode.class),
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "byIdentity", type = RubyNode.class)
})
public abstract class SetNode extends RubyNode {

    @Child private HashNode hashNode = new HashNode();
    @Child private LookupEntryNode lookupEntryNode;
    @Child private CompareHashKeysNode compareHashKeysNode = new CompareHashKeysNode();
    @Child private FreezeHashKeyIfNeededNode freezeHashKeyIfNeededNode = FreezeHashKeyIfNeededNodeGen.create(null, null);

    public static SetNode create() {
        return SetNodeGen.create(null, null, null, null);
    }

    public abstract Object executeSet(VirtualFrame frame, DynamicObject hash, Object key, Object value, boolean byIdentity);

    @Specialization(guards = "isNullHash(hash)")
    public Object setNull(VirtualFrame frame, DynamicObject hash, Object originalKey, Object value, boolean byIdentity,
                    @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile) {
        assert HashOperations.verifyStore(getContext(), hash);
        boolean compareByIdentity = byIdentityProfile.profile(byIdentity);
        final Object key = freezeHashKeyIfNeededNode.executeFreezeIfNeeded(frame, originalKey, compareByIdentity);

        final int hashed = hashNode.hash(frame, key, compareByIdentity);

        Object store = PackedArrayStrategy.createStore(getContext(), hashed, key, value);
        assert HashOperations.verifyStore(getContext(), store, 1, null, null);
        Layouts.HASH.setStore(hash, store);
        Layouts.HASH.setSize(hash, 1);
        Layouts.HASH.setFirstInSequence(hash, null);
        Layouts.HASH.setLastInSequence(hash, null);

        assert HashOperations.verifyStore(getContext(), hash);
        return value;
    }

    @ExplodeLoop
    @Specialization(guards = "isPackedHash(hash)")
    public Object setPackedArray(VirtualFrame frame, DynamicObject hash, Object originalKey, Object value, boolean byIdentity,
                    @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile,
                    @Cached("createBinaryProfile()") ConditionProfile strategyProfile,
                    @Cached("create()") BranchProfile extendProfile) {
        assert HashOperations.verifyStore(getContext(), hash);
        final boolean compareByIdentity = byIdentityProfile.profile(byIdentity);
        final Object key = freezeHashKeyIfNeededNode.executeFreezeIfNeeded(frame, originalKey, compareByIdentity);

        final int hashed = hashNode.hash(frame, key, compareByIdentity);

        final Object[] store = (Object[]) Layouts.HASH.getStore(hash);
        final int size = Layouts.HASH.getSize(hash);

        for (int n = 0; n < getContext().getOptions().HASH_PACKED_ARRAY_MAX; n++) {
            if (n < size) {
                final int otherHashed = PackedArrayStrategy.getHashed(store, n);
                final Object otherKey = PackedArrayStrategy.getKey(store, n);
                if (equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed)) {
                    PackedArrayStrategy.setValue(store, n, value);
                    assert HashOperations.verifyStore(getContext(), hash);
                    return value;
                }
            }
        }

        extendProfile.enter();

        if (strategyProfile.profile(size + 1 <= getContext().getOptions().HASH_PACKED_ARRAY_MAX)) {
            PackedArrayStrategy.setHashedKeyValue(store, size, hashed, key, value);
            Layouts.HASH.setSize(hash, size + 1);
            return value;
        } else {
            PackedArrayStrategy.promoteToBuckets(getContext(), hash, store, size);
            BucketsStrategy.addNewEntry(getContext(), hash, hashed, key, value);
        }

        assert HashOperations.verifyStore(getContext(), hash);

        return value;
    }

    @Specialization(guards = "isBucketHash(hash)")
    public Object setBuckets(VirtualFrame frame, DynamicObject hash, Object originalKey, Object value, boolean byIdentity,
                    @Cached("createBinaryProfile()") ConditionProfile byIdentityProfile,
                    @Cached("createBinaryProfile()") ConditionProfile foundProfile,
                    @Cached("createBinaryProfile()") ConditionProfile bucketCollisionProfile,
                    @Cached("createBinaryProfile()") ConditionProfile appendingProfile,
                    @Cached("createBinaryProfile()") ConditionProfile resizeProfile) {
        assert HashOperations.verifyStore(getContext(), hash);
        final boolean compareByIdentity = byIdentityProfile.profile(byIdentity);
        final Object key = freezeHashKeyIfNeededNode.executeFreezeIfNeeded(frame, originalKey, compareByIdentity);

        final HashLookupResult result = lookup(frame, hash, key);
        final Entry entry = result.getEntry();

        if (foundProfile.profile(entry == null)) {
            final Entry[] entries = (Entry[]) Layouts.HASH.getStore(hash);

            final Entry newEntry = new Entry(result.getHashed(), key, value);

            if (bucketCollisionProfile.profile(result.getPreviousEntry() == null)) {
                entries[result.getIndex()] = newEntry;
            } else {
                result.getPreviousEntry().setNextInLookup(newEntry);
            }

            final Entry lastInSequence = Layouts.HASH.getLastInSequence(hash);

            if (appendingProfile.profile(lastInSequence == null)) {
                Layouts.HASH.setFirstInSequence(hash, newEntry);
            } else {
                lastInSequence.setNextInSequence(newEntry);
                newEntry.setPreviousInSequence(lastInSequence);
            }

            Layouts.HASH.setLastInSequence(hash, newEntry);

            final int newSize = Layouts.HASH.getSize(hash) + 1;

            Layouts.HASH.setSize(hash, newSize);

            // TODO CS 11-May-15 could store the next size for resize instead of doing a float operation each time

            if (resizeProfile.profile(newSize / (double) entries.length > BucketsStrategy.LOAD_FACTOR)) {
                BucketsStrategy.resize(getContext(), hash);
            }
        } else {
            entry.setKeyValue(result.getHashed(), key, value);
        }

        assert HashOperations.verifyStore(getContext(), hash);

        return value;
    }

    private HashLookupResult lookup(VirtualFrame frame, DynamicObject hash, Object key) {
        if (lookupEntryNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookupEntryNode = insert(new LookupEntryNode());
        }
        return lookupEntryNode.lookup(frame, hash, key);
    }

    protected boolean equalKeys(VirtualFrame frame, boolean compareByIdentity, Object key, int hashed, Object otherKey, int otherHashed) {
        return compareHashKeysNode.equalKeys(frame, compareByIdentity, key, hashed, otherKey, otherHashed);
    }

}
