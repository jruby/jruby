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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.rope.ConcatRope;
import org.jruby.truffle.runtime.rope.LeafRope;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;
import org.jruby.truffle.runtime.rope.SubstringRope;
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

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)", "isLeafRope(base)" })
        public Rope substringLeafRope(LeafRope base, int offset, int byteLength,
                                  @Cached("createBinaryProfile()") ConditionProfile is7BitProfile) {
            return makeSubstring(base, offset, byteLength, is7BitProfile);
        }

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)", "isSubstringRope(base)" })
        public Rope substringSubstringRope(SubstringRope base, int offset, int byteLength,
                                      @Cached("createBinaryProfile()") ConditionProfile is7BitProfile) {
            return makeSubstring(base.getChild(), offset + base.getOffset(), byteLength, is7BitProfile);
        }

        @Specialization(guards = { "byteLength > 0", "!sameAsBase(base, offset, byteLength)", "isConcatRope(base)" })
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

        @CompilerDirectives.TruffleBoundary
        private Rope makeSubstringNon7Bit(Rope base, int offset, int byteLength) {
            final long packedLengthAndCodeRange = RopeOperations.calculateCodeRangeAndLength(base.getEncoding(), base.getBytes(), offset, offset + byteLength);
            final int codeRange = StringSupport.unpackArg(packedLengthAndCodeRange);
            final int characterLength = StringSupport.unpackResult(packedLengthAndCodeRange);

            return new SubstringRope(base, offset, byteLength, characterLength, codeRange);
        }

        protected static boolean sameAsBase(Rope base, int offset, int byteLength) {
            return (byteLength - offset) == base.byteLength();
        }

        protected static boolean is7Bit(Rope base) {
            return base.getCodeRange() == StringSupport.CR_7BIT;
        }

        protected static boolean isLeafRope(Rope rope) {
            return (rope instanceof LeafRope);
        }

        protected static boolean isSubstringRope(Rope rope) {
            return (rope instanceof SubstringRope);
        }

        protected static boolean isConcatRope(Rope rope) {
            return (rope instanceof ConcatRope);
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

        @Specialization(guards = { "!left.isEmpty()", "!right.isEmpty()" })
        public Rope concat(Rope left, Rope right, Encoding encoding) {
            return new ConcatRope(left, right, encoding);
        }

    }
}
