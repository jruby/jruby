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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

public class RepeatLoopNode extends FormatNode {

    private final int count;

    @Child private FormatNode child;

    public RepeatLoopNode(RubyContext context, int count, FormatNode child) {
        super(context);
        this.count = count;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        for (int i = 0; i < count; i++) {
            child.execute(frame);
        }

        if (CompilerDirectives.inInterpreter()) {
            LoopNode.reportLoopCount(this, count);
        }

        return null;
    }

}
