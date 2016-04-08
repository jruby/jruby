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
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.util.ByteList;

public class MutableRope extends LeafRope {

    private final ByteList byteList;

    protected MutableRope(byte[] bytes, Encoding encoding, CodeRange codeRange, boolean singleByteOptimizable, long characterLength) {
        super(bytes, encoding, codeRange, singleByteOptimizable, characterLength);
        this.byteList = new ByteList(bytes, encoding, true);
    }

    public MutableRope(Rope original) {
        this(original.getBytes(),
                original.getEncoding(),
                original.getCodeRange(),
                original.isSingleByteOptimizable(),
                original.characterLength());
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        byteList.setEncoding(newEncoding);
        return this;
    }

    @Override
    public byte getByteSlow(long index) {
        if (!CoreLibrary.fitsIntoInteger(index)) {
            CompilerDirectives.transferToInterpreter();
            throw new RopeTooLongException("Index outside of int range");
        }

        return (byte) byteList.get((int) index);
    }

    public ByteList getByteList() {
        return byteList;
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return byteList.toString();
    }

}
