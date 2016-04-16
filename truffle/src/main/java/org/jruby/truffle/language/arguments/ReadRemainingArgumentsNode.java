/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.language.RubyNode;

public class ReadRemainingArgumentsNode extends RubyNode {

    private final int start;
    private final ConditionProfile remainingArguments = ConditionProfile.createBinaryProfile();

    public ReadRemainingArgumentsNode(int start) {
        this.start = start;
    }

    @Override
    public Object[] executeObjectArray(VirtualFrame frame) {
        final int count = RubyArguments.getArgumentsCount(frame);

        if (remainingArguments.profile(start < count)) {
            return RubyArguments.getArguments(frame, start);
        } else {
            return new Object[] {};
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeObjectArray(frame);
    }

}
