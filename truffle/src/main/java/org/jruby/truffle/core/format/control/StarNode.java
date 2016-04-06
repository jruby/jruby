/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.control;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

public class StarNode extends FormatNode {

    @Child private LoopNode loopNode;

    public StarNode(RubyContext context, FormatNode child) {
        super(context);
        loopNode = Truffle.getRuntime().createLoopNode(new StarRepeatingNode(child));
    }

    @Override
    public Object execute(VirtualFrame frame) {
        loopNode.executeLoop(frame);
        return null;
    }

    private class StarRepeatingNode extends Node implements RepeatingNode {

        @Child private FormatNode child;

        private final ConditionProfile conditionProfile = ConditionProfile.createBinaryProfile();

        public StarRepeatingNode(FormatNode child) {
            this.child = child;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (conditionProfile.profile(getSourcePosition(frame) >= getSourceLength(frame))) {
                return false;
            }

            child.execute(frame);
            return true;
        }
    }

}
