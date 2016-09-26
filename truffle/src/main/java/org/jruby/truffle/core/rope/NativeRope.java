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

import jnr.ffi.Pointer;
import jnr.ffi.provider.MemoryManager;
import org.jcodings.Encoding;

public class NativeRope extends Rope {

    private Pointer pointer;

    public NativeRope(MemoryManager memoryManager, byte[] bytes, Encoding encoding, int characterLength) {
        super(encoding, CodeRange.CR_UNKNOWN, false, bytes.length, characterLength, 1, null);

        pointer = memoryManager.allocateDirect(bytes.length, false);
        pointer.put(0, bytes, 0, bytes.length);
    }

    @Override
    protected byte[] getBytesSlow() {
        final byte[] bytes = new byte[byteLength()];
        pointer.get(0, bytes, 0, bytes.length);
        return bytes;
    }

    @Override
    public byte getByteSlow(int index) {
        return get(index);
    }

    @Override
    public byte get(int index) {
        return pointer.getByte(index);
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        throw new UnsupportedOperationException();
    }

    public Pointer getNativePointer() {
        return pointer;
    }

}
