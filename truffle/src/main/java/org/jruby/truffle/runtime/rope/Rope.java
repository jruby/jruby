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

public abstract class Rope {

    public abstract int characterLength();

    public abstract int byteLength();

    public boolean isEmpty() {
        return byteLength() == 0;
    }

    public final ByteList getUnsafeByteList() {
        return new ByteList(getBytes(), getEncoding(), false);
    }

    public final ByteList toByteListCopy() { return new ByteList(getBytes(), getEncoding(), true); }

    public abstract byte[] getBytes();

    public byte[] getBytesCopy() {
        final byte[] originalBytes = getBytes();
        final byte[] ret = new byte[originalBytes.length];

        System.arraycopy(originalBytes, 0, ret, 0, ret.length);

        return ret;
    }

    public abstract byte[] extractRange(int offset, int length);

    public abstract Encoding getEncoding();

    public abstract int getCodeRange();

    public abstract boolean isSingleByteOptimizable();

    public abstract int depth();

    @Override
    public String toString() {
        // This should be used for debugging only.
        return new String(getBytes());
    }

}
