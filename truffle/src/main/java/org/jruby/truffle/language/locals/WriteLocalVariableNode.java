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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

public class WriteLocalVariableNode extends RubyNode {

    private final FrameSlot frameSlot;

    @Child private RubyNode valueNode;
    @Child private WriteFrameSlotNode writeFrameSlotNode;

    public static WriteLocalVariableNode createWriteLocalVariableNode(RubyContext context, SourceSection sourceSection,
                                                                      FrameSlot frameSlot, RubyNode valueNode) {
        if (context.getCallGraph() == null) {
            return new WriteLocalVariableNode(context, sourceSection, frameSlot, valueNode);
        } else {
            return new InstrumentedWriteLocalVariableNode(context, sourceSection, frameSlot, valueNode);
        }
    }

    protected WriteLocalVariableNode(RubyContext context, SourceSection sourceSection,
                                     FrameSlot frameSlot, RubyNode valueNode) {
        super(context, sourceSection);
        this.frameSlot = frameSlot;
        this.valueNode = valueNode;

    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (writeFrameSlotNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            writeFrameSlotNode = insert(WriteFrameSlotNodeGen.create(frameSlot));
        }

        final Object value = valueNode.execute(frame);
        return writeFrameSlotNode.executeWrite(frame, value);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

}
