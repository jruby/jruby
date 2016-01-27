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

public class SubstringRope extends Rope {

    private final Rope child;
    private final int offset;

    public SubstringRope(Rope child, int offset, int byteLength, int characterLength, int codeRange) {
        // TODO (nirvdrum 07-Jan-16) Verify that this rope is only used for character substrings and not arbitrary byte slices. The former should always have the child's code range while the latter may not.
        super(child.getEncoding(), codeRange, child.isSingleByteOptimizable(), byteLength, characterLength, child.depth() + 1, null);
        this.child = child;
        this.offset = offset;
    }

    @Override
    public int get(int index) {
        return child.get(index + offset);
    }

    @Override
    public byte[] extractRange(int offset, int length) {
        assert length <= this.byteLength();

        return child.extractRange(this.offset + offset, length);
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
