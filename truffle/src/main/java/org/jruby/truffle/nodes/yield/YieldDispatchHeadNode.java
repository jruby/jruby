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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;

public class YieldDispatchHeadNode extends Node {

    @Child private YieldDispatchNode dispatch;

    public YieldDispatchHeadNode(RubyContext context) {
        dispatch = new UninitializedYieldDispatchNode(context);

    }

    public Object dispatch(VirtualFrame frame, RubyBasicObject block, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        return dispatch.dispatchWithSelfAndBlock(frame, block, ProcNodes.getSelfCapturedInScope(block), ProcNodes.getBlockCapturedInScope(block), argumentsObjects);
    }

    public Object dispatchWithModifiedBlock(VirtualFrame frame, RubyBasicObject block, RubyBasicObject modifiedBlock, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);
        assert modifiedBlock == null || RubyGuards.isRubyProc(modifiedBlock);
        return dispatch.dispatchWithSelfAndBlock(frame, block, ProcNodes.getSelfCapturedInScope(block), modifiedBlock, argumentsObjects);
    }

    public Object dispatchWithModifiedSelf(VirtualFrame currentFrame, RubyBasicObject block, Object self, Object... argumentsObjects) {
        assert block == null || RubyGuards.isRubyProc(block);

        // TODO: assumes this also changes the default definee.

        Frame frame = ProcNodes.getDeclarationFrame(block);

        if (frame != null) {
            FrameSlot slot = getVisibilitySlot(frame);
            Object oldVisibility = frame.getValue(slot);

            try {
                frame.setObject(slot, Visibility.PUBLIC);

                return dispatch.dispatchWithSelfAndBlock(currentFrame, block, self, ProcNodes.getBlockCapturedInScope(block), argumentsObjects);
            } finally {
                frame.setObject(slot, oldVisibility);
            }
        } else {
            return dispatch.dispatchWithSelfAndBlock(currentFrame, block, self, ProcNodes.getBlockCapturedInScope(block), argumentsObjects);
        }
    }

    @TruffleBoundary
    private FrameSlot getVisibilitySlot(Frame frame) {
        return frame.getFrameDescriptor().findOrAddFrameSlot(RubyModule.VISIBILITY_FRAME_SLOT_ID, "dynamic visibility for def", FrameSlotKind.Object);
    }

    public YieldDispatchNode getDispatch() {
        return dispatch;
    }

}
