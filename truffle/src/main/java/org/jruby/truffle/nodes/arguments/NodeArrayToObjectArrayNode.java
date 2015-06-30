/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.arguments;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;

public class NodeArrayToObjectArrayNode extends RubyNode {

    @Children final RubyNode[] nodes;

    public NodeArrayToObjectArrayNode(RubyContext context, SourceSection sourceSection, RubyNode[] nodes) {
        super(context, sourceSection);
        this.nodes = nodes;
    }

    @Override
    @ExplodeLoop
    public Object[] execute(VirtualFrame frame) {
        final Object[] values = new Object[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            values[i] = nodes[i].execute(frame);
        }

        return values;
    }

}
