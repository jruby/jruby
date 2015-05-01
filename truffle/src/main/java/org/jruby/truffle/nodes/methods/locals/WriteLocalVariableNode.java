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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.WriteNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value = "rhs", type = RubyNode.class)
public abstract class WriteLocalVariableNode extends FrameSlotNode implements WriteNode {

    public WriteLocalVariableNode(RubyContext context, SourceSection sourceSection, FrameSlot frameSlot) {
        super(context, sourceSection, frameSlot);
    }

    @Specialization(guards = "isBooleanKind(frame)")
    public boolean doFixnum(VirtualFrame frame, boolean value) {
        setBoolean(frame, value);
        return value;
    }

    @Specialization(guards = "isFixnumKind(frame)")
    public int doFixnum(VirtualFrame frame, int value) {
        setFixnum(frame, value);
        return value;
    }

    @Specialization(guards = "isLongFixnumKind(frame)")
    public long doLongFixnum(VirtualFrame frame, long value) {
        setLongFixnum(frame, value);
        return value;
    }

    @Specialization(guards = "isFloatKind(frame)")
    public double doFloat(VirtualFrame frame, double value) {
        setFloat(frame, value);
        return value;
    }

    @Specialization(guards = "isObjectKind(frame)")
    public Object doObject(VirtualFrame frame, Object value) {
        setObject(frame, value);
        return value;
    }

    @Override
    public RubyNode makeReadNode() {
        return ReadLocalVariableNodeGen.create(getContext(), getSourceSection(), frameSlot);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        return getContext().makeString("assignment");
    }

}
