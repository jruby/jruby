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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;

public final class WhileNode extends RubyNode {

    @Child private LoopNode loopNode;
    private final BranchProfile breakUsed = BranchProfile.create();

    private WhileNode(RubyContext context, SourceSection sourceSection, RepeatingNode repeatingNode) {
        super(context, sourceSection);
        this.loopNode = Truffle.getRuntime().createLoopNode(repeatingNode);
    }

    public static WhileNode createWhile(RubyContext context, SourceSection sourceSection, RubyNode condition, RubyNode body) {
        RepeatingNode repeatingNode = new WhileRepeatingNode(context, condition, body);
        return new WhileNode(context, sourceSection, repeatingNode);
    }

    public static WhileNode createDoWhile(RubyContext context, SourceSection sourceSection, RubyNode condition, RubyNode body) {
        RepeatingNode repeatingNode = new DoWhileRepeatingNode(context, condition, body);
        return new WhileNode(context, sourceSection, repeatingNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            loopNode.executeLoop(frame);
        } catch (BreakException e) {
            breakUsed.enter();
            return e.getResult();
        }

        return getContext().getCoreLibrary().getNilObject();
    }
    
    private static abstract class WhileRepeatingBaseNode extends Node implements RepeatingNode {

        protected final RubyContext context;

        @Child protected BooleanCastNode condition;
        @Child protected RubyNode body;

        protected final BranchProfile redoUsed = BranchProfile.create();
        protected final BranchProfile nextUsed = BranchProfile.create();

        public WhileRepeatingBaseNode(RubyContext context, RubyNode condition, RubyNode body) {
            this.context = context;
            this.condition = BooleanCastNodeFactory.create(context, condition.getSourceSection(), condition);
            this.body = body;
        }
    }

    private static class WhileRepeatingNode extends WhileRepeatingBaseNode implements RepeatingNode {

        public WhileRepeatingNode(RubyContext context, RubyNode condition, RubyNode body) {
            super(context, condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (!condition.executeBoolean(frame)) {
                return false;
            }

            while (true) { // for redo
                context.getSafepointManager().poll(this);
                try {
                    body.execute(frame);
                    return true;
                } catch (NextException e) {
                    nextUsed.enter();
                    return true;
                } catch (RedoException e) {
                    // Just continue in the while(true) loop.
                    redoUsed.enter();
                }
            }
        }

    }
    
    private static class DoWhileRepeatingNode extends WhileRepeatingBaseNode implements RepeatingNode {

        public DoWhileRepeatingNode(RubyContext context, RubyNode condition, RubyNode body) {
            super(context, condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            context.getSafepointManager().poll(this);
            try {
                body.execute(frame);
            } catch (NextException e) {
                nextUsed.enter();
            } catch (RedoException e) {
                // Just continue to next iteration without executing the condition.
                redoUsed.enter();
                return true;
            }

            return condition.executeBoolean(frame);
        }

    }

}
