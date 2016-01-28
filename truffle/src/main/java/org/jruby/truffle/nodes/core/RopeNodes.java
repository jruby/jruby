/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.core;

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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.rope.AsciiOnlyLeafRope;
import org.jruby.truffle.runtime.rope.ConcatRope;
import org.jruby.truffle.runtime.rope.InvalidLeafRope;
import org.jruby.truffle.runtime.rope.LeafRope;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;
import org.jruby.truffle.runtime.rope.SubstringRope;
import org.jruby.truffle.runtime.rope.ValidLeafRope;
import org.jruby.util.StringSupport;

public abstract class RopeNodes {

    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "base"),
            @NodeChild(type = RubyNode.class, value = "offset"),
            @NodeChild(type = RubyNode.class, value = "byteLength")
    })
    public abstract static class MakeSubstringNode extends RubyNode {

        public MakeSubstringNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Rope executeMake(Rope base, int offset, int byteLength);

        @Specialization(guards = "byteLength == 0")
        public Rope substringZeroLength(Rope base, int offset, int byteLength) {
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

            return executeMake(root, offset, byteLength);
        }

        private Rope makeSubstring(Rope base, int offset, int byteLength, ConditionProfile is7BitProfile) {
            if (is7BitProfile.profile(base.getCodeRange() == StringSupport.CR_7BIT)) {
                return new SubstringRope(base, offset, byteLength, byteLength, StringSupport.CR_7BIT);
            }

            return makeSubstringNon7Bit(base, offset, byteLength);
        }

        @TruffleBoundary
        private Rope makeSubstringNon7Bit(Rope base, int offset, int byteLength) {
            final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
            final int codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
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

            final int codeRange = commonCodeRange(left.getCodeRange(), right.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile);

            return makeLeafRopeNode.executeMake(bytes, encoding, codeRange);
        }

        @Specialization(guards = { "!right.isEmpty()", "left.byteLength() >= SHORT_LEAF_BYTESIZE_THRESHOLD", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatLeavesGeneral(LeafRope left, LeafRope right, Encoding encoding,
                                 @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile leftDepthGreaterThanRightProfile) {
            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile, leftDepthGreaterThanRightProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatWithReduce(ConcatRope left, LeafRope right, Encoding encoding,
                                     @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile leftDepthGreaterThanRightProfile) {

            if ((left.getRight().byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD) && (left.getRight() instanceof LeafRope)) {
                final Rope compacted = concatLeaves((LeafRope) left.getRight(), right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile);
                return concat(left.getLeft(), compacted, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile, leftDepthGreaterThanRightProfile);
            }

            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile, leftDepthGreaterThanRightProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()", "right.byteLength() < SHORT_LEAF_BYTESIZE_THRESHOLD" })
        public Rope concatSubstringLeaf(SubstringRope left, LeafRope right, Encoding encoding,
                                     @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile,
                                     @Cached("createBinaryProfile()") ConditionProfile leftDepthGreaterThanRightProfile) {
            return concat(left, right, encoding, sameCodeRangeProfile, brokenCodeRangeProfile, isLeftSingleByteOptimizableProfile, leftDepthGreaterThanRightProfile);
        }

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()" })
        public Rope concat(Rope left, Rope right, Encoding encoding,
                           @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                           @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                           @Cached("createBinaryProfile()") ConditionProfile isLeftSingleByteOptimizableProfile,
                           @Cached("createBinaryProfile()") ConditionProfile leftDepthGreaterThanRightProfile) {
            int depth = depth(left, right, leftDepthGreaterThanRightProfile);
            /*if (depth >= 10) {
                System.out.println("ConcatRope depth: " + depth);
            }*/

            return new ConcatRope(left, right, encoding,
                    commonCodeRange(left.getCodeRange(), right.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile),
                    isSingleByteOptimizable(left, right, isLeftSingleByteOptimizableProfile),
                    depth);
        }

        private int commonCodeRange(int first, int second,
                                    ConditionProfile sameCodeRangeProfile,
                                    ConditionProfile brokenCodeRangeProfile) {
            if (sameCodeRangeProfile.profile(first == second)) {
                return first;
            }

            if (brokenCodeRangeProfile.profile((first == StringSupport.CR_BROKEN) || (second == StringSupport.CR_BROKEN))) {
                return StringSupport.CR_BROKEN;
            }

            // If we get this far, one must be CR_7BIT and the other must be CR_VALID, so promote to the more general code range.
            return StringSupport.CR_VALID;
        }

        private boolean isSingleByteOptimizable(Rope left, Rope right, ConditionProfile isLeftSingleByteOptimizableProfile) {
            if (isLeftSingleByteOptimizableProfile.profile(left.isSingleByteOptimizable())) {
                return right.isSingleByteOptimizable();
            }

            return false;
        }

        private int depth(Rope left, Rope right, ConditionProfile leftDepthGreaterThanRightProfile) {
            if (leftDepthGreaterThanRightProfile.profile(left.depth() >= right.depth())) {
                return left.depth() + 1;
            }

            return right.depth() + 1;
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

        public abstract LeafRope executeMake(byte[] bytes, Encoding encoding, int codeRange);

        @Specialization(guards = "is7Bit(codeRange)")
        public LeafRope makeAsciiOnlyLeafRope(byte[] bytes, Encoding encoding, int codeRange) {
            return new AsciiOnlyLeafRope(bytes, encoding);
        }

        @Specialization(guards = "isValid(codeRange)")
        public LeafRope makeValidLeafRope(byte[] bytes, Encoding encoding, int codeRange) {
            final int characterLength = RopeOperations.strLength(encoding, bytes, 0, bytes.length);

            return new ValidLeafRope(bytes, encoding, characterLength);
        }

        @Specialization(guards = "isBroken(codeRange)")
        public LeafRope makeInvalidLeafRope(byte[] bytes, Encoding encoding, int codeRange) {
            return new InvalidLeafRope(bytes, encoding);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "isBinaryString(encoding)" })
        public LeafRope makeUnknownLeafRopeBinary(byte[] bytes, Encoding encoding, int codeRange,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile) {
            int newCodeRange = StringSupport.CR_7BIT;
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] < 0) {
                    newCodeRange = StringSupport.CR_VALID;
                    break;
                }
            }

            if (discovered7BitProfile.profile(newCodeRange == StringSupport.CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            return new ValidLeafRope(bytes, encoding, bytes.length);
        }

        @Specialization(guards = { "isUnknown(codeRange)", "!isBinaryString(encoding)" })
        public LeafRope makeUnknownLeafRope(byte[] bytes, Encoding encoding, int codeRange,
                                            @Cached("createBinaryProfile()") ConditionProfile discovered7BitProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile discoveredValidProfile) {
            final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(encoding, bytes, 0, bytes.length);
            final int newCodeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            if (discovered7BitProfile.profile(newCodeRange == StringSupport.CR_7BIT)) {
                return new AsciiOnlyLeafRope(bytes, encoding);
            }

            if (discoveredValidProfile.profile(newCodeRange == StringSupport.CR_VALID)) {
                return new ValidLeafRope(bytes, encoding, characterLength);
            }

            return new InvalidLeafRope(bytes, encoding);
        }

        protected static boolean is7Bit(int codeRange) {
            return codeRange == StringSupport.CR_7BIT;
        }

        protected static boolean isValid(int codeRange) {
            return codeRange == StringSupport.CR_VALID;
        }

        protected static boolean isBroken(int codeRange) {
            return codeRange == StringSupport.CR_BROKEN;
        }

        protected static boolean isUnknown(int codeRange) {
            return codeRange == StringSupport.CR_UNKNOWN;
        }

        protected static boolean isBinaryString(Encoding encoding) {
            return encoding == ASCIIEncoding.INSTANCE;
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
                    StringSupport.codeRangeAsString(rope.getCodeRange()),
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
                    StringSupport.codeRangeAsString(rope.getCodeRange()),
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
                    StringSupport.codeRangeAsString(rope.getCodeRange()),
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
