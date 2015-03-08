/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.hash;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.BasicObjectNodesFactory;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyHash;
import org.jruby.truffle.runtime.hash.Entry;
import org.jruby.truffle.runtime.hash.HashOperations;
import org.jruby.truffle.runtime.hash.HashSearchResult;

public class FindEntryNode extends RubyNode {

    @Child CallDispatchHeadNode hashNode;
    @Child CallDispatchHeadNode eqlNode;
    @Child BasicObjectNodes.ReferenceEqualNode equalNode;
    
    private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

    public FindEntryNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = DispatchHeadNodeFactory.createMethodCall(context, true);
        eqlNode = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(context, sourceSection, null, null);
    }

    public HashSearchResult search(VirtualFrame frame, RubyHash hash, Object key) {
        final Object hashValue = hashNode.call(frame, key, "hash", null);

        final int hashed;

        if (hashValue instanceof Integer) {
            hashed = (int) hashValue;
        } else if (hashValue instanceof Long) {
            hashed = (int) (long) hashValue;
        } else {
            throw new UnsupportedOperationException();
        }

        final Entry[] entries = (Entry[]) hash.getStore();
        final int index = HashOperations.getIndex(hashed, entries.length);
        Entry entry = entries[index];

        Entry previousEntry = null;

        while (entry != null) {
            if (byIdentityProfile.profile(hash.isCompareByIdentity())) {
                if (equalNode.executeReferenceEqual(frame, key, entry.getKey())) {
                    return new HashSearchResult(index, previousEntry, entry);
                }
            } else {
                if (eqlNode.callBoolean(frame, key, "eql?", null, entry.getKey())) {
                    return new HashSearchResult(index, previousEntry, entry);
                }
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashSearchResult(index, previousEntry, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

}
