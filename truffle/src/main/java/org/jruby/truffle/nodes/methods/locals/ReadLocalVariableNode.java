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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.ReadNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.translator.BodyTranslator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class ReadLocalVariableNode extends FrameSlotNode implements ReadNode {

    public ReadLocalVariableNode(RubyContext context, SourceSection sourceSection, FrameSlot slot) {
        super(context, sourceSection, slot);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public boolean doBoolean(VirtualFrame frame) throws FrameSlotTypeException {
        return getBoolean(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public int doFixnum(VirtualFrame frame) throws FrameSlotTypeException {
        return getFixnum(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public long doLongFixnum(VirtualFrame frame) throws FrameSlotTypeException {
        return getLongFixnum(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public double doFloat(VirtualFrame frame) throws FrameSlotTypeException {
        return getFloat(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public Object doObject(VirtualFrame frame) throws FrameSlotTypeException {
        return getObject(frame);
    }

    @Specialization
    public Object doValue(VirtualFrame frame) {
        return getValue(frame);
    }

    @Override
    public RubyNode makeWriteNode(RubyNode rhs) {
        return WriteLocalVariableNodeGen.create(getContext(), getSourceSection(), frameSlot, rhs);
    }

    public static final Set<String> ALWAYS_DEFINED_GLOBALS = new HashSet<>(Arrays.asList("$~"));

    @Override
    public Object isDefined(VirtualFrame frame) {
        // TODO(CS): copy and paste of ReadLevelVariableNode
        if (BodyTranslator.FRAME_LOCAL_GLOBAL_VARIABLES.contains(frameSlot.getIdentifier())) {
            if (ALWAYS_DEFINED_GLOBALS.contains(frameSlot.getIdentifier()) || doValue(frame) != nil()) {
                return getContext().makeString("global-variable");
            } else {
                return nil();
            }
        } else {
            return getContext().makeString("local-variable");
        }
    }

}
