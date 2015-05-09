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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;

public abstract class WriteAbstractFrameSlotNode extends Node {

    protected final FrameSlot frameSlot;

    public WriteAbstractFrameSlotNode(FrameSlot frameSlot) {
        assert frameSlot != null;
        this.frameSlot = frameSlot;
    }

    public abstract Object executeWrite(Frame frame, Object value);

    @Specialization(guards = "isBooleanKind(frame)")
    public boolean doFixnum(Frame frame, boolean value) {
        setBoolean(frame, value);
        return value;
    }

    @Specialization(guards = "isFixnumKind(frame)")
    public int doFixnum(Frame frame, int value) {
        setFixnum(frame, value);
        return value;
    }

    @Specialization(guards = "isLongFixnumKind(frame)")
    public long doLongFixnum(Frame frame, long value) {
        setLongFixnum(frame, value);
        return value;
    }

    @Specialization(guards = "isFloatKind(frame)")
    public double doFloat(Frame frame, double value) {
        setFloat(frame, value);
        return value;
    }

    @Specialization(guards = "isObjectKind(frame)")
    public Object doObject(Frame frame, Object value) {
        setObject(frame, value);
        return value;
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

    protected final void setBoolean(Frame frame, boolean value) {
        frame.setBoolean(frameSlot, value);
    }

    protected final void setFixnum(Frame frame, int value) {
        frame.setInt(frameSlot, value);
    }

    protected final void setLongFixnum(Frame frame, long value) {
        frame.setLong(frameSlot, value);
    }

    protected final void setFloat(Frame frame, double value) {
        frame.setDouble(frameSlot, value);
    }

    protected final void setObject(Frame frame, Object value) {
        frame.setObject(frameSlot, value);
    }

    protected final boolean getBoolean(Frame frame) throws FrameSlotTypeException {
        return frame.getBoolean(frameSlot);
    }

    protected final int getFixnum(Frame frame) throws FrameSlotTypeException {
        return frame.getInt(frameSlot);
    }

    protected final long getLongFixnum(Frame frame) throws FrameSlotTypeException {
        return frame.getLong(frameSlot);
    }

    protected final double getFloat(Frame frame) throws FrameSlotTypeException {
        return frame.getDouble(frameSlot);
    }

    protected final Object getObject(Frame frame) throws FrameSlotTypeException {
        return frame.getObject(frameSlot);
    }

    protected final Object getValue(Frame frame) {
        return frame.getValue(frameSlot);
    }

    protected final boolean isBooleanKind(Frame frame) {
        return isKind(FrameSlotKind.Boolean);
    }

    protected final boolean isFixnumKind(Frame frame) {
        return isKind(FrameSlotKind.Int);
    }

    protected final boolean isLongFixnumKind(Frame frame) {
        return isKind(FrameSlotKind.Long);
    }

    protected final boolean isFloatKind(Frame frame) {
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
        return frameSlot.getKind() == kind || initialSetKind(kind);
    }

    private boolean initialSetKind(FrameSlotKind kind) {
        if (frameSlot.getKind() == FrameSlotKind.Illegal) {
            CompilerDirectives.transferToInterpreter();
            frameSlot.setKind(kind);
            return true;
        }
        return false;
    }

}
