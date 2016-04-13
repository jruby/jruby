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

public class AsciiOnlyLeafRope extends LeafRope {

    public AsciiOnlyLeafRope(byte[] bytes, Encoding encoding) {
        super(bytes, encoding, CodeRange.CR_7BIT, true, bytes.length);
    }

    @Override
    public Rope withEncoding(Encoding newEncoding, CodeRange newCodeRange) {
        if (newCodeRange != getCodeRange()) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("Cannot fast-path updating encoding with different code range.");
        }

        return new AsciiOnlyLeafRope(getRawBytes(), newEncoding);
    }
}
