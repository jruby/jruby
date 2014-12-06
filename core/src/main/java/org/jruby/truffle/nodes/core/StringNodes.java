/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.joni.Option;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.truffle.runtime.rubinius.RubiniusByteArray;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

            return getContext().makeString(a.toString() + b.toString());
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

            return new RubyString(getContext().getCoreLibrary().getStringClass(), outputBytes);
        }
    }

    @CoreMethod(names = {"==", "===", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(@SuppressWarnings("unused") RubyString a, @SuppressWarnings("unused") RubyNilClass b) {
            return false;
        }

        @Specialization
        public boolean equal(RubyString a, RubyString b) {
            return a.equals(b.toString());
        }

        @Specialization
        public boolean equal(RubyString a, Object b) {
            if (b instanceof RubyString) {
                return equal(a, (RubyString) b);
            } else {
                return false;
            }
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubyString a, RubyString b) {
            notDesignedForCompilation();

            return a.toString().compareTo(b.toString());
        }
    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class ConcatNode extends CoreMethodNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConcatNode(ConcatNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString concat(RubyString string, RubyString other) {
            string.getBytes().append(other.getBytes());
            return string;
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
                return context.makeString(StringFormatter.format(format.toString(), Arrays.asList(((RubyArray) args[0]).slowToArray())));
            } else {
                multipleArgumentsProfile.enter();
                return context.makeString(StringFormatter.format(format.toString(), Arrays.asList(args)));
            }
        }
    }

    @CoreMethod(names = {"[]", "slice"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1})
    public abstract static class GetIndexNode extends CoreMethodNode {

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        public RubyString getIndexInBounds(RubyString string, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            final int normalisedIndex = string.normaliseIndex(index);
            final ByteList bytes = string.getBytes();

            if (normalisedIndex < 0 || normalisedIndex >= bytes.length()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                return getContext().makeString(bytes.charAt(normalisedIndex));
            }
        }

        @Specialization(contains = "getIndexInBounds")
        public Object getIndex(RubyString string, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = string.normaliseIndex(index);
            final ByteList bytes = string.getBytes();

            if (normalisedIndex < 0 || normalisedIndex >= bytes.length()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return getContext().makeString(bytes.charAt(normalisedIndex));
            }
        }

        @Specialization
        public Object getIndex(RubyString string, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            final String javaString = string.toString();
            final int begin = string.normaliseIndex(range.getBegin());

            if (begin < 0 || begin >= javaString.length()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final int end = string.normaliseIndex(range.getEnd());
                final int excludingEnd = string.clampExclusiveIndex(range.doesExcludeEnd() ? end : end+1);

                return getContext().makeString(javaString.substring(begin, excludingEnd));
            }
        }

        @Specialization
        public RubyString getIndex(RubyString string, int start, int length) {
            // TODO(CS): not sure if this is right - encoding
            // TODO(CS): why does subSequence return CharSequence?
            final int begin = string.normaliseIndex(start);
            final int exclusiveEnd = string.normaliseIndex(start + length);
            return new RubyString(getContext().getCoreLibrary().getStringClass(), (ByteList) string.getBytes().subSequence(begin, exclusiveEnd - begin));
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
            return regexp.matchCommon(string.getBytes(), true);
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
                store[n] = toUnsignedInt(bytes[n]);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, bytes.length);
        }

        public static int toUnsignedInt(byte x) {
            return ((int) x) & 0xff;
        }

    }

    @CoreMethod(names = "chomp")
    public abstract static class ChompNode extends CoreMethodNode {

        public ChompNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChompNode(ChompNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chomp(RubyString string) {
            notDesignedForCompilation();

            return string.getContext().makeString(string.toString().trim());
        }
    }

    @CoreMethod(names = "chomp!")
    public abstract static class ChompBangNode extends CoreMethodNode {

        public ChompBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChompBangNode(ChompBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chompBang(RubyString string) {
            notDesignedForCompilation();

            string.set(ByteList.create(string.toString().trim()));
            return string;
        }
    }

    @CoreMethod(names = "downcase")
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

            return string.getContext().makeString(string.toString().toLowerCase());
        }
    }

    @CoreMethod(names = "downcase!")
    public abstract static class DowncaseBangNode extends CoreMethodNode {

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseBangNode(DowncaseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString downcase(RubyString string) {
            notDesignedForCompilation();

            string.set(ByteList.create(string.toString().toLowerCase()));
            return string;
        }
    }

    @CoreMethod(names = "each_line")
    public abstract static class EachLineNode extends YieldingCoreMethodNode {

        public EachLineNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachLineNode(EachLineNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray eachLine(RubyString string) {
            notDesignedForCompilation();

            final List<Object> lines = new ArrayList<>();

            String str = string.toString();
            int start = 0;

            while (start < str.length()) {
                int end = str.indexOf('\n', start);

                if (end == -1) {
                    lines.add(getContext().makeString(str.substring(start)));
                    break;
                }

                String line = str.substring(start, end+1);
                start = end+1;

                lines.add(getContext().makeString(line));
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), lines.toArray(new Object[lines.size()]));
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
            notDesignedForCompilation();

            return string.toString().isEmpty();
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

            return RubyEncoding.getEncoding(getContext(), string.getBytes().getEncoding());
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

        @Specialization
        public RubyString forceEncoding(RubyString string, RubyString encodingName) {
            notDesignedForCompilation();

            RubyEncoding encoding = RubyEncoding.getEncoding(getContext(), encodingName.toString());
            string.forceEncoding(encoding.getEncoding());

            return string;
        }

    }

    @CoreMethod(names = "gsub", required = 2)
    public abstract static class GsubNode extends RegexpNodes.EscapingNode {

        public GsubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GsubNode(GsubNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString gsub(VirtualFrame frame, RubyString string, RubyString regexpString, RubyString replacement) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), escape(frame, regexpString).getBytes(), Option.DEFAULT);
            return gsub(string, regexp, replacement);
        }

        @Specialization
        public RubyString gsub(RubyString string, RubyRegexp regexp, RubyString replacement) {
            notDesignedForCompilation();

            return regexp.gsub(string.toString(), replacement.toString());
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

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect(RubyString string) {
            notDesignedForCompilation();

            return getContext().makeString(org.jruby.RubyString.inspect19(getContext().getRuntime(), string.getBytes()).toString());
        }
    }

    @CoreMethod(names = "initialize", required = 1)
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

    @CoreMethod(names = "match", required = 1)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(RubyString string, RubyString regexpString) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getBytes(), Option.DEFAULT);
            return regexp.matchCommon(string.getBytes(), false);
        }

        @Specialization
        public Object match(RubyString string, RubyRegexp regexp) {
            return regexp.matchCommon(string.getBytes(), false);
        }
    }

    @CoreMethod(names = "rjust", required = 1, optional = 1, lowerFixnumParameters = 0)
    public abstract static class RjustNode extends CoreMethodNode {

        public RjustNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RjustNode(RjustNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString rjust(RubyString string, int length, @SuppressWarnings("unused") UndefinedPlaceholder padding) {
            notDesignedForCompilation();

            return getContext().makeString(RubyString.rjust(string.toString(), length, " "));
        }

        @Specialization
        public RubyString rjust(RubyString string, int length, RubyString padding) {
            notDesignedForCompilation();

            return getContext().makeString(RubyString.rjust(string.toString(), length, padding.toString()));
        }

    }

    @CoreMethod(names = "rstrip")
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

            return getContext().makeString(str.substring(0, last+1));
        }

    }

    @CoreMethod(names = "scan", required = 1)
    public abstract static class ScanNode extends CoreMethodNode {

        public ScanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ScanNode(ScanNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyString regexpString) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), regexpString.getBytes(), Option.DEFAULT);
            return scan(string, regexp);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyRegexp regexp) {
            notDesignedForCompilation();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), (Object[]) regexp.scan(string));
        }

    }

    @CoreMethod(names = "setbyte", required = 2)
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
            return string.getBytes().getRealSize();
        }
    }

    @CoreMethod(names = "split", required = 1)
    public abstract static class SplitNode extends CoreMethodNode {

        public SplitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SplitNode(SplitNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray split(RubyString string, RubyString sep) {
            notDesignedForCompilation();

            final String[] components = string.toString().split(Pattern.quote(sep.toString()));

            final Object[] objects = new Object[components.length];

            for (int n = 0; n < objects.length; n++) {
                objects[n] = getContext().makeString(components[n]);
            }

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), objects);
        }

        @Specialization
        public RubyArray split(RubyString string, RubyRegexp sep) {
            notDesignedForCompilation();

            return RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), (Object[]) sep.split(string.toString()));
        }
    }

    @CoreMethod(names = "start_with?", required = 1)
    public abstract static class StartWithNode extends CoreMethodNode {

        public StartWithNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StartWithNode(StartWithNode prev) {
            super(prev);
        }

        @Specialization
        public boolean endWith(RubyString string, RubyString b) {
            notDesignedForCompilation();

            return string.toString().startsWith(b.toString());
        }
    }

    @CoreMethod(names = "sub", required = 2)
    public abstract static class SubNode extends RegexpNodes.EscapingNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString sub(VirtualFrame frame, RubyString string, RubyString regexpString, RubyString replacement) {
            notDesignedForCompilation();

            final RubyRegexp regexp = new RubyRegexp(this, getContext().getCoreLibrary().getRegexpClass(), escape(frame, regexpString).getBytes(), Option.DEFAULT);
            return sub(string, regexp, replacement);
        }

        @Specialization
        public RubyString sub(RubyString string, RubyRegexp regexp, RubyString replacement) {
            notDesignedForCompilation();

            return regexp.sub(string.toString(), replacement.toString());
        }
    }

    @CoreMethod(names = "sum")
    public abstract static class SumNode extends CoreMethodNode {

        public SumNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SumNode(SumNode prev) {
            super(prev);
        }

        @Specialization
        public int sum(RubyString string) {
            notDesignedForCompilation();

            return (int) getContext().toTruffle(getContext().toJRuby(string).sum(getContext().getRuntime().getCurrentContext()));
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
            notDesignedForCompilation();

            return Double.parseDouble(string.toString());
        }
    }

    @CoreMethod(names = "to_i")
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
        }

        @Specialization
        public Object toI(RubyString string) {
            notDesignedForCompilation();

            if (string.toString().length() == 0) {
                return 0;
            }

            try {
                final int value = Integer.parseInt(string.toString());

                if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
                    return value;
                } else {
                    return bignum(value);
                }
            } catch (NumberFormatException e) {
                return bignum(new BigInteger(string.toString()));
            }
        }
    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toS(RubyString string) {
            return string;
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

            return getContext().newSymbol(string.toString());
        }
    }

    @CoreMethod(names = "reverse")
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

            return RubyString.fromJavaString(string.getLogicalClass(), new StringBuilder(string.toString()).reverse().toString());
        }
    }

    @CoreMethod(names = "reverse!")
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

            string.set(ByteList.create(new StringBuilder(string.toString()).reverse().toString()));
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

    // Rubinius API
    @CoreMethod(names = "data")
    public abstract static class DataNode extends CoreMethodNode {
        public DataNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DataNode(DataNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBasicObject data(RubyString string) {
            return new RubiniusByteArray(getContext().getCoreLibrary().getRubiniusLibrary().getByteArrayCLass(), string.getBytes().getUnsafeBytes());
        }
    }
}
