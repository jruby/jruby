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
import org.jruby.util.ByteList;

import java.nio.ByteOrder;

/**
 * Read a string that contains a binary string (literally a string of binary
 * digits written using 1 and 0 characters) and write as actual binary data.
 * <pre>
 * ["01101111", "01101011"].pack('B8B8') => "ok"
 */
@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class WriteBitStringNode extends FormatNode {

    private final ByteOrder byteOrder;
    private final boolean star;
    private final int length;

    public WriteBitStringNode(RubyContext context, ByteOrder byteOrder, boolean star, int length) {
        super(context);
        this.byteOrder = byteOrder;
        this.star = star;
        this.length = length;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        // Bit string logic copied from jruby.util.Pack - see copyright and authorship there

        final ByteList lCurElemString = bytes;

        int occurrences;

        if (star) {
            occurrences = lCurElemString.length();
        } else {
            occurrences = length;
        }

        int currentByte = 0;
        int padLength = 0;

        if (occurrences > lCurElemString.length()) {
            padLength = (occurrences - lCurElemString.length()) / 2 + (occurrences + lCurElemString.length()) % 2;
            occurrences = lCurElemString.length();
        }

        if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
            for (int i = 0; i < occurrences;) {
                if ((lCurElemString.charAt(i++) & 1) != 0) {//if the low bit is set
                    currentByte |= 128; //set the high bit of the result
                }

                if ((i & 7) == 0) {
                    writeByte(frame, (byte) (currentByte & 0xff));
                    currentByte = 0;
                    continue;
                }

                //if the index is not a multiple of 8, we are not on a byte boundary
                currentByte >>= 1; //shift the byte
            }

            if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
                currentByte >>= 7 - (occurrences & 7); //we need to pad the last byte
                writeByte(frame, (byte) (currentByte & 0xff));
            }
        } else {
            for (int i = 0; i < occurrences;) {
                currentByte |= lCurElemString.charAt(i++) & 1;

                // we filled up current byte; append it and create next one
                if ((i & 7) == 0) {
                    writeByte(frame, (byte) (currentByte & 0xff));
                    currentByte = 0;
                    continue;
                }

                //if the index is not a multiple of 8, we are not on a byte boundary
                currentByte <<= 1;
            }

            if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
                currentByte <<= 7 - (occurrences & 7); //we need to pad the last byte
                writeByte(frame, (byte) (currentByte & 0xff));
            }
        }

        writeNullBytes(frame, padLength);

        return null;
    }

}
