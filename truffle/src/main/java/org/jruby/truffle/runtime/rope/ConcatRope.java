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
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.CodeRangeable;

import java.util.Arrays;

public class ConcatRope extends Rope {

    private final Rope left;
    private final Rope right;

    private byte[] bytes;
    private final Encoding encoding;

    public ConcatRope(Rope left, Rope right, Encoding encoding) {
        this.left = left;
        this.right = right;
        this.encoding = encoding;
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
    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public int getCodeRange() {
        // TODO (nirvdrum 07-Jan-16) This method does a bit of branching. We may want to pass a ConditionProfile in to simplify that or do something clever with bit masks.
        return StringOperations.commonCodeRange(left.getCodeRange(), right.getCodeRange());
    }

    @Override
    public boolean isSingleByteOptimizable() {
        return left.isSingleByteOptimizable() && right.isSingleByteOptimizable();
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
