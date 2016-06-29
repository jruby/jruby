/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read.bytes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.rope.AsciiOnlyLeafRope;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadBinaryStringNode extends FormatNode {

    final boolean readToEnd;
    final boolean readToNull;
    final int count;
    final boolean trimTrailingSpaces;
    final boolean trimTrailingNulls;
    final boolean trimToFirstNull;

    public ReadBinaryStringNode(RubyContext context, boolean readToEnd, boolean readToNull, int count,
                                boolean trimTrailingSpaces, boolean trimTrailingNulls, boolean trimToFirstNull) {
        super(context);
        this.readToEnd = readToEnd;
        this.readToNull = readToNull;
        this.count = count;
        this.trimTrailingSpaces = trimTrailingSpaces;
        this.trimTrailingNulls = trimTrailingNulls;
        this.trimToFirstNull = trimToFirstNull;
    }

    @Specialization(guards = "isNull(source)")
    public void read(VirtualFrame frame, Object source) {
        // Advance will handle the error
        advanceSourcePosition(frame, count);

        throw new IllegalStateException();
    }

    @Specialization
    public DynamicObject read(VirtualFrame frame, byte[] source) {
        final int start = getSourcePosition(frame);

        int length;

        if (readToEnd) {
            length = 0;

            while (start + length < getSourceLength(frame)
                    && (!readToNull || (start + length < getSourceLength(frame) && source[start + length] != 0))) {
                length++;
            }

            if (start + length < getSourceLength(frame) && source[start + length] == 0) {
                length++;
            }
        } else if (readToNull) {
            length = 0;

            while (start + length < getSourceLength(frame)
                    && length < count
                    && (!readToNull || (start + length < getSourceLength(frame) && source[start + length] != 0))) {
                length++;
            }

            if (start + length < getSourceLength(frame) && source[start + length] == 0) {
                length++;
            }
        } else {
            length = count;

            if (start + length >= getSourceLength(frame)) {
                length = getSourceLength(frame) - start;
            }
        }

        int usedLength = length;

        while (usedLength > 0 && ((trimTrailingSpaces && source[start + usedLength - 1] == ' ')
                                    || (trimTrailingNulls && source[start + usedLength - 1] == 0))) {
            usedLength--;
        }

        if (trimToFirstNull) {
            final int firstNull = indexOfFirstNull(source, start, usedLength);

            if (firstNull != -1 && trimTrailingNulls) {
                usedLength = firstNull;
            }
        }

        setSourcePosition(frame, start + length);

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(),
                new AsciiOnlyLeafRope(Arrays.copyOfRange(source, start, start + usedLength), ASCIIEncoding.INSTANCE));
    }

    private int indexOfFirstNull(byte[] bytes, int start, int length) {
        for (int n = 0; n < length; n++) {
            if (bytes[start + n] == 0) {
                return n;
            }
        }

        return -1;
    }

}
