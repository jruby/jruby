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

public class SetSourcePositionNode extends FormatNode {

    private final int position;

    private final ConditionProfile rangeProfile = ConditionProfile.createBinaryProfile();

    public SetSourcePositionNode(RubyContext context, int position) {
        super(context);
        this.position = position;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (rangeProfile.profile(position > getSourceLength(frame))) {
            throw new OutsideOfStringException();
        }

        setSourcePosition(frame, position);
        return null;
    }

}
