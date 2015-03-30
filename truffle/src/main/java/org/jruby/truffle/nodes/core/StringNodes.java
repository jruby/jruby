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
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.jcodings.exception.EncodingException;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.joni.Matcher;
import org.joni.Option;
import org.jruby.Ruby;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.CmpIntNode;
import org.jruby.truffle.nodes.cast.CmpIntNodeFactory;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeFactory;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.IsFrozenNode;
import org.jruby.truffle.nodes.objects.IsFrozenNodeFactory;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodesFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.Pack;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;

@CoreClass(name = "String")
public abstract class StringNodes {

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(value = "string"),
        @NodeChild(value = "other")
    })
    public abstract static class AddNode extends RubyNode {

        @Child private TaintResultNode taintResultNode;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
            taintResultNode = prev.taintResultNode;
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), other);
        }

        @Specialization
        public RubyString add(RubyString string, RubyString other) {
            final Encoding enc = string.checkEncoding(other, this);
            final RubyString ret = getContext().makeString(getContext().getCoreLibrary().getStringClass(),
                    StringSupport.addByteLists(string.getByteList(), other.getByteList()));

            if (taintResultNode == null) {
                CompilerDirectives.transferToInterpreter();
                taintResultNode = insert(new TaintResultNode(getContext(), getSourceSection(), false, new int[]{}));
            }

            ret.getByteList().setEncoding(enc);
            taintResultNode.maybeTaint(string, ret);
            taintResultNode.maybeTaint(other, ret);

            return ret;
        }
    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends CoreMethodNode {

        private final ConditionProfile negativeTimesProfile = ConditionProfile.createBinaryProfile();

        @Child private ToIntNode toIntNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
            toIntNode = prev.toIntNode;
        }

        @Specialization
        public RubyString multiply(RubyString string, int times) {
            if (negativeTimesProfile.profile(times < 0)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }

            final ByteList inputBytes = string.getBytes();
            final ByteList outputBytes = new ByteList(string.getBytes().length() * times);

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

        @Specialization(guards = { "!isRubyBignum(arguments[1])", "!isInteger(arguments[1])" })
        public RubyString multiply(VirtualFrame frame, RubyString string, Object times) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return multiply(string, toIntNode.executeIntegerFixnum(frame, times));
        }
    }

    @CoreMethod(names = {"==", "===", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        @Child private StringPrimitiveNodes.StringEqualPrimitiveNode stringEqualNode;
        @Child private KernelNodes.RespondToNode respondToNode;
        @Child private CallDispatchHeadNode objectEqualNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            stringEqualNode = StringPrimitiveNodesFactory.StringEqualPrimitiveNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            stringEqualNode = prev.stringEqualNode;
        }

        @Specialization
        public boolean equal(RubyString a, RubyString b) {
            return stringEqualNode.stringEqual(a, b);
        }

        @Specialization(guards = "!isRubyString(arguments[1])")
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
    public abstract static class CompareNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode cmpNode;
        @Child private CmpIntNode cmpIntNode;
        @Child private KernelNodes.RespondToNode respondToCmpNode;
        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToStrNode toStrNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
            cmpNode = prev.cmpNode;
            cmpIntNode = prev.cmpIntNode;
            respondToCmpNode = prev.respondToCmpNode;
            respondToToStrNode = prev.respondToToStrNode;
            toStrNode = prev.toStrNode;
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

        @Specialization(guards = "!isRubyString(arguments[1])")
        public Object compare(VirtualFrame frame, RubyString a, Object b) {
            notDesignedForCompilation();

            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            if (respondToToStrNode.doesRespondTo(frame, b, getContext().makeString("to_str"), false)) {
                if (toStrNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
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
                    cmpIntNode = insert(CmpIntNodeFactory.create(getContext(), getSourceSection(), null, null, null));
                }

                return -(cmpIntNode.executeIntegerFixnum(frame, cmpResult, a, b));
            }

            return nil();
        }
    }

    @CoreMethod(names = { "<<", "concat" }, required = 1, taintFromParameters = 0, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(value = "string"),
            @NodeChild(value = "other")
    })
    public abstract static class ConcatNode extends RubyNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConcatNode(ConcatNode prev) {
            super(prev);
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

    @CoreMethod(names = "%", required = 1, argumentsAsArray = true)
    public abstract static class FormatNode extends CoreMethodNode {

        public FormatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FormatNode(FormatNode prev) {
            super(prev);
        }

        private final BranchProfile singleArrayProfile = BranchProfile.create();
        private final BranchProfile multipleArgumentsProfile = BranchProfile.create();

        @Specialization
        public RubyString format(RubyString format, Object[] args) {
            return formatSlow(format, args);
        }

        @CompilerDirectives.TruffleBoundary
        private RubyString formatSlow(RubyString format, Object[] args) {
            final RubyContext context = getContext();

            if (args.length == 1 && args[0] instanceof RubyArray) {
                singleArrayProfile.enter();
                return context.makeString(StringFormatter.format(getContext(), format.toString(), Arrays.asList(((RubyArray) args[0]).slowToArray())), format.getByteList().getEncoding());
            } else {
                multipleArgumentsProfile.enter();
                return context.makeString(StringFormatter.format(getContext(), format.toString(), Arrays.asList(args)), format.getByteList().getEncoding());
            }
        }
    }

    @CoreMethod(names = {"[]", "slice"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1}, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode getMatchDataIndexNode;
        @Child private CallDispatchHeadNode includeNode;
        @Child private CallDispatchHeadNode matchNode;
        @Child private CallDispatchHeadNode dupNode;
        @Child private StringPrimitiveNodes.StringSubstringPrimitiveNode substringNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
            toIntNode = prev.toIntNode;
            getMatchDataIndexNode = prev.getMatchDataIndexNode;
            includeNode = prev.includeNode;
            matchNode = prev.matchNode;
            dupNode = prev.dupNode;
            substringNode = prev.substringNode;
        }

        public Object getIndex(RubyString string, int index, UndefinedPlaceholder undefined) {
            int normalizedIndex = string.normalizeIndex(index);
            final ByteList bytes = string.getBytes();

            if (normalizedIndex < 0 || normalizedIndex >= bytes.length()) {
                outOfBounds.enter();
                return nil();
            } else {
                return getContext().makeString(string.getLogicalClass(), bytes.charAt(normalizedIndex), string.getByteList().getEncoding());
            }
        }

        @Specialization(guards = { "!isRubyRange(arguments[1])", "!isRubyRegexp(arguments[1])", "!isRubyString(arguments[1])" })
        public Object getIndex(VirtualFrame frame, RubyString string, Object index, UndefinedPlaceholder undefined) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return getIndex(string, toIntNode.executeIntegerFixnum(frame, index), undefined);
        }

        @Specialization
        public Object slice(RubyString string, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            final String javaString = string.toString();
            final int begin = string.normalizeIndex(range.getBegin());

            if (begin < 0 || begin > javaString.length()) {
                outOfBounds.enter();
                return nil();
            } else {
                final int end = string.normalizeIndex(range.getEnd());
                final int excludingEnd = string.clampExclusiveIndex(range.doesExcludeEnd() ? end : end+1);

                if (begin > excludingEnd) {
                    return getContext().makeString("");
                }

                return getContext().makeString(string.getLogicalClass(),
                        javaString.substring(begin, excludingEnd),
                        string.getByteList().getEncoding());
            }
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyString string, int start, int length) {
            if (substringNode == null) {
                CompilerDirectives.transferToInterpreter();

                substringNode = insert(StringPrimitiveNodesFactory.StringSubstringPrimitiveNodeFactory.create(
                        getContext(), getSourceSection(), new RubyNode[] { null, null, null }));
            }

            return substringNode.execute(frame, string, start, length);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(arguments[2])")
        public Object slice(VirtualFrame frame, RubyString string, int start, Object length) {
            notDesignedForCompilation();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return slice(frame, string, start, toIntNode.executeIntegerFixnum(frame, length));
        }

        @Specialization(guards = { "!isRubyRange(arguments[1])", "!isRubyRegexp(arguments[1])", "!isRubyString(arguments[1])", "!isUndefinedPlaceholder(arguments[2])" })
        public Object slice(VirtualFrame frame, RubyString string, Object start, Object length) {
            notDesignedForCompilation();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return slice(frame, string, toIntNode.executeIntegerFixnum(frame, start), toIntNode.executeIntegerFixnum(frame, length));
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyString string, RubyRegexp regexp, UndefinedPlaceholder capture) {
            notDesignedForCompilation();

            return slice(frame, string, regexp, 0);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(arguments[2])")
        public Object slice(VirtualFrame frame, RubyString string, RubyRegexp regexp, Object capture) {
            notDesignedForCompilation();

            if (matchNode == null) {
                CompilerDirectives.transferToInterpreter();
                matchNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object matchData = matchNode.call(frame, regexp, "match", null, string);

            if (matchData == nil()) {
                return matchData;
            }

            if (getMatchDataIndexNode == null) {
                CompilerDirectives.transferToInterpreter();
                getMatchDataIndexNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return getMatchDataIndexNode.call(frame, matchData, "[]", null, capture);
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
    }

    @CoreMethod(names = "[]=", required = 2, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class ElementSetNode extends CoreMethodNode {

        @Child private SizeNode sizeNode;
        @Child private ToStrNode toStrNode;

        public ElementSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
            toStrNode = ToStrNodeFactory.create(context, sourceSection, null);
        }

        public ElementSetNode(ElementSetNode prev) {
            super(prev);
            sizeNode = prev.sizeNode;
            toStrNode = prev.toStrNode;
        }

        @Specialization
        public RubyString elementSet(VirtualFrame frame, RubyString string, int index, Object replacement) {
            final RubyString coerced = toStrNode.executeRubyString(frame, replacement);
            StringNodesHelper.replaceInternal(string, StringNodesHelper.checkIndex(string, index, this), 1, coerced);

            return coerced;
        }

        @Specialization
        public RubyString elementSet(VirtualFrame frame, RubyString string, RubyRange.IntegerFixnumRange range, Object replacement) {
            notDesignedForCompilation();

            int begin = range.getBegin();
            int end = range.getEnd();
            final int stringLength = sizeNode.executeIntegerFixnum(frame, string);

            if (begin < 0) {
                begin += stringLength;

                if (begin < 0) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
                }

            } else if (begin > stringLength) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }

            if (end > stringLength) {
                end = stringLength;
            } else if (end < 0) {
                end += stringLength;
            }

            if (! range.doesExcludeEnd()) {
                end++;
            }

            int length = end - begin;

            if (length < 0) {
                length = 0;
            }

            final RubyString coerced = toStrNode.executeRubyString(frame, replacement);
            StringNodesHelper.replaceInternal(string, StringNodesHelper.checkIndex(string, begin, this), length, coerced);

            return coerced;
        }
    }

    @CoreMethod(names = "=~", required = 1)
    public abstract static class MatchOperatorNode extends CoreMethodNode {

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchOperatorNode(MatchOperatorNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyString string, RubyRegexp regexp) {
            return regexp.matchCommon(string, true, false);
        }
    }

    @CoreMethod(names = "ascii_only?")
    public abstract static class ASCIIOnlyNode extends CoreMethodNode {

        public ASCIIOnlyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ASCIIOnlyNode(ASCIIOnlyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean asciiOnly(RubyString string) {
            notDesignedForCompilation();

            if (!string.getBytes().getEncoding().isAsciiCompatible()) {
                return false;
            }

            for (byte b : string.getBytes().unsafeBytes()) {
                if ((b & 0x80) != 0) {
                    return false;
                }
            }

            return true;
        }
    }

    @CoreMethod(names = "b", taintFromSelf = true)
    public abstract static class BNode extends CoreMethodNode {

        public BNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BNode(BNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString b(RubyString string) {
            final ByteList bytes = string.getBytes().dup();
            bytes.setEncoding(ASCIIEncoding.INSTANCE);
            return getContext().makeString(bytes);
        }

    }

    @CoreMethod(names = "bytes")
    public abstract static class BytesNode extends CoreMethodNode {

        public BytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BytesNode(BytesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray bytes(RubyString string) {
            final byte[] bytes = string.getBytes().bytes();

            final int[] store = new int[bytes.length];

            for (int n = 0; n < store.length; n++) {
                store[n] = ((int) bytes[n]) & 0xFF;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, bytes.length);
        }

    }

    @CoreMethod(names = "bytesize")
    public abstract static class ByteSizeNode extends CoreMethodNode {

        public ByteSizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ByteSizeNode(ByteSizeNode prev) {
            super(prev);
        }

        @Specialization
        public int byteSize(RubyString string) {
            return string.getBytes().length();
        }

    }

    @CoreMethod(names = "chop!", raiseIfFrozenSelf = true)
    public abstract static class ChopBangNode extends CoreMethodNode {

        @Child private SizeNode sizeNode;

        public ChopBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        public ChopBangNode(ChopBangNode prev) {
            super(prev);
            sizeNode = prev.sizeNode;
        }

        @Specialization
        public Object chopBang(VirtualFrame frame, RubyString string) {
            notDesignedForCompilation();

            if (sizeNode.executeIntegerFixnum(frame, string) == 0) {
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
    public abstract static class CountNode extends CoreMethodNode {

        @Child private ToStrNode toStr;

        public CountNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeFactory.create(context, sourceSection, null);
        }

        public CountNode(CountNode prev) {
            super(prev);
            toStr = prev.toStr;
        }

        @Specialization
        public int count(VirtualFrame frame, RubyString string, Object[] otherStrings) {
            notDesignedForCompilation();

            if (otherStrings.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
            }

            return countSlow(frame, string, otherStrings);
        }

        @CompilerDirectives.TruffleBoundary
        private int countSlow(VirtualFrame frame, RubyString string, Object[] args) {
            RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeRubyString(frame, args[i]);
            }

            return string.count(otherStrings);
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "data")
    public abstract static class DataNode extends CoreMethodNode {

        public DataNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DataNode(DataNode prev) {
            super(prev);
        }

        @Specialization
        public RubiniusByteArray data(RubyString string) {
            return new RubiniusByteArray(getContext().getCoreLibrary().getByteArrayClass(), string.getBytes());
        }
    }

    @CoreMethod(names = "delete!", argumentsAsArray = true, raiseIfFrozenSelf = true)
    public abstract static class DeleteBangNode extends CoreMethodNode {

        @Child private ToStrNode toStr;

        public DeleteBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStr = ToStrNodeFactory.create(context, sourceSection, null);
        }

        public DeleteBangNode(DeleteBangNode prev) {
            super(prev);
            toStr = prev.toStr;
        }

        @Specialization
        public Object deleteBang(VirtualFrame frame, RubyString string, Object[] otherStrings) {
            if (string.getBytes().length() == 0) {
                return nil();
            }

            if (otherStrings.length == 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorEmptyVarargs(this));
            }

            return deleteBangSlow(frame, string, otherStrings);
        }

        @CompilerDirectives.TruffleBoundary
        private Object deleteBangSlow(VirtualFrame frame, RubyString string, Object[] args) {
            RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeRubyString(frame, args[i]);
            }

            RubyString otherString = otherStrings[0];
            Encoding enc = string.checkEncoding(otherString, this);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherString.getBytes(),
                    getContext().getRuntime(),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                enc = string.checkEncoding(otherStrings[i], this);
                tables = StringSupport.trSetupTable(otherStrings[i].getBytes(), getContext().getRuntime(), squeeze, tables, false, enc);
            }

            if (StringSupport.delete_bangCommon19(string, getContext().getRuntime(), squeeze, tables, enc) == null) {
                return nil();
            }

            return string;
        }
    }

    @CoreMethod(names = "downcase", taintFromSelf = true)
    public abstract static class DowncaseNode extends CoreMethodNode {

        public DowncaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseNode(DowncaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString downcase(RubyString string) {
            notDesignedForCompilation();
            final ByteList newByteList = StringNodesHelper.downcase(getContext().getRuntime(), string.getByteList());

            return string.getContext().makeString(string.getLogicalClass(), newByteList);
        }
    }

    @CoreMethod(names = "downcase!", raiseIfFrozenSelf = true)
    public abstract static class DowncaseBangNode extends CoreMethodNode {

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseBangNode(DowncaseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject downcase(RubyString string) {
            notDesignedForCompilation();

            final ByteList newByteList = StringNodesHelper.downcase(getContext().getRuntime(), string.getByteList());

            if (newByteList.equal(string.getBytes())) {
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

        public EachByteNode(EachByteNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString eachByte(VirtualFrame frame, RubyString string, RubyProc block) {
            final ByteList bytes = string.getBytes();

            for (int i = 0; i < bytes.getRealSize(); i++) {
                yield(frame, block, bytes.get(i) & 0xff);
            }

            return string;
        }

    }

    @CoreMethod(names = "each_char", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachCharNode(EachCharNode prev) {
            super(prev);
        }

        @Specialization(guards = "isValidOr7BitEncoding")
        public RubyString eachChar(VirtualFrame frame, RubyString string, RubyProc block) {
            ByteList strByteList = string.getByteList();
            byte[] ptrBytes = strByteList.unsafeBytes();
            int ptr = strByteList.begin();
            int len = strByteList.getRealSize();
            Encoding enc = string.getBytes().getEncoding();

            final int stringLength = string.getBytes().length();
            int n;

            for (int i = 0; i < stringLength; i += n) {
                n = StringSupport.encFastMBCLen(ptrBytes, ptr + i, ptr + len, enc);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        @Specialization(guards = "!isValidOr7BitEncoding")
        public RubyString eachCharMultiByteEncoding(VirtualFrame frame, RubyString string, RubyProc block) {
            ByteList strByteList = string.getByteList();
            byte[] ptrBytes = strByteList.unsafeBytes();
            int ptr = strByteList.begin();
            int len = strByteList.getRealSize();
            Encoding enc = string.getBytes().getEncoding();

            final int stringLength = string.getBytes().length();
            int n;

            for (int i = 0; i < stringLength; i += n) {
                n = multiByteStringLength(enc, ptrBytes, ptr + i, ptr + len);

                yield(frame, block, substr(string, i, n));
            }

            return string;
        }

        public static boolean isValidOr7BitEncoding(RubyString string) {
            return string.isCodeRangeValid() || CodeRangeSupport.isCodeRangeAsciiOnly(string);
        }

        @TruffleBoundary
        private int multiByteStringLength(Encoding enc, byte[] bytes, int p, int end) {
            return StringSupport.length(enc, bytes, p, end);
        }

        // TODO (nirvdrum 10-Mar-15): This was extracted from JRuby, but likely will need to become a Rubinius primitive.
        private Object substr(RubyString string, int beg, int len) {
            final ByteList bytes = string.getBytes();

            int length = bytes.length();
            if (len < 0 || beg > length) return nil();

            if (beg < 0) {
                beg += length;
                if (beg < 0) return nil();
            }

            int end = Math.min(length, beg + len);

            final ByteList substringBytes = new ByteList(bytes, beg, end - beg);
            substringBytes.setEncoding(bytes.getEncoding());

            return getContext().makeString(string.getLogicalClass(), substringBytes);
        }
    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends CoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean empty(RubyString string) {
            return string.getBytes().length() == 0;
        }
    }

    @CoreMethod(names = "encoding")
    public abstract static class EncodingNode extends CoreMethodNode {

        public EncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodingNode(EncodingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyEncoding encoding(RubyString string) {
            notDesignedForCompilation();

            return RubyEncoding.getEncoding(string.getBytes().getEncoding());
        }
    }

    @CoreMethod(names = "force_encoding", required = 1)
    public abstract static class ForceEncodingNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ForceEncodingNode(ForceEncodingNode prev) {
            super(prev);
            toStrNode = prev.toStrNode;
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

        @Specialization(guards = { "!isRubyString(arguments[1])", "!isRubyEncoding(arguments[1])" })
        public RubyString forceEncoding(VirtualFrame frame, RubyString string, Object encoding) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return forceEncoding(string, toStrNode.executeRubyString(frame, encoding));
        }

    }

    @CoreMethod(names = "getbyte", required = 1)
    public abstract static class GetByteNode extends CoreMethodNode {

        public GetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetByteNode(GetByteNode prev) {
            super(prev);
        }

        @Specialization
        public int getByte(RubyString string, int index) {
            return string.getBytes().get(index);
        }
    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public int hash(RubyString string) {
            return string.getBytes().hashCode();
        }

    }

    @CoreMethod(names = "inspect", taintFromSelf = true)
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyString inspect(RubyString string) {
            notDesignedForCompilation();

            final org.jruby.RubyString inspected = (org.jruby.RubyString) org.jruby.RubyString.inspect19(getContext().getRuntime(), string.getBytes());
            return getContext().makeString(inspected.getByteList());
        }
    }

    @CoreMethod(names = "initialize", optional = 1, taintFromParameters = 0)
    public abstract static class InitializeNode extends CoreMethodNode {

        @Child private IsFrozenNode isFrozenNode;
        @Child private ToStrNode toStrNode;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
            isFrozenNode = prev.isFrozenNode;
            toStrNode = prev.toStrNode;
        }

        @Specialization
        public RubyString initialize(RubyString self, UndefinedPlaceholder from) {
            return self;
        }

        @Specialization
        public RubyString initialize(RubyString self, RubyString from) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeFactory.create(getContext(), getSourceSection(), null));
            }

            if (isFrozenNode.executeIsFrozen(self)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(
                        getContext().getCoreLibrary().frozenError(self.getLogicalClass().getName(), this));
            }

            self.set(from.getBytes());
            self.setCodeRange(from.getCodeRange());

            return self;
        }

        @Specialization(guards = { "!isRubyString(arguments[1])", "!isUndefinedPlaceholder(arguments[1])" })
        public RubyString initialize(VirtualFrame frame, RubyString self, Object from) {
            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return initialize(self, toStrNode.executeRubyString(frame, from));
        }
    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization
        public Object initializeCopy(RubyString self, RubyString from) {
            notDesignedForCompilation();

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
        @NodeChild(value = "string"),
        @NodeChild(value = "index"),
        @NodeChild(value = "otherString")
    })
    public abstract static class InsertNode extends RubyNode {

        @Child private CallDispatchHeadNode concatNode;
        @Child private TaintResultNode taintResultNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            concatNode = DispatchHeadNodeFactory.createMethodCall(context);
            taintResultNode = new TaintResultNode(context, sourceSection, false, new int[] {});
        }

        public InsertNode(InsertNode prev) {
            super(prev);
            concatNode = prev.concatNode;
            taintResultNode = prev.taintResultNode;
        }

        @CreateCast("index") public RubyNode coerceIndexToInt(RubyNode index) {
            return ToIntNodeFactory.create(getContext(), getSourceSection(), index);
        }

        @CreateCast("otherString") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), other);
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

    @CoreMethod(names = "match", required = 1, taintFromSelf = true)
    public abstract static class MatchNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode regexpMatchNode;

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            regexpMatchNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
            regexpMatchNode = prev.regexpMatchNode;
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyString regexpString) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getBytes(), Option.DEFAULT);

            return regexpMatchNode.call(frame, regexp, "match", null, string);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyRegexp regexp) {
            return regexpMatchNode.call(frame, regexp, "match", null, string);
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "modify!")
    public abstract static class ModifyBangNode extends CoreMethodNode {

        public ModifyBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModifyBangNode(ModifyBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString modifyBang(RubyString string) {
            string.modify();
            return string;
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "num_bytes=", required = 1)
    public abstract static class SetNumBytesNode extends CoreMethodNode {

        public SetNumBytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetNumBytesNode(SetNumBytesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString setNumBytes(RubyString string, int count) {
            string.getByteList().view(0, count);
            return string;
        }
    }

    @CoreMethod(names = "ord")
    public abstract static class OrdNode extends CoreMethodNode {

        public OrdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OrdNode(OrdNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public int ord(RubyString string) {
            return ((org.jruby.RubyFixnum) getContext().toJRuby(string).ord(getContext().getRuntime().getCurrentContext())).getIntValue();
        }
    }

    @CoreMethod(names = "replace", required = 1, raiseIfFrozenSelf = true, taintFromParameters = 0)
    @NodeChildren({
        @NodeChild(value = "string"),
        @NodeChild(value = "other")
    })
    public abstract static class ReplaceNode extends RubyNode {

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReplaceNode(ReplaceNode prev) {
            super(prev);
        }

        @CreateCast("other") public RubyNode coerceOtherToString(RubyNode other) {
            return ToStrNodeFactory.create(getContext(), getSourceSection(), other);
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

    @CoreMethod(names = "rindex", required = 1, optional = 1, lowerFixnumParameters = 1)
    public abstract static class RindexNode extends CoreMethodNode {

        @Child private SizeNode sizeNode;

        public RindexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sizeNode = StringNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { null });
        }

        public RindexNode(RindexNode prev) {
            super(prev);
            sizeNode = prev.sizeNode;
        }

        @Specialization
        public Object rindex(VirtualFrame frame, RubyString string, RubyString subString, @SuppressWarnings("unused") UndefinedPlaceholder endPosition) {
            notDesignedForCompilation();

            return rindex(frame, string, subString, sizeNode.executeIntegerFixnum(frame, string));
        }

        @Specialization
        public Object rindex(VirtualFrame frame, RubyString string, RubyString subString, int endPosition) {
            notDesignedForCompilation();

            final int stringLength = sizeNode.executeIntegerFixnum(frame, string);
            int normalizedEndPosition = endPosition;

            if (endPosition < 0) {
                normalizedEndPosition = endPosition + stringLength;

                if (normalizedEndPosition < 0) {
                    return nil();
                }
            } else if (endPosition > stringLength) {
                normalizedEndPosition = stringLength;
            }

            int result = StringSupport.rindex(string.getBytes(), stringLength, subString.length(),
                    normalizedEndPosition, subString, string.getBytes().getEncoding()
            );

            if (result >= 0) {
                return result;
            } else {
                return nil();
            }
        }
    }

    @CoreMethod(names = "swapcase", taintFromSelf = true)
    public abstract static class SwapcaseNode extends CoreMethodNode {
        public SwapcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SwapcaseNode(SwapcaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString swapcase(RubyString string) {
            notDesignedForCompilation();

            ByteList byteList = StringNodesHelper.swapcase(string);
            return getContext().makeString(string.getLogicalClass(), byteList);
        }
    }

    @CoreMethod(names = "swapcase!", raiseIfFrozenSelf = true)
    public abstract static class SwapcaseBangNode extends CoreMethodNode {
        public SwapcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SwapcaseBangNode(SwapcaseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString swapcase(RubyString string) {
            notDesignedForCompilation();

            ByteList byteList = StringNodesHelper.swapcase(string);
            string.set(byteList);
            return string;
        }
    }

    @CoreMethod(names = "rstrip", taintFromSelf = true)
    public abstract static class RStripNode extends CoreMethodNode {

        public RStripNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RStripNode(RStripNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString rstrip(RubyString string) {
            notDesignedForCompilation();

            String str = string.toString();
            int last = str.length()-1;
            while (last >= 0 && " \r\n\t".indexOf(str.charAt(last)) != -1) {
                last--;
            }

            return getContext().makeString(str.substring(0, last + 1));
        }

    }

    @CoreMethod(names = "dump", taintFromSelf = true)
    @ImportGuards(StringGuards.class)
    public abstract static class DumpNode extends CoreMethodNode {

        public DumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpNode(DumpNode prev) {
            super(prev);
        }

        @Specialization(guards = "isAsciiCompatible")
        public RubyString dumpAsciiCompatible(RubyString string) {
            // Taken from org.jruby.RubyString#dump

            ByteList outputBytes = dumpCommon(string);

            final RubyString result = getContext().makeString(string.getLogicalClass(), outputBytes);
            result.getByteList().setEncoding(string.getByteList().getEncoding());
            result.setCodeRange(StringSupport.CR_7BIT);

            return result;
        }

        @Specialization(guards = "!isAsciiCompatible")
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

    @CoreMethod(names = "scan", required = 1, needsBlock = true, taintFromParameters = 0)
    public abstract static class ScanNode extends YieldingCoreMethodNode {

        public ScanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ScanNode(ScanNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyString regexpString, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getBytes(), Option.DEFAULT);
            return scan(string, regexp, block);
        }

        @Specialization
        public RubyString scan(VirtualFrame frame, RubyString string, RubyString regexpString, RubyProc block) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getBytes(), Option.DEFAULT);
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

            final byte[] stringBytes = string.getBytes().bytes();
            final Encoding encoding = string.getBytes().getEncoding();
            final Matcher matcher = regexp.getRegex().matcher(stringBytes);

            int p = string.getBytes().getBegin();
            int end = 0;
            int range = p + string.getBytes().getRealSize();

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
                    end = StringSupport.positionEndForScan(string.getBytes(), matcher, encoding, p, range);
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
                    end = StringSupport.positionEndForScan(string.getBytes(), matcher, encoding, p, range);
                }

                regexp.setThread("$~", lastGoodMatchData);
            }

            return string;
        }
    }

    @CoreMethod(names = "setbyte", required = 2, raiseIfFrozenSelf = true)
    public abstract static class SetByteNode extends CoreMethodNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetByteNode(SetByteNode prev) {
            super(prev);
        }

        @Specialization
        public Object setByte(RubyString string, int index, Object value) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException("getbyte not implemented");
        }
    }

    @CoreMethod(names = {"size", "length"})
    @ImportGuards(StringGuards.class)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        public abstract int executeIntegerFixnum(VirtualFrame frame, RubyString string);

        @Specialization(guards = "isSingleByteOptimizable")
        public int sizeSingleByte(RubyString string) {
            return string.getByteList().getRealSize();
        }

        @Specialization(guards = "!isSingleByteOptimizable")
        public int size(RubyString string) {
            return StringSupport.strLengthFromRubyString(string);
        }
    }

    @CoreMethod(names = "succ", taintFromSelf = true)
    public abstract static class SuccNode extends CoreMethodNode {

        public SuccNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SuccNode(SuccNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString succ(RubyString string) {
            notDesignedForCompilation();

            if (string.length() > 0) {
                return getContext().makeString(string.getLogicalClass(), StringSupport.succCommon(getContext().getRuntime(), string.getBytes()));
            } else {
                return getContext().makeString(string.getLogicalClass(), "");
            }
        }
    }

    @CoreMethod(names = "succ!", raiseIfFrozenSelf = true)
    public abstract static class SuccBangNode extends CoreMethodNode {

        public SuccBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SuccBangNode(SuccBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString succBang(RubyString string) {
            notDesignedForCompilation();

            if (string.getByteList().getRealSize() > 0) {
                string.set(StringSupport.succCommon(getContext().getRuntime(), string.getBytes()));
            }

            return string;
        }
    }

    // String#sum is in Java because without OSR we can't warm up the Rubinius implementation

    @CoreMethod(names = "sum", optional = 1)
    public abstract static class SumNode extends CoreMethodNode {

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

        public SumNode(SumNode prev) {
            super(prev);
            addNode = prev.addNode;
            subNode = prev.subNode;
            shiftNode = prev.shiftNode;
            andNode = prev.andNode;
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

        @Specialization(guards = {"!isInteger(arguments[1])", "!isLong(arguments[1])", "!isUndefinedPlaceholder(arguments[1])"})
        public Object sum(VirtualFrame frame, RubyString string, Object bits) {
            return ruby(frame, "sum Rubinius::Type.coerce_to(bits, Fixnum, :to_int)", "bits", bits);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(RubyString string) {
            try {
                return Double.parseDouble(string.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    @CoreMethod(names = { "to_s", "to_str" })
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization(guards = "!isStringSubclass")
        public RubyString toS(RubyString string) {
            return string;
        }

        @Specialization(guards = "isStringSubclass")
        public Object toSOnSubclass(VirtualFrame frame, RubyString string) {
            return ruby(frame, "''.replace(self)", "self", string);
        }

        public boolean isStringSubclass(RubyString string) {
            return string.getLogicalClass() != getContext().getCoreLibrary().getStringClass();
        }

    }

    @CoreMethod(names = {"to_sym", "intern"})
    public abstract static class ToSymNode extends CoreMethodNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSymNode(ToSymNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol toSym(RubyString string) {
            notDesignedForCompilation();

            return getContext().newSymbol(string.getByteList());
        }
    }

    @CoreMethod(names = "reverse!", raiseIfFrozenSelf = true)
    @ImportGuards(StringGuards.class)
    public abstract static class ReverseBangNode extends CoreMethodNode {

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReverseBangNode(ReverseBangNode prev) {
            super(prev);
        }

        @Specialization(guards = "reverseIsEqualToSelf")
        public RubyString reverseNoOp(RubyString string) {
            return string;
        }

        @Specialization(guards = { "!reverseIsEqualToSelf", "isSingleByteOptimizable" })
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

        @Specialization(guards = { "!reverseIsEqualToSelf", "!isSingleByteOptimizable" })
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

    @CoreMethod(names = "unpack", required = 1)
    public abstract static class UnpackNode extends ArrayCoreMethodNode {

        public UnpackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnpackNode(UnpackNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyArray unpack(RubyString string, RubyString format) {
            final org.jruby.RubyArray jrubyArray = Pack.unpack(getContext().getRuntime(), string.getBytes(), format.getBytes());
            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), jrubyArray.toArray());
        }

    }

    @CoreMethod(names = "upcase", taintFromSelf = true)
    public abstract static class UpcaseNode extends CoreMethodNode {

        public UpcaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpcaseNode(UpcaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString upcase(RubyString string) {
            notDesignedForCompilation();
            final ByteList byteListString = StringNodesHelper.upcase(getContext().getRuntime(), string.getByteList());

            return string.getContext().makeString(string.getLogicalClass(), byteListString);
        }

    }

    @CoreMethod(names = "upcase!", raiseIfFrozenSelf = true)
    public abstract static class UpcaseBangNode extends CoreMethodNode {

        public UpcaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpcaseBangNode(UpcaseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject upcaseBang(RubyString string) {
            notDesignedForCompilation();

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
    public abstract static class ValidEncodingQueryNode extends CoreMethodNode {

        public ValidEncodingQueryNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ValidEncodingQueryNode(ValidEncodingQueryNode prev) {
            super(prev);
        }

        @Specialization
        public boolean validEncodingQuery(RubyString string) {
            return string.scanForCodeRange() != StringSupport.CR_BROKEN;
        }

    }

    @CoreMethod(names = "capitalize!", raiseIfFrozenSelf = true)
    public abstract static class CapitalizeBangNode extends CoreMethodNode {

        public CapitalizeBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CapitalizeBangNode(CapitalizeBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject capitalizeBang(RubyString string) {
            notDesignedForCompilation();

            String javaString = string.toString();

            if (javaString.isEmpty()) {
                return nil();
            } else {
                final ByteList byteListString = StringNodesHelper.capitalize(string);
                
                if (string.getByteList().equals(byteListString)) {
                    return nil();
                }else {
                    string.set(byteListString);
                    return string;
                }
            }
        }
    }

    @CoreMethod(names = "capitalize", taintFromSelf = true)
    public abstract static class CapitalizeNode extends CoreMethodNode {

        public CapitalizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CapitalizeNode(CapitalizeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString capitalize(RubyString string) {
            notDesignedForCompilation();
            String javaString = string.toString();

            if (javaString.isEmpty()) {
                return string;
            } else {
                final ByteList byteListString = StringNodesHelper.capitalize(string);
                return string.getContext().makeString(string.getLogicalClass(), byteListString);
            }
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends CoreMethodNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClearNode(ClearNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString clear(RubyString string) {
            notDesignedForCompilation();
            ByteList empty = ByteList.EMPTY_BYTELIST;
            empty.setEncoding(string.getBytes().getEncoding());

            string.set(empty);
            return string;
        }
    }

    public static class StringNodesHelper {

        @TruffleBoundary
        public static ByteList capitalize(RubyString string) {
            String javaString = string.toString();
            String head = javaString.substring(0, 1).toUpperCase(Locale.ENGLISH);
            String tail = javaString.substring(1, javaString.length()).toLowerCase(Locale.ENGLISH);
            ByteList byteListString = ByteList.create(head + tail);
            byteListString.setEncoding(string.getBytes().getEncoding());
            return byteListString;
        }

        @TruffleBoundary
        public static ByteList upcase(Ruby runtime, ByteList string) {
            return runtime.newString(string).upcase(runtime.getCurrentContext()).getByteList();
        }

        @TruffleBoundary
        public static ByteList downcase(Ruby runtime, ByteList string) {
            return runtime.newString(string).downcase(runtime.getCurrentContext()).getByteList();
        }

        @TruffleBoundary
        public static ByteList chompWithString(RubyString string, RubyString stringToChomp) {

            String tempString = string.toString();

            if (tempString.endsWith(stringToChomp.toString())) {
                tempString = tempString.substring(0, tempString.length() - stringToChomp.toString().length());
            }

            ByteList byteList = ByteList.create(tempString);
            byteList.setEncoding(string.getBytes().getEncoding());

            return byteList;
        }

        @TruffleBoundary
        public static ByteList swapcase(RubyString string) {
            char[] charArray = string.toString().toCharArray();
            StringBuilder newString = new StringBuilder();

            for (int i = 0; i < charArray.length; i++) {
                char current = charArray[i];

                if (Character.isLowerCase(current)) {
                    newString.append(Character.toString(current).toUpperCase(Locale.ENGLISH));
                } else if (Character.isUpperCase(current)){
                    newString.append(Character.toString(current).toLowerCase(Locale.ENGLISH));
                } else {
                    newString.append(current);
                }
            }
            ByteList byteListString = ByteList.create(newString);
            byteListString.setEncoding(string.getBytes().getEncoding());

            return byteListString;
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

        @TruffleBoundary
        public static void replaceInternal(RubyString string, int start, int length, RubyString replacement) {
            StringSupport.replaceInternal19(start, length, string, replacement);
        }
    }

}
