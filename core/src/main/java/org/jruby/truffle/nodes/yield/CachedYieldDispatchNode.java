/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class CachedYieldDispatchNode extends YieldDispatchNode {

    @Child protected DirectCallNode callNode;
    @Child protected YieldDispatchNode next;

    public CachedYieldDispatchNode(RubyContext context, RubyProc block, YieldDispatchNode next) {
        super(context);
        callNode = Truffle.getRuntime().createDirectCallNode(block.getMethod().getCallTarget());
        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        RubyNode.notDesignedForCompilation();

        if (block.getMethod().getCallTarget() != callNode.getCallTarget()) {
            return next.dispatch(frame, block, argumentsObjects);
        }

        return callNode.call(frame, RubyArguments.pack(block.getMethod().getDeclarationFrame(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope(), argumentsObjects));
    }
}
