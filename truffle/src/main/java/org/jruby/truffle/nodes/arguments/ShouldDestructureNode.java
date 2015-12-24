/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.profiles.BranchProfile;

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.RespondToNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Switches between loading arguments as normal and doing a destructure.
 */
public class ShouldDestructureNode extends RubyNode {

    @Child private RespondToNode respondToCheck;

    private final BranchProfile checkRespondProfile = BranchProfile.create();

    public ShouldDestructureNode(RubyContext context, SourceSection sourceSection, RespondToNode respondToCheck) {
        super(context, sourceSection);
        this.respondToCheck = respondToCheck;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        // If the caller supplied no arguments, or more than one argument, there's no need to destructure this time

        if (RubyArguments.getUserArgumentsCount(frame.getArguments()) != 1) {
            return false;
        }

        // If the single argument is a RubyArray, destructure
        // TODO(CS): can we not just rely on the respondToCheck? Should experiment.

        if (RubyGuards.isRubyArray(RubyArguments.getUserArgument(frame.getArguments(), 0))) {
            return true;
        }

        // If the single argument responds to #to_ary, then destructure

        checkRespondProfile.enter();

        return respondToCheck.executeBoolean(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
