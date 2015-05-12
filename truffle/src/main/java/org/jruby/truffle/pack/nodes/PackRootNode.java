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
import org.jruby.truffle.pack.runtime.PackFrameDescriptor;
import org.jruby.truffle.pack.runtime.PackResult;
import org.jruby.truffle.runtime.array.ArrayUtils;

/**
 * The node at the root of a pack expression.
 */
public class PackRootNode extends RootNode {

    private final String description;
    private final PackEncoding encoding;

    @Child private PackNode child;

    @CompilerDirectives.CompilationFinal private int expectedLength = ArrayUtils.capacity(0, 0);

    public PackRootNode(String description, PackEncoding encoding, PackNode child) {
        super(new NullSourceSection("pack", description), PackFrameDescriptor.FRAME_DESCRIPTOR);
        this.description = description;
        this.encoding = encoding;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        frame.setObject(PackFrameDescriptor.SOURCE_SLOT, frame.getArguments()[0]);
        frame.setInt(PackFrameDescriptor.SOURCE_LENGTH_SLOT, (int) frame.getArguments()[1]);
        frame.setInt(PackFrameDescriptor.SOURCE_POSITION_SLOT, 0);
        frame.setObject(PackFrameDescriptor.OUTPUT_SLOT, new byte[expectedLength]);
        frame.setInt(PackFrameDescriptor.OUTPUT_POSITION_SLOT, 0);
        frame.setBoolean(PackFrameDescriptor.TAINT_SLOT, false);

        child.execute(frame);

        final int outputLength;

        try {
            outputLength = frame.getInt(PackFrameDescriptor.OUTPUT_POSITION_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        if (outputLength > expectedLength) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            expectedLength = ArrayUtils.capacity(expectedLength, outputLength);
        }

        final byte[] output;

        try {
            output = (byte[]) frame.getObject(PackFrameDescriptor.OUTPUT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        final boolean taint;

        try {
            taint = frame.getBoolean(PackFrameDescriptor.TAINT_SLOT);
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
