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
 * Some of the code in this class is modified from org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */

package org.jruby.truffle.core.rope;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.util.StringSupport;

import static org.jruby.truffle.core.rope.CodeRange.CR_7BIT;
import static org.jruby.truffle.core.rope.CodeRange.CR_BROKEN;
import static org.jruby.truffle.core.rope.CodeRange.CR_VALID;

public abstract class RopeNodes {

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "base"),
            @NodeChild(type = RubyNode.class, value = "offset"),
            @NodeChild(type = RubyNode.class, value = "byteLength")
    })
    public abstract static class MakeSubstringNode extends RubyNode {

        public static MakeSubstringNode create(RubyContext context, SourceSection sourceSection) {
            return RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        public MakeSubstringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeMake(Rope base, int offset, int byteLength);

        @Specialization(guards = "byteLength == 0")
        public Rope substringZeroLength(Rope base, int offset, int byteLength,
                                        @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                        @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                        @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit) {
            if (isUTF8.profile(base.getEncoding() == UTF8Encoding.INSTANCE)) {
                return RopeOperations.EMPTY_UTF8_ROPE;
            }

            if (isUSAscii.profile(base.getEncoding() == USASCIIEncoding.INSTANCE)) {
                return RopeOperations.EMPTY_US_ASCII_ROPE;
            }

            if (isAscii8Bit.profile(base.getEncoding() == ASCIIEncoding.INSTANCE)) {
                return RopeOperations.EMPTY_ASCII_8BIT_ROPE;
            }

            return RopeOperations.withEncoding(RopeOperations.EMPTY_UTF8_ROPE, base.getEncoding());
        }

        @Specialization(guards = { "byteLength > 0", "sameAsBase(base, offset, byteLength)" })
        public Rope substringSameAsBase(Rope base, int offset, int byteLength) {
            return base;
        }

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringLeafRope(LeafRope base, int offset, int byteLength,
                                  @Cached("createBinaryProfile()") ConditionProfile is7BitProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile);
        }

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringSubstringRope(SubstringRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile) {
            return makeSubstring(base.getChild(), offset + base.getOffset(), byteLength, is7BitProfile);
        }

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringConcatRope(ConcatRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile) {
            Rope root = base;

            while (root instanceof ConcatRope) {
                ConcatRope concatRoot = (ConcatRope) root;
                Rope left = concatRoot.getLeft();
                Rope right = concatRoot.getRight();

                // CASE 1: Fits in left.
                if (offset + byteLength <= left.byteLength()) {
                    root = left;
                    continue;
                }

                // CASE 2: Fits in right.
                if (offset >= left.byteLength()) {
                    offset -= left.byteLength();
                    root = right;
                    continue;
                }

                // CASE 3: Spans left and right.
                if (byteLength == root.byteLength()) {
                    return root;
                } else {
                    return makeSubstring(root, offset, byteLength, is7BitProfile);
                }
            }

            return makeSubstring(root, offset, byteLength, is7BitProfile);
        }

        private Rope makeSubstring(Rope base, int offset, int byteLength, ConditionProfile is7BitProfile) {
            if (is7BitProfile.profile(base.getCodeRange() == CR_7BIT)) {
                return new SubstringRope(base, offset, byteLength, byteLength, CR_7BIT);
            }

            return makeSubstringNon7Bit(base, offset, byteLength);
        }

        @TruffleBoundary
        private Rope makeSubstringNon7Bit(Rope base, int offset, int byteLength) {
            final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
            final CodeRange codeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            /*
            if (base.depth() >= 10) {
                System.out.println("SubstringRope depth: " + (base.depth() + 1));
            }
            */

            return new SubstringRope(base, offset, byteLength, characterLength, codeRange);
        }

        protected static boolean sameAsBase(Rope base, int offset, int byteLength) {
            return (byteLength - offset) == base.byteLength();
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "left"),
            @NodeChild(type = RubyNode.class, value = "right"),
            @NodeChild(type = RubyNode.class, value = "encoding")
    })
    public abstract static class MakeConcatNode extends RubyNode {

        protected static final int SHORT_LEAF_BYTESIZE_THRESHOLD = 128;

        @Child private MakeLeafRopeNode makeLeafRopeNode;

        public MakeConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeMake(Rope left, Rope right, Encoding encoding);

        @Specialization(guards = { "left.isEmpty()", "right.getEncoding() == encoding" })
        public Rope concatEmptyLeftSameEncoding(Rope left, Rope right, Encoding encoding) {
            return right;
        }

        @Specialization(guards = { "left.isEmpty()", "right.getEncoding() != encoding" })
        public Rope concatEmptyLeftDifferentEncoding(Rope left, Rope right, Encoding encoding) {
            return RopeOperations.withEncoding(right, encoding);
        }

        @Specialization(guards = { "right.isEmpty()", "left.getEncoding() == encoding" })
        public Rope concatEmptyRightSameEncoding(Rope left, Rope right, Encoding encoding) {
            return left;
        }

        @Specialization(guards = { "right.isEmpty()", "left.getEncoding() != encoding" })
        public Rope concatEmptyRightDifferentEncoding(Rope left, Rope right, Encoding encoding) {
            return RopeOperations.withEncoding(left, encoding);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()", "left.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatLeaves(LeafRope left, LeafRope right, Encoding encoding,
                                 @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile) {
            if (makeLeafRopeNode == null) {
                CompilerDirectives.transferToInterpreter();
                makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final byte[] bytes = new byte[left.byteLength() + right.byteLength()];
            System.arraycopy(left.getBytes(), 0, bytes, 0, left.byteLength());
            System.arraycopy(right.getBytes(), 0, bytes, left.byteLength(), right.byteLength());

            final CodeRange codeRange = commonCodeRange(left.getCodeRange(), right.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile);

            return makeLeafRopeNode.executeMake(bytes, encoding, codeRange);
        }

        @Specialization(guards = { "!right.isEmpty()", "left.byteLength() >= SHORT_LEAF_BYTESIZE_THRESHOLD", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatLeavesGeneral(LeafRope left, LeafRope right, Encoding encoding,
                                 @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile) {
            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatWithReduce(ConcatRope left, LeafRope right, Encoding encoding,
                                     @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile) {

            if ((left.getRight().byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD) && (left.getRight() instanceof LeafRope)) {
                final Rope compacted = concatLeaves((LeafRope) left.getRight(), right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile);
                return concat(left.getLeft(), compacted, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile);
            }

            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatSubstringLeaf(SubstringRope left, LeafRope right, Encoding encoding,
                                     @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile) {
            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()" })
        public Rope concat(Rope left, Rope right, Encoding encoding,
                           @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                           @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                           @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile) {
            int depth = depth(left, right);
            /*if (depth >= 10) {
                System.out.println("ConcatRope depth: " + depth);
            }*/

            return new ConcatRope(left, right, encoding,
                    commonCodeRange(left.getCodeRange(), right.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile),
                    isSingleByteOptimizable(left, right, isLeftSingleByteOptimizableProfile),
                    depth);
        }

        private CodeRange commonCodeRange(CodeRange first, CodeRange second,
                                    ConditionProfile sameCodeRangeProfile,
                                    ConditionProfile brokenCodeRangeProfile) {
            if (sameCodeRangeProfile.profile(first == second)) {
                return first;
            }

            if (brokenCodeRangeProfile.profile((first == CR_BROKEN) || (second == CR_BROKEN))) {
                return CR_BROKEN;
            }

            // If we get this far, one must be CR_7BIT and the other must be CR_VALID, so promote to the more general code range.
            return CR_VALID;
        }

        private boolean isSingleByteOptimizable(Rope left, Rope right, ConditionProfile isLeftSingleByteOptimizableProfile) {
            if (isLeftSingleByteOptimizableProfile.profile(left.isSingleByteOptimizable())) {
                return right.isSingleByteOptimizable();
            }

            return false;
        }

        private int depth(Rope left, Rope right) {
            return max(left.depth(), right.depth()) + 1;
        }

        private int max(int x, int y) {
            // This approach is adapted from http://graphics.stanford.edu/~seander/bithacks.html?1=1#IntegerMinOrMax
            return x - ((x - y) & ((x - y) >> (Integer.SIZE - 1)));
        }

        protected static boolean isShortLeafRope(Rope rope) {
            return (rope.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD) && isLeafRope(rope);
        }

        protected static boolean isLeafRope(Rope rope) {
            return rope instanceof LeafRope;
        }
    }


    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "bytes"),
            @NodeChild(type = RubyNode.class, value = "encoding"),
            @NodeChild(type = RubyNode.class, value = "codeRange")
    })
    public abstract static class MakeLeafRopeNode extends RubyNode {

        public MakeLeafRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract LeafRope executeMake(byte[] bytes, Encoding encoding, CodeRange codeRange);

        @Specialization(guards = "is7Bit(codeRange)")
        public LeafRope makeAsciiOnlyLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange) {
            return new AsciiOnlyLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isValid(codeRange)", "isFixedWidth(encoding)" })
        public LeafRope makeValidLeafRopeFixedWidthEncoding(byte[] bytes, Encoding encoding, CodeRange codeRange) {
            final int characterLength = bytes.length / encoding.minLength();

            return new ValidLeafRope(bytes, encoding, characterLength);
        }

        @Specialization(guards = { "isValid(codeRange)", "!isFixedWidth(encoding)" })
        public LeafRope makeValidLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange) {
            // Exctracted from StringSupport.strLength.

            int characterLength = 0;
            int p = 0;
            int e = bytes.length;

            while (p < e) {
                if (Encoding.isAscii(bytes[p])) {
                    int q = StringSupport.searchNonAscii(bytes, p, e);
                    if (q == -1) {
                        characterLength += (e - p);
                        break;
                    }
                    characterLength += q - p;
                    p = q;
                }
                p += StringSupport.encFastMBCLen(bytes, p, e, encoding);
                characterLength++;
            }

            return new ValidLeafRope(bytes, encoding, characterLength);
        }

        @Specialization(guards = "isBroken(codeRange)")
        public LeafRope makeInvalidLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange) {
            return new InvalidLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "isEmpty(bytes)" })
        public LeafRope makeUnknownLeafRopeEmpty(byte[] bytes, Encoding encoding, CodeRange codeRange,
                                                 @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                                 @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                                 @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit,
                                                 @Cached("createBinaryProfile()") ConditionProfile isAsciiCompatible) {
            if (isUTF8.profile(encoding == UTF8Encoding.INSTANCE)) {
                return RopeOperations.EMPTY_UTF8_ROPE;
            }

            if (isUSAscii.profile(encoding == USASCIIEncoding.INSTANCE)) {
                return RopeOperations.EMPTY_US_ASCII_ROPE;
            }

            if (isAscii8Bit.profile(encoding == ASCIIEncoding.INSTANCE)) {
                return RopeOperations.EMPTY_ASCII_8BIT_ROPE;
            }

            if (isAsciiCompatible.profile(encoding.isAsciiCompatible())) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            return new ValidLeafRope(bytes, encoding, 0);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isEmpty(bytes)", "isBinaryString(encoding)" })
        public LeafRope makeUnknownLeafRopeBinary(byte[] bytes, Encoding encoding, CodeRange codeRange,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile) {
            CodeRange newCodeRange = CR_7BIT;
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] < 0) {
                    newCodeRange = CR_VALID;
                    break;
                }
            }

            if (discovered7BitProfile.profile(newCodeRange == CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            return new ValidLeafRope(bytes, encoding, bytes.length);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isEmpty(bytes)", "!isBinaryString(encoding)", "isAsciiCompatible(encoding)" })
        public LeafRope makeUnknownLeafRopeAsciiCompatible(byte[] bytes, Encoding encoding, CodeRange codeRange,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile discoveredValidProfile) {
            final long packedLengthAndCodeRange = StringSupport.strLengthWithCodeRangeAsciiCompatible(encoding, bytes, 0, bytes.length);
            final CodeRange newCodeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            if (discovered7BitProfile.profile(newCodeRange == CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            if (discoveredValidProfile.profile(newCodeRange == CR_VALID)) {
                return new ValidLeafRope(bytes, encoding, characterLength);
            }

            return new InvalidLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isEmpty(bytes)", "!isBinaryString(encoding)", "!isAsciiCompatible(encoding)" })
        public LeafRope makeUnknownLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile discoveredValidProfile) {
            final long packedLengthAndCodeRange = StringSupport.strLengthWithCodeRangeNonAsciiCompatible(encoding, bytes, 0, bytes.length);
            final CodeRange newCodeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            if (discovered7BitProfile.profile(newCodeRange == CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            if (discoveredValidProfile.profile(newCodeRange == CR_VALID)) {
                return new ValidLeafRope(bytes, encoding, characterLength);
            }

            return new InvalidLeafRope(bytes, encoding);
        }

        protected static boolean is7Bit(CodeRange codeRange) {
            return codeRange == CR_7BIT;
        }

        protected static boolean isValid(CodeRange codeRange) {
            return codeRange == CR_VALID;
        }

        protected static boolean isBroken(CodeRange codeRange) {
            return codeRange == CR_BROKEN;
        }

        protected static boolean isUnknown(CodeRange codeRange) {
            return codeRange == CodeRange.CR_UNKNOWN;
        }

        protected static boolean isBinaryString(Encoding encoding) {
            return encoding == ASCIIEncoding.INSTANCE;
        }

        protected static boolean isEmpty(byte[] bytes) {
            return bytes.length == 0;
        }

        protected static boolean isAsciiCompatible(Encoding encoding) {
            return encoding.isAsciiCompatible();
        }

        protected static boolean isFixedWidth(Encoding encoding) {
            return encoding.isFixedWidth();
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope"),
            @NodeChild(type = RubyNode.class, value = "currentLevel"),
            @NodeChild(type = RubyNode.class, value = "printString")
    })
    public abstract static class DebugPrintRopeNode extends RubyNode {

        public DebugPrintRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract DynamicObject executeDebugPrint(Rope rope, int currentLevel, boolean printString);

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintLeafRope(LeafRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(String.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; D: %d)",
                    printString ? rope.toString() : "<skipped>",
                    rope.getClass().getSimpleName(),
                    bytesAreNull,
                    rope.byteLength(),
                    rope.characterLength(),
                    rope.getCodeRange(),
                    rope.depth()));

            return nil();
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintSubstringRope(SubstringRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(String.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; O: %d; D: %d)",
                    printString ? rope.toString() : "<skipped>",
                    rope.getClass().getSimpleName(),
                    bytesAreNull,
                    rope.byteLength(),
                    rope.characterLength(),
                    rope.getCodeRange(),
                    rope.getOffset(),
                    rope.depth()));

            executeDebugPrint(rope.getChild(), currentLevel + 1, printString);

            return nil();
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintConcatRope(ConcatRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(String.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; D: %d; LD: %d; RD: %d)",
                    printString ? rope.toString() : "<skipped>",
                    rope.getClass().getSimpleName(),
                    bytesAreNull,
                    rope.byteLength(),
                    rope.characterLength(),
                    rope.getCodeRange(),
                    rope.depth(),
                    rope.getLeft().depth(),
                    rope.getRight().depth()));

            executeDebugPrint(rope.getLeft(), currentLevel + 1, printString);
            executeDebugPrint(rope.getRight(), currentLevel + 1, printString);

            return nil();
        }

        private void printPreamble(int level) {
            if (level > 0) {
                for (int i = 0; i < level; i++) {
                    System.err.print("|  ");
                }
            }
        }

    }
}
