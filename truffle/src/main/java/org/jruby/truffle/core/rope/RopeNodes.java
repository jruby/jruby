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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.Arrays;

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

        public static MakeSubstringNode createX() {
            return RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        public abstract Rope executeMake(Rope base, int offset, int byteLength);

        @Specialization(guards = "byteLength == 0")
        public Rope substringZeroBytes(Rope base, int offset, int byteLength,
                                        @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                        @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                        @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit,
                                        @Cached("create()") WithEncodingNode withEncodingNode) {
            if (isUTF8.profile(base.getEncoding() == UTF8Encoding.INSTANCE)) {
                return RopeConstants.EMPTY_UTF8_ROPE;
            }

            if (isUSAscii.profile(base.getEncoding() == USASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_US_ASCII_ROPE;
            }

            if (isAscii8Bit.profile(base.getEncoding() == ASCIIEncoding.INSTANCE)) {
                return RopeConstants.EMPTY_ASCII_8BIT_ROPE;
            }

            return withEncodingNode.executeWithEncoding(RopeConstants.EMPTY_ASCII_8BIT_ROPE, base.getEncoding(), CR_7BIT);
        }

        @Specialization(guards = "byteLength == 1")
        public Rope substringOneByte(Rope base, int offset, int byteLength,
                                        @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                        @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                        @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit,
                                        @Cached("create()") GetByteNode getByteNode) {
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

        @Specialization(guards = { "byteLength > 1", "sameAsBase(base, byteLength)" })
        public Rope substringSameAsBase(Rope base, int offset, int byteLength) {
            return base;
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, byteLength)" })
        public Rope substringLeafRope(LeafRope base, int offset, int byteLength,
                                  @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                  @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, byteLength)" })
        public Rope substringSubstringRope(SubstringRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base.getChild(), offset + base.getOffset(), byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, byteLength)" })
        public Rope substringRepeatingRope(RepeatingRope base, int offset, int byteLength,
                                          @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile matchesChildProfile) {
            final boolean offsetFitsChild = offset % base.getChild().byteLength() == 0;
            final boolean byteLengthFitsChild = byteLength == base.getChild().byteLength();

            // TODO (nirvdrum 07-Apr-16) We can specialize any number of children that fit perfectly into the length, not just count == 1. But we may need to create a new RepeatingNode to handle count > 1.
            if (matchesChildProfile.profile(offsetFitsChild && byteLengthFitsChild)) {
                return base.getChild();
            }

            return makeSubstring(base, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, byteLength)" })
        public Rope substringLazyRope(LazyRope base, int offset, int byteLength,
                                           @Cached("createBinaryProfile()") ConditionProfile is7BitProfile,
                                           @Cached("createBinaryProfile()") ConditionProfile isBinaryStringProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile, isBinaryStringProfile);
        }

        @Specialization(guards = { "byteLength > 1", "!sameAsBase(base, byteLength)" })
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
                    return new AsciiOnlyLeafRope(RopeOperations.extractRange(base, offset, byteLength), base.getEncoding());
                }
            }

            return makeSubstringNon7Bit(base, offset, byteLength);
        }

        @TruffleBoundary
        private Rope makeSubstringNon7Bit(Rope base, int offset, int byteLength) {
            final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
            final CodeRange codeRange = CodeRange.fromInt(StringSupport.unpackArg(packedLengthAndCodeRange));
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);
            final boolean singleByteOptimizable = base.isSingleByteOptimizable() || (codeRange == CR_7BIT);

            /*
            if (base.depth() >= 10) {
                System.out.println("SubstringRope depth: " + (base.depth() + 1));
            }
            */

            if (getContext().getOptions().ROPE_LAZY_SUBSTRINGS) {
                return new SubstringRope(base, singleByteOptimizable, offset, byteLength, characterLength, codeRange);
            } else {
                if (makeLeafRopeNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null));
                }

                final byte[] bytes = RopeOperations.extractRange(base, offset, byteLength);

                return makeLeafRopeNode.executeMake(bytes, base.getEncoding(), codeRange, characterLength);
            }
        }

        protected static boolean sameAsBase(Rope base, int byteLength) {
            // A SubstringRope's byte length is not allowed to be larger than its child. Thus, if it has the same
            // byte length as its child, it must be logically equivalent to the child.
            return byteLength == base.byteLength();
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "left"),
            @NodeChild(type = RubyNode.class, value = "right"),
            @NodeChild(type = RubyNode.class, value = "encoding")
    })
    public abstract static class MakeConcatNode extends RubyNode {

        public abstract Rope executeMake(Rope left, Rope right, Encoding encoding);

        @Specialization(guards = "isMutableRope(left)")
        public Rope concatMutableRope(RopeBuffer left, Rope right, Encoding encoding,
                                      @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile) {
            try {
                Math.addExact(left.byteLength(), right.byteLength());
            } catch (ArithmeticException e) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError("Result of string concatenation exceeds the system maximum string length", this));
            }

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
            try {
                Math.addExact(left.byteLength(), right.byteLength());
            } catch (ArithmeticException e) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError("Result of string concatenation exceeds the system maximum string length", this));
            }

            int depth = depth(left, right);
            /*if (depth >= 10) {
                System.out.println("ConcatRope depth: " + depth);
            }*/

            return new ConcatRope(left, right, encoding,
                    commonCodeRange(left.getCodeRange(), right.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile),
                    isSingleByteOptimizable(left, right, isLeftSingleByteOptimizableProfile),
                    depth);
        }

        public static CodeRange commonCodeRange(CodeRange first, CodeRange second,
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
            return rope instanceof RopeBuffer;
        }
    }


    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "bytes"),
            @NodeChild(type = RubyNode.class, value = "encoding"),
            @NodeChild(type = RubyNode.class, value = "codeRange"),
            @NodeChild(type = RubyNode.class, value = "characterLength")
    })
    public abstract static class MakeLeafRopeNode extends RubyNode {

        public static MakeLeafRopeNode create() {
            return RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
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

        @TruffleBoundary
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
                    throw new UnsupportedOperationException("Code range is reported as valid, but is invalid for the given encoding: " + encoding.toString());
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

        @TruffleBoundary
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
            @NodeChild(type = RubyNode.class, value = "base"),
            @NodeChild(type = RubyNode.class, value = "times")
    })
    @ImportStatic(RopeGuards.class)
    public abstract static class MakeRepeatingNode extends RubyNode {

        public static MakeRepeatingNode create() {
            return RopeNodesFactory.MakeRepeatingNodeGen.create(null, null);
        }

        public abstract Rope executeMake(Rope base, int times);

        @Specialization(guards = "times == 0")
        public Rope repeatZero(Rope base, int times,
                               @Cached("create()") WithEncodingNode withEncodingNode) {
            return withEncodingNode.executeWithEncoding(RopeConstants.EMPTY_UTF8_ROPE, base.getEncoding(), CodeRange.CR_7BIT);
        }

        @Specialization(guards = "times == 1")
        public Rope repeatOne(Rope base, int times,
                              @Cached("create()") WithEncodingNode withEncodingNode) {
            return base;
        }

        @Specialization(guards = "times > 1")
        public Rope multiplyBuffer(RopeBuffer base, int times) {
            final ByteList inputBytes = base.getByteList();
            int len = inputBytes.realSize() * times;
            final ByteList outputBytes = new ByteList(len);
            outputBytes.realSize(len);

            int n = inputBytes.realSize();

            System.arraycopy(inputBytes.unsafeBytes(), inputBytes.begin(), outputBytes.unsafeBytes(), 0, n);
            while (n <= len / 2) {
                System.arraycopy(outputBytes.unsafeBytes(), 0, outputBytes.unsafeBytes(), n, n);
                n *= 2;
            }
            System.arraycopy(outputBytes.unsafeBytes(), 0, outputBytes.unsafeBytes(), n, len - n);


            outputBytes.setEncoding(inputBytes.getEncoding());

            return new RopeBuffer(outputBytes, base.getCodeRange(), base.isSingleByteOptimizable(), base.characterLength() * times);
        }

        @Specialization(guards = { "!isRopeBuffer(base)", "isSingleByteString(base)", "times > 1" })
        @TruffleBoundary
        public Rope multiplySingleByteString(Rope base, int times,
                                             @Cached("create()") MakeLeafRopeNode makeLeafRopeNode) {
            final byte filler = base.getBytes()[0];

            byte[] buffer = new byte[times];
            Arrays.fill(buffer, filler);

            return makeLeafRopeNode.executeMake(buffer, base.getEncoding(), base.getCodeRange(), times);
        }

        @Specialization(guards = { "!isRopeBuffer(base)", "!isSingleByteString(base)", "times > 1" })
        public Rope repeat(Rope base, int times) {
            try {
                Math.multiplyExact(base.byteLength(), times);
            } catch (ArithmeticException e) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError("Result of repeating string exceeds the system maximum string length", this));
            }

            return new RepeatingRope(base, times);
        }

    }


    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope"),
            @NodeChild(type = RubyNode.class, value = "currentLevel"),
            @NodeChild(type = RubyNode.class, value = "printString")
    })
    public abstract static class DebugPrintRopeNode extends RubyNode {

        public abstract DynamicObject executeDebugPrint(Rope rope, int currentLevel, boolean printString);

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintLeafRope(LeafRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(StringUtils.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; D: %d)",
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

            System.err.println(StringUtils.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; O: %d; D: %d)",
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

            System.err.println(StringUtils.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; D: %d; LD: %d; RD: %d)",
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

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintRepeatingRope(RepeatingRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(StringUtils.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; T: %d; D: %d)",
                    printString ? rope.toString() : "<skipped>",
                    rope.getClass().getSimpleName(),
                    bytesAreNull,
                    rope.byteLength(),
                    rope.characterLength(),
                    rope.getCodeRange(),
                    rope.getTimes(),
                    rope.depth()));

            executeDebugPrint(rope.getChild(), currentLevel + 1, printString);

            return nil();
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject debugPrintLazyInt(LazyIntRope rope, int currentLevel, boolean printString) {
            printPreamble(currentLevel);

            // Converting a rope to a java.lang.String may populate the byte[], so we need to query for the array status beforehand.
            final boolean bytesAreNull = rope.getRawBytes() == null;

            System.err.println(StringUtils.format("%s (%s; BN: %b; BL: %d; CL: %d; CR: %s; V: %d, D: %d)",
                    printString ? rope.toString() : "<skipped>",
                    rope.getClass().getSimpleName(),
                    bytesAreNull,
                    rope.byteLength(),
                    rope.characterLength(),
                    rope.getCodeRange(),
                    rope.getValue(),
                    rope.depth()));

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

        public static WithEncodingNode create() {
            return RopeNodesFactory.WithEncodingNodeGen.create(null, null, null);
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
                "isAsciiCompatibleChange(rope, encoding)",
                "rope.getClass() == cachedRopeClass"
        }, limit = "getCacheLimit()")
        public Rope withEncodingCr7Bit(Rope rope, Encoding encoding, CodeRange codeRange,
                                       @Cached("rope.getClass()") Class<? extends Rope> cachedRopeClass) {
            return cachedRopeClass.cast(rope).withEncoding(encoding, CodeRange.CR_7BIT);
        }

        @Specialization(guards = {
                "rope.getEncoding() != encoding",
                "rope.getCodeRange() != codeRange",
                "!isAsciiCompatibleChange(rope, encoding)"
        })
        public Rope withEncoding(Rope rope, Encoding encoding, CodeRange codeRange,
                                 @Cached("create()") MakeLeafRopeNode makeLeafRopeNode) {
            return makeLeafRopeNode.executeMake(rope.getBytes(), encoding, codeRange, NotProvided.INSTANCE);
        }

        protected static boolean isAsciiCompatibleChange(Rope rope, Encoding encoding) {
            return rope.getCodeRange() == CR_7BIT && encoding.isAsciiCompatible();
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

        public static GetByteNode create() {
            return RopeNodesFactory.GetByteNodeGen.create(null, null);
        }

        public abstract int executeGetByte(Rope rope, int index);

        @Specialization(guards = "rope.getRawBytes() != null")
        public int getByte(Rope rope, int index) {
            return rope.getRawBytes()[index] & 0xff;
        }

        @Specialization(guards = "rope.getRawBytes() == null")
        public int getByte(LazyRope rope, int index) {
            return rope.getBytes()[index] & 0xff;
        }

        @Specialization(guards = "rope.getRawBytes() == null")
        public int getByteSubstringRope(SubstringRope rope, int index,
                                        @Cached("createBinaryProfile()") ConditionProfile childRawBytesNullProfile) {
            if (childRawBytesNullProfile.profile(rope.getChild().getRawBytes() == null)) {
                return rope.getByteSlow(index) & 0xff;
            }

            return rope.getChild().getRawBytes()[index + rope.getOffset()] & 0xff;
        }

        @Specialization(guards = "rope.getRawBytes() == null")
        public int getByteRepeatingRope(RepeatingRope rope, int index,
                                        @Cached("createBinaryProfile()") ConditionProfile childRawBytesNullProfile) {
            if (childRawBytesNullProfile.profile(rope.getChild().getRawBytes() == null)) {
                return rope.getByteSlow(index) & 0xff;
            }

            return rope.getChild().getRawBytes()[index % rope.getChild().byteLength()] & 0xff;
        }

        @Specialization(guards = "rope.getRawBytes() == null")
        public int getByteConcatRope(ConcatRope rope, int index,
                                     @Cached("createBinaryProfile()") ConditionProfile chooseLeftChildProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile leftChildRawBytesNullProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile rightChildRawBytesNullProfile) {
            if (chooseLeftChildProfile.profile(index < rope.getLeft().byteLength())) {
                if (leftChildRawBytesNullProfile.profile(rope.getLeft().getRawBytes() == null)) {
                    return rope.getLeft().getByteSlow(index) & 0xff;
                }

                return rope.getLeft().getRawBytes()[index] & 0xff;
            }

            if (rightChildRawBytesNullProfile.profile(rope.getRight().getRawBytes() == null)) {
                return rope.getRight().getByteSlow(index - rope.getLeft().byteLength()) & 0xff;
            }

            return rope.getRight().getRawBytes()[index - rope.getLeft().byteLength()] & 0xff;
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope"),
            @NodeChild(type = RubyNode.class, value = "index")
    })
    public abstract static class GetCodePointNode extends RubyNode {

        public static GetCodePointNode create() {
            return RopeNodesFactory.GetCodePointNodeGen.create(null, null);
        }

        public abstract int executeGetCodePoint(Rope rope, int index);

        @Specialization(guards = "rope.isSingleByteOptimizable()")
        public int getCodePointSingleByte(Rope rope, int index,
                                          @Cached("create()") GetByteNode getByteNode) {
            return getByteNode.executeGetByte(rope, index);
        }

        @Specialization(guards = { "!rope.isSingleByteOptimizable()", "rope.getEncoding().isUTF8()" })
        public int getCodePointUTF8(Rope rope, int index,
                                    @Cached("create()") GetByteNode getByteNode,
                                    @Cached("createBinaryProfile()") ConditionProfile singleByteCharProfile,
                                    @Cached("create()") BranchProfile errorProfile) {
            final int firstByte = getByteNode.executeGetByte(rope, index);
            if (singleByteCharProfile.profile(firstByte < 128)) {
                return firstByte;
            }

            return getCodePointMultiByte(rope, index, errorProfile);
        }

        @Specialization(guards = { "!rope.isSingleByteOptimizable()", "!rope.getEncoding().isUTF8()" })
        public int getCodePointMultiByte(Rope rope, int index,
                                         @Cached("create()") BranchProfile errorProfile) {
            final byte[] bytes = rope.getBytes();
            final Encoding encoding = rope.getEncoding();

            final int characterLength = preciseCharacterLength(encoding, bytes, index, rope.byteLength());
            if (characterLength <= 0) {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().argumentError("invalid byte sequence in " + encoding, null));
            }

            return mbcToCode(encoding, bytes, index, rope.byteLength());
        }

        @TruffleBoundary
        private int preciseCharacterLength(Encoding encoding, byte[] bytes, int start, int end) {
            return StringSupport.preciseLength(encoding, bytes, start, end);
        }

        @TruffleBoundary
        private int mbcToCode(Encoding encoding, byte[] bytes, int start, int end) {
            return encoding.mbcToCode(bytes, start, end);
        }

    }

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rope")
    })
    @ImportStatic(RopeGuards.class)
    public abstract static class FlattenNode extends RubyNode {

        @Child private MakeLeafRopeNode makeLeafRopeNode;

        public static FlattenNode create() {
            return RopeNodesFactory.FlattenNodeGen.create(null, null, null);
        }

        public FlattenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = MakeLeafRopeNode.create();
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

    }

}
