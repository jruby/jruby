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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.ReadNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.BodyTranslator;

public abstract class ReadLevelVariableNode extends FrameSlotNode implements ReadNode {

    private final int varLevel;

    public ReadLevelVariableNode(RubyContext context, SourceSection sourceSection, FrameSlot slot, int level) {
        super(context, sourceSection, slot);
        this.varLevel = level;
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public boolean doBoolean(VirtualFrame frame) throws FrameSlotTypeException {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getBoolean(levelFrame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public int doFixnum(VirtualFrame frame) throws FrameSlotTypeException {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getFixnum(levelFrame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public long doLongFixnum(VirtualFrame frame) throws FrameSlotTypeException {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getLongFixnum(levelFrame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public double doFloat(VirtualFrame frame) throws FrameSlotTypeException {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getFloat(levelFrame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public Object doObject(VirtualFrame frame) throws FrameSlotTypeException {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getObject(levelFrame);
    }

    @Specialization
    public Object doValue(VirtualFrame frame) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        return getValue(levelFrame);
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return WriteLevelVariableNodeGen.create(getContext(), getSourceSection(), frameSlot, varLevel, rhs);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        // TODO(CS): copy and paste of ReadLocalVariableNode
        if (BodyTranslator.FRAME_LOCAL_GLOBAL_VARIABLES.contains(frameSlot.getIdentifier())) {
            if (ReadLocalVariableNode.ALWAYS_DEFINED_GLOBALS.contains(frameSlot.getIdentifier()) || doValue(frame) != nil()) {
                return getContext().makeString("global-variable");
            } else {
                return nil();
            }
        } else {
            return getContext().makeString("local-variable");
        }
    }

}
