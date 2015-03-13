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
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyProc;

public class YieldDispatchHeadNode extends Node {

    @Child private YieldDispatchNode dispatch;

    public YieldDispatchHeadNode(RubyContext context) {
        dispatch = new UninitializedYieldDispatchNode(context);

    }

    public Object dispatch(VirtualFrame frame, RubyProc block, Object... argumentsObjects) {
        return dispatch.dispatchWithSelfAndBlock(frame, block, block.getSelfCapturedInScope(), block.getBlockCapturedInScope(), argumentsObjects);
    }

    public Object dispatchWithModifiedBlock(VirtualFrame frame, RubyProc block, RubyProc modifiedBlock, Object... argumentsObjects) {
        return dispatch.dispatchWithSelfAndBlock(frame, block, block.getSelfCapturedInScope(), modifiedBlock, argumentsObjects);
    }

    public Object dispatchWithModifiedSelf(VirtualFrame currentFrame, RubyProc block, Object self, Object... argumentsObjects) {
        // TODO: assumes this also changes the default definee.

        Frame frame = block.getDeclarationFrame();
        FrameSlot slot = frame.getFrameDescriptor().findOrAddFrameSlot(RubyModule.VISIBILITY_FRAME_SLOT_ID, "dynamic visibility for def", FrameSlotKind.Object);
        Object oldVisibility = frame.getValue(slot);

        try {
            frame.setObject(slot, Visibility.PUBLIC);

            return dispatch.dispatchWithSelfAndBlock(currentFrame, block, self, block.getBlockCapturedInScope(), argumentsObjects);
        } finally {
            frame.setObject(slot, oldVisibility);
        }
    }

    public YieldDispatchNode getDispatch() {
        return dispatch;
    }

}
