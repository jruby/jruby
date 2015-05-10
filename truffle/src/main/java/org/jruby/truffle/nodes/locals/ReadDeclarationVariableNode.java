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
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.ReadNode;
import org.jruby.truffle.translator.Translator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ReadDeclarationVariableNode extends RubyNode implements ReadNode {

    private final int frameDepth;

    @Child private ReadFrameSlotNode readFrameSlotNode;

    public ReadDeclarationVariableNode(RubyContext context, SourceSection sourceSection,
                                       int frameDepth, FrameSlot slot) {
        super(context, sourceSection);
        readFrameSlotNode = ReadFrameSlotNodeGen.create(slot);
        this.frameDepth = frameDepth;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readFrameSlotNode.executeRead(RubyArguments.getDeclarationFrame(frame, frameDepth));
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return new WriteDeclarationVariableNode(getContext(), getSourceSection(),
                rhs, frameDepth, readFrameSlotNode.getFrameSlot());
    }

    public static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Arrays.asList("$~"));

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (Translator.FRAME_LOCAL_GLOBAL_VARIABLES.contains(readFrameSlotNode.getFrameSlot().getIdentifier())) {
            if (ALWAYS_DEFINED_GLOBALS.contains(readFrameSlotNode.getFrameSlot().getIdentifier())
                    || readFrameSlotNode.executeRead(RubyArguments.getDeclarationFrame(frame, frameDepth)) != nil()) {
                return getContext().makeString("global-variable");
            } else {
                return nil();
            }
        } else {
            return getContext().makeString("local-variable");
        }
    }

}
