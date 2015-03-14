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
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Region;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeFactory;
import org.jruby.truffle.nodes.coerce.ToStrNode;
import org.jruby.truffle.nodes.coerce.ToStrNodeFactory;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodes;
import org.jruby.truffle.nodes.rubinius.StringPrimitiveNodesFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeSupport;
import org.jruby.util.Pack;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@CoreClass(name = "String")
public abstract class StringNodes {

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends CoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString add(RubyString a, RubyString b) {
            notDesignedForCompilation();

            return (RubyString) getContext().toTruffle(getContext().toJRuby(a).op_plus(getContext().getRuntime().getCurrentContext(), getContext().toJRuby(b)));
        }
    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0)
    public abstract static class MulNode extends CoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString add(RubyString string, int times) {
            notDesignedForCompilation();

            final ByteList inputBytes = string.getBytes();
            final ByteList outputBytes = new ByteList(string.getBytes().length() * times);

            for (int n = 0; n < times; n++) {
                outputBytes.append(inputBytes);
            }

            outputBytes.setEncoding(inputBytes.getEncoding());

            return new RubyString(getContext().getCoreLibrary().getStringClass(), outputBytes);
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
    public abstract static class CompareNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubyString a, RubyString b) {
            notDesignedForCompilation();

            final int result = a.toString().compareTo(b.toString());

            if (result < 0) {
                return -1;
            } else if (result > 0) {
                return 1;
            }

            return 0;
        }

        @Specialization(guards = "!isRubyString(b)")
        public Object compare(VirtualFrame frame, RubyString a, Object b) {
            notDesignedForCompilation();

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
            string.getByteList().append((byte) other);
            return string;
        }

        @Specialization
        public RubyString concat(RubyString string, long other) {
            string.getByteList().append((byte) other);
            return string;
        }

        @TruffleBoundary
        @Specialization
        public RubyString concat(RubyString string, RubyString other) {
            // TODO (nirvdrum 06-Feb-15) This shouldn't be designed for compilation because we don't support all the String semantics yet, but a bench9000 benchmark has it on a hot path, so commenting out for now.
            //notDesignedForCompilation();

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

        @Specialization(guards = {"!isInteger(other)", "!isLong(other)", "!isRubyString(other)"})
        public Object concat(VirtualFrame frame, RubyString string, Object other) {
            notDesignedForCompilation();
            return ruby(frame, "concat StringValue(other)", "other", other);
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
        @Child private KernelNodes.DupNode dupNode;

        private final BranchProfile outOfBounds = BranchProfile.create();

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        public Object getIndex(RubyString string, int index, @SuppressWarnings("unused") UndefinedPlaceholder undefined) {
            int normalizedIndex = string.normalizeIndex(index);
            final ByteList bytes = string.getBytes();

            if (normalizedIndex < 0 || normalizedIndex >= bytes.length()) {
                outOfBounds.enter();
                return nil();
            } else {
                return getContext().makeString(string.getLogicalClass(), bytes.charAt(normalizedIndex), string.getByteList().getEncoding());
            }
        }

        @Specialization(guards = { "!isRubyRange(index)", "!isRubyRegexp(index)", "!isRubyString(index)" })
        public Object getIndex(VirtualFrame frame, RubyString string, Object index, UndefinedPlaceholder undefined) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return getIndex(string, toIntNode.executeIntegerFixnum(frame, index), undefined);
        }

        @Specialization
        public Object slice(RubyString string, RubyRange.IntegerFixnumRange range, @SuppressWarnings("unused") UndefinedPlaceholder undefined) {
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
        public Object slice(RubyString string, int start, int length) {
            // TODO(CS): not sure if this is right - encoding
            final ByteList bytes = string.getBytes();
            final int begin = string.normalizeIndex(start);

            if (begin < 0 || begin > bytes.length() || length < 0) {
                outOfBounds.enter();
                return nil();
            } else {
                final int end = Math.min(bytes.length(), begin + length);

                final ByteList byteList = new ByteList(bytes, begin, end - begin);
                byteList.setEncoding(string.getByteList().getEncoding());

                return getContext().makeString(string.getLogicalClass(), byteList);
            }
        }

        @Specialization(guards = "!isUndefinedPlaceholder(length)")
        public Object slice(VirtualFrame frame, RubyString string, int start, Object length) {
            notDesignedForCompilation();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return slice(string, start, toIntNode.executeIntegerFixnum(frame, length));
        }

        @Specialization(guards = { "!isRubyRange(start)", "!isRubyRegexp(start)", "!isRubyString(start)", "!isUndefinedPlaceholder(length)" })
        public Object slice(VirtualFrame frame, RubyString string, Object start, Object length) {
            notDesignedForCompilation();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return slice(string, toIntNode.executeIntegerFixnum(frame, start), toIntNode.executeIntegerFixnum(frame, length));
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyString string, RubyRegexp regexp, @SuppressWarnings("unused") UndefinedPlaceholder capture) {
            notDesignedForCompilation();

            return slice(frame, string, regexp, 0);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(capture)")
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
        public Object slice(VirtualFrame frame, RubyString string, RubyString matchStr, @SuppressWarnings("unused") UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            if (includeNode == null) {
                CompilerDirectives.transferToInterpreter();
                includeNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            boolean result = includeNode.callBoolean(frame, string, "include?", null, matchStr);

            if (result) {
                if (dupNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    dupNode = insert(KernelNodesFactory.DupNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
                }

                return dupNode.dup(frame, matchStr);
            }

            return nil();
        }
    }

    @CoreMethod(names = "[]=", required = 2, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class ElementSetNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;

        public ElementSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toStrNode = ToStrNodeFactory.create(context, sourceSection, null);
        }

        public ElementSetNode(ElementSetNode prev) {
            super(prev);
            toStrNode = prev.toStrNode;
        }

        @Specialization
        public RubyString elementSet(VirtualFrame frame, RubyString string, int index, Object replacement) {
            notDesignedForCompilation();

            if (index < 0) {
                if (-index > string.length()) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), this));
                }

                index = index + string.length();

            } else if (index > string.length()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), this));
            }

            final RubyString coerced = toStrNode.executeRubyString(frame, replacement);
            StringSupport.replaceInternal19(index, 1, string, coerced);

            return coerced;
        }

        @Specialization
        public RubyString elementSet(VirtualFrame frame, RubyString string, RubyRange.IntegerFixnumRange range, Object replacement) {
            notDesignedForCompilation();

            int begin = range.getBegin();
            int end = range.getEnd();
            final int stringLength = string.length();

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
            StringSupport.replaceInternal19(begin, length, string, coerced);

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

    @CoreMethod(names = "chomp!", optional = 1, raiseIfFrozenSelf = true)
    public abstract static class ChompBangNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;

        public ChompBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChompBangNode(ChompBangNode prev) {
            super(prev);
        }

        @Specialization
        public Object chompBang(RubyString string, UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            if (string.length() == 0) {
                return nil();
            }

            string.set(StringNodesHelper.chomp(string));
            return string;
        }

        @Specialization
        public RubyNilClass chompBangWithNil(RubyString string, RubyNilClass stringToChomp) {
            return nil();
        }

        @Specialization(guards = { "!isUndefinedPlaceholder(stringToChomp)", "!isRubyNilClass(stringToChomp)" })
        public RubyString chompBangWithString(VirtualFrame frame, RubyString string, Object stringToChomp) {
            notDesignedForCompilation();

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
            }

            string.set(StringNodesHelper.chompWithString(string, toStrNode.executeRubyString(frame, stringToChomp)));
            return string;
        }
    }

    @CoreMethod(names = "chop!", raiseIfFrozenSelf = true)
    public abstract static class ChopBangNode extends CoreMethodNode {

        public ChopBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChopBangNode(ChopBangNode prev) {
            super(prev);
        }

        @Specialization
        public Object chopBang(RubyString string) {
            notDesignedForCompilation();

            if (string.length() == 0) {
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

        private int countSlow(VirtualFrame frame, RubyString string, Object[] args) {
            notDesignedForCompilation();

            RubyString[] otherStrings = new RubyString[args.length];

            for (int i = 0; i < args.length; i++) {
                otherStrings[i] = toStr.executeRubyString(frame, args[i]);
            }

            return string.count(otherStrings);
        }
    }

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
            Encoding enc = string.checkEncoding(otherString);

            boolean[] squeeze = new boolean[StringSupport.TRANS_SIZE + 1];
            StringSupport.TrTables tables = StringSupport.trSetupTable(otherString.getBytes(),
                    getContext().getRuntime(),
                    squeeze, null, true, enc);

            for (int i = 1; i < otherStrings.length; i++) {
                enc = string.checkEncoding(otherStrings[i]);
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
            ByteList newByteList = StringNodesHelper.downcase(string);

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

            ByteList newByteList = StringNodesHelper.downcase(string);

            if (newByteList.equal(string.getBytes())) {
                return nil();
            } else {
                string.set(newByteList);
                return string;
            }
        }
    }

    @CoreMethod(names = "each_byte", needsBlock = true)
    public abstract static class EachByteNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        public EachByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachByteNode(EachByteNode prev) {
            super(prev);
        }

        @Specialization
        public Object eachByte(VirtualFrame frame, RubyString string, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, string, "to_enum", null, getContext().newSymbol("each_byte"));
        }

        @Specialization
        public RubyString eachByte(VirtualFrame frame, RubyString string, RubyProc block) {
            notDesignedForCompilation();

            final ByteList bytes = string.getBytes();
            final int begin = bytes.getBegin();

            for (int i = 0; i < bytes.getRealSize(); i++) {
                yield(frame, block, bytes.get(begin + i));
            }

            return string;
        }

    }

    @CoreMethod(names = "each_char", needsBlock = true)
    public abstract static class EachCharNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        public EachCharNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachCharNode(EachCharNode prev) {
            super(prev);
        }

        @Specialization
        public Object eachChar(VirtualFrame frame, RubyString string, @SuppressWarnings("unused") UndefinedPlaceholder block) {
            notDesignedForCompilation();

            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, string, "to_enum", null, getContext().newSymbol("each_char"));
        }

        @Specialization(guards = "isValidOr7BitEncoding(string)")
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

        @Specialization(guards = "!isValidOr7BitEncoding(string)")
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

    @CoreMethod(names = "encode", optional = 2)
    public abstract static class EncodeNode extends CoreMethodNode {

        @Child private ToStrNode toStrNode;
        @Child private EncodingNodes.DefaultInternalNode defaultInternalNode;

        public EncodeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EncodeNode(EncodeNode prev) {
            super(prev);
        }

        @TruffleBoundary
        @Specialization
        public RubyString encode(RubyString string, RubyString encoding, @SuppressWarnings("unused") UndefinedPlaceholder options) {

            final org.jruby.RubyString jrubyString = getContext().toJRuby(string);
            final org.jruby.RubyString jrubyEncodingString = getContext().toJRuby(encoding);
            final org.jruby.RubyString jrubyTranscoded = (org.jruby.RubyString) jrubyString.encode(getContext().getRuntime().getCurrentContext(), jrubyEncodingString);

            return getContext().toTruffle(jrubyTranscoded);
        }

        @Specialization
        public RubyString encode(RubyString string, RubyString encoding, @SuppressWarnings("unused") RubyHash options) {

            // TODO (nirvdrum 20-Feb-15) We need to do something with the options hash. I'm stubbing this out just to get the jUnit mspec formatter running.
            return encode(string, encoding, UndefinedPlaceholder.INSTANCE);
        }

        @TruffleBoundary
        @Specialization
        public RubyString encode(RubyString string, RubyEncoding encoding, @SuppressWarnings("unused") UndefinedPlaceholder options) {

            final org.jruby.RubyString jrubyString = getContext().toJRuby(string);
            final org.jruby.RubyString jrubyEncodingString = getContext().toJRuby(getContext().makeString(encoding.getName()));
            final org.jruby.RubyString jrubyTranscoded = (org.jruby.RubyString) jrubyString.encode(getContext().getRuntime().getCurrentContext(), jrubyEncodingString);

            return getContext().toTruffle(jrubyTranscoded);
        }

        @Specialization(guards = { "!isRubyString(encoding)", "!isRubyEncoding(encoding)", "!isUndefinedPlaceholder(encoding)" })
        public RubyString encode(VirtualFrame frame, RubyString string, Object encoding, UndefinedPlaceholder options) {

            if (toStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStrNode = insert(ToStrNodeFactory.create(getContext(), getSourceSection(), null));
            }

            return encode(string, toStrNode.executeRubyString(frame, encoding), options);
        }

        @Specialization
        public RubyString encode(RubyString string, @SuppressWarnings("unused") UndefinedPlaceholder encoding, @SuppressWarnings("unused") UndefinedPlaceholder options) {

            if (defaultInternalNode == null) {
                CompilerDirectives.transferToInterpreter();
                defaultInternalNode = insert(EncodingNodesFactory.DefaultInternalNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{}));
            }

            final Object defaultInternalEncoding = defaultInternalNode.defaultInternal();

            if (defaultInternalEncoding == nil()) {
                return encode(string, RubyEncoding.getEncoding("UTF-8"), UndefinedPlaceholder.INSTANCE);
            }

            return encode(string, (RubyEncoding) defaultInternalEncoding, UndefinedPlaceholder.INSTANCE);
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

    @CoreMethod(names = "end_with?", required = 1)
    public abstract static class EndWithNode extends CoreMethodNode {

        public EndWithNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EndWithNode(EndWithNode prev) {
            super(prev);
        }

        @Specialization
        public boolean endWith(RubyString string, RubyString b) {
            notDesignedForCompilation();

            return string.toString().endsWith(b.toString());
        }
    }

    @CoreMethod(names = "force_encoding", required = 1)
    public abstract static class ForceEncodingNode extends CoreMethodNode {

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ForceEncodingNode(ForceEncodingNode prev) {
            super(prev);
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

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString initialize(RubyString self, UndefinedPlaceholder from) {
            return self;
        }

        @Specialization
        public RubyString initialize(RubyString self, RubyString from) {
            notDesignedForCompilation();

            self.set(from.getBytes());
            self.setCodeRange(from.getCodeRange());

            return self;
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

            self.getBytes().replace(from.getBytes().bytes());

            return self;
        }

    }

    @CoreMethod(names = "insert", required = 2, lowerFixnumParameters = 0, raiseIfFrozenSelf = true, taintFromParameters = 1)
    public abstract static class InsertNode extends CoreMethodNode {

        @Child private ConcatNode concatNode;
        @Child private GetIndexNode getIndexNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            concatNode = StringNodesFactory.ConcatNodeFactory.create(context, sourceSection, null, null);
            getIndexNode = StringNodesFactory.GetIndexNodeFactory.create(context, sourceSection, new RubyNode[]{});
        }

        public InsertNode(InsertNode prev) {
            super(prev);
            concatNode = prev.concatNode;
            getIndexNode = prev.getIndexNode;
        }

        @Specialization
        public RubyString insert(RubyString string, int index, RubyString otherString) {
            notDesignedForCompilation();

            if (index == -1) {
                concatNode.concat(string, otherString);

                return string;

            } else if (index < 0) {
                // Incrementing first seems weird, but MRI does it and it's significant because it uses the modified
                // index value in its error messages.  This seems wrong, but we should be compatible.
                index++;

                if (-index > string.length()) {
                    CompilerDirectives.transferToInterpreter();

                    throw new RaiseException(getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), this));
                }

                index = index + string.length();

            } else if (index > string.length()) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().indexError(String.format("index %d out of string", index), this));
            }

            // TODO (Kevin): using node directly and cast
            RubyString firstPart = (RubyString) getIndexNode.slice(string, 0, index);
            RubyString secondPart = (RubyString) getIndexNode.slice(string, index, string.length());

            RubyString concatenated = concatNode.concat(concatNode.concat(firstPart, otherString), secondPart);

            string.set(concatenated.getBytes());

            return string;
        }
    }

    @CoreMethod(names = "ljust", required = 1, optional = 1, lowerFixnumParameters = 0)
    public abstract static class LjustNode extends CoreMethodNode {

        public LjustNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LjustNode(LjustNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString ljust(RubyString string, int length, @SuppressWarnings("unused") UndefinedPlaceholder padding) {
            notDesignedForCompilation();

            return getContext().makeString(RubyString.ljust(string.toString(), length, " "));
        }

        @Specialization
        public RubyString ljust(RubyString string, int length, RubyString padding) {
            notDesignedForCompilation();

            return getContext().makeString(RubyString.ljust(string.toString(), length, padding.toString()));
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

    @CoreMethod(names = "ord")
    public abstract static class OrdNode extends CoreMethodNode {

        public OrdNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public OrdNode(OrdNode prev) {
            super(prev);
        }

        @Specialization
        public int ord(RubyString string) {
            notDesignedForCompilation();
            return ((org.jruby.RubyFixnum) getContext().toJRuby(string).ord(getContext().getRuntime().getCurrentContext())).getIntValue();
        }
    }

    @CoreMethod(names = "replace", required = 1, raiseIfFrozenSelf = true, taintFromParameters = 0)
    public abstract static class ReplaceNode extends CoreMethodNode {

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReplaceNode(ReplaceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString replace(RubyString string, RubyString other) {
            notDesignedForCompilation();

            if (string == other) {
                return string;
            }

            string.getByteList().replace(other.getByteList().bytes());
            string.setCodeRange(other.getCodeRange());

            return string;
        }

    }

    @CoreMethod(names = "rindex", required = 1, optional = 1, lowerFixnumParameters = 1)
    public abstract static class RindexNode extends CoreMethodNode {

        public RindexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RindexNode(RindexNode prev) {
            super(prev);
        }

        @Specialization
        public Object rindex(RubyString string, RubyString subString, @SuppressWarnings("unused") UndefinedPlaceholder endPosition) {
            notDesignedForCompilation();

            return rindex(string, subString, string.length());
        }

        @Specialization
        public Object rindex(RubyString string, RubyString subString, int endPosition) {
            notDesignedForCompilation();

            int normalizedEndPosition = endPosition;

            if (endPosition < 0) {
                normalizedEndPosition = endPosition + string.length();

                if (normalizedEndPosition < 0) {
                    return nil();
                }
            } else if (endPosition > string.length()) {
                normalizedEndPosition = string.length();
            }

            int result = StringSupport.rindex(string.getBytes(), string.length(), subString.length(),
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
    public abstract static class DumpNode extends CoreMethodNode {

        public DumpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DumpNode(DumpNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString rstrip(RubyString string) {
            notDesignedForCompilation();

            return string.dump();
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
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
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
                return getContext().makeString(string.getLogicalClass(), StringSupport.succCommon(string.getBytes()));
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

            if (string.length() > 0) {
                string.set(StringSupport.succCommon(string.getBytes()));
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

        @Specialization(guards = {"!isInteger(bits)", "!isLong(bits)", "!isUndefinedPlaceholder(bits)"})
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

    @CoreMethod(names = "reverse", taintFromSelf = true)
    public abstract static class ReverseNode extends CoreMethodNode {

        public ReverseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReverseNode(ReverseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString reverse(RubyString string) {
            notDesignedForCompilation();

            return RubyString.fromByteList(string.getLogicalClass(), StringNodesHelper.reverse(string));
        }
    }

    @CoreMethod(names = "reverse!", raiseIfFrozenSelf = true)
    public abstract static class ReverseBangNode extends CoreMethodNode {

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReverseBangNode(ReverseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString reverse(RubyString string) {
            notDesignedForCompilation();

            string.set(StringNodesHelper.reverse(string));
            return string;
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
            final ByteList byteListString = StringNodesHelper.upcase(string);

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
        public RubyString upcaseBang(RubyString string) {
            notDesignedForCompilation();
            final ByteList byteListString = StringNodesHelper.upcase(string);
            string.set(byteListString);

            return string;
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

    @CoreMethod(names = "chr")
    public abstract static class ChrNode extends CoreMethodNode {

        public ChrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChrNode(ChrNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chr(RubyString string) {
            notDesignedForCompilation();
            if (string.toString().isEmpty()) {
                return string;
            } else {
                String head = string.toString().substring(0, 1);
                ByteList byteString = ByteList.create(head);
                byteString.setEncoding(string.getBytes().getEncoding());

                return string.getContext().makeString(byteString);
            }
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
        public static ByteList upcase(RubyString string) {
            ByteList byteListString = ByteList.create(string.toString().toUpperCase(Locale.ENGLISH));
            byteListString.setEncoding(string.getBytes().getEncoding());
            return byteListString;
        }

        @TruffleBoundary
        public static ByteList downcase(RubyString string) {
            ByteList newByteList = ByteList.create(string.toString().toLowerCase(Locale.ENGLISH));
            newByteList.setEncoding(string.getBytes().getEncoding());

            return newByteList;
        }

        @TruffleBoundary
        public static ByteList chomp(RubyString string) {
            String javaString = string.toString();
            if (javaString.endsWith("\r")) {
                String newString = javaString.substring(0, javaString.length()-1);
                ByteList byteListString = ByteList.create(newString);
                byteListString.setEncoding(string.getBytes().getEncoding());

                return byteListString;
            } else {
                ByteList byteListString = ByteList.create(javaString.trim());
                byteListString.setEncoding(string.getBytes().getEncoding());

                return byteListString;
            }

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
        public static ByteList reverse(RubyString string) {
            ByteList byteListString = ByteList.create(new StringBuilder(string.toString()).reverse().toString());
            byteListString.setEncoding(string.getBytes().getEncoding());

            return byteListString;
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
    }

    @CoreMethod(names = "_set_num_bytes", required = 1)
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

}
