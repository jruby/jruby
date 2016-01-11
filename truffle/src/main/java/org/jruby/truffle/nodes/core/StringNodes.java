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
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.POSIX;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.format.parser.UnpackCompiler;
import org.jruby.truffle.format.runtime.PackResult;
import org.jruby.truffle.format.runtime.exceptions.*;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.StringCachingGuards;
import org.jruby.truffle.nodes.cast.CmpIntNode;
import org.jruby.truffle.nodes.cast.CmpIntNodeGen;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayCoreMethodNode;
import org.jruby.truffle.nodes.core.fixnum.FixnumLowerNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.rubinius.ByteArrayNodes;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodesFactory;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.EncodingOperations;
import org.jruby.truffle.runtime.core.StringCodeRangeableWrapper;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.rope.Rope;
import org.jruby.truffle.runtime.rope.RopeOperations;
import org.jruby.util.*;
import org.jruby.util.io.EncodingUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.jruby.truffle.runtime.core.StringOperations.EMPTY_UTF8_ROPE;
import static org.jruby.truffle.runtime.core.StringOperations.rope;
import static org.jruby.truffle.runtime.core.StringOperations.encoding;
import static org.jruby.truffle.runtime.core.StringOperations.codeRange;

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
            return allocateObjectNode.allocate(rubyClass, EMPTY_UTF8_ROPE, null);
        }

    }

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class AddNode extends CoreMethodNode {

        @Child private TaintResultNode taintResultNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(getContext(), getSourceSection());
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject add(DynamicObject string, DynamicObject other) {
            final Rope left = rope(string);
            final Rope right = rope(other);

            final Encoding enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeableReadOnly(other), this);

            final Rope concatRope = RopeOperations.concat(left, right, enc);

            final DynamicObject ret = Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(),
                    concatRope,
                    null);

            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject multiply(DynamicObject string, int times) {
            if (times < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }

            final Rope retRope;

            if (times == 0) {
                retRope = StringOperations.EMPTY_UTF8_ROPE;
            } else if (times == 1) {
                retRope = rope(string);
            } else {
                final Rope baseRope = rope(string);
                final Rope concatLeafRope = RopeOperations.concat(baseRope, baseRope, baseRope.getEncoding());

                final boolean timesIsPowerOf2 = (times & (times - 1)) == 0;
                final double log2_times = Math.log(times) / Math.log(2);

                final int lowestLevelWidth = timesIsPowerOf2 ? times / 2 : (int) (Math.pow(2, Math.floor(log2_times)));
                final int populateNode = times - lowestLevelWidth;

                Rope[] nextLevel = new Rope[lowestLevelWidth];
                for (int i = 0; i < nextLevel.length; i++) {
                    if (i < populateNode) {
                        nextLevel[i] = concatLeafRope;
                    } else {
                        nextLevel[i] = null;
                    }
                }

                final int levels = (int) Math.ceil(log2_times);
                boolean canCacheLeftTree = true;
                boolean canCacheRightTree = true;

                for (int level = levels - 1; level > 0; level--) {
                    final int levelWidth = (int) Math.pow(2, level - 1);
                    final Rope[] currentLevel = new Rope[levelWidth];
                    Rope cachedRope = null;

                    for (int i = 0; i < levelWidth; i++) {
                        final Rope left = nextLevel[i * 2];
                        final Rope right = nextLevel[i * 2 + 1];

                        if (left == null) {
                            currentLevel[i] = concatLeafRope;

                            if (i < levelWidth / 2) {
                                canCacheLeftTree = false;
                            } else {
                                canCacheRightTree = false;
                            }
                        } else if (right == null) {
                            currentLevel[i] = RopeOperations.concat(left, baseRope, baseRope.getEncoding());

                            if (i < levelWidth / 2) {
                                canCacheLeftTree = false;
                            } else {
                                canCacheRightTree = false;
                            }
                        } else {
                            if ((canCacheLeftTree && i < levelWidth / 2) || (canCacheRightTree && i >= levelWidth / 2)) {
                                if (cachedRope == null) {
                                    cachedRope = RopeOperations.concat(left, right, baseRope.getEncoding());
                                }

                                currentLevel[i] = cachedRope;
                            } else {
                                currentLevel[i] = RopeOperations.concat(left, right, baseRope.getEncoding());
                            }
                        }
                    }

                    nextLevel = currentLevel;
                }

                retRope = nextLevel[0];
            }

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), retRope, null);
        }

        @Specialization(guards = "isRubyBignum(times)")
        public DynamicObject multiply(DynamicObject string, DynamicObject times) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(
                    getContext().getCoreLibrary().rangeError("bignum too big to convert into `long'", this));
        }

        @Specialization(guards = { "!isRubyBignum(times)", "!isInteger(times)" })
        public DynamicObject multiply(VirtualFrame frame, DynamicObject string, Object times) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return multiply(string, toIntNode.doInt(frame, times));
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

            if (respondToNode.doesRespondToString(frame, b, create7BitString(StringOperations.encodeByteList("to_str", UTF8Encoding.INSTANCE)), false)) {
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

            final int ret = StringOperations.getByteListReadOnly(a).cmp(StringOperations.getByteListReadOnly(b));

            if ((ret == 0) && !StringSupport.areComparable(StringOperations.getCodeRangeableReadOnly(a), StringOperations.getCodeRangeableReadOnly(b))) {
                return encoding(a).getIndex() > encoding(b).getIndex() ? 1 : -1;
            }

            return ret;
        }

        @Specialization(guards = "!isRubyString(b)")
        public Object compare(VirtualFrame frame, DynamicObject a, Object b) {
            CompilerDirectives.transferToInterpreter();

            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            if (respondToToStrNode.doesRespondToString(frame, b, create7BitString(StringOperations.encodeByteList("to_str", UTF8Encoding.INSTANCE)), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
                }

                try {
                    final DynamicObject coerced = toStrNode.executeToStr(frame, b);

                    return compare(a, coerced);
                } catch (RaiseException e) {
                    if (Layouts.BASIC_OBJECT.getLogicalClass(e.getRubyException()) == getContext().getCoreLibrary().getTypeErrorClass()) {
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

            if (respondToCmpNode.doesRespondToString(frame, b, create7BitString(StringOperations.encodeByteList("<=>", UTF8Encoding.INSTANCE)), false)) {
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

    @CoreMethod(names = {"[]", "slice"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1}, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private SizeNode sizeNode;
        @Child private StringPrimitiveNodes.StringSubstringPrimitiveNode substringNode;
        @Child private AllocateObjectNode allocateObjectNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "wasNotProvided(length) || isRubiniusUndefined(length)")
        public Object getIndex(VirtualFrame frame, DynamicObject string, int index, Object length) {
            final int stringLength = getSizeNode().executeInteger(frame, string);
            int normalizedIndex = StringOperations.normalizeIndex(stringLength, index);

            if (normalizedIndex < 0 || normalizedIndex >= StringOperations.byteLength(string)) {
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

            final int stringLength = getSizeNode().executeInteger(frame, string);
            begin = StringOperations.normalizeIndex(stringLength, begin);

            if (begin < 0 || begin > stringLength) {
                outOfBounds.enter();
                return nil();
            } else {

                if (begin == stringLength) {
                    final ByteList byteList = new ByteList();
                    byteList.setEncoding(encoding(string));
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(byteList, StringSupport.CR_UNKNOWN), null);
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
            return sliceCapture(frame, string, regexp, 0);
        }

        @Specialization(guards = {"isRubyRegexp(regexp)", "wasProvided(capture)"})
        public Object sliceCapture(VirtualFrame frame, DynamicObject string, DynamicObject regexp, Object capture) {
            // Extracted from Rubinius's definition of String#[].
            return ruby(frame, "match, str = subpattern(index, other); Regexp.last_match = match; str", "index", regexp, "other", capture);
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
            return object == getContext().getCoreLibrary().getRubiniusUndefined();
        }

        private SizeNode getSizeNode() {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreter();
                sizeNode = insert(StringNodesFactory.SizeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }

            return sizeNode;
        }

    }

    @CoreMethod(names = "ascii_only?")
    public abstract static class ASCIIOnlyNode extends CoreMethodArrayArgumentsNode {

        public ASCIIOnlyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean asciiOnly(DynamicObject string) {
            return StringOperations.scanForCodeRange(string) == StringSupport.CR_7BIT;
        }

    }

    @CoreMethod(names = "b", taintFromSelf = true)
    public abstract static class BNode extends CoreMethodArrayArgumentsNode {

        public BNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject b(DynamicObject string) {
            return createString(RopeOperations.template(rope(string), ASCIIEncoding.INSTANCE));
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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), store, store.length);
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
        public Object caseCmpSingleByte(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19.

            if (StringSupport.areCompatible(StringOperations.getCodeRangeable(string), StringOperations.getCodeRangeable(other)) == null) {
                return nil();
            }

            return StringOperations.getByteListReadOnly(string).caseInsensitiveCmp(StringOperations.getByteListReadOnly(other));
        }

        @Specialization(guards = {"isRubyString(other)", "!bothSingleByteOptimizable(string, other)"})
        public Object caseCmp(DynamicObject string, DynamicObject other) {
            // Taken from org.jruby.RubyString#casecmp19 and

            final Encoding encoding = StringSupport.areCompatible(StringOperations.getCodeRangeable(string), StringOperations.getCodeRangeable(other));

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

    @CoreMethod(names = "chop!", raiseIfFrozenSelf = true)
    public abstract static class ChopBangNode extends CoreMethodArrayArgumentsNode {

        @Child private SizeNode sizeNode;

        public ChopBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        @Specialization
        public Object chopBang(VirtualFrame frame, DynamicObject string) {
            if (sizeNode.executeInteger(frame, string) == 0) {
                return nil();
            }

            final int newLength = choppedLength(string);

            StringOperations.getByteList(string).view(0, newLength);

            if (codeRange(string) != StringSupport.CR_7BIT) {
                StringOperations.clearCodeRange(string);
            }

            return string;
        }

        @TruffleBoundary
        private int choppedLength(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            return StringSupport.choppedLength19(StringOperations.getCodeRangeable(string), getContext().getRuntime());
        }
    }

    @CoreMethod(names = "count", rest = true)
    public abstract static class CountNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public CountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public int count(VirtualFrame frame, DynamicObject string, Object[] args) {
            if (rope(string).isEmpty()) {
                return 0;
            }

            if (args.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
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
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), getContext().getRuntime(), table, null, true, enc);
            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];

                assert RubyGuards.isRubyString(otherStr);

                enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(otherStr), this);
                tables = StringSupport.trSetupTable(StringOperations.getByteListReadOnly(otherStr), getContext().getRuntime(), table, tables, false, enc);
            }

            return StringSupport.countCommon19(StringOperations.getByteListReadOnly(string), getContext().getRuntime(), table, tables, enc);
        }
    }

    @CoreMethod(names = "crypt", required = 1, taintFromSelf = true, taintFromParameter = 0)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "string"),
            @NodeChild(type = RubyNode.class, value = "salt")
    })
    public abstract static class CryptNode extends CoreMethodNode {

        public CryptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("salt") public RubyNode coerceSaltToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "isRubyString(salt)")
        public Object crypt(DynamicObject string, DynamicObject salt) {
            // Taken from org.jruby.RubyString#crypt.

            final ByteList value = StringOperations.getByteListReadOnly(string);

            final Encoding ascii8bit = getContext().getRuntime().getEncodingService().getAscii8bitEncoding();
            ByteList otherBL = StringOperations.getByteListReadOnly(salt).dup();
            final DynamicObject otherStr = createString(otherBL);

            StringOperations.modify(otherStr);
            StringSupport.associateEncoding(StringOperations.getCodeRangeable(otherStr), ascii8bit);

            if (otherBL.length() < 2) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final POSIX posix = posix();
            final byte[] keyBytes = Arrays.copyOfRange(value.unsafeBytes(), value.begin(), value.realSize());
            final byte[] saltBytes = Arrays.copyOfRange(otherBL.unsafeBytes(), otherBL.begin(), otherBL.realSize());

            if (saltBytes[0] == 0 || saltBytes[1] == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("salt too short (need >= 2 bytes)", this));
            }

            final byte[] cryptedString = posix.crypt(keyBytes, saltBytes);

            // We differ from MRI in that we do not process salt to make it work and we will
            // return any errors via errno.
            if (cryptedString == null) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().errnoError(posix.errno(), this));
            }

            final DynamicObject result = createString(new ByteList(cryptedString, 0, cryptedString.length - 1));
            StringSupport.associateEncoding(StringOperations.getCodeRangeable(result), ascii8bit);

            return result;
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "data")
    public abstract static class DataNode extends CoreMethodArrayArgumentsNode {

        public DataNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject data(DynamicObject string) {
            // TODO (nirvdrum 08-Jan-16) ByteArrays might be better served if backed by a byte[] instead of a ByteList.
            return ByteArrayNodes.createByteArray(getContext().getCoreLibrary().getByteArrayFactory(), StringOperations.getByteListReadOnly(string));
        }
    }

    @CoreMethod(names = "delete!", rest = true, raiseIfFrozenSelf = true)
    public abstract static class DeleteBangNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public DeleteBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public Object deleteBang(VirtualFrame frame, DynamicObject string, Object... args) {
            if (rope(string).isEmpty()) {
                return nil();
            }

            if (args.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
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
            Encoding enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(otherString), this);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteList(otherString),
                    getContext().getRuntime(),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                assert RubyGuards.isRubyString(otherStrings[i]);

                enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(otherStrings[i]), this);
                tables = StringSupport.trSetupTable(StringOperations.getByteList(otherStrings[i]), getContext().getRuntime(), squeeze, tables, false, enc);
            }

            if (StringSupport.delete_bangCommon19(StringOperations.getCodeRangeable(string), getContext().getRuntime(), squeeze, tables, enc) == null) {
                return nil();
            }

            return string;
        }
    }

    @CoreMethod(names = "downcase!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DowncaseBangNode extends CoreMethodArrayArgumentsNode {

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
                Layouts.STRING.setRope(string, RopeOperations.create(outputBytes, rope.getEncoding(), rope.getCodeRange()));

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
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", encoding), this));
            }

            if (emptyStringProfile.profile(rope.isEmpty())) {
                return nil();
            }

            final byte[] outputBytes = rope.getBytesCopy();

            try {
                final boolean modified = multiByteDowncase(encoding, outputBytes, 0, outputBytes.length);

                if (modifiedProfile.profile(modified)) {
                    return string;
                } else {
                    return nil();
                }
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
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
        public DynamicObject eachByte(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            final byte[] bytes = rope(string).getBytes();

            for (int i = 0; i < bytes.length; i++) {
                yield(frame, block, bytes[i] & 0xff);
            }

            return string;
        }

    }

    @CoreMethod(names = "each_char", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(StringGuards.class)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private TaintResultNode taintResultNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "isValidOr7BitEncoding(string)")
        public DynamicObject eachChar(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            byte[] ptrBytes = rope(string).getBytes();
            int len = ptrBytes.length;
            Encoding enc = encoding(string);

            int n;

            for (int i = 0; i < len; i += n) {
                n = StringSupport.encFastMBCLen(ptrBytes, i, len, enc);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        @Specialization(guards = "!isValidOr7BitEncoding(string)")
        public DynamicObject eachCharMultiByteEncoding(VirtualFrame frame, DynamicObject string, DynamicObject block) {
            byte[] ptrBytes = rope(string).getBytes();
            int len = ptrBytes.length;
            Encoding enc = encoding(string);

            int n;

            for (int i = 0; i < len; i += n) {
                n = multiByteStringLength(enc, ptrBytes, i, len);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        @TruffleBoundary
        private int multiByteStringLength(Encoding enc, byte[] bytes, int p, int end) {
            return StringSupport.length(enc, bytes, p, end);
        }

        // TODO (nirvdrum 10-Mar-15): This was extracted from JRuby, but likely will need to become a Rubinius primitive.
        private Object substr(DynamicObject string, int beg, int len) {
            final Rope rope = rope(string);

            int length = rope.byteLength();
            if (len < 0 || beg > length) return nil();

            if (beg < 0) {
                beg += length;
                if (beg < 0) return nil();
            }

            int end = Math.min(length, beg + len);

            final Rope substringRope = RopeOperations.substring(rope, beg, end - beg);

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

        @Child private ToStrNode toStrNode;

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(encodingName)")
        public DynamicObject forceEncodingString(DynamicObject string, DynamicObject encodingName,
                                                 @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile) {
            final DynamicObject encoding = EncodingNodes.getEncoding(encodingName.toString());
            return forceEncodingEncoding(string, encoding, differentEncodingProfile);
        }

        @Specialization(guards = "isRubyEncoding(rubyEncoding)")
        public DynamicObject forceEncodingEncoding(DynamicObject string, DynamicObject rubyEncoding,
                                                   @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile) {
            final Encoding encoding = EncodingOperations.getEncoding(rubyEncoding);

            if (differentEncodingProfile.profile(encoding(string) != encoding)) {
                StringOperations.forceEncoding(string, encoding);
            }

            return string;
        }

        @Specialization(guards = { "!isRubyString(encoding)", "!isRubyEncoding(encoding)" })
        public DynamicObject forceEncoding(VirtualFrame frame, DynamicObject string, Object encoding,
                                           @Cached("createBinaryProfile()") ConditionProfile differentEncodingProfile) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return forceEncodingString(string, toStrNode.executeToStr(frame, encoding), differentEncodingProfile);
        }

    }

    @CoreMethod(names = "getbyte", required = 1, lowerFixnumParameters = 0)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getByte(DynamicObject string, int index,
                              @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                              @Cached("createBinaryProfile()") ConditionProfile indexOutOfBoundsProfile) {
            final byte[] bytes = rope(string).getBytes();

            if (negativeIndexProfile.profile(index < 0)) {
                index += bytes.length;
            }

            if (indexOutOfBoundsProfile.profile((index < 0) || (index >= bytes.length))) {
                return nil();
            }

            return bytes[index] & 0xff;
        }
    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject string) {
            return StringOperations.getByteListReadOnly(string).hashCode();
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
                        getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(self)).getName(), this));
            }

            Layouts.STRING.setRope(self, rope(from));

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

        @Specialization(guards = "isRubyString(from)")
        public Object initializeCopy(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }

            Layouts.STRING.setRope(self, rope(from));

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

        @Child private CallDispatchHeadNode concatNode;
        @Child private SizeNode sizeNode;
        @Child private TaintResultNode taintResultNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            concatNode = DispatchHeadNodeFactory.createMethodCall(context);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] {});
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeGen.create(getContext(), getSourceSection(), index);
        }

        @CreateCast("otherString") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = "isRubyString(otherString)")
        public Object insert(VirtualFrame frame, DynamicObject string, int index, DynamicObject otherString) {
            if (index == -1) {
                return concatNode.call(frame, string, "<<", null, otherString);

            } else if (index < 0) {
                // Incrementing first seems weird, but MRI does it and it's significant because it uses the modified
                // index value in its error messages.  This seems wrong, but we should be compatible.
                index++;
            }

            final int stringLength = sizeNode.executeInteger(frame, string);
            StringNodesHelper.replaceInternal(string, StringNodesHelper.checkIndex(stringLength, index, this), 0, otherString);

            return taintResultNode.maybeTaint(otherString, string);
        }
    }

    @CoreMethod(names = "lstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class LstripBangNode extends CoreMethodArrayArgumentsNode {

        public LstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object lstripBangSingleByte(DynamicObject string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#singleByteLStrip.

            if (StringOperations.getByteList(string).getRealSize() == 0) {
                return nil();
            }

            final int s = StringOperations.getByteList(string).getBegin();
            final int end = s + StringOperations.getByteList(string).getRealSize();
            final byte[]bytes = StringOperations.getByteList(string).getUnsafeBytes();

            int p = s;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;
            if (p > s) {
                StringOperations.getByteList(string).view(p - s, end - p);
                StringOperations.keepCodeRange(string);

                return string;
            }

            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object lstripBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#multiByteLStrip.

            if (StringOperations.getByteList(string).getRealSize() == 0) {
                return nil();
            }

            final Encoding enc = EncodingUtils.STR_ENC_GET(StringOperations.getCodeRangeable(string));
            final int s = StringOperations.getByteList(string).getBegin();
            final int end = s + StringOperations.getByteList(string).getRealSize();
            final byte[]bytes = StringOperations.getByteList(string).getUnsafeBytes();

            int p = s;

            while (p < end) {
                int c = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, p, end);
                if (!ASCIIEncoding.INSTANCE.isSpace(c)) break;
                p += StringSupport.codeLength(enc, c);
            }

            if (p > s) {
                StringOperations.getByteList(string).view(p - s, end - p);
                StringOperations.keepCodeRange(string);

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

        public SetNumBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject setNumBytes(DynamicObject string, int count) {
            Layouts.STRING.setRope(string, RopeOperations.substring(rope(string), 0, count));

            return string;
        }
    }

    @CoreMethod(names = "ord")
    public abstract static class OrdNode extends CoreMethodArrayArgumentsNode {

        public OrdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int ord(DynamicObject string) {
            final StringCodeRangeableWrapper codeRangeable = StringOperations.getCodeRangeableReadOnly(string);
            final ByteList bytes = codeRangeable.getByteList();

            try {
                return codePoint(EncodingUtils.STR_ENC_GET(codeRangeable), bytes.getUnsafeBytes(), bytes.begin(), bytes.begin() + bytes.realSize());
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
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

        @Specialization(guards = "isRubyString(other)")
        public DynamicObject replace(DynamicObject string, DynamicObject other) {
            if (string == other) {
                return string;
            }

            Layouts.STRING.setRope(string, rope(other));

            return string;
        }

    }

    @CoreMethod(names = "rstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class RstripBangNode extends CoreMethodArrayArgumentsNode {

        public RstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
                Layouts.STRING.setRope(string, RopeOperations.substring(rope, 0, endp - start + 1));

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
                int point = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, prev, end);
                if (point != 0 && !ASCIIEncoding.INSTANCE.isSpace(point)) break;
                endp = prev;
            }

            if (endp < end) {
                Layouts.STRING.setRope(string, RopeOperations.substring(rope, 0, endp - start));

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

        public SwapcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject swapcaseSingleByte(DynamicObject string,
                                                @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#swapcase_bang19.

            final ByteList value = StringOperations.getByteList(string);
            final Encoding enc = value.getEncoding();

            if (enc.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (value.getRealSize() == 0) {
                return nil();
            }

            StringOperations.modifyAndKeepCodeRange(string);

            final int s = value.getBegin();
            final int end = s + value.getRealSize();
            final byte[]bytes = value.getUnsafeBytes();

            if (singleByteOptimizableProfile.profile(StringSupport.isSingleByteOptimizable(StringOperations.getCodeRangeable(string), enc))) {
                if (StringSupport.singleByteSwapcase(bytes, s, end)) {
                    return string;
                }
            } else {
                if (StringSupport.multiByteSwapcase(getContext().getRuntime(), enc, bytes, s, end)) {
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

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, StringSupport.CR_7BIT), null);

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

            final DynamicObject result = allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(outputBytes, StringSupport.CR_UNKNOWN), null);
            StringOperations.getByteList(result).setEncoding(ASCIIEncoding.INSTANCE);
            StringOperations.setCodeRange(result, StringSupport.CR_7BIT);

            return result;
        }

        @TruffleBoundary
        private ByteList dumpCommon(DynamicObject string) {
            assert RubyGuards.isRubyString(string);
            return StringSupport.dumpCommon(getContext().getRuntime(), StringOperations.getByteList(string));
        }
    }

    @CoreMethod(names = "setbyte", required = 2, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "index"),
        @NodeChild(type = RubyNode.class, value = "value")
    })
    public abstract static class SetByteNode extends CoreMethodNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

            StringOperations.modify(string);
            StringOperations.clearCodeRange(string);
            StringOperations.getByteList(string).getUnsafeBytes()[normalizedIndex] = (byte) value;

            return value;
        }
    }

    @CoreMethod(names = {"size", "length"})
    @ImportStatic(StringGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract int executeInteger(VirtualFrame frame, DynamicObject string);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int sizeSingleByteRope(DynamicObject string) {
            return rope(string).characterLength();
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int size(DynamicObject string) {
            // TODO (nirvdrum Jan.-07-2016) This should be made much more efficient.
            return StringSupport.strLengthFromRubyString(StringOperations.getCodeRangeableReadOnly(string));
        }

    }

    @CoreMethod(names = "squeeze!", rest = true, raiseIfFrozenSelf = true)
    public abstract static class SqueezeBangNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public SqueezeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "zeroArgs(args)")
        public Object squeezeBangZeroArgs(DynamicObject string, Object[] args,
                                          @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            if (rope(string).isEmpty()) {
                return nil();
            }

            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
            for (int i = 0; i < StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

            StringOperations.modifyAndKeepCodeRange(string);

            if (singleByteOptimizableProfile.profile(StringOperations.singleByteOptimizable(string))) {
                if (! StringSupport.singleByteSqueeze(StringOperations.getByteList(string), squeeze)) {
                    return nil();
                }
            } else {
                if (! squeezeCommonMultiByte(StringOperations.getByteList(string), squeeze, null, encoding(string), false)) {
                    return nil();
                }
            }

            return string;
        }

        @Specialization(guards = "!zeroArgs(args)")
        public Object squeezeBang(VirtualFrame frame, DynamicObject string, Object[] args,
                                  @Cached("createBinaryProfile()") ConditionProfile singleByteOptimizableProfile) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            if (rope(string).isEmpty()) {
                return nil();
            }

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

            DynamicObject otherStr = otherStrings[0];
            Encoding enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(otherStr), this);
            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(StringOperations.getByteList(otherStr), getContext().getRuntime(), squeeze, null, true, enc);

            boolean singlebyte = StringOperations.singleByteOptimizable(string) && StringOperations.singleByteOptimizable(otherStr);

            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];
                enc = StringOperations.checkEncoding(getContext(), string, StringOperations.getCodeRangeable(otherStr), this);
                singlebyte = singlebyte && StringOperations.singleByteOptimizable(otherStr);
                tables = StringSupport.trSetupTable(StringOperations.getByteList(otherStr), getContext().getRuntime(), squeeze, tables, false, enc);
            }

            StringOperations.modifyAndKeepCodeRange(string);

            if (singleByteOptimizableProfile.profile(singlebyte)) {
                if (! StringSupport.singleByteSqueeze(StringOperations.getByteList(string), squeeze)) {
                    return nil();
                }
            } else {
                if (! StringSupport.multiByteSqueeze(getContext().getRuntime(), StringOperations.getByteList(string), squeeze, tables, enc, true)) {
                    return nil();
                }
            }

            return string;
        }

        @TruffleBoundary
        private boolean squeezeCommonMultiByte(ByteList value, boolean squeeze[], StringSupport.TrTables tables, Encoding enc, boolean isArg) {
            return StringSupport.multiByteSqueeze(getContext().getRuntime(), value, squeeze, tables, enc, isArg);
        }

        public static boolean zeroArgs(Object[] args) {
            return args.length == 0;
        }
    }

    @CoreMethod(names = "succ", taintFromSelf = true)
    public abstract static class SuccNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateObjectNode;

        public SuccNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject succ(DynamicObject string) {
            final Rope rope = rope(string);

            if (rope.isEmpty()) {
                return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), RopeOperations.template(EMPTY_UTF8_ROPE, rope.getEncoding()), null);
            } else {
                final ByteList succByteList = StringSupport.succCommon(getContext().getRuntime(), StringOperations.getByteListReadOnly(string));

                return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(string), StringOperations.ropeFromByteList(succByteList, rope.getCodeRange()), null);
            }
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
                final ByteList succByteList = StringSupport.succCommon(getContext().getRuntime(), StringOperations.getByteListReadOnly(string));

                Layouts.STRING.setRope(string, StringOperations.ropeFromByteList(succByteList, rope.getCodeRange()));
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

            final byte[] bytes = StringOperations.getByteList(string).getUnsafeBytes();
            int p = StringOperations.getByteList(string).getBegin();
            final int len = StringOperations.getByteList(string).getRealSize();
            final int end = p + len;

            if (bits >= 8 * 8) { // long size * bits in byte
                Object sum = 0;
                while (p < end) {
                    //modifyCheck(bytes, len);
                    sum = addNode.call(frame, sum, "+", null, bytes[p++] & 0xff);
                }
                if (bits != 0) {
                    final Object mod = shiftNode.call(frame, 1, "<<", null, bits);
                    sum =  andNode.call(frame, sum, "&", null, subNode.call(frame, mod, "-", null, 1));
                }
                return sum;
            } else {
                long sum = 0;
                while (p < end) {
                    //modifyCheck(bytes, len);
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
        public Object sum(VirtualFrame frame, DynamicObject string, Object bits) {
            return ruby(frame, "sum Rubinius::Type.coerce_to(bits, Fixnum, :to_int)", "bits", bits);
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
        public Object toSOnSubclass(VirtualFrame frame, DynamicObject string) {
            return ruby(frame, "''.replace(self)", "self", string);
        }

        public boolean isStringSubclass(DynamicObject string) {
            return Layouts.BASIC_OBJECT.getLogicalClass(string) != getContext().getCoreLibrary().getStringClass();
        }

    }

    @CoreMethod(names = {"to_sym", "intern"})
    public abstract static class ToSymNode extends CoreMethodArrayArgumentsNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toSym(DynamicObject string) {
            return getSymbol(StringOperations.getByteListReadOnly(string));
        }
    }

    @CoreMethod(names = "reverse!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class ReverseBangNode extends CoreMethodArrayArgumentsNode {

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

            Layouts.STRING.setRope(string, RopeOperations.create(reversedBytes, rope.getEncoding(), rope.getCodeRange()));

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
            boolean single = true;

            while (p < end) {
                int cl = StringSupport.length(enc, originalBytes, p, end);
                if (cl > 1 || (originalBytes[p] & 0x80) != 0) {
                    single = false;
                    op -= cl;
                    System.arraycopy(originalBytes, p, reversedBytes, op, cl);
                    p += cl;
                } else {
                    reversedBytes[--op] = originalBytes[p++];
                }
            }

            // TODO (nirvdrum 09-Jan-16): If we guarantee no strings can have an unknown code range, this check can be removed.
            int codeRange = rope.getCodeRange();
            if (codeRange == StringSupport.CR_UNKNOWN) {
                codeRange = single ? StringSupport.CR_7BIT : StringSupport.CR_VALID;
            }

            Layouts.STRING.setRope(string, RopeOperations.create(reversedBytes, rope.getEncoding(), codeRange));

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

        @Specialization(guards = {"isRubyString(fromStr)", "isRubyString(toStr)"})
        public Object trBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (StringOperations.getByteList(self).getRealSize() == 0) {
                return nil();
            }

            if (StringOperations.getByteList(toStr).getRealSize() == 0) {
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

        @Specialization(guards = {"isRubyString(fromStr)", "isRubyString(toStr)"})
        public Object trSBang(VirtualFrame frame, DynamicObject self, DynamicObject fromStr, DynamicObject toStr) {
            if (StringOperations.getByteList(self).getRealSize() == 0) {
                return nil();
            }

            if (StringOperations.getByteList(toStr).getRealSize() == 0) {
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

        public UnpackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(format)", "byteListsEqual(format, cachedFormat)"}, limit = "getCacheLimit()")
        public DynamicObject unpackCached(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject format,
                @Cached("privatizeByteList(format)") ByteList cachedFormat,
                @Cached("create(compileFormat(format))") DirectCallNode callUnpackNode) {
            final ByteList bytes = StringOperations.getByteListReadOnly(string);

            final PackResult result;

            try {
                // TODO CS 20-Dec-15 bytes() creates a copy as the nodes aren't ready for a start offset yet
                result = (PackResult) callUnpackNode.call(frame, new Object[]{bytes.bytes(), bytes.length()});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishUnpack(cachedFormat, result);
        }

        @Specialization(contains = "unpackCached", guards = "isRubyString(format)")
        public DynamicObject unpackUncached(
                VirtualFrame frame,
                DynamicObject string,
                DynamicObject format,
                @Cached("create()") IndirectCallNode callUnpackNode) {
            final ByteList bytes = StringOperations.getByteListReadOnly(string);

            final PackResult result;

            try {
                // TODO CS 20-Dec-15 bytes() creates a copy as the nodes aren't ready for a start offset yet
                result = (PackResult) callUnpackNode.call(frame, compileFormat(format), new Object[]{bytes.bytes(), bytes.length()});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishUnpack(StringOperations.getByteListReadOnly(format), result);
        }

        private RuntimeException handleException(PackException exception) {
            try {
                throw exception;
            } catch (FormatException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
            } catch (TooFewArgumentsException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("too few arguments", this));
            } catch (NoImplicitConversionException e) {
                return new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(e.getObject(), e.getTarget(), this));
            } catch (OutsideOfStringException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("X outside of string", this));
            } catch (CantCompressNegativeException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("can't compress negative numbers", this));
            } catch (RangeException e) {
                return new RaiseException(getContext().getCoreLibrary().rangeError(e.getMessage(), this));
            } catch (CantConvertException e) {
                return new RaiseException(getContext().getCoreLibrary().typeError(e.getMessage(), this));
            }
        }

        private DynamicObject finishUnpack(ByteList format, PackResult result) {
            final DynamicObject array = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), result.getOutput(), result.getOutputLength());

            if (format.length() == 0) {
                //StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
            } else {
                switch (result.getEncoding()) {
                    case DEFAULT:
                    case ASCII_8BIT:
                        break;
                    case US_ASCII:
                        //StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
                        break;
                    case UTF_8:
                        //StringOperations.forceEncoding(string, UTF8Encoding.INSTANCE);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
                }

                taintNode.executeTaint(array);
            }

            return array;
        }

        @Specialization
        public Object unpack(VirtualFrame frame, DynamicObject array, boolean format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object unpack(VirtualFrame frame, DynamicObject array, int format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object unpack(VirtualFrame frame, DynamicObject array, long format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = "isNil(format)")
        public Object unpackNil(VirtualFrame frame, DynamicObject array, Object format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = {"!isRubyString(format)", "!isBoolean(format)", "!isInteger(format)", "!isLong(format)", "!isNil(format)"})
        public Object unpack(VirtualFrame frame, DynamicObject array, Object format) {
            return ruby(frame, "unpack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            assert RubyGuards.isRubyString(format);
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

        public UpcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public DynamicObject upcaseSingleByte(DynamicObject string) {
            final Rope rope = rope(string);
            final ByteList bytes = rope.toByteListCopy();

            if (rope.isEmpty()) {
                return nil();
            }

            final boolean modified = singleByteUpcase(bytes.unsafeBytes(), bytes.begin(), bytes.realSize());
            if (modified) {
                Layouts.STRING.setRope(string, StringOperations.ropeFromByteList(bytes, rope.getCodeRange()));

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
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", encoding), this));
            }

            if (rope.isEmpty()) {
                return nil();
            }

            final ByteList bytes = rope.toByteListCopy();

            try {
                final boolean modified = multiByteUpcase(encoding, bytes.unsafeBytes(), bytes.begin(), bytes.realSize());
                if (modified) {
                    Layouts.STRING.setRope(string, StringOperations.ropeFromByteList(bytes, rope.getCodeRange()));

                    return string;
                } else {
                    return nil();
                }
            } catch (IllegalArgumentException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
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
    public abstract static class ValidEncodingQueryNode extends CoreMethodArrayArgumentsNode {

        public ValidEncodingQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean validEncodingQuery(DynamicObject string) {
            return StringOperations.scanForCodeRange(string) != StringSupport.CR_BROKEN;
        }

    }

    @CoreMethod(names = "capitalize!", raiseIfFrozenSelf = true)
    public abstract static class CapitalizeBangNode extends CoreMethodArrayArgumentsNode {

        public CapitalizeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public DynamicObject capitalizeBang(DynamicObject string) {
            // Taken from org.jruby.RubyString#capitalize_bang19.

            final ByteList value = StringOperations.getByteList(string);
            final Encoding enc = value.getEncoding();

            if (enc.isDummy()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (value.getRealSize() == 0) {
                return nil();
            }

            StringOperations.modifyAndKeepCodeRange(string);

            int s = value.getBegin();
            int end = s + value.getRealSize();
            byte[]bytes = value.getUnsafeBytes();
            boolean modify = false;

            int c = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, s, end);
            if (enc.isLower(c)) {
                enc.codeToMbc(StringSupport.toUpper(enc, c), bytes, s);
                modify = true;
            }

            s += StringSupport.codeLength(enc, c);
            while (s < end) {
                c = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, s, end);
                if (enc.isUpper(c)) {
                    enc.codeToMbc(StringSupport.toLower(enc, c), bytes, s);
                    modify = true;
                }
                s += StringSupport.codeLength(enc, c);
            }

            return modify ? string : nil();
        }
    }

    @CoreMethod(names = "capitalize", taintFromSelf = true)
    public abstract static class CapitalizeNode extends CoreMethodArrayArgumentsNode {

        @Child CallDispatchHeadNode capitalizeBangNode;
        @Child CallDispatchHeadNode dupNode;

        public CapitalizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            capitalizeBangNode = DispatchHeadNodeFactory.createMethodCall(context);
            dupNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object capitalize(VirtualFrame frame, DynamicObject string) {
            final Object duped = dupNode.call(frame, string, "dup", null);
            capitalizeBangNode.call(frame, duped, "capitalize!", null);

            return duped;
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends CoreMethodArrayArgumentsNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject clear(DynamicObject string) {
            Layouts.STRING.setRope(string, RopeOperations.template(EMPTY_UTF8_ROPE, encoding(string)));

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

            final int length = StringOperations.getByteList(string).getRealSize();

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
        public static void replaceInternal(DynamicObject string, int start, int length, DynamicObject replacement) {
            assert RubyGuards.isRubyString(string);
            assert RubyGuards.isRubyString(replacement);
            StringSupport.replaceInternal19(start, length, StringOperations.getCodeRangeable(string), StringOperations.getCodeRangeable(replacement));
        }

        @TruffleBoundary
        private static Object trTransHelper(RubyContext context, DynamicObject self, DynamicObject fromStr, DynamicObject toStr, boolean sFlag) {
            assert RubyGuards.isRubyString(self);
            assert RubyGuards.isRubyString(fromStr);
            assert RubyGuards.isRubyString(toStr);

            final CodeRangeable ret = StringSupport.trTransHelper(context.getRuntime(), StringOperations.getCodeRangeable(self), StringOperations.getCodeRangeable(fromStr), StringOperations.getCodeRangeable(toStr), sFlag);

            if (ret == null) {
                return context.getCoreLibrary().getNilObject();
            }

            return self;
        }
    }

}
