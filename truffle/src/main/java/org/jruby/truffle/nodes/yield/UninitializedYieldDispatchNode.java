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

import java.util.concurrent.Callable;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;

import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedYieldDispatchNode extends YieldDispatchNode {

    private int depth = 0;

    public UninitializedYieldDispatchNode(RubyContext context) {
        super(context);
    }

    @Override
    protected boolean guard(RubyProc block) {
        return false;
    }

    @Override
    public Object dispatchWithSelfAndBlock(VirtualFrame frame, final RubyProc block, Object self, RubyProc modifiedBlock, Object... argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final YieldDispatchNode dispatch = atomic(new Callable<YieldDispatchNode>() {
            @Override
            public YieldDispatchNode call() {
                // First try to see if we did not a miss a specialization added by another thread.
                final YieldDispatchHeadNode dispatchHead = getHeadNode();
                final YieldDispatchNode first = dispatchHead.getDispatch();

                YieldDispatchNode lookupDispatch = first;
                while (lookupDispatch != null) {
                    if (lookupDispatch.guard(block)) {
                        // This one worked, no need to rewrite anything.
                        return lookupDispatch;
                    }
                    lookupDispatch = lookupDispatch.getNext();
                }

                final YieldDispatchNode newDispatchNode;
                if (depth < DispatchNode.DISPATCH_POLYMORPHIC_MAX) {
                    depth++;
                    newDispatchNode = new CachedYieldDispatchNode(getContext(), block, first);
                } else {
                    newDispatchNode = new GeneralYieldDispatchNode(getContext());
                }

                first.replace(newDispatchNode);
                return first;
            }
        });

        return dispatch.dispatchWithSelfAndBlock(frame, block, self, modifiedBlock, argumentsObjects);
    }

    private YieldDispatchHeadNode getHeadNode() {
        return NodeUtil.findParent(this, YieldDispatchHeadNode.class);
    }

}
