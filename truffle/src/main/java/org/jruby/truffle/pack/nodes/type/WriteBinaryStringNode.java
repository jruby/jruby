/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.util.ByteList;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteBinaryStringNode extends PackNode {

    private final boolean pad;
    private final int width;
    private final byte padding;

    public WriteBinaryStringNode(boolean pad, int width, byte padding) {
        this.pad = pad;
        this.width = width;
        this.padding = padding;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        if (pad) {
            final int lengthFromBytes = Math.min(width, bytes.length());
            final int lengthFromPadding = width - lengthFromBytes;

            write(frame, bytes.getUnsafeBytes(), bytes.begin(), lengthFromBytes);

            for (int n = 0; n < lengthFromPadding; n++) {
                write(frame, padding);
            }
        } else {
            write(frame, bytes.getUnsafeBytes(), bytes.begin(), bytes.length());
        }

        return null;
    }

}
