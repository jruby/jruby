/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.PrimitiveValueProfile;
import com.oracle.truffle.api.profiles.ValueProfile;
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public class ProfileArgumentNode extends RubyNode {

    @Child private RubyNode readArgumentNode;

    private final ConditionProfile objectProfile = ConditionProfile.createBinaryProfile();
    private final ValueProfile primitiveProfile = PrimitiveValueProfile.createEqualityProfile();
    private final ValueProfile classProfile = ValueProfile.createClassProfile();

    public ProfileArgumentNode(RubyNode readArgumentNode) {
        this.readArgumentNode = readArgumentNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = readArgumentNode.execute(frame);

        if (objectProfile.profile(value instanceof TruffleObject)) {
            return classProfile.profile(value);
        } else {
            return primitiveProfile.profile(value);
        }
    }

}
