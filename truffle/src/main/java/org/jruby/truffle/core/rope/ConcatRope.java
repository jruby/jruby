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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jcodings.Encoding;

public class ConcatRope extends Rope {

    private final Rope left;
    private final Rope right;
    private final boolean balanced;

    public ConcatRope(Rope left, Rope right, Encoding encoding, CodeRange codeRange, boolean singleByteOptimizable, int depth, boolean balanced) {
        this(left, right, encoding, codeRange, singleByteOptimizable, depth, null, balanced);
    }

    private ConcatRope(Rope left, Rope right, Encoding encoding, CodeRange codeRange, boolean singleByteOptimizable, int depth, byte[] bytes, boolean balanced) {
        super(encoding,
                codeRange,
                singleByteOptimizable,
                left.byteLength() + right.byteLength(),
                left.characterLength() + right.characterLength(),
                depth,
                bytes);

        this.left = left;
        this.right = right;
        this.balanced = balanced;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        return new ConcatRope(getLeft(), getRight(), newEncoding, newCodeRange, isSingleByteOptimizable(), depth(), getRawBytes(), balanced);
    }

    @Override
    @TruffleBoundary
    public byte getByteSlow(int index) {
        if (index < left.byteLength()) {
            return left.getByteSlow(index);
        }

        return right.getByteSlow(index - left.byteLength());
    }

    public Rope getLeft() {
        return left;
    }

    public Rope getRight() {
        return right;
    }

    public boolean isBalanced() {
        return balanced;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return left.toString() + right.toString();
    }

}
