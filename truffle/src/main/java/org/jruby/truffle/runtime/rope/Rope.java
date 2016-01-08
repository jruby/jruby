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

    public abstract int length();

    public abstract int byteLength();

    public abstract ByteList getByteList();

    public abstract byte[] getBytes();

    public abstract byte[] extractRange(int offset, int length);

    public abstract Encoding getEncoding();

    public abstract int getCodeRange();

    public abstract boolean isSingleByteOptimizable();

    @Override
    public String toString() {
        // This should be used for debugging only.
        return new String(getBytes());
    }

}
