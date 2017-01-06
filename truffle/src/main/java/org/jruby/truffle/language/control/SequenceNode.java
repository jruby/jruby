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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public final class SequenceNode extends RubyNode {

    @Children private final RubyNode[] body;

    public SequenceNode(RubyNode... body) {
        this.body = body;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        for (int n = 0; n < body.length - 1; n++) {
            body[n].executeVoid(frame);
        }

        return body[body.length - 1].execute(frame);
    }

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (int n = 0; n < body.length; n++) {
            body[n].executeVoid(frame);
        }
    }

    public RubyNode[] getSequence() {
        return body;
    }

}
