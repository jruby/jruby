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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;

import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.util.cli.Options;

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedYieldDispatchNode extends YieldDispatchNode {

    private int depth = 0;

    public UninitializedYieldDispatchNode(RubyContext context) {
        super(context);
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        depth++;

        if (depth == DispatchNode.DISPATCH_POLYMORPHIC_MAX) {
            final YieldDispatchHeadNode dispatchHead = (YieldDispatchHeadNode) NodeUtil.getNthParent(this, depth);
            final GeneralYieldDispatchNode newGeneralYield = new GeneralYieldDispatchNode(getContext());
            dispatchHead.getDispatch().replace(newGeneralYield);
            return newGeneralYield.dispatch(frame, block, argumentsObjects);
        }

        final CachedYieldDispatchNode dispatch = new CachedYieldDispatchNode(getContext(), block, this);
        replace(dispatch);
        return dispatch.dispatch(frame, block, argumentsObjects);
    }

    @Override
    public Object dispatchWithModifiedBlock(VirtualFrame frame, RubyProc block, RubyProc modifiedBlock, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        depth++;

        if (depth == DispatchNode.DISPATCH_POLYMORPHIC_MAX) {
            final YieldDispatchHeadNode dispatchHead = (YieldDispatchHeadNode) NodeUtil.getNthParent(this, depth);
            final GeneralYieldDispatchNode newGeneralYield = new GeneralYieldDispatchNode(getContext());
            dispatchHead.getDispatch().replace(newGeneralYield);
            return newGeneralYield.dispatch(frame, block, argumentsObjects);
        }

        final CachedYieldDispatchNode dispatch = new CachedYieldDispatchNode(getContext(), block, this);
        replace(dispatch);
        return dispatch.dispatchWithModifiedBlock(frame, block, modifiedBlock, argumentsObjects);
    }

    @Override
    public Object dispatchWithModifiedSelf(VirtualFrame frame, RubyProc block, Object self, Object... argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        depth++;

        if (depth == DispatchNode.DISPATCH_POLYMORPHIC_MAX) {
            final YieldDispatchHeadNode dispatchHead = (YieldDispatchHeadNode) NodeUtil.getNthParent(this, depth);
            final GeneralYieldDispatchNode newGeneralYield = new GeneralYieldDispatchNode(getContext());
            dispatchHead.getDispatch().replace(newGeneralYield);
            return newGeneralYield.dispatchWithModifiedSelf(frame, block, self, argumentsObjects);
        }

        final CachedYieldDispatchNode dispatch = new CachedYieldDispatchNode(getContext(), block, this);
        replace(dispatch);
        return dispatch.dispatchWithModifiedSelf(frame, block, self, argumentsObjects);
    }

}
