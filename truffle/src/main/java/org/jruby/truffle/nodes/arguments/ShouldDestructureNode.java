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
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.RespondToNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.methods.Arity;

/**
 * Switches between loading arguments as normal and doing a destructure.
 */
public class ShouldDestructureNode extends RubyNode {

    private final Arity arity;
    @Child private RespondToNode respondToCheck;

    private final BranchProfile checkRespondProfile = BranchProfile.create();

    public ShouldDestructureNode(RubyContext context, SourceSection sourceSection, Arity arity, RespondToNode respondToCheck) {
        super(context, sourceSection);
        this.arity = arity;
        this.respondToCheck = respondToCheck;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        // TODO(CS): express this using normal nodes?

        // If we don't accept any arguments, there's never any need to destructure
        // TODO(CS): is this guaranteed by the translator anyway?

        if (!arity.allowsMore() && arity.getRequired() == 0 && arity.getOptional() == 0) {
            return false;
        }

        // If we only accept one argument, there's never any need to destructure

        if (!arity.allowsMore() && arity.getRequired() == 1 && arity.getOptional() == 0) {
            return false;
        }

        // If the caller supplied no arguments, or more than one argument, there's no need to destructure this time

        if (RubyArguments.getUserArgumentsCount(frame.getArguments()) != 1) {
            return false;
        }

        // If the single argument is a RubyArray, destructure
        // TODO(CS): can we not just reply on the respondToCheck? Should experiment.

        if (RubyArguments.getUserArgument(frame.getArguments(), 0) instanceof RubyArray) {
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
