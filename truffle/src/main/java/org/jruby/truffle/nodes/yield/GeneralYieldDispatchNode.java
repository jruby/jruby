/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

@NodeInfo(cost = NodeCost.MEGAMORPHIC)
public class GeneralYieldDispatchNode extends YieldDispatchNode {

    @Child private IndirectCallNode callNode;

    public GeneralYieldDispatchNode(RubyContext context) {
        super(context);
        callNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    protected boolean guard(RubyProc block) {
        return true;
    }

    @Override
    public Object dispatchWithSelfAndBlock(VirtualFrame frame, RubyProc block, Object self, RubyProc modifiedBlock, Object... argumentsObjects) {
        return callNode.call(frame, block.getCallTargetForBlocks(),
                RubyArguments.pack(block.getMethod(), block.getDeclarationFrame(), self, modifiedBlock, argumentsObjects));
    }

}
