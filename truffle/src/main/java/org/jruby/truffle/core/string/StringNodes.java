/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Contains code modified from JRuby's RubyString.java
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.RubiniusOnly;
import org.jruby.truffle.core.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayCoreMethodNode;
import org.jruby.truffle.core.cast.CmpIntNode;
import org.jruby.truffle.core.cast.CmpIntNodeGen;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.format.FormatExceptionTranslator;
import org.jruby.truffle.core.format.exceptions.FormatException;
import org.jruby.truffle.core.format.unpack.ArrayResult;
import org.jruby.truffle.core.format.unpack.UnpackCompiler;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.RopeBuffer;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodes.MakeRepeatingNode;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.rubinius.StringPrimitiveNodes;
import org.jruby.truffle.core.rubinius.StringPrimitiveNodesFactory;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.ConvertDouble;
import org.jruby.util.StringSupport;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.jruby.truffle.core.rope.RopeConstants.EMPTY_ASCII_8BIT_ROPE;
import static org.jruby.truffle.core.rope.RopeConstants.EMPTY_UTF8_ROPE;
import static org.jruby.truffle.core.string.StringOperations.encoding;
import static org.jruby.truffle.core.string.StringOperations.rope;

@CoreClass(name = "String")
public abstract class StringNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, EMPTY_ASCII_8BIT_ROPE, null);
        }

    }

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class AddNode extends CoreMethodNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private TaintResultNode taintResultNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            taintResultNode = new TaintResultNode(getContext(), getSourceSection());
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject add(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding enc = StringOperations.checkEncoding(getContext(), string, other, this);

            final Rope concatRope = makeConcatNode.executeMake(left, right, enc);

            final DynamicObject ret = Layouts.STRING.createString(coreLibrary().getStringFactory(), concatRope);

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private ToIntNode toIntNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        public abstract DynamicObject executeInt(VirtualFrame frame, DynamicObject string, int times);

        @Specialization(guards = "times < 0")
        @TruffleBoundary
        public DynamicObject multiplyTimesNegative(DynamicObject string, int times) {
            throw new RaiseException(coreLibrary().argumentError("negative argument", this));
        }

        @Specialization(guards = "times >= 0")
        public DynamicObject multiply(DynamicObject string, int times,
                                      @Cached("create(getContext(), getSourceSection())") MakeRepeatingNode makeRepeatingNode) {
            final Rope repeated = makeRepeatingNode.executeMake(rope(string), times);

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), repeated, null);
        }

        @Specialization(guards = "isRubyBignum(times)")
        @TruffleBoundary
        public DynamicObject multiply(DynamicObject string, DynamicObject times) {
            throw new RaiseException(coreLibrary().rangeError("bignum too big to convert into `long'", this));
        }

        @Specialization(guards = { "!isRubyBignum(times)", "!isInteger(times)" })
        public DynamicObject multiply(VirtualFrame frame, DynamicObject string, Object times) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return executeInt(frame, string, toIntNode.doInt(frame, times));
        }

        protected static boolean isSingleByteString(DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            return rope(string).byteLength() == 1;
        }

    }

    @CoreMethod(names = {"==", "===", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private StringPrimitiveNodes.StringEqualPrimitiveNode stringEqualNode;
        @Child private KernelNodes.RespondToNode respondToNode;
        @Child private CallDispatchHeadNode objectEqualNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            stringEqualNode = StringPrimitiveNodesFactory.StringEqualPrimitiveNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        @Specialization(guards = "isRubyString(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return stringEqualNode.executeStringEqual(a, b);
        }

        @Specialization(guards = "!isRubyString(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, Object b) {
            if (respondToNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            if (respondToNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (objectEqualNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    objectEqualNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                return objectEqualNode.callBoolean(frame, b, "==", null, a);
            }

            return false;
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode cmpNode;
        @Child private CmpIntNode cmpIntNode;
        @Child private KernelNodes.RespondToNode respondToCmpNode;
        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToStrNode toStrNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyString(b)")
        public int compare(DynamicObject a, DynamicObject b) {
            // Taken from org.jruby.RubyString#op_cmp

            final Rope firstRope = rope(a);
            final Rope secondRope = rope(b);

            final int ret = RopeOperations.cmp(firstRope, secondRope);

            if ((ret == 0) && !RopeOperations.areComparable(firstRope, secondRope)) {
                return firstRope.getEncoding().getIndex() > secondRope.getEncoding().getIndex() ? 1 : -1;
            }

            return ret;
        }

        @Specialization(guards = "!isRubyString(b)")
        public Object compare(VirtualFrame frame, DynamicObject a, Object b) {
            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            if (respondToToStrNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
                }

                try {
                    final DynamicObject coerced = toStrNode.executeToStr(frame, b);

                    return compare(a, coerced);
                } catch (RaiseException e) {
                    if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getTypeErrorClass()) {
                        return nil();
                    } else {
                        throw e;
                    }
                }
            }

            if (respondToCmpNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToCmpNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            if (respondToCmpNode.doesRespondToString(frame, b, create7BitString("<=>", UTF8Encoding.INSTANCE), false)) {
                if (cmpNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    cmpNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                final Object cmpResult = cmpNode.call(frame, b, "<=>", null, a);

                if (cmpResult == nil()) {
                    return nil();
                }

                if (cmpIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    cmpIntNode = insert(CmpIntNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }

                return -(cmpIntNode.executeCmpInt(frame, cmpResult, a, b));
            }

            return nil();
        }
    }

    @CoreMethod(names = { "<<", "concat" }, required = 1, taintFromParameter = 0, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "other")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class ConcatNode extends CoreMethodNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private StringPrimitiveNodes.StringAppendPrimitiveNode stringAppendNode;

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyString(other)", "is7Bit(string)", "is7Bit(other)" })
        public DynamicObject concatStringSingleByte(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            if (makeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                makeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            StringOperations.setRope(string, makeConcatNode.executeMake(left, right, left.getEncoding()));

            return string;
        }

        @Specialization(guards =  { "isRubyString(other)", "!is7Bit(string) || !is7Bit(other)" })
        public Object concatString(DynamicObject string, DynamicObject other) {
            if (stringAppendNode == null) {
                CompilerDirectives.transferToInterpreter();
                stringAppendNode = insert(StringPrimitiveNodesFactory.StringAppendPrimitiveNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
            }

            return stringAppendNode.executeStringAppend(string, other);
        }

        @Specialization(guards = "!isRubyString(other)")
        public Object concat(DynamicObject string, Object other) {
            return ruby("string.concat_internal(other)", "string", string, "other", other);
        }
    }

    @CoreMethod(names = {"[]", "slice"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1}, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private StringPrimitiveNodes.StringSubstringPrimitiveNode substringNode;
        @Child private AllocateObjectNode allocateObjectNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "wasNotProvided(length) || isRubiniusUndefined(length)")
        public Object getIndex(VirtualFrame frame, DynamicObject string, int index, Object length) {
            final Rope rope = rope(string);
            final int stringLength = rope.characterLength();
            int normalizedIndex = StringOperations.normalizeIndex(stringLength, index);

            if (normalizedIndex < 0 || normalizedIndex >= rope.byteLength()) {
                outOfBounds.enter();
                return nil();
            } else {
                return getSubstringNode().execute(frame, string, index, 1);
            }
        }

        @Specialization(guards = { "!isRubyRange(index)", "!isRubyRegexp(index)", "!isRubyString(index)", "wasNotProvided(length) || isRubiniusUndefined(length)" })
        public Object getIndex(VirtualFrame frame, DynamicObject string, Object index, Object length) {
            return getIndex(frame, string, getToIntNode().doInt(frame, index), length);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)"})
        public Object sliceIntegerRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length) {
            return sliceRange(frame, string, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range), Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)"})
        public Object sliceLongRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values should be properly lowered, only if possible.
            return sliceRange(frame, string, (int) Layouts.LONG_FIXNUM_RANGE.getBegin(range), (int) Layouts.LONG_FIXNUM_RANGE.getEnd(range), Layouts.LONG_FIXNUM_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = {"isObjectRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)"})
        public Object sliceObjectRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values may return Fixnums beyond int boundaries and we should handle that -- Bignums are always errors.
            final int coercedBegin = getToIntNode().doInt(frame, Layouts.OBJECT_RANGE.getBegin(range));
            final int coercedEnd = getToIntNode().doInt(frame, Layouts.OBJECT_RANGE.getEnd(range));

            return sliceRange(frame, string, coercedBegin, coercedEnd, Layouts.OBJECT_RANGE.getExcludedEnd(range));
        }

        private Object sliceRange(VirtualFrame frame, DynamicObject string, int begin, int end, boolean doesExcludeEnd) {
            assert RubyGuards.isRubyString(string);

            final int stringLength = rope(string).characterLength();
            begin = StringOperations.normalizeIndex(stringLength, begin);

            if (begin < 0 || begin > stringLength) {
                outOfBounds.enter();
                return nil();
            } else {

                if (begin == stringLength) {
                    final ByteList byteList = new ByteList();
                    byteList.setEncoding(encoding(string));
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.withEncodingVerySlow(RopeConstants.EMPTY_ASCII_8BIT_ROPE, encoding(string)), null);
                }

                end = StringOperations.normalizeIndex(stringLength, end);
                int length = StringOperations.clampExclusiveIndex(string, doesExcludeEnd ? end : end + 1);

                if (length > stringLength) {
                    length = stringLength;
                }

                length -= begin;

                if (length < 0) {
                    length = 0;
                }

                return getSubstringNode().execute(frame, string, begin, length);
            }
        }

        @Specialization
        public Object slice(VirtualFrame frame, DynamicObject string, int start, int length) {
            return getSubstringNode().execute(frame, string, start, length);
        }

        @Specialization(guards = "wasProvided(length)")
        public Object slice(VirtualFrame frame, DynamicObject string, int start, Object length) {
            return slice(frame, string, start, getToIntNode().doInt(frame, length));
        }

        @Specialization(guards = { "!isRubyRange(start)", "!isRubyRegexp(start)", "!isRubyString(start)", "wasProvided(length)" })
        public Object slice(VirtualFrame frame, DynamicObject string, Object start, Object length) {
            return slice(frame, string, getToIntNode().doInt(frame, start), getToIntNode().doInt(frame, length));
        }

        @Specialization(guards = {"isRubyRegexp(regexp)", "wasNotProvided(capture) || isRubiniusUndefined(capture)"})
        public Object slice1(VirtualFrame frame, DynamicObject string, DynamicObject regexp, Object capture) {
            return sliceCapture(string, regexp, 0);
        }

        @Specialization(guards = {"isRubyRegexp(regexp)", "wasProvided(capture)"})
        public Object sliceCapture(DynamicObject string, DynamicObject regexp, Object capture) {
            // Extracted from Rubinius's definition of String#[].
            return ruby("match, str = subpattern(index, other); Regexp.last_match = match; str", "index", regexp, "other", capture);
        }

        @Specialization(guards = {"wasNotProvided(length) || isRubiniusUndefined(length)", "isRubyString(matchStr)"})
        public Object slice2(VirtualFrame frame, DynamicObject string, DynamicObject matchStr, Object length) {
            if (includeNode == null) {
                CompilerDirectives.transferToInterpreter();
                includeNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            boolean result = includeNode.callBoolean(frame, string, "include?", null, matchStr);

            if (result) {
                if (dupNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    dupNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                throw new TaintResultNode.DoNotTaint(dupNode.call(frame, matchStr, "dup", null));
            }

            return nil();
        }

        private ToIntNode getToIntNode() {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return toIntNode;
        }

        private StringPrimitiveNodes.StringSubstringPrimitiveNode getSubstringNode() {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreter();

                substringNode = insert(StringPrimitiveNodesFactory.StringSubstringPrimitiveNodeFactory.create(
                        getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            return substringNode;
        }

        protected boolean isRubiniusUndefined(Object object) {
            return object == coreLibrary().getRubiniusUndefined();
        }

    }

    @CoreMethod(names = "ascii_only?")
    @ImportStatic(StringGuards.class)
    public abstract static class ASCIIOnlyNode extends CoreMethodArrayArgumentsNode {

        public ASCIIOnlyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isAsciiCompatible(string)", "is7Bit(string)" })
        public boolean asciiOnlyAsciiCompatible7BitCR(DynamicObject string) {
            return true;
        }

        @Specialization(guards = { "isAsciiCompatible(string)", "!is7Bit(string)" })
        public boolean asciiOnlyAsciiCompatible(DynamicObject string) {
            return false;
        }

        @Specialization(guards = "!isAsciiCompatible(string)")
        public boolean asciiOnly(DynamicObject string) {
            return false;
        }

    }

    @CoreMethod(names = "b", taintFromSelf = true)
    public abstract static class BNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode;

        public BNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization
        public DynamicObject b(DynamicObject string) {
            final Rope newRope = withEncodingNode.executeWithEncoding(rope(string), ASCIIEncoding.INSTANCE, CodeRange.CR_UNKNOWN);

            return createString(newRope);
        }

    }

    @CoreMethod(names = "bytes")
    public abstract static class BytesNode extends CoreMethodArrayArgumentsNode {

        public BytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject bytes(DynamicObject string) {
            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();

            final int[] store = new int[bytes.length];

            for (int n = 0; n < store.length; n++) {
                store[n] = ((int) bytes[n]) & 0xFF;
            }

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

    }

    @CoreMethod(names = "bytesize")
    public abstract static class ByteSizeNode extends CoreMethodArrayArgumentsNode {

        public ByteSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int byteSize(DynamicObject string) {
            return rope(string).byteLength();
        }

    }

    @CoreMethod(names = "casecmp", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class CaseCmpNode extends CoreMethodNode {

        public CaseCmpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = {"isRubyString(other)", "bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmpSingleByte(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19.

            if (RopeOperations.areCompatible(rope(string), rope(other)) == null) {
                return nil();
            }

            return StringOperations.getByteListReadOnly(string).caseInsensitiveCmp(StringOperations.getByteListReadOnly(other));
        }

        @Specialization(guards = {"isRubyString(other)", "!bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmp(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19 and

            final Encoding encoding = RopeOperations.areCompatible(rope(string), rope(other));

            if (encoding == null) {
                return nil();
            }

            return multiByteCasecmp(encoding, StringOperations.getByteListReadOnly(string), StringOperations.getByteListReadOnly(other));
        }

        @TruffleBoundary
        private int multiByteCasecmp(Encoding enc, ByteList value, ByteList otherValue) {
            return StringSupport.multiByteCasecmp(enc, value, otherValue);
        }

        public static boolean bothSingleByteOptimizable(DynamicObject string, DynamicObject other) {
            assert RubyGuards.isRubyString(string);
            assert RubyGuards.isRubyString(other);

            return rope(string).isSingleByteOptimizable() && rope(other).isSingleByteOptimizable();
        }
    }

    @CoreMethod(names = "count", rest = true)
    @ImportStatic(StringGuards.class)
    public abstract static class CountNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public CountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization(guards = "isEmpty(string)")
        public int count(DynamicObject string, Object[] args) {
            return 0;
        }

        @Specialization(guards = "!isEmpty(string)")
        public int count(VirtualFrame frame, DynamicObject string, Object[] args) {
            if (args.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentErrorEmptyVarargs(this));
            }

            DynamicObject[] otherStrings = new DynamicObject[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeToStr(frame, args[i]);
            }

            return countSlow(string, otherStrings);
        }

        @TruffleBoundary
        private int countSlow(DynamicObject string, DynamicObject... otherStrings) {
            assert RubyGuards.isRubyString(string);

            DynamicObject otherStr = otherStrings[0];
            Encoding enc = encoding(otherStr);

            final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), getContext().getJRubyRuntime(), table, null, true, enc);
            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];

                assert RubyGuards.isRubyString(otherStr);

                enc = StringOperations.checkEncoding(getContext(), string, otherStr, this);
                tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), getContext().getJRubyRuntime(), table, tables, false, enc);
            }

            return StringSupport.strCount(StringOperations.getByteListReadOnly(string), getContext().getJRubyRuntime(), table, tables, enc);
        }
    }

    @CoreMethod(names = "crypt", required = 1, taintFromSelf = true, taintFromParameter = 0)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "salt")
    })
    public abstract static class CryptNode extends CoreMethodNode {

        @Child private TaintResultNode taintResultNode;

        public CryptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("salt") public RubyNode coerceSaltToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "isRubyString(salt)")
        @TruffleBoundary
        public Object crypt(DynamicObject string, DynamicObject salt) {
            // Taken from org.jruby.RubyString#crypt.

            final Rope value = rope(string);
            final Rope other = rope(salt);

            final Encoding ascii8bit = getContext().getJRubyRuntime().getEncodingService().getAscii8bitEncoding();
            if (other.byteLength() < 2) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final TrufflePosix posix = posix();
            final byte[] keyBytes = Arrays.copyOfRange(value.getBytes(), 0, value.byteLength());
            final byte[] saltBytes = Arrays.copyOfRange(other.getBytes(), 0, other.byteLength());

            if (saltBytes[0] == 0 || saltBytes[1] == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final byte[] cryptedString = posix.crypt(keyBytes, saltBytes);

            // We differ from MRI in that we do not process salt to make it work and we will
            // return any errors via errno.
            if (cryptedString == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().errnoError(posix.errno(), this));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            final DynamicObject ret = createString(StringOperations.ropeFromByteList(new ByteList(cryptedString, 0, cryptedString.length - 1, ascii8bit, false)));

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(salt, ret);

            return ret;
        }

    }

    @CoreMethod(names = "delete!", rest = true, raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DeleteBangNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public DeleteBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization(guards = "isEmpty(string)")
        public DynamicObject deleteBangEmpty(DynamicObject string, Object... args) {
            return nil();
        }

        @Specialization(guards = "!isEmpty(string)")
        public Object deleteBang(VirtualFrame frame, DynamicObject string, Object... args) {
            if (args.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentErrorEmptyVarargs(this));
            }

            DynamicObject[] otherStrings = new DynamicObject[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeToStr(frame, args[i]);
            }

            return deleteBangSlow(string, otherStrings);
        }

        @TruffleBoundary
        private Object deleteBangSlow(DynamicObject string, DynamicObject... otherStrings) {
            assert RubyGuards.isRubyString(string);

            DynamicObject otherString = otherStrings[0];
            Encoding enc = StringOperations.checkEncoding(getContext(), string, otherString, this);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherString),
                    getContext().getJRubyRuntime(),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                assert RubyGuards.isRubyString(otherStrings[i]);

                enc = StringOperations.checkEncoding(getContext(), string, otherStrings[i], this);
                tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStrings[i]), getContext().getJRubyRuntime(), squeeze, tables, false, enc);
            }

            final CodeRangeable buffer = StringOperations.getCodeRangeableReadWrite(string);
            if (StringSupport.delete_bangCommon19(buffer, getContext().getJRubyRuntime(), squeeze, tables, enc) == null) {
                return nil();
            }

            StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer.getByteList(), buffer.getCodeRange()));

            return string;
        }
    }

    @CoreMethod(names = "downcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DowncaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        @Specialization(guards = { "isEmpty(string)", "isSingleByteOptimizable(string)" })
        public DynamicObject downcaseSingleByteEmpty(DynamicObject string) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(string)", "isSingleByteOptimizable(string)" })
        public DynamicObject downcaseSingleByte(DynamicObject string,
                                                @Cached("createBinaryProfile()") ConditionProfile modifiedProfile) {
            final Rope rope = rope(string);
            final byte[] outputBytes = rope.getBytesCopy();

            final boolean modified = singleByteDowncase(outputBytes, 0, outputBytes.length);
            if (modifiedProfile.profile(modified)) {
                StringOperations.setRope(string, makeLeafRopeNode.executeMake(outputBytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                return string;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public DynamicObject downcase(DynamicObject string,
                                      @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile modifiedProfile) {
            final Rope rope = rope(string);
            final Encoding encoding = rope.getEncoding();

            if (encoding.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        coreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", encoding), this));
            }

            if (emptyStringProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final byte[] outputBytes = rope.getBytesCopy();

            try {
                final boolean modified = multiByteDowncase(encoding, outputBytes, 0, outputBytes.length);

                if (modifiedProfile.profile(modified)) {
                    StringOperations.setRope(string, makeLeafRopeNode.executeMake(outputBytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                    return string;
                } else {
                    return nil();
                }
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError(e.getMessage(), this));
            }
        }

        @TruffleBoundary
        private boolean singleByteDowncase(byte[] bytes, int s, int end) {
            return StringSupport.singleByteDowncase(bytes, s, end);
        }

        @TruffleBoundary
        private boolean multiByteDowncase(Encoding encoding, byte[] bytes, int s, int end) {
            return StringSupport.multiByteDowncase(encoding, bytes, s, end);
        }
    }

    @CoreMethod(names = "each_byte", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachByteNode extends YieldingCoreMethodNode {

        public EachByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject eachByte(VirtualFrame frame, DynamicObject string, DynamicObject block,
                                      @Cached("createBinaryProfile()") ConditionProfile ropeChangedProfile) {
            Rope rope = rope(string);
            byte[] bytes = rope.getBytes();

            for (int i = 0; i < bytes.length; i++) {
                yield(frame, block, bytes[i] & 0xff);

                Rope updatedRope = rope(string);
                if (ropeChangedProfile.profile(rope != updatedRope)) {
                    rope = updatedRope;
                    bytes = updatedRope.getBytes();
                }
            }

            return string;
        }

    }

    @CoreMethod(names = "each_char", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(StringGuards.class)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = "isValidOr7BitEncoding(string)")
        public DynamicObject eachChar(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            final Rope rope = rope(string);
            final byte[] ptrBytes = rope.getBytes();
            final int len = ptrBytes.length;
            final Encoding enc = rope.getEncoding();

            int n;

            for (int i = 0; i < len; i += n) {
                n = StringSupport.encFastMBCLen(ptrBytes, i, len, enc);

                yield(frame, block, substr(rope, string, i, n));
            }

            return string;
        }

        @Specialization(guards = "!isValidOr7BitEncoding(string)")
        public DynamicObject eachCharMultiByteEncoding(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            final Rope rope = rope(string);
            final byte[] ptrBytes = rope.getBytes();
            final int len = ptrBytes.length;
            final Encoding enc = rope.getEncoding();

            int n;

            for (int i = 0; i < len; i += n) {
                n = multiByteStringLength(enc, ptrBytes, i, len);

                yield(frame, block, substr(rope, string, i, n));
            }

            return string;
        }

        @TruffleBoundary
        private int multiByteStringLength(Encoding enc, byte[] bytes, int p, int end) {
            return StringSupport.length(enc, bytes, p, end);
        }

        // TODO (nirvdrum 10-Mar-15): This was extracted from JRuby, but likely will need to become a Rubinius primitive.
        // Don't be tempted to extract the rope from the passed string. If the block being yielded to modifies the
        // source string, you'll get a different rope. Unlike String#each_byte, String#each_char does not make
        // modifications to the string visible to the rest of the iteration.
        private Object substr(Rope rope, DynamicObject string, int beg, int len) {
            int length = rope.byteLength();
            if (len < 0 || beg > length) return nil();

            if (beg < 0) {
                beg += length;
                if (beg < 0) return nil();
            }

            int end = Math.min(length, beg + len);

            final Rope substringRope = makeSubstringNode.executeMake(rope, beg, end - beg);

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            // TODO (nirvdrum 08-Jan-16) For CR_7BIT, we should always be able set to CR_7BIT. CR_VALID is trickier because any one character could be 7-bit.
            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope, null);

            return taintResultNode.maybeTaint(string, ret);
        }
    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean empty(DynamicObject string) {
            return rope(string).isEmpty();
        }
    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject encoding(DynamicObject string) {
            return EncodingNodes.getEncoding(StringOperations.encoding(string));
        }

    }

    @CoreMethod(names = "force_encoding", required = 1, raiseIfFrozenSelf = true)
    public abstract static class ForceEncodingNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode;
        @Child private ToStrNode toStrNode;

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(context, sourceSection, null, null, null);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(encodingName)")
        public DynamicObject forceEncodingString(DynamicObject string, DynamicObject encodingName,
                                                 @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile,
                                                 @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
            final DynamicObject encoding = EncodingNodes.getEncoding(encodingName.toString());
            return forceEncodingEncoding(string, encoding, differentEncodingProfile, mutableRopeProfile);
        }

        @Specialization(guards = "isRubyEncoding(rubyEncoding)")
        public DynamicObject forceEncodingEncoding(DynamicObject string, DynamicObject rubyEncoding,
                                                   @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile,
                                                   @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope rope = rope(string);

            if (differentEncodingProfile.profile(rope.getEncoding() != encoding)) {
                if (mutableRopeProfile.profile(rope instanceof RopeBuffer)) {
                    ((RopeBuffer) rope).getByteList().setEncoding(encoding);
                } else {
                    final Rope newRope = withEncodingNode.executeWithEncoding(rope, encoding, CodeRange.CR_UNKNOWN);
                    StringOperations.setRope(string, newRope);
                }
            }

            return string;
        }

        @Specialization(guards = { "!isRubyString(encoding)", "!isRubyEncoding(encoding)" })
        public DynamicObject forceEncoding(VirtualFrame frame, DynamicObject string, Object encoding,
                                           @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile,
                                           @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return forceEncodingString(string, toStrNode.executeToStr(frame, encoding), differentEncodingProfile, mutableRopeProfile);
        }

    }

    @CoreMethod(names = "getbyte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode ropeGetByteNode;

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            ropeGetByteNode = RopeNodesFactory.GetByteNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public Object getByte(DynamicObject string, int index,
                              @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                              @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile) {
            final Rope rope = rope(string);

            if (negativeIndexProfile.profile(index < 0)) {
                index += rope.byteLength();
            }

            if (indexOutOfBoundsProfile.profile((index < 0) || (index >= rope.byteLength()))) {
                return nil();
            }

            return ropeGetByteNode.executeGetByte(rope, index);
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject string) {
            return rope(string).hashCode();
        }

    }

    @CoreMethod(names = "initialize", optional = 1, taintFromParameter = 0)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;
        @Child private ToStrNode toStrNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject initialize(DynamicObject self, NotProvided from) {
            return self;
        }

        @Specialization(guards = "isRubyString(from)")
        public DynamicObject initialize(DynamicObject self, DynamicObject from) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
            }

            if (isFrozenNode.executeIsFrozen(self)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        coreLibrary().frozenError(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(self)).getName(), this));
            }

            StringOperations.setRope(self, rope(from));

            return self;
        }

        @Specialization(guards = { "!isRubyString(from)", "wasProvided(from)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject self, Object from) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return initialize(self, toStrNode.executeToStr(frame, from));
        }
    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "self == from")
        public Object initializeCopySelfIsSameAsFrom(DynamicObject self, DynamicObject from) {
            return self;
        }


        @Specialization(guards = { "self != from", "isRubyString(from)" })
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            StringOperations.setRope(self, rope(from));

            return self;
        }

    }

    @CoreMethod(names = "insert", required = 2, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "otherString")
    })
    public abstract static class InsertNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode appendNode;
        @Child private StringPrimitiveNodes.CharacterByteIndexNode characterByteIndexNode;
        @Child private RopeNodes.MakeConcatNode prependMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode leftMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode rightMakeConcatNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            characterByteIndexNode = StringPrimitiveNodesFactory.CharacterByteIndexNodeFactory.create(context, sourceSection, new RubyNode[] {});
            leftMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            rightMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            leftMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            rightMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeGen.create(getContext(), getSourceSection(), index);
        }

        @CreateCast("otherString") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = { "indexAtStartBound(index)", "isRubyString(other)" })
        public Object insertPrepend(DynamicObject string, int index, DynamicObject other) {
            final Rope left = rope(other);
            final Rope right = rope(string);

            final Encoding compatibleEncoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, other);

            if (compatibleEncoding == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().encodingCompatibilityError(
                        String.format("incompatible encodings: %s and %s", left.getEncoding(), right.getEncoding()), this));
            }

            if (prependMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreter();
                prependMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            StringOperations.setRope(string, prependMakeConcatNode.executeMake(left, right, compatibleEncoding));

            return taintResultNode.maybeTaint(other, string);
        }

        @Specialization(guards = { "indexAtEndBound(index)", "isRubyString(other)" })
        public Object insertAppend(VirtualFrame frame, DynamicObject string, int index, DynamicObject other) {
            if (appendNode == null) {
                CompilerDirectives.transferToInterpreter();
                appendNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            appendNode.call(frame, string, "append", null, other);

            return taintResultNode.maybeTaint(other, string);
        }

        @Specialization(guards = { "!indexAtEitherBounds(index)", "isRubyString(other)" })
        public Object insert(VirtualFrame frame, DynamicObject string, int index, DynamicObject other,
                             @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            if (negativeIndexProfile.profile(index < 0)) {
                // Incrementing first seems weird, but MRI does it and it's significant because it uses the modified
                // index value in its error messages.  This seems wrong, but we should be compatible.
                index++;
            }

            final Rope source = rope(string);
            final Rope insert = rope(other);
            final Encoding compatibleEncoding = EncodingNodes.CompatibleQueryNode.compatibleEncodingForStrings(string, other);

            if (compatibleEncoding == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().encodingCompatibilityError(
                        String.format("incompatible encodings: %s and %s", source.getEncoding(), insert.getEncoding()), this));
            }

            final int stringLength = source.characterLength();
            final int normalizedIndex = StringNodesHelper.checkIndex(stringLength, index, this);
            final int byteIndex = characterByteIndexNode.executeInt(frame, string, normalizedIndex, 0);

            final Rope splitLeft = leftMakeSubstringNode.executeMake(source, 0, byteIndex);
            final Rope splitRight = rightMakeSubstringNode.executeMake(source, byteIndex, source.byteLength() - byteIndex);
            final Rope joinedLeft = leftMakeConcatNode.executeMake(splitLeft, insert, compatibleEncoding);
            final Rope joinedRight = rightMakeConcatNode.executeMake(joinedLeft, splitRight, compatibleEncoding);

            StringOperations.setRope(string, joinedRight);

            return taintResultNode.maybeTaint(other, string);
        }

        protected  boolean indexAtStartBound(int index) {
            return index == 0;
        }

        protected boolean indexAtEndBound(int index) {
            // TODO (nirvdrum 14-Jan-16) Now that we know the character length of the string, we can update the check for positive numbers as well.
            return index == -1;
        }

        protected boolean indexAtEitherBounds(int index) {
            return indexAtStartBound(index) || indexAtEndBound(index);
        }
    }

    @CoreMethod(names = "lstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class LstripBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public LstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = "isEmpty(string)")
        public DynamicObject lstripBangEmptyString(DynamicObject string) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = { "!isEmpty(string)", "isSingleByteOptimizable(string)" })
        public Object lstripBangSingleByte(DynamicObject string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#singleByteLStrip.

            final Rope rope = rope(string);
            final int s = rope.begin();
            final int end = s + rope.byteLength();
            final byte[] bytes = rope.getBytes();

            int p = s;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;
            if (p > s) {
                StringOperations.setRope(string, makeSubstringNode.executeMake(rope, p - s, end - p));

                return string;
            }

            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = { "!isEmpty(string)", "!isSingleByteOptimizable(string)" })
        public Object lstripBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#multiByteLStrip.

            final Rope rope = rope(string);
            final Encoding enc = RopeOperations.STR_ENC_GET(rope);
            final int s = rope.begin();
            final int end = s + rope.byteLength();
            final byte[] bytes = rope.getBytes();

            int p = s;

            while (p < end) {
                int c = StringSupport.codePoint(getContext().getJRubyRuntime(), enc, bytes, p, end);
                if (!ASCIIEncoding.INSTANCE.isSpace(c)) break;
                p += StringSupport.codeLength(enc, c);
            }

            if (p > s) {
                StringOperations.setRope(string, makeSubstringNode.executeMake(rope, p - s, end - p));

                return string;
            }

            return nil();
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "modify!", raiseIfFrozenSelf = true)
    public abstract static class ModifyBangNode extends CoreMethodArrayArgumentsNode {

        public ModifyBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject modifyBang(DynamicObject string) {
            StringOperations.modify(string);
            return string;
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "num_bytes=", lowerFixnumParameters = 0, required = 1)
    public abstract static class SetNumBytesNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public SetNumBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization
        public DynamicObject setNumBytes(DynamicObject string, int count) {
            final Rope rope = rope(string);

            if (count > rope.byteLength()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError(
                        String.format("Invalid byte count: %d exceeds string size of %d bytes", count, rope.byteLength()), this));
            }

            StringOperations.setRope(string, makeSubstringNode.executeMake(rope, 0, count));

            return string;
        }
    }

    @CoreMethod(names = "ord")
    @ImportStatic(StringGuards.class)
    public abstract static class OrdNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode ropeGetByteNode;

        public OrdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isEmpty(string)")
        @TruffleBoundary
        public int ordEmpty(DynamicObject string) {
            throw new RaiseException(coreLibrary().argumentError("empty string", this));
        }

        // TODO (nirvdrum 03-Feb-16): Is it possible to have a single-byte optimizable string that isn't ASCII-compatible?
        @Specialization(guards = { "!isEmpty(string)", "isSingleByteOptimizable(string)" })
        public int ordAsciiOnly(DynamicObject string) {
            if (ropeGetByteNode == null) {
                CompilerDirectives.transferToInterpreter();
                ropeGetByteNode = insert(RopeNodes.GetByteNode.create(getContext(), getSourceSection()));
            }

            return ropeGetByteNode.executeGetByte(rope(string), 0);
        }

        @Specialization(guards = { "!isEmpty(string)", "!isSingleByteOptimizable(string)" })
        public int ord(DynamicObject string) {
            final Rope rope = rope(string);

            try {
                return codePoint(rope.getEncoding(), rope.getBytes(), rope.begin(), rope.begin() + rope.byteLength());
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError(e.getMessage(), this));
            }
        }

        @TruffleBoundary
        private int codePoint(Encoding encoding, byte[] bytes, int p, int end) {
            return StringSupport.codePoint(encoding, bytes, p, end);
        }

    }

    @CoreMethod(names = "replace", required = 1, raiseIfFrozenSelf = true, taintFromParameter = 0)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class ReplaceNode extends CoreMethodNode {

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "string == other")
        public DynamicObject replaceStringIsSameAsOther(DynamicObject string, DynamicObject other) {
            return string;
        }


        @Specialization(guards = { "string != other", "isRubyString(other)" })
        public DynamicObject replace(DynamicObject string, DynamicObject other) {
            StringOperations.setRope(string, rope(other));

            return string;
        }

    }

    @CoreMethod(names = "rstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class RstripBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public RstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @Specialization(guards = "isEmpty(string)")
        public DynamicObject rstripBangEmptyString(DynamicObject string) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = { "!isEmpty(string)", "isSingleByteOptimizable(string)" })
        public Object rstripBangSingleByte(DynamicObject string) {
            // Taken from org.jruby.RubyString#rstrip_bang19 and org.jruby.RubyString#singleByteRStrip19.

            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();
            final int start = 0;
            final int end = rope.byteLength();
            int endp = end - 1;
            while (endp >= start && (bytes[endp] == 0 ||
                    ASCIIEncoding.INSTANCE.isSpace(bytes[endp] & 0xff))) endp--;

            if (endp < end - 1) {
                StringOperations.setRope(string, makeSubstringNode.executeMake(rope, 0, endp - start + 1));

                return string;
            }

            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = { "!isEmpty(string)", "!isSingleByteOptimizable(string)" })
        public Object rstripBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#rstrip_bang19 and org.jruby.RubyString#multiByteRStrip19.

            final Rope rope = rope(string);
            final Encoding enc = RopeOperations.STR_ENC_GET(rope);
            final byte[] bytes = rope.getBytes();
            final int start = 0;
            final int end = rope.byteLength();

            int endp = end;
            int prev;
            while ((prev = prevCharHead(enc, bytes, start, endp, end)) != -1) {
                int point = StringSupport.codePoint(getContext().getJRubyRuntime(), enc, bytes, prev, end);
                if (point != 0 && !ASCIIEncoding.INSTANCE.isSpace(point)) break;
                endp = prev;
            }

            if (endp < end) {
                StringOperations.setRope(string, makeSubstringNode.executeMake(rope, 0, endp - start));

                return string;
            }
            return nil();
        }

        @TruffleBoundary
        private int prevCharHead(Encoding enc, byte[]bytes, int p, int s, int end) {
            return enc.prevCharHead(bytes, p, s, end);
        }
    }

    @CoreMethod(names = "swapcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class SwapcaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public SwapcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject swapcaseSingleByte(DynamicObject string,
                                                @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#swapcase_bang19.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();

            if (enc.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        coreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (emptyStringProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final int s = rope.begin();
            final int end = s + rope.byteLength();
            final byte[] bytes = rope.getBytesCopy();

            if (singleByteOptimizableProfile.profile(rope.isSingleByteOptimizable())) {
                if (StringSupport.singleByteSwapcase(bytes, s, end)) {
                    StringOperations.setRope(string, makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                    return string;
                }
            } else {
                if (StringSupport.multiByteSwapcase(getContext().getJRubyRuntime(), enc, bytes, s, end)) {
                    StringOperations.setRope(string, makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                    return string;
                }
            }

            return nil();
        }
    }

    @CoreMethod(names = "dump", taintFromSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DumpNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public DumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "isAsciiCompatible(string)")
        public DynamicObject dumpAsciiCompatible(DynamicObject string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);
            outputBytes.setEncoding(encoding(string));

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT), null);

            return result;
        }

        @TruffleBoundary
        @Specialization(guards = "!isAsciiCompatible(string)")
        public DynamicObject dump(DynamicObject string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);

            try {
                outputBytes.append(".force_encoding(\"".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedOperationException(e);
            }

            outputBytes.append(encoding(string).getName());
            outputBytes.append((byte) '"');
            outputBytes.append((byte) ')');

            outputBytes.setEncoding(ASCIIEncoding.INSTANCE);

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT), null);

            return result;
        }

        @TruffleBoundary
        private ByteList dumpCommon(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            return StringSupport.dumpCommon(getContext().getJRubyRuntime(), StringOperations.getByteListReadOnly(string));
        }
    }

    @CoreMethod(names = "setbyte", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class SetByteNode extends CoreMethodNode {

        @Child private RopeNodes.MakeConcatNode composedMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode middleMakeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            composedMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            middleMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(context, sourceSection, null, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
            leftMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
            rightMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(context, sourceSection, null, null, null);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return FixnumLowerNodeGen.create(getContext(), getSourceSection(),
                    ToIntNodeGen.create(getContext(), getSourceSection(), index));
        }

        @CreateCast("value") public RubyNode coerceValueToInt(RubyNode value) {
            return FixnumLowerNodeGen.create(getContext(), getSourceSection(),
                    ToIntNodeGen.create(getContext(), getSourceSection(), value));
        }

        @Specialization
        public int setByte(DynamicObject string, int index, int value) {
            final int normalizedIndex = StringNodesHelper.checkIndexForRef(string, index, this);

            final Rope rope = rope(string);

            final Rope left = leftMakeSubstringNode.executeMake(rope, 0, normalizedIndex);
            final Rope right = rightMakeSubstringNode.executeMake(rope, normalizedIndex + 1, rope.byteLength() - normalizedIndex - 1);
            final Rope middle = makeLeafRopeNode.executeMake(new byte[] { (byte) value }, rope.getEncoding(), CodeRange.CR_UNKNOWN, NotProvided.INSTANCE);
            final Rope composed = composedMakeConcatNode.executeMake(middleMakeConcatNode.executeMake(left, middle, rope.getEncoding()), right, rope.getEncoding());

            StringOperations.setRope(string, composed);

            return value;
        }
    }

    @CoreMethod(names = {"size", "length"})
    @ImportStatic(StringGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject string,
                        @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
            final Rope rope = rope(string);

            if (mutableRopeProfile.profile(rope instanceof RopeBuffer)) {
                // TODO (nirvdrum 11-Mar-16): This response is only correct for CR_7BIT. Mutable ropes have not been updated for multi-byte characters.
                return ((RopeBuffer) rope).getByteList().realSize();
            } else {
                return rope.characterLength();
            }
        }

    }

    @CoreMethod(names = "squeeze!", rest = true, raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class SqueezeBangNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public SqueezeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isEmpty(string)")
        public DynamicObject squeezeBangEmptyString(DynamicObject string, Object[] args) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(string)", "zeroArgs(args)" })
        @TruffleBoundary
        public Object squeezeBangZeroArgs(DynamicObject string, Object[] args,
                                          @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            final Rope rope = rope(string);
            final ByteList buffer = RopeOperations.toByteListCopy(rope);

            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
            for (int i = 0; i < StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

            if (singleByteOptimizableProfile.profile(rope.isSingleByteOptimizable())) {
                if (! StringSupport.singleByteSqueeze(buffer, squeeze)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            } else {
                if (! squeezeCommonMultiByte(buffer, squeeze, null, encoding(string), false)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            }

            return string;
        }

        @Specialization(guards = { "!isEmpty(string)", "!zeroArgs(args)" })
        public Object squeezeBang(VirtualFrame frame, DynamicObject string, Object[] args,
                                  @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final DynamicObject[] otherStrings = new DynamicObject[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStrNode.executeToStr(frame, args[i]);
            }

            return performSqueezeBang(string, otherStrings, singleByteOptimizableProfile);
        }

        @TruffleBoundary
        private Object performSqueezeBang(DynamicObject string, DynamicObject[] otherStrings,
                                          @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {

            final Rope rope = rope(string);
            final ByteList buffer = RopeOperations.toByteListCopy(rope);

            DynamicObject otherStr = otherStrings[0];
            Rope otherRope = rope(otherStr);
            Encoding enc = StringOperations.checkEncoding(getContext(), string, otherStr, this);
            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(RopeOperations.getByteListReadOnly(otherRope), getContext().getJRubyRuntime(), squeeze, null, true, enc);

            boolean singlebyte = rope.isSingleByteOptimizable() && otherRope.isSingleByteOptimizable();

            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];
                otherRope = rope(otherStr);
                enc = StringOperations.checkEncoding(getContext(), string, otherStr, this);
                singlebyte = singlebyte && otherRope.isSingleByteOptimizable();
                tables = StringSupport.trSetupTable(RopeOperations.getByteListReadOnly(otherRope), getContext().getJRubyRuntime(), squeeze, tables, false, enc);
            }

            if (singleByteOptimizableProfile.profile(singlebyte)) {
                if (! StringSupport.singleByteSqueeze(buffer, squeeze)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            } else {
                if (! StringSupport.multiByteSqueeze(getContext().getJRubyRuntime(), buffer, squeeze, tables, enc, true)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            }

            return string;
        }

        @TruffleBoundary
        private boolean squeezeCommonMultiByte(ByteList value, boolean squeeze[], StringSupport.TrTables tables, Encoding enc, boolean isArg) {
            return StringSupport.multiByteSqueeze(getContext().getJRubyRuntime(), value, squeeze, tables, enc, isArg);
        }

        public static boolean zeroArgs(Object[] args) {
            return args.length == 0;
        }
    }

    @CoreMethod(names = "succ!", raiseIfFrozenSelf = true)
    public abstract static class SuccBangNode extends CoreMethodArrayArgumentsNode {

        public SuccBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject succBang(DynamicObject string) {
            final Rope rope = rope(string);

            if (! rope.isEmpty()) {
                final ByteList succByteList = StringSupport.succCommon(getContext().getJRubyRuntime(), StringOperations.getByteListReadOnly(string));

                StringOperations.setRope(string, StringOperations.ropeFromByteList(succByteList, rope.getCodeRange()));
            }

            return string;
        }
    }

    // String#sum is in Java because without OSR we can't warm up the Rubinius implementation

    @CoreMethod(names = "sum", optional = 1)
    public abstract static class SumNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode addNode;
        @Child private CallDispatchHeadNode subNode;
        @Child private CallDispatchHeadNode shiftNode;
        @Child private CallDispatchHeadNode andNode;

        public SumNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            addNode = DispatchHeadNodeFactory.createMethodCall(context);
            subNode = DispatchHeadNodeFactory.createMethodCall(context);
            shiftNode = DispatchHeadNodeFactory.createMethodCall(context);
            andNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object sum(VirtualFrame frame, DynamicObject string, int bits) {
            return sum(frame, string, (long) bits);
        }

        @Specialization
        public Object sum(VirtualFrame frame, DynamicObject string, long bits) {
            // Copied from JRuby

            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();
            int p = rope.begin();
            final int len = rope.byteLength();
            final int end = p + len;

            if (bits >= 8 * 8) { // long size * bits in byte
                Object sum = 0;
                while (p < end) {
                    sum = addNode.call(frame, sum, "+", null, bytes[p++] & 0xff);
                }
                if (bits != 0) {
                    final Object mod = shiftNode.call(frame, 1, "<<", null, bits);
                    sum = andNode.call(frame, sum, "&", null, subNode.call(frame, mod, "-", null, 1));
                }
                return sum;
            } else {
                long sum = 0;
                while (p < end) {
                    sum += bytes[p++] & 0xff;
                }
                return bits == 0 ? sum : sum & (1L << bits) - 1L;
            }
        }

        @Specialization
        public Object sum(VirtualFrame frame, DynamicObject string, NotProvided bits) {
            return sum(frame, string, 16);
        }

        @Specialization(guards = { "!isInteger(bits)", "!isLong(bits)", "wasProvided(bits)" })
        public Object sum(DynamicObject string, Object bits) {
            return ruby("sum Rubinius::Type.coerce_to(bits, Fixnum, :to_int)", "bits", bits);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public double toF(DynamicObject string) {
            try {
                return convertToDouble(string);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @TruffleBoundary
        private double convertToDouble(DynamicObject string) {
            return ConvertDouble.byteListToDouble19(StringOperations.getByteListReadOnly(string), false);
        }
    }

    @CoreMethod(names = { "to_s", "to_str" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "!isStringSubclass(string)")
        public DynamicObject toS(DynamicObject string) {
            return string;
        }

        @Specialization(guards = "isStringSubclass(string)")
        public Object toSOnSubclass(DynamicObject string) {
            return ruby("''.replace(self)", "self", string);
        }

        public boolean isStringSubclass(DynamicObject string) {
            return Layouts.BASIC_OBJECT.getLogicalClass(string) != coreLibrary().getStringClass();
        }

    }

    @CoreMethod(names = {"to_sym", "intern"})
    public abstract static class ToSymNode extends CoreMethodArrayArgumentsNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toSym(DynamicObject string) {
            return getSymbol(rope(string));
        }
    }

    @CoreMethod(names = "reverse!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class ReverseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        @Specialization(guards = "reverseIsEqualToSelf(string)")
        public DynamicObject reverseNoOp(DynamicObject string) {
            return string;
        }

        @Specialization(guards = { "!reverseIsEqualToSelf(string)", "isSingleByteOptimizable(string)" })
        public DynamicObject reverseSingleByteOptimizable(DynamicObject string) {
            final Rope rope = rope(string);
            final byte[] originalBytes = rope.getBytes();
            final int len = originalBytes.length;
            final byte[] reversedBytes = new byte[len];

            for (int i = 0; i < len; i++) {
                reversedBytes[len - i - 1] = originalBytes[i];;
            }

            StringOperations.setRope(string, makeLeafRopeNode.executeMake(reversedBytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

            return string;
        }

        @Specialization(guards = { "!reverseIsEqualToSelf(string)", "!isSingleByteOptimizable(string)" })
        public DynamicObject reverse(DynamicObject string) {
            // Taken from org.jruby.RubyString#reverse!

            final Rope rope = rope(string);
            final byte[] originalBytes = rope.getBytes();
            int p = 0;
            final int len = originalBytes.length;

            final Encoding enc = rope.getEncoding();
            final int end = p + len;
            int op = len;
            final byte[] reversedBytes = new byte[len];

            while (p < end) {
                int cl = StringSupport.length(enc, originalBytes, p, end);
                if (cl > 1 || (originalBytes[p] & 0x80) != 0) {
                    op -= cl;
                    System.arraycopy(originalBytes, p, reversedBytes, op, cl);
                    p += cl;
                } else {
                    reversedBytes[--op] = originalBytes[p++];
                }
            }

            StringOperations.setRope(string, makeLeafRopeNode.executeMake(reversedBytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

            return string;
        }

        public static boolean reverseIsEqualToSelf(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            // TODO (nirvdrum 08-Jan-16) I suspect this invariant holds for multi-byte characters as well. If we have the logical string length calculated already, we can use it here as well.
            return rope(string).byteLength() <= 1;
        }
    }

    @CoreMethod(names = "tr!", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "self"),
        @NodeChild(type = RubyNode.class, value = "fromStr"),
        @NodeChild(type = RubyNode.class, value = "toStrNode")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class TrBangNode extends CoreMethodNode {

        @Child private DeleteBangNode deleteBangNode;

        public TrBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("fromStr") public RubyNode coerceFromStrToString(RubyNode fromStr) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), toStr);
        }

        @Specialization(guards = "isEmpty(self)")
        public Object trBangEmpty(DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(self)", "isRubyString(fromStr)", "isRubyString(toStr)" })
        public Object trBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (rope(toStr).isEmpty()) {
                if (deleteBangNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
                }

                return deleteBangNode.deleteBang(frame, self, fromStr);
            }

            return StringNodesHelper.trTransHelper(getContext(), self, fromStr, toStr, false);
        }
    }

    @CoreMethod(names = "tr_s!", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "fromStr"),
            @NodeChild(type = RubyNode.class, value = "toStrNode")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class TrSBangNode extends CoreMethodNode {

        @Child private DeleteBangNode deleteBangNode;

        public TrSBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("fromStr") public RubyNode coerceFromStrToString(RubyNode fromStr) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), toStr);
        }

        @Specialization(guards = "isEmpty(self)")
        public DynamicObject trSBangEmpty(DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(self)", "isRubyString(fromStr)", "isRubyString(toStr)" })
        public Object trSBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (rope(toStr).isEmpty()) {
                if (deleteBangNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
                }

                return deleteBangNode.deleteBang(frame, self, fromStr);
            }

            return StringNodesHelper.trTransHelper(getContext(), self, fromStr, toStr, true);
        }
    }

    @CoreMethod(names = "unpack", required = 1, taintFromParameter = 0)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class UnpackNode extends ArrayCoreMethodNode {

        @Child private TaintNode taintNode;

        private final BranchProfile exceptionProfile = BranchProfile.create();

        public UnpackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(
                guards = {
                        "isRubyString(format)",
                        "ropesEqual(format, cachedFormat)"
                },
                limit = "getCacheLimit()")
        public DynamicObject unpackCached(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject format,
                @Cached("privatizeRope(format)") Rope cachedFormat,
                @Cached("create(compileFormat(format))") DirectCallNode callUnpackNode) {
            final Rope rope = rope(string);

            final ArrayResult result;

            try {
                result = (ArrayResult) callUnpackNode.call(frame,
                        new Object[]{ rope.getBytes(), rope.byteLength() });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishUnpack(result);
        }

        @Specialization(contains = "unpackCached", guards = "isRubyString(format)")
        public DynamicObject unpackUncached(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject format,
                @Cached("create()") IndirectCallNode callUnpackNode) {
            final Rope rope = rope(string);

            final ArrayResult result;

            try {
                result = (ArrayResult) callUnpackNode.call(frame, compileFormat(format),
                        new Object[]{ rope.getBytes(), rope.byteLength() });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishUnpack(result);
        }

        private DynamicObject finishUnpack(ArrayResult result) {
            final DynamicObject array = Layouts.ARRAY.createArray(
                    coreLibrary().getArrayFactory(), result.getOutput(), result.getOutputLength());

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
                }

                taintNode.executeTaint(array);
            }

            return array;
        }

        @Specialization(guards = {
                "!isRubyString(format)",
                "!isBoolean(format)",
                "!isInteger(format)",
                "!isLong(format)",
                "!isNil(format)"})
        public Object unpack(DynamicObject array, Object format) {
            return ruby("unpack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            return new UnpackCompiler(getContext(), this).compile(format.toString());
        }

        protected int getCacheLimit() {
            return getContext().getOptions().UNPACK_CACHE;
        }

    }

    @CoreMethod(names = "upcase", taintFromSelf = true)
    public abstract static class UpcaseNode extends CoreMethodArrayArgumentsNode {

        @Child CallDispatchHeadNode dupNode;
        @Child CallDispatchHeadNode upcaseBangNode;

        public UpcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dupNode = DispatchHeadNodeFactory.createMethodCall(context);
            upcaseBangNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object upcase(VirtualFrame frame, DynamicObject string) {
            final Object duped = dupNode.call(frame, string, "dup", null);
            upcaseBangNode.call(frame, duped, "upcase!", null);

            return duped;
        }

    }

    @CoreMethod(names = "upcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class UpcaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public UpcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public DynamicObject upcaseSingleByte(DynamicObject string,
                                              @Cached("createBinaryProfile()") ConditionProfile isEmptyProfile,
                                              @Cached("createBinaryProfile()") ConditionProfile modifiedProfile) {
            final Rope rope = rope(string);

            if (isEmptyProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final byte[] bytes = rope.getBytesCopy();
            final boolean modified = singleByteUpcase(bytes, 0, bytes.length);

            if (modifiedProfile.profile(modified)) {
                final Rope newRope = makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength());
                StringOperations.setRope(string, newRope);

                return string;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public DynamicObject upcase(DynamicObject string) {
            final Rope rope = rope(string);
            final Encoding encoding = rope.getEncoding();

            if (encoding.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        coreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", encoding), this));
            }

            if (rope.isEmpty()) {
                return nil();
            }

            final ByteList bytes = RopeOperations.toByteListCopy(rope);

            try {
                final boolean modified = multiByteUpcase(encoding, bytes.unsafeBytes(), bytes.begin(), bytes.realSize());
                if (modified) {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(bytes, rope.getCodeRange()));

                    return string;
                } else {
                    return nil();
                }
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError(e.getMessage(), this));
            }
        }

        @TruffleBoundary
        private boolean singleByteUpcase(byte[] bytes, int s, int end) {
            return StringSupport.singleByteUpcase(bytes, s, end);
        }

        @TruffleBoundary
        private boolean multiByteUpcase(Encoding encoding, byte[] bytes, int s, int end) {
            return StringSupport.multiByteUpcase(encoding, bytes, s, end);
        }

    }

    @CoreMethod(names = "valid_encoding?")
    @ImportStatic(StringGuards.class)
    public abstract static class ValidEncodingQueryNode extends CoreMethodArrayArgumentsNode {

        public ValidEncodingQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isBrokenCodeRange(string)")
        public boolean validEncodingQueryBroken(DynamicObject string) {
            return false;
        }

        @Specialization(guards = "!isBrokenCodeRange(string)")
        public boolean validEncodingQuery(DynamicObject string) {
            return true;
        }

    }

    @CoreMethod(names = "capitalize!", raiseIfFrozenSelf = true)
    public abstract static class CapitalizeBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public CapitalizeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(context, sourceSection, null, null, null, null);
        }

        @Specialization
        @TruffleBoundary
        public DynamicObject capitalizeBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#capitalize_bang19.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();

            if (enc.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        coreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (rope.isEmpty()) {
                return nil();
            }

            StringOperations.modifyAndKeepCodeRange(string);

            int s = 0;
            int end = s + rope.byteLength();
            byte[] bytes = rope.getBytesCopy();
            boolean modify = false;

            int c = StringSupport.codePoint(getContext().getJRubyRuntime(), enc, bytes, s, end);
            if (enc.isLower(c)) {
                enc.codeToMbc(StringSupport.toUpper(enc, c), bytes, s);
                modify = true;
            }

            s += StringSupport.codeLength(enc, c);
            while (s < end) {
                c = StringSupport.codePoint(getContext().getJRubyRuntime(), enc, bytes, s, end);
                if (enc.isUpper(c)) {
                    enc.codeToMbc(StringSupport.toLower(enc, c), bytes, s);
                    modify = true;
                }
                s += StringSupport.codeLength(enc, c);
            }

            if (modify) {
                StringOperations.setRope(string, makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                return string;
            }

            return nil();
        }
    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode;

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            withEncodingNode = RopeNodes.WithEncodingNode.create(context, sourceSection);
        }

        @Specialization
        public DynamicObject clear(DynamicObject string,
                                   @Cached("createBinaryProfile()") ConditionProfile isUTF8,
                                   @Cached("createBinaryProfile()") ConditionProfile isUSAscii,
                                   @Cached("createBinaryProfile()") ConditionProfile isAscii8Bit) {
            final Rope rope = rope(string);
            final Rope emptyRope;

            if (isUTF8.profile(rope.getEncoding() == UTF8Encoding.INSTANCE)) {
                emptyRope = RopeConstants.EMPTY_UTF8_ROPE;
            } else if (isUSAscii.profile(rope.getEncoding() == USASCIIEncoding.INSTANCE)) {
                emptyRope = RopeConstants.EMPTY_US_ASCII_ROPE;
            } else if (isAscii8Bit.profile(rope.getEncoding() == ASCIIEncoding.INSTANCE)) {
                emptyRope = RopeConstants.EMPTY_ASCII_8BIT_ROPE;
            } else {
                emptyRope = withEncodingNode.executeWithEncoding(RopeConstants.EMPTY_ASCII_8BIT_ROPE, rope.getEncoding(), CodeRange.CR_7BIT);
            }

            StringOperations.setRope(string, emptyRope);

            return string;
        }
    }

    public static class StringNodesHelper {

        public static int checkIndex(int length, int index, RubyNode node) {
            if (index > length) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
            }

            if (index < 0) {
                if (-index > length) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(
                            node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
                }

                index += length;
            }

            return index;
        }

        public static int checkIndexForRef(DynamicObject string, int index, RubyNode node) {
            assert RubyGuards.isRubyString(string);

            final int length = rope(string).byteLength();

            if (index >= length) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
            }

            if (index < 0) {
                if (-index > length) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(
                            node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
                }

                index += length;
            }

            return index;
        }

        @TruffleBoundary
        private static Object trTransHelper(RubyContext context, DynamicObject self, DynamicObject fromStr, DynamicObject toStr, boolean sFlag) {
            assert RubyGuards.isRubyString(self);
            assert RubyGuards.isRubyString(fromStr);
            assert RubyGuards.isRubyString(toStr);

            final CodeRangeable buffer = StringOperations.getCodeRangeableReadWrite(self);
            final CodeRangeable ret = StringSupport.trTransHelper(context.getJRubyRuntime(), buffer, StringOperations.getCodeRangeableReadOnly(fromStr), StringOperations.getCodeRangeableReadOnly(toStr), sFlag);

            if (ret == null) {
                return context.getCoreLibrary().getNilObject();
            }

            StringOperations.setRope(self, StringOperations.ropeFromByteList(buffer.getByteList(), buffer.getCodeRange()));

            return self;
        }
    }

}
