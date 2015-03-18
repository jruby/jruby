/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.runtime.PackFrame;

import java.util.Arrays;

@TypeSystemReference(PackTypes.class)
public abstract class PackNode extends Node {

    public abstract Object execute(VirtualFrame frame);

    public int getSourceLength(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrame.INSTANCE.getSourceLengthSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int getSourcePosition(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrame.INSTANCE.getSourcePositionSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setSourcePosition(VirtualFrame frame, int position) {
        frame.setInt(PackFrame.INSTANCE.getSourcePositionSlot(), position);
    }

    protected byte[] getOutput(VirtualFrame frame) {
        try {
            return (byte[]) frame.getObject(PackFrame.INSTANCE.getOutputSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setOutput(VirtualFrame frame, byte[] output) {
        frame.setObject(PackFrame.INSTANCE.getOutputSlot(), output);
    }

    protected int getOutputPosition(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrame.INSTANCE.getOutputPositionSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void setOutputPosition(VirtualFrame frame, int position) {
        frame.setInt(PackFrame.INSTANCE.getOutputPositionSlot(), position);
    }

    protected int readInt(VirtualFrame frame, int[] source) {
        final int sourcePosition = getSourcePosition(frame);

        if (sourcePosition == getSourceLength(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        setSourcePosition(frame, sourcePosition + 1);

        return source[sourcePosition];
    }

    protected int readInt(VirtualFrame frame, IRubyObject[] source) {
        final int sourcePosition = getSourcePosition(frame);

        if (sourcePosition == getSourceLength(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        setSourcePosition(frame, sourcePosition + 1);

        return toInt(source[sourcePosition]);
    }

    @CompilerDirectives.TruffleBoundary
    private int toInt(IRubyObject object) {
        return object.convertToInteger().getIntValue();
    }

    protected void write32UnsignedLittle(VirtualFrame frame, int value) {
        write(frame, (byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24));
    }

    protected void write32UnsignedBig(VirtualFrame frame, int value) {
        write(frame, (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value);
    }

    @ExplodeLoop
    protected void write(VirtualFrame frame, byte... values) {
        byte[] output = getOutput(frame);
        final int outputPosition = getOutputPosition(frame);

        if (outputPosition + values.length > output.length) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            output = Arrays.copyOf(output, output.length * 2);
            setOutput(frame, output);
        }

        System.arraycopy(values, 0, output, outputPosition, values.length);
        setOutputPosition(frame, outputPosition + values.length);
    }

}
