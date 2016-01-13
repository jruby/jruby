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

public class ConcatRope extends Rope {

    private final Rope left;
    private final Rope right;

    private byte[] bytes;

    public ConcatRope(Rope left, Rope right, Encoding encoding) {
        super(encoding,
                StringOperations.commonCodeRange(left.getCodeRange(), right.getCodeRange()),
                left.isSingleByteOptimizable() && right.isSingleByteOptimizable(),
                left.byteLength() + right.byteLength(),
                left.characterLength() + right.characterLength(),
                Math.max(left.depth(), right.depth()) + 1);

        this.left = left;
        this.right = right;
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
        byte[] leftBytes;
        byte[] rightBytes;
        final int leftLength = left.byteLength();

        if (offset < leftLength) {
            // The left branch might not be large enough to extract the full byte range we want. In that case,
            // we'll extract what we can and extract the difference from the right side.
            if (offset + length > leftLength) {
                leftBytes = left.extractRange(offset, leftLength - offset);
            } else {
                leftBytes = left.extractRange(offset, length);
            }

            if (leftBytes.length < length) {
                rightBytes = right.extractRange(0, length - leftBytes.length);

                final byte[] ret = new byte[length];
                System.arraycopy(leftBytes, 0, ret, 0, leftBytes.length);
                System.arraycopy(rightBytes, 0, ret, leftBytes.length, rightBytes.length);

                return ret;
            }  else {
                return leftBytes;
            }
        }

        return right.extractRange(offset - leftLength, length);
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
