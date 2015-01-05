/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;

public class WhileNode extends RubyNode {

    @Child protected LoopNode loopNode;

    public WhileNode(RubyContext context, SourceSection sourceSection, BooleanCastNode condition, RubyNode body) {
        super(context, sourceSection);
        loopNode = Truffle.getRuntime().createLoopNode(new WhileRepeatingNode(context, condition, body));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            loopNode.executeLoop(frame);
        } catch (BreakException e) {
            return e.getResult();
        }

        return getContext().getCoreLibrary().getNilObject();
    }

    private static class WhileRepeatingNode extends Node implements RepeatingNode {

        private final RubyContext context;

        @Child protected BooleanCastNode condition;
        @Child protected RubyNode body;

        public WhileRepeatingNode(RubyContext context, BooleanCastNode condition, RubyNode body) {
            this.context = context;
            this.condition = condition;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (!condition.executeBoolean(frame)) {
                return false;
            }

            while (true) { // for redo
                try {
                    body.execute(frame);
                    return true;
                } catch (NextException e) {
                    return true;
                } catch (RedoException e) {
                    // Just continue in the while(true) loop.
                    context.getSafepointManager().poll();
                }
            }
        }

    }

}
