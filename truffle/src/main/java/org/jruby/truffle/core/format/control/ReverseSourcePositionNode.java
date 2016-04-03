/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.exceptions.OutsideOfStringException;

public class ReverseSourcePositionNode extends FormatNode {

    private boolean star;

    private final ConditionProfile rangeProfile = ConditionProfile.createBinaryProfile();

    public ReverseSourcePositionNode(RubyContext context, boolean star) {
        super(context);
        this.star = star;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final int position = getSourcePosition(frame);

        if (star) {
            final int remaining = getSourceLength(frame) - position;

            final int target = position - remaining;

            if (rangeProfile.profile(target < 0)) {
                throw new OutsideOfStringException();
            }

            setSourcePosition(frame, target);
        } else {
            if (rangeProfile.profile(position == 0)) {
                throw new OutsideOfStringException();
            }

            setSourcePosition(frame, position - 1);
        }

        return null;
    }

}
