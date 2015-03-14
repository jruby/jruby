/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.util.ByteList;
import org.jruby.util.unsafe.UnsafeHolder;
import sun.misc.Unsafe;

import java.util.Arrays;

public class ByteWriter {

    @CompilerDirectives.CompilationFinal private byte[] bytes;
    @CompilerDirectives.CompilationFinal private long bytesAddress;
    private int position;

    public ByteWriter(int expectedLength) {
        bytes = new byte[expectedLength];
    }

    public void writeUInt32LE(int value) {
        write((byte) value, (byte) (value >>> 8), (byte) (value >>> 16), (byte) (value >>> 24));
    }

    public void writeUInt32BE(int value) {
        write((byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value);
    }

    @ExplodeLoop
    public void write(byte... values) {
        if (position + values.length > bytes.length) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }

        for (byte value : values) {
            //bytes[position] = value;
            UnsafeHolder.U.putByte(
                    bytes,
                    Unsafe.ARRAY_BYTE_BASE_OFFSET + Unsafe.ARRAY_BYTE_INDEX_SCALE * (long) position,
                    value);
            position++;
        }
    }

    public void back() {
        position--;
    }

    public int getLength() {
        return position;
    }

    public ByteList toByteList() {
        return new ByteList(bytes, 0, position, USASCIIEncoding.INSTANCE, false);
    }

}
