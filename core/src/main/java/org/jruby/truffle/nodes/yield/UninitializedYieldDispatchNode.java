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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedYieldDispatchNode extends YieldDispatchNode {

    private static final int MAX_DISPATCHES = 4;
    private static final int MAX_DEPTH = MAX_DISPATCHES + 1; // MAX_DISPATCHES + UninitializedDispatchNode

    public UninitializedYieldDispatchNode(RubyContext context) {
        super(context);
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyProc block, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        int depth = getDepth();
        final YieldDispatchHeadNode dispatchHead = (YieldDispatchHeadNode) NodeUtil.getNthParent(this, depth);

        if (depth > MAX_DEPTH) {
            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, getEncapsulatingSourceSection().getSource().getName(), getEncapsulatingSourceSection().getStartLine(), "resorting to a general yield node");

            final GeneralYieldDispatchNode newGeneralYield = new GeneralYieldDispatchNode(getContext());
            dispatchHead.getDispatch().replace(newGeneralYield);
            return newGeneralYield.dispatch(frame, block, argumentsObjects);
        }

        final CachedYieldDispatchNode dispatch = new CachedYieldDispatchNode(getContext(), block, this);
        replace(dispatch);
        return dispatch.dispatch(frame, block, argumentsObjects);
    }

    public int getDepth() {
        int depth = 1;
        Node parent = this.getParent();

        while (!(parent instanceof YieldDispatchHeadNode)) {
            parent = parent.getParent();
            depth++;
        }

        return depth;
    }

}
