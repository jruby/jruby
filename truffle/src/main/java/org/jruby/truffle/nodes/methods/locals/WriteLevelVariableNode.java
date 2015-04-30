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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.WriteNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "rhs", type = RubyNode.class)
public abstract class WriteLevelVariableNode extends FrameSlotNode implements WriteNode {

    private final int varLevel;

    public WriteLevelVariableNode(RubyContext context, SourceSection sourceSection, FrameSlot frameSlot, int level) {
        super(context, sourceSection, frameSlot);
        this.varLevel = level;
    }

    @Specialization(guards = "isBooleanKind(frame)")
    public boolean doBoolean(VirtualFrame frame, boolean value) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        setBoolean(levelFrame, value);
        return value;
    }

    @Specialization(guards = "isFixnumKind(frame)")
    public int doFixnum(VirtualFrame frame, int value) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        setFixnum(levelFrame, value);
        return value;
    }

    @Specialization(guards = "isLongFixnumKind(frame)")
    public long doLongFixnum(VirtualFrame frame, long value) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        setLongFixnum(levelFrame, value);
        return value;
    }

    @Specialization(guards = "isFloatKind(frame)")
    public double doFloat(VirtualFrame frame, double value) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        setFloat(levelFrame, value);
        return value;
    }

    @Specialization(guards = "isObjectKind(frame)")
    public Object doObject(VirtualFrame frame, Object value) {
        MaterializedFrame levelFrame = RubyArguments.getDeclarationFrame(frame, varLevel);
        setObject(levelFrame, value);
        return value;
    }

    @Override
    public RubyNode makeReadNode() {
        return ReadLevelVariableNodeGen.create(getContext(), getSourceSection(), frameSlot, varLevel);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("assignment");
    }

}
