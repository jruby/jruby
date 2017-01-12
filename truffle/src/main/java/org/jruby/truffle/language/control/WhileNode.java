/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.LoopConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.RubyNode;

public final class WhileNode extends RubyNode {

    @Child private LoopNode loopNode;

    public WhileNode(RepeatingNode repeatingNode) {
        loopNode = Truffle.getRuntime().createLoopNode(repeatingNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loopNode.executeLoop(frame);
        return nil();
    }

    private static abstract class WhileRepeatingBaseNode extends Node implements RepeatingNode {

        protected final RubyContext context;

        @Child protected BooleanCastNode condition;
        @Child protected RubyNode body;

        protected final LoopConditionProfile conditionProfile = LoopConditionProfile.createCountingProfile();
        protected final BranchProfile redoUsed = BranchProfile.create();
        protected final BranchProfile nextUsed = BranchProfile.create();

        public WhileRepeatingBaseNode(RubyContext context, RubyNode condition, RubyNode body) {
            this.context = context;
            this.condition = BooleanCastNodeGen.create(condition);
            this.body = body;
        }

        @Override
        public String toString() {
            SourceSection sourceSection = getEncapsulatingSourceSection();
            if (sourceSection != null && sourceSection.isAvailable()) {
                return "while loop at " + RubyLanguage.fileLine(sourceSection);
            } else {
                return "while loop";
            }
        }

    }

    public static class WhileRepeatingNode extends WhileRepeatingBaseNode implements RepeatingNode {

        public WhileRepeatingNode(RubyContext context, RubyNode condition, RubyNode body) {
            super(context, condition, body);
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (!conditionProfile.profile(condition.executeBoolean(frame))) {
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

    public static class DoWhileRepeatingNode extends WhileRepeatingBaseNode implements RepeatingNode {

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

            return conditionProfile.profile(condition.executeBoolean(frame));
        }

    }

}
