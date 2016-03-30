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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

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
        CompilerDirectives.transferToInterpreter();

        // Advance will handle the error
        advanceSourcePosition(frame, count);

        throw new IllegalStateException();
    }

    @Specialization
    public Object read(VirtualFrame frame, byte[] source) {
        final int start = getSourcePosition(frame);

        int length;
        ByteList result;

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

        result = new ByteList(source, start, usedLength, true);

        if (trimToFirstNull) {
            final int firstNull = result.indexOf(0);

            if (firstNull != -1 && trimTrailingNulls) {
                result.realSize(firstNull);
            }
        }

        setSourcePosition(frame, start + length);

        return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(),
                StringOperations.ropeFromByteList(result, StringSupport.CR_UNKNOWN));
    }

}
