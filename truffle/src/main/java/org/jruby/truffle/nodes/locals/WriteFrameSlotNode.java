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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;

public abstract class WriteFrameSlotNode extends Node {

    protected final FrameSlot frameSlot;

    public WriteFrameSlotNode(FrameSlot frameSlot) {
        assert frameSlot != null;
        this.frameSlot = frameSlot;
    }

    public abstract Object executeWrite(Frame frame, Object value);

    @Specialization(guards = "isBooleanKind(frame)")
    public boolean writeBoolean(Frame frame, boolean value) {
        frame.setBoolean(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isIntegerKind(frame)")
    public int writeInteger(Frame frame, int value) {
        frame.setInt(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isLongKind(frame)")
    public long writeLong(Frame frame, long value) {
        frame.setLong(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isDoubleKind(frame)")
    public double writeDouble(Frame frame, double value) {
        frame.setDouble(frameSlot, value);
        return value;
    }

    @Specialization(guards = "isObjectKind(frame)")
    public Object writeObject(Frame frame, Object value) {
        frame.setObject(frameSlot, value);
        return value;
    }

    protected final boolean isBooleanKind(Frame frame) {
        return isKind(FrameSlotKind.Boolean);
    }

    protected final boolean isIntegerKind(Frame frame) {
        return isKind(FrameSlotKind.Int);
    }

    protected final boolean isLongKind(Frame frame) {
        return isKind(FrameSlotKind.Long);
    }

    protected final boolean isDoubleKind(Frame frame) {
        return isKind(FrameSlotKind.Double);
    }

    protected final boolean isObjectKind(Frame frame) {
        if (frameSlot.getKind() != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(FrameSlotKind.Object);
        }
        return true;
    }

    private boolean isKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == kind) {
            return true;
        } else {
            return initialSetKind(kind);
        }
    }

    private boolean initialSetKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

}
