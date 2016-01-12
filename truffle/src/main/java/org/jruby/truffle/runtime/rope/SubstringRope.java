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

public class SubstringRope extends Rope {

    private final Rope child;
    private final int offset;
    private final int byteLength;
    private final int characterLength;
    private final int codeRange;
    private final boolean isSingleByteOptimizable;
    private final int depth;

    private byte[] bytes;

    public SubstringRope(Rope child, int offset, int byteLength, int characterLength, int codeRange) {
        this.child = child;
        this.offset = offset;
        this.byteLength = byteLength;
        this.characterLength = characterLength;
        this.codeRange = codeRange;
        this.isSingleByteOptimizable = child.isSingleByteOptimizable(); // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        this.depth = child.depth() + 1;
    }

    @Override
    public int characterLength() {
        return characterLength;
    }

    @Override
    public int byteLength() {
        return byteLength;
    }

    @Override
    public byte[] getBytes() {
        if (bytes == null) {
            bytes = child.extractRange(offset, byteLength);
        }

        return bytes;
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert length <= this.byteLength;

        return child.extractRange(this.offset + offset, length);
    }

    @Override
    public Encoding getEncoding() {
        return child.getEncoding();
    }

    @Override
    public int getCodeRange() {
        return codeRange;
    }

    @Override
    public boolean isSingleByteOptimizable() {
        return isSingleByteOptimizable;
    }

    @Override
    public int depth() {
        return depth;
    }
}
