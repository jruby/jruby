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

import org.jcodings.Encoding;

public abstract class LeafRope extends Rope {

    public LeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange, boolean singleByteOptimizable, int characterLength) {
        super(encoding, codeRange, singleByteOptimizable, bytes.length, characterLength, 1, bytes);
    }

    @Override
    public byte getByteSlow(int index) {
        return getRawBytes()[index];
    }

    @Override
    public String toString() {
        // This should be used for debugging only.
        return RopeOperations.decodeUTF8(this);
    }

    public LeafRope computeHashCode() {
        hashCode();
        return this;
    }

}
