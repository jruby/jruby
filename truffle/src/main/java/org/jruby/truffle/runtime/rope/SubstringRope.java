/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.runtime.rope;

import org.jcodings.Encoding;
import org.jruby.util.ByteList;

public class SubstringRope extends Rope {

    private final Rope child;
    private final int offset;
    private final int length;

    private byte[] bytes;
    private ByteList byteList;

    public SubstringRope(Rope child, int offset, int length) {
        this.child = child;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public ByteList getByteList() {
        if (byteList == null) {
            byteList = new ByteList(getBytes(), getEncoding(), false);
        }

        return byteList;
    }

    @Override
    public byte[] getBytes() {
        if (bytes == null) {
            bytes = child.extractRange(offset, length);
        }

        return bytes;
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert length <= this.length;

        return child.extractRange(this.offset + offset, length);
    }

    @Override
    public Encoding getEncoding() {
        return child.getEncoding();
    }
}
