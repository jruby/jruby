/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.joni.Option;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.ArrayUtilities;
import org.jruby.truffle.runtime.core.array.IntegerArrayStore;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.core.range.FixnumRange;
import org.jruby.util.ByteList;
import org.jruby.util.Pack;

import java.util.Arrays;
import java.util.regex.Pattern;

@CoreClass(name = "String")
public abstract class StringNodes {

    @CoreMethod(names = "+", minArgs = 1, maxArgs = 1)
    public abstract static class AddNode extends CoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString add(RubyString a, RubyString b) {
            return new RubyString(a.getRubyClass().getContext().getCoreLibrary().getStringClass(), a.toString() + b.toString());
        }
    }

    @CoreMethod(names = "*", minArgs = 1, maxArgs = 1)
    public abstract static class MulNode extends CoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @CompilerDirectives.SlowPath
        @Specialization
        public RubyString add(RubyString string, int times) {
            final StringBuilder builder = new StringBuilder();

            for (int n = 0; n < times; n++) {
                builder.append(string.toString());
            }

            return getContext().makeString(builder.toString());
        }
    }

    @CoreMethod(names = {"==", "==="}, minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(@SuppressWarnings("unused") RubyString a, @SuppressWarnings("unused") NilPlaceholder b) {
            return false;
        }

        @Specialization
        public boolean equal(RubyString a, RubyString b) {
            return a.equals(b.toString());
        }

        @Specialization
        public boolean equal(RubyString a, RubySymbol b) {
            return equal(a, b.toRubyString());
        }
    }

    @CoreMethod(names = "!=", minArgs = 1, maxArgs = 1)
    public abstract static class NotEqualNode extends CoreMethodNode {

        public NotEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NotEqualNode(NotEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean equal(@SuppressWarnings("unused") RubyString a, @SuppressWarnings("unused") NilPlaceholder b) {
            return true;
        }

        @Specialization
        public boolean notEqual(RubyString a, RubyString b) {
            return !a.toString().equals(b.toString());
        }

    }

    @CoreMethod(names = "<=>", minArgs = 1, maxArgs = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(RubyString a, RubyString b) {
            return a.toString().compareTo(b.toString());
        }
    }

    @CoreMethod(names = "<<", minArgs = 1, maxArgs = 1)
    public abstract static class ConcatNode extends CoreMethodNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConcatNode(ConcatNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString concat(RubyString string, RubyString other) {
            string.concat(other);
            return string;
        }
    }

    @CoreMethod(names = "%", minArgs = 1, maxArgs = 1, isSplatted = true)
    public abstract static class FormatNode extends CoreMethodNode {

        public FormatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FormatNode(FormatNode prev) {
            super(prev);
        }

        private final BranchProfile singleArrayProfile = new BranchProfile();
        private final BranchProfile multipleArgumentsProfile = new BranchProfile();

        @Specialization
        public RubyString format(RubyString format, Object[] args) {
            return formatSlow(format, args);
        }

        @CompilerDirectives.SlowPath
        private RubyString formatSlow(RubyString format, Object[] args) {
            final RubyContext context = getContext();

            if (args.length == 1 && args[0] instanceof RubyArray) {
                singleArrayProfile.enter();
                return context.makeString(StringFormatter.format(format.toString(), ((RubyArray) args[0]).asList()));
            } else {
                multipleArgumentsProfile.enter();
                return context.makeString(StringFormatter.format(format.toString(), Arrays.asList(args)));
            }
        }
    }

    @CoreMethod(names = "[]", minArgs = 1, maxArgs = 2)
    public abstract static class GetIndexNode extends CoreMethodNode {


        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        @CompilerDirectives.SlowPath
        @Specialization(order = 1)
        public Object getIndex(RubyString string, int index, UndefinedPlaceholder undefined) {
            final String javaString = string.toString();
            final int normalisedIndex = ArrayUtilities.normaliseIndex(javaString.length(), index);
            return getContext().makeString(javaString.charAt(normalisedIndex));
        }

        @CompilerDirectives.SlowPath
        @Specialization(order = 2)
        public Object getIndex(RubyString string, FixnumRange range, UndefinedPlaceholder undefined) {
            final String javaString = string.toString();

            final int stringLength = javaString.length();

            if (range.doesExcludeEnd()) {
                final int begin = ArrayUtilities.normaliseIndex(stringLength, range.getBegin());
                final int exclusiveEnd = ArrayUtilities.normaliseExclusiveIndex(stringLength, range.getExclusiveEnd());
                return getContext().makeString(javaString.substring(begin, exclusiveEnd));
            } else {
                final int begin = ArrayUtilities.normaliseIndex(stringLength, range.getBegin());
                final int inclusiveEnd = ArrayUtilities.normaliseIndex(stringLength, range.getInclusiveEnd());
                return getContext().makeString(javaString.substring(begin, inclusiveEnd + 1));
            }
        }

        @CompilerDirectives.SlowPath
        @Specialization(order = 3)
        public Object getIndex(RubyString string, int start, int length) {
            final String javaString = string.toString();

            if (length > javaString.length() - start) {
                length = javaString.length() - start;
            }

            if (start > javaString.length()) {
                return NilPlaceholder.INSTANCE;
            }

            return getContext().makeString(javaString.substring(start, start + length));
        }
    }

    @CoreMethod(names = "=~", minArgs = 1, maxArgs = 1)
    public abstract static class MatchOperatorNode extends CoreMethodNode {

        public MatchOperatorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchOperatorNode(MatchOperatorNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyRegexp regexp) {
            return regexp.matchOperator(frame, string.toString());
        }
    }

    @CoreMethod(names = "bytes", maxArgs = 0)
    public abstract static class BytesNode extends CoreMethodNode {

        public BytesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BytesNode(BytesNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray chomp(RubyString string) {
            final byte[] bytes = string.getBytes().bytes();

            final int[] ints = new int[bytes.length];

            for (int n = 0; n < ints.length; n++) {
                ints[n] = RubyFixnum.toUnsignedInt(bytes[n]);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new IntegerArrayStore(ints));
        }
    }

    @CoreMethod(names = "chomp", maxArgs = 0)
    public abstract static class ChompNode extends CoreMethodNode {

        public ChompNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChompNode(ChompNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chomp(RubyString string) {
            return string.getRubyClass().getContext().makeString(string.toString().trim());
        }
    }

    @CoreMethod(names = "chomp!", maxArgs = 0)
    public abstract static class ChompBangNode extends CoreMethodNode {

        public ChompBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChompBangNode(ChompBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chompBang(RubyString string) {
            string.replace(string.toString().trim());
            return string;
        }
    }

    @CoreMethod(names = "downcase", maxArgs = 0)
    public abstract static class DowncaseNode extends CoreMethodNode {

        public DowncaseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseNode(DowncaseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString downcase(RubyString string) {
            return string.getRubyClass().getContext().makeString(string.toString().toLowerCase());
        }
    }

    @CoreMethod(names = "downcase!", maxArgs = 0)
    public abstract static class DowncaseBangNode extends CoreMethodNode {

        public DowncaseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DowncaseBangNode(DowncaseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString downcase(RubyString string) {
            string.replace(string.toString().toLowerCase());
            return string;
        }
    }

    @CoreMethod(names = "empty?", maxArgs = 0)
    public abstract static class EmptyNode extends CoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean empty(RubyString string) {
            return string.toString().isEmpty();
        }
    }

    @CoreMethod(names = "end_with?", minArgs = 1, maxArgs = 1)
    public abstract static class EndWithNode extends CoreMethodNode {

        public EndWithNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EndWithNode(EndWithNode prev) {
            super(prev);
        }

        @Specialization
        public boolean endWith(RubyString string, RubyString b) {
            return string.toString().endsWith(b.toString());
        }
    }

    @CoreMethod(names = "force_encoding", minArgs = 1, maxArgs = 1)
    public abstract static class ForceEncodingNode extends CoreMethodNode {

        public ForceEncodingNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ForceEncodingNode(ForceEncodingNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString forceEncoding(RubyString string, RubyString encodingName) {
            RubyEncoding encoding = RubyEncoding.findEncodingByName(encodingName);
            string.forceEncoding(encoding.getRubyEncoding().getEncoding());

            return string;
        }

    }

    @CoreMethod(names = "gsub", minArgs = 2, maxArgs = 2)
    public abstract static class GsubNode extends CoreMethodNode {

        public GsubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GsubNode(GsubNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString gsub(RubyString string, RubyString regexpString, RubyString replacement) {
            final RubyRegexp regexp = new RubyRegexp(getContext().getCoreLibrary().getRegexpClass(), regexpString.toString(), Option.DEFAULT);
            return gsub(string, regexp, replacement);
        }

        @Specialization
        public RubyString gsub(RubyString string, RubyRegexp regexp, RubyString replacement) {
            return regexp.gsub(string.toString(), replacement.toString());
        }
    }

    @CoreMethod(names = "getbyte", minArgs = 1, maxArgs = 1)
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

    @CoreMethod(names = "inspect", maxArgs = 0)
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect(RubyString string) {
            return getContext().makeString("\"" + string.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        }
    }

    @CoreMethod(names = "initialize", minArgs = 1, maxArgs = 1)
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
            self.setStringValue(from);
            return self;
        }
    }

    @CoreMethod(names = "ljust", minArgs = 1, maxArgs = 2)
    public abstract static class LjustNode extends CoreMethodNode {

        public LjustNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LjustNode(LjustNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString ljust(RubyString string, int length, @SuppressWarnings("unused") UndefinedPlaceholder padding) {
            return getContext().makeString(RubyString.ljust(string.toString(), length, " "));
        }

        @Specialization
        public RubyString ljust(RubyString string, int length, RubyString padding) {
            return getContext().makeString(RubyString.ljust(string.toString(), length, padding.toString()));
        }

    }

    @CoreMethod(names = "setbyte", minArgs = 2, maxArgs = 2)
    public abstract static class SetByteNode extends CoreMethodNode {

        public SetByteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SetByteNode(SetByteNode prev) {
            super(prev);
        }

        @Specialization
        public Object setByte(RubyString string, int index, Object value) {
            throw new UnsupportedOperationException("getbyte not implemented");
        }
    }

    @CoreMethod(names = "size", maxArgs = 0)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubyString string) {
            return string.toString().length();
        }
    }

    @CoreMethod(names = "slice", minArgs = 2, maxArgs = 2)
    public abstract static class SliceNode extends CoreMethodNode {

        public SliceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SliceNode(SliceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString slice(RubyString string, int start, int length) {
            return getContext().makeString(string.toString().substring(start, start + length));
        }
    }

    @CoreMethod(names = "match", minArgs = 1, maxArgs = 1)
    public abstract static class MatchNode extends CoreMethodNode {

        public MatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MatchNode(MatchNode prev) {
            super(prev);
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyString regexpString) {
            final RubyRegexp regexp = new RubyRegexp(getContext().getCoreLibrary().getRegexpClass(), regexpString.toString(), Option.DEFAULT);
            return regexp.match(frame.getCaller().unpack(), string.toString());
        }

        @Specialization
        public Object match(VirtualFrame frame, RubyString string, RubyRegexp regexp) {
            return regexp.match(frame.getCaller().unpack(), string.toString());
        }
    }

    @CoreMethod(names = "rjust", minArgs = 1, maxArgs = 2)
    public abstract static class RjustNode extends CoreMethodNode {

        public RjustNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RjustNode(RjustNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString rjust(RubyString string, int length, @SuppressWarnings("unused") UndefinedPlaceholder padding) {
            return getContext().makeString(RubyString.rjust(string.toString(), length, " "));
        }

        @Specialization
        public RubyString rjust(RubyString string, int length, RubyString padding) {
            return getContext().makeString(RubyString.rjust(string.toString(), length, padding.toString()));
        }

    }

    @CoreMethod(names = "scan", minArgs = 1, maxArgs = 1)
    public abstract static class ScanNode extends CoreMethodNode {

        public ScanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ScanNode(ScanNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyString regexpString) {
            final RubyRegexp regexp = new RubyRegexp(getContext().getCoreLibrary().getRegexpClass(), regexpString.toString(), Option.DEFAULT);
            return scan(string, regexp);
        }

        @Specialization
        public RubyArray scan(RubyString string, RubyRegexp regexp) {
            return RubyArray.specializedFromObjects(getContext().getCoreLibrary().getArrayClass(), regexp.scan(string));
        }

    }

    @CoreMethod(names = "split", minArgs = 1, maxArgs = 1)
    public abstract static class SplitNode extends CoreMethodNode {

        public SplitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SplitNode(SplitNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray split(RubyString string, RubyString sep) {
            final String[] components = string.toString().split(Pattern.quote(sep.toString()));

            final Object[] objects = new Object[components.length];

            for (int n = 0; n < objects.length; n++) {
                objects[n] = getContext().makeString(components[n]);
            }

            return RubyArray.specializedFromObjects(getContext().getCoreLibrary().getArrayClass(), objects);
        }

        @Specialization
        public RubyArray split(RubyString string, RubyRegexp sep) {
            return RubyArray.specializedFromObjects(getContext().getCoreLibrary().getArrayClass(), sep.split(string.toString()));
        }
    }

    @CoreMethod(names = "start_with?", minArgs = 1, maxArgs = 1)
    public abstract static class StartWithNode extends CoreMethodNode {

        public StartWithNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StartWithNode(StartWithNode prev) {
            super(prev);
        }

        @Specialization
        public boolean endWith(RubyString string, RubyString b) {
            return string.toString().startsWith(b.toString());
        }
    }

    @CoreMethod(names = "to_f", maxArgs = 0)
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(RubyString string) {
            return Double.parseDouble(string.toString());
        }
    }

    @CoreMethod(names = "to_i", maxArgs = 0)
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
        }

        @Specialization
        public Object toI(RubyString string) {
            return string.toInteger();
        }
    }

    @CoreMethod(names = "to_s", maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString toF(RubyString string) {
            return string;
        }
    }

    @CoreMethod(names = {"to_sym", "intern"}, maxArgs = 0)
    public abstract static class ToSymNode extends CoreMethodNode {

        public ToSymNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSymNode(ToSymNode prev) {
            super(prev);
        }

        @Specialization
        public RubySymbol toSym(RubyString string) {
            return getContext().newSymbol(string.toString());
        }
    }

    @CoreMethod(names = "reverse", maxArgs = 0)
    public abstract static class ReverseNode extends CoreMethodNode {

        public ReverseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReverseNode(ReverseNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString reverse(RubyString string) {
            return new RubyString(getContext().getCoreLibrary().getStringClass(), string.getReverseString());
        }
    }

    @CoreMethod(names = "reverse!", maxArgs = 0)
    public abstract static class ReverseBangNode extends CoreMethodNode {

        public ReverseBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReverseBangNode(ReverseBangNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString reverse(RubyString string) {
            string.reverseStringValue();
            return string;
        }
    }

    @CoreMethod(names = "unpack", minArgs = 1, maxArgs = 1)
    public abstract static class UnpackNode extends ArrayCoreMethodNode {

        public UnpackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnpackNode(UnpackNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray unpack(RubyString string, RubyString format) {
            final org.jruby.RubyArray jrubyArray = Pack.unpack(getContext().getRuntime(), string.getBytes(), format.getBytes());
            return RubyArray.specializedFromObjects(getContext().getCoreLibrary().getArrayClass(), jrubyArray.toArray());
        }

    }
}
