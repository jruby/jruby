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
import org.jruby.truffle.pack.runtime.Endianness;
import org.jruby.util.ByteList;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteBitStringNode extends PackNode {

    private final Endianness endianness;
    private final int length;

    public WriteBitStringNode(Endianness endianness, int length) {
        this.endianness = endianness;
        this.length = length;
    }

    @Specialization
    public Object write(VirtualFrame frame, ByteList bytes) {
        final byte[] b = bytes.unsafeBytes();
        int begin = bytes.begin();

        int currentByte = 0;

        int padLength = 0;

        for (int n = 0; n < length; n++) {
            byte currentChar = b[begin + n];

            switch (endianness) {
                case LITTLE: {
                    if ((currentChar & 1) != 0) {//if the low bit is set
                        currentByte |= 128; //set the high bit of the result
                    }

                    if (((n - 1) & 7) == 0) {
                        writeBytes(frame, (byte) (currentByte & 0xff));
                        currentByte = 0;
                        continue;
                    }

                    //if the index is not a multiple of 8, we are not on a byte boundary
                    currentByte >>= 1; //shift the byte
                } break;
                case BIG: {
                    currentByte |= currentChar & 1;

                    // we filled up current byte; append it and create next one
                    if (((n - 1) & 7) == 0) {
                        writeBytes(frame, (byte) (currentByte & 0xff));
                        currentByte = 0;
                        continue;
                    }

                    //if the index is not a multiple of 8, we are not on a byte boundary
                    currentByte <<= 1;
                } break;
            }
        }

        switch (endianness) {
            case LITTLE: {
                if ((length & 7) != 0) { //if the length is not a multiple of 8
                    currentByte >>= 7 - (length & 7); //we need to pad the last byte
                    writeBytes(frame, (byte) (currentByte & 0xff));
                }
            } break;
            case BIG: {
                if ((length & 7) != 0) { //if the length is not a multiple of 8
                    currentByte <<= 7 - (length & 7); //we need to pad the last byte
                    writeBytes(frame, (byte) (currentByte & 0xff));
                }
            } break;
        }

        return null;
    }

}
