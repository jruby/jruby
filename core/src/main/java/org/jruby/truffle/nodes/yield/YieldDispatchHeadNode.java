/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.yield;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;

public class YieldDispatchHeadNode extends Node {

    @Child protected YieldDispatchNode dispatch;

    public YieldDispatchHeadNode(RubyContext context) {
        dispatch = new UninitializedYieldDispatchNode(context);

    }

    public Object dispatch(VirtualFrame frame, RubyProc block, Object... argumentsObjects) {
        return dispatch.dispatch(frame, block, argumentsObjects);
    }

    public Object dispatchWithModifiedBlock(VirtualFrame frame, RubyProc block, RubyProc modifiedBlock, Object... argumentsObjects) {
        return dispatch.dispatchWithModifiedBlock(frame, block, modifiedBlock, argumentsObjects);
    }

    public Object dispatchWithModifiedSelf(VirtualFrame frame, RubyProc block, Object self, Object... argumentsObjects) {
        return dispatch.dispatchWithModifiedSelf(frame, block, self, argumentsObjects);
    }

    public YieldDispatchNode getDispatch() {
        return dispatch;
    }

}
