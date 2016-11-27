/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Some of the code in this class is transposed from org.jruby.RubyString
 * and String Support and licensed under the same EPL1.0/GPL 2.0/LGPL 2.1
 * used throughout.
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
 * Some of the code in this class is transposed from org.jruby.util.ByteList,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 *
 * Copyright (C) 2007-2010 JRuby Community
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
 *
 * Some of the code in this class is transliterated from C++ code in Rubinius.
 *
 * Copyright (c) 2007-2014, Evan Phoenix and contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * Neither the name of Rubinius nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jruby.truffle.core.string;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Fallback;
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
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.PrimitiveNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayCoreMethodNode;
import org.jruby.truffle.core.cast.ArrayAttributeCastNodeGen;
import org.jruby.truffle.core.cast.CmpIntNode;
import org.jruby.truffle.core.cast.CmpIntNodeGen;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.cast.ToStrNode;
import org.jruby.truffle.core.cast.ToStrNodeGen;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.encoding.EncodingNodesFactory;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.format.FormatExceptionTranslator;
import org.jruby.truffle.core.format.exceptions.FormatException;
import org.jruby.truffle.core.format.unpack.ArrayResult;
import org.jruby.truffle.core.format.unpack.UnpackCompiler;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.regexp.RegexpNodes.RegexpSetLastMatchPrimitiveNode;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.ConcatRope;
import org.jruby.truffle.core.rope.LeafRope;
import org.jruby.truffle.core.rope.RepeatingRope;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeBuffer;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodes.MakeRepeatingNode;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.rope.SubstringRope;
import org.jruby.truffle.core.string.StringNodesFactory.StringAreComparableNodeGen;
import org.jruby.truffle.core.string.StringNodesFactory.StringEqualNodeGen;
import org.jruby.truffle.language.CheckLayoutNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.IsTaintedNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.ConvertDouble;
import org.jruby.util.StringSupport;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jruby.truffle.core.rope.RopeConstants.EMPTY_ASCII_8BIT_ROPE;
import static org.jruby.truffle.core.string.StringOperations.encoding;
import static org.jruby.truffle.core.string.StringOperations.rope;

@CoreClass("String")
public abstract class StringNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateObjectNode.allocate(rubyClass, EMPTY_ASCII_8BIT_ROPE);
        }

    }

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class AddNode extends CoreMethodNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private TaintResultNode taintResultNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(context, sourceSection, null, null);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            taintResultNode = new TaintResultNode(null, null);
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(null, null, other);
        }

        @Specialization(guards = { "!isRopeBuffer(string)", "isRubyString(other)", "getEncoding(string) == getEncoding(other)" })
        public DynamicObject addSameEncoding(DynamicObject string, DynamicObject other) {
            return add(string, other, getEncoding(string));
        }

        @Specialization(guards = { "!isRopeBuffer(string)", "isRubyString(other)", "getEncoding(string) != getEncoding(other)", "isUTF8AndUSASCII(string, other)" })
        public DynamicObject addUTF8AndUSASCII(DynamicObject string, DynamicObject other) {
            return add(string, other, UTF8Encoding.INSTANCE);
        }

        @Specialization(guards = { "!isRopeBuffer(string)", "isRubyString(other)", "getEncoding(string) != getEncoding(other)", "!isUTF8AndUSASCII(string, other)" })
        public DynamicObject addDifferentEncodings(DynamicObject string, DynamicObject other) {
            final Encoding enc = checkEncodingNode.executeCheckEncoding(string, other);
            return add(string, other, enc);
        }

        @Specialization(guards = { "isRopeBuffer(string)", "is7Bit(string)", "is7Bit(other)" })
        public DynamicObject addRopeBuffer(DynamicObject string, DynamicObject other,
                                           @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile) {
            final Encoding enc = checkEncodingNode.executeCheckEncoding(string, other);

            final RopeBuffer left = (RopeBuffer) rope(string);
            final ByteList leftByteList = left.getByteList();
            final Rope right = rope(other);

            final ByteList concatByteList;
            if (ropeBufferProfile.profile(right instanceof RopeBuffer)) {
                concatByteList = StringSupport.addByteLists(leftByteList, ((RopeBuffer) right).getByteList());
            } else {
                // Taken from org.jruby.util.StringSupport.addByteLists.

                final int newLength = leftByteList.realSize() + right.byteLength();
                concatByteList = new ByteList(newLength);
                concatByteList.realSize(newLength);
                System.arraycopy(leftByteList.unsafeBytes(), leftByteList.begin(), concatByteList.unsafeBytes(), 0, leftByteList.realSize());
                System.arraycopy(right.getBytes(), 0, concatByteList.unsafeBytes(), leftByteList.realSize(), right.byteLength());
            }

            concatByteList.setEncoding(enc);

            final RopeBuffer concatRope = new RopeBuffer(concatByteList, left.getCodeRange(), left.isSingleByteOptimizable(), concatByteList.realSize());
            final DynamicObject ret = Layouts.STRING.createString(coreLibrary().getStringFactory(), concatRope);

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }

        private DynamicObject add(DynamicObject string, DynamicObject other, Encoding encoding) {
            Rope left = rope(string);
            final Rope right = rope(other);

            final Rope concatRope = makeConcatNode.executeMake(left, right, encoding);

            final DynamicObject ret = Layouts.STRING.createString(coreLibrary().getStringFactory(), concatRope);

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }

        protected Encoding getEncoding(DynamicObject string) {
            return Layouts.STRING.getRope(string).getEncoding();
        }

        protected boolean isUTF8AndUSASCII(DynamicObject string, DynamicObject other) {
            final Encoding stringEncoding = getEncoding(string);
            final Encoding otherEncoding = getEncoding(other);

            if (stringEncoding != UTF8Encoding.INSTANCE && otherEncoding != UTF8Encoding.INSTANCE) {
                return false;
            }

            return stringEncoding == USASCIIEncoding.INSTANCE || otherEncoding == USASCIIEncoding.INSTANCE;
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnum = 1, taintFrom = 0)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private ToIntNode toIntNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        public abstract DynamicObject executeInt(VirtualFrame frame, DynamicObject string, int times);

        @Specialization(guards = "times < 0")
        public DynamicObject multiplyTimesNegative(DynamicObject string, int times) {
            throw new RaiseException(coreExceptions().argumentError("negative argument", this));
        }

        @Specialization(guards = "times >= 0")
        public DynamicObject multiply(DynamicObject string, int times,
                                      @Cached("create()") MakeRepeatingNode makeRepeatingNode) {
            final Rope repeated = makeRepeatingNode.executeMake(rope(string), times);

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), repeated);
        }

        @Specialization(guards = "isRubyBignum(times)")
        public DynamicObject multiply(DynamicObject string, DynamicObject times) {
            throw new RaiseException(coreExceptions().rangeError("bignum too big to convert into `long'", this));
        }

        @Specialization(guards = { "!isRubyBignum(times)", "!isInteger(times)" })
        public DynamicObject multiply(VirtualFrame frame, DynamicObject string, Object times) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
            }

            return executeInt(frame, string, toIntNode.doInt(frame, times));
        }

    }

    @CoreMethod(names = {"==", "===", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private StringEqualNode stringEqualNode;
        @Child private KernelNodes.RespondToNode respondToNode;
        @Child private CallDispatchHeadNode objectEqualNode;
        @Child private CheckLayoutNode checkLayoutNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            stringEqualNode = StringEqualNodeGen.create(null, null);
        }

        @Specialization(guards = "isRubyString(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return stringEqualNode.executeStringEqual(a, b);
        }

        @Specialization(guards = "!isRubyString(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, Object b) {
            if (respondToNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), null, null, null, null));
            }

            if (respondToNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (objectEqualNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    objectEqualNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                return objectEqualNode.callBoolean(frame, b, "==", null, a);
            }

            return false;
        }

        protected boolean isRubyString(DynamicObject object) {
            if (checkLayoutNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkLayoutNode = insert(new CheckLayoutNode());
            }

            return checkLayoutNode.isString(object);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode cmpNode;
        @Child private CmpIntNode cmpIntNode;
        @Child private KernelNodes.RespondToNode respondToCmpNode;
        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToStrNode toStrNode;

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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), null, null, null, null));
            }

            if (respondToToStrNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToCmpNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), null, null, null, null));
            }

            if (respondToCmpNode.doesRespondToString(frame, b, create7BitString("<=>", UTF8Encoding.INSTANCE), false)) {
                if (cmpNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cmpNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                final Object cmpResult = cmpNode.call(frame, b, "<=>", a);

                if (cmpResult == nil()) {
                    return nil();
                }

                if (cmpIntNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cmpIntNode = insert(CmpIntNodeGen.create(getContext(), null, null, null, null));
                }

                return -(cmpIntNode.executeCmpInt(frame, cmpResult, a, b));
            }

            return nil();
        }
    }

    @CoreMethod(names = { "<<", "concat" }, required = 1, taintFrom = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "other")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class ConcatNode extends CoreMethodNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private StringAppendPrimitiveNode stringAppendNode;

        @Specialization(guards = { "isRubyString(other)", "is7Bit(string)", "is7Bit(other)" })
        public DynamicObject concatStringSingleByte(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            if (makeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                makeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            StringOperations.setRope(string, makeConcatNode.executeMake(left, right, left.getEncoding()));

            return string;
        }

        @Specialization(guards =  { "isRubyString(other)", "!is7Bit(string) || !is7Bit(other)" })
        public Object concatString(DynamicObject string, DynamicObject other) {
            if (stringAppendNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stringAppendNode = insert(StringNodesFactory.StringAppendPrimitiveNodeFactory.create(getContext(), null, new RubyNode[] {}));
            }

            return stringAppendNode.executeStringAppend(string, other);
        }

        @Specialization(guards = "!isRubyString(other)")
        public Object concat(
                VirtualFrame frame,
                DynamicObject string,
                Object other,
                @Cached("createMethodCall()") CallDispatchHeadNode callNode) {
            return callNode.call(frame, string, "concat_internal", other);
        }

    }

    @CoreMethod(names = { "[]", "slice" }, required = 1, optional = 1, lowerFixnum = { 1, 2 }, taintFrom = 0)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private StringSubstringPrimitiveNode substringNode;
        @Child private AllocateObjectNode allocateObjectNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization(guards = "wasNotProvided(length) || isRubiniusUndefined(length)")
        public Object getIndex(VirtualFrame frame, DynamicObject string, int index, Object length) {
            // Check for the only difference from str[index, 1]
            if (index == rope(string).characterLength()) {
                outOfBounds.enter();
                return nil();
            }
            return getSubstringNode().execute(frame, string, index, 1);
        }

        @Specialization(guards = { "!isRubyRange(index)", "!isRubyRegexp(index)", "!isRubyString(index)", "wasNotProvided(length) || isRubiniusUndefined(length)" })
        public Object getIndex(VirtualFrame frame, DynamicObject string, Object index, Object length, @Cached("new()") SnippetNode snippetNode) {
            return getIndex(frame, string, (int)snippetNode.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", index), length);
        }

        @Specialization(guards = { "isIntRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)" })
        public Object sliceIntegerRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length) {
            return sliceRange(frame, string, Layouts.INT_RANGE.getBegin(range), Layouts.INT_RANGE.getEnd(range), Layouts.INT_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = { "isLongRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)" })
        public Object sliceLongRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values should be properly lowered, only if possible.
            return sliceRange(frame, string, (int) Layouts.LONG_RANGE.getBegin(range), (int) Layouts.LONG_RANGE.getEnd(range), Layouts.LONG_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = {"isObjectRange(range)", "wasNotProvided(length) || isRubiniusUndefined(length)"})
        public Object sliceObjectRange(VirtualFrame frame, DynamicObject string, DynamicObject range, Object length, @Cached("new()") SnippetNode snippetNode1, @Cached("new()") SnippetNode snippetNode2) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values may return Fixnums beyond int boundaries and we should handle that -- Bignums are always errors.
            final int coercedBegin = (int)snippetNode1.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", Layouts.OBJECT_RANGE.getBegin(range));
            final int coercedEnd = (int)snippetNode2.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", Layouts.OBJECT_RANGE.getEnd(range));

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
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.withEncodingVerySlow(RopeConstants.EMPTY_ASCII_8BIT_ROPE, encoding(string)));
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
        public Object slice(VirtualFrame frame, DynamicObject string, int start, Object length, @Cached("new()") SnippetNode snippetNode) {
            return slice(frame, string, start, (int)snippetNode.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", length));
        }

        @Specialization(guards = { "!isRubyRange(start)", "!isRubyRegexp(start)", "!isRubyString(start)", "wasProvided(length)" })
        public Object slice(VirtualFrame frame, DynamicObject string, Object start, Object length, @Cached("new()") SnippetNode snippetNode1, @Cached("new()") SnippetNode snippetNode2) {
            return slice(frame, string, (int)snippetNode1.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", start), (int)snippetNode2.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", length));
        }

        @Specialization(guards = {"isRubyRegexp(regexp)", "wasNotProvided(capture) || isRubiniusUndefined(capture)"})
        public Object slice1(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject regexp,
                Object capture,
                @Cached("createMethodCallIgnoreVisibility()") CallDispatchHeadNode callNode,
                @Cached("create()") RegexpSetLastMatchPrimitiveNode setLastMatchNode) {
            return sliceCapture(frame, string, regexp, 0, callNode, setLastMatchNode);
        }

        @Specialization(guards = {"isRubyRegexp(regexp)", "wasProvided(capture)"})
        public Object sliceCapture(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject regexp,
                Object capture,
                @Cached("createMethodCallIgnoreVisibility()") CallDispatchHeadNode callNode,
                @Cached("create()") RegexpSetLastMatchPrimitiveNode setLastMatchNode) {
            final Object matchStrPair = callNode.call(frame, string, "subpattern", regexp, capture);

            if (matchStrPair == nil()) {
                setLastMatchNode.executeSetLastMatch(nil());
                return nil();
            }

            final Object[] array = (Object[]) Layouts.ARRAY.getStore((DynamicObject) matchStrPair);

            setLastMatchNode.executeSetLastMatch(array[0]);

            return array[1];
        }

        @Specialization(guards = {"wasNotProvided(length) || isRubiniusUndefined(length)", "isRubyString(matchStr)"})
        public Object slice2(VirtualFrame frame, DynamicObject string, DynamicObject matchStr, Object length) {
            if (includeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                includeNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            boolean result = includeNode.callBoolean(frame, string, "include?", null, matchStr);

            if (result) {
                if (dupNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dupNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                throw new TaintResultNode.DoNotTaint(dupNode.call(frame, matchStr, "dup"));
            }

            return nil();
        }

        private StringSubstringPrimitiveNode getSubstringNode() {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                substringNode = insert(StringNodesFactory.StringSubstringPrimitiveNodeFactory.create(null));
            }

            return substringNode;
        }

        @Override
        protected boolean isRubiniusUndefined(Object object) {
            return object == coreLibrary().getRubiniusUndefined();
        }

    }

    @CoreMethod(names = "ascii_only?")
    @ImportStatic(StringGuards.class)
    public abstract static class ASCIIOnlyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = { "is7Bit(string)" })
        public boolean asciiOnlyAsciiCompatible7BitCR(DynamicObject string) {
            return true;
        }

        @Specialization(guards = { "!is7Bit(string)" })
        public boolean asciiOnlyAsciiCompatible(DynamicObject string) {
            return false;
        }

    }

    @CoreMethod(names = "b", taintFrom = 0)
    public abstract static class BNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode;

        public BNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(null, null, null);
        }

        @Specialization
        public DynamicObject b(DynamicObject string) {
            final Rope rope = rope(string);

            // If the rope is already known to be 7-bit, it'll continue to be 7-bit in ASCII 8-bit. Otherwise, it must
            // be valid since there's no way to have a broken code range in ASCII 8-bit (all byte values are valid).
            final CodeRange newCodeRange = rope.getCodeRange() == CodeRange.CR_7BIT ? CodeRange.CR_7BIT : CodeRange.CR_VALID;
            final Rope newRope = withEncodingNode.executeWithEncoding(rope, ASCIIEncoding.INSTANCE, newCodeRange);

            return createString(newRope);
        }

    }

    @CoreMethod(names = "bytes", needsBlock = true)
    public abstract static class BytesNode extends YieldingCoreMethodNode {

        @Specialization
        public DynamicObject bytes(VirtualFrame frame, DynamicObject string, NotProvided block) {
            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();

            final int[] store = new int[bytes.length];

            for (int n = 0; n < store.length; n++) {
                store[n] = ((int) bytes[n]) & 0xFF;
            }

            return createArray(store, store.length);
        }

        @Specialization
        public DynamicObject bytes(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            Rope rope = rope(string);
            byte[] bytes = rope.getBytes();

            for (int i = 0; i < bytes.length; i++) {
                yield(frame, block, bytes[i] & 0xff);
            }

            return string;
        }

    }

    @CoreMethod(names = "bytesize")
    public abstract static class ByteSizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int byteSize(DynamicObject string, @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile) {
            final Rope rope = rope(string);

            if (ropeBufferProfile.profile(rope instanceof RopeBuffer)) {
                return ((RopeBuffer) rope).getByteList().realSize();
            }

            return rope(string).byteLength();
        }

    }

    @CoreMethod(names = "casecmp", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class CaseCmpNode extends CoreMethodNode {

        @Child private EncodingNodes.CompatibleQueryNode compatibleQueryNode;

        public CaseCmpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compatibleQueryNode = EncodingNodesFactory.CompatibleQueryNodeFactory.create(context, sourceSection, new RubyNode[] {});
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(null, null, other);
        }

        @Specialization(guards = {"isRubyString(other)", "bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmpSingleByte(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19.

            if (compatibleQueryNode.executeCompatibleQuery(string, other) == nil()) {
                return nil();
            }

            return StringOperations.getByteListReadOnly(string).caseInsensitiveCmp(StringOperations.getByteListReadOnly(other));
        }

        @Specialization(guards = {"isRubyString(other)", "!bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmp(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19 and

            final DynamicObject encoding = compatibleQueryNode.executeCompatibleQuery(string, other);

            if (encoding == nil()) {
                return nil();
            }

            return multiByteCasecmp(Layouts.ENCODING.getEncoding(encoding), StringOperations.getByteListReadOnly(string), StringOperations.getByteListReadOnly(other));
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

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private ToStrNode toStr;

        public CountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(context, sourceSection, null, null);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization(guards = "isEmpty(string)")
        public int count(DynamicObject string, Object[] args) {
            return 0;
        }

        @Specialization(guards = "!isEmpty(string)")
        public int count(VirtualFrame frame, DynamicObject string, Object[] args,
                @Cached("create()") BranchProfile errorProfile) {
            if (args.length == 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentErrorEmptyVarargs(this));
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
            Encoding enc = checkEncodingNode.executeCheckEncoding(string, otherStr);

            final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), table, null, true, enc);
            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];

                assert RubyGuards.isRubyString(otherStr);

                enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
                tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), table, tables, false, enc);
            }

            return StringSupport.strCount(StringOperations.getByteListReadOnly(string), table, tables, enc);
        }
    }

    @CoreMethod(names = "crypt", required = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "salt")
    })
    public abstract static class CryptNode extends CoreMethodNode {

        @Child private TaintResultNode taintResultNode;

        @CreateCast("salt") public RubyNode coerceSaltToString(RubyNode other) {
            return ToStrNodeGen.create(null, null, other);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = "isRubyString(salt)")
        public Object crypt(DynamicObject string, DynamicObject salt) {
            // Taken from org.jruby.RubyString#crypt.

            final Rope value = rope(string);
            final Rope other = rope(salt);

            if (other.byteLength() < 2) {
                throw new RaiseException(coreExceptions().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final TrufflePosix posix = posix();
            final byte[] keyBytes = Arrays.copyOfRange(value.getBytes(), 0, value.byteLength());
            final byte[] saltBytes = Arrays.copyOfRange(other.getBytes(), 0, other.byteLength());

            if (saltBytes[0] == 0 || saltBytes[1] == 0) {
                throw new RaiseException(coreExceptions().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final byte[] cryptedString = posix.crypt(keyBytes, saltBytes);

            // We differ from MRI in that we do not process salt to make it work and we will
            // return any errors via errno.
            if (cryptedString == null) {
                throw new RaiseException(coreExceptions().errnoError(posix.errno(), this));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintResultNode = insert(new TaintResultNode(getContext(), null));
            }

            final DynamicObject ret = createString(StringOperations.ropeFromByteList(new ByteList(cryptedString, 0, cryptedString.length - 1, ASCIIEncoding.INSTANCE, false)));

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(salt, ret);

            return ret;
        }

    }

    @CoreMethod(names = "delete!", rest = true, raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DeleteBangNode extends CoreMethodArrayArgumentsNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private ToStrNode toStr;

        public DeleteBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(context, sourceSection, null, null);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        public abstract DynamicObject executeDeleteBang(VirtualFrame frame, DynamicObject string, Object[] args);

        @Specialization(guards = "isEmpty(string)")
        public DynamicObject deleteBangEmpty(DynamicObject string, Object[] args) {
            return nil();
        }

        @Specialization(guards = "!isEmpty(string)")
        public Object deleteBang(VirtualFrame frame, DynamicObject string, Object[] args,
                @Cached("create()") BranchProfile errorProfile) {
            if (args.length == 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentErrorEmptyVarargs(this));
            }

            DynamicObject[] otherStrings = new DynamicObject[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeToStr(frame, args[i]);
            }

            return deleteBangSlow(string, otherStrings);
        }

        @TruffleBoundary
        private Object deleteBangSlow(DynamicObject string, DynamicObject[] otherStrings) {
            assert RubyGuards.isRubyString(string);

            DynamicObject otherString = otherStrings[0];
            Encoding enc = checkEncodingNode.executeCheckEncoding(string, otherString);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherString),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                assert RubyGuards.isRubyString(otherStrings[i]);

                enc = checkEncodingNode.executeCheckEncoding(string, otherStrings[i]);
                tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStrings[i]), squeeze, tables, false, enc);
            }

            final CodeRangeable buffer = StringOperations.getCodeRangeableReadWrite(string, checkEncodingNode);
            if (StringSupport.delete_bangCommon19(buffer, squeeze, tables, enc) == null) {
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
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
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
                throw new RaiseException(coreExceptions().encodingCompatibilityErrorIncompatibleWithOperation(encoding, this));
            }

            if (emptyStringProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final byte[] outputBytes = rope.getBytesCopy();
            final boolean modified = multiByteDowncase(encoding, outputBytes, 0, outputBytes.length);

            if (modifiedProfile.profile(modified)) {
                StringOperations.setRope(string, makeLeafRopeNode.executeMake(outputBytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                return string;
            } else {
                return nil();
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

    @CoreMethod(names = "each_byte", needsBlock = true, enumeratorSize = "bytesize")
    public abstract static class EachByteNode extends YieldingCoreMethodNode {

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

    @CoreMethod(names = "each_char", needsBlock = true, enumeratorSize = "size")
    @ImportStatic(StringGuards.class)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "!isBrokenCodeRange(string)")
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

        @Specialization(guards = "isBrokenCodeRange(string)")
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintResultNode = insert(new TaintResultNode(getContext(), null));
            }

            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope);

            return taintResultNode.maybeTaint(string, ret);
        }
    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject encoding(DynamicObject string) {
            return getContext().getEncodingManager().getRubyEncoding(StringOperations.encoding(string));
        }

    }

    @CoreMethod(names = "force_encoding", required = 1, raiseIfFrozenSelf = true)
    public abstract static class ForceEncodingNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode;
        @Child private ToStrNode toStrNode;

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(null, null, null);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(encodingName)")
        public DynamicObject forceEncodingString(DynamicObject string, DynamicObject encodingName,
                                                 @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile,
                                                 @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile) {
            final DynamicObject encoding = getContext().getEncodingManager().getRubyEncoding(StringOperations.decodeUTF8(encodingName));
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            return forceEncodingString(string, toStrNode.executeToStr(frame, encoding), differentEncodingProfile, mutableRopeProfile);
        }

    }

    @CoreMethod(names = "getbyte", required = 1, lowerFixnum = 1)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode ropeGetByteNode;

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            ropeGetByteNode = RopeNodesFactory.GetByteNodeGen.create(null, null);
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

        @Specialization
        public int hash(DynamicObject string) {
            return rope(string).hashCode();
        }

    }

    @CoreMethod(names = "initialize", optional = 1, taintFrom = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private IsFrozenNode isFrozenNode;
        @Child private ToStrNode toStrNode;

        @Specialization
        public DynamicObject initialize(DynamicObject self, NotProvided from) {
            return self;
        }

        @Specialization
        public DynamicObject initializeJavaString(DynamicObject self, String from) {
            raiseIfFrozen(self);
            StringOperations.setRope(self, StringOperations.encodeRope(from, ASCIIEncoding.INSTANCE));
            return self;
        }

        @Specialization(guards = "isRubyString(from)")
        public DynamicObject initialize(DynamicObject self, DynamicObject from) {
            raiseIfFrozen(self);

            StringOperations.setRope(self, rope(from));

            return self;
        }

        @Specialization(guards = { "!isRubyString(from)", "!isString(from)", "wasProvided(from)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject self, Object from) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
            }

            return initialize(self, toStrNode.executeToStr(frame, from));
        }

        protected void raiseIfFrozen(Object object) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), null, null));
            }
            isFrozenNode.raiseIfFrozen(object);
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "self == from")
        public Object initializeCopySelfIsSameAsFrom(DynamicObject self, DynamicObject from) {
            return self;
        }


        @Specialization(guards = { "self != from", "isRubyString(from)" })
        public Object initializeCopy(DynamicObject self, DynamicObject from,
                                     @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile) {
            final Rope rope = rope(from);

            if (ropeBufferProfile.profile(rope instanceof RopeBuffer)) {
                StringOperations.setRope(self, ((RopeBuffer) rope).dup());
            } else {
                StringOperations.setRope(self, rope);
            }

            return self;
        }

    }

    @CoreMethod(names = "insert", required = 2, lowerFixnum = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "otherString")
    })
    public abstract static class InsertNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode appendNode;
        @Child private CharacterByteIndexNode characterByteIndexNode;
        @Child private CheckCharacterIndexNode checkCharacterIndexNode;
        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private RopeNodes.MakeConcatNode prependMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode leftMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode rightMakeConcatNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            characterByteIndexNode = StringNodesFactory.CharacterByteIndexNodeFactory.create(new RubyNode[] {});
            checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(context, sourceSection, null, null);
            checkCharacterIndexNode = StringNodesFactory.CheckCharacterIndexNodeGen.create(null, null);
            leftMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            rightMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            leftMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            rightMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeGen.create(index);
        }

        @CreateCast("otherString") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(null, null, other);
        }

        @Specialization(guards = { "indexAtStartBound(index)", "isRubyString(other)" })
        public Object insertPrepend(DynamicObject string, int index, DynamicObject other) {
            final Rope left = rope(other);
            final Rope right = rope(string);

            final Encoding compatibleEncoding = checkEncodingNode.executeCheckEncoding(string, other);

            if (prependMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                prependMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            StringOperations.setRope(string, prependMakeConcatNode.executeMake(left, right, compatibleEncoding));

            return taintResultNode.maybeTaint(other, string);
        }

        @Specialization(guards = { "indexAtEndBound(index)", "isRubyString(other)" })
        public Object insertAppend(VirtualFrame frame, DynamicObject string, int index, DynamicObject other) {
            if (appendNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            appendNode.call(frame, string, "append", other);

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
            final Encoding compatibleEncoding = checkEncodingNode.executeCheckEncoding(string, other);

            final int normalizedIndex = checkCharacterIndexNode.executeCheck(source, index);
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

    @ImportStatic(StringGuards.class)
    @NodeChildren({ @NodeChild("string"), @NodeChild("index") })
    public static abstract class CheckCharacterIndexNode extends RubyNode {

        public abstract int executeCheck(Rope string, int index);

        @Specialization
        protected int check(Rope rope, int characterIndex,
                            @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                            @Cached("create()") BranchProfile errorProfile) {
            final int characterLength = rope.characterLength();

            if (characterIndex > characterLength) {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(characterIndex, this));
            }

            if (negativeIndexProfile.profile(characterIndex < 0)) {
                if (-characterIndex > characterLength) {
                    errorProfile.enter();
                    throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(characterIndex, this));
                }

                characterIndex += characterLength;
            }

            return characterIndex;
        }

    }

    @CoreMethod(names = "lstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class LstripBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public LstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
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
            final int s = 0;
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
        public Object lstripBang(DynamicObject string,
                                 @Cached("create()") RopeNodes.GetCodePointNode getCodePointNode) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#multiByteLStrip.

            final Rope rope = rope(string);
            final Encoding enc = RopeOperations.STR_ENC_GET(rope);
            final int s = 0;
            final int end = s + rope.byteLength();

            int p = s;
            while (p < end) {
                int c = getCodePointNode.executeGetCodePoint(rope, p);
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

    @CoreMethod(names = "ord")
    @ImportStatic(StringGuards.class)
    public abstract static class OrdNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isEmpty(string)")
        public int ordEmpty(DynamicObject string) {
            throw new RaiseException(coreExceptions().argumentError("empty string", this));
        }

        @Specialization(guards = "!isEmpty(string)")
        public int ord(DynamicObject string,
                       @Cached("create()") RopeNodes.GetCodePointNode getCodePointNode) {
            return getCodePointNode.executeGetCodePoint(rope(string), 0);
        }

    }

    @CoreMethod(names = "replace", required = 1, raiseIfFrozenSelf = true, taintFrom = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class ReplaceNode extends CoreMethodNode {

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(null, null, other);
        }

        @Specialization(guards = "string == other")
        public DynamicObject replaceStringIsSameAsOther(DynamicObject string, DynamicObject other) {
            return string;
        }


        @Specialization(guards = { "string != other", "isRubyString(other)" })
        public DynamicObject replace(DynamicObject string, DynamicObject other,
                                     @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile) {
            final Rope rope = rope(other);

            if (ropeBufferProfile.profile(rope instanceof RopeBuffer)) {
                StringOperations.setRope(string, ((RopeBuffer) rope).dup());
            } else {
                StringOperations.setRope(string, rope);
            }

            return string;
        }

    }

    @CoreMethod(names = "rstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class RstripBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public RstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
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
        public Object rstripBang(DynamicObject string,
                                 @Cached("create()") RopeNodes.GetCodePointNode getCodePointNode) {
            // Taken from org.jruby.RubyString#rstrip_bang19 and org.jruby.RubyString#multiByteRStrip19.

            final Rope rope = rope(string);
            final Encoding enc = RopeOperations.STR_ENC_GET(rope);
            final byte[] bytes = rope.getBytes();
            final int start = 0;
            final int end = rope.byteLength();

            int endp = end;
            int prev;
            while ((prev = prevCharHead(enc, bytes, start, endp, end)) != -1) {
                int point = getCodePointNode.executeGetCodePoint(rope, prev);
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
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public DynamicObject swapcaseSingleByte(DynamicObject string,
                                                @Cached("createBinaryProfile()") ConditionProfile emptyStringProfile,
                                                @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#swapcase_bang19.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();

            if (enc.isDummy()) {
                throw new RaiseException(coreExceptions().encodingCompatibilityErrorIncompatibleWithOperation(enc, this));
            }

            if (emptyStringProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final int s = 0;
            final int end = s + rope.byteLength();
            final byte[] bytes = rope.getBytesCopy();

            if (singleByteOptimizableProfile.profile(rope.isSingleByteOptimizable())) {
                if (StringSupport.singleByteSwapcase(bytes, s, end)) {
                    StringOperations.setRope(string, makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                    return string;
                }
            } else {
                if (StringSupport.multiByteSwapcase(enc, bytes, s, end)) {
                    StringOperations.setRope(string, makeLeafRopeNode.executeMake(bytes, rope.getEncoding(), rope.getCodeRange(), rope.characterLength()));

                    return string;
                }
            }

            return nil();
        }
    }

    @CoreMethod(names = "dump", taintFrom = 0)
    @ImportStatic(StringGuards.class)
    public abstract static class DumpNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public DumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization(guards = "isAsciiCompatible(string)")
        public DynamicObject dumpAsciiCompatible(DynamicObject string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);
            outputBytes.setEncoding(encoding(string));

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT));

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

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT));

            return result;
        }

        @TruffleBoundary
        private ByteList dumpCommon(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            return dumpCommon(StringOperations.getByteListReadOnly(string));
        }

        private ByteList dumpCommon(ByteList byteList) {
            ByteList buf = null;
            Encoding enc = byteList.getEncoding();

            int p = byteList.getBegin();
            int end = p + byteList.getRealSize();
            byte[]bytes = byteList.getUnsafeBytes();

            int len = 2;
            while (p < end) {
                int c = bytes[p++] & 0xff;

                switch (c) {
                    case '"':case '\\':case '\n':case '\r':case '\t':case '\f':
                    case '\013': case '\010': case '\007': case '\033':
                        len += 2;
                        break;
                    case '#':
                        len += isEVStr(bytes, p, end) ? 2 : 1;
                        break;
                    default:
                        if (ASCIIEncoding.INSTANCE.isPrint(c)) {
                            len++;
                        } else {
                            if (enc.isUTF8()) {
                                int n = preciseLength(enc, bytes, p - 1, end) - 1;
                                if (n > 0) {
                                    if (buf == null) buf = new ByteList();
                                    int cc = codePointX(enc, bytes, p - 1, end);
                                    buf.append(String.format("%x", cc).getBytes(StandardCharsets.US_ASCII));
                                    len += buf.getRealSize() + 4;
                                    buf.setRealSize(0);
                                    p += n;
                                    break;
                                }
                            }
                            len += 4;
                        }
                        break;
                }
            }

            if (!enc.isAsciiCompatible()) {
                len += ".force_encoding(\"".length() + enc.getName().length + "\")".length();
            }

            ByteList outBytes = new ByteList(len);
            byte out[] = outBytes.getUnsafeBytes();
            int q = 0;
            p = byteList.getBegin();
            end = p + byteList.getRealSize();

            out[q++] = '"';
            while (p < end) {
                int c = bytes[p++] & 0xff;
                if (c == '"' || c == '\\') {
                    out[q++] = '\\';
                    out[q++] = (byte)c;
                } else if (c == '#') {
                    if (isEVStr(bytes, p, end)) out[q++] = '\\';
                    out[q++] = '#';
                } else if (c == '\n') {
                    out[q++] = '\\';
                    out[q++] = 'n';
                } else if (c == '\r') {
                    out[q++] = '\\';
                    out[q++] = 'r';
                } else if (c == '\t') {
                    out[q++] = '\\';
                    out[q++] = 't';
                } else if (c == '\f') {
                    out[q++] = '\\';
                    out[q++] = 'f';
                } else if (c == '\013') {
                    out[q++] = '\\';
                    out[q++] = 'v';
                } else if (c == '\010') {
                    out[q++] = '\\';
                    out[q++] = 'b';
                } else if (c == '\007') {
                    out[q++] = '\\';
                    out[q++] = 'a';
                } else if (c == '\033') {
                    out[q++] = '\\';
                    out[q++] = 'e';
                } else if (ASCIIEncoding.INSTANCE.isPrint(c)) {
                    out[q++] = (byte)c;
                } else {
                    out[q++] = '\\';
                    if (enc.isUTF8()) {
                        int n = preciseLength(enc, bytes, p - 1, end) - 1;
                        if (n > 0) {
                            int cc = codePointX(enc, bytes, p - 1, end);
                            p += n;
                            outBytes.setRealSize(q);
                            outBytes.append(String.format("u{%x}", cc).getBytes(StandardCharsets.US_ASCII));
                            q = outBytes.getRealSize();
                            continue;
                        }
                    }
                    outBytes.setRealSize(q);
                    outBytes.append(String.format("x%02X", c).getBytes(StandardCharsets.US_ASCII));
                    q = outBytes.getRealSize();
                }
            }
            out[q++] = '"';
            outBytes.setRealSize(q);
            assert out == outBytes.getUnsafeBytes(); // must not reallocate

            return outBytes;
        }

        private static boolean isEVStr(byte[] bytes, int p, int end) {
            return p < end ? isEVStr(bytes[p] & 0xff) : false;
        }

        private static boolean isEVStr(int c) {
            return c == '$' || c == '@' || c == '{';
        }

        // rb_enc_precise_mbclen
        private static int preciseLength(Encoding enc, byte[]bytes, int p, int end) {
            if (p >= end) return -1 - (1);
            int n = enc.length(bytes, p, end);
            if (n > end - p) return MBCLEN_NEEDMORE(n - (end - p));
            return n;
        }

        private static int MBCLEN_NEEDMORE(int n) {
            return -1 - n;
        }

        private static int codePoint(Encoding enc, byte[] bytes, int p, int end) {
            if (p >= end) throw new IllegalArgumentException("empty string");
            int cl = preciseLength(enc, bytes, p, end);
            if (cl <= 0) throw new IllegalArgumentException("invalid byte sequence in " + enc);
            return enc.mbcToCode(bytes, p, end);
        }

        private int codePointX(Encoding enc, byte[] bytes, int p, int end) {
            try {
                return codePoint(enc, bytes, p, end);
            } catch (IllegalArgumentException e) {
                throw new RaiseException(getContext().getCoreExceptions().argumentError(e.getMessage(), this));
            }
        }
    }

    @CoreMethod(names = "setbyte", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "value")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class SetByteNode extends CoreMethodNode {

        @Child private CheckByteIndexNode checkByteIndexNode;
        @Child private RopeNodes.MakeConcatNode composedMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode middleMakeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            checkByteIndexNode = StringNodesFactory.CheckByteIndexNodeGen.create(null, null);
            composedMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            middleMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
            leftMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            rightMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return FixnumLowerNodeGen.create(null, null, ToIntNodeGen.create(index));
        }

        @CreateCast("value") public RubyNode coerceValueToInt(RubyNode value) {
            return FixnumLowerNodeGen.create(null, null, ToIntNodeGen.create(value));
        }

        public abstract int executeSetByte(DynamicObject string, int index, Object value);

        @Specialization(guards = "!isRopeBuffer(string)")
        public int setByte(DynamicObject string, int index, int value) {
            final Rope rope = rope(string);
            final int normalizedIndex = checkByteIndexNode.executeCheck(rope, index);

            final Rope left = leftMakeSubstringNode.executeMake(rope, 0, normalizedIndex);
            final Rope right = rightMakeSubstringNode.executeMake(rope, normalizedIndex + 1, rope.byteLength() - normalizedIndex - 1);
            final Rope middle = makeLeafRopeNode.executeMake(new byte[] { (byte) value }, rope.getEncoding(), CodeRange.CR_UNKNOWN, NotProvided.INSTANCE);
            final Rope composed = composedMakeConcatNode.executeMake(middleMakeConcatNode.executeMake(left, middle, rope.getEncoding()), right, rope.getEncoding());

            StringOperations.setRope(string, composed);

            return value;
        }

        @Specialization(guards = "isRopeBuffer(string)")
        public int setByteRopeBuffer(DynamicObject string, int index, int value) {
            final RopeBuffer rope = (RopeBuffer) rope(string);
            final int normalizedIndex = checkByteIndexNode.executeCheck(rope, index);

            rope.getByteList().set(normalizedIndex, value);

            return value;
        }

    }

    @ImportStatic(StringGuards.class)
    @NodeChildren({ @NodeChild("string"), @NodeChild("index") })
    public static abstract class CheckByteIndexNode extends RubyNode {

        public abstract int executeCheck(Rope string, int index);

        @Specialization
        protected int checkIndex(Rope rope, int byteIndex,
                                 @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                                 @Cached("create()") BranchProfile errorProfile) {
            final int byteLength = rope.byteLength();

            if (byteIndex >= byteLength) {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(byteIndex, this));
            }

            if (negativeIndexProfile.profile(byteIndex < 0)) {
                if (-byteIndex > byteLength) {
                    errorProfile.enter();
                    throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(byteIndex, this));
                }

                byteIndex += byteLength;
            }

            return byteIndex;
        }

    }

    @CoreMethod(names = {"size", "length"})
    @ImportStatic(StringGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int size(DynamicObject string,
                        @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile,
                        @Cached("createBinaryProfile()") ConditionProfile isSingleByteOptimizableRopeBufferProfile) {
            final Rope rope = rope(string);

            if (ropeBufferProfile.profile(rope instanceof RopeBuffer)) {
                if (isSingleByteOptimizableRopeBufferProfile.profile(rope.isSingleByteOptimizable())) {
                    return ((RopeBuffer) rope).getByteList().realSize();
                } else {
                    final ByteList byteList = ((RopeBuffer) rope).getByteList();
                    return RopeOperations.strLength(rope.getEncoding(), byteList.unsafeBytes(), byteList.begin(), byteList.realSize());
                }
            } else {
                return rope.characterLength();
            }
        }

    }

    @CoreMethod(names = "squeeze!", rest = true, raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class SqueezeBangNode extends CoreMethodArrayArgumentsNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private ToStrNode toStrNode;

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
                toStrNode = insert(ToStrNodeGen.create(getContext(), null, null));
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

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(getContext(), null, null, null));
            }

            final Rope rope = rope(string);
            final ByteList buffer = RopeOperations.toByteListCopy(rope);

            DynamicObject otherStr = otherStrings[0];
            Rope otherRope = rope(otherStr);
            Encoding enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(RopeOperations.getByteListReadOnly(otherRope), squeeze, null, true, enc);

            boolean singlebyte = rope.isSingleByteOptimizable() && otherRope.isSingleByteOptimizable();

            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];
                otherRope = rope(otherStr);
                enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
                singlebyte = singlebyte && otherRope.isSingleByteOptimizable();
                tables = StringSupport.trSetupTable(RopeOperations.getByteListReadOnly(otherRope), squeeze, tables, false, enc);
            }

            if (singleByteOptimizableProfile.profile(singlebyte)) {
                if (! StringSupport.singleByteSqueeze(buffer, squeeze)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            } else {
                if (! StringSupport.multiByteSqueeze(buffer, squeeze, tables, enc, true)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, StringOperations.ropeFromByteList(buffer));
                }
            }

            return string;
        }

        @TruffleBoundary
        private boolean squeezeCommonMultiByte(ByteList value, boolean squeeze[], StringSupport.TrTables tables, Encoding enc, boolean isArg) {
            return StringSupport.multiByteSqueeze(value, squeeze, tables, enc, isArg);
        }

        public static boolean zeroArgs(Object[] args) {
            return args.length == 0;
        }
    }

    @CoreMethod(names = "succ!", raiseIfFrozenSelf = true)
    public abstract static class SuccBangNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject succBang(DynamicObject string) {
            final Rope rope = rope(string);

            if (! rope.isEmpty()) {
                final ByteList succByteList = StringSupport.succCommon(StringOperations.getByteListReadOnly(string));

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
            int p = 0;
            final int len = rope.byteLength();
            final int end = p + len;

            if (bits >= 8 * 8) { // long size * bits in byte
                Object sum = 0;
                while (p < end) {
                    sum = addNode.call(frame, sum, "+", bytes[p++] & 0xff);
                }
                if (bits != 0) {
                    final Object mod = shiftNode.call(frame, 1, "<<", bits);
                    sum = andNode.call(frame, sum, "&", subNode.call(frame, mod, "-", 1));
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
        public Object sum(VirtualFrame frame,
                          DynamicObject string,
                          Object bits,
                          @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "sum Rubinius::Type.coerce_to(bits, Fixnum, :to_int)", "bits", bits);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

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

        @Specialization(guards = "!isStringSubclass(string)")
        public DynamicObject toS(DynamicObject string) {
            return string;
        }

        @Specialization(guards = "isStringSubclass(string)")
        public Object toSOnSubclass(
                VirtualFrame frame,
                DynamicObject string,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "''.replace(self)", "self", string);
        }

        public boolean isStringSubclass(DynamicObject string) {
            return Layouts.BASIC_OBJECT.getLogicalClass(string) != coreLibrary().getStringClass();
        }

    }

    @CoreMethod(names = {"to_sym", "intern"})
    public abstract static class ToSymNode extends CoreMethodArrayArgumentsNode {

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
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
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
                reversedBytes[len - i - 1] = originalBytes[i];
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

            return rope(string).characterLength() <= 1;
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

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private DeleteBangNode deleteBangNode;

        @CreateCast("fromStr") public RubyNode coerceFromStrToString(RubyNode fromStr) {
            return ToStrNodeGen.create(null, null, fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(null, null, toStr);
        }

        @Specialization(guards = "isEmpty(self)")
        public Object trBangEmpty(DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(self)", "isRubyString(fromStr)", "isRubyString(toStr)" })
        public Object trBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (rope(toStr).isEmpty()) {
                if (deleteBangNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(getContext(), null, new RubyNode[] {}));
                }

                return deleteBangNode.executeDeleteBang(frame, self, new DynamicObject[] { fromStr });
            }

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(getContext(), null, null, null));
            }

            return StringNodesHelper.trTransHelper(getContext(), checkEncodingNode, self, fromStr, toStr, false);
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

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private DeleteBangNode deleteBangNode;

        @CreateCast("fromStr") public RubyNode coerceFromStrToString(RubyNode fromStr) {
            return ToStrNodeGen.create(null, null, fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(null, null, toStr);
        }

        @Specialization(guards = "isEmpty(self)")
        public DynamicObject trSBangEmpty(DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            return nil();
        }

        @Specialization(guards = { "!isEmpty(self)", "isRubyString(fromStr)", "isRubyString(toStr)" })
        public Object trSBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (rope(toStr).isEmpty()) {
                if (deleteBangNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(getContext(), null, new RubyNode[] {}));
                }

                return deleteBangNode.executeDeleteBang(frame, self, new DynamicObject[] { fromStr });
            }

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(getContext(), null, null, null));
            }

            return StringNodesHelper.trTransHelper(getContext(), checkEncodingNode, self, fromStr, toStr, true);
        }
    }

    @CoreMethod(names = "unpack", required = 1, taintFrom = 1)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class UnpackNode extends ArrayCoreMethodNode {

        @Child private TaintNode taintNode;

        private final BranchProfile exceptionProfile = BranchProfile.create();

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
            final DynamicObject array = createArray(result.getOutput(), result.getOutputLength());

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    taintNode = insert(TaintNodeGen.create(getContext(), null, null));
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
        public Object unpack(
                VirtualFrame frame,
                DynamicObject array,
                Object format,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "unpack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            return new UnpackCompiler(getContext(), this).compile(format.toString());
        }

        protected int getCacheLimit() {
            return getContext().getOptions().UNPACK_CACHE;
        }

    }

    @CoreMethod(names = "upcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class UpcaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public UpcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
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
                throw new RaiseException(coreExceptions().encodingCompatibilityErrorIncompatibleWithOperation(encoding, this));
            }

            if (rope.isEmpty()) {
                return nil();
            }

            final ByteList bytes = RopeOperations.toByteListCopy(rope);
            final boolean modified = multiByteUpcase(encoding, bytes.unsafeBytes(), bytes.begin(), bytes.realSize());
            if (modified) {
                StringOperations.setRope(string, StringOperations.ropeFromByteList(bytes, rope.getCodeRange()));

                return string;
            } else {
                return nil();
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

        @Child private RopeNodes.GetCodePointNode getCodePointNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public CapitalizeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getCodePointNode = RopeNodes.GetCodePointNode.create();
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
        }

        @Specialization
        @TruffleBoundary(throwsControlFlowException = true)
        public DynamicObject capitalizeBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#capitalize_bang19.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();

            if (enc.isDummy()) {
                throw new RaiseException(coreExceptions().encodingCompatibilityErrorIncompatibleWithOperation(enc, this));
            }

            if (rope.isEmpty()) {
                return nil();
            }

            int s = 0;
            int end = s + rope.byteLength();
            byte[] bytes = rope.getBytesCopy();
            boolean modify = false;

            int c = getCodePointNode.executeGetCodePoint(rope, s);
            if (enc.isLower(c)) {
                enc.codeToMbc(StringSupport.toUpper(enc, c), bytes, s);
                modify = true;
            }

            s += StringSupport.codeLength(enc, c);
            while (s < end) {
                c = getCodePointNode.executeGetCodePoint(rope, s);
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

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeSubstringNode = RopeNodes.MakeSubstringNode.createX();
        }

        @Specialization
        public DynamicObject clear(DynamicObject string) {
            StringOperations.setRope(string, makeSubstringNode.executeMake(rope(string), 0, 0));

            return string;
        }
    }

    public static class StringNodesHelper {

        @TruffleBoundary
        private static Object trTransHelper(RubyContext context, EncodingNodes.CheckEncodingNode checkEncodingNode,
                                            DynamicObject self, DynamicObject fromStr,
                                            DynamicObject toStr, boolean sFlag) {
            assert RubyGuards.isRubyString(self);
            assert RubyGuards.isRubyString(fromStr);
            assert RubyGuards.isRubyString(toStr);

            final CodeRangeable buffer = StringOperations.getCodeRangeableReadWrite(self, checkEncodingNode);
            final CodeRangeable ret = StringSupport.trTransHelper(buffer,
                    StringOperations.getCodeRangeableReadOnly(fromStr, checkEncodingNode),
                    StringOperations.getCodeRangeableReadOnly(toStr, checkEncodingNode),
                    sFlag);

            if (ret == null) {
                return context.getCoreLibrary().getNilObject();
            }

            StringOperations.setRope(self, StringOperations.ropeFromByteList(buffer.getByteList(), buffer.getCodeRange()));

            return self;
        }
    }

    @Primitive(name = "character_printable_p")
    public static abstract class CharacterPrintablePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public boolean isCharacterPrintable(DynamicObject character) {
            final Rope rope = rope(character);
            final Encoding encoding = rope.getEncoding();

            final int codepoint = encoding.mbcToCode(rope.getBytes(), 0, rope.byteLength());

            return encoding.isPrint(codepoint);
        }

    }

    @Primitive(name = "string_append")
    public static abstract class StringAppendPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;
        @Child private RopeNodes.MakeConcatNode makeConcatNode;

        public StringAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(context, sourceSection, null, null);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
        }

        public abstract DynamicObject executeStringAppend(DynamicObject string, DynamicObject other);

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringAppend(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding compatibleEncoding = checkEncodingNode.executeCheckEncoding(string, other);

            StringOperations.setRope(string, makeConcatNode.executeMake(left, right, compatibleEncoding));

            return string;
        }

    }

    @Primitive(name = "string_awk_split")
    public static abstract class StringAwkSplitPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.GetCodePointNode getCodePointNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public StringAwkSplitPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            getCodePointNode = RopeNodes.GetCodePointNode.create();
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject stringAwkSplit(DynamicObject string, int lim) {
            final List<DynamicObject> ret = new ArrayList<>();
            final Rope rope = rope(string);
            final boolean limit = lim > 0;
            int i = lim > 0 ? 1 : 0;

            byte[]bytes = rope.getBytes();
            int p = 0;
            int ptr = p;
            int len = rope.byteLength();
            int end = p + len;
            Encoding enc = rope.getEncoding();
            boolean skip = true;

            int e = 0, b = 0;
            final boolean singlebyte = rope.isSingleByteOptimizable();
            while (p < end) {
                final int c;
                if (singlebyte) {
                    c = bytes[p++] & 0xff;
                } else {
                    c = getCodePointNode.executeGetCodePoint(rope, p);
                    p += StringSupport.length(enc, bytes, p, end);
                }

                if (skip) {
                    if (enc.isSpace(c)) {
                        b = p - ptr;
                    } else {
                        e = p - ptr;
                        skip = false;
                        if (limit && lim <= i) break;
                    }
                } else {
                    if (enc.isSpace(c)) {
                        ret.add(makeString(string, b, e - b));
                        skip = true;
                        b = p - ptr;
                        if (limit) i++;
                    } else {
                        e = p - ptr;
                    }
                }
            }

            if (len > 0 && (limit || len > b || lim < 0)) ret.add(makeString(string, b, len - b));

            Object[] objects = ret.toArray();
            return createArray(objects, objects.length);
        }

        // because the factory is not constant
        @TruffleBoundary
        private DynamicObject makeString(DynamicObject source, int index, int length) {
            assert RubyGuards.isRubyString(source);

            final Rope rope = makeSubstringNode.executeMake(rope(source), index, length);

            final DynamicObject ret = Layouts.STRING.createString(Layouts.CLASS.getInstanceFactory(Layouts.BASIC_OBJECT.getLogicalClass(source)), rope);
            taintResultNode.maybeTaint(source, ret);

            return ret;
        }
    }

    @Primitive(name = "string_byte_substring")
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "index"),
            @NodeChild(type = RubyNode.class, value = "length")
    })
    public static abstract class StringByteSubstringPrimitiveNode extends PrimitiveNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public static StringByteSubstringPrimitiveNode create(RubyContext context, SourceSection sourceSection) {
            return StringNodesFactory.StringByteSubstringPrimitiveNodeFactory.create(context, sourceSection, null, null, null);
        }

        public StringByteSubstringPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ArrayAttributeCastNodeGen.create(null, null, "index", index);
        }

        @CreateCast("length") public RubyNode coerceLengthToInt(RubyNode length) {
            return ArrayAttributeCastNodeGen.create(null, null, "length", length);
        }

        public Object executeStringByteSubstring(DynamicObject string, Object index, Object length) { return nil(); }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, int index, NotProvided length,
                                          @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile lengthTooLongProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile nilSubstringProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile emptySubstringProfile) {
            final DynamicObject subString = (DynamicObject) stringByteSubstring(string, index, 1, negativeLengthProfile, indexOutOfBoundsProfile, lengthTooLongProfile);

            if (nilSubstringProfile.profile(subString == nil())) {
                return subString;
            }

            if (emptySubstringProfile.profile(rope(subString).isEmpty())) {
                return nil();
            }

            return subString;
        }

        @Specialization
        public Object stringByteSubstring(DynamicObject string, int index, int length,
                                          @Cached("createBinaryProfile()") ConditionProfile negativeLengthProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile lengthTooLongProfile) {
            if (negativeLengthProfile.profile(length < 0)) {
                return nil();
            }

            final Rope rope = rope(string);
            final int stringByteLength = rope.byteLength();
            final int normalizedIndex = StringOperations.normalizeIndex(stringByteLength, index);

            if (indexOutOfBoundsProfile.profile(normalizedIndex < 0 || normalizedIndex > stringByteLength)) {
                return nil();
            }

            if (lengthTooLongProfile.profile(normalizedIndex + length > stringByteLength)) {
                length = rope.byteLength() - normalizedIndex;
            }

            final Rope substringRope = makeSubstringNode.executeMake(rope, normalizedIndex, length);
            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope);

            return taintResultNode.maybeTaint(string, result);
        }

        @Specialization(guards = "isRubyRange(range)")
        public Object stringByteSubstring(DynamicObject string, DynamicObject range, NotProvided length) {
            return null;
        }

    }

    @Primitive(name = "string_chr_at", lowerFixnum = 1)
    @ImportStatic(StringGuards.class)
    public static abstract class StringChrAtPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "indexOutOfBounds(string, byteIndex)")
        public Object stringChrAtOutOfBounds(DynamicObject string, int byteIndex) {
            return nil();
        }

        @Specialization(guards = { "!indexOutOfBounds(string, byteIndex)", "isSingleByteOptimizable(string)" })
        public Object stringChrAtSingleByte(DynamicObject string, int byteIndex,
                                            @Cached("create(getContext(), getSourceSection())") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, 1);
        }

        @Specialization(guards = { "!indexOutOfBounds(string, byteIndex)", "!isSingleByteOptimizable(string)" })
        public Object stringChrAt(DynamicObject string, int byteIndex,
                                  @Cached("create(getContext(), getSourceSection())") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
            // Taken from Rubinius's Character::create_from.

            final Rope rope = rope(string);
            final int end = rope.byteLength();
            final int c = preciseLength(rope, byteIndex, end);

            if (! StringSupport.MBCLEN_CHARFOUND_P(c)) {
                return nil();
            }

            final int n = StringSupport.MBCLEN_CHARFOUND_LEN(c);
            if (n + byteIndex > end) {
                return nil();
            }

            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, n);
        }

        @TruffleBoundary
        private int preciseLength(final Rope rope, final int p, final int end) {
            return StringSupport.preciseLength(rope.getEncoding(), rope.getBytes(), p, end);
        }

        protected static boolean indexOutOfBounds(DynamicObject string, int byteIndex) {
            return ((byteIndex < 0) || (byteIndex >= rope(string).byteLength()));
        }

    }

    @Primitive(name = "string_compare_substring")
    public static abstract class StringCompareSubstringPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyString(other)")
        public int stringCompareSubstring(VirtualFrame frame, DynamicObject string, DynamicObject other, int start, int size,
                @Cached("create()") BranchProfile errorProfile) {
            // Transliterated from Rubinius C++.

            final int stringLength = StringOperations.rope(string).characterLength();
            final int otherLength = StringOperations.rope(other).characterLength();

            if (start < 0) {
                start += otherLength;
            }

            if (start > otherLength) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError(formatError(start), this));
            }

            if (start < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().indexError(formatError(start), this));
            }

            if (start + size > otherLength) {
                size = otherLength - start;
            }

            if (size > stringLength) {
                size = stringLength;
            }

            final Rope rope = StringOperations.rope(string);
            final Rope otherRope = StringOperations.rope(other);

            // TODO (nirvdrum 21-Jan-16): Reimplement with something more friendly to rope byte[] layout?
            return ByteList.memcmp(rope.getBytes(), 0, size,
                    otherRope.getBytes(), start, size);
        }

        @TruffleBoundary
        private String formatError(int start) {
            return StringUtils.format("index %d out of string", start);
        }

    }

    @ImportStatic(StringGuards.class)
    @NodeChildren({ @NodeChild("first"), @NodeChild("second") })
    public static abstract class StringAreComparableNode extends RubyNode {

        public abstract boolean executeAreComparable(DynamicObject first, DynamicObject second);

        @Specialization(guards = "getEncoding(a) == getEncoding(b)")
        protected boolean sameEncoding(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Specialization(guards = "isEmpty(a)")
        protected boolean firstEmpty(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Specialization(guards = "isEmpty(b)")
        protected boolean secondEmpty(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Specialization(guards = { "is7Bit(a)", "is7Bit(b)" })
        protected boolean bothCR7bit(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Specialization(guards = { "is7Bit(a)", "isAsciiCompatible(b)" })
        protected boolean CR7bitASCII(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Specialization(guards = { "isAsciiCompatible(a)", "is7Bit(b)" })
        protected boolean ASCIICR7bit(DynamicObject a, DynamicObject b) {
            return true;
        }

        @Fallback
        protected boolean notCompatible(Object a, Object b) {
            return false;
        }

        protected static Encoding getEncoding(DynamicObject string) {
            return rope(string).getEncoding();
        }

    }

    @ImportStatic(StringGuards.class)
    @NodeChildren({ @NodeChild("first"), @NodeChild("second") })
    public static abstract class StringEqualNode extends RubyNode {

        @Child StringAreComparableNode areComparableNode;

        public abstract boolean executeStringEqual(DynamicObject string, DynamicObject other);

        @Specialization(guards = {
                "ropeReferenceEqual(string, other)"
        })
        public boolean stringEqualsRopeEquals(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "!areComparable(string, other)"
        })
        public boolean stringEqualNotComparable(DynamicObject string, DynamicObject other) {
            return false;
        }

        @Specialization(guards = {
                "areComparable(string, other)",
                "byteLength(string) != byteLength(other)"
        })
        public boolean stringEqualDifferentLength(DynamicObject string, DynamicObject other) {
            return false;
        }

        @Specialization(guards = {
                "areComparable(string, other)",
                "!ropeReferenceEqual(string, other)",
                "bytesReferenceEqual(string, other)"
        })
        public boolean stringEqualsBytesEquals(DynamicObject string, DynamicObject other) {
            return true;
        }

        @Specialization(guards = {
                "areComparable(string, other)",
                "!ropeReferenceEqual(string, other)",
                "byteLength(string) == 1",
                "byteLength(other) == 1",
                "hasRawBytes(string)",
                "hasRawBytes(other)"
        })
        public boolean equalCharacters(DynamicObject string, DynamicObject other) {
            final Rope a = rope(string);
            final Rope b = rope(other);

            return a.getRawBytes()[0] == b.getRawBytes()[0];
        }

        @Specialization(guards = {
                "areComparable(string, other)",
                "!ropeReferenceEqual(string, other)",
                "!bytesReferenceEqual(string, other)",
                "byteLength(string) == byteLength(other)"
        }, contains = "equalCharacters")
        public boolean fullEqual(DynamicObject string, DynamicObject other,
                                 @Cached("createBinaryProfile()") ConditionProfile hashCodesCalculatedProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile differentHashCodesProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile aHasRawBytesProfile,
                                 @Cached("createBinaryProfile()") ConditionProfile bHasRawBytesProfile) {
            final Rope a = rope(string);
            final Rope b = rope(other);

            if (hashCodesCalculatedProfile.profile(a.isHashCodeCalculated() && b.isHashCodeCalculated())) {
                if (differentHashCodesProfile.profile(a.hashCode() != b.hashCode())) {
                    return false;
                }
            }

            final byte[] aBytes;
            if (aHasRawBytesProfile.profile(a.getRawBytes() != null)) {
                aBytes = a.getRawBytes();
            } else {
                aBytes = a.getBytes();
            }

            final byte[] bBytes;
            if (bHasRawBytesProfile.profile(b.getRawBytes() != null)) {
                bBytes = b.getRawBytes();
            } else {
                bBytes = b.getBytes();
            }

            return arraysEquals(aBytes, bBytes);
        }

        private boolean arraysEquals(byte[] a, byte[] b) {
            assert a.length == b.length;

            for (int i = 0; i < a.length; i++) {
                if (a[i] != b[i]) {
                    return false;
                }
            }

            return true;
        }

        protected boolean areComparable(DynamicObject first, DynamicObject second) {
            if (areComparableNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                areComparableNode = insert(StringAreComparableNodeGen.create(null, null));
            }

            return areComparableNode.executeAreComparable(first, second);
        }

        protected static boolean ropeReferenceEqual(DynamicObject first, DynamicObject second) {
            return rope(first) == rope(second);
        }

        protected static boolean bytesReferenceEqual(DynamicObject first, DynamicObject second) {
            final Rope firstRope = rope(first);
            final Rope secondRope = rope(second);

            return firstRope.getRawBytes() != null &&
                    firstRope.getRawBytes() == secondRope.getRawBytes();
        }

        protected static int byteLength(DynamicObject string) {
            return rope(string).byteLength();
        }

        protected static boolean hasRawBytes(DynamicObject string) {
            return rope(string).getRawBytes() != null;
        }

    }

    @Primitive(name = "string_escape", needsSelf = false)
    public abstract static class StringEscapePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private IsTaintedNode isTaintedNode = IsTaintedNodeGen.create(null, null, null);
        @Child private TaintNode taintNode = TaintNodeGen.create(null, null, null);
        private final ConditionProfile taintedProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        public DynamicObject string_escape(DynamicObject string) {
            final DynamicObject result = create7BitString(rbStrEscape(StringOperations.getByteListReadOnly(string)), USASCIIEncoding.INSTANCE);

            if (taintedProfile.profile(isTaintedNode.isTainted(string))) {
                taintNode.executeTaint(result);
            }

            return result;
        }

        // MRI: rb_str_escape
        @TruffleBoundary
        private static ByteList rbStrEscape(ByteList str) {
            Encoding enc = str.getEncoding();
            ByteList strBL = str;
            byte[] pBytes = strBL.unsafeBytes();
            int p = strBL.begin();
            int pend = p + strBL.realSize();
            int prev = p;
            ByteList result = new ByteList();
            boolean unicode_p = enc.isUnicode();
            boolean asciicompat = enc.isAsciiCompatible();

            while (p < pend) {
                int c, cc;
                int n = enc.length(pBytes, p, pend);
                if (!MBCLEN_CHARFOUND_P(n)) {
                    if (p > prev) result.append(pBytes, prev, p - prev);
                    n = enc.minLength();
                    if (pend < p + n)
                        n = (pend - p);
                    while ((n--) > 0) {
                        result.append(String.format("\\x%02X", (long) (pBytes[p] & 0377)).getBytes(StandardCharsets.US_ASCII));
                        prev = ++p;
                    }
                    continue;
                }
                n = MBCLEN_CHARFOUND_LEN(n);
                c = enc.mbcToCode(pBytes, p, pend);
                p += n;
                switch (c) {
                    case '\n': cc = 'n'; break;
                    case '\r': cc = 'r'; break;
                    case '\t': cc = 't'; break;
                    case '\f': cc = 'f'; break;
                    case '\013': cc = 'v'; break;
                    case '\010': cc = 'b'; break;
                    case '\007': cc = 'a'; break;
                    case 033: cc = 'e'; break;
                    default: cc = 0; break;
                }
                if (cc != 0) {
                    if (p - n > prev) result.append(pBytes, prev, p - n - prev);
                    result.append('\\');
                    result.append((byte) cc);
                    prev = p;
                }
                else if (asciicompat && Encoding.isAscii(c) && (c < 0x7F && c > 31 /*ISPRINT(c)*/)) {
                }
                else {
                    if (p - n > prev) result.append(pBytes, prev, p - n - prev);

                    if (unicode_p && (c & 0xFFFFFFFFL) < 0x7F && Encoding.isAscii(c) && ASCIIEncoding.INSTANCE.isPrint(c)) {
                        result.append(String.format("%c", (char) (c & 0xFFFFFFFFL)).getBytes(StandardCharsets.US_ASCII));
                    } else {
                        result.append(String.format(escapedCharFormat(c, unicode_p), c & 0xFFFFFFFFL).getBytes(StandardCharsets.US_ASCII));
                    }

                    prev = p;
                }
            }
            if (p > prev) result.append(pBytes, prev, p - prev);

            return result;
        }

        private static int MBCLEN_CHARFOUND_LEN(int r) {
            return r;
        }

        // MBCLEN_CHARFOUND_P, ONIGENC_MBCLEN_CHARFOUND_P
        private static boolean MBCLEN_CHARFOUND_P(int r) {
            return 0 < r;
        }

        private static String escapedCharFormat(int c, boolean isUnicode) {
            String format;
            // c comparisons must be unsigned 32-bit
            if (isUnicode) {

                if ((c & 0xFFFFFFFFL) < 0x7F && Encoding.isAscii(c) && ASCIIEncoding.INSTANCE.isPrint(c)) {
                    throw new UnsupportedOperationException();
                } else if (c < 0x10000) {
                    format = "\\u%04X";
                } else {
                    format = "\\u{%X}";
                }
            } else {
                if ((c & 0xFFFFFFFFL) < 0x100) {
                    format = "\\x%02X";
                } else {
                    format = "\\x{%X}";
                }
            }
            return format;
        }

    }

    @Primitive(name = "string_find_character", lowerFixnum = 1)
    @ImportStatic(StringGuards.class)
    public static abstract class StringFindCharacterNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public StringFindCharacterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
            makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        }

        @Specialization(guards = "offset < 0")
        public Object stringFindCharacterNegativeOffset(DynamicObject string, int offset) {
            return nil();
        }

        @Specialization(guards = "offsetTooLarge(string, offset)")
        public Object stringFindCharacterOffsetTooLarge(DynamicObject string, int offset) {
            return nil();
        }

        @Specialization(guards = { "offset >= 0", "!offsetTooLarge(string, offset)", "isSingleByteOptimizable(string)" })
        public Object stringFindCharacterSingleByte(DynamicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            final Rope rope = rope(string);

            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, 1));

            return propagate(string, ret);
        }

        @TruffleBoundary
        @Specialization(guards = { "offset >= 0", "!offsetTooLarge(string, offset)", "!isSingleByteOptimizable(string)" })
        public Object stringFindCharacter(DynamicObject string, int offset) {
            // Taken from Rubinius's String::find_character.

            final Rope rope = rope(string);

            final Encoding enc = rope.getEncoding();
            final int clen = StringSupport.preciseLength(enc, rope.getBytes(), offset, offset + enc.maxLength());

            final DynamicObject ret;
            if (StringSupport.MBCLEN_CHARFOUND_P(clen)) {
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, clen));
            } else {
                ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), makeSubstringNode.executeMake(rope, offset, 1));
            }

            return propagate(string, ret);
        }

        private Object propagate(DynamicObject string, DynamicObject ret) {
            return maybeTaint(string, ret);
        }

        private Object maybeTaint(DynamicObject source, DynamicObject value) {
            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintResultNode = insert(new TaintResultNode(getContext(), null));
            }

            return taintResultNode.maybeTaint(source, value);
        }

        protected static boolean offsetTooLarge(DynamicObject string, int offset) {
            assert RubyGuards.isRubyString(string);

            return offset >= rope(string).byteLength();
        }

    }

    @Primitive(name = "string_from_codepoint", needsSelf = false, lowerFixnum = 1)
    public static abstract class StringFromCodepointPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = { "isRubyEncoding(rubyEncoding)", "isSimple(longCode, rubyEncoding)", "isCodepoint(longCode)" })
        public DynamicObject stringFromCodepointSimple(long longCode, DynamicObject rubyEncoding,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUTF8Profile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isUSAsciiProfile,
                                                       @Cached("createBinaryProfile()") ConditionProfile isAscii8BitProfile) {
            final int code = (int) longCode; // isSimple() guarantees this is OK
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope rope;

            if (isUTF8Profile.profile(encoding == UTF8Encoding.INSTANCE)) {
                rope = RopeConstants.UTF8_SINGLE_BYTE_ROPES[code];
            } else if (isUSAsciiProfile.profile(encoding == USASCIIEncoding.INSTANCE)) {
                rope = RopeConstants.US_ASCII_SINGLE_BYTE_ROPES[code];
            } else if (isAscii8BitProfile.profile(encoding == ASCIIEncoding.INSTANCE)) {
                rope = RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[code];
            } else {
                rope = RopeOperations.create(new byte[] { (byte) code }, encoding, CodeRange.CR_UNKNOWN);
            }

            return createString(rope);
        }

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization(guards = { "isRubyEncoding(rubyEncoding)", "!isSimple(code, rubyEncoding)", "isCodepoint(code)" })
        public DynamicObject stringFromCodepoint(long code, DynamicObject rubyEncoding) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final int length;

            try {
                length = encoding.codeToMbcLength((int) code);
            } catch (EncodingException e) {
                throw new RaiseException(coreExceptions().rangeError(code, rubyEncoding, this));
            }

            if (length <= 0) {
                throw new RaiseException(coreExceptions().rangeError(code, rubyEncoding, this));
            }

            final byte[] bytes = new byte[length];

            try {
                encoding.codeToMbc((int) code, bytes, 0);
            } catch (EncodingException e) {
                throw new RaiseException(coreExceptions().rangeError(code, rubyEncoding, this));
            }

            if (StringSupport.preciseLength(encoding, bytes, 0, length) != length) {
                throw new RaiseException(coreExceptions().rangeError(code, rubyEncoding, this));
            }

            return createString(RopeOperations.create(bytes, encoding, CodeRange.CR_VALID));
        }

        protected boolean isCodepoint(long code) {
            // Fits in an unsigned int
            return code >= 0 && code < (1L << 32);
        }

        protected boolean isSimple(long code, DynamicObject encoding) {
            final Encoding enc = EncodingOperations.getEncoding(encoding);

            return (enc.isAsciiCompatible() && code >= 0x00 && code < 0x80) || (enc == ASCIIEncoding.INSTANCE && code >= 0x00 && code <= 0xFF);
        }

    }

    @Primitive(name = "string_to_f", needsSelf = false)
    public static abstract class StringToFPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary(throwsControlFlowException = true)
        @Specialization
        public Object stringToF(DynamicObject string, boolean strict) {
            final Rope rope = rope(string);
            final ByteList byteList = RopeOperations.getByteListReadOnly(rope);
            if (byteList.getRealSize() == 0) {
                throw new RaiseException(coreExceptions().argumentError("invalid value for Float()", this));
            }
            if (string.toString().startsWith("0x")) {
                try {
                    return Double.parseDouble(string.toString());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            try {
                return ConvertDouble.byteListToDouble19(byteList, strict);
            } catch (NumberFormatException e) {
                if (strict) {
                    throw new RaiseException(coreExceptions().argumentError("invalid value for Float()", this));
                }
                return 0.0;
            }
        }
    }

    @Primitive(name = "string_index", lowerFixnum = 2)
    @ImportStatic(StringGuards.class)
    public static abstract class StringIndexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child StringByteCharacterIndexNode byteIndexToCharIndexNode;

        @Specialization(guards = { "isRubyString(pattern)", "isBrokenCodeRange(pattern)" })
        public DynamicObject stringIndexBrokenCodeRange(DynamicObject string, DynamicObject pattern, int start) {
            return nil();
        }


        @Specialization(guards = { "isRubyString(pattern)", "!isBrokenCodeRange(pattern)" })
        public Object stringIndex(VirtualFrame frame, DynamicObject string, DynamicObject pattern, int start) {
            if (byteIndexToCharIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                byteIndexToCharIndexNode = insert(StringNodesFactory.StringByteCharacterIndexNodeFactory.create(new RubyNode[]{}));
            }

            // Rubinius will pass in a byte index for the `start` value, but StringSupport.index requires a character index.
            final int charIndex = byteIndexToCharIndexNode.executeStringByteCharacterIndex(frame, string, start, 0);

            final int index = index(rope(string), rope(pattern), charIndex, encoding(string));

            if (index == -1) {
                return nil();
            }

            return index;
        }

        @TruffleBoundary
        private int index(Rope source, Rope other, int offset, Encoding enc) {
            // Taken from org.jruby.util.StringSupport.index.

            int sourceLen = source.characterLength();
            int otherLen = other.characterLength();

            if (offset < 0) {
                offset += sourceLen;
                if (offset < 0) return -1;
            }

            if (sourceLen - offset < otherLen) return -1;
            byte[]bytes = source.getBytes();
            int p = 0;
            int end = p + source.byteLength();
            if (offset != 0) {
                offset = source.isSingleByteOptimizable() ? offset : StringSupport.offset(enc, bytes, p, end, offset);
                p += offset;
            }
            if (otherLen == 0) return offset;

            while (true) {
                int pos = indexOf(source, other, p);
                if (pos < 0) return pos;
                pos -= p;
                int t = enc.rightAdjustCharHead(bytes, p, p + pos, end);
                if (t == p + pos) return pos + offset;
                if ((sourceLen -= t - p) <= 0) return -1;
                offset += t - p;
                p = t;
            }
        }

        @TruffleBoundary
        private int indexOf(Rope sourceRope, Rope otherRope, int fromIndex) {
            // Taken from org.jruby.util.ByteList.indexOf.

            final byte[] source = sourceRope.getBytes();
            final int sourceOffset = 0;
            final int sourceCount = sourceRope.byteLength();
            final byte[] target = otherRope.getBytes();
            final int targetOffset = 0;
            final int targetCount = otherRope.byteLength();

            if (fromIndex >= sourceCount) return (targetCount == 0 ? sourceCount : -1);
            if (fromIndex < 0) fromIndex = 0;
            if (targetCount == 0) return fromIndex;

            byte first  = target[targetOffset];
            int max = sourceOffset + (sourceCount - targetCount);

            for (int i = sourceOffset + fromIndex; i <= max; i++) {
                if (source[i] != first) while (++i <= max && source[i] != first);

                if (i <= max) {
                    int j = i + 1;
                    int end = j + targetCount - 1;
                    for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++);

                    if (j == end) return i - sourceOffset;
                }
            }
            return -1;
        }
    }

    @Primitive(name = "string_character_byte_index", needsSelf = false, lowerFixnum = { 1, 2 })
    @ImportStatic(StringGuards.class)
    public static abstract class CharacterByteIndexNode extends PrimitiveArrayArgumentsNode {

        public abstract int executeInt(VirtualFrame frame, DynamicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringCharacterByteIndex(DynamicObject string, int index, int start) {
            return start + index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int stringCharacterByteIndexMultiByteEncoding(DynamicObject string, int index, int start) {
            final Rope rope = rope(string);

            return StringSupport.nth(rope.getEncoding(), rope.getBytes(), start, rope.byteLength(), index);
        }
    }

    @Primitive(name = "string_byte_character_index", needsSelf = false)
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteCharacterIndexNode extends PrimitiveArrayArgumentsNode {

        public abstract int executeStringByteCharacterIndex(VirtualFrame frame, DynamicObject string, int index, int start);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int stringByteCharacterIndexSingleByte(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index;
        }

        @Specialization(guards = { "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int stringByteCharacterIndexFixedWidth(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.
            return index / encoding(string).minLength();
        }

        @Specialization(guards = { "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)", "isValidUtf8(string)" })
        public int stringByteCharacterIndexValidUtf8(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index.

            // TODO (nirvdrum 02-Apr-15) There's a way to optimize this for UTF-8, but porting all that code isn't necessary at the moment.
            return stringByteCharacterIndex(string, index, start);
        }

        @TruffleBoundary
        @Specialization(guards = { "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)", "!isValidUtf8(string)" })
        public int stringByteCharacterIndex(DynamicObject string, int index, int start) {
            // Taken from Rubinius's String::find_byte_character_index and Encoding::find_byte_character_index.

            final Rope rope = rope(string);
            final byte[] bytes = rope.getBytes();
            final Encoding encoding = rope.getEncoding();
            int p = start;
            final int end = bytes.length;
            int charIndex = 0;

            while (p < end && index > 0) {
                final int charLen = StringSupport.length(encoding, bytes, p, end);
                p += charLen;
                index -= charLen;
                charIndex++;
            }

            return charIndex;
        }
    }

    @Primitive(name = "string_character_index", needsSelf = false, lowerFixnum = 3)
    public static abstract class StringCharacterIndexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(pattern)")
        public Object stringCharacterIndex(DynamicObject string, DynamicObject pattern, int offset) {
            if (offset < 0) {
                return nil();
            }

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);

            final int total = stringRope.byteLength();
            int p = 0;
            final int e = p + total;
            int pp = 0;
            final int pe = pp + patternRope.byteLength();
            int s;
            int ss;

            final byte[] stringBytes = stringRope.getBytes();
            final byte[] patternBytes = patternRope.getBytes();

            if (stringRope.isSingleByteOptimizable()) {
                for(s = p += offset, ss = pp; p < e; s = ++p) {
                    if (stringBytes[p] != patternBytes[pp]) continue;

                    while (p < e && pp < pe && stringBytes[p] == patternBytes[pp]) {
                        p++;
                        pp++;
                    }

                    if (pp < pe) {
                        p = s;
                        pp = ss;
                    } else {
                        return s;
                    }
                }

                return nil();
            }

            final Encoding enc = stringRope.getEncoding();
            int index = 0;
            int c;

            while(p < e && index < offset) {
                c = StringSupport.preciseLength(enc, stringBytes, p, e);

                if (StringSupport.MBCLEN_CHARFOUND_P(c)) {
                    p += c;
                    index++;
                } else {
                    return nil();
                }
            }

            for(s = p, ss = pp; p < e; s = p += c, ++index) {
                c = StringSupport.preciseLength(enc, stringBytes, p, e);
                if ( !StringSupport.MBCLEN_CHARFOUND_P(c)) return nil();

                if (stringBytes[p] != patternBytes[pp]) continue;

                while (p < e && pp < pe) {
                    boolean breakOut = false;

                    for (int pc = p + c; p < e && p < pc && pp < pe; ) {
                        if (stringBytes[p] == patternBytes[pp]) {
                            ++p;
                            ++pp;
                        } else {
                            breakOut = true;
                            break;
                        }
                    }

                    if (breakOut) {
                        break;
                    }

                    c = StringSupport.preciseLength(enc, stringBytes, p, e);
                    if (! StringSupport.MBCLEN_CHARFOUND_P(c)) break;
                }

                if (pp < pe) {
                    p = s;
                    pp = ss;
                } else {
                    return index;
                }
            }

            return nil();
        }

    }

    @Primitive(name = "string_byte_index", needsSelf = false, lowerFixnum = { 1, 2 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteIndexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode;

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object stringByteIndexSingleByte(DynamicObject string, int index, int start,
                                                @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile) {
            if (indexTooLargeProfile.profile(index > rope(string).byteLength())) {
                return nil();
            }

            return index;
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object stringByteIndex(DynamicObject string, int index, int start,
                                      @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile invalidByteProfile,
                                      @Cached("create()") BranchProfile errorProfile) {
            // Taken from Rubinius's String::byte_index.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();
            int p = 0;
            final int e = p + rope.byteLength();

            int i, k = index;

            if (k < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentError("character index is negative", this));
            }

            for (i = 0; i < k && p < e; i++) {
                final int c = StringSupport.preciseLength(enc, rope.getBytes(), p, e);

                // If it's an invalid byte, just treat it as a single byte
                if(invalidByteProfile.profile(! StringSupport.MBCLEN_CHARFOUND_P(c))) {
                    ++p;
                } else {
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(c);
                }
            }

            if (indexTooLargeProfile.profile(i < k)) {
                return nil();
            } else {
                return p;
            }
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringByteIndex(DynamicObject string, DynamicObject pattern, int offset,
                                      @Cached("createBinaryProfile()") ConditionProfile emptyPatternProfile,
                @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                @Cached("create()") BranchProfile errorProfile) {
            // Taken from Rubinius's String::byte_index.

            if (offset < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentError("negative start given", this));
            }

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);

            if (emptyPatternProfile.profile(patternRope.isEmpty())) return offset;

            if (brokenCodeRangeProfile.profile(stringRope.getCodeRange() == CodeRange.CR_BROKEN)) {
                return nil();
            }

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(getContext(), null, null, null));
            }

            final Encoding encoding = checkEncodingNode.executeCheckEncoding(string, pattern);
            int p = 0;
            final int e = p + stringRope.byteLength();
            int pp = 0;
            final int pe = pp + patternRope.byteLength();
            int s;
            int ss;

            final byte[] stringBytes = stringRope.getBytes();
            final byte[] patternBytes = patternRope.getBytes();

            for(s = p, ss = pp; p < e; s = ++p) {
                if (stringBytes[p] != patternBytes[pp]) continue;

                while (p < e && pp < pe && stringBytes[p] == patternBytes[pp]) {
                    p++;
                    pp++;
                }

                if (pp < pe) {
                    p = s;
                    pp = ss;
                } else {
                    final int c = StringSupport.preciseLength(encoding, stringBytes, s, e);

                    if (StringSupport.MBCLEN_CHARFOUND_P(c)) {
                        return s;
                    } else {
                        return nil();
                    }
                }
            }

            return nil();
        }
    }

    // Port of Rubinius's String::previous_byte_index.
    //
    // This method takes a byte index, finds the corresponding character the byte index belongs to, and then returns
    // the byte index marking the start of the previous character in the string.
    @Primitive(name = "string_previous_byte_index")
    @ImportStatic(StringGuards.class)
    public static abstract class StringPreviousByteIndexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "index < 0")
        public Object stringPreviousByteIndexNegativeIndex(DynamicObject string, int index) {
            throw new RaiseException(coreExceptions().argumentError("negative index given", this));
        }

        @Specialization(guards = "index == 0")
        public Object stringPreviousByteIndexZeroIndex(DynamicObject string, int index) {
            return nil();
        }

        @Specialization(guards = { "index > 0", "isSingleByteOptimizable(string)" })
        public int stringPreviousByteIndexSingleByteOptimizable(DynamicObject string, int index) {
            return index - 1;
        }

        @Specialization(guards = { "index > 0", "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int stringPreviousByteIndexFixedWidthEncoding(DynamicObject string, int index,
                                                             @Cached("createBinaryProfile()") ConditionProfile firstCharacterProfile) {
            final Encoding encoding = encoding(string);

            // TODO (nirvdrum 11-Apr-16) Determine whether we need to be bug-for-bug compatible with Rubinius.
            // Implement a bug in Rubinius. We already special-case the index == 0 by returning nil. For all indices
            // corresponding to a given character, we treat them uniformly. However, for the first character, we only
            // return nil if the index is 0. If any other index into the first character is encountered, we return 0.
            // It seems unlikely this will ever be encountered in practice, but it's here for completeness.
            if (firstCharacterProfile.profile(index < encoding.maxLength())) {
                return 0;
            }

            return (index / encoding.maxLength() - 1) * encoding.maxLength();
        }

        @Specialization(guards = { "index > 0", "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)" })
        @TruffleBoundary
        public Object stringPreviousByteIndex(DynamicObject string, int index) {
            final Rope rope = rope(string);
            final int p = 0;
            final int end = p + rope.byteLength();

            final int b = rope.getEncoding().prevCharHead(rope.getBytes(), p, p + index, end);

            if (b == -1) {
                return nil();
            }

            return b - p;
        }

    }

    @Primitive(name = "string_copy_from", needsSelf = false, lowerFixnum = { 3, 4, 5 })
    public static abstract class StringCopyFromPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = { "isRubyString(other)", "size >= 0", "!offsetTooLarge(start, other)", "!offsetTooLargeRaw(dest, string)" })
        public DynamicObject stringCopyFrom(DynamicObject string, DynamicObject other, int start, int size, int dest,
                                            @Cached("createBinaryProfile()") ConditionProfile negativeStartOffsetProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile sizeTooLargeInReplacementProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile negativeDestinationOffsetProfile,
                                            @Cached("createBinaryProfile()") ConditionProfile sizeTooLargeInStringProfile) {
            // Taken from Rubinius's String::copy_from.

            int src = start;
            int dst = dest;
            int cnt = size;

            final Rope otherRope = rope(other);
            int osz = otherRope.byteLength();
            if(negativeStartOffsetProfile.profile(src < 0)) src = 0;
            if(sizeTooLargeInReplacementProfile.profile(cnt > osz - src)) cnt = osz - src;

            final ByteList stringBytes = RopeOperations.toByteListCopy(Layouts.STRING.getRope(string));
            int sz = stringBytes.unsafeBytes().length - stringBytes.begin();
            if(negativeDestinationOffsetProfile.profile(dst < 0)) dst = 0;
            if(sizeTooLargeInStringProfile.profile(cnt > sz - dst)) cnt = sz - dst;

            System.arraycopy(otherRope.getBytes(), src, stringBytes.getUnsafeBytes(), stringBytes.begin() + dest, cnt);

            StringOperations.setRope(string, StringOperations.ropeFromByteList(stringBytes));

            return string;
        }

        @Specialization(guards = { "isRubyString(other)", "size < 0 || (offsetTooLarge(start, other) || offsetTooLargeRaw(dest, string))" })
        public DynamicObject stringCopyFromWithNegativeSize(DynamicObject string, DynamicObject other, int start, int size, int dest) {
            return string;
        }

        protected boolean offsetTooLarge(int offset, DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            return offset >= Layouts.STRING.getRope(string).byteLength();
        }

        protected boolean offsetTooLargeRaw(int offset, DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            // This bounds checks on the total capacity rather than the virtual
            // size() of the String. This allows for string adjustment within
            // the capacity without having to change the virtual size first.

            // TODO (nirvdrum 21-Jan-16) Verify whether we still need this method as we never have spare capacity allocated with ropes.
            final Rope rope = rope(string);
            return offset >= rope.byteLength();
        }

    }

    @Primitive(name = "string_rindex", lowerFixnum = 2)
    public static abstract class StringRindexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode patternGetByteNode;
        @Child private RopeNodes.GetByteNode stringGetByteNode;

        public StringRindexPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            patternGetByteNode = RopeNodes.GetByteNode.create();
            stringGetByteNode = RopeNodes.GetByteNode.create();
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object stringRindex(DynamicObject string, DynamicObject pattern, int start,
                @Cached("create()") BranchProfile errorProfile) {
            // Taken from Rubinius's String::rindex.

            int pos = start;

            if (pos < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentError("negative start given", this));
            }

            final Rope stringRope = rope(string);
            final Rope patternRope = rope(pattern);
            final int total = stringRope.byteLength();
            final int matchSize = patternRope.byteLength();

            if (pos >= total) {
                pos = total - 1;
            }

            switch(matchSize) {
                case 0: {
                    return start;
                }

                case 1: {
                    final int matcher = patternGetByteNode.executeGetByte(patternRope, 0);

                    while (pos >= 0) {
                        if (stringGetByteNode.executeGetByte(stringRope, pos) == matcher) {
                            return pos;
                        }

                        pos--;
                    }

                    return nil();
                }

                default: {
                    if (total - pos < matchSize) {
                        pos = total - matchSize;
                    }

                    int cur = pos;

                    while (cur >= 0) {
                        // TODO (nirvdrum 21-Jan-16): Investigate a more rope efficient memcmp.
                        if (ByteList.memcmp(stringRope.getBytes(), cur, patternRope.getBytes(), 0, matchSize) == 0) {
                            return cur;
                        }

                        cur--;
                    }
                }
            }

            return nil();
        }

    }

    @Primitive(name = "string_pattern", lowerFixnum = { 1, 2 })
    public static abstract class StringPatternPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private RopeNodes.MakeRepeatingNode makeRepeatingNode;

        public StringPatternPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
            makeLeafRopeNode = RopeNodes.MakeLeafRopeNode.create();
            makeRepeatingNode = RopeNodes.MakeRepeatingNode.create();
        }

        @Specialization(guards = "value >= 0")
        public DynamicObject stringPatternZero(DynamicObject stringClass, int size, int value) {
            final Rope repeatingRope = makeRepeatingNode.executeMake(RopeConstants.ASCII_8BIT_SINGLE_BYTE_ROPES[value], size);

            return allocateObjectNode.allocate(stringClass, repeatingRope);
        }

        @Specialization(guards = { "isRubyString(string)", "patternFitsEvenly(string, size)" })
        public DynamicObject stringPatternFitsEvenly(DynamicObject stringClass, int size, DynamicObject string) {
            final Rope rope = rope(string);
            final Rope repeatingRope = makeRepeatingNode.executeMake(rope, size / rope.byteLength());

            return allocateObjectNode.allocate(stringClass, repeatingRope);
        }

        @Specialization(guards = { "isRubyString(string)", "!patternFitsEvenly(string, size)" })
        @TruffleBoundary
        public DynamicObject stringPattern(DynamicObject stringClass, int size, DynamicObject string) {
            final Rope rope = rope(string);
            final byte[] bytes = new byte[size];

            // TODO (nirvdrum 21-Jan-16): Investigate whether using a ConcatRope (potentially combined with a RepeatingRope) would be better here.
            if (! rope.isEmpty()) {
                for (int n = 0; n < size; n += rope.byteLength()) {
                    System.arraycopy(rope.getBytes(), 0, bytes, n, Math.min(rope.byteLength(), size - n));
                }
            }

            // If we reach this specialization, the `size` attribute will cause a truncated `string` to appear at the
            // end of the resulting string in order to pad the value out. A truncated CR_7BIT string is always CR_7BIT.
            // A truncated CR_VALID string could be any of the code range values.
            final CodeRange codeRange = rope.getCodeRange() == CodeRange.CR_7BIT ? CodeRange.CR_7BIT : CodeRange.CR_UNKNOWN;
            final Object characterLength = codeRange == CodeRange.CR_7BIT ? size : NotProvided.INSTANCE;

            return allocateObjectNode.allocate(stringClass, makeLeafRopeNode.executeMake(bytes, encoding(string), codeRange, characterLength));
        }

        protected boolean patternFitsEvenly(DynamicObject string, int size) {
            assert RubyGuards.isRubyString(string);

            final int byteLength = rope(string).byteLength();

            return byteLength > 0 && (size % byteLength) == 0;
        }

    }

    @Primitive(name = "string_splice", needsSelf = false, lowerFixnum = { 3, 4 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringSplicePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeConcatNode appendMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode prependMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode leftMakeConcatNode;
        @Child private RopeNodes.MakeConcatNode rightMakeConcatNode;
        @Child private RopeNodes.MakeSubstringNode prependMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode;
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode;

        @Specialization(guards = { "indexAtStartBound(spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)" })
        public Object splicePrepend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding) {
            if (prependMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                prependMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null));
            }

            if (prependMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                prependMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope original = rope(string);
            final Rope left = rope(other);
            final Rope right = prependMakeSubstringNode.executeMake(original, byteCountToReplace, original.byteLength() - byteCountToReplace);

            StringOperations.setRope(string, prependMakeConcatNode.executeMake(left, right, encoding));

            return string;
        }

        @Specialization(guards = { "indexAtEndBound(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)" })
        public Object spliceAppend(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope left = rope(string);
            final Rope right = rope(other);

            if (appendMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                appendMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            StringOperations.setRope(string, appendMakeConcatNode.executeMake(left, right, encoding));

            return string;
        }

        @Specialization(guards = { "!indexAtEitherBounds(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)", "!isRopeBuffer(string)" })
        public DynamicObject splice(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding,
                                    @Cached("createBinaryProfile()") ConditionProfile insertStringIsEmptyProfile,
                                    @Cached("createBinaryProfile()") ConditionProfile splitRightIsEmptyProfile) {
            if (leftMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                leftMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null));
            }

            if (rightMakeSubstringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                rightMakeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null));
            }

            if (leftMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                leftMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            if (rightMakeConcatNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                rightMakeConcatNode = insert(RopeNodesFactory.MakeConcatNodeGen.create(null, null, null));
            }

            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final Rope source = rope(string);
            final Rope insert = rope(other);
            final int rightSideStartingIndex = spliceByteIndex + byteCountToReplace;

            final Rope splitLeft = leftMakeSubstringNode.executeMake(source, 0, spliceByteIndex);
            final Rope splitRight = rightMakeSubstringNode.executeMake(source, rightSideStartingIndex, source.byteLength() - rightSideStartingIndex);

            final Rope joinedLeft;
            if (insertStringIsEmptyProfile.profile(insert.isEmpty())) {
                joinedLeft = splitLeft;
            } else {
                joinedLeft = leftMakeConcatNode.executeMake(splitLeft, insert, encoding);
            }

            final Rope joinedRight;
            if (splitRightIsEmptyProfile.profile(splitRight.isEmpty())) {
                joinedRight = joinedLeft;
            } else {
                joinedRight = rightMakeConcatNode.executeMake(joinedLeft, splitRight, encoding);
            }

            StringOperations.setRope(string, joinedRight);

            return string;
        }

        @Specialization(guards = { "!indexAtEitherBounds(string, spliceByteIndex)", "isRubyString(other)", "isRubyEncoding(rubyEncoding)", "isRopeBuffer(string)", "isSingleByteOptimizable(string)" })
        public DynamicObject spliceBuffer(DynamicObject string, DynamicObject other, int spliceByteIndex, int byteCountToReplace, DynamicObject rubyEncoding,
                                          @Cached("createBinaryProfile()") ConditionProfile sameCodeRangeProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);
            final RopeBuffer source = (RopeBuffer) rope(string);
            final Rope insert = rope(other);
            final int rightSideStartingIndex = spliceByteIndex + byteCountToReplace;

            final ByteList byteList = new ByteList(source.byteLength() + insert.byteLength() - byteCountToReplace);

            byteList.append(source.getByteList(), 0, spliceByteIndex);
            byteList.append(insert.getBytes());
            byteList.append(source.getByteList(), rightSideStartingIndex, source.byteLength() - rightSideStartingIndex);
            byteList.setEncoding(encoding);

            final Rope buffer = new RopeBuffer(byteList,
                    RopeNodes.MakeConcatNode.commonCodeRange(source.getCodeRange(), insert.getCodeRange(), sameCodeRangeProfile, brokenCodeRangeProfile),
                    source.isSingleByteOptimizable() && insert.isSingleByteOptimizable(),
                    source.characterLength() + insert.characterLength() - byteCountToReplace);

            StringOperations.setRope(string, buffer);

            return string;
        }

        protected  boolean indexAtStartBound(int index) {
            return index == 0;
        }

        protected boolean indexAtEndBound(DynamicObject string, int index) {
            assert RubyGuards.isRubyString(string);

            return index == rope(string).byteLength();
        }

        protected boolean indexAtEitherBounds(DynamicObject string, int index) {
            assert RubyGuards.isRubyString(string);

            return indexAtStartBound(index) || indexAtEndBound(string, index);
        }

        protected boolean isRopeBuffer(DynamicObject string) {
            assert RubyGuards.isRubyString(string);

            return rope(string) instanceof RopeBuffer;
        }
    }

    @Primitive(name = "string_to_inum")
    public static abstract class StringToInumPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public Object stringToInum(DynamicObject string, int fixBase, boolean strict,
                                   @Cached("create(getContext(), getSourceSection())") FixnumOrBignumNode fixnumOrBignumNode) {
            return ConvertBytes.byteListToInum19(getContext(),
                    this,
                    fixnumOrBignumNode,
                    string,
                    fixBase,
                    strict);
        }

    }

    @Primitive(name = "string_byte_append")
    public static abstract class StringByteAppendPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.MakeConcatNode makeConcatNode;
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;

        public StringByteAppendPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
            makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
        }

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringByteAppend(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            // The semantics of this primitive are such that the original string's byte[] should be extended without
            // any modification to the other properties of the string. This is counter-intuitive because adding bytes
            // from another string may very well change the code range for the source string. Updating the code range,
            // however, breaks other things so we can't do it. As an example, StringIO starts with an empty UTF-8
            // string and then appends ASCII-8BIT bytes, but must retain the original UTF-8 encoding. The binary contents
            // of the ASCII-8BIT string could give the resulting string a CR_BROKEN code range on UTF-8, but if we do
            // this, StringIO ceases to work -- the resulting string must retain the original CR_7BIT code range. It's
            // ugly, but seems to be due to a difference in how Rubinius keeps track of byte optimizable strings.

            final Rope rightConverted = makeLeafRopeNode.executeMake(right.getBytes(), left.getEncoding(), left.getCodeRange(), NotProvided.INSTANCE);

            StringOperations.setRope(string, makeConcatNode.executeMake(left, rightConverted, left.getEncoding()));

            return string;
        }

    }

    @Primitive(name = "string_substring", lowerFixnum = { 1, 2 })
    @ImportStatic(StringGuards.class)
    public static abstract class StringSubstringPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public abstract Object execute(VirtualFrame frame, DynamicObject string, int beg, int len);

        @Specialization(guards = "!indexTriviallyOutOfBounds(string, beg, len)")
        public Object stringSubstring(DynamicObject string, int beg, int len,
                                      @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile stillNegativeIndexProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile tooLargeTotalProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile foundSingleByteOptimizableDescendentProfile) {
            final Rope rope = rope(string);

            int index = beg;
            int length = len;
            if (negativeIndexProfile.profile(index < 0)) {
                index += rope.characterLength();

                if (stillNegativeIndexProfile.profile(index < 0)) {
                    return nil();
                }
            }

            if (tooLargeTotalProfile.profile(index + length > rope.characterLength())) {
                length = rope.characterLength() - index;
            }

            if (singleByteOptimizableProfile.profile((length == 0) || rope.isSingleByteOptimizable())) {
                if (mutableRopeProfile.profile(rope instanceof RopeBuffer)) {
                    return makeBuffer(string, index, length);
                }

                return makeRope(string, rope, index, length);
            } else {
                final SearchResult searchResult = searchForSingleByteOptimizableDescendant(rope, index, length);

                if (foundSingleByteOptimizableDescendentProfile.profile(searchResult.rope.isSingleByteOptimizable())) {
                    return makeRope(string, searchResult.rope, searchResult.index, length);
                }

                return stringSubstringMultitByte(string, index, length);
            }
        }

        @TruffleBoundary
        private SearchResult searchForSingleByteOptimizableDescendant(Rope base, int index, int length) {
            // If we've found something that's single-byte optimizable, we can halt the search. Taking a substring of
            // a single byte optimizable rope is a fast operation.
            if (base.isSingleByteOptimizable()) {
                return new SearchResult(index, base);
            }

            if (base instanceof LeafRope) {
                return new SearchResult(index, base);
            } else if (base instanceof SubstringRope) {
                final SubstringRope substringRope = (SubstringRope) base;
                return searchForSingleByteOptimizableDescendant(substringRope.getChild(), index + substringRope.getOffset(), length);
            } else if (base instanceof ConcatRope) {
                final ConcatRope concatRope = (ConcatRope) base;
                final Rope left = concatRope.getLeft();
                final Rope right = concatRope.getRight();

                if (index < left.characterLength()) {
                    return searchForSingleByteOptimizableDescendant(left, index, length);
                } else if (index >= left.characterLength()) {
                    return searchForSingleByteOptimizableDescendant(right, index - left.characterLength(), length);
                } else {
                    return new SearchResult(index, concatRope);
                }
            } else if (base instanceof RepeatingRope) {
                final RepeatingRope repeatingRope = (RepeatingRope) base;

                if (index + length < repeatingRope.getChild().characterLength()) {
                    return searchForSingleByteOptimizableDescendant(repeatingRope.getChild(), index, length);
                } else {
                    return new SearchResult(index, repeatingRope);
                }
            } else {
                throw new UnsupportedOperationException("Don't know how to traverse rope type: " + base.getClass().getName());
            }
        }

        @TruffleBoundary
        private Object stringSubstringMultitByte(DynamicObject string, int beg, int len) {
            // Taken from org.jruby.RubyString#substr19 & org.jruby.RubyString#multibyteSubstr19.

            final Rope rope = rope(string);
            final int length = rope.byteLength();
            final boolean isMutableRope = rope instanceof RopeBuffer;

            final Encoding enc = rope.getEncoding();
            int p;
            int s = 0;
            int end = s + length;
            byte[]bytes = rope.getBytes();

            if (beg < 0) {
                if (len > -beg) len = -beg;
                if (-beg * enc.maxLength() < length >>> 3) {
                    beg = -beg;
                    int e = end;
                    while (beg-- > len && (e = enc.prevCharHead(bytes, s, e, e)) != -1) {} // nothing
                    p = e;
                    if (p == -1) {
                        return nil();
                    }
                    while (len-- > 0 && (p = enc.prevCharHead(bytes, s, p, e)) != -1) {} // nothing
                    if (p == -1) {
                        return nil();
                    }

                    if (isMutableRope) {
                        return makeBuffer(string, p - s, e - p);
                    }

                    return makeRope(string, rope, p - s, e - p);
                } else {
                    beg += rope.characterLength();
                    if (beg < 0) {
                        return nil();
                    }
                }
            } else if (beg > 0 && beg > rope.characterLength()) {
                return nil();
            }
            if (len == 0) {
                p = 0;
            } else if (StringOperations.isCodeRangeValid(string) && enc instanceof UTF8Encoding) {
                p = StringSupport.utf8Nth(bytes, s, end, beg);
                len = StringSupport.utf8Offset(bytes, p, end, len);
            } else if (enc.isFixedWidth()) {
                int w = enc.maxLength();
                p = s + beg * w;
                if (p > end) {
                    p = end;
                    len = 0;
                } else if (len * w > end - p) {
                    len = end - p;
                } else {
                    len *= w;
                }
            } else if ((p = StringSupport.nth(enc, bytes, s, end, beg)) == end) {
                len = 0;
            } else {
                len = StringSupport.offset(enc, bytes, p, end, len);
            }

            if (isMutableRope) {
                return makeBuffer(string, p - s, len);
            }

            return makeRope(string, rope, p - s, len);
        }

        @Specialization(guards = "indexTriviallyOutOfBounds(string, beg, len)")
        public Object stringSubstringNegativeLength(DynamicObject string, int beg, int len) {
            return nil();
        }

        protected static boolean indexTriviallyOutOfBounds(DynamicObject string, int index, int length) {
            assert RubyGuards.isRubyString(string);

            return (length < 0) || (index > rope(string).characterLength());
        }

        private DynamicObject makeRope(DynamicObject string, Rope rope, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                allocateNode = insert(AllocateObjectNode.create());
            }

            if (makeSubstringNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                makeSubstringNode = insert(RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null));
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintResultNode = insert(new TaintResultNode(getContext(), null));
            }

            final DynamicObject ret = allocateNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(string),
                    makeSubstringNode.executeMake(rope, beg, len));

            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

        private DynamicObject makeBuffer(DynamicObject string, int beg, int len) {
            assert RubyGuards.isRubyString(string);

            final RopeBuffer buffer = (RopeBuffer) rope(string);

            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                allocateNode = insert(AllocateObjectNode.create());
            }

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                taintResultNode = insert(new TaintResultNode(getContext(), null));
            }

            final DynamicObject ret = allocateNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(string),
                    new RopeBuffer(new ByteList(buffer.getByteList(), beg, len), buffer.getCodeRange(), buffer.isSingleByteOptimizable(), len));

            taintResultNode.maybeTaint(string, ret);

            return ret;
        }

        private static final class SearchResult {
            public final int index;
            public final Rope rope;

            public SearchResult(final int index, final Rope rope) {
                this.index = index;
                this.rope = rope;
            }
        }

    }

    @Primitive(name = "string_from_bytearray", needsSelf = false, lowerFixnum = { 2, 3 })
    public static abstract class StringFromByteArrayPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubiniusByteArray(bytes)")
        public DynamicObject stringFromByteArray(DynamicObject bytes, int start, int count) {
            // Data is copied here - can we do something COW?
            final ByteList byteList = Layouts.BYTE_ARRAY.getBytes(bytes);
            return createString(new ByteList(byteList, start, count));
        }

    }

}
