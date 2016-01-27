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

    public LeafRope(byte[] bytes, Encoding encoding, int codeRange, boolean singleByteOptimizable, int characterLength) {
        super(encoding, codeRange, singleByteOptimizable, bytes.length, characterLength, 1, bytes);
    }

    @Override
    public int get(int index) {
        return getRawBytes()[index];
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert offset + length <= byteLength();

        final int trueLength = Math.min(length, byteLength());
        final byte[] ret = new byte[trueLength];

        System.arraycopy(getRawBytes(), offset, ret, 0, trueLength);

        return ret;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return RopeOperations.decodeUTF8(this);
    }

}
