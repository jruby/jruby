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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.pack.runtime.PackFrameDescriptor;
import org.jruby.truffle.pack.runtime.exceptions.TooFewArgumentsException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.util.Arrays;

/**
 * The root of the pack nodes.
 * <p>
 * Contains methods to change the state of the parser which is stored in the
 * frame.
 */
@ImportStatic(PackGuards.class)
public abstract class PackNode extends Node {

    private final RubyContext context;

    private final ConditionProfile writeMoreThanZeroBytes = ConditionProfile.createBinaryProfile();

    public PackNode(RubyContext context) {
        this.context = context;
    }

    public abstract Object execute(VirtualFrame frame);

    /**
     * Get the length of the source array.
     */
    public int getSourceLength(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrameDescriptor.SOURCE_LENGTH_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the current position we are reading from in the source array.
     */
    protected int getSourcePosition(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrameDescriptor.SOURCE_POSITION_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the current position we will read from next in the source array.
     */
    protected void setSourcePosition(VirtualFrame frame, int position) {
        frame.setInt(PackFrameDescriptor.SOURCE_POSITION_SLOT, position);
    }

    /**
     * Advanced the position we are reading from in the source array by one
     * element.
     */
    protected int advanceSourcePosition(VirtualFrame frame) {
        final int sourcePosition = getSourcePosition(frame);

        if (sourcePosition == getSourceLength(frame)) {
            CompilerDirectives.transferToInterpreter();
            throw new TooFewArgumentsException();
        }

        setSourcePosition(frame, sourcePosition + 1);

        return sourcePosition;
    }

    /**
     * Get the output array we are writing to.
     */
    protected byte[] getOutput(VirtualFrame frame) {
        try {
            return (byte[]) frame.getObject(PackFrameDescriptor.OUTPUT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the output array we are writing to. This should never be used in the
     * compiled code - having to change the output array to resize is is a
     * deoptimizing action.
     */
    protected void setOutput(VirtualFrame frame, byte[] output) {
        CompilerAsserts.neverPartOfCompilation();
        frame.setObject(PackFrameDescriptor.OUTPUT_SLOT, output);
    }

    /**
     * Get the current position we are writing to the in the output array.
     */
    protected int getOutputPosition(VirtualFrame frame) {
        try {
            return frame.getInt(PackFrameDescriptor.OUTPUT_POSITION_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Set the current position we are writing to in the output array.
     */
    protected void setOutputPosition(VirtualFrame frame, int position) {
        frame.setInt(PackFrameDescriptor.OUTPUT_POSITION_SLOT, position);
    }

    /**
     * Set the output to be tainted.
     */
    protected void setTainted(VirtualFrame frame) {
        frame.setBoolean(PackFrameDescriptor.TAINT_SLOT, true);
    }

    /**
     * Write an array of bytes to the output.
     */
    protected void writeBytes(VirtualFrame frame, byte... values) {
        writeBytes(frame, values, 0, values.length);
    }

    /**
     * Write a {@link ByteList} to the output.
     */
    protected void writeBytes(VirtualFrame frame, ByteList values) {
        writeBytes(frame, values.getUnsafeBytes(), values.begin(), values.length());
    }

    /**
     * Write a range of an array of bytes to the output.
     */
    protected void writeBytes(VirtualFrame frame, byte[] values, int valuesStart, int valuesLength) {
        byte[] output = ensureCapacity(frame, valuesLength);
        final int outputPosition = getOutputPosition(frame);
        System.arraycopy(values, valuesStart, output, outputPosition, valuesLength);
        setOutputPosition(frame, outputPosition + valuesLength);
    }

    /**
     * Write null bytes to the output.
     */
    protected void writeNullBytes(VirtualFrame frame, int length) {
        if (writeMoreThanZeroBytes.profile(length > 0)) {
            ensureCapacity(frame, length);
            final int outputPosition = getOutputPosition(frame);
            setOutputPosition(frame, outputPosition + length);
        }
    }

    private byte[] ensureCapacity(VirtualFrame frame, int length) {
        byte[] output = getOutput(frame);
        final int outputPosition = getOutputPosition(frame);

        if (outputPosition + length > output.length) {
            // If we ran out of output byte[], deoptimize and next time we'll allocate more

            CompilerDirectives.transferToInterpreterAndInvalidate();
            output = Arrays.copyOf(output, (output.length + length) * 2);
            setOutput(frame, output);
        }

        return output;
    }

    protected RubyContext getContext() {
        return context;
    }

    protected boolean isNil(Object object) {
        return object == context.getCoreLibrary().getNilObject();
    }

}
