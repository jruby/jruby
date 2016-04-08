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

public class AdvanceSourcePositionNode extends FormatNode {

    private final boolean toEnd;

    private final ConditionProfile rangeProfile = ConditionProfile.createBinaryProfile();

    public AdvanceSourcePositionNode(RubyContext context, boolean toEnd) {
        super(context);
        this.toEnd = toEnd;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (toEnd) {
            setSourcePosition(frame, getSourceLength(frame));
        } else {
            final long position = getSourcePosition(frame);

            if (rangeProfile.profile(position + 1 > getSourceLength(frame))) {
                throw new OutsideOfStringException();
            }

            setSourcePosition(frame, getSourcePosition(frame) + 1);
        }

        return null;
    }

}
