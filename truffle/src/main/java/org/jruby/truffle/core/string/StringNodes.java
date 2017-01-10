/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
 * Some of the code in this class is transposed from org.jruby.util.ByteList and org.jruby.RubyString,
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
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF32BEEncoding;
import org.jcodings.specific.UTF32LEEncoding;
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
import org.jruby.truffle.core.array.ArrayUtils;
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
import org.jruby.truffle.language.objects.IsTaintedNode;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.yield.YieldNode;
import org.jruby.truffle.platform.posix.TrufflePosix;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jruby.truffle.core.rope.CodeRange.CR_7BIT;
import static org.jruby.truffle.core.rope.CodeRange.CR_VALID;
import static org.jruby.truffle.core.rope.RopeConstants.EMPTY_ASCII_8BIT_ROPE;
import static org.jruby.truffle.core.string.StringOperations.encoding;
import static org.jruby.truffle.core.string.StringOperations.rope;
import static org.jruby.truffle.core.string.StringSupport.MBCLEN_CHARFOUND_LEN;
import static org.jruby.truffle.core.string.StringSupport.MBCLEN_CHARFOUND_P;
import static org.jruby.truffle.core.string.StringSupport.MBCLEN_INVALID_P;
import static org.jruby.truffle.core.string.StringSupport.MBCLEN_NEEDMORE_P;

@CoreClass("String")
public abstract class StringNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private TaintResultNode taintResultNode = new TaintResultNode();

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(other);
        }

        @Specialization(guards = { "!isRopeBuffer(string)", "isRubyString(other)" })
        public DynamicObject add(DynamicObject string, DynamicObject other,
                                 @Cached("create()") StringAppendNode stringAppendNode) {
            final Rope concatRope = stringAppendNode.executeStringAppend(string, other);

            final DynamicObject ret = allocateObjectNode.allocate(coreLibrary().getStringClass(), concatRope);

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }

        @Specialization(guards = { "isRopeBuffer(string)", "is7Bit(string)", "is7Bit(other)" })
        public DynamicObject addRopeBuffer(DynamicObject string, DynamicObject other,
                                           @Cached("createBinaryProfile()") ConditionProfile ropeBufferProfile,
                                           @Cached("create()") EncodingNodes.CheckEncodingNode checkEncodingNode) {
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

    }

    @CoreMethod(names = "*", required = 1, lowerFixnum = 1, taintFrom = 0)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private ToIntNode toIntNode;

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

        @Child private StringEqualNode stringEqualNode = StringEqualNodeGen.create(null, null);
        @Child private KernelNodes.RespondToNode respondToNode;
        @Child private CallDispatchHeadNode objectEqualNode;
        @Child private CheckLayoutNode checkLayoutNode;

        @Specialization(guards = "isRubyString(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return stringEqualNode.executeStringEqual(a, b);
        }

        @Specialization(guards = "!isRubyString(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, Object b) {
            if (respondToNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToNode = insert(KernelNodesFactory.RespondToNodeFactory.create(null, null, null));
            }

            if (respondToNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (objectEqualNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    objectEqualNode = insert(DispatchHeadNodeFactory.createMethodCall());
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
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(null, null, null));
            }

            if (respondToToStrNode.doesRespondToString(frame, b, create7BitString("to_str", UTF8Encoding.INSTANCE), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toStrNode = insert(ToStrNodeGen.create(null));
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
                respondToCmpNode = insert(KernelNodesFactory.RespondToNodeFactory.create(null, null, null));
            }

            if (respondToCmpNode.doesRespondToString(frame, b, create7BitString("<=>", UTF8Encoding.INSTANCE), false)) {
                if (cmpNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cmpNode = insert(DispatchHeadNodeFactory.createMethodCall());
                }

                final Object cmpResult = cmpNode.call(frame, b, "<=>", a);

                if (cmpResult == nil()) {
                    return nil();
                }

                if (cmpIntNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    cmpIntNode = insert(CmpIntNodeGen.create(null, null, null));
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

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject concat(DynamicObject string, DynamicObject other,
                                                    @Cached("create()") StringAppendPrimitiveNode stringAppendNode) {
            return stringAppendNode.executeStringAppend(string, other);
        }

        @Specialization(guards = "!isRubyString(other)")
        public Object concatGeneric(
                VirtualFrame frame,
                DynamicObject string,
                Object other,
                @Cached("createMethodCall()") CallDispatchHeadNode callNode) {
            return callNode.call(frame, string, "concat_internal", other);
        }

    }

    @CoreMethod(names = { "[]", "slice" }, required = 1, optional = 1, lowerFixnum = { 1, 2 }, taintFrom = 0)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private NormalizeIndexNode normalizeIndexNode;
        @Child private StringSubstringPrimitiveNode substringNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        @Specialization
        public Object getIndex(VirtualFrame frame, DynamicObject string, int index, NotProvided length) {
            // Check for the only difference from str[index, 1]
            if (index == rope(string).characterLength()) {
                outOfBounds.enter();
                return nil();
            }
            return getSubstringNode().execute(frame, string, index, 1);
        }

        @Specialization(guards = { "!isRubyRange(index)", "!isRubyRegexp(index)", "!isRubyString(index)" })
        public Object getIndex(VirtualFrame frame, DynamicObject string, Object index, NotProvided length, @Cached("new()") SnippetNode snippetNode) {
            return getIndex(frame, string, (int)snippetNode.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", index), length);
        }

        @Specialization(guards = "isIntRange(range)")
        public Object sliceIntegerRange(VirtualFrame frame, DynamicObject string, DynamicObject range, NotProvided length) {
            return sliceRange(frame, string, Layouts.INT_RANGE.getBegin(range), Layouts.INT_RANGE.getEnd(range), Layouts.INT_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = "isLongRange(range)")
        public Object sliceLongRange(VirtualFrame frame, DynamicObject string, DynamicObject range, NotProvided length) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values should be properly lowered, only if possible.
            return sliceRange(frame, string, (int) Layouts.LONG_RANGE.getBegin(range), (int) Layouts.LONG_RANGE.getEnd(range), Layouts.LONG_RANGE.getExcludedEnd(range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object sliceObjectRange(VirtualFrame frame, DynamicObject string, DynamicObject range, NotProvided length,
                @Cached("new()") SnippetNode snippetNode1,
                @Cached("new()") SnippetNode snippetNode2) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values may return Fixnums beyond int boundaries and we should handle that -- Bignums are always errors.
            final int coercedBegin = (int)snippetNode1.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", Layouts.OBJECT_RANGE.getBegin(range));
            final int coercedEnd = (int)snippetNode2.execute(frame, "Rubinius::Type.rb_num2int(v)", "v", Layouts.OBJECT_RANGE.getEnd(range));

            return sliceRange(frame, string, coercedBegin, coercedEnd, Layouts.OBJECT_RANGE.getExcludedEnd(range));
        }

        private Object sliceRange(VirtualFrame frame, DynamicObject string, int begin, int end, boolean doesExcludeEnd) {
            assert RubyGuards.isRubyString(string);

            if (normalizeIndexNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                normalizeIndexNode = insert(StringNodesFactory.NormalizeIndexNodeGen.create(null, null));
            }

            final int stringLength = rope(string).characterLength();
            begin = normalizeIndexNode.executeNormalize(begin, stringLength);

            if (begin < 0 || begin > stringLength) {
                outOfBounds.enter();
                return nil();
            } else {

                if (begin == stringLength) {
                    final ByteList byteList = new ByteList();
                    byteList.setEncoding(encoding(string));
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.withEncodingVerySlow(RopeConstants.EMPTY_ASCII_8BIT_ROPE, encoding(string)));
                }

                end = normalizeIndexNode.executeNormalize(end, stringLength);
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

        @Specialization(guards = "isRubyRegexp(regexp)")
        public Object slice1(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject regexp,
                NotProvided capture,
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

        @Specialization(guards = "isRubyString(matchStr)")
        public Object slice2(VirtualFrame frame, DynamicObject string, DynamicObject matchStr, NotProvided length) {
            if (includeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                includeNode = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            boolean result = includeNode.callBoolean(frame, string, "include?", null, matchStr);

            if (result) {
                if (dupNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    dupNode = insert(DispatchHeadNodeFactory.createMethodCall());
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

        @Child private RopeNodes.WithEncodingNode withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(null, null, null);

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
                store[n] = bytes[n] & 0xFF;
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

        @Child private EncodingNodes.NegotiateCompatibleEncodingNode negotiateCompatibleEncodingNode = EncodingNodesFactory.NegotiateCompatibleEncodingNodeGen.create(null, null);

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(other);
        }

        @Specialization(guards = {"isRubyString(other)", "bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmpSingleByte(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19.

            if (negotiateCompatibleEncodingNode.executeNegotiate(string, other) == null) {
                return nil();
            }

            return RopeOperations.caseInsensitiveCmp(rope(string), rope(other));
        }

        @Specialization(guards = {"isRubyString(other)", "!bothSingleByteOptimizable(string, other)"})
        @TruffleBoundary
        public Object caseCmp(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19 and

            final Encoding encoding = negotiateCompatibleEncodingNode.executeNegotiate(string, other);

            if (encoding == null) {
                return nil();
            }

            return StringSupport.multiByteCasecmp(encoding, rope(string), rope(other));
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

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(null, null);
        @Child private ToStrNode toStr = ToStrNodeGen.create(null);

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
            StringSupport.TrTables tables = StringSupport.trSetupTable(rope(otherStr), table, null, true, enc);
            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];

                assert RubyGuards.isRubyString(otherStr);

                enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
                tables = StringSupport.trSetupTable(rope(otherStr), table, tables, false, enc);
            }

            return StringSupport.strCount(rope(string), table, tables, enc);
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
            return ToStrNodeGen.create(other);
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
                taintResultNode = insert(new TaintResultNode());
            }

            final DynamicObject ret = createString(RopeOperations.ropeFromByteList(new ByteList(cryptedString, 0, cryptedString.length - 1, ASCIIEncoding.INSTANCE, false)));

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(salt, ret);

            return ret;
        }

    }

    @CoreMethod(names = "delete!", rest = true, raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DeleteBangNode extends CoreMethodArrayArgumentsNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(null, null);
        @Child private ToStrNode toStr = ToStrNodeGen.create(null);

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
            StringSupport.TrTables tables = StringSupport.trSetupTable(rope(otherString),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                assert RubyGuards.isRubyString(otherStrings[i]);

                enc = checkEncodingNode.executeCheckEncoding(string, otherStrings[i]);
                tables = StringSupport.trSetupTable(rope(otherStrings[i]), squeeze, tables, false, enc);
            }

            final Rope processedRope = StringSupport.delete_bangCommon19(rope(string), squeeze, tables, enc);
            if (processedRope == null) {
                return nil();
            }

            StringOperations.setRope(string, processedRope);

            return string;
        }
    }

    @CoreMethod(names = "downcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DowncaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        @Child private TaintResultNode taintResultNode;

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
                taintResultNode = insert(new TaintResultNode());
            }

            final DynamicObject ret = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), substringRope);

            return taintResultNode.maybeTaint(string, ret);
        }
    }

    @CoreMethod(names = "force_encoding", required = 1, raiseIfFrozenSelf = true)
    public abstract static class ForceEncodingNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.WithEncodingNode withEncodingNode = RopeNodesFactory.WithEncodingNodeGen.create(null, null, null);
        @Child private ToStrNode toStrNode;

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
                toStrNode = insert(ToStrNodeGen.create(null));
            }

            return forceEncodingString(string, toStrNode.executeToStr(frame, encoding), differentEncodingProfile, mutableRopeProfile);
        }

    }

    @CoreMethod(names = "getbyte", required = 1, lowerFixnum = 1)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        @Child private NormalizeIndexNode normalizeIndexNode = StringNodesFactory.NormalizeIndexNodeGen.create(null, null);
        @Child private RopeNodes.GetByteNode ropeGetByteNode = RopeNodesFactory.GetByteNodeGen.create(null, null);

        @Specialization
        public Object getByte(DynamicObject string, int index,
                              @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                              @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile) {
            final Rope rope = rope(string);
            final int normalizedIndex = normalizeIndexNode.executeNormalize(index, rope.byteLength());

            if (indexOutOfBoundsProfile.profile((normalizedIndex < 0) || (normalizedIndex >= rope.byteLength()))) {
                return nil();
            }

            return ropeGetByteNode.executeGetByte(rope, normalizedIndex);
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int hash(DynamicObject string) {
            return rope(string).hashCode();
        }

    }

    @Primitive(name = "string_initialize")
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        @Specialization
        public DynamicObject initializeJavaString(DynamicObject self, String from) {
            StringOperations.setRope(self, StringOperations.encodeRope(from, ASCIIEncoding.INSTANCE));
            return self;
        }

        @Specialization(guards = "isRubyString(from)")
        public DynamicObject initialize(DynamicObject self, DynamicObject from) {
            StringOperations.setRope(self, rope(from));
            return self;
        }

        @Specialization(guards = {"!isRubyString(from)", "!isString(from)"})
        public DynamicObject initialize(VirtualFrame frame, DynamicObject self, Object from) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(null));
            }

            return initialize(self, toStrNode.executeToStr(frame, from));
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

    @CoreMethod(names = "lstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class LstripBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);

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
            return ToStrNodeGen.create(other);
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

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);

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

    @Primitive(name = "string_scrub")
    public abstract static class ScrubNode extends PrimitiveArrayArgumentsNode {

        private static final byte[] SCRUB_REPL_UTF8 = new byte[]{(byte)0xEF, (byte)0xBF, (byte)0xBD};
        private static final byte[] SCRUB_REPL_ASCII = new byte[]{(byte)'?'};
        private static final byte[] SCRUB_REPL_UTF16BE = new byte[]{(byte)0xFF, (byte)0xFD};
        private static final byte[] SCRUB_REPL_UTF16LE = new byte[]{(byte)0xFD, (byte)0xFF};
        private static final byte[] SCRUB_REPL_UTF32BE = new byte[]{(byte)0x00, (byte)0x00, (byte)0xFF, (byte)0xFD};
        private static final byte[] SCRUB_REPL_UTF32LE = new byte[]{(byte)0xFD, (byte)0xFF, (byte)0x00, (byte)0x00};

        @Child private YieldNode yieldNode = new YieldNode();
        @Child private IsTaintedNode isTaintedNode = IsTaintedNode.create();
        @Child private TaintNode taintNode = TaintNode.create();
        @Child private CallDispatchHeadNode strCompatAndValidNode = DispatchHeadNodeFactory.createMethodCall(true);
        @Child private RopeNodes.MakeConcatNode makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);

        @Specialization
        public DynamicObject scrubDefault(VirtualFrame frame, DynamicObject string, Object repl, DynamicObject block,
                                          @Cached("createBinaryProfile()") ConditionProfile validRangeProfile,
                                          @Cached("createBinaryProfile()") ConditionProfile asciiCompatibleProfile) {
            final DynamicObject rubyEncoding = getContext().getEncodingManager().getRubyEncoding(StringOperations.encoding(string));
            final Rope rope = rope(string);
            Rope buf = null;
            CodeRange cr = rope.getCodeRange();
            final Encoding enc;
            final Encoding encidx;
            boolean tainted = false;

            if (validRangeProfile.profile(cr == CR_7BIT || cr == CR_VALID)) {
                return nil();
            }

            enc = rope.getEncoding();
            if (repl != nil()) {
                repl = strCompatAndValidNode.call(frame, string, "str_compat_and_valid", repl, rubyEncoding);
                tainted |= isTaintedNode.executeIsTainted(repl);
            }

            if (enc.isDummy()) {
                return nil();
            }
            encidx = enc;

            final byte[] pBytes = rope.getBytes();
            final int e = pBytes.length;

            if (asciiCompatibleProfile.profile(enc.isAsciiCompatible())) {
                int p = 0;
                int p1 = 0;
                final byte[] repBytes;
                final int replen;
                final boolean rep7bit_p;
                if (block != nil()) {
                    repBytes = null;
                    replen = 0;
                    rep7bit_p = false;
                } else if (repl != nil()) {
                    final Rope replRope = rope((DynamicObject) repl);
                    repBytes = replRope.getBytesCopy();
                    replen = repBytes.length;
                    rep7bit_p = (replRope.getCodeRange() == CodeRange.CR_7BIT);
                } else if (encidx == UTF8Encoding.INSTANCE) {
                    repBytes = SCRUB_REPL_UTF8;
                    replen = repBytes.length;
                    rep7bit_p = false;
                } else {
                    repBytes = SCRUB_REPL_ASCII;
                    replen = repBytes.length;
                    rep7bit_p = false;
                }
                cr = CR_7BIT;

                p = StringSupport.searchNonAscii(pBytes, p, e);
                if (p == -1) {
                    p = e;
                }
                while (p < e) {
                    int ret = enc.length(pBytes, p, e);
                    if (MBCLEN_NEEDMORE_P(ret)) {
                        break;
                    }
                    else if (MBCLEN_CHARFOUND_P(ret)) {
                        cr = CR_VALID;
                        p += MBCLEN_CHARFOUND_LEN(ret);
                    }
                    else if (MBCLEN_INVALID_P(ret)) {
                    /*
                     * p1~p: valid ascii/multibyte chars
                     * p ~e: invalid bytes + unknown bytes
                     */
                        int clen = enc.maxLength();
                        if (buf == null) {
                            buf = RopeConstants.EMPTY_ASCII_8BIT_ROPE;
                        }
                        if (p > p1) {
                            buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(pBytes, p1, p), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                        }

                        if (e - p < clen){
                            clen = e - p;
                        }
                        if (clen <= 2) {
                            clen = 1;
                        }
                        else {
                            final int q = p;
                            clen--;
                            for (; clen > 1; clen--) {
                                ret = enc.length(pBytes, q, q + clen);
                                if (MBCLEN_NEEDMORE_P(ret)) break;
                                if (MBCLEN_INVALID_P(ret)) continue;
                            }
                        }
                        if (repBytes != null) {
                            buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(repBytes, 0, replen), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                            if (!rep7bit_p) {
                                cr = CR_VALID;
                            }
                        }
                        else {
                            repl = yield(frame, block, createString(ArrayUtils.extractRange(pBytes, p, p + clen), enc));
                            repl = strCompatAndValidNode.call(frame, string, "str_compat_and_valid", repl, rubyEncoding);
                            tainted |= isTaintedNode.executeIsTainted(repl);
                            buf = makeConcatNode.executeMake(buf, rope((DynamicObject)repl), enc);
                            if (StringGuards.isValidCodeRange((DynamicObject) repl)) {
                                cr = CR_VALID;
                            }
                        }
                        p += clen;
                        p1 = p;
                        p = StringSupport.searchNonAscii(pBytes, p, e);
                        if (p == -1) {
                            p = e;
                            break;
                        }
                    }
                }
                if (buf == null) {
                    if (p == e) {
//                        setCodeRange(cr);
                        return nil();
                    }
                    buf = RopeConstants.EMPTY_ASCII_8BIT_ROPE;
                }
                if (p1 < p) {
                    buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(pBytes, p1, p), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                }
                if (p < e) {
                    if (repBytes != null) {
                        buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(repBytes, 0, replen), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                        if (!rep7bit_p) {
                            cr = CR_VALID;
                        }
                    }
                    else {
                        repl = yield(frame, block, createString(ArrayUtils.extractRange(pBytes, p, e), enc));
                        repl = strCompatAndValidNode.call(frame, string, "str_compat_and_valid", repl, rubyEncoding);
                        tainted |= isTaintedNode.executeIsTainted(repl);
                        buf = makeConcatNode.executeMake(buf, rope((DynamicObject)repl), enc);
                        if (rope((DynamicObject) repl).getCodeRange() == CR_VALID) {
                            cr = CR_VALID;
                        }
                    }
                }
            }
            else {
                /* ASCII incompatible */
                int p = 0;
                int p1 = 0;
                final int mbminlen = enc.minLength();

                final byte[] repBytes;
                if (repl != nil()) {
                    final Rope replRope = rope((DynamicObject) repl);
                    repBytes = replRope.getBytesCopy();
                } else if (encidx == UTF16BEEncoding.INSTANCE) {
                    repBytes = SCRUB_REPL_UTF16BE;
                } else if (encidx == UTF16LEEncoding.INSTANCE) {
                    repBytes = SCRUB_REPL_UTF16LE;
                } else if (encidx == UTF32BEEncoding.INSTANCE) {
                    repBytes = SCRUB_REPL_UTF32BE;
                } else if (encidx == UTF32LEEncoding.INSTANCE) {
                    repBytes = SCRUB_REPL_UTF32LE;
                } else {
                    repBytes = SCRUB_REPL_ASCII;
                }

                while (p < e) {
                    int ret = StringSupport.preciseLength(enc, pBytes, p, e);
                    if (MBCLEN_NEEDMORE_P(ret)) {
                        break;
                    }
                    else if (MBCLEN_CHARFOUND_P(ret)) {
                        p += MBCLEN_CHARFOUND_LEN(ret);
                    }
                    else if (MBCLEN_INVALID_P(ret)) {
                        final int q = p;
                        int clen = enc.maxLength();
                        if (buf == null) {
                            buf = RopeConstants.EMPTY_ASCII_8BIT_ROPE;
                        }

                        if (p > p1) {
                            buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(pBytes, p1, p), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                        }

                        if (e - p < clen) clen = e - p;
                        if (clen <= mbminlen * 2) {
                            clen = mbminlen;
                        }
                        else {
                            clen -= mbminlen;
                            for (; clen > mbminlen; clen-=mbminlen) {
                                ret = enc.length(pBytes, q, q + clen);
                                if (MBCLEN_NEEDMORE_P(ret)) break;
                                if (MBCLEN_INVALID_P(ret)) continue;
                            }
                        }
                        if (repBytes != null) {
                            buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(repBytes, 0, repBytes.length), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                        } else {
                            repl = yield(frame, block, createString(ArrayUtils.extractRange(pBytes, p, e), enc));
                            repl = strCompatAndValidNode.call(frame, string, "str_compat_and_valid", repl, rubyEncoding);
                            tainted |= isTaintedNode.executeIsTainted(repl);
                            buf = makeConcatNode.executeMake(buf, rope((DynamicObject) repl), enc);
                        }
                        p += clen;
                        p1 = p;
                    }
                }
                if (buf == null) {
                    if (p == e) {
//                        setCodeRange(CR_VALID);
                        return nil();
                    }
                    buf = RopeConstants.EMPTY_ASCII_8BIT_ROPE;
                }
                if (p1 < p) {
                    buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(pBytes, p1, p), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                }
                if (p < e) {
                    if (repBytes != null) {
                        buf = makeConcatNode.executeMake(buf, RopeOperations.create(ArrayUtils.extractRange(repBytes, 0, repBytes.length), ASCIIEncoding.INSTANCE, CodeRange.CR_VALID), enc);
                    }
                    else {
                        repl = yield(frame, block, createString(ArrayUtils.extractRange(pBytes, p, e), enc));
                        repl = strCompatAndValidNode.call(frame, string, "str_compat_and_valid", repl, rubyEncoding);
                        tainted |= isTaintedNode.executeIsTainted(repl);
                        buf = makeConcatNode.executeMake(buf, rope((DynamicObject)repl), enc);
                    }
                }
                cr = CR_VALID;
            }

            final boolean isTaint = isTaintedNode.executeIsTainted(string);

            final DynamicObject resultString = createString(buf);

            if (tainted | isTaint) {
                taintNode.executeTaint(resultString);
            }

//            ((RubyString)buf).setEncodingAndCodeRange(enc, cr);

            return resultString;
        }

        public Object yield(VirtualFrame frame, DynamicObject block, Object... arguments) {
            return yieldNode.dispatch(frame, block, arguments);
        }

    }

    @CoreMethod(names = "swapcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class SwapcaseBangNode extends CoreMethodArrayArgumentsNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

        @Specialization(guards = "isAsciiCompatible(string)")
        public DynamicObject dumpAsciiCompatible(DynamicObject string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);
            outputBytes.setEncoding(encoding(string));

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT));

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

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.ropeFromByteList(outputBytes, CodeRange.CR_7BIT));

            return result;
        }

        @TruffleBoundary
        private ByteList dumpCommon(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            return dumpCommon(rope(string));
        }

        private ByteList dumpCommon(Rope rope) {
            ByteList buf = null;
            Encoding enc = rope.getEncoding();

            int p = 0;
            int end = rope.byteLength();
            byte[]bytes = rope.getBytes();

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
            p = 0;
            end = rope.byteLength();

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

    @CoreMethod(names = "setbyte", required = 2, raiseIfFrozenSelf = true, lowerFixnum = { 1, 2 })
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "value")
    })
    @ImportStatic(StringGuards.class)
    public abstract static class SetByteNode extends CoreMethodNode {

        @Child private CheckIndexNode checkIndexNode = StringNodesFactory.CheckIndexNodeGen.create(null, null);
        @Child private RopeNodes.MakeConcatNode composedMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
        @Child private RopeNodes.MakeConcatNode middleMakeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);
        @Child private RopeNodes.MakeSubstringNode leftMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        @Child private RopeNodes.MakeSubstringNode rightMakeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return FixnumLowerNodeGen.create(ToIntNodeGen.create(index));
        }

        @CreateCast("value") public RubyNode coerceValueToInt(RubyNode value) {
            return FixnumLowerNodeGen.create(ToIntNodeGen.create(value));
        }

        public abstract int executeSetByte(DynamicObject string, int index, Object value);

        @Specialization(guards = "!isRopeBuffer(string)")
        public int setByte(DynamicObject string, int index, int value) {
            final Rope rope = rope(string);
            final int normalizedIndex = checkIndexNode.executeCheck(index, rope.byteLength());

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
            final int normalizedIndex = checkIndexNode.executeCheck(index, rope.byteLength());

            rope.getByteList().set(normalizedIndex, value);

            return value;
        }

    }

    @NodeChildren({ @NodeChild("index"), @NodeChild("length") })
    public static abstract class CheckIndexNode extends RubyNode {

        public abstract int executeCheck(int index, int length);

        @Specialization
        protected int checkIndex(int index, int length,
                                 @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                                 @Cached("create()") BranchProfile errorProfile) {
            if (index >= length) {
                errorProfile.enter();
                throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(index, this));
            }

            if (negativeIndexProfile.profile(index < 0)) {
                if (-index > length) {
                    errorProfile.enter();
                    throw new RaiseException(getContext().getCoreExceptions().indexErrorOutOfString(index, this));
                }

                index += length;
            }

            return index;
        }

    }

    @NodeChildren({ @NodeChild("index"), @NodeChild("length") })
    public static abstract class NormalizeIndexNode extends RubyNode {

        public abstract int executeNormalize(int index, int length);

        @Specialization
        protected int normalizeIndex(int index, int length,
                                     @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            if (negativeIndexProfile.profile(index < 0)) {
                return index + length;
            }

            return index;
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
                    StringOperations.setRope(string, RopeOperations.ropeFromByteList(buffer));
                }
            } else {
                if (! squeezeCommonMultiByte(buffer, squeeze, null, encoding(string), false)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, RopeOperations.ropeFromByteList(buffer));
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
                toStrNode = insert(ToStrNodeGen.create(null));
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
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(null, null));
            }

            final Rope rope = rope(string);
            final ByteList buffer = RopeOperations.toByteListCopy(rope);

            DynamicObject otherStr = otherStrings[0];
            Rope otherRope = rope(otherStr);
            Encoding enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherRope, squeeze, null, true, enc);

            boolean singlebyte = rope.isSingleByteOptimizable() && otherRope.isSingleByteOptimizable();

            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];
                otherRope = rope(otherStr);
                enc = checkEncodingNode.executeCheckEncoding(string, otherStr);
                singlebyte = singlebyte && otherRope.isSingleByteOptimizable();
                tables = StringSupport.trSetupTable(otherRope, squeeze, tables, false, enc);
            }

            if (singleByteOptimizableProfile.profile(singlebyte)) {
                if (! StringSupport.singleByteSqueeze(buffer, squeeze)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, RopeOperations.ropeFromByteList(buffer));
                }
            } else {
                if (! StringSupport.multiByteSqueeze(buffer, squeeze, tables, enc, true)) {
                    return nil();
                } else {
                    StringOperations.setRope(string, RopeOperations.ropeFromByteList(buffer));
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
                final ByteList succByteList = StringSupport.succCommon(rope);

                StringOperations.setRope(string, RopeOperations.ropeFromByteList(succByteList, rope.getCodeRange()));
            }

            return string;
        }
    }

    // String#sum is in Java because without OSR we can't warm up the Rubinius implementation

    @CoreMethod(names = "sum", optional = 1)
    public abstract static class SumNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode addNode = DispatchHeadNodeFactory.createMethodCall();
        @Child private CallDispatchHeadNode subNode = DispatchHeadNodeFactory.createMethodCall();
        @Child private CallDispatchHeadNode shiftNode = DispatchHeadNodeFactory.createMethodCall();
        @Child private CallDispatchHeadNode andNode = DispatchHeadNodeFactory.createMethodCall();

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
            return new DoubleConverter().parse(rope(string), false, true);
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

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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
            return ToStrNodeGen.create(fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(toStr);
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
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(new RubyNode[] {}));
                }

                return deleteBangNode.executeDeleteBang(frame, self, new DynamicObject[] { fromStr });
            }

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(null, null));
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
            return ToStrNodeGen.create(fromStr);
        }

        @CreateCast("toStrNode") public RubyNode coerceToStrToString(RubyNode toStr) {
            return ToStrNodeGen.create(toStr);
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
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(new RubyNode[] {}));
                }

                return deleteBangNode.executeDeleteBang(frame, self, new DynamicObject[] { fromStr });
            }

            if (checkEncodingNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                checkEncodingNode = insert(EncodingNodesFactory.CheckEncodingNodeGen.create(null, null));
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
                    taintNode = insert(TaintNode.create());
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

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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
                StringOperations.setRope(string, RopeOperations.ropeFromByteList(bytes, rope.getCodeRange()));

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

        @Child private RopeNodes.GetCodePointNode getCodePointNode = RopeNodes.GetCodePointNode.create();
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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

        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodes.MakeSubstringNode.create();

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

            final Encoding e1 = checkEncodingNode.executeCheckEncoding(self, fromStr);
            final Encoding e2 = checkEncodingNode.executeCheckEncoding(self, toStr);
            final Encoding enc = e1 == e2 ? e1 : checkEncodingNode.executeCheckEncoding(fromStr, toStr);

            final Rope ret = StringSupport.trTransHelper(rope(self), rope(fromStr), rope(toStr), e1, enc, sFlag);

            if (ret == null) {
                return context.getCoreLibrary().getNilObject();
            }

            StringOperations.setRope(self, ret);

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

        @Child private StringAppendNode stringAppendNode = StringNodesFactory.StringAppendNodeGen.create(null, null);

        public static StringAppendPrimitiveNode create() {
            return StringNodesFactory.StringAppendPrimitiveNodeFactory.create(null);
        }

        public abstract DynamicObject executeStringAppend(DynamicObject string, DynamicObject other);

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject stringAppend(DynamicObject string, DynamicObject other) {
            StringOperations.setRope(string, stringAppendNode.executeStringAppend(string, other));

            return string;
        }

    }

    @Primitive(name = "string_awk_split")
    public static abstract class StringAwkSplitPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.GetCodePointNode getCodePointNode = RopeNodes.GetCodePointNode.create();
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        @Child private TaintResultNode taintResultNode = new TaintResultNode();

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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private NormalizeIndexNode normalizeIndexNode = StringNodesFactory.NormalizeIndexNodeGen.create(null, null);
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        @Child private TaintResultNode taintResultNode = new TaintResultNode();

        public static StringByteSubstringPrimitiveNode create() {
            return StringNodesFactory.StringByteSubstringPrimitiveNodeFactory.create(null, null, null);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ArrayAttributeCastNodeGen.create("index", index);
        }

        @CreateCast("length") public RubyNode coerceLengthToInt(RubyNode length) {
            return ArrayAttributeCastNodeGen.create("length", length);
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
            final int normalizedIndex = normalizeIndexNode.executeNormalize(index, stringByteLength);

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
                                            @Cached("create()") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, 1);
        }

        @Specialization(guards = { "!indexOutOfBounds(string, byteIndex)", "!isSingleByteOptimizable(string)" })
        public Object stringChrAt(DynamicObject string, int byteIndex,
                                  @Cached("create()") StringByteSubstringPrimitiveNode stringByteSubstringNode) {
            // Taken from Rubinius's Character::create_from.

            final Rope rope = rope(string);
            final int end = rope.byteLength();
            final int c = StringSupport.preciseLength(rope.getEncoding(), rope.getBytes(), byteIndex, end);

            if (! StringSupport.MBCLEN_CHARFOUND_P(c)) {
                return nil();
            }

            final int n = StringSupport.MBCLEN_CHARFOUND_LEN(c);
            if (n + byteIndex > end) {
                return nil();
            }

            return stringByteSubstringNode.executeStringByteSubstring(string, byteIndex, n);
        }

        protected static boolean indexOutOfBounds(DynamicObject string, int byteIndex) {
            return ((byteIndex < 0) || (byteIndex >= rope(string).byteLength()));
        }

    }

    @Primitive(name = "string_compare_substring")
    public static abstract class StringCompareSubstringPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private NormalizeIndexNode normalizeIndexNode = StringNodesFactory.NormalizeIndexNodeGen.create(null, null);

        @Specialization(guards = "isRubyString(other)")
        public int stringCompareSubstring(VirtualFrame frame, DynamicObject string, DynamicObject other, int start, int size,
                @Cached("create()") BranchProfile errorProfile) {
            // Transliterated from Rubinius C++.

            final int stringLength = StringOperations.rope(string).characterLength();
            final int otherLength = StringOperations.rope(other).characterLength();

            start = normalizeIndexNode.executeNormalize(start, otherLength);

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
            return ArrayUtils.memcmp(rope.getBytes(), 0, otherRope.getBytes(), start, size);
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

        @Child private StringAreComparableNode areComparableNode;

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

        @Child private IsTaintedNode isTaintedNode = IsTaintedNode.create();
        @Child private TaintNode taintNode = TaintNode.create();
        private final ConditionProfile taintedProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        public DynamicObject string_escape(DynamicObject string) {
            final DynamicObject result = StringOperations.createString(getContext(), rbStrEscape(rope(string)));

            if (taintedProfile.profile(isTaintedNode.isTainted(string))) {
                taintNode.executeTaint(result);
            }

            return result;
        }

        // MRI: rb_str_escape
        @TruffleBoundary
        private static Rope rbStrEscape(Rope str) {
            Encoding enc = str.getEncoding();
            byte[] pBytes = str.getBytes();
            int p = 0;
            int pend = str.byteLength();
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

            result.setEncoding(USASCIIEncoding.INSTANCE);
            return RopeOperations.ropeFromByteList(result, CodeRange.CR_7BIT);
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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode = RopeNodesFactory.MakeSubstringNodeGen.create(null, null, null);
        @Child private TaintResultNode taintResultNode;

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
                taintResultNode = insert(new TaintResultNode());
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
        public Object stringToF(DynamicObject string, boolean strict,
                                @Cached("create(getSourceIndexLength())") FixnumOrBignumNode fixnumOrBignumNode) {
            final Rope rope = rope(string);
            if (rope.isEmpty()) {
                throw new RaiseException(coreExceptions().argumentError(coreStrings().INVALID_VALUE_FOR_FLOAT.getRope(), this));
            }
            if (string.toString().startsWith("0x")) {
                try {
                    return Double.parseDouble(string.toString());
                } catch (NumberFormatException e) {
                    // Try falling back to this implementation if the first fails, niether 100% complete
                    final Object result = ConvertBytes.byteListToInum19(getContext(), this, fixnumOrBignumNode, string, 16, true);
                    if (result instanceof Integer) {
                        return ((Integer) result).doubleValue();
                    } else if (result instanceof Long) {
                        return ((Long) result).doubleValue();
                    } else if (result instanceof Double) {
                        return result;
                    } else {
                        return null;
                    }
                }
            }
            try {
                return new DoubleConverter().parse(rope, strict, true);
            } catch (NumberFormatException e) {
                if (strict) {
                    throw new RaiseException(coreExceptions().argumentError(coreStrings().INVALID_VALUE_FOR_FLOAT.getRope(), this));
                }
                return 0.0;
            }
        }
    }

    @Primitive(name = "string_index", lowerFixnum = 2)
    @ImportStatic(StringGuards.class)
    public static abstract class StringIndexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private StringByteCharacterIndexNode byteIndexToCharIndexNode = StringNodesFactory.StringByteCharacterIndexNodeFactory.create(null);
        @Child private NormalizeIndexNode normalizeIndexNode = StringNodesFactory.NormalizeIndexNodeGen.create(null, null);

        @Specialization(guards = { "isRubyString(pattern)", "isBrokenCodeRange(pattern)" })
        public DynamicObject stringIndexBrokenCodeRange(DynamicObject string, DynamicObject pattern, int start) {
            return nil();
        }

        @Specialization(guards = { "isRubyString(pattern)", "!isBrokenCodeRange(pattern)" })
        public Object stringIndex(VirtualFrame frame, DynamicObject string, DynamicObject pattern, int start) {
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

            offset = normalizeIndexNode.executeNormalize(offset, sourceLen);

            if (offset < 0) {
                return -1;
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
                if (source[i] != first) {
                    while (++i <= max && source[i] != first) {
                    }
                }

                if (i <= max) {
                    int j = i + 1;
                    int end = j + targetCount - 1;
                    for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++) {
                    }

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

        @Specialization(guards = { "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int stringCharacterByteIndexFixedWidthEncoding(DynamicObject string, int index, int start) {
            final Encoding encoding = encoding(string);

            return start + index * encoding.minLength();
        }

        @TruffleBoundary
        @Specialization(guards = { "!isSingleByteOptimizable(string)", "!isFixedWidthEncoding(string)" })
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

    @NodeChildren({ @NodeChild("string"), @NodeChild("characterIndex") })
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteIndexFromCharIndexNode extends RubyNode {

        public static StringByteIndexFromCharIndexNode create() {
            return StringNodesFactory.StringByteIndexFromCharIndexNodeGen.create(null, null);
        }

        @CreateCast("characterIndex") public RubyNode coerceCharacterIndexToInt(RubyNode characterIndex) {
            return FixnumLowerNodeGen.create(characterIndex);
        }

        public abstract Object executeFindByteIndex(DynamicObject string, int characterIndex);

        @Specialization(guards = "characterIndex < 0")
        protected Object byteIndexNegativeIndex(DynamicObject string, int characterIndex) {
            throw new RaiseException(
                    getContext().getCoreExceptions().argumentError(
                            coreStrings().CHARACTER_INDEX_NEGATIVE.getRope(), this));
        }

        @Specialization(guards = "characterIndexTooLarge(string, characterIndex)")
        protected Object byteIndexTooLarge(DynamicObject string, int characterIndex) {
            return nil();
        }

        @Specialization(guards = { "characterIndexInBounds(string, characterIndex)", "isSingleByteOptimizable(string)" })
        protected Object singleByte(DynamicObject string, int characterIndex) {
            return characterIndex;
        }

        @Specialization(guards = { "characterIndexInBounds(string, characterIndex)", "!isSingleByteOptimizable(string)" })
        protected Object multiBytes(DynamicObject string, int characterIndex,
                                    @Cached("createBinaryProfile()") ConditionProfile indexTooLargeProfile,
                                    @Cached("createBinaryProfile()") ConditionProfile invalidByteProfile,
                                    @Cached("create()") BranchProfile errorProfile) {
            // Taken from Rubinius's String::byte_index.

            final Rope rope = rope(string);
            final Encoding enc = rope.getEncoding();
            int p = 0;
            final int e = p + rope.byteLength();

            int i, k = characterIndex;

            for (i = 0; i < k && p < e; i++) {
                final int c = StringSupport.preciseLength(enc, rope.getBytes(), p, e);

                // TODO (nirvdrum 22-Dec-16): Consider having a specialized version for CR_BROKEN strings to avoid these checks.
                // If it's an invalid byte, just treat it as a single byte
                if(invalidByteProfile.profile(! StringSupport.MBCLEN_CHARFOUND_P(c))) {
                    ++p;
                } else {
                    p += StringSupport.MBCLEN_CHARFOUND_LEN(c);
                }
            }

            // TODO (nirvdrum 22-Dec-16): Since we specialize elsewhere on index being too large, do we need this? Can character boundary search in a CR_BROKEN string cause us to encounter this case?
            if (indexTooLargeProfile.profile(i < k)) {
                return nil();
            } else {
                return p;
            }
        }

        protected boolean characterIndexTooLarge(DynamicObject string, int characterIndex) {
            return characterIndex > rope(string).characterLength();
        }

        protected boolean characterIndexInBounds(DynamicObject string, int characterIndex) {
            return characterIndex >= 0 && !characterIndexTooLarge(string, characterIndex);
        }

    }

    @Primitive(name = "string_byte_index", needsSelf = false, lowerFixnum = 1)
    @ImportStatic(StringGuards.class)
    public static abstract class StringByteIndexNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        protected Object findByteIndexFromCharIndex(DynamicObject string, int characterIndex, int offset,
                                                    @Cached("create()") StringByteIndexFromCharIndexNode stringByteIndexFromCharIndexNode) {
            return stringByteIndexFromCharIndexNode.executeFindByteIndex(string, characterIndex);
        }

        @Specialization(guards = "isRubyString(pattern)")
        public Object pattern(DynamicObject string, DynamicObject pattern, int offset,
                              @Cached("createBinaryProfile()") ConditionProfile emptyPatternProfile,
                              @Cached("createBinaryProfile()") ConditionProfile brokenCodeRangeProfile,
                              @Cached("create()") BranchProfile errorProfile,
                              @Cached("create()")EncodingNodes.CheckEncodingNode checkEncodingNode) {
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
    public static abstract class StringPreviousByteIndexNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "index < 0")
        public Object negativeIndex(DynamicObject string, int index) {
            throw new RaiseException(coreExceptions().argumentError("negative index given", this));
        }

        @Specialization(guards = "index == 0")
        public Object zeroIndex(DynamicObject string, int index) {
            return nil();
        }

        @Specialization(guards = { "index > 0", "isSingleByteOptimizable(string)" })
        public int singleByteOptimizable(DynamicObject string, int index) {
            return index - 1;
        }

        @Specialization(guards = { "index > 0", "!isSingleByteOptimizable(string)", "isFixedWidthEncoding(string)" })
        public int fixedWidthEncoding(DynamicObject string, int index,
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
        public Object other(DynamicObject string, int index) {
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

    @Primitive(name = "string_rindex", lowerFixnum = 2)
    public static abstract class StringRindexPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Child private RopeNodes.GetByteNode patternGetByteNode = RopeNodes.GetByteNode.create();
        @Child private RopeNodes.GetByteNode stringGetByteNode = RopeNodes.GetByteNode.create();

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
                        if (ArrayUtils.memcmp(stringRope.getBytes(), cur, patternRope.getBytes(), 0, matchSize) == 0) {
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

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodes.MakeLeafRopeNode.create();
        @Child private RopeNodes.MakeRepeatingNode makeRepeatingNode = RopeNodes.MakeRepeatingNode.create();

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

        protected boolean indexAtStartBound(int index) {
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
                                   @Cached("create(getSourceIndexLength())") FixnumOrBignumNode fixnumOrBignumNode) {
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

        @Child private RopeNodes.MakeConcatNode makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);
        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode = RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null);

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
        @Child private NormalizeIndexNode normalizeIndexNode = StringNodesFactory.NormalizeIndexNodeGen.create(null, null);
        @Child private RopeNodes.MakeSubstringNode makeSubstringNode;
        @Child private TaintResultNode taintResultNode;

        public abstract Object execute(VirtualFrame frame, DynamicObject string, int beg, int len);

        @Specialization(guards = "!indexTriviallyOutOfBounds(string, beg, len)")
        public Object stringSubstring(DynamicObject string, int beg, int len,
                                      @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile tooLargeTotalProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile mutableRopeProfile,
                                      @Cached("createBinaryProfile()") ConditionProfile foundSingleByteOptimizableDescendentProfile) {
            final Rope rope = rope(string);

            int index = normalizeIndexNode.executeNormalize(beg, rope.characterLength());
            int length = len;

            if (negativeIndexProfile.profile(index < 0)) {
                return nil();
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
            } else if (StringGuards.isValidCodeRange(string) && enc instanceof UTF8Encoding) {
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
                taintResultNode = insert(new TaintResultNode());
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
                taintResultNode = insert(new TaintResultNode());
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

    @NodeChildren({ @NodeChild("string"), @NodeChild("other") })
    public static abstract class StringAppendNode extends RubyNode {

        @Child private EncodingNodes.CheckEncodingNode checkEncodingNode = EncodingNodesFactory.CheckEncodingNodeGen.create(null, null);
        @Child private RopeNodes.MakeConcatNode makeConcatNode = RopeNodesFactory.MakeConcatNodeGen.create(null, null, null);

        public static StringAppendNode create() {
            return StringNodesFactory.StringAppendNodeGen.create(null, null);
        }

        public abstract Rope executeStringAppend(DynamicObject string, DynamicObject other);

        @Specialization
        public Rope stringAppend(DynamicObject string, DynamicObject other) {
            assert RubyGuards.isRubyString(string);
            assert RubyGuards.isRubyString(other);

            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding compatibleEncoding = checkEncodingNode.executeCheckEncoding(string, other);

            return makeConcatNode.executeMake(left, right, compatibleEncoding);
        }

    }

}
