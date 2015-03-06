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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.methods.locals.FlipFlopStateNode;
import org.jruby.truffle.runtime.RubyContext;

public class FlipFlopNode extends RubyNode {

    @Child private BooleanCastNode begin;
    @Child private BooleanCastNode end;
    @Child private FlipFlopStateNode stateNode;

    private final boolean exclusive;

    public FlipFlopNode(RubyContext context, SourceSection sourceSection, RubyNode begin, RubyNode end, FlipFlopStateNode stateNode, boolean exclusive) {
        super(context, sourceSection);
        this.begin = BooleanCastNodeFactory.create(context, sourceSection, begin);
        this.end = BooleanCastNodeFactory.create(context, sourceSection, end);
        this.stateNode = stateNode;
        this.exclusive = exclusive;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) {
        notDesignedForCompilation("066915ef91df4e2da91a14b73ccf4f03");

        if (exclusive) {
            if (stateNode.getState(frame)) {
                if (end.executeBoolean(frame)) {
                    stateNode.setState(frame, false);
                }

                return true;
            } else {
                final boolean newState = begin.executeBoolean(frame);
                stateNode.setState(frame, newState);
                return newState;
            }
        } else {
            if (stateNode.getState(frame)) {
                if (end.executeBoolean(frame)) {
                    stateNode.setState(frame, false);
                }

                return true;
            } else {
                if (begin.executeBoolean(frame)) {
                    stateNode.setState(frame, !end.executeBoolean(frame));
                    return true;
                }

                return false;
            }
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeBoolean(frame);
    }

}
