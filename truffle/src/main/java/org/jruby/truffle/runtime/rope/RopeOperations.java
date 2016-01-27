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
import java.util.ArrayDeque;
import java.util.Deque;

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

    public static Rope withEncoding(Rope originalRope, Encoding newEncoding, int newCodeRange) {
        if ((originalRope.getEncoding() == newEncoding) && (originalRope.getCodeRange() == newCodeRange)) {
            return originalRope;
        }

        return create(originalRope.getBytes(), newEncoding, newCodeRange);
    }

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
    public static long calculateCodeRangeAndLength(Encoding encoding, byte[] bytes, int start, int end) {
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

        return create(flattenBytes(rope), rope.getEncoding(), rope.getCodeRange());
    }

    @TruffleBoundary
    /**
     * Performs an iterative depth first search of the Rope tree to calculate its byte[] without needing to populate
     * the byte[] for each level beneath. Every LeafRope has its byte[] populated by definition. The goal is to determine
     * which descendant LeafRopes contribute bytes to the top-most Rope's logical byte[] and how many bytes they should
     * contribute. Then each such LeafRope copies the appropriate range of bytes to a shared byte[].
     *
     * Rope trees can be very deep. An iterative algorithm is preferable to recursion because it removes the overhead
     * of stack frame management. Additionally, a recursive algorithm will eventually overflow the stack if the Rope
     * tree is too deep.
     */
    public static byte[] flattenBytes(Rope rope) {
        if (rope instanceof LeafRope) {
            return rope.getRawBytes();
        }

        int bufferPosition = 0;
        int offset = 0;

        final byte[] buffer = new byte[rope.byteLength()];

        // As we traverse the rope tree, we need to keep track of any bounded lengths of SubstringRopes. LeafRopes always
        // provide their full byte[]. ConcatRope always provides the full byte[] of each of its children. SubstringRopes,
        // in contrast, may bound the length of their children. Since we may have SubstringRopes of SubstringRopes, we
        // need to track each SubstringRope's bounded length and how much that bounded length contributes to the total
        // byte[] for any ancestor (e.g., a SubstringRope of a ConcatRope with SubstringRopes for each of its children).
        // Because we need to track multiple levels, we can't use a single updated int.
        final Deque<Integer> substringLengths = new ArrayDeque<>();

        final Deque<Rope> workStack = new ArrayDeque<>();
        workStack.push(rope);

        while (!workStack.isEmpty()) {
            final Rope current = workStack.pop();

            // An empty rope trivially cannot contribute to filling the output buffer.
            if (current.isEmpty()) {
                continue;
            }

            if (current instanceof ConcatRope) {
                final ConcatRope concatRope = (ConcatRope) current;

                // In the absence of any SubstringRopes, we always take the full contents of the ConcatRope.
                if (substringLengths.isEmpty()) {
                    workStack.push(concatRope.getRight());
                    workStack.push(concatRope.getLeft());
                } else {
                    final int leftLength = concatRope.getLeft().byteLength();

                    // If we reach here, this ConcatRope is a descendant of a SubstringRope at some level. Based on
                    // the currently calculated byte[] offset and the number of bytes to extract, determine which of
                    // the ConcatRope's children we need to visit.
                    if (offset < leftLength) {
                        if ((offset + substringLengths.peek()) > leftLength) {
                            workStack.push(concatRope.getRight());
                            workStack.push(concatRope.getLeft());
                        } else {
                            workStack.push(concatRope.getLeft());
                        }
                    } else {
                        // If we can skip the left child entirely, we need to update the offset so it's accurate for
                        // the right child as each child's starting point is 0.
                        offset -= leftLength;
                        workStack.push(concatRope.getRight());
                    }
                }
            } else if (current instanceof SubstringRope) {
                final SubstringRope substringRope = (SubstringRope) current;

                // If this SubstringRope is a descendant of another SubstringRope, we need to increment the offset
                // so that when we finally reach a LeafRope, we're extracting bytes from the correct location.
                offset += substringRope.getOffset();

                workStack.push(substringRope.getChild());

                // Either we haven't seen another SubstringRope or it's been cleared off the work queue. In either case,
                // we can start fresh.
                if (substringLengths.isEmpty()) {
                    substringLengths.push(substringRope.byteLength());
                } else {
                    // Since we may be taking a substring of a substring, we need to note that we're not extracting the
                    // entirety of the current SubstringRope.
                    final int adjustedByteLength = substringRope.byteLength() - (offset - substringRope.getOffset());

                    // We have to do some bookkeeping once we encounter multiple SubstringRopes along the same ancestry
                    // chain. The top of the stack always indicates the number of bytes to extract from any descendants.
                    // Any bytes extracted from this SubstringRope must contribute to the total of the parent SubstringRope
                    // and are thus deducted. We can't simply update a total byte count, however, because we need distinct
                    // counts for each level.
                    //
                    // For example:                    SubstringRope (byteLength = 6)
                    //                                       |
                    //                                   ConcatRope (byteLength = 20)
                    //                                    /      \
                    //         SubstringRope (byteLength = 4)  LeafRope (byteLength = 16)
                    //               |
                    //           LeafRope (byteLength = 50)
                    //
                    // In this case we need to know that we're only extracting 4 bytes from descendants of the second
                    // SubstringRope. And those 4 bytes contribute to the total 6 bytes from the ancestor SubstringRope.
                    // The top of stack manipulation performed here maintains that invariant.

                    if (substringLengths.peek() > adjustedByteLength) {
                        final int bytesToCopy = substringLengths.pop();
                        substringLengths.push(bytesToCopy - adjustedByteLength);
                        substringLengths.push(adjustedByteLength);
                    }
                }
            } else if (current instanceof LeafRope) {
                // In the absence of any SubstringRopes, we always take the full contents of the LeafRope.
                if (substringLengths.isEmpty()) {
                    System.arraycopy(current.getRawBytes(), offset, buffer, bufferPosition, current.byteLength());
                    bufferPosition += current.byteLength();
                } else {
                    int bytesToCopy = substringLengths.pop();
                    final int currentBytesToCopy;

                    // If we reach here, this LeafRope is a descendant of a SubstringRope at some level. Based on
                    // the currently calculated byte[] offset and the number of bytes to extract, determine how many
                    // bytes we can copy to the buffer.
                    if (bytesToCopy > (current.byteLength() - offset)) {
                        currentBytesToCopy = current.byteLength() - offset;
                    } else {
                        currentBytesToCopy = bytesToCopy;
                    }

                    System.arraycopy(current.getRawBytes(), offset, buffer, bufferPosition, currentBytesToCopy);
                    bufferPosition += currentBytesToCopy;
                    bytesToCopy -= currentBytesToCopy;

                    // If this LeafRope wasn't able to satisfy the remaining byte count from the ancestor SubstringRope,
                    // update the byte count for the next item in the work queue.
                    if (bytesToCopy > 0) {
                        substringLengths.push(bytesToCopy);
                    }
                }

                // By definition, offsets only affect the start of the rope. Once we've copied bytes out of a LeafRope,
                // we need to reset the offset or subsequent items in the work queue will copy from the wrong location.
                //
                // NB: In contrast to the number of bytes to extract, the offset can be shared and updated by multiple
                // levels of SubstringRopes. Thus, we do not need to maintain offsets in a stack and it is appropriate
                // to clear the offset after the first time we use it, since it will have been updated accordingly at
                // each SubstringRope encountered for this SubstringRope ancestry chain.
                offset = 0;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException("Don't know how to flatten rope of type: " + rope.getClass().getName());
            }
        }

        return buffer;
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
