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

import com.oracle.truffle.api.CompilerDirectives;
import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;

import java.nio.charset.StandardCharsets;

public class LazyIntRope extends LazyRope {

    final int value;

    public LazyIntRope(int value) {
        this(value, USASCIIEncoding.INSTANCE, length(value));
    }

    protected LazyIntRope(int value, Encoding encoding, int length) {
        super(encoding, length, length);
        this.value = value;
        assert Integer.toString(value).length() == length;
    }

    private static int length(int value) {
        if (value < 0) {
            /*
             * We can't represent -Integer.MIN_VALUE, and we're about to multiple by 10 to add the space needed for the
             * negative character, so handle both of those out-of-range cases.
             */

            if (value <= -1000000000) {
                return 11;
            }

            value = -value;
            value *= 10;
        }

        // If values were evenly distributed it would make more sense to do this in the reverse order, but they aren't

        if (value < 10) {
            return 1;
        }

        if (value < 100) {
            return 2;
        }

        if (value < 1000) {
            return 3;
        }

        if (value < 10000) {
            return 4;
        }

        if (value < 100000) {
            return 5;
        }

        if (value < 1000000) {
            return 6;
        }

        if (value < 10000000) {
            return 7;
        }

        if (value < 100000000) {
            return 8;
        }

        if (value < 1000000000) {
            return 9;
        }

        return 10;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        return new LazyIntRope(value, newEncoding, length(value));
    }

    @Override
    public byte[] fulfill() {
        if (bytes == null) {
            bytes = Integer.toString(value).getBytes(StandardCharsets.US_ASCII);
        }

        return bytes;
    }

    public int getValue() {
        return value;
    }
}
