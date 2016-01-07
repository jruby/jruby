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
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.util.ByteList;

import java.util.Arrays;

public class ConcatRope extends Rope {

    private final Rope left;
    private final Rope right;

    private byte[] bytes;
    private ByteList byteList;
    private Encoding encoding;

    public ConcatRope(Rope left, Rope right, Encoding encoding) {
        this.left = left;
        this.right = right;
        this.encoding = encoding;
    }

    public ConcatRope(Rope left, Rope right) {
        this(left, right, null);
    }

    @Override
    public int length() {
        return left.length() + right.length();
    }

    @Override
    public int byteLength() {
        return  left.byteLength() + right.byteLength();
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
            final byte[] leftBytes = left.getBytes();
            final byte[] rightBytes = right.getBytes();

            bytes = new byte[leftBytes.length + rightBytes.length];
            System.arraycopy(leftBytes, 0, bytes, 0, leftBytes.length);
            System.arraycopy(rightBytes, 0, bytes, leftBytes.length, rightBytes.length);
        }

        return bytes;
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        if (offset < left.length()) {
            return left.extractRange(offset, length);
        }

        return right.extractRange(offset - left.length(), length);
    }

    @Override
    public Encoding getEncoding() {
        if (encoding == null) {
            if (left.getEncoding() == right.getEncoding()) {
                encoding = left.getEncoding();
            } else {
                throw new IllegalStateException("ConcatRope only supports working with children ropes of the same Encoding");
            }
        }

        return encoding;
    }

    @Override
    public int hashCode() {
        return left.hashCode() + right.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof ConcatRope) {
            final ConcatRope other = (ConcatRope) o;

            return left.equals(other.left) && right.equals(other.right);
        }

        return false;
    }
}
