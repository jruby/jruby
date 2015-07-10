/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.hash;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.BasicObjectNodesFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.hash.BucketsStrategy;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashLookupResult;
import org.jruby.truffle.runtime.hash.PackedArrayStrategy;

@ImportStatic(HashGuards.class)
@NodeChildren({
        @NodeChild(value = "hash", type = RubyNode.class),
        @NodeChild(value = "key", type = RubyNode.class),
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "byIdentity", type = RubyNode.class)
})
public abstract class SetNode extends RubyNode {

    @Child private HashNode hashNode;
    @Child private CallDispatchHeadNode eqlNode;
    @Child private BasicObjectNodes.ReferenceEqualNode equalNode;
    @Child private LookupEntryNode lookupEntryNode;

    private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

    private final BranchProfile extendProfile = BranchProfile.create();
    private final ConditionProfile strategyProfile = ConditionProfile.createBinaryProfile();

    public SetNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = new HashNode(context, sourceSection);
        eqlNode = DispatchHeadNodeFactory.createMethodCall(context);
        equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
    }

    public abstract Object executeSet(VirtualFrame frame, RubyBasicObject hash, Object key, Object value, boolean byIdentity);

    @Specialization(guards = { "isNullHash(hash)", "!isRubyString(key)" })
    public Object setNull(VirtualFrame frame, RubyBasicObject hash, Object key, Object value, boolean byIdentity) {
        HashNodes.setStore(hash, PackedArrayStrategy.createStore(hashNode.hash(frame, key), key, value), 1, null, null);
        assert HashNodes.verifyStore(hash);
        return value;
    }

    @Specialization(guards = {"isNullHash(hash)", "byIdentity", "isRubyString(key)"})
    public Object setNullByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setNull(frame, hash, (Object) key, value, byIdentity);
    }

    @Specialization(guards = {"isNullHash(hash)", "!byIdentity", "isRubyString(key)"})
    public Object setNullNotByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setNull(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value, byIdentity);
    }

    @ExplodeLoop
    @Specialization(guards = {"isPackedHash(hash)", "!isRubyString(key)"})
    public Object setPackedArray(VirtualFrame frame, RubyBasicObject hash, Object key, Object value, boolean byIdentity) {
        assert HashNodes.verifyStore(hash);

        final int hashed = hashNode.hash(frame, key);

        final Object[] store = (Object[]) HashNodes.getStore(hash);
        final int size = HashNodes.getSize(hash);

        for (int n = 0; n < PackedArrayStrategy.MAX_ENTRIES; n++) {
            if (n < size) {
                if (hashed == PackedArrayStrategy.getHashed(store, n)) {
                    final boolean equal;

                    if (byIdentityProfile.profile(byIdentity)) {
                        equal = equalNode.executeReferenceEqual(frame, key, PackedArrayStrategy.getKey(store, n));
                    } else {
                        equal = eqlNode.callBoolean(frame, key, "eql?", null, PackedArrayStrategy.getKey(store, n));
                    }

                    if (equal) {
                        PackedArrayStrategy.setValue(store, n, value);
                        assert HashNodes.verifyStore(hash);
                        return value;
                    }
                }
            }
        }

        extendProfile.enter();

        if (strategyProfile.profile(size + 1 <= PackedArrayStrategy.MAX_ENTRIES)) {
            PackedArrayStrategy.setHashedKeyValue(store, size, hashed, key, value);
            HashNodes.setSize(hash, size + 1);
            return value;
        } else {
            PackedArrayStrategy.promoteToBuckets(hash, store, size);
            BucketsStrategy.addNewEntry(hash, hashed, key, value);
        }

        assert HashNodes.verifyStore(hash);

        return value;
    }

    @Specialization(guards = {"isPackedHash(hash)", "byIdentity", "isRubyString(key)"})
    public Object setPackedArrayByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setPackedArray(frame, hash, key, value, byIdentity);
    }

    @Specialization(guards = {"isPackedHash(hash)", "!byIdentity", "isRubyString(key)"})
    public Object setPackedArrayNotByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setPackedArray(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value, byIdentity);
    }

    // Can't be @Cached yet as we call from the RubyString specialisation
    private final ConditionProfile foundProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile bucketCollisionProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile appendingProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile resizeProfile = ConditionProfile.createBinaryProfile();

    @Specialization(guards = {"isBucketHash(hash)", "!isRubyString(key)"})
    public Object setBuckets(VirtualFrame frame, RubyBasicObject hash, Object key, Object value, boolean byIdentity) {
        assert HashNodes.verifyStore(hash);

        if (lookupEntryNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            lookupEntryNode = insert(new LookupEntryNode(getContext(), getEncapsulatingSourceSection()));
        }

        final HashLookupResult result = lookupEntryNode.lookup(frame, hash, key);

        final Entry entry = result.getEntry();

        if (foundProfile.profile(entry == null)) {
            final Entry[] entries = (Entry[]) HashNodes.getStore(hash);

            final Entry newEntry = new Entry(result.getHashed(), key, value);

            if (bucketCollisionProfile.profile(result.getPreviousEntry() == null)) {
                entries[result.getIndex()] = newEntry;
            } else {
                result.getPreviousEntry().setNextInLookup(newEntry);
            }

            final Entry lastInSequence = HashNodes.getLastInSequence(hash);

            if (appendingProfile.profile(lastInSequence == null)) {
                HashNodes.setFirstInSequence(hash, newEntry);
            } else {
                lastInSequence.setNextInSequence(newEntry);
                newEntry.setPreviousInSequence(lastInSequence);
            }

            HashNodes.setLastInSequence(hash, newEntry);

            final int newSize = HashNodes.getSize(hash) + 1;

            HashNodes.setSize(hash, newSize);

            // TODO CS 11-May-15 could store the next size for resize instead of doing a float operation each time

            if (resizeProfile.profile(newSize / (double) entries.length > BucketsStrategy.LOAD_FACTOR)) {
                BucketsStrategy.resize(hash);
            }
        } else {
            entry.setKeyValue(result.getHashed(), key, value);
        }

        assert HashNodes.verifyStore(hash);

        return value;
    }

    @Specialization(guards = {"isBucketHash(hash)", "byIdentity", "isRubyString(key)"})
    public Object setBucketsByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setBuckets(frame, hash, (Object) key, value, byIdentity);
    }

    @Specialization(guards = {"isBucketHash(hash)", "!byIdentity", "isRubyString(key)"})
    public Object setBucketsNotByIdentity(VirtualFrame frame, RubyBasicObject hash, RubyBasicObject key, Object value, boolean byIdentity) {
        return setBuckets(frame, hash, ruby(frame, "key.frozen? ? key : key.dup.freeze", "key", key), value, byIdentity);
    }

}
