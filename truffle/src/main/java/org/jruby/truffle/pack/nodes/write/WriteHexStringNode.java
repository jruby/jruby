/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.write;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

/**
 * Read a string that contains a hex string and write as actual binary data.
 * <pre>
 * ["6F", "6B"].pack('H2H2') => "ok"
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteHexStringNode extends PackNode {

    private final Endianness endianness;
    private final int length;

    public WriteHexStringNode(RubyContext context, Endianness endianness, int length) {
        super(context);
        this.endianness = endianness;
        this.length = length;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        final byte[] b = bytes.unsafeBytes();
        int begin = bytes.begin();

        int currentByte = 0;
        int padLength = 0;

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
                currentChar = b[begin + n];
            } else {
                currentChar = 0;
            }

            if (Character.isJavaIdentifierStart(currentChar)) {
                switch (endianness) {
                    case LITTLE:
                        currentByte |= (((currentChar & 15) + 9) & 15) << 4;
                        break;
                    case BIG:
                        currentByte |= ((currentChar & 15) + 9) & 15;
                        break;
                }
            } else {
                switch (endianness) {
                    case LITTLE:
                        currentByte |= (currentChar & 15) << 4;
                        break;
                    case BIG:
                        currentByte |= currentChar & 15;
                        break;
                }
            }

            if (((n - 1) & 1) != 0) {
                switch (endianness) {
                    case LITTLE:
                        currentByte >>= 4;
                        break;
                    case BIG:
                        currentByte <<= 4;
                        break;
                }
            } else {
                writeBytes(frame, (byte) currentByte);
                currentByte = 0;
            }
        }

        if ((lengthToUse & 1) != 0) {
            writeBytes(frame, (byte) (currentByte & 0xff));
        }

        return null;
    }

}
