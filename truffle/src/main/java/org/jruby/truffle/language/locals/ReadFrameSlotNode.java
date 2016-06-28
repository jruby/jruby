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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.Node;

public abstract class ReadFrameSlotNode extends Node {

    protected final FrameSlot frameSlot;

    public ReadFrameSlotNode(FrameSlot slot) {
        this.frameSlot = slot;
    }

    public abstract Object executeRead(Frame frame);

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    public boolean readBoolean(Frame frame) throws FrameSlotTypeException {
        return frame.getBoolean(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    public int readInt(Frame frame) throws FrameSlotTypeException {
        return frame.getInt(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    public long readLong(Frame frame) throws FrameSlotTypeException {
        return frame.getLong(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    public double readDouble(Frame frame) throws FrameSlotTypeException {
        return frame.getDouble(frameSlot);
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class)
    public Object readObject(Frame frame) throws FrameSlotTypeException {
        return frame.getObject(frameSlot);
    }

    @Specialization(contains = { "readBoolean", "readInt", "readLong", "readDouble", "readObject" })
    public Object readAny(Frame frame) {
        return frame.getValue(frameSlot);
    }

    public final FrameSlot getFrameSlot() {
        return frameSlot;
    }

}
