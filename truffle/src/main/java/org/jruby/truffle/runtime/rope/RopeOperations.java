/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.rope;

import com.oracle.truffle.api.CompilerDirectives;
import org.jcodings.Encoding;
import org.jruby.RubyEncoding;
import org.jruby.util.StringSupport;

public class RopeOperations {

    public static LeafRope create(byte[] bytes, Encoding encoding, int codeRange) {
        if (codeRange == StringSupport.CR_UNKNOWN) {
            codeRange = StringSupport.codeRangeScan(encoding, bytes, 0, bytes.length);
        }

        switch(codeRange) {
            case StringSupport.CR_7BIT: return new AsciiOnlyLeafRope(bytes, encoding);
            case StringSupport.CR_VALID: return new ValidLeafRope(bytes, encoding);
            case StringSupport.CR_UNKNOWN: return new UnknownLeafRope(bytes, encoding);
            case StringSupport.CR_BROKEN: return new InvalidLeafRope(bytes, encoding);
            default: throw new RuntimeException(String.format("Unknown code range type: %d", codeRange));
        }
    }

    public static Rope concat(Rope left, Rope right, Encoding encoding) {
        return new ConcatRope(left, right, encoding);
    }

    public static Rope substring(Rope base, int offset, int length) {
        return new SubstringRope(base, offset, length);
    }

    public static Rope template(Rope originalRope, Encoding newEncoding, int newCodeRange) {
        if ((originalRope.getEncoding() == newEncoding) && (originalRope.getCodeRange() == newCodeRange)) {
            return originalRope;
        }

        return create(originalRope.getBytes(), newEncoding, newCodeRange);
    }

    public static Rope template(Rope originalRope, Encoding newEncoding) {
        return create(originalRope.getBytes(), newEncoding, originalRope.getCodeRange());
    }

    @CompilerDirectives.TruffleBoundary
    public static String decodeUTF8(Rope rope) {
        return RubyEncoding.decodeUTF8(rope.getBytes(), 0, rope.byteLength());
    }

}
