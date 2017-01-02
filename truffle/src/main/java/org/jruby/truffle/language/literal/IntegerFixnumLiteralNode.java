/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.RubyNode;

@NodeInfo(cost = NodeCost.NONE)
public class IntegerFixnumLiteralNode extends RubyNode {

    private final int value;

    public IntegerFixnumLiteralNode(int value) {
        this.value = value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeInteger(frame);
    }

    @Override
    public int executeInteger(VirtualFrame frame) {
        return value;
    }

}
