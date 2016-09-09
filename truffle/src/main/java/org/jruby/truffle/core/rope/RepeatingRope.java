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
    protected byte[] getBytesSlow() {
        if (child.getRawBytes() != null) {
            final byte[] childBytes = child.getRawBytes();
            int len = childBytes.length * times;
            final byte[] ret = new byte[len];

            int n = childBytes.length;

            System.arraycopy(childBytes, 0, ret, 0, n);
            while (n <= len / 2) {
                System.arraycopy(ret, 0, ret, n, n);
                n *= 2;
            }
            System.arraycopy(ret, 0, ret, n, len - n);

            return ret;
        }

        return super.getBytesSlow();
    }

    @Override
    protected byte getByteSlow(int index) {
        return child.getByteSlow(index % child.byteLength());
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
