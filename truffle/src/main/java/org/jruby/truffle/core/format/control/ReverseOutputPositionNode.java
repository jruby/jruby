/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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

public class ReverseOutputPositionNode extends FormatNode {

    private final ConditionProfile rangeProfile = ConditionProfile.createBinaryProfile();

    public ReverseOutputPositionNode(RubyContext context) {
        super(context);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final int position = getOutputPosition(frame);

        if (rangeProfile.profile(position == 0)) {
            throw new OutsideOfStringException();
        }

        setOutputPosition(frame, position - 1);
        return null;
    }

}
