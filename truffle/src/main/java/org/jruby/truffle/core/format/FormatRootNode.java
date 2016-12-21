/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.language.backtrace.InternalRootNode;

/**
 * The node at the root of a pack expression.
 */
public class FormatRootNode extends RootNode implements InternalRootNode {

    private final RubyContext context;
    private final FormatEncoding encoding;

    @Child private FormatNode child;

    @CompilationFinal private int expectedLength = 0;

    public FormatRootNode(RubyContext context, SourceSection sourceSection, FormatEncoding encoding, FormatNode child) {
        super(RubyLanguage.class, sourceSection, FormatFrameDescriptor.FRAME_DESCRIPTOR);
        this.context = context;
        this.encoding = encoding;
        this.child = child;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        frame.setObject(FormatFrameDescriptor.SOURCE_SLOT, frame.getArguments()[0]);
        frame.setInt(FormatFrameDescriptor.SOURCE_LENGTH_SLOT, (int) frame.getArguments()[1]);
        frame.setInt(FormatFrameDescriptor.SOURCE_POSITION_SLOT, 0);
        frame.setObject(FormatFrameDescriptor.OUTPUT_SLOT, new byte[expectedLength]);
        frame.setInt(FormatFrameDescriptor.OUTPUT_POSITION_SLOT, 0);
        frame.setInt(FormatFrameDescriptor.STRING_LENGTH_SLOT, 0);
        frame.setInt(FormatFrameDescriptor.STRING_CODE_RANGE_SLOT, CodeRange.CR_UNKNOWN.toInt());
        frame.setBoolean(FormatFrameDescriptor.TAINT_SLOT, false);

        child.execute(frame);

        final int outputLength;

        try {
            outputLength = frame.getInt(FormatFrameDescriptor.OUTPUT_POSITION_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        if (outputLength > expectedLength) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            /*
             * Don't over-compensate and allocate 2x or something like that for next time, as we have to copy the
             * byte[] at the end if it's too big to make it fit its contents. In the ideal case the byte[] is exactly
             * the right size. If we have to keep making it bigger in the slow-path, we can live with that.
             */

            expectedLength = outputLength;
        }

        final byte[] output;

        try {
            output = (byte[]) frame.getObject(FormatFrameDescriptor.OUTPUT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        final boolean taint;

        try {
            taint = frame.getBoolean(FormatFrameDescriptor.TAINT_SLOT);
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        final int stringLength;

        if (encoding == FormatEncoding.UTF_8) {
            try {
                stringLength = frame.getInt(FormatFrameDescriptor.STRING_LENGTH_SLOT);
            } catch (FrameSlotTypeException e) {
                throw new IllegalStateException(e);
            }
        } else {
            stringLength = outputLength;
        }

        final CodeRange stringCodeRange;

        try {
            stringCodeRange = CodeRange.fromInt(frame.getInt(FormatFrameDescriptor.STRING_CODE_RANGE_SLOT));
        } catch (FrameSlotTypeException e) {
            throw new IllegalStateException(e);
        }

        return new BytesResult(output, outputLength, stringLength, stringCodeRange, taint, encoding);
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    public String getName() {
        return "unpack";
    }

    @Override
    public String toString() {
        return getName();
    }

    public RubyContext getContext() {
        return context;
    }
}
