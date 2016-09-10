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
import org.jruby.truffle.platform.NativePointer;
import org.jruby.truffle.platform.SimpleNativeMemoryManager;
import org.jruby.util.unsafe.UnsafeHolder;

public class NativeRope extends Rope {

    private NativePointer pointer;

    public NativeRope(SimpleNativeMemoryManager nativeMemoryManager, byte[] bytes, Encoding encoding, int characterLength) {
        super(encoding, CodeRange.CR_UNKNOWN, false, bytes.length, characterLength, 1, null);

        pointer = nativeMemoryManager.allocate(bytes.length);

        for (int n = 0; n < bytes.length; n++) {
            pointer.writeByte(n, bytes[n]);
        }
    }

    @Override
    protected byte[] getBytesSlow() {
        final byte[] bytes = new byte[byteLength()];

        for (int n = 0; n < bytes.length; n++) {
            bytes[n] = pointer.readByte(n);
        }

        return bytes;
    }

    @Override
    public byte getByteSlow(int index) {
        return get(index);
    }

    @Override
    public byte get(int index) {
        return pointer.readByte(index);
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        throw new UnsupportedOperationException();
    }

    public NativePointer getNativePointer() {
        return pointer;
    }

}
