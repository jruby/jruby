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
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.util.ByteList;

import java.util.Arrays;

public class ByteWriter {

    @CompilerDirectives.CompilationFinal private byte[] bytes;
    private int position;

    public ByteWriter(int capacity) {
        bytes = new byte[capacity];
    }

    @ExplodeLoop
    public void write(byte... values) {
        if (position + values.length >= bytes.length) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }

        for (byte value : values) {
            // TODO CS 11-Mar-15 ensure array bounds checks here are removed
            bytes[position] = value;
            position++;
        }
    }

    public int getLength() {
        return position;
    }

    public ByteList toByteList() {
        return new ByteList(bytes, 0, position, USASCIIEncoding.INSTANCE, false);
    }

}
