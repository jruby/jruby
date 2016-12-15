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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.language.RubyGuards;

@ImportStatic(RubyGuards.class)
public abstract class WriteFrameSlotNode extends Node {

    private final FrameSlot frameSlot;

    public WriteFrameSlotNode(FrameSlot frameSlot) {
        this.frameSlot = frameSlot;
    }

    public abstract Object executeWrite(Frame frame, Object value);

    @Specialization(guards = "checkBooleanKind(frame)")
    public boolean writeBoolean(Frame frame, boolean value) {
        frame.setBoolean(frameSlot, value);
        return value;
    }

    @Specialization(guards = "checkIntegerKind(frame)")
    public int writeInteger(Frame frame, int value) {
        frame.setInt(frameSlot, value);
        return value;
    }

    @Specialization(guards = "checkLongKind(frame)")
    public long writeLong(Frame frame, long value) {
        frame.setLong(frameSlot, value);
        return value;
    }

    @Specialization(guards = "checkDoubleKind(frame)")
    public double writeDouble(Frame frame, double value) {
        frame.setDouble(frameSlot, value);
        return value;
    }

    @Specialization(guards = "checkObjectKind(frame)",
            contains = { "writeBoolean", "writeInteger", "writeLong", "writeDouble" })
    public Object writeObject(Frame frame, Object value) {
        frame.setObject(frameSlot, value);
        return value;
    }

    protected boolean checkBooleanKind(Frame frame) {
        return checkKind(FrameSlotKind.Boolean);
    }

    protected boolean checkIntegerKind(Frame frame) {
        return checkKind(FrameSlotKind.Int);
    }

    protected boolean checkLongKind(Frame frame) {
        return checkKind(FrameSlotKind.Long);
    }

    protected boolean checkDoubleKind(Frame frame) {
        return checkKind(FrameSlotKind.Double);
    }

    protected boolean checkObjectKind(Frame frame) {
        if (frameSlot.getKind() != FrameSlotKind.Object) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(FrameSlotKind.Object);
        }

        return true;
    }

    private boolean checkKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == kind) {
            return true;
        } else {
            return initialSetKind(kind);
        }
    }

    private boolean initialSetKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            frameSlot.setKind(kind);
            return true;
        }

        return false;
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

}
