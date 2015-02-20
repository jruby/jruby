/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Represents a Ruby {@code if} expression. Note that in this representation we always have an
 * {@code else} part.
 */
public class IfNode extends RubyNode {

    @Child private BooleanCastNode condition;
    @Child private RubyNode thenBody;
    @Child private RubyNode elseBody;

    private final BranchProfile thenBranch = BranchProfile.create();
    private final BranchProfile elseBranch = BranchProfile.create();

    // private final ConditionProfile conditionProfile = ConditionProfile.createCountingProfile();

    public IfNode(RubyContext context, SourceSection sourceSection, RubyNode condition, RubyNode thenBody, RubyNode elseBody) {
        super(context, sourceSection);

        assert condition != null;
        assert thenBody != null;
        assert elseBody != null;

        this.condition = BooleanCastNodeFactory.create(context, sourceSection, condition);
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        // TODO CS 20-Feb-15 - we'd like to use this but it causes problems where we do things like x = if ...
        // if (conditionProfile.profile(condition.executeBoolean(frame))) {
        if (condition.executeBoolean(frame)) {
            thenBranch.enter();
            return thenBody.execute(frame);
        } else {
            elseBranch.enter();
            return elseBody.execute(frame);
        }
    }
}
