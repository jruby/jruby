/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.write.bytes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteBinaryStringNode extends FormatNode {

    private final boolean pad;
    private final boolean padOnNil;
    private final int width;
    private final byte padding;
    private final boolean takeAll;
    private final boolean appendNull;

    public WriteBinaryStringNode(RubyContext context, boolean pad, boolean padOnNil,
                                 int width, byte padding, boolean takeAll, boolean appendNull) {
        super(context);
        this.pad = pad;
        this.padOnNil = padOnNil;
        this.width = width;
        this.padding = padding;
        this.takeAll = takeAll;
        this.appendNull = appendNull;
    }

    @Specialization(guards = "isNil(nil)")
    public Object write(VirtualFrame frame, Object nil) {
        if (padOnNil) {
            for (int n = 0; n < width; n++) {
                writeByte(frame, padding);
            }
        } else if (appendNull) {
            writeByte(frame, (byte) 0);
        }

        return null;
    }

    @Specialization
    public Object write(VirtualFrame frame, byte[] bytes) {
        final int lengthFromBytes;

        if (takeAll) {
            lengthFromBytes = bytes.length;
        } else {
            lengthFromBytes = Math.min(width, bytes.length);
        }

        if (pad) {
            final int lengthFromPadding = width - lengthFromBytes;

            writeBytes(frame, bytes, lengthFromBytes);

            for (int n = 0; n < lengthFromPadding; n++) {
                writeByte(frame, padding);
            }
        } else {
            writeBytes(frame, bytes, lengthFromBytes);
        }

        if (appendNull) {
            writeByte(frame, (byte) 0);
        }

        return null;
    }

}
