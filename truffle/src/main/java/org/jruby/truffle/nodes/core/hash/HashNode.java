/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.hash;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

public class HashNode extends RubyNode {

    @Child private CallDispatchHeadNode hashNode;

    private final ConditionProfile isIntegerProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isLongProfile = ConditionProfile.createBinaryProfile();

    public HashNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = DispatchHeadNodeFactory.createMethodCall(context, true);
    }

    public int hash(VirtualFrame frame, Object key) {
        final Object hashedObject = hashNode.call(frame, key, "hash", null);

        if (isIntegerProfile.profile(hashedObject instanceof Integer)) {
            return (int) hashedObject;
        } else if (isLongProfile.profile(hashedObject instanceof Long)) {
            return (int) (long) hashedObject;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw new UnsupportedOperationException();
    }

}
