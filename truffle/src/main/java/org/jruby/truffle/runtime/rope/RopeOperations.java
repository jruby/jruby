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
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyEncoding;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

public class RopeOperations {

    public static final Rope EMPTY_UTF8_ROPE = create(new byte[] {}, UTF8Encoding.INSTANCE, StringSupport.CR_7BIT);

    @CompilerDirectives.TruffleBoundary
    public static LeafRope create(byte[] bytes, Encoding encoding, int codeRange) {
        int characterLength = -1;

        if (codeRange == StringSupport.CR_UNKNOWN) {
            final long packedLengthAndCodeRange = calculateCodeRangeAndLength(encoding, bytes, 0, bytes.length);

            codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
            characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);
        }

        switch(codeRange) {
            case StringSupport.CR_7BIT: return new AsciiOnlyLeafRope(bytes, encoding);
            case StringSupport.CR_VALID: return new ValidLeafRope(bytes, encoding, characterLength);
            case StringSupport.CR_UNKNOWN: return new UnknownLeafRope(bytes, encoding);
            case StringSupport.CR_BROKEN: return new InvalidLeafRope(bytes, encoding);
            default: throw new RuntimeException(String.format("Unknown code range type: %d", codeRange));
        }
    }

    public static Rope concat(Rope left, Rope right, Encoding encoding) {
        if (right.isEmpty()) {
            return template(left, encoding);
        }

        if (left.isEmpty()) {
            return template(right, encoding);
        }

        return new ConcatRope(left, right, encoding);
    }

    public static Rope substring(Rope base, int offset, int byteLength) {
        if (byteLength == 0) {
            return template(EMPTY_UTF8_ROPE, base.getEncoding());
        }

        if (byteLength - offset == base.byteLength()) {
            return base;
        }

        if (base.getCodeRange() == StringSupport.CR_7BIT) {
            return new SubstringRope(base, offset, byteLength, byteLength, StringSupport.CR_7BIT);
        }

        final long packedLengthAndCodeRange = calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
        final int codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
        final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

        return new SubstringRope(base, offset, byteLength, characterLength, codeRange);
    }

    public static Rope template(Rope originalRope, Encoding newEncoding, int newCodeRange) {
        if ((originalRope.getEncoding() == newEncoding) && (originalRope.getCodeRange() == newCodeRange)) {
            return originalRope;
        }

        return create(originalRope.getBytes(), newEncoding, newCodeRange);
    }

    public static Rope template(Rope originalRope, Encoding newEncoding) {
        return template(originalRope, newEncoding, originalRope.getCodeRange());
    }

    @CompilerDirectives.TruffleBoundary
    public static String decodeUTF8(Rope rope) {
        return RubyEncoding.decodeUTF8(rope.getBytes(), 0, rope.byteLength());
    }

    // MRI: get_actual_encoding
    @CompilerDirectives.TruffleBoundary
    public static Encoding STR_ENC_GET(Rope rope) {
        return EncodingUtils.getActualEncoding(rope.getEncoding(), rope.getBytes(), 0, rope.byteLength());
    }

    @CompilerDirectives.TruffleBoundary
    private static long calculateCodeRangeAndLength(Encoding encoding, byte[] bytes, int start, int end) {
        if (encoding.isAsciiCompatible()) {
            return StringSupport.strLengthWithCodeRangeAsciiCompatible(encoding, bytes, start, end);
        } else {
            return StringSupport.strLengthWithCodeRangeNonAsciiCompatible(encoding, bytes, start, end);
        }
    }

}
