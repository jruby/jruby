/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Some of the code in this class is modified from org.jruby.runtime.Helpers,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */
package org.jruby.truffle.runtime.rope;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class RopeOperations {

    public static final Rope EMPTY_ASCII_8BIT_ROPE = create(new byte[] {}, ASCIIEncoding.INSTANCE, StringSupport.CR_7BIT);
    public static final Rope EMPTY_UTF8_ROPE = create(new byte[] {}, UTF8Encoding.INSTANCE, StringSupport.CR_7BIT);

    public static LeafRope create(byte[] bytes, Encoding encoding, int codeRange) {
        int characterLength = -1;

        if (codeRange == StringSupport.CR_UNKNOWN) {
            final long packedLengthAndCodeRange = calculateCodeRangeAndLength(encoding, bytes, 0, bytes.length);

            codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
            characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);
        } else if (codeRange == StringSupport.CR_VALID) {
            characterLength = strLength(encoding, bytes, 0, bytes.length);
        }

        switch(codeRange) {
            case StringSupport.CR_7BIT: return new AsciiOnlyLeafRope(bytes, encoding);
            case StringSupport.CR_VALID: return new ValidLeafRope(bytes, encoding, characterLength);
            case StringSupport.CR_BROKEN: return new InvalidLeafRope(bytes, encoding);
            default: {
                CompilerDirectives.transferToInterpreter();
                throw new RuntimeException(String.format("Unknown code range type: %d", codeRange));
            }
        }
    }

    @TruffleBoundary
    public static Rope concat(Rope left, Rope right, Encoding encoding) {
        if (right.isEmpty()) {
            return withEncoding(left, encoding);
        }

        if (left.isEmpty()) {
            return withEncoding(right, encoding);
        }

        return new ConcatRope(left, right, encoding);
    }

    public static Rope substring(Rope base, int offset, int byteLength) {
        if (byteLength == 0) {
            return withEncoding(EMPTY_UTF8_ROPE, base.getEncoding());
        }

        if (byteLength - offset == base.byteLength()) {
            return base;
        }

        if (base instanceof SubstringRope) {
            return substringSubstringRope((SubstringRope) base, offset, byteLength);
        } else if (base instanceof ConcatRope) {
            return substringConcatRope((ConcatRope) base, offset, byteLength);
        }

        return makeSubstring(base, offset, byteLength);
    }

    private static Rope substringSubstringRope(SubstringRope base, int offset, int byteLength) {
        return makeSubstring(base.getChild(), offset + base.getOffset(), byteLength);
    }

    @TruffleBoundary
    private static Rope substringConcatRope(ConcatRope base, int offset, int byteLength) {
        if (offset + byteLength <= base.getLeft().byteLength()) {
            return substring(base.getLeft(), offset, byteLength);
        } else if (offset >= base.getLeft().byteLength()) {
            return substring(base.getRight(), offset - base.getLeft().byteLength(), byteLength);
        }

        return makeSubstring(base, offset, byteLength);
    }

    private static Rope makeSubstring(Rope base, int offset, int byteLength) {
        if (base.getCodeRange() == StringSupport.CR_7BIT) {
            return new SubstringRope(base, offset, byteLength, byteLength, StringSupport.CR_7BIT);
        }

        return makeSubstringNon7Bit(base, offset, byteLength);
    }

    @TruffleBoundary
    private static Rope makeSubstringNon7Bit(Rope base, int offset, int byteLength) {
        final long packedLengthAndCodeRange = calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
        final int codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
        final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

        return new SubstringRope(base, offset, byteLength, characterLength, codeRange);
    }

    public static Rope withEncoding(Rope originalRope, Encoding newEncoding, int newCodeRange) {
        if ((originalRope.getEncoding() == newEncoding) && (originalRope.getCodeRange() == newCodeRange)) {
            return originalRope;
        }

        return create(originalRope.getBytes(), newEncoding, newCodeRange);
    }

    @TruffleBoundary
    public static Rope withEncoding(Rope originalRope, Encoding newEncoding) {
        return withEncoding(originalRope, newEncoding, originalRope.getCodeRange());
    }

    @TruffleBoundary
    public static String decodeUTF8(Rope rope) {
        return RubyEncoding.decodeUTF8(rope.getBytes(), 0, rope.byteLength());
    }

    @TruffleBoundary
    public static String decodeRope(Ruby runtime, Rope value) {
        if (value instanceof LeafRope) {
            int begin = value.getBegin();
            int length = value.byteLength();

            Encoding encoding = value.getEncoding();

            if (encoding == UTF8Encoding.INSTANCE) {
                return RubyEncoding.decodeUTF8(value.getBytes(), begin, length);
            }

            Charset charset = runtime.getEncodingService().charsetForEncoding(encoding);

            if (charset == null) {
                try {
                    return new String(value.getBytes(), begin, length, encoding.toString());
                } catch (UnsupportedEncodingException uee) {
                    return value.toString();
                }
            }

            return RubyEncoding.decode(value.getBytes(), begin, length, charset);
        } else if (value instanceof SubstringRope) {
            final SubstringRope substringRope = (SubstringRope) value;

            return decodeRope(runtime, substringRope.getChild()).substring(substringRope.getOffset(), substringRope.getOffset() + substringRope.characterLength());
        } else if (value instanceof ConcatRope) {
            final ConcatRope concatRope = (ConcatRope) value;

            return decodeRope(runtime, concatRope.getLeft()) + decodeRope(runtime, concatRope.getRight());
        } else {
            throw new RuntimeException("Decoding to String is not supported for rope of type: " + value.getClass().getName());
        }
    }

    // MRI: get_actual_encoding
    @TruffleBoundary
    public static Encoding STR_ENC_GET(Rope rope) {
        return EncodingUtils.getActualEncoding(rope.getEncoding(), rope.getBytes(), 0, rope.byteLength());
    }

    @TruffleBoundary
    private static long calculateCodeRangeAndLength(Encoding encoding, byte[] bytes, int start, int end) {
        if (bytes.length == 0) {
            return StringSupport.pack(0, encoding.isAsciiCompatible() ? StringSupport.CR_7BIT : StringSupport.CR_VALID);
        } else if (encoding.isAsciiCompatible()) {
            return StringSupport.strLengthWithCodeRangeAsciiCompatible(encoding, bytes, start, end);
        } else {
            return StringSupport.strLengthWithCodeRangeNonAsciiCompatible(encoding, bytes, start, end);
        }
    }

    @TruffleBoundary
    public static int strLength(Encoding enc, byte[] bytes, int p, int end) {
        return StringSupport.strLength(enc, bytes, p, end);
    }

    public static LeafRope flatten(Rope rope) {
        if (rope instanceof LeafRope) {
            return (LeafRope) rope;
        }

        return create(rope.getBytes(), rope.getEncoding(), rope.getCodeRange());
    }

    public static int hashCodeForLeafRope(byte[] bytes, int startingHashCode, int offset, int length) {
        assert offset <= bytes.length;
        assert length <= bytes.length;

        int hashCode = startingHashCode;
        final int endIndex = offset + length;
        for (int i = offset; i < endIndex; i++) {
            hashCode = 31 * hashCode + bytes[i];
        }

        return hashCode;
    }

    @TruffleBoundary
    public static int hashForRange(Rope rope, int startingHashCode, int offset, int length) {
        if (rope instanceof LeafRope) {
            return hashCodeForLeafRope(rope.getBytes(), startingHashCode, offset, length);
        } else if (rope instanceof SubstringRope) {
            final SubstringRope substringRope = (SubstringRope) rope;

            return hashForRange(substringRope.getChild(), startingHashCode, offset + substringRope.getOffset(), length);
        } else if (rope instanceof ConcatRope) {
            final ConcatRope concatRope = (ConcatRope) rope;
            final Rope left = concatRope.getLeft();
            final Rope right = concatRope.getRight();

            int hash = startingHashCode;
            final int leftLength = left.byteLength();

            if (offset < leftLength) {
                // The left branch might not be large enough to extract the full hash code we want. In that case,
                // we'll extract what we can and extract the difference from the right side.
                if (offset + length > leftLength) {
                    final int coveredByLeft = leftLength - offset;
                    hash = hashForRange(left, hash, offset, coveredByLeft);
                    hash = hashForRange(right, hash, 0, length - coveredByLeft);

                    return hash;
                } else {
                    return hashForRange(left, hash, offset, length);
                }
            }

            return hashForRange(right, hash, offset - leftLength, length);
        } else {
            throw new RuntimeException("Hash code not supported for rope of type: " + rope.getClass().getName());
        }
    }

}
