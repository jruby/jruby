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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.util.ByteList;
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

        @Child private MakeLeafRopeNode makeLeafRopeNode;

        public static MakeSubstringNode create(RubyContext context, SourceSection sourceSection) {
            return RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        public MakeSubstringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeMake(Rope base, int offset, int byteLength);

        @Specialization(guards = "byteLength == 0")
        public Rope substringZeroBytes(Rope base, int offset, int byteLength,
                                        @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                        @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                        @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit) {
            if (isUTF8.profile(base.getEncoding() == UTF8Encoding.INSTANCE)) {
                return RopeConstants.EMPTY_UTF8_ROPE;
            }

            if (isUSAscii.profile(base.getEncoding() == USASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_US_ASCII_ROPE;
            }

            if (isAscii8Bit.profile(base.getEncoding() == ASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_ASCII_8BIT_ROPE;
            }

            return RopeOperations.withEncodingVerySlow(RopeConstants.EMPTY_UTF8_ROPE, base.getEncoding());
        }

        @Specialization(guards = "byteLength == 1")
        public Rope substringOneByte(Rope base, int offset, int byteLength,
                                        @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                        @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                        @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit,
                                        @Cached("create(getContext(), getSourceSection())") GetByteNode getByteNode) {
            final int index = getByteNode.executeGetByte(base, offset);

            if (isUTF8.profile(base.getEncoding() == UTF8Encoding.INSTANCE)) {
                return RopeConstants.UTF8_SINGLE_BYTE_ROPES[index];
            }

            if (isUSAscii.profile(base.getEncoding() == USASCIIEncoding.INSTANCE)) {
                return RopeConstants.US_ASCII_SINGLE_BYTE_ROPES[index];
            }

            if (isAscii8Bit.profile(base.getEncoding() == ASCIIEncoding.INSTANCE)) {
                return RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[index];
            }

            return RopeOperations.withEncodingVerySlow(RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[index], base.getEncoding());
        }

        @Specialization(guards = { "byteLength > 1", "sameAsBase(base, offset, byteLength)" })
        public Rope substringSameAsBase(Rope base, int offset, int byteLength) {
            return base;
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringLeafRope(LeafRope base, int offset, int byteLength,
                                  @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                  @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringSubstringRope(SubstringRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base.getChild(), offset + base.getOffset(), byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringMultiplyRope(RepeatingRope base, int offset, int byteLength,
                                          @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, offset, byteLength)" })
        public Rope substringConcatRope(ConcatRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
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
                    return makeSubstring(root, offset, byteLength, is7BitProfile, isBinaryStringProfile);
                }
            }

            return makeSubstring(root, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        private Rope makeSubstring(Rope base, int offset, int byteLength, ConditionProfile is7BitProfile, ConditionProfile isBinaryStringProfile) {
            if (is7BitProfile.profile(base.getCodeRange() == CR_7BIT)) {
                if (getContext().getOptions().ROPE_LAZY_SUBSTRINGS) {
                    return new SubstringRope(base, offset, byteLength, byteLength, CR_7BIT);
                } else {
                    return new AsciiOnlyLeafRope(base.extractRange(offset, byteLength), base.getEncoding());
                }
            }

            // We short-circuit here to avoid the costly process of recalculating information we already know, such as
            // whether the string has a valid code range.
            if (isBinaryStringProfile.profile(base.getEncoding() == ASCIIEncoding.INSTANCE)) {
                if (getContext().getOptions().ROPE_LAZY_SUBSTRINGS) {
                    return new SubstringRope(base, offset, byteLength, byteLength, CR_VALID);
                } else {
                    return new ValidLeafRope(base.extractRange(offset, byteLength), base.getEncoding(), byteLength);
                }
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

            if (getContext().getOptions().ROPE_LAZY_SUBSTRINGS) {
                return new SubstringRope(base, offset, byteLength, characterLength, codeRange);
            } else {
                if (makeLeafRopeNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(getContext(), getSourceSection(), null, null, null, null));
                }

                final byte[] bytes = base.extractRange(offset, byteLength);

                return makeLeafRopeNode.executeMake(bytes, base.getEncoding(), codeRange, characterLength);
            }
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

        public MakeConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeMake(Rope left, Rope right, Encoding encoding);

        @Specialization(guards = "isMutableRope(left)")
        public Rope concatMutableRope(MutableRope left, Rope right, Encoding encoding,
                                      @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile) {
            final ByteList byteList = left.getByteList();

            byteList.append(right.getBytes());

            if (differentEncodingProfile.profile(byteList.getEncoding() != encoding)) {
                byteList.setEncoding(encoding);
            }

            return left;
        }

        @Specialization(guards = { "!isMutableRope(left)" })
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
            return Math.max(left.depth(), right.depth()) + 1;
        }

        protected static boolean isMutableRope(Rope rope) {
            return rope instanceof MutableRope;
        }
    }


    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "bytes"),
            @NodeChild(type = RubyNode.class, value = "encoding"),
            @NodeChild(type = RubyNode.class, value = "codeRange"),
            @NodeChild(type = RubyNode.class, value = "characterLength")
    })
    public abstract static class MakeLeafRopeNode extends RubyNode {

        public static MakeLeafRopeNode create(RubyContext context, SourceSection sourceSection) {
            return RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        public MakeLeafRopeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract LeafRope executeMake(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength);

        @Specialization(guards = "is7Bit(codeRange)")
        public LeafRope makeAsciiOnlyLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength) {
            return new AsciiOnlyLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isValid(codeRange)", "wasProvided(characterLength)" })
        public LeafRope makeValidLeafRopeWithCharacterLength(byte[] bytes, Encoding encoding, CodeRange codeRange, int characterLength) {
            return new ValidLeafRope(bytes, encoding, characterLength);
        }

        @Specialization(guards = { "isValid(codeRange)", "isFixedWidth(encoding)", "wasNotProvided(characterLength)" })
        public LeafRope makeValidLeafRopeFixedWidthEncoding(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength) {
            final int calculatedCharacterLength = bytes.length / encoding.minLength();

            return new ValidLeafRope(bytes, encoding, calculatedCharacterLength);
        }

        @Specialization(guards = { "isValid(codeRange)", "!isFixedWidth(encoding)", "wasNotProvided(characterLength)" })
        public LeafRope makeValidLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength) {
            // Exctracted from StringSupport.strLength.

            int calculatedCharacterLength = 0;
            int p = 0;
            int e = bytes.length;

            while (p < e) {
                if (Encoding.isAscii(bytes[p])) {
                    int q = StringSupport.searchNonAscii(bytes, p, e);
                    if (q == -1) {
                        calculatedCharacterLength += (e - p);
                        break;
                    }
                    calculatedCharacterLength += q - p;
                    p = q;
                }
                int delta = StringSupport.encFastMBCLen(bytes, p, e, encoding);

                if (delta < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new UnsupportedOperationException("Code rang is reported as valid, but is invalid for the given encoding: " + encoding.toString());
                }

                p += delta;
                calculatedCharacterLength++;
            }

            return new ValidLeafRope(bytes, encoding, calculatedCharacterLength);
        }

        @Specialization(guards = "isBroken(codeRange)")
        public LeafRope makeInvalidLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength) {
            return new InvalidLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "isEmpty(bytes)" })
        public LeafRope makeUnknownLeafRopeEmpty(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength,
                                                 @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                                 @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                                 @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit,
                                                 @Cached("createBinaryProfile()") ConditionProfile isAsciiCompatible) {
            if (isUTF8.profile(encoding == UTF8Encoding.INSTANCE)) {
                return RopeConstants.EMPTY_UTF8_ROPE;
            }

            if (isUSAscii.profile(encoding == USASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_US_ASCII_ROPE;
            }

            if (isAscii8Bit.profile(encoding == ASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_ASCII_8BIT_ROPE;
            }

            if (isAsciiCompatible.profile(encoding.isAsciiCompatible())) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            return new ValidLeafRope(bytes, encoding, 0);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isEmpty(bytes)", "isBinaryString(encoding)" })
        public LeafRope makeUnknownLeafRopeBinary(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength,
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
        public LeafRope makeUnknownLeafRopeAsciiCompatible(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile discoveredValidProfile) {
            final long packedLengthAndCodeRange = StringSupport.strLengthWithCodeRangeAsciiCompatible(encoding, bytes, 0, bytes.length);
            final CodeRange newCodeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int calculatedCharacterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            if (discovered7BitProfile.profile(newCodeRange == CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            if (discoveredValidProfile.profile(newCodeRange == CR_VALID)) {
                return new ValidLeafRope(bytes, encoding, calculatedCharacterLength);
            }

            return new InvalidLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isEmpty(bytes)", "!isBinaryString(encoding)", "!isAsciiCompatible(encoding)" })
        public LeafRope makeUnknownLeafRope(byte[] bytes, Encoding encoding, CodeRange codeRange, Object characterLength,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile discoveredValidProfile) {
            final long packedLengthAndCodeRange = StringSupport.strLengthWithCodeRangeNonAsciiCompatible(encoding, bytes, 0, bytes.length);
            final CodeRange newCodeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int calculatedCharacterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            if (discovered7BitProfile.profile(newCodeRange == CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            if (discoveredValidProfile.profile(newCodeRange == CR_VALID)) {
                return new ValidLeafRope(bytes, encoding, calculatedCharacterLength);
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

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope"),
            @NodeChild(type = RubyNode.class, value = "encoding"),
            @NodeChild(type = RubyNode.class, value = "codeRange")
    })
    public abstract static class WithEncodingNode extends RubyNode {

        public WithEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeWithEncoding(Rope rope, Encoding encoding, CodeRange codeRange);

        @Specialization(guards = "rope.getEncoding() == encoding")
        public Rope withEncodingSameEncoding(Rope rope, Encoding encoding, CodeRange codeRange) {
            return rope;
        }

        @Specialization(guards = {
                "rope.getEncoding() != encoding",
                "rope.getCodeRange() == codeRange"
        })
        public Rope withEncodingSameCodeRange(Rope rope, Encoding encoding, CodeRange codeRange) {
            return rope.withEncoding(encoding, codeRange);
        }

        @Specialization(guards = {
                "rope.getEncoding() != encoding",
                "rope.getCodeRange() != codeRange",
                "isAsciiCompatbileChange(rope, encoding)",
                "rope.getClass() == cachedRopeClass"
        }, limit = "getCacheLimit()")
        public Rope withEncodingCr7Bit(Rope rope, Encoding encoding, CodeRange codeRange,
                                       @Cached("rope.getClass()") Class<? extends Rope> cachedRopeClass) {
            return cachedRopeClass.cast(rope).withEncoding(encoding, CodeRange.CR_7BIT);
        }

        @Specialization(guards = {
                "rope.getEncoding() != encoding",
                "rope.getCodeRange() != codeRange",
                "!isAsciiCompatbileChange(rope, encoding)"
        })
        public Rope withEncoding(Rope rope, Encoding encoding, CodeRange codeRange,
                                 @Cached("create(getContext(), getSourceSection())") MakeLeafRopeNode makeLeafRopeNode) {
            return makeLeafRopeNode.executeMake(rope.getBytes(), encoding, codeRange, NotProvided.INSTANCE);
        }

        protected static boolean isAsciiCompatbileChange(Rope rope, Encoding encoding) {
            return rope.getCodeRange() == CR_7BIT && encoding.isAsciiCompatible();
        }

        protected static boolean is7Bit(Rope rope) {
            return rope.getCodeRange() == CR_7BIT;
        }

        protected int getCacheLimit() {
            return getContext().getOptions().ROPE_CLASS_CACHE;
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope"),
            @NodeChild(type = RubyNode.class, value = "index")
    })
    public abstract static class GetByteNode extends RubyNode {

        public static GetByteNode create(RubyContext context, SourceSection sourceSection) {
            return RopeNodesFactory.GetByteNodeGen.create(context, sourceSection, null, null);
        }

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract int executeGetByte(Rope rope, int index);

        @Specialization(guards = "rope.getRawBytes() != null")
        public int getByte(Rope rope, int index) {
            return rope.getRawBytes()[index] & 0xff;
        }

        @Specialization(guards = "rope.getRawBytes() == null")
        @TruffleBoundary
        public int getByteSlow(Rope rope, int index) {
            return rope.get(index) & 0xff;
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope")
    })
    public abstract static class FlattenNode extends RubyNode {

        @Child private MakeLeafRopeNode makeLeafRopeNode;

        public static FlattenNode create(RubyContext context, SourceSection sourceSection) {
            return RopeNodesFactory.FlattenNodeGen.create(context, sourceSection, null);
        }

        public FlattenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = MakeLeafRopeNode.create(context, sourceSection);
        }

        public abstract LeafRope executeFlatten(Rope rope);

        @Specialization
        public LeafRope flattenLeafRope(LeafRope rope) {
            return rope;
        }

        @Specialization(guards = { "!isLeafRope(rope)", "rope.getRawBytes() != null" })
        public LeafRope flattenNonLeafWithBytes(Rope rope) {
            return makeLeafRopeNode.executeMake(rope.getRawBytes(), rope.getEncoding(), rope.getCodeRange(), rope.characterLength());
        }

        @Specialization(guards = { "!isLeafRope(rope)", "rope.getRawBytes() == null" })
        public LeafRope flatten(Rope rope) {
            // NB: We call RopeOperations.flatten here rather than Rope#getBytes so we don't populate the byte[] in
            // the source `rope`. Otherwise, we'll end up a fully populated reference in both the source `rope` and the
            // flattened one, which could adversely affect GC.
            final byte[] bytes = RopeOperations.flattenBytes(rope);

            return makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength());
        }

        protected static boolean isLeafRope(Rope rope) {
            return rope instanceof LeafRope;
        }

    }

}
