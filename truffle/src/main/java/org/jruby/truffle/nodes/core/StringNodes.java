/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import jnr.posix.POSIX;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.joni.Matcher;
import org.joni.Option;
import org.jruby.Ruby;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.CmpIntNode;
import org.jruby.truffle.nodes.cast.CmpIntNodeGen;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.IsFrozenNode;
import org.jruby.truffle.nodes.objects.IsFrozenNodeGen;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodesFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.*;
import org.jruby.util.io.EncodingUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

@CoreClass(name = "String")
public abstract class StringNodes {

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "string"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    public abstract static class AddNode extends CoreMethodNode {

        @Child private TaintResultNode taintResultNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization
        public RubyString add(RubyString string, RubyString other) {
            final Encoding enc = string.checkEncoding(other, this);
            final RubyString ret = getContext().makeString(getContext().getCoreLibrary().getStringClass(),
                    StringSupport.addByteLists(string.getByteList(), other.getByteList()));

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            ret.getByteList().setEncoding(enc);
            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }
    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile negativeTimesProfile = ConditionProfile.createBinaryProfile();

        @Child private ToIntNode toIntNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString multiply(RubyString string, int times) {
            if (negativeTimesProfile.profile(times < 0)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }

            final ByteList inputBytes = string.getByteList();
            final ByteList outputBytes = new ByteList(string.getByteList().length() * times);

            for (int n = 0; n < times; n++) {
                outputBytes.append(inputBytes);
            }

            outputBytes.setEncoding(inputBytes.getEncoding());
            final RubyString ret = getContext().makeString(string.getLogicalClass(), outputBytes);
            ret.setCodeRange(string.getCodeRange());

            return ret;
        }

        @Specialization
        public RubyString multiply(RubyString string, RubyBignum times) {
            CompilerDirectives.transferToInterpreter();

            throw new RaiseException(
                    getContext().getCoreLibrary().rangeError("bignum too big to convert into `long'", this));
        }

        @Specialization(guards = { "!isRubyBignum(times)", "!isInteger(times)" })
        public RubyString multiply(VirtualFrame frame, RubyString string, Object times) {
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

        @Specialization
        public boolean equal(RubyString a, RubyString b) {
            return stringEqualNode.stringEqual(a, b);
        }

        @Specialization(guards = "!isRubyString(b)")
        public boolean equal(VirtualFrame frame, RubyString a, Object b) {
            if (respondToNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            if (respondToNode.doesRespondTo(frame, b, getContext().makeString("to_str"), false)) {
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

        @Specialization
        public int compare(RubyString a, RubyString b) {
            // Taken from org.jruby.RubyString#op_cmp

            final int ret = a.getByteList().cmp(b.getByteList());

            if ((ret == 0) && !StringSupport.areComparable(a, b)) {
                return a.getByteList().getEncoding().getIndex() > b.getByteList().getEncoding().getIndex() ? 1 : -1;
            }

            return ret;
        }

        @Specialization(guards = "!isRubyString(b)")
        public Object compare(VirtualFrame frame, RubyString a, Object b) {
            notDesignedForCompilation();

            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            if (respondToToStrNode.doesRespondTo(frame, b, getContext().makeString("to_str"), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
                }

                try {
                    final RubyString coerced = toStrNode.executeRubyString(frame, b);

                    return compare(a, coerced);
                } catch (RaiseException e) {
                    if (e.getRubyException().getLogicalClass() == getContext().getCoreLibrary().getTypeErrorClass()) {
                        return nil();
                    } else {
                        throw e;
                    }
                }
            }

            if (respondToCmpNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToCmpNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            if (respondToCmpNode.doesRespondTo(frame, b, getContext().makeString("<=>"), false)) {
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
    public abstract static class ConcatNode extends CoreMethodNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString concat(RubyString string, int other) {
            if (other < 0) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(charRangeException(other));
            }

            return concatNumeric(string, other);
        }

        @Specialization
        public RubyString concat(RubyString string, long other) {
            if (other < 0) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(charRangeException(other));
            }

            return concatNumeric(string, (int) other);
        }

        @Specialization
        public RubyString concat(RubyString string, RubyBignum other) {
            if (other.bigIntegerValue().signum() < 0) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().rangeError("bignum out of char range", this));
            }

            return concatNumeric(string, other.bigIntegerValue().intValue());
        }

        @TruffleBoundary
        @Specialization
        public RubyString concat(RubyString string, RubyString other) {
            final int codeRange = other.getCodeRange();
            final int[] ptr_cr_ret = { codeRange };

            try {
                EncodingUtils.encCrStrBufCat(getContext().getRuntime(), string, other.getByteList(), other.getByteList().getEncoding(), codeRange, ptr_cr_ret);
            } catch (org.jruby.exceptions.RaiseException e) {
                if (e.getException().getMetaClass() == getContext().getRuntime().getEncodingCompatibilityError()) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().encodingCompatibilityError(e.getException().message.asJavaString(), this));
                }

                throw e;
            }

            other.setCodeRange(ptr_cr_ret[0]);

            return string;
        }

        @Specialization(guards = {"!isInteger(other)", "!isLong(other)", "!isRubyBignum(other)", "!isRubyString(other)"})
        public Object concat(VirtualFrame frame, RubyString string, Object other) {
            notDesignedForCompilation();
            return ruby(frame, "concat StringValue(other)", "other", other);
        }

        @TruffleBoundary
        private RubyString concatNumeric(RubyString string, int c) {
            // Taken from org.jruby.RubyString#concatNumeric

            final ByteList value = string.getByteList();
            Encoding enc = value.getEncoding();
            int cl;

            try {
                cl = StringSupport.codeLength(enc, c);
                string.modify(value.getRealSize() + cl);
                string.clearCodeRange();

                if (enc == USASCIIEncoding.INSTANCE) {
                    if (c > 0xff) {
                        throw new RaiseException(charRangeException(c));

                    }
                    if (c > 0x79) {
                        value.setEncoding(ASCIIEncoding.INSTANCE);
                        enc = value.getEncoding();
                    }
                }

                enc.codeToMbc(c, value.getUnsafeBytes(), value.getBegin() + value.getRealSize());
            } catch (EncodingException e) {
                throw new RaiseException(charRangeException(c));
            }

            value.setRealSize(value.getRealSize() + cl);

            return string;
        }

        private RubyException charRangeException(Number value) {
            return getContext().getCoreLibrary().rangeError(
                    String.format("%d out of char range", value), this);
        }
    }

    @CoreMethod(names = {"[]", "slice"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1}, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private SizeNode sizeNode;
        @Child private StringPrimitiveNodes.StringSubstringPrimitiveNode substringNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getIndex(VirtualFrame frame, RubyString string, int index, UndefinedPlaceholder undefined) {
            int normalizedIndex = string.normalizeIndex(index);
            final ByteList bytes = string.getByteList();

            if (normalizedIndex < 0 || normalizedIndex >= bytes.length()) {
                outOfBounds.enter();
                return nil();
            } else {
                return getSubstringNode().execute(frame, string, index, 1);
            }
        }

        @Specialization(guards = { "!isRubyRange(index)", "!isRubyRegexp(index)", "!isRubyString(index)" })
        public Object getIndex(VirtualFrame frame, RubyString string, Object index, UndefinedPlaceholder undefined) {
            return getIndex(frame, string, getToIntNode().doInt(frame, index), undefined);
        }

        @Specialization
        public Object sliceIntegerRange(VirtualFrame frame, RubyString string, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder undefined) {
            return sliceRange(frame, string, range.getBegin(), range.getEnd(), range.doesExcludeEnd());
        }

        @Specialization
        public Object sliceLongRange(VirtualFrame frame, RubyString string, RubyRange.LongFixnumRange range, UndefinedPlaceholder undefined) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values should be properly lowered, only if possible.
            return sliceRange(frame, string, (int) range.getBegin(), (int) range.getEnd(), range.doesExcludeEnd());
        }

        @Specialization
        public Object sliceObjectRange(VirtualFrame frame, RubyString string, RubyRange.ObjectRange range, UndefinedPlaceholder undefined) {
            // TODO (nirvdrum 31-Mar-15) The begin and end values may return Fixnums beyond int boundaries and we should handle that -- Bignums are always errors.
            final int coercedBegin = getToIntNode().doInt(frame, range.getBegin());
            final int coercedEnd = getToIntNode().doInt(frame, range.getEnd());

            return sliceRange(frame, string, coercedBegin, coercedEnd, range.doesExcludeEnd());
        }

        private Object sliceRange(VirtualFrame frame, RubyString string, int begin, int end, boolean doesExcludeEnd) {
            if (sizeNode == null) {
                CompilerDirectives.transferToInterpreter();
                sizeNode = insert(StringNodesFactory.SizeNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null}));
            }

            final int stringLength = sizeNode.executeInteger(frame, string);
            begin = string.normalizeIndex(stringLength, begin);

            if (begin < 0 || begin > stringLength) {
                outOfBounds.enter();
                return nil();
            } else {

                if (begin == stringLength) {
                    return getContext().makeString(string.getLogicalClass(), "", string.getByteList().getEncoding());
                }

                end = string.normalizeIndex(stringLength, end);
                int length = string.clampExclusiveIndex(doesExcludeEnd ? end : end + 1);

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
        public Object slice(VirtualFrame frame, RubyString string, int start, int length) {
            return getSubstringNode().execute(frame, string, start, length);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(length)")
        public Object slice(VirtualFrame frame, RubyString string, int start, Object length) {
            return slice(frame, string, start, getToIntNode().doInt(frame, length));
        }

        @Specialization(guards = { "!isRubyRange(start)", "!isRubyRegexp(start)", "!isRubyString(start)", "!isUndefinedPlaceholder(length)" })
        public Object slice(VirtualFrame frame, RubyString string, Object start, Object length) {
            return slice(frame, string, getToIntNode().doInt(frame, start), getToIntNode().doInt(frame, length));
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyString string, RubyRegexp regexp, UndefinedPlaceholder capture) {
            return slice(frame, string, regexp, 0);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(capture)")
        public Object slice(VirtualFrame frame, RubyString string, RubyRegexp regexp, Object capture) {
            // Extracted from Rubinius's definition of String#[].
            return ruby(frame, "match, str = subpattern(index, other); Regexp.last_match = match; str", "index", regexp, "other", capture);
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyString string, RubyString matchStr, UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

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
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodArrayArgumentsNode {

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object match(RubyString string, RubyRegexp regexp) {
            return regexp.matchCommon(string, true, false);
        }
    }

    @CoreMethod(names = "ascii_only?")
    public abstract static class ASCIIOnlyNode extends CoreMethodArrayArgumentsNode {

        public ASCIIOnlyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean asciiOnly(RubyString string) {
            notDesignedForCompilation();

            if (!string.getByteList().getEncoding().isAsciiCompatible()) {
                return false;
            }

            for (byte b : string.getByteList().unsafeBytes()) {
                if ((b & 0x80) != 0) {
                    return false;
                }
            }

            return true;
        }
    }

    @CoreMethod(names = "b", taintFromSelf = true)
    public abstract static class BNode extends CoreMethodArrayArgumentsNode {

        public BNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString b(RubyString string) {
            final ByteList bytes = string.getByteList().dup();
            bytes.setEncoding(ASCIIEncoding.INSTANCE);
            return getContext().makeString(bytes);
        }

    }

    @CoreMethod(names = "bytes")
    public abstract static class BytesNode extends CoreMethodArrayArgumentsNode {

        public BytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray bytes(RubyString string) {
            final byte[] bytes = string.getByteList().bytes();

            final int[] store = new int[bytes.length];

            for (int n = 0; n < store.length; n++) {
                store[n] = ((int) bytes[n]) & 0xFF;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, bytes.length);
        }

    }

    @CoreMethod(names = "bytesize")
    public abstract static class ByteSizeNode extends CoreMethodArrayArgumentsNode {

        public ByteSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int byteSize(RubyString string) {
            return string.getByteList().length();
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

        @Specialization(guards = "bothSingleByteOptimizable(string, other)")
        public Object caseCmpSingleByte(RubyString string, RubyString other) {
            // Taken from org.jruby.RubyString#casecmp19.

            if (StringSupport.areCompatible(string, other) == null) {
                return nil();
            }

            return string.getByteList().caseInsensitiveCmp(other.getByteList());
        }

        @Specialization(guards = "!bothSingleByteOptimizable(string, other)")
        public Object caseCmp(RubyString string, RubyString other) {
            // Taken from org.jruby.RubyString#casecmp19 and

            final Encoding encoding = StringSupport.areCompatible(string, other);

            if (encoding == null) {
                return nil();
            }

            return multiByteCasecmp(encoding, string.getByteList(), other.getByteList());
        }

        @TruffleBoundary
        private int multiByteCasecmp(Encoding enc, ByteList value, ByteList otherValue) {
            return StringSupport.multiByteCasecmp(enc, value, otherValue);
        }

        public static boolean bothSingleByteOptimizable(RubyString string, RubyString other) {
            final boolean stringSingleByteOptimizable = StringSupport.isSingleByteOptimizable(string, string.getByteList().getEncoding());
            final boolean otherSingleByteOptimizable = StringSupport.isSingleByteOptimizable(other, other.getByteList().getEncoding());

            return stringSingleByteOptimizable && otherSingleByteOptimizable;
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
        public Object chopBang(VirtualFrame frame, RubyString string) {
            notDesignedForCompilation();

            if (sizeNode.executeInteger(frame, string) == 0) {
                return nil();
            }

            final int newLength = choppedLength(string);

            string.getByteList().view(0, newLength);

            if (string.getCodeRange() != StringSupport.CR_7BIT) {
                string.clearCodeRange();
            }

            return string;
        }

        @TruffleBoundary
        private int choppedLength(RubyString string) {
            return StringSupport.choppedLength19(string, getContext().getRuntime());
        }
    }

    @CoreMethod(names = "count", argumentsAsArray = true)
    public abstract static class CountNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public CountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public int count(VirtualFrame frame, RubyString string, Object[] otherStrings) {
            notDesignedForCompilation();

            if (string.getByteList().getRealSize() == 0) {
                return 0;
            }

            if (otherStrings.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
            }

            return countSlow(frame, string, otherStrings);
        }

        private int countSlow(VirtualFrame frame, RubyString string, Object[] args) {
            notDesignedForCompilation();

            RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeRubyString(frame, args[i]);
            }

            RubyString otherStr = otherStrings[0];
            Encoding enc = otherStr.getByteList().getEncoding();

            final boolean[]table = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.getByteList(), getContext().getRuntime(), table, null, true, enc);
            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];

                enc = string.checkEncoding(otherStr, this);
                tables = StringSupport.trSetupTable(otherStr.getByteList(), getContext().getRuntime(), table, tables, false, enc);
            }

            return StringSupport.countCommon19(string.getByteList(), getContext().getRuntime(), table, tables, enc);
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

        @Specialization
        public Object crypt(RubyString string, RubyString salt) {
            // Taken from org.jruby.RubyString#crypt.

            final ByteList value = string.getByteList();

            final Encoding ascii8bit = getContext().getRuntime().getEncodingService().getAscii8bitEncoding();
            ByteList otherBL = salt.getByteList().dup();
            final RubyString otherStr = getContext().makeString(otherBL);

            otherStr.modify();
            StringSupport.associateEncoding(otherStr, ascii8bit);

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

            final RubyString result = getContext().makeString(new ByteList(cryptedString, 0, cryptedString.length - 1));
            StringSupport.associateEncoding(result, ascii8bit);

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
        public RubiniusByteArray data(RubyString string) {
            return new RubiniusByteArray(getContext().getCoreLibrary().getByteArrayClass(), string.getByteList());
        }
    }

    @CoreMethod(names = "delete!", argumentsAsArray = true, raiseIfFrozenSelf = true)
    public abstract static class DeleteBangNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStr;

        public DeleteBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public Object deleteBang(VirtualFrame frame, RubyString string, Object... otherStrings) {
            if (string.getByteList().length() == 0) {
                return nil();
            }

            if (otherStrings.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
            }

            return deleteBangSlow(frame, string, otherStrings);
        }

        private Object deleteBangSlow(VirtualFrame frame, RubyString string, Object... args) {
            notDesignedForCompilation();

            RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeRubyString(frame, args[i]);
            }

            RubyString otherString = otherStrings[0];
            Encoding enc = string.checkEncoding(otherString, this);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherString.getByteList(),
                    getContext().getRuntime(),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                enc = string.checkEncoding(otherStrings[i], this);
                tables = StringSupport.trSetupTable(otherStrings[i].getByteList(), getContext().getRuntime(), squeeze, tables, false, enc);
            }

            if (StringSupport.delete_bangCommon19(string, getContext().getRuntime(), squeeze, tables, enc) == null) {
                return nil();
            }

            return string;
        }
    }

    @CoreMethod(names = "downcase", taintFromSelf = true)
    public abstract static class DowncaseNode extends CoreMethodArrayArgumentsNode {

        public DowncaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyString downcase(RubyString string) {
            final ByteList newByteList = StringNodesHelper.downcase(getContext().getRuntime(), string.getByteList());
            return string.getContext().makeString(string.getLogicalClass(), newByteList);
        }
    }

    @CoreMethod(names = "downcase!", raiseIfFrozenSelf = true)
    public abstract static class DowncaseBangNode extends CoreMethodArrayArgumentsNode {

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject downcase(RubyString string) {
            final ByteList newByteList = StringNodesHelper.downcase(getContext().getRuntime(), string.getByteList());

            if (newByteList.equal(string.getByteList())) {
                return nil();
            } else {
                string.set(newByteList);
                return string;
            }
        }
    }

    @CoreMethod(names = "each_byte", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachByteNode extends YieldingCoreMethodNode {

        public EachByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString eachByte(VirtualFrame frame, RubyString string, RubyProc block) {
            final ByteList bytes = string.getByteList();

            for (int i = 0; i < bytes.getRealSize(); i++) {
                yield(frame, block, bytes.get(i) & 0xff);
            }

            return string;
        }

    }

    @CoreMethod(names = "each_char", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(StringGuards.class)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private TaintResultNode taintResultNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isValidOr7BitEncoding(string)")
        public RubyString eachChar(VirtualFrame frame, RubyString string, RubyProc block) {
            ByteList strByteList = string.getByteList();
            byte[] ptrBytes = strByteList.unsafeBytes();
            int ptr = strByteList.begin();
            int len = strByteList.getRealSize();
            Encoding enc = string.getByteList().getEncoding();

            int n;

            for (int i = 0; i < len; i += n) {
                n = StringSupport.encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        @Specialization(guards = "!isValidOr7BitEncoding(string)")
        public RubyString eachCharMultiByteEncoding(VirtualFrame frame, RubyString string, RubyProc block) {
            ByteList strByteList = string.getByteList();
            byte[] ptrBytes = strByteList.unsafeBytes();
            int ptr = strByteList.begin();
            int len = strByteList.getRealSize();
            Encoding enc = string.getByteList().getEncoding();

            int n;

            for (int i = 0; i < len; i += n) {
                n = multiByteStringLength(enc, ptrBytes, ptr + i, ptr + len);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        @TruffleBoundary
        private int multiByteStringLength(Encoding enc, byte[] bytes, int p, int end) {
            return StringSupport.length(enc, bytes, p, end);
        }

        // TODO (nirvdrum 10-Mar-15): This was extracted from JRuby, but likely will need to become a Rubinius primitive.
        private Object substr(RubyString string, int beg, int len) {
            final ByteList bytes = string.getByteList();

            int length = bytes.length();
            if (len < 0 || beg > length) return nil();

            if (beg < 0) {
                beg += length;
                if (beg < 0) return nil();
            }

            int end = Math.min(length, beg + len);

            final ByteList substringBytes = new ByteList(bytes, beg, end - beg);
            substringBytes.setEncoding(bytes.getEncoding());

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection()));
            }

            final RubyString ret = getContext().makeString(string.getLogicalClass(), substringBytes);

            return taintResultNode.maybeTaint(string, ret);
        }
    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodArrayArgumentsNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean empty(RubyString string) {
            return string.getByteList().length() == 0;
        }
    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodArrayArgumentsNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyEncoding encoding(RubyString string) {
            return RubyEncoding.getEncoding(string.getByteList().getEncoding());
        }
    }

    @CoreMethod(names = "force_encoding", required = 1)
    public abstract static class ForceEncodingNode extends CoreMethodArrayArgumentsNode {

        @Child private ToStrNode toStrNode;

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyString forceEncoding(RubyString string, RubyString encodingName) {
            final RubyEncoding encoding = RubyEncoding.getEncoding(encodingName.toString());
            return forceEncoding(string, encoding);
        }

        @Specialization
        public RubyString forceEncoding(RubyString string, RubyEncoding encoding) {
            string.forceEncoding(encoding.getEncoding());
            return string;
        }

        @Specialization(guards = { "!isRubyString(encoding)", "!isRubyEncoding(encoding)" })
        public RubyString forceEncoding(VirtualFrame frame, RubyString string, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return forceEncoding(string, toStrNode.executeRubyString(frame, encoding));
        }

    }

    @CoreMethod(names = "getbyte", required = 1)
    public abstract static class GetByteNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile negativeIndexProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile indexOutOfBoundsProfile = ConditionProfile.createBinaryProfile();

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getByte(RubyString string, int index) {
            final ByteList bytes = string.getByteList();

            if (negativeIndexProfile.profile(index < 0)) {
                index += bytes.getRealSize();
            }

            if (indexOutOfBoundsProfile.profile((index < 0) || (index >= bytes.getRealSize()))) {
                return nil();
            }

            return string.getByteList().get(index) & 0xff;
        }
    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubyString string) {
            return string.getByteList().hashCode();
        }

    }

    @CoreMethod(names = "inspect", taintFromSelf = true)
    public abstract static class InspectNode extends CoreMethodArrayArgumentsNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyString inspect(RubyString string) {
            notDesignedForCompilation();

            final org.jruby.RubyString inspected = (org.jruby.RubyString) org.jruby.RubyString.inspect19(getContext().getRuntime(), string.getByteList());
            return getContext().makeString(inspected.getByteList());
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
        public RubyString initialize(RubyString self, UndefinedPlaceholder from) {
            return self;
        }

        @Specialization
        public RubyString initialize(RubyString self, RubyString from) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
            }

            if (isFrozenNode.executeIsFrozen(self)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        getContext().getCoreLibrary().frozenError(self.getLogicalClass().getName(), this));
            }

            // TODO (nirvdrum 03-Apr-15): Rather than dup every time, we should do CoW on String mutations.
            self.set(from.getByteList().dup());
            self.setCodeRange(from.getCodeRange());

            return self;
        }

        @Specialization(guards = { "!isRubyString(from)", "!isUndefinedPlaceholder(from)" })
        public RubyString initialize(VirtualFrame frame, RubyString self, Object from) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            return initialize(self, toStrNode.executeRubyString(frame, from));
        }
    }

    @CoreMethod(names = "initialize_copy", required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodArrayArgumentsNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initializeCopy(RubyString self, RubyString from) {
            if (self == from) {
                return self;
            }

            self.getByteList().replace(from.getByteList().bytes());
            self.getByteList().setEncoding(from.getByteList().getEncoding());
            self.setCodeRange(from.getCodeRange());

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
        @Child private TaintResultNode taintResultNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            concatNode = DispatchHeadNodeFactory.createMethodCall(context);
            taintResultNode = new TaintResultNode(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeGen.create(getContext(), getSourceSection(), index);
        }

        @CreateCast("otherString") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization
        public Object insert(VirtualFrame frame, RubyString string, int index, RubyString otherString) {
            if (index == -1) {
                return concatNode.call(frame, string, "<<", null, otherString);

            } else if (index < 0) {
                // Incrementing first seems weird, but MRI does it and it's significant because it uses the modified
                // index value in its error messages.  This seems wrong, but we should be compatible.
                index++;
            }

            StringNodesHelper.replaceInternal(string, StringNodesHelper.checkIndex(string, index, this), 0, otherString);

            return taintResultNode.maybeTaint(otherString, string);
        }
    }

    @CoreMethod(names = "lstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class LstripBangNode extends CoreMethodArrayArgumentsNode {

        public LstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object lstripBangSingleByte(RubyString string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#singleByteLStrip.

            if (string.getByteList().getRealSize() == 0) {
                return nil();
            }

            final int s = string.getByteList().getBegin();
            final int end = s + string.getByteList().getRealSize();
            final byte[]bytes = string.getByteList().getUnsafeBytes();

            int p = s;
            while (p < end && ASCIIEncoding.INSTANCE.isSpace(bytes[p] & 0xff)) p++;
            if (p > s) {
                string.getByteList().view(p - s, end - p);
                string.keepCodeRange();

                return string;
            }

            return nil();
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object lstripBang(RubyString string) {
            // Taken from org.jruby.RubyString#lstrip_bang19 and org.jruby.RubyString#multiByteLStrip.

            if (string.getByteList().getRealSize() == 0) {
                return nil();
            }

            final Encoding enc = EncodingUtils.STR_ENC_GET(string);
            final int s = string.getByteList().getBegin();
            final int end = s + string.getByteList().getRealSize();
            final byte[]bytes = string.getByteList().getUnsafeBytes();

            int p = s;

            while (p < end) {
                int c = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, p, end);
                if (!ASCIIEncoding.INSTANCE.isSpace(c)) break;
                p += StringSupport.codeLength(enc, c);
            }

            if (p > s) {
                string.getByteList().view(p - s, end - p);
                string.keepCodeRange();

                return string;
            }

            return nil();
        }
    }

    @CoreMethod(names = "match", required = 1, taintFromSelf = true)
    public abstract static class MatchNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode regexpMatchNode;

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            regexpMatchNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyString regexpString) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getByteList(), Option.DEFAULT);

            return regexpMatchNode.call(frame, regexp, "match", null, string);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyRegexp regexp) {
            return regexpMatchNode.call(frame, regexp, "match", null, string);
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "modify!", raiseIfFrozenSelf = true)
    public abstract static class ModifyBangNode extends CoreMethodArrayArgumentsNode {

        public ModifyBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString modifyBang(RubyString string) {
            string.modify();
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
        public RubyString setNumBytes(RubyString string, int count) {
            string.getByteList().view(0, count);
            return string;
        }
    }

    @CoreMethod(names = "ord")
    public abstract static class OrdNode extends CoreMethodArrayArgumentsNode {

        public OrdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public int ord(RubyString string) {
            return ((org.jruby.RubyFixnum) getContext().toJRuby(string).ord(getContext().getRuntime().getCurrentContext())).getIntValue();
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

        @Specialization
        public RubyString replace(RubyString string, RubyString other) {
            if (string == other) {
                return string;
            }

            string.getByteList().replace(other.getByteList().bytes());
            string.getByteList().setEncoding(other.getByteList().getEncoding());
            string.setCodeRange(other.getCodeRange());

            return string;
        }

    }

    @CoreMethod(names = "rstrip!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class RstripBangNode extends CoreMethodArrayArgumentsNode {

        public RstripBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public Object rstripBangSingleByte(RubyString string) {
            // Taken from org.jruby.RubyString#rstrip_bang19 and org.jruby.RubyString#singleByteRStrip19.

            if (string.getByteList().getRealSize() == 0) {
                return nil();
            }

            final byte[] bytes = string.getByteList().getUnsafeBytes();
            final int start = string.getByteList().getBegin();
            final int end = start + string.getByteList().getRealSize();
            int endp = end - 1;
            while (endp >= start && (bytes[endp] == 0 ||
                    ASCIIEncoding.INSTANCE.isSpace(bytes[endp] & 0xff))) endp--;

            if (endp < end - 1) {
                string.getByteList().view(0, endp - start + 1);
                string.keepCodeRange();

                return string;
            }

            return nil();
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public Object rstripBang(RubyString string) {
            // Taken from org.jruby.RubyString#rstrip_bang19 and org.jruby.RubyString#multiByteRStrip19.

            if (string.getByteList().getRealSize() == 0) {
                return nil();
            }

            final Encoding enc = EncodingUtils.STR_ENC_GET(string);
            final byte[] bytes = string.getByteList().getUnsafeBytes();
            final int start = string.getByteList().getBegin();
            final int end = start + string.getByteList().getRealSize();

            int endp = end;
            int prev;
            while ((prev = prevCharHead(enc, bytes, start, endp, end)) != -1) {
                int point = StringSupport.codePoint(getContext().getRuntime(), enc, bytes, prev, end);
                if (point != 0 && !ASCIIEncoding.INSTANCE.isSpace(point)) break;
                endp = prev;
            }

            if (endp < end) {
                string.getByteList().view(0, endp - start);
                string.keepCodeRange();

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

        private final ConditionProfile dummyEncodingProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile singleByteOptimizableProfile = ConditionProfile.createBinaryProfile();

        public SwapcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject swapcaseSingleByte(RubyString string) {
            // Taken from org.jruby.RubyString#swapcase_bang19.

            final ByteList value = string.getByteList();
            final Encoding enc = value.getEncoding();

            if (dummyEncodingProfile.profile(enc.isDummy())) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (value.getRealSize() == 0) {
                return nil();
            }

            string.modifyAndKeepCodeRange();

            final int s = value.getBegin();
            final int end = s + value.getRealSize();
            final byte[]bytes = value.getUnsafeBytes();

            if (singleByteOptimizableProfile.profile(StringSupport.isSingleByteOptimizable(string, enc))) {
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

    @CoreMethod(names = "strip")
    public abstract static class StripNode extends CoreMethodArrayArgumentsNode {

        public StripNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString strip(RubyString string) {
            notDesignedForCompilation();

            // Hacky implementation to get something working

            return getContext().makeString(string.toString().trim());
        }

    }

    @CoreMethod(names = "dump", taintFromSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class DumpNode extends CoreMethodArrayArgumentsNode {

        public DumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isAsciiCompatible(string)")
        public RubyString dumpAsciiCompatible(RubyString string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);

            final RubyString result = getContext().makeString(string.getLogicalClass(), outputBytes);
            result.getByteList().setEncoding(string.getByteList().getEncoding());
            result.setCodeRange(StringSupport.CR_7BIT);

            return result;
        }

        @Specialization(guards = "!isAsciiCompatible(string)")
        public RubyString dump(RubyString string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);

            try {
                outputBytes.append(".force_encoding(\"".getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedOperationException(e);
            }

            outputBytes.append(string.getByteList().getEncoding().getName());
            outputBytes.append((byte) '"');
            outputBytes.append((byte) ')');

            final RubyString result = getContext().makeString(string.getLogicalClass(), outputBytes);
            result.getByteList().setEncoding(ASCIIEncoding.INSTANCE);
            result.setCodeRange(StringSupport.CR_7BIT);

            return result;
        }

        @TruffleBoundary
        private ByteList dumpCommon(RubyString string) {
            return StringSupport.dumpCommon(getContext().getRuntime(), string.getByteList());
        }
    }

    @CoreMethod(names = "scan", required = 1, needsBlock = true, taintFromParameter = 0)
    public abstract static class ScanNode extends YieldingCoreMethodNode {

        public ScanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyString regexpString, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getByteList(), Option.DEFAULT);
            return scan(string, regexp, block);
        }

        @Specialization
        public RubyString scan(VirtualFrame frame, RubyString string, RubyString regexpString, RubyProc block) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getByteList(), Option.DEFAULT);
            return scan(frame, string, regexp, block);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyRegexp regexp, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), (Object[]) regexp.scan(string));
        }

        @Specialization
        public RubyString scan(VirtualFrame frame, RubyString string, RubyRegexp regexp, RubyProc block) {
            notDesignedForCompilation();

            // TODO (nirvdrum 12-Jan-15) Figure out a way to make this not just a complete copy & paste of RubyRegexp#scan.

            final RubyContext context = getContext();

            final byte[] stringBytes = string.getByteList().bytes();
            final Encoding encoding = string.getByteList().getEncoding();
            final Matcher matcher = regexp.getRegex().matcher(stringBytes);

            int p = string.getByteList().getBegin();
            int end = 0;
            int range = p + string.getByteList().getRealSize();

            Object lastGoodMatchData = nil();

            if (regexp.getRegex().numberOfCaptures() == 0) {
                while (true) {
                    Object matchData = regexp.matchCommon(string, false, true, matcher, p + end, range);

                    if (matchData == context.getCoreLibrary().getNilObject()) {
                        break;
                    }

                    RubyMatchData md = (RubyMatchData) matchData;
                    Object[] values = md.getValues();

                    assert values.length == 1;

                    yield(frame, block, values[0]);

                    lastGoodMatchData = matchData;
                    end = StringSupport.positionEndForScan(string.getByteList(), matcher, encoding, p, range);
                }

                regexp.setThread("$~", lastGoodMatchData);
            } else {
                while (true) {
                    Object matchData = regexp.matchCommon(string, false, true, matcher, p + end, stringBytes.length);

                    if (matchData == context.getCoreLibrary().getNilObject()) {
                        break;
                    }

                    final Object[] captures = ((RubyMatchData) matchData).getCaptures();
                    yield(frame, block, new RubyArray(context.getCoreLibrary().getArrayClass(), captures, captures.length));

                    lastGoodMatchData = matchData;
                    end = StringSupport.positionEndForScan(string.getByteList(), matcher, encoding, p, range);
                }

                regexp.setThread("$~", lastGoodMatchData);
            }

            return string;
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
            return new FixnumLowerNode(ToIntNodeGen.create(getContext(), getSourceSection(), index));
        }

        @CreateCast("value") public RubyNode coerceValueToInt(RubyNode value) {
            return new FixnumLowerNode(ToIntNodeGen.create(getContext(), getSourceSection(), value));
        }

        @Specialization
        public int setByte(RubyString string, int index, int value) {
            final int normalizedIndex = StringNodesHelper.checkIndexForRef(string, index, this);

            string.modify();
            string.clearCodeRange();
            string.getByteList().getUnsafeBytes()[normalizedIndex] = (byte) value;

            return value;
        }
    }

    @CoreMethod(names = {"size", "length"})
    @ImportStatic(StringGuards.class)
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract int executeInteger(VirtualFrame frame, RubyString string);

        @Specialization(guards = "isSingleByteOptimizable(string)")
        public int sizeSingleByte(RubyString string) {
            return string.getByteList().getRealSize();
        }

        @Specialization(guards = "!isSingleByteOptimizable(string)")
        public int size(RubyString string) {
            return StringSupport.strLengthFromRubyString(string);
        }
    }

    @CoreMethod(names = "squeeze!", argumentsAsArray = true, raiseIfFrozenSelf = true)
    public abstract static class SqueezeBangNode extends CoreMethodArrayArgumentsNode {

        private ConditionProfile singleByteOptimizableProfile = ConditionProfile.createBinaryProfile();

        @Child private ToStrNode toStrNode;

        public SqueezeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "zeroArgs(string, args)")
        public Object squeezeBangZeroArgs(VirtualFrame frame, RubyString string, Object... args) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            if (string.getByteList().length() == 0) {
                return nil();
            }

            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE];
            for (int i = 0; i < StringSupport.TRANS_SIZE; i++) squeeze[i] = true;

            string.modifyAndKeepCodeRange();

            if (singleByteOptimizableProfile.profile(string.singleByteOptimizable())) {
                if (! StringSupport.singleByteSqueeze(string.getByteList(), squeeze)) {
                    return nil();
                }
            } else {
                if (! squeezeCommonMultiByte(string.getByteList(), squeeze, null, string.getByteList().getEncoding(), false)) {
                    return nil();
                }
            }

            return string;
        }

        @Specialization(guards = "!zeroArgs(string, args)")
        public Object squeezeBang(VirtualFrame frame, RubyString string, Object... args) {
            // Taken from org.jruby.RubyString#squeeze_bang19.

            if (string.getByteList().length() == 0) {
                return nil();
            }

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toStrNode = insert(ToStrNodeGen.create(getContext(), getSourceSection(), null));
            }

            final RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStrNode.executeRubyString(frame, args[i]);
            }

            RubyString otherStr = otherStrings[0];
            Encoding enc = string.checkEncoding(otherStr, this);
            final boolean squeeze[] = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherStr.getByteList(), getContext().getRuntime(), squeeze, null, true, enc);

            boolean singlebyte = string.singleByteOptimizable() && otherStr.singleByteOptimizable();

            for (int i = 1; i < otherStrings.length; i++) {
                otherStr = otherStrings[i];
                enc = string.checkEncoding(otherStr);
                singlebyte = singlebyte && otherStr.singleByteOptimizable();
                tables = StringSupport.trSetupTable(otherStr.getByteList(), getContext().getRuntime(), squeeze, tables, false, enc);
            }

            string.modifyAndKeepCodeRange();

            if (singleByteOptimizableProfile.profile(singlebyte)) {
                if (! StringSupport.singleByteSqueeze(string.getByteList(), squeeze)) {
                    return nil();
                }
            } else {
                if (! StringSupport.multiByteSqueeze(getContext().getRuntime(), string.getByteList(), squeeze, tables, enc, true)) {
                    return nil();
                }
            }

            return string;
        }

        @TruffleBoundary
        private boolean squeezeCommonMultiByte(ByteList value, boolean squeeze[], StringSupport.TrTables tables, Encoding enc, boolean isArg) {
            return StringSupport.multiByteSqueeze(getContext().getRuntime(), value, squeeze, tables, enc, isArg);
        }

        public static boolean zeroArgs(RubyString string, Object... args) {
            return args.length == 0;
        }
    }

    @CoreMethod(names = "succ", taintFromSelf = true)
    public abstract static class SuccNode extends CoreMethodArrayArgumentsNode {

        public SuccNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString succ(RubyString string) {
            notDesignedForCompilation();

            if (string.length() > 0) {
                return getContext().makeString(string.getLogicalClass(), StringSupport.succCommon(getContext().getRuntime(), string.getByteList()));
            } else {
                return getContext().makeString(string.getLogicalClass(), "");
            }
        }
    }

    @CoreMethod(names = "succ!", raiseIfFrozenSelf = true)
    public abstract static class SuccBangNode extends CoreMethodArrayArgumentsNode {

        public SuccBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyString succBang(RubyString string) {
            notDesignedForCompilation();

            if (string.getByteList().getRealSize() > 0) {
                string.set(StringSupport.succCommon(getContext().getRuntime(), string.getByteList()));
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
        public Object sum(VirtualFrame frame, RubyString string, int bits) {
            return sum(frame, string, (long) bits);
        }

        @Specialization
        public Object sum(VirtualFrame frame, RubyString string, long bits) {
            // Copied from JRuby

            final byte[] bytes = string.getByteList().getUnsafeBytes();
            int p = string.getByteList().getBegin();
            final int len = string.getByteList().getRealSize();
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
        public Object sum(VirtualFrame frame, RubyString string, UndefinedPlaceholder bits) {
            return sum(frame, string, 16);
        }

        @Specialization(guards = {"!isInteger(bits)", "!isLong(bits)", "!isUndefinedPlaceholder(bits)"})
        public Object sum(VirtualFrame frame, RubyString string, Object bits) {
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
        public double toF(RubyString string) {
            try {
                return convertToDouble(string);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        @TruffleBoundary
        private double convertToDouble(RubyString string) {
            return ConvertDouble.byteListToDouble19(string.getByteList(), false);
        }
    }

    @CoreMethod(names = { "to_s", "to_str" })
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "!isStringSubclass(string)")
        public RubyString toS(RubyString string) {
            return string;
        }

        @Specialization(guards = "isStringSubclass(string)")
        public Object toSOnSubclass(VirtualFrame frame, RubyString string) {
            return ruby(frame, "''.replace(self)", "self", string);
        }

        public boolean isStringSubclass(RubyString string) {
            return string.getLogicalClass() != getContext().getCoreLibrary().getStringClass();
        }

    }

    @CoreMethod(names = {"to_sym", "intern"})
    public abstract static class ToSymNode extends CoreMethodArrayArgumentsNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubySymbol toSym(RubyString string) {
            notDesignedForCompilation();

            return getContext().getSymbol(string.getByteList());
        }
    }

    @CoreMethod(names = "reverse!", raiseIfFrozenSelf = true)
    @ImportStatic(StringGuards.class)
    public abstract static class ReverseBangNode extends CoreMethodArrayArgumentsNode {

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "reverseIsEqualToSelf(string)")
        public RubyString reverseNoOp(RubyString string) {
            return string;
        }

        @Specialization(guards = { "!reverseIsEqualToSelf(string)", "isSingleByteOptimizable(string)" })
        public RubyString reverseSingleByteOptimizable(RubyString string) {
            // Taken from org.jruby.RubyString#reverse!

            string.modify();

            final byte[] bytes = string.getByteList().getUnsafeBytes();
            final int p = string.getByteList().getBegin();
            final int len = string.getByteList().getRealSize();

            for (int i = 0; i < len >> 1; i++) {
                byte b = bytes[p + i];
                bytes[p + i] = bytes[p + len - i - 1];
                bytes[p + len - i - 1] = b;
            }

            return string;
        }

        @Specialization(guards = { "!reverseIsEqualToSelf(string)", "!isSingleByteOptimizable(string)" })
        public RubyString reverse(RubyString string) {
            // Taken from org.jruby.RubyString#reverse!

            string.modify();

            final byte[] bytes = string.getByteList().getUnsafeBytes();
            int p = string.getByteList().getBegin();
            final int len = string.getByteList().getRealSize();

            final Encoding enc = string.getByteList().getEncoding();
            final int end = p + len;
            int op = len;
            final byte[] obytes = new byte[len];
            boolean single = true;

            while (p < end) {
                int cl = StringSupport.length(enc, bytes, p, end);
                if (cl > 1 || (bytes[p] & 0x80) != 0) {
                    single = false;
                    op -= cl;
                    System.arraycopy(bytes, p, obytes, op, cl);
                    p += cl;
                } else {
                    obytes[--op] = bytes[p++];
                }
            }

            string.getByteList().setUnsafeBytes(obytes);
            if (string.getCodeRange() == StringSupport.CR_UNKNOWN) {
                string.setCodeRange(single ? StringSupport.CR_7BIT : StringSupport.CR_VALID);
            }

            return string;
        }

        public static boolean reverseIsEqualToSelf(RubyString string) {
            return string.getByteList().getRealSize() <= 1;
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

        @Specialization
        public Object trBang(VirtualFrame frame, RubyString self, RubyString fromStr, RubyString toStr) {
            if (self.getByteList().getRealSize() == 0) {
                return nil();
            }

            if (toStr.getByteList().getRealSize() == 0) {
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

        @Specialization
        public Object trSBang(VirtualFrame frame, RubyString self, RubyString fromStr, RubyString toStr) {
            if (self.getByteList().getRealSize() == 0) {
                return nil();
            }

            if (toStr.getByteList().getRealSize() == 0) {
                if (deleteBangNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    deleteBangNode = insert(StringNodesFactory.DeleteBangNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] {}));
                }

                return deleteBangNode.deleteBang(frame, self, fromStr);
            }

            return StringNodesHelper.trTransHelper(getContext(), self, fromStr, toStr, true);
        }
    }

    @CoreMethod(names = "unpack", required = 1)
    public abstract static class UnpackNode extends ArrayCoreMethodNode {

        public UnpackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray unpack(RubyString string, RubyString format) {
            final org.jruby.RubyArray jrubyArray = Pack.unpack(getContext().getRuntime(), string.getByteList(), format.getByteList());
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), jrubyArray.toArray());
        }

    }

    @CoreMethod(names = "upcase", taintFromSelf = true)
    public abstract static class UpcaseNode extends CoreMethodArrayArgumentsNode {

        public UpcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyString upcase(RubyString string) {
            final ByteList byteListString = StringNodesHelper.upcase(getContext().getRuntime(), string.getByteList());
            return string.getContext().makeString(string.getLogicalClass(), byteListString);
        }

    }

    @CoreMethod(names = "upcase!", raiseIfFrozenSelf = true)
    public abstract static class UpcaseBangNode extends CoreMethodArrayArgumentsNode {

        public UpcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyBasicObject upcaseBang(RubyString string) {
            final ByteList byteListString = StringNodesHelper.upcase(getContext().getRuntime(), string.getByteList());

            if (byteListString.equal(string.getByteList())) {
                return nil();
            } else {
                string.set(byteListString);
                return string;
            }
        }
    }

    @CoreMethod(names = "valid_encoding?")
    public abstract static class ValidEncodingQueryNode extends CoreMethodArrayArgumentsNode {

        public ValidEncodingQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean validEncodingQuery(RubyString string) {
            return string.scanForCodeRange() != StringSupport.CR_BROKEN;
        }

    }

    @CoreMethod(names = "capitalize!", raiseIfFrozenSelf = true)
    public abstract static class CapitalizeBangNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile dummyEncodingProfile = ConditionProfile.createBinaryProfile();

        public CapitalizeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        @TruffleBoundary
        public RubyBasicObject capitalizeBang(RubyString string) {
            // Taken from org.jruby.RubyString#capitalize_bang19.

            final ByteList value = string.getByteList();
            final Encoding enc = value.getEncoding();

            if (dummyEncodingProfile.profile(enc.isDummy())) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().encodingCompatibilityError(
                                String.format("incompatible encoding with this operation: %s", enc), this));
            }

            if (value.getRealSize() == 0) {
                return nil();
            }

            string.modifyAndKeepCodeRange();

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
        public Object capitalize(VirtualFrame frame, RubyString string) {
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
        public RubyString clear(RubyString string) {
            notDesignedForCompilation();
            ByteList empty = ByteList.EMPTY_BYTELIST;
            empty.setEncoding(string.getByteList().getEncoding());

            string.set(empty);
            return string;
        }
    }

    public static class StringNodesHelper {

        @TruffleBoundary
        public static ByteList upcase(Ruby runtime, ByteList string) {
            return runtime.newString(string).upcase(runtime.getCurrentContext()).getByteList();
        }

        @TruffleBoundary
        public static ByteList downcase(Ruby runtime, ByteList string) {
            return runtime.newString(string).downcase(runtime.getCurrentContext()).getByteList();
        }

        public static int checkIndex(RubyString string, int index, RubyNode node) {
            if (index > string.length()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
            }

            if (index < 0) {
                if (-index > string.length()) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(
                            node.getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), node));
                }

                index += string.length();
            }

            return index;
        }

        public static int checkIndexForRef(RubyString string, int index, RubyNode node) {
            final int length = string.getByteList().getRealSize();

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
        public static void replaceInternal(RubyString string, int start, int length, RubyString replacement) {
            StringSupport.replaceInternal19(start, length, string, replacement);
        }

        @TruffleBoundary
        private static Object trTransHelper(RubyContext context, RubyString self, RubyString fromStr, RubyString toStr, boolean sFlag) {
            final CodeRangeable ret = StringSupport.trTransHelper(context.getRuntime(), self, fromStr, toStr, sFlag);

            if (ret == null) {
                return context.getCoreLibrary().getNilObject();
            }

            return ret;
        }
    }

}
