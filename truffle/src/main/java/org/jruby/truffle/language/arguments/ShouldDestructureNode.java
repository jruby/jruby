/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.arguments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.dispatch.RespondToNode;

public class ShouldDestructureNode extends RubyNode {

    @Child private RubyNode readArrayNode;
    @Child private RespondToNode respondToCheck;

    private final BranchProfile checkIsArrayProfile = BranchProfile.create();

    public ShouldDestructureNode(RubyNode readArrayNode) {
        this.readArrayNode = readArrayNode;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        if (RubyArguments.getArgumentsCount(frame) != 1) {
            return false;
        }

        checkIsArrayProfile.enter();

        if (RubyGuards.isRubyArray(RubyArguments.getArgument(frame, 0))) {
            return true;
        }

        if (respondToCheck == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            respondToCheck = insert(new RespondToNode(readArrayNode, "to_ary"));
        }

        return respondToCheck.executeBoolean(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
