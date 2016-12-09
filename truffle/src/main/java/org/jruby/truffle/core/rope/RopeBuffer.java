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
import org.jruby.truffle.util.ByteList;

public class RopeBuffer extends LeafRope {

    private final ByteList byteList;

    protected RopeBuffer(byte[] bytes, Encoding encoding, CodeRange codeRange, boolean singleByteOptimizable, int characterLength) {
        super(bytes, encoding, codeRange, singleByteOptimizable, characterLength);
        this.byteList = new ByteList(bytes, encoding, false);
    }

    public RopeBuffer(Rope original) {
        this(original.getBytesCopy(),
                original.getEncoding(),
                original.getCodeRange(),
                original.isSingleByteOptimizable(),
                original.characterLength());
    }

    public RopeBuffer(ByteList byteList, CodeRange codeRange, boolean singleByteOptimizable, int characterLength) {
        super(byteList.unsafeBytes(), byteList.getEncoding(), codeRange, singleByteOptimizable, characterLength);
        this.byteList =  byteList;
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        byteList.setEncoding(newEncoding);
        return this;
    }

    @Override
    public byte getByteSlow(int index) {
        return (byte) byteList.get(index);
    }

    public ByteList getByteList() {
        return byteList;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return byteList.toString();
    }

    public RopeBuffer dup() {
        return new RopeBuffer(byteList.dup(), getCodeRange(), isSingleByteOptimizable(), characterLength());
    }
}
