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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.cli.Options;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class CachedYieldDispatchNode extends YieldDispatchNode {

    private static final boolean INLINER_ALWAYS_CLONE_YIELD = Options.TRUFFLE_INLINER_ALWAYS_CLONE_YIELD.load();
    private static final boolean INLINER_ALWAYS_INLINE_YIELD = Options.TRUFFLE_INLINER_ALWAYS_INLINE_YIELD.load();

    @Child private DirectCallNode callNode;
    @Child private YieldDispatchNode next;

    public CachedYieldDispatchNode(RubyContext context, DynamicObject block, YieldDispatchNode next) {
        super(context);

        assert RubyGuards.isRubyProc(block);

        callNode = Truffle.getRuntime().createDirectCallNode(ProcNodes.PROC_LAYOUT.getCallTargetForBlocks(block));
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
    protected boolean guard(DynamicObject block) {
        return ProcNodes.PROC_LAYOUT.getCallTargetForBlocks(block) == callNode.getCallTarget();
    }

    @Override
    protected YieldDispatchNode getNext() {
        return next;
    }

    @Override
    public Object dispatchWithSelfAndBlock(VirtualFrame frame, DynamicObject block, Object self, DynamicObject modifiedBlock, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        assert modifiedBlock == null || RubyGuards.isRubyProc(modifiedBlock);

        if (guard(block)) {
            return callNode.call(frame, RubyArguments.pack(ProcNodes.PROC_LAYOUT.getMethod(block), ProcNodes.PROC_LAYOUT.getDeclarationFrame(block), self, modifiedBlock, argumentsObjects));
        } else {
            return next.dispatchWithSelfAndBlock(frame, block, self, modifiedBlock, argumentsObjects);
        }
    }

    @Override
    public String toString() {
        return String.format("CachedYieldDispatchNode(%s)", callNode.getCallTarget());
    }

}
