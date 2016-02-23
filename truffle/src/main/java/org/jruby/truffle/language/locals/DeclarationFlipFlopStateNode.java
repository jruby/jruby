/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.locals;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.language.arguments.RubyArguments;

public class DeclarationFlipFlopStateNode extends FlipFlopStateNode {

    private final int frameLevel;
    private final FrameSlot frameSlot;

    public DeclarationFlipFlopStateNode(int frameLevel, FrameSlot frameSlot) {
        this.frameLevel = frameLevel;
        this.frameSlot = frameSlot;
    }

    @Override
    public boolean getState(VirtualFrame frame) {
        final MaterializedFrame declarationFrame = RubyArguments.getDeclarationFrame(frame, frameLevel);

        try {
            return declarationFrame.getBoolean(frameSlot);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void setState(VirtualFrame frame, boolean state) {
        final MaterializedFrame declarationFrame = RubyArguments.getDeclarationFrame(frame, frameLevel);
        declarationFrame.setBoolean(frameSlot, state);
    }

}
