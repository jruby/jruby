/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.RubyNode;

public class AndNode extends RubyNode {

    @Child private RubyNode left;
    @Child private RubyNode right;

    @Child private BooleanCastNode leftCast;

    private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public AndNode(RubyNode left, RubyNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object leftValue = left.execute(frame);

        if (leftCast == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            leftCast = insert(BooleanCastNodeGen.create(null));
        }

        final boolean leftBoolean = leftCast.executeToBoolean(leftValue);

        if (conditionProfile.profile(leftBoolean)) {
            return right.execute(frame);
        } else {
            return leftValue;
        }
    }

}
