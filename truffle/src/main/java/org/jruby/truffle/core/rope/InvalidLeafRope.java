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

public class InvalidLeafRope extends LeafRope {

    public InvalidLeafRope(byte[] bytes, Encoding encoding, int characterLength) {
        super(bytes, encoding, CodeRange.CR_BROKEN, false, characterLength);
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        final int newCharacterLength = RopeOperations.strLength(newEncoding, getRawBytes(), 0, byteLength());
        return new InvalidLeafRope(getRawBytes(), newEncoding, newCharacterLength);
    }
}
