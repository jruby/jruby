/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.hash;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

public class HashNode extends RubyBaseNode {

    @Child private CallDispatchHeadNode hashNode;

    private final ConditionProfile isIntegerProfile = ConditionProfile.createBinaryProfile();
    private final ConditionProfile isLongProfile = ConditionProfile.createBinaryProfile();

    public HashNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        hashNode = DispatchHeadNodeFactory.createMethodCall(context, true);
    }

    public int hash(VirtualFrame frame, Object key) {
        final Object hashedObject = hashNode.call(frame, key, "hash");

        if (isIntegerProfile.profile(hashedObject instanceof Integer)) {
            return (int) hashedObject;
        } else if (isLongProfile.profile(hashedObject instanceof Long)) {
            return (int) (long) hashedObject;
        } else {
            throw new UnsupportedOperationException();
        }
    }

}
