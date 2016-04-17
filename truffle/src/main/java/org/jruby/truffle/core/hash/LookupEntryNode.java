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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.basicobject.BasicObjectNodes;
import org.jruby.truffle.core.basicobject.BasicObjectNodesFactory;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class LookupEntryNode extends RubyBaseNode {

    @Child HashNode hashNode;
    @Child CallDispatchHeadNode eqlNode;
    @Child BasicObjectNodes.ReferenceEqualNode equalNode;
    
    private final ConditionProfile byIdentityProfile = ConditionProfile.createBinaryProfile();

    public LookupEntryNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = new HashNode(context, sourceSection);
        eqlNode = DispatchHeadNodeFactory.createMethodCall(context);
        equalNode = BasicObjectNodesFactory.ReferenceEqualNodeFactory.create(null, null);
    }

    public HashLookupResult lookup(VirtualFrame frame, DynamicObject hash, Object key) {
        final int hashed = hashNode.hash(frame, key);

        final Entry[] entries = (Entry[]) Layouts.HASH.getStore(hash);
        final int index = BucketsStrategy.getBucketIndex(hashed, entries.length);
        Entry entry = entries[index];

        Entry previousEntry = null;

        while (entry != null) {
            if (byIdentityProfile.profile(Layouts.HASH.getCompareByIdentity(hash))) {
                if (equalNode.executeReferenceEqual(frame, key, entry.getKey())) {
                    return new HashLookupResult(hashed, index, previousEntry, entry);
                }
            } else {
                if (eqlNode.callBoolean(frame, key, "eql?", null, entry.getKey())) {
                    return new HashLookupResult(hashed, index, previousEntry, entry);
                }
            }

            previousEntry = entry;
            entry = entry.getNextInLookup();
        }

        return new HashLookupResult(hashed, index, previousEntry, null);
    }

}
