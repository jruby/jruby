/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.builtins;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;

public class InvokePrimitiveNode extends RubyNode {

    @Child private RubyNode primitive;

    private final ConditionProfile primitiveSucceededCondition = ConditionProfile.createBinaryProfile();

    public InvokePrimitiveNode(RubyNode primitive) {
        this.primitive = primitive;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        primitive.execute(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object value = primitive.execute(frame);

        if (primitiveSucceededCondition.profile(value != null)) {
            return value;
        } else {
            return nil();
        }
    }

}
