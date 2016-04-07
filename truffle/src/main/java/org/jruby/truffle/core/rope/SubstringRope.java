/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jcodings.Encoding;

public class SubstringRope extends Rope {

    private final Rope child;
    private final int offset;

    public SubstringRope(Rope child, int offset, int byteLength, int characterLength, CodeRange codeRange) {
        // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        this(child, child.getEncoding(), offset, byteLength, characterLength, codeRange);
    }

    private SubstringRope(Rope child, Encoding encoding, int offset, int byteLength, int characterLength, CodeRange codeRange) {
        // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        super(encoding, codeRange, child.isSingleByteOptimizable(), byteLength, characterLength, child.depth() + 1, null);
        this.child = child;
        this.offset = offset;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        return new SubstringRope(getChild(), newEncoding, getOffset(), byteLength(), characterLength(), newCodeRange);
    }

    @Override
    public byte getByteSlow(int index) {
        return child.getByteSlow(index + offset);
    }

    public Rope getChild() {
        return child;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return child.toString().substring(offset, offset + byteLength());
    }

}
