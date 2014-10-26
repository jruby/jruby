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
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;

public class DoWhileNode extends RubyNode {

    @Child protected LoopNode loopNode;

    public DoWhileNode(RubyContext context, SourceSection sourceSection, BooleanCastNode condition, RubyNode body) {
        super(context, sourceSection);
        loopNode = Truffle.getRuntime().createLoopNode(new DoWhileRepeatingNode(condition, body));
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

    private static class DoWhileRepeatingNode extends Node implements RepeatingNode {

        @Child protected BooleanCastNode condition;
        @Child protected RubyNode body;

        public DoWhileRepeatingNode(BooleanCastNode condition, RubyNode body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            while (true) { // for redo
                try {
                    body.execute(frame);
                    break;
                } catch (NextException e) {
                    break;
                } catch (RedoException e) {
                    // Just continue in the while(true) loop.
                }
            }

            return condition.executeBoolean(frame);
        }

    }

}
