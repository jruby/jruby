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
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;

public class WriteDeclarationVariableNode extends RubyNode {

    private final int frameDepth;
    private final FrameSlot frameSlot;

    @Child private RubyNode valueNode;
    @Child private WriteFrameSlotNode writeFrameSlotNode;

    public WriteDeclarationVariableNode(RubyContext context, SourceSection sourceSection,
                                        FrameSlot frameSlot, int frameDepth, RubyNode valueNode) {
        super(context, sourceSection);
        this.frameDepth = frameDepth;
        this.frameSlot = frameSlot;
        this.valueNode = valueNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (writeFrameSlotNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeFrameSlotNode = insert(WriteFrameSlotNodeGen.create(frameSlot));
        }

        final MaterializedFrame declarationFrame = RubyArguments.getDeclarationFrame(frame, frameDepth);
        final Object value = valueNode.execute(frame);
        return writeFrameSlotNode.executeWrite(declarationFrame, value);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

}
