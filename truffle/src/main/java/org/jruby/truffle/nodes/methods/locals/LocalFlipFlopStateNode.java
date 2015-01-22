/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods.locals;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class LocalFlipFlopStateNode extends FlipFlopStateNode {

    private final FrameSlot frameSlot;

    public LocalFlipFlopStateNode(SourceSection sourceSection, FrameSlot frameSlot) {
        super(sourceSection);
        this.frameSlot = frameSlot;
    }

    @Override
    public boolean getState(VirtualFrame frame) {
        try {
            return frame.getBoolean(frameSlot);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void setState(VirtualFrame frame, boolean state) {
        frame.setBoolean(frameSlot, state);
    }

}
