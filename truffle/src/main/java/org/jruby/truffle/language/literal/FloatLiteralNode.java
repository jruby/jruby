/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public class FloatLiteralNode extends RubyNode {

    private final double value;

    public FloatLiteralNode(RubyContext context, SourceSection sourceSection, double value) {
        super(context, sourceSection);
        this.value = value;
    }

    @Override
    public double executeDouble(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeDouble(frame);
    }

}
