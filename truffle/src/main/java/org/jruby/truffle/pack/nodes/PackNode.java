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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.pack.runtime.PackFrame;
import org.jruby.truffle.pack.runtime.TooFewArgumentsException;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.util.ByteList;

import java.math.BigInteger;
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

    protected int advanceSourcePosition(VirtualFrame frame) {
        final int sourcePosition = getSourcePosition(frame);

        if (sourcePosition == getSourceLength(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new TooFewArgumentsException();
        }

        setSourcePosition(frame, sourcePosition + 1);

        return sourcePosition;
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

    protected void setTainted(VirtualFrame frame) {
        frame.setBoolean(PackFrame.INSTANCE.getTaintSlot(), true);
    }

    protected void writeBytes(VirtualFrame frame, byte... values) {
        writeBytes(frame, values, 0, values.length);
    }

    protected void writeBytes(VirtualFrame frame, ByteList values) {
        writeBytes(frame, values.getUnsafeBytes(), values.begin(), values.length());
    }

    protected void writeBytes(VirtualFrame frame, byte[] values, int valuesStart, int valuesLength) {
        byte[] output = getOutput(frame);
        final int outputPosition = getOutputPosition(frame);

        if (outputPosition + valuesLength > output.length) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            output = Arrays.copyOf(output, (output.length + valuesLength) * 2);
            setOutput(frame, output);
        }

        System.arraycopy(values, valuesStart, output, outputPosition, valuesLength);
        setOutputPosition(frame, outputPosition + valuesLength);
    }

    protected boolean isNull(Object object) {
        return object == null;
    }

    protected boolean isRubyString(Object object) {
        return object instanceof RubyString;
    }

    protected boolean isRubyNilClass(Object object) {
        return object instanceof RubyString;
    }

    protected boolean isBoolean(Object object) {
        return object instanceof Boolean;
    }

    protected boolean isInteger(Object object) {
        return object instanceof Integer;
    }

    protected boolean isLong(Object object) {
        return object instanceof Long;
    }

    protected boolean isBigInteger(Object object) {
        return object instanceof BigInteger;
    }

    protected boolean isRubyBignum(Object object) {
        return object instanceof RubyBignum;
    }

    protected boolean isIRubyArray(Object[] array) {
        return array instanceof IRubyObject[];
    }

}
