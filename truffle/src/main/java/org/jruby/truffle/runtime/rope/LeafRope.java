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

import java.util.Arrays;

public class LeafRope extends Rope {

    private final byte[] bytes;
    private ByteList byteList;
    private final Encoding encoding;
    private int hashCode = -1;

    public LeafRope(byte[] bytes, Encoding encoding) {
        this.bytes = bytes;
        this.encoding = encoding;
    }

    @Override
    public int length() {
        return bytes.length;
    }

    @Override
    public ByteList getByteList() {
        if (byteList == null) {
            byteList = new ByteList(bytes, encoding, false);
        }

        return byteList;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = Arrays.hashCode(bytes) + encoding.hashCode();
        }

        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof LeafRope) {
            final LeafRope other = (LeafRope) o;

            return encoding == other.getEncoding() && Arrays.equals(bytes, other.getBytes());
        }

        return false;
    }
}
