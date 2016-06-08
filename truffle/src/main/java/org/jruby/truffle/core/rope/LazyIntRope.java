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
import com.oracle.truffle.api.nodes.ExplodeLoop;
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

    @ExplodeLoop
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

        for (int n = 1, limit = 10; n < 10; n++, limit *= 10) {
            if (value < limit) {
                return n;
            }
        }

        return 10;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
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
