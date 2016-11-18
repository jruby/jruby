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
 * Some of the code in this class is modified from org.jruby.runtime.Helpers and org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Contains code modified from ByteList's ByteList.java
 *
 * Copyright (C) 2007-2010 JRuby Community
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 */
package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.encoding.EncodingManager;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import static org.jruby.truffle.core.rope.CodeRange.CR_7BIT;
import static org.jruby.truffle.core.rope.CodeRange.CR_BROKEN;
import static org.jruby.truffle.core.rope.CodeRange.CR_UNKNOWN;
import static org.jruby.truffle.core.rope.CodeRange.CR_VALID;

public class RopeOperations {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final ConcurrentHashMap<Encoding, Charset> encodingToCharsetMap = new ConcurrentHashMap<>();

    @TruffleBoundary
    public static LeafRope create(byte[] bytes, Encoding encoding, CodeRange codeRange) {
        if (bytes.length == 1) {
            final int index = bytes[0] & 0xff;

            if (encoding == UTF8Encoding.INSTANCE) {
                return RopeConstants.UTF8_SINGLE_BYTE_ROPES[index];
            }

            if (encoding == USASCIIEncoding.INSTANCE) {
                return RopeConstants.US_ASCII_SINGLE_BYTE_ROPES[index];
            }

            if (encoding == ASCIIEncoding.INSTANCE) {
                return RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[index];
            }
        }

        int characterLength = -1;

        if (codeRange == CR_UNKNOWN) {
            final long packedLengthAndCodeRange = calculateCodeRangeAndLength(encoding, bytes, 0, bytes.length);

            codeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);
        } else if (codeRange == CR_VALID) {
            characterLength = strLength(encoding, bytes, 0, bytes.length);
        }

        switch(codeRange) {
            case CR_7BIT: return new AsciiOnlyLeafRope(bytes, encoding);
            case CR_VALID: return new ValidLeafRope(bytes, encoding, characterLength);
            case CR_BROKEN: return new InvalidLeafRope(bytes, encoding);
            default: {
                throw new RuntimeException(StringUtils.format("Unknown code range type: %d", codeRange));
            }
        }
    }

    @TruffleBoundary
    public static Rope withEncodingVerySlow(Rope originalRope, Encoding newEncoding, CodeRange newCodeRange) {
        if ((originalRope.getEncoding() == newEncoding) && (originalRope.getCodeRange() == newCodeRange)) {
            return originalRope;
        }

        if (originalRope.getCodeRange() == newCodeRange) {
            return originalRope.withEncoding(newEncoding, newCodeRange);
        }

        if ((originalRope.getCodeRange() == CR_7BIT) && newEncoding.isAsciiCompatible()) {
            return originalRope.withEncoding(newEncoding, CR_7BIT);
        }

        return create(originalRope.getBytes(), newEncoding, newCodeRange);
    }

    public static Rope withEncodingVerySlow(Rope originalRope, Encoding newEncoding) {
        return withEncodingVerySlow(originalRope, newEncoding, originalRope.getCodeRange());
    }

    @TruffleBoundary
    public static String decodeUTF8(Rope rope) {
        return decode(UTF8, rope.getBytes(), 0, rope.byteLength());
    }

    public static String decodeUTF8(byte[] bytes, int offset, int byteLength) {
        return decode(UTF8, bytes, offset, byteLength);
    }

    @TruffleBoundary
    public static String decodeRope(Rope value) {
        // TODO CS 9-May-16 having recursive problems with this, so flatten up front for now

        value = flatten(value);

        int begin = 0;
        int length = value.byteLength();

        Encoding encoding = value.getEncoding();
        Charset charset = encodingToCharsetMap.computeIfAbsent(encoding, EncodingManager::charsetForEncoding);

        return decode(charset, value.getBytes(), begin, length);
    }

    @TruffleBoundary
    public static String decode(Charset charset, byte[] bytes, int offset, int byteLength) {
        return charset.decode(ByteBuffer.wrap(bytes, offset, byteLength)).toString();
    }

    // MRI: get_actual_encoding
    @TruffleBoundary
    public static Encoding STR_ENC_GET(Rope rope) {
        return EncodingUtils.getActualEncoding(rope.getEncoding(), rope.getBytes(), 0, rope.byteLength());
    }

    @TruffleBoundary
    public static long calculateCodeRangeAndLength(Encoding encoding, byte[] bytes, int start, int end) {
        if (bytes.length == 0) {
            return StringSupport.pack(0, encoding.isAsciiCompatible() ? CR_7BIT.toInt() : CR_VALID.toInt());
        } else if (encoding == ASCIIEncoding.INSTANCE) {
            return strLengthWithCodeRangeBinaryString(bytes, start, end);
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

    private static long strLengthWithCodeRangeBinaryString(byte[] bytes, int start, int end) {
        CodeRange codeRange = CR_7BIT;

        for (int i = start; i < end; i++) {
            if (bytes[i] < 0) {
                codeRange = CR_VALID;
                break;
            }
        }

        return StringSupport.pack(end - start, codeRange.toInt());
    }

    public static LeafRope flatten(Rope rope) {
        if (rope instanceof LeafRope) {
            return (LeafRope) rope;
        }

        return create(flattenBytes(rope), rope.getEncoding(), rope.getCodeRange());
    }

    public static void visitBytes(Rope rope, BytesVisitor visitor) {
        visitBytes(rope, visitor, 0, rope.byteLength());
    }

    @TruffleBoundary
    public static void visitBytes(Rope rope, BytesVisitor visitor, int offset, int length) {
        // TODO CS 9-May-16 make this the primitive, and have flatten use it

        visitor.accept(flattenBytes(rope), offset, length);
    }

    @TruffleBoundary
    public static byte[] extractRange(Rope rope, int offset, int length) {
        final byte[] result = new byte[length];

        final Memo<Integer> resultPosition = new Memo<>(0);

        visitBytes(rope, (bytes, offset1, length1) -> {
            final int resultPositionValue = resultPosition.get();
            System.arraycopy(bytes, offset1, result, resultPositionValue, length1);
            resultPosition.set(resultPositionValue + length1);
        }, offset, length);

        return result;
    }

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
    @TruffleBoundary
    public static byte[] flattenBytes(Rope rope) {
        if (rope.getRawBytes() != null) {
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

            // Force lazy ropes
            if (current instanceof LazyRope) {
                ((LazyRope) current).fulfill();
            }

            if (current.getRawBytes() != null) {
                // In the absence of any SubstringRopes, we always take the full contents of the current rope.
                if (substringLengths.isEmpty()) {
                    System.arraycopy(current.getRawBytes(), offset, buffer, bufferPosition, current.byteLength());
                    bufferPosition += current.byteLength();
                } else {
                    int bytesToCopy = substringLengths.pop();
                    final int currentBytesToCopy;

                    // If we reach here, this rope is a descendant of a SubstringRope at some level. Based on
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

                    // If this rope wasn't able to satisfy the remaining byte count from the ancestor SubstringRope,
                    // update the byte count for the next item in the work queue.
                    if (bytesToCopy > 0) {
                        substringLengths.push(bytesToCopy);
                    }
                }

                // By definition, offsets only affect the start of the rope. Once we've copied bytes out of a rope,
                // we need to reset the offset or subsequent items in the work queue will copy from the wrong location.
                //
                // NB: In contrast to the number of bytes to extract, the offset can be shared and updated by multiple
                // levels of SubstringRopes. Thus, we do not need to maintain offsets in a stack and it is appropriate
                // to clear the offset after the first time we use it, since it will have been updated accordingly at
                // each SubstringRope encountered for this SubstringRope ancestry chain.
                offset = 0;

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
                // so that when we finally reach a rope with its byte[] filled, we're extracting bytes from the correct
                // location.
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
            } else if (current instanceof RepeatingRope) {
                final RepeatingRope repeatingRope = (RepeatingRope) current;

                // In the absence of any SubstringRopes, we always take the full contents of the RepeatingRope.
                if (substringLengths.isEmpty()) {
                    // TODO (nirvdrum 06-Apr-16) Rather than process the same child over and over, there may be opportunity to re-use the results from a single pass.
                    for (int i = 0; i < repeatingRope.getTimes(); i++) {
                        workStack.push(repeatingRope.getChild());
                    }
                } else {
                    final int bytesToCopy = substringLengths.peek();
                    final int patternLength = repeatingRope.getChild().byteLength();

                    // Fix the offset to be appropriate for a given child. The offset is reset the first time it is
                    // consumed, so there's no need to worry about adversely affecting anything by adjusting it here.
                    offset %= repeatingRope.getChild().byteLength();

                    // The loopCount has to be precisely determined so every repetion has at least some parts used.
                    // It has to account for the begging we don't need (offset), has to reach the end but, and must not
                    // have extra repetitions.
                    int loopCount = (offset + bytesToCopy + patternLength - 1 ) / patternLength;

                    // TODO (nirvdrum 25-Aug-2016): Flattening the rope with CR_VALID will cause a character length recalculation, even though we already know what it is. That operation should be made more optimal.
                    final Rope flattenedChild = flatten(repeatingRope.getChild());
                    for (int i = 0; i < loopCount; i++) {
                        workStack.push(flattenedChild);
                    }
                }
            } else {
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
        } else if (rope instanceof RepeatingRope) {
            final RepeatingRope repeatingRope = (RepeatingRope) rope;
            final Rope child = repeatingRope.getChild();

            int remainingLength = length;
            final int patternLength = child.byteLength();
            int loopCount = (length + patternLength - 1) / patternLength;

            offset %= child.byteLength();

            // Adjust the loop count in case we're straddling two boundaries.
            if (offset > 0 && ((length - (patternLength - offset)) % patternLength) > 0) {
                loopCount++;
            }

            int hash = startingHashCode;
            for (int i = 0; i < loopCount; i++) {
                hash = hashForRange(child, hash, offset, remainingLength >= child.byteLength() ? child.byteLength() : remainingLength % child.byteLength());
                remainingLength = child.byteLength() - offset;
                offset = 0;
            }

            return hash;
        } else if (rope instanceof LazyRope) {
            return hashCodeForLeafRope(rope.getBytes(), startingHashCode, offset, length);
        } else {
            throw new RuntimeException("Hash code not supported for rope of type: " + rope.getClass().getName());
        }
    }

    @TruffleBoundary
    public static int cmp(Rope string, Rope other) {
        // Taken from org.jruby.util.ByteList#cmp.

        if (string == other) return 0;
        final int size = string.byteLength();
        final int len =  Math.min(size, other.byteLength());
        int offset = -1;

        final byte[] bytes = string.getBytes();
        final byte[] otherBytes = other.getBytes();

        // a bit of VM/JIT weirdness here: though in most cases
        // performance is improved if array references are kept in
        // a local variable (saves an instruction per access, as I
        // [slightly] understand it), in some cases, when two (or more?)
        // arrays are being accessed, the member reference is actually
        // faster.  this is one of those cases...
        for (  ; ++offset < len && bytes[offset] == otherBytes[offset]; ) ;
        if (offset < len) {
            return (bytes[offset]&0xFF) > (otherBytes[offset]&0xFF) ? 1 : -1;
        }
        return size == other.byteLength() ? 0 : size == len ? -1 : 1;
    }

    public static boolean areComparable(Rope rope, Rope other) {
        // Taken from org.jruby.util.StringSupport.areComparable.

        if (rope.getEncoding() == other.getEncoding() ||
                rope.isEmpty() || other.isEmpty()) return true;
        return areComparableViaCodeRange(rope, other);
    }

    public static boolean areComparableViaCodeRange(Rope string, Rope other) {
        // Taken from org.jruby.util.StringSupport.areComparableViaCodeRange.

        CodeRange cr1 = string.getCodeRange();
        CodeRange cr2 = other.getCodeRange();

        if (cr1 == CR_7BIT && (cr2 == CR_7BIT || other.getEncoding().isAsciiCompatible())) return true;
        if (cr2 == CR_7BIT && string.getEncoding().isAsciiCompatible()) return true;
        return false;
    }


    public static ByteList getByteListReadOnly(Rope rope) {
        return new ByteList(rope.getBytes(), rope.getEncoding(), false);
    }

    public static ByteList toByteListCopy(Rope rope) {
        return new ByteList(rope.getBytes(), rope.getEncoding(), true);
    }

    @TruffleBoundary
    public static Rope format(RubyContext context, Object... values) {
        Rope rope = null;

        for (Object value : values) {
            final Rope valueRope;

            if (value instanceof DynamicObject && RubyGuards.isRubyString(value)) {
                final Rope stringRope = Layouts.STRING.getRope((DynamicObject) value);
                final Encoding encoding = stringRope.getEncoding();

                if (encoding == UTF8Encoding.INSTANCE
                        || encoding == USASCIIEncoding.INSTANCE
                        || encoding == ASCIIEncoding.INSTANCE) {
                    valueRope = stringRope;
                } else {
                    valueRope = StringOperations.encodeRope(decodeRope(stringRope), UTF8Encoding.INSTANCE);
                }
            } else if (value instanceof Integer) {
                valueRope = new LazyIntRope((int) value);
            } else if (value instanceof String) {
                valueRope = context.getRopeTable().getRope((String) value);
            } else {
                throw new IllegalArgumentException();
            }

            if (rope == null) {
                rope = valueRope;
            } else {
                if (valueRope == null) {
                    throw new UnsupportedOperationException(value.getClass().toString());
                }

                rope = new ConcatRope(
                        rope,
                        valueRope,
                        UTF8Encoding.INSTANCE,
                        commonCodeRange(rope.getCodeRange(), valueRope.getCodeRange()),
                        rope.isSingleByteOptimizable() && valueRope.isSingleByteOptimizable(),
                        Math.max(rope.depth(), valueRope.depth()) + 1);

            }
        }

        if (rope == null) {
            rope = RopeConstants.EMPTY_UTF8_ROPE;
        }

        return rope;
    }

    private static CodeRange commonCodeRange(CodeRange first, CodeRange second) {
        if (first == second) {
            return first;
        }

        if ((first == CR_BROKEN) || (second == CR_BROKEN)) {
            return CR_BROKEN;
        }

        // If we get this far, one must be CR_7BIT and the other must be CR_VALID, so promote to the more general code range.

        return CR_VALID;
    }

}
