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

public abstract class ReadFrameSlotNode extends Node {

    protected final FrameSlot frameSlot;

    public ReadFrameSlotNode(FrameSlot slot) {
        assert slot != null;
        this.frameSlot = slot;
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public boolean doBoolean(Frame frame) throws FrameSlotTypeException {
        return getBoolean(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public int doFixnum(Frame frame) throws FrameSlotTypeException {
        return getFixnum(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public long doLongFixnum(Frame frame) throws FrameSlotTypeException {
        return getLongFixnum(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public double doFloat(Frame frame) throws FrameSlotTypeException {
        return getFloat(frame);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public Object doObject(Frame frame) throws FrameSlotTypeException {
        return getObject(frame);
    }

    @Specialization
    public Object doValue(Frame frame) {
        return getValue(frame);
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

    @Override
    public ReadFrameSlotNode copy() {
        return (ReadFrameSlotNode) super.copy();
    }

    protected final void setBoolean(Frame frame, boolean value) {
        frame.setBoolean(frameSlot, value);
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

}
