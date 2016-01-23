/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.rope;

import org.jcodings.Encoding;

import java.util.Arrays;

public abstract class LeafRope extends Rope {

    private final byte[] bytes;

    public LeafRope(byte[] bytes, Encoding encoding, int codeRange, boolean singleByteOptimizable, int characterLength) {
        super(encoding, codeRange, singleByteOptimizable, bytes.length, characterLength, 1);
        this.bytes = bytes;
    }

    @Override
    public int get(int index) {
        return bytes[index];
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert offset + length <= bytes.length;

        final int trueLength = Math.min(length, byteLength());
        final byte[] ret = new byte[trueLength];

        System.arraycopy(bytes, offset, ret, 0, trueLength);

        return ret;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return RopeOperations.decodeUTF8(this);
    }

    @Override
    protected void fillBytes(byte[] buffer, int bufferPosition, int offset, int byteLength) {
        assert offset + byteLength <= bytes.length;

        System.arraycopy(bytes, offset, buffer, bufferPosition, byteLength);
    }
}
