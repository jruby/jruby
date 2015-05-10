/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.locals;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyArguments;

public class DeclarationFlipFlopStateNode extends FlipFlopStateNode {

    private final int level;
    private final FrameSlot frameSlot;

    public DeclarationFlipFlopStateNode(SourceSection sourceSection, int level,
                                        FrameSlot frameSlot) {
        super(sourceSection);
        this.level = level;
        this.frameSlot = frameSlot;
    }

    @Override
    public boolean getState(VirtualFrame frame) {
        final MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, level);

        try {
            return levelFrame.getBoolean(frameSlot);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void setState(VirtualFrame frame, boolean state) {
        final MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, level);
        levelFrame.setBoolean(frameSlot, state);
    }

}
