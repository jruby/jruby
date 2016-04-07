/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rope;

import org.jcodings.Encoding;

public class RepeatingRope extends Rope {

    private final Rope child;
    private final int times;

    public RepeatingRope(Rope child, int times) {
        super(child.getEncoding(), child.getCodeRange(), child.isSingleByteOptimizable(), child.byteLength() * times, child.characterLength() * times, child.depth() + 1, null);
        this.child = child;
        this.times = times;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        return new RepeatingRope(child.withEncoding(newEncoding, newCodeRange), times);
    }

    @Override
    protected byte getByteSlow(int index) {
        return child.getByteSlow(index % child.byteLength());
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert length <= this.byteLength();

        final byte[] ret = new byte[length];

        if (getRawBytes() != null) {
            System.arraycopy(getRawBytes(), offset, ret, 0, length);
        } else {
            final int start = offset % child.byteLength();
            final byte[] firstPart = child.extractRange(start, child.byteLength() - start);
            final int lengthMinusFirstPart = length - firstPart.length;
            final int remainingEnd = lengthMinusFirstPart % child.byteLength();

            System.arraycopy(firstPart, 0, ret, 0, firstPart.length);

            if (lengthMinusFirstPart >= child.byteLength()) {
                final byte[] secondPart = child.getBytes();

                final int repeatPartCount = lengthMinusFirstPart / child.byteLength();
                for (int i = 0; i < repeatPartCount; i++) {
                    System.arraycopy(secondPart, 0, ret, firstPart.length + (secondPart.length * i), secondPart.length);
                }

                if (remainingEnd > 0) {
                    final byte[] thirdPart = child.extractRange(0, remainingEnd);
                    System.arraycopy(thirdPart, 0, ret, length - thirdPart.length, thirdPart.length);
                }
            } else {
                final byte[] secondPart = child.extractRange(0, remainingEnd);
                System.arraycopy(secondPart, 0, ret, length - secondPart.length, secondPart.length);
            }
        }

        return ret;
    }

    public Rope getChild() {
        return child;
    }

    public int getTimes() {
        return times;
    }

    @Override
    public String toString() {
        final String childString = child.toString();
        final StringBuilder builder = new StringBuilder(childString.length() * times);

        for (int i = 0; i < times; i++) {
            builder.append(childString);
        }

        return builder.toString();
    }
}
