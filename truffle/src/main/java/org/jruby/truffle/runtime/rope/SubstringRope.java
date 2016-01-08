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
import org.jruby.util.ByteList;

public class SubstringRope extends Rope {

    private final Rope child;
    private final int offset;
    private final int length;

    private byte[] bytes;

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
    public int byteLength() {
        // TODO (nirvdrum Jan.-07-2016) This is horribly inefficient.
        return getBytes().length;
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

    @Override
    public int getCodeRange() {
        // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        return child.getCodeRange();
    }

    @Override
    public boolean isSingleByteOptimizable() {
        // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        return child.isSingleByteOptimizable();
    }
}
