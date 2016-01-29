/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.nodes.write;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.core.format.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

import java.nio.ByteOrder;

/**
 * Read a string that contains a hex string and write as actual binary data.
 * <pre>
 * ["6F", "6B"].pack('H2H2') => "ok"
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteHexStringNode extends PackNode {

    private final ByteOrder byteOrder;
    private final int length;

    public WriteHexStringNode(RubyContext context, ByteOrder byteOrder, int length) {
        super(context);
        this.byteOrder = byteOrder;
        this.length = length;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        int currentByte = 0;

        final int lengthToUse;

        if (length == -1) {
            lengthToUse = bytes.length();
        } else {
            lengthToUse = length;
        }

        // Hex string logic copied from jruby.util.Pack - see copyright and authorship there

        for (int n = 0; n < lengthToUse; n++) {
            byte currentChar;

            if (n < bytes.length()) {
                currentChar = (byte) bytes.get(n);
            } else {
                currentChar = 0;
            }

            if (Character.isJavaIdentifierStart(currentChar)) {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    currentByte |= (((currentChar & 15) + 9) & 15) << 4;
                } else {
                    currentByte |= ((currentChar & 15) + 9) & 15;
                }
            } else {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    currentByte |= (currentChar & 15) << 4;
                } else {
                    currentByte |= currentChar & 15;
                }
            }

            if (((n - 1) & 1) != 0) {
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    currentByte >>= 4;
                } else {
                    currentByte <<= 4;
                }
            } else {
                writeByte(frame, (byte) currentByte);
                currentByte = 0;
            }
        }

        if ((lengthToUse & 1) != 0) {
            writeByte(frame, (byte) (currentByte & 0xff));
        }

        return null;
    }

}
