/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.regexp;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.joni.Region;
import org.joni.exception.ValueException;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.RubiniusOnly;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.array.ArrayUtils;
import org.jruby.truffle.core.cast.TaintResultNode;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.Arrays;

@CoreClass(name = "MatchData")
public abstract class MatchDataNodes {

    public static Object[] getCaptures(DynamicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        // There should always be at least one value because the entire matched string must be in the values array.
        // Thus, there is no risk of an ArrayIndexOutOfBoundsException here.
        return ArrayUtils.extractRange(Layouts.MATCH_DATA.getValues(matchData), 1, Layouts.MATCH_DATA.getValues(matchData).length);
    }

    public static Object begin(RubyContext context, DynamicObject matchData, int index) {
        // Taken from org.jruby.RubyMatchData
        int b = Layouts.MATCH_DATA.getRegion(matchData).beg[index];

        if (b < 0) {
            return context.getCoreLibrary().getNilObject();
        }

        if (!StringGuards.isSingleByteOptimizable(Layouts.MATCH_DATA.getSource(matchData))) {
            b = getCharOffsets(matchData).beg[index];
        }

        return b;
    }

    public static Object end(RubyContext context, DynamicObject matchData, int index) {
        // Taken from org.jruby.RubyMatchData
        int e = Layouts.MATCH_DATA.getRegion(matchData).end[index];

        if (e < 0) {
            return context.getCoreLibrary().getNilObject();
        }

        if (!StringGuards.isSingleByteOptimizable(Layouts.MATCH_DATA.getSource(matchData))) {
            e = getCharOffsets(matchData).end[index];
        }

        return e;
    }

    private static void updatePairs(ByteList source, Encoding encoding, Pair[] pairs) {
        // Taken from org.jruby.RubyMatchData
        Arrays.sort(pairs);

        int length = pairs.length;
        byte[]bytes = source.getUnsafeBytes();
        int p = source.getBegin();
        int s = p;
        int c = 0;

        for (int i = 0; i < length; i++) {
            int q = s + pairs[i].bytePos;
            c += StringSupport.strLength(encoding, bytes, p, q);
            pairs[i].charPos = c;
            p = q;
        }
    }

    private static Region getCharOffsetsManyRegs(DynamicObject matchData, ByteList source, Encoding encoding) {
        // Taken from org.jruby.RubyMatchData
        final Region regs = Layouts.MATCH_DATA.getRegion(matchData);
        int numRegs = regs.numRegs;

        final Region charOffsets = new Region(numRegs);

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                charOffsets.beg[i] = regs.beg[i];
                charOffsets.end[i] = regs.end[i];
            }
            return charOffsets;
        }

        Pair[] pairs = new Pair[numRegs * 2];
        for (int i = 0; i < pairs.length; i++) {
            pairs[i] = new Pair();
        }

        int numPos = 0;
        for (int i = 0; i < numRegs; i++) {
            if (regs.beg[i] < 0) {
                continue;
            }
            pairs[numPos++].bytePos = regs.beg[i];
            pairs[numPos++].bytePos = regs.end[i];
        }

        updatePairs(source, encoding, pairs);

        Pair key = new Pair();
        for (int i = 0; i < regs.numRegs; i++) {
            if (regs.beg[i] < 0) {
                charOffsets.beg[i] = charOffsets.end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            charOffsets.beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            charOffsets.end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        }

        return charOffsets;
    }

    public static Region getCharOffsets(DynamicObject matchData) {
        // Taken from org.jruby.RubyMatchData
        Region charOffsets = Layouts.MATCH_DATA.getCharOffsets(matchData);
        if (charOffsets != null) {
            return charOffsets;
        } else {
            return createCharOffsets(matchData);
        }
    }

    @TruffleBoundary
    private static Region createCharOffsets(DynamicObject matchData) {
        final ByteList source = StringOperations.getByteListReadOnly(Layouts.MATCH_DATA.getSource(matchData));
        final Encoding enc = source.getEncoding();
        final Region charOffsets = getCharOffsetsManyRegs(matchData, source, enc);
        Layouts.MATCH_DATA.setCharOffsets(matchData, charOffsets);
        return charOffsets;
    }

    @CoreMethod(names = "[]", required = 1, optional = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public Object getIndex(DynamicObject matchData, int index, NotProvided length) {
            final Object[] values = Layouts.MATCH_DATA.getValues(matchData);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, index);

            if ((normalizedIndex < 0) || (normalizedIndex >= values.length)) {
                return nil();
            } else {
                return values[normalizedIndex];
            }
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public Object getIndex(DynamicObject matchData, int index, int length) {
            // TODO BJF 15-May-2015 Need to handle negative indexes and lengths and out of bounds
            final Object[] values = Layouts.MATCH_DATA.getValues(matchData);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, index);
            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, length);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubySymbol(index)")
        public Object getIndexSymbol(DynamicObject matchData, DynamicObject index, NotProvided length) {
            try {
                final Rope value = Layouts.SYMBOL.getRope(index);
                final int i = Layouts.REGEXP.getRegex(Layouts.MATCH_DATA.getRegexp(matchData)).nameToBackrefNumber(value.getBytes(), value.begin(), value.begin() + value.byteLength(), Layouts.MATCH_DATA.getRegion(matchData));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            } catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                    coreLibrary().indexError(String.format("undefined group name reference: %s", Layouts.SYMBOL.getString(index)), this));
            }
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization(guards = "isRubyString(index)")
        public Object getIndexString(DynamicObject matchData, DynamicObject index, NotProvided length) {
            try {
                final Rope value = StringOperations.rope(index);
                final int i = Layouts.REGEXP.getRegex(Layouts.MATCH_DATA.getRegexp(matchData)).nameToBackrefNumber(value.getBytes(), value.begin(), value.begin() + value.byteLength(), Layouts.MATCH_DATA.getRegion(matchData));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            }
            catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        coreLibrary().indexError(String.format("undefined group name reference: %s", index.toString()), this));
            }
        }

        @Specialization(guards = {"!isRubySymbol(index)", "!isRubyString(index)", "!isIntegerFixnumRange(index)"})
        public Object getIndex(VirtualFrame frame, DynamicObject matchData, Object index, NotProvided length) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return getIndex(matchData, toIntNode.doInt(frame, index), NotProvided.INSTANCE);
        }

        @TruffleBoundary
        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object getIndex(DynamicObject matchData, DynamicObject range, NotProvided len) {
            final Object[] values = Layouts.MATCH_DATA.getValues(matchData);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range));
            final int end = ArrayOperations.normalizeIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range));
            final int exclusiveEnd = ArrayOperations.clampExclusiveIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end : end + 1);
            final int length = exclusiveEnd - normalizedIndex;

            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, length);
        }

    }

    @CoreMethod(names = "begin", required = 1, lowerFixnumParameters = 1)
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "inBounds(matchData, index)")
        public Object begin(DynamicObject matchData, int index) {
            return MatchDataNodes.begin(getContext(), matchData, index);
        }

        @TruffleBoundary
        @Specialization(guards = "!inBounds(matchData, index)")
        public Object beginError(DynamicObject matchData, int index) {
            throw new RaiseException(coreLibrary().indexError(String.format("index %d out of matches", index), this));
        }

        protected boolean inBounds(DynamicObject matchData, int index) {
            return index >= 0 && index < Layouts.MATCH_DATA.getRegion(matchData).numRegs;
        }
    }


    @CoreMethod(names = "captures")
    public abstract static class CapturesNode extends CoreMethodArrayArgumentsNode {

        public CapturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public DynamicObject toA(DynamicObject matchData) {
            Object[] objects = getCaptures(matchData);
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
        }
    }

    @CoreMethod(names = "end", required = 1, lowerFixnumParameters = 1)
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "inBounds(matchData, index)")
        public Object end(DynamicObject matchData, int index) {
            return MatchDataNodes.end(getContext(), matchData, index);
        }

        @TruffleBoundary
        @Specialization(guards = "!inBounds(matchData, index)")
        public Object endError(DynamicObject matchData, int index) {
            throw new RaiseException(coreLibrary().indexError(String.format("index %d out of matches", index), this));
        }

        protected boolean inBounds(DynamicObject matchData, int index) {
            return index >= 0 && index < Layouts.MATCH_DATA.getRegion(matchData).numRegs;
        }
    }

    @RubiniusOnly
    @CoreMethod(names = "full")
    public abstract static class FullNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode newTupleNode;

        public FullNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object full(VirtualFrame frame, DynamicObject matchData) {
            if (Layouts.MATCH_DATA.getFullTuple(matchData) != null) {
                return Layouts.MATCH_DATA.getFullTuple(matchData);
            }

            if (newTupleNode == null) {
                CompilerDirectives.transferToInterpreter();
                newTupleNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object fullTuple = newTupleNode.call(frame,
                    coreLibrary().getTupleClass(),
                    "create",
                    null, Layouts.MATCH_DATA.getBegin(matchData), Layouts.MATCH_DATA.getEnd(matchData));

            Layouts.MATCH_DATA.setFullTuple(matchData, fullTuple);

            return fullTuple;
        }
    }

    @CoreMethod(names = { "length", "size" })
    public abstract static class LengthNode extends CoreMethodArrayArgumentsNode {

        public LengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int length(DynamicObject matchData) {
            return Layouts.MATCH_DATA.getValues(matchData).length;
        }

    }

    @CoreMethod(names = "pre_match")
    public abstract static class PreMatchNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintResultNode taintResultNode;

        public PreMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(getContext(), getSourceSection());
        }

        @Specialization
        public Object preMatch(DynamicObject matchData) {
            return taintResultNode.maybeTaint(Layouts.MATCH_DATA.getSource(matchData), Layouts.MATCH_DATA.getPre(matchData));
        }

    }

    @CoreMethod(names = "post_match")
    public abstract static class PostMatchNode extends CoreMethodArrayArgumentsNode {

        @Child private TaintResultNode taintResultNode;

        public PostMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            taintResultNode = new TaintResultNode(getContext(), getSourceSection());
        }

        @Specialization
        public Object postMatch(DynamicObject matchData) {
            return taintResultNode.maybeTaint(Layouts.MATCH_DATA.getSource(matchData), Layouts.MATCH_DATA.getPost(matchData));
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toA(DynamicObject matchData) {
            Object[] objects = ArrayUtils.copy(Layouts.MATCH_DATA.getValues(matchData));
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), objects, objects.length);
        }
    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toS(DynamicObject matchData) {
            final Rope rope = StringOperations.rope(Layouts.MATCH_DATA.getGlobal(matchData));
            return createString(rope);
        }
    }

    @CoreMethod(names = "regexp")
    public abstract static class RegexpNode extends CoreMethodArrayArgumentsNode {

        public RegexpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject regexp(DynamicObject matchData) {
            return Layouts.MATCH_DATA.getRegexp(matchData);
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // MatchData can be allocated in MRI but it does not seem to be any useful
        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @RubiniusOnly
    @NodeChild(value = "self")
    public abstract static class RubiniusSourceNode extends RubyNode {

        public RubiniusSourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject rubiniusSource(DynamicObject matchData) {
            return Layouts.MATCH_DATA.getSource(matchData);
        }
    }

    public static final class Pair implements Comparable<Pair> {
        int bytePos, charPos;

        @Override
        public int compareTo(Pair pair) {
            return bytePos - pair.bytePos;
        }
    }

}
