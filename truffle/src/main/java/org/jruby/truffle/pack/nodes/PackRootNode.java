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
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.NullSourceSection;
import org.jruby.truffle.pack.runtime.PackEncoding;
import org.jruby.truffle.pack.runtime.PackFrame;
import org.jruby.truffle.pack.runtime.PackResult;
import org.jruby.truffle.runtime.util.ArrayUtils;

/**
 * The node at the root of a pack expression.
 */
public class PackRootNode extends RootNode {

    private final String description;
    private final PackEncoding encoding;

    @Child private PackNode child;

    @CompilerDirectives.CompilationFinal private int expectedLength = ArrayUtils.capacity(0, 0);

    public PackRootNode(String description, PackEncoding encoding, PackNode child) {
        super(new NullSourceSection("pack", description), PackFrame.INSTANCE.getFrameDescriptor());
        this.description = description;
        this.encoding = encoding;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        frame.setObject(PackFrame.INSTANCE.getSourceSlot(), frame.getArguments()[0]);
        frame.setInt(PackFrame.INSTANCE.getSourceLengthSlot(), (int) frame.getArguments()[1]);
        frame.setInt(PackFrame.INSTANCE.getSourcePositionSlot(), 0);
        frame.setObject(PackFrame.INSTANCE.getOutputSlot(), new byte[expectedLength]);
        frame.setInt(PackFrame.INSTANCE.getOutputPositionSlot(), 0);
        frame.setBoolean(PackFrame.INSTANCE.getTaintSlot(), false);

        child.execute(frame);

        final int outputLength;

        try {
            outputLength = frame.getInt(PackFrame.INSTANCE.getOutputPositionSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        if (outputLength > expectedLength) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            expectedLength = ArrayUtils.capacity(expectedLength, outputLength);
        }

        final byte[] output;

        try {
            output = (byte[]) frame.getObject(PackFrame.INSTANCE.getOutputSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        final boolean taint;

        try {
            taint = frame.getBoolean(PackFrame.INSTANCE.getTaintSlot());
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        return new PackResult(output, outputLength, taint, encoding);
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    public String toString() {
        return description;
    }

}
