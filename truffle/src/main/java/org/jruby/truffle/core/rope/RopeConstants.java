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

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;

public class RopeConstants {

    public static final LeafRope EMPTY_ASCII_8BIT_ROPE;
    public static final LeafRope EMPTY_US_ASCII_ROPE;
    public static final LeafRope EMPTY_UTF8_ROPE;

    public static final LeafRope[] UTF8_SINGLE_BYTE_ROPES = new LeafRope[256];
    public static final LeafRope[] US_ASCII_SINGLE_BYTE_ROPES = new LeafRope[256];
    public static final LeafRope[] ASCII_8BIT_SINGLE_BYTE_ROPES = new LeafRope[256];

    static {
        final byte[] emptyBytes = new byte[] {};

        EMPTY_UTF8_ROPE = new AsciiOnlyLeafRope(emptyBytes, UTF8Encoding.INSTANCE);
        EMPTY_US_ASCII_ROPE = new AsciiOnlyLeafRope(emptyBytes, USASCIIEncoding.INSTANCE);
        EMPTY_ASCII_8BIT_ROPE = new AsciiOnlyLeafRope(emptyBytes, ASCIIEncoding.INSTANCE);

        for (int i = 0; i < 128; i++) {
            final byte[] bytes = new byte[] { (byte) i };

            UTF8_SINGLE_BYTE_ROPES[i] = new AsciiOnlyLeafRope(bytes, UTF8Encoding.INSTANCE);
            US_ASCII_SINGLE_BYTE_ROPES[i] = new AsciiOnlyLeafRope(bytes, USASCIIEncoding.INSTANCE);
            ASCII_8BIT_SINGLE_BYTE_ROPES[i] = new AsciiOnlyLeafRope(bytes, ASCIIEncoding.INSTANCE);
        }

        for (int i = 128; i < 256; i++) {
            final byte[] bytes = new byte[] { (byte) i };

            UTF8_SINGLE_BYTE_ROPES[i] = new InvalidLeafRope(bytes, UTF8Encoding.INSTANCE);
            US_ASCII_SINGLE_BYTE_ROPES[i] = new InvalidLeafRope(bytes, USASCIIEncoding.INSTANCE);
            ASCII_8BIT_SINGLE_BYTE_ROPES[i] = new ValidLeafRope(bytes, ASCIIEncoding.INSTANCE, 1);
        }
    }

}
