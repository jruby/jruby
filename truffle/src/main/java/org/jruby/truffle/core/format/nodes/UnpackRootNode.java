/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.format.runtime.PackEncoding;
import org.jruby.truffle.core.format.runtime.PackFrameDescriptor;
import org.jruby.truffle.core.format.runtime.PackResult;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.language.backtrace.InternalRootNode;

public class UnpackRootNode extends RootNode implements InternalRootNode {

    private final String description;
    private final PackEncoding encoding;

    @Child private PackNode child;

    @CompilationFinal private int expectedLength = ArrayUtils.capacity(0, 0);

    public UnpackRootNode(String description, PackEncoding encoding, PackNode child) {
        super(RubyLanguage.class, SourceSection.createUnavailable("unpack", description), PackFrameDescriptor.FRAME_DESCRIPTOR);
        this.description = description;
        this.encoding = encoding;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        frame.setObject(PackFrameDescriptor.SOURCE_SLOT, frame.getArguments()[0]);
        frame.setInt(PackFrameDescriptor.SOURCE_LENGTH_SLOT, (int) frame.getArguments()[1]);
        frame.setInt(PackFrameDescriptor.SOURCE_POSITION_SLOT, 0);
        frame.setObject(PackFrameDescriptor.OUTPUT_SLOT, new Object[expectedLength]);
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

        final Object[] output;

        try {
            output = (Object[]) frame.getObject(PackFrameDescriptor.OUTPUT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        final boolean taint;

        try {
            taint = frame.getBoolean(PackFrameDescriptor.TAINT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        return new PackResult(output, outputLength, -1, CodeRange.CR_BROKEN, taint, encoding);
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
