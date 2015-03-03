/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.util.cli.Options;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class CachedYieldDispatchNode extends YieldDispatchNode {

    private static final boolean INLINER_ALWAYS_CLONE_YIELD = Options.TRUFFLE_INLINER_ALWAYS_CLONE_YIELD.load();
    private static final boolean INLINER_ALWAYS_INLINE_YIELD = Options.TRUFFLE_INLINER_ALWAYS_INLINE_YIELD.load();

    @Child private DirectCallNode callNode;
    @Child private YieldDispatchNode next;

    public CachedYieldDispatchNode(RubyContext context, RubyProc block, YieldDispatchNode next) {
        super(context);

        callNode = Truffle.getRuntime().createDirectCallNode(block.getCallTargetForBlocks());
        insert(callNode);

        if (INLINER_ALWAYS_CLONE_YIELD && callNode.isCallTargetCloningAllowed()) {
            callNode.cloneCallTarget();
        }

        if (INLINER_ALWAYS_INLINE_YIELD && callNode.isInlinable()) {
            callNode.forceInlining();
        }

        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        if (block.getCallTargetForBlocks() != callNode.getCallTarget()) {
            return next.dispatch(frame, block, argumentsObjects);
        }

        return callNode.call(frame, RubyArguments.pack(block.getMethod(), block.getDeclarationFrame(), block.getSelfCapturedInScope(), block.getBlockCapturedInScope(), argumentsObjects));
    }

    @Override
    public Object dispatchWithModifiedBlock(VirtualFrame frame, RubyProc block, RubyProc modifiedBlock, Object[] argumentsObjects) {
        if (block.getCallTargetForBlocks() != callNode.getCallTarget()) {
            return next.dispatch(frame, block, argumentsObjects);
        }

        return callNode.call(frame, RubyArguments.pack(block.getMethod(), block.getDeclarationFrame(), block.getSelfCapturedInScope(), modifiedBlock, argumentsObjects));
    }

    @Override
    public Object dispatchWithModifiedSelf(VirtualFrame frame, RubyProc block, Object self, Object... argumentsObjects) {
        if (block.getCallTargetForBlocks() != callNode.getCallTarget()) {
            return next.dispatchWithModifiedSelf(frame, block, self, argumentsObjects);
        }

        return callNode.call(frame, RubyArguments.pack(block.getMethod(), block.getDeclarationFrame(), self, block.getBlockCapturedInScope(), argumentsObjects));
    }

    @Override
    public String toString() {
        return String.format("CachedYieldDispatchNode(%s)", callNode.getCallTarget());
    }

}
