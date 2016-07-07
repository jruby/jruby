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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;

public class WriteLocalVariableNode extends RubyNode {

    private final FrameSlot frameSlot;

    @Child private RubyNode valueNode;
    @Child private WriteFrameSlotNode writeFrameSlotNode;

    public WriteLocalVariableNode(RubyContext context, SourceSection sourceSection,
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
        recordWrite(value);
        return writeFrameSlotNode.executeWrite(frame, value);
    }

    @TruffleBoundary
    private void recordWrite(Object value) {
        final RubyContext context = getContext();

        if (context.getCallGraph() != null) {
            final String name = frameSlot.getIdentifier().toString();
            final String type = Layouts.CLASS.getFields(context.getCoreLibrary().getLogicalClass(value)).getName();
            context.getCallGraph().recordLocalWrite((RubyRootNode) getRootNode(), name, type);
        }
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return coreStrings().ASSIGNMENT.createInstance();
    }

}
