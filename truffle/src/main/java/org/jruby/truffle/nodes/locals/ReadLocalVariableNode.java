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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.ReadNode;
import org.jruby.truffle.translator.Translator;

public class ReadLocalVariableNode extends RubyNode implements ReadNode {

    @Child private ReadFrameSlotNode readFrameSlotNode;

    public ReadLocalVariableNode(RubyContext context, SourceSection sourceSection,
                                 FrameSlot slot) {
        super(context, sourceSection);
        readFrameSlotNode = ReadFrameSlotNodeGen.create(slot);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readFrameSlotNode.executeRead(frame);
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteLocalVariableNode(getContext(), getSourceSection(), rhs,
                readFrameSlotNode.getFrameSlot());
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (Translator.FRAME_LOCAL_GLOBAL_VARIABLES.contains(readFrameSlotNode.getFrameSlot().getIdentifier())) {
            if (Translator.ALWAYS_DEFINED_GLOBALS.contains(readFrameSlotNode.getFrameSlot().getIdentifier())
                    || readFrameSlotNode.executeRead(frame) != nil()) {
                return getContext().makeString("global-variable");
            } else {
                return nil();
            }
        } else {
            return getContext().makeString("local-variable");
        }
    }

}
