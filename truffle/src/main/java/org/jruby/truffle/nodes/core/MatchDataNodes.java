/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.Encoding;
import org.joni.Region;
import org.joni.exception.ValueException;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.TaintResultNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyIntegerFixnumRange;
import org.jruby.truffle.runtime.core.RubyMatchData;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
import org.jruby.util.StringSupport;

import java.util.Arrays;

@CoreClass(name = "MatchData")
public abstract class MatchDataNodes {

    public static RubyBasicObject createRubyMatchData(RubyClass rubyClass, RubyBasicObject source, RubyBasicObject regexp, Region region, Object[] values, RubyBasicObject pre, RubyBasicObject post, RubyBasicObject global, int begin, int end) {
        return new RubyMatchData(rubyClass, source, regexp, region, values, pre, post, global, begin, end);
    }

    public static Object[] getValues(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return Arrays.copyOf(((RubyMatchData) matchData).fields.values, ((RubyMatchData) matchData).fields.values.length);
    }

    public static Object[] getCaptures(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        // There should always be at least one value because the entire matched string must be in the values array.
        // Thus, there is no risk of an ArrayIndexOutOfBoundsException here.
        return ArrayUtils.extractRange(((RubyMatchData) matchData).fields.values, 1, ((RubyMatchData) matchData).fields.values.length);
    }

    public static Object begin(RubyBasicObject matchData, int index) {
        assert RubyGuards.isRubyMatchData(matchData);
        final int b = (((RubyMatchData) matchData).fields.region == null) ? ((RubyMatchData) matchData).fields.begin : ((RubyMatchData) matchData).fields.region.beg[index];

        if (b < 0) {
            return matchData.getContext().getCoreLibrary().getNilObject();
        }

        updateCharOffset(matchData);

        return ((RubyMatchData) matchData).fields.charOffsets.beg[index];
    }

    public static Object end(RubyBasicObject matchData, int index) {
        assert RubyGuards.isRubyMatchData(matchData);
        int e = (((RubyMatchData) matchData).fields.region == null) ? ((RubyMatchData) matchData).fields.end : ((RubyMatchData) matchData).fields.region.end[index];

        if (e < 0) {
            return matchData.getContext().getCoreLibrary().getNilObject();
        }

        final CodeRangeable sourceWrapped = StringNodes.getCodeRangeable(((RubyMatchData) matchData).fields.source);
        if (!StringSupport.isSingleByteOptimizable(sourceWrapped, sourceWrapped.getByteList().getEncoding())) {
            updateCharOffset(matchData);
            e = ((RubyMatchData) matchData).fields.charOffsets.end[index];
        }

        return e;
    }

    public static int getNumberOfRegions(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.region.numRegs;
    }

    public static int getBackrefNumber(RubyBasicObject matchData, ByteList value) {
        assert RubyGuards.isRubyMatchData(matchData);
        return RegexpNodes.getRegex(((RubyMatchData) matchData).fields.regexp).nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), ((RubyMatchData) matchData).fields.region);
    }

    public static RubyBasicObject getPre(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.pre;
    }

    public static RubyBasicObject getPost(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.post;
    }

    public static RubyBasicObject getGlobal(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.global;
    }

    public static Region getRegion(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.region;
    }

    public static RubyBasicObject getSource(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.source;
    }

    public static RubyBasicObject getRegexp(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.regexp;
    }

    public static Object getFullTuple(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.fullTuple;
    }

    public static void setFullTuple(RubyBasicObject matchData, Object fullTuple) {
        assert RubyGuards.isRubyMatchData(matchData);
        ((RubyMatchData) matchData).fields.fullTuple = fullTuple;
    }

    public static int getFullBegin(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.begin;
    }

    public static int getFullEnd(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        return ((RubyMatchData) matchData).fields.end;
    }

    public static void updatePairs(ByteList value, Encoding encoding, Pair[] pairs) {
        Arrays.sort(pairs);

        int length = pairs.length;
        byte[]bytes = value.getUnsafeBytes();
        int p = value.getBegin();
        int s = p;
        int c = 0;

        for (int i = 0; i < length; i++) {
            int q = s + pairs[i].bytePos;
            c += StringSupport.strLength(encoding, bytes, p, q);
            pairs[i].charPos = c;
            p = q;
        }
    }

    public static void updateCharOffsetOnlyOneReg(RubyBasicObject matchData, ByteList value, Encoding encoding) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (((RubyMatchData) matchData).fields.charOffsetUpdated) return;

        if (((RubyMatchData) matchData).fields.charOffsets == null || ((RubyMatchData) matchData).fields.charOffsets.numRegs < 1)
            ((RubyMatchData) matchData).fields.charOffsets = new Region(1);

        if (encoding.maxLength() == 1) {
            ((RubyMatchData) matchData).fields.charOffsets.beg[0] = ((RubyMatchData) matchData).fields.begin;
            ((RubyMatchData) matchData).fields.charOffsets.end[0] = ((RubyMatchData) matchData).fields.end;
            ((RubyMatchData) matchData).fields.charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[2];
        if (((RubyMatchData) matchData).fields.begin >= 0) {
            pairs[0] = new Pair();
            pairs[0].bytePos = ((RubyMatchData) matchData).fields.begin;
            pairs[1] = new Pair();
            pairs[1].bytePos = ((RubyMatchData) matchData).fields.end;
        }

        updatePairs(value, encoding, pairs);

        if (((RubyMatchData) matchData).fields.begin < 0) {
            ((RubyMatchData) matchData).fields.charOffsets.beg[0] = ((RubyMatchData) matchData).fields.charOffsets.end[0] = -1;
            return;
        }
        Pair key = new Pair();
        key.bytePos = ((RubyMatchData) matchData).fields.begin;
        ((RubyMatchData) matchData).fields.charOffsets.beg[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        key.bytePos = ((RubyMatchData) matchData).fields.end;
        ((RubyMatchData) matchData).fields.charOffsets.end[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;

        ((RubyMatchData) matchData).fields.charOffsetUpdated = true;
    }

    public static void updateCharOffsetManyRegs(RubyBasicObject matchData, ByteList value, Encoding encoding) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (((RubyMatchData) matchData).fields.charOffsetUpdated) return;

        final Region regs = ((RubyMatchData) matchData).fields.region;
        int numRegs = regs.numRegs;

        if (((RubyMatchData) matchData).fields.charOffsets == null || ((RubyMatchData) matchData).fields.charOffsets.numRegs < numRegs)
            ((RubyMatchData) matchData).fields.charOffsets = new Region(numRegs);

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                ((RubyMatchData) matchData).fields.charOffsets.beg[i] = regs.beg[i];
                ((RubyMatchData) matchData).fields.charOffsets.end[i] = regs.end[i];
            }
            ((RubyMatchData) matchData).fields.charOffsetUpdated = true;
            return;
        }

        Pair[] pairs = new Pair[numRegs * 2];
        for (int i = 0; i < pairs.length; i++) pairs[i] = new Pair();

        int numPos = 0;
        for (int i = 0; i < numRegs; i++) {
            if (regs.beg[i] < 0) continue;
            pairs[numPos++].bytePos = regs.beg[i];
            pairs[numPos++].bytePos = regs.end[i];
        }

        updatePairs(value, encoding, pairs);

        Pair key = new Pair();
        for (int i = 0; i < regs.numRegs; i++) {
            if (regs.beg[i] < 0) {
                ((RubyMatchData) matchData).fields.charOffsets.beg[i] = ((RubyMatchData) matchData).fields.charOffsets.end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            ((RubyMatchData) matchData).fields.charOffsets.beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            ((RubyMatchData) matchData).fields.charOffsets.end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        }

        ((RubyMatchData) matchData).fields.charOffsetUpdated = true;
    }

    public static void updateCharOffset(RubyBasicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (((RubyMatchData) matchData).fields.charOffsetUpdated) return;

        ByteList value = StringNodes.getByteList(((RubyMatchData) matchData).fields.source);
        Encoding enc = value.getEncoding();

        if (((RubyMatchData) matchData).fields.region == null) {
            updateCharOffsetOnlyOneReg(matchData, value, enc);
        } else {
            updateCharOffsetManyRegs(matchData, value, enc);
        }

        ((RubyMatchData) matchData).fields.charOffsetUpdated = true;
    }

    @CoreMethod(names = "[]", required = 1, optional = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getIndex(RubyBasicObject matchData, int index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            final Object[] values = getValues(matchData);
            final int normalizedIndex = ArrayNodes.normalizeIndex(values.length, index);

            if ((normalizedIndex < 0) || (normalizedIndex >= values.length)) {
                return nil();
            } else {
                return values[normalizedIndex];
            }
        }

        @Specialization
        public Object getIndex(RubyBasicObject matchData, int index, int length) {
            CompilerDirectives.transferToInterpreter();
            // TODO BJF 15-May-2015 Need to handle negative indexes and lengths and out of bounds
            final Object[] values = getValues(matchData);
            final int normalizedIndex = ArrayNodes.normalizeIndex(values.length, index);
            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return createArray(store, length);
        }

        @Specialization(guards = "isRubySymbol(index)")
        public Object getIndexSymbol(RubyBasicObject matchData, RubyBasicObject index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                final int i = getBackrefNumber(matchData, SymbolNodes.getByteList(index));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            } catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                    getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", SymbolNodes.getString(index)), this));
            }
        }

        @Specialization(guards = "isRubyString(index)")
        public Object getIndexString(RubyBasicObject matchData, RubyBasicObject index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                final int i = getBackrefNumber(matchData, StringNodes.getByteList(index));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            }
            catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", index.toString()), this));
            }
        }

        @Specialization(guards = {"!isRubySymbol(index)", "!isRubyString(index)", "!isIntegerFixnumRange(index)"})
        public Object getIndex(VirtualFrame frame, RubyBasicObject matchData, Object index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return getIndex(matchData, toIntNode.doInt(frame, index), NotProvided.INSTANCE);
        }

        @Specialization(guards = {"!isRubySymbol(range)", "!isRubyString(range)"})
        public Object getIndex(VirtualFrame frame, RubyBasicObject matchData, RubyIntegerFixnumRange range, NotProvided len) {
            final Object[] values = getValues(matchData);
            final int normalizedIndex = ArrayNodes.normalizeIndex(values.length, range.getBegin());
            final int end = ArrayNodes.normalizeIndex(values.length, range.getEnd());
            final int exclusiveEnd = ArrayNodes.clampExclusiveIndex(values.length, range.doesExcludeEnd() ? end : end + 1);
            final int length = exclusiveEnd - normalizedIndex;

            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return createArray(store, length);
        }

    }

    @CoreMethod(names = "begin", required = 1, lowerFixnumParameters = 1)
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile badIndexProfile = ConditionProfile.createBinaryProfile();

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object begin(RubyBasicObject matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= getNumberOfRegions(matchData)))) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("index %d out of matches", index), this));

            } else {
                return MatchDataNodes.begin(matchData, index);
            }
        }
    }


    @CoreMethod(names = "captures")
    public abstract static class CapturesNode extends CoreMethodArrayArgumentsNode {

        public CapturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toA(RubyBasicObject matchData) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), getCaptures(matchData));
        }
    }

    @CoreMethod(names = "end", required = 1, lowerFixnumParameters = 1)
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile badIndexProfile = ConditionProfile.createBinaryProfile();

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object end(RubyBasicObject matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= getNumberOfRegions(matchData)))) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("index %d out of matches", index), this));

            } else {
                return MatchDataNodes.end(matchData, index);
            }
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
        public Object full(VirtualFrame frame, RubyBasicObject matchData) {
            if (getFullTuple(matchData) != null) {
                return getFullTuple(matchData);
            }

            if (newTupleNode == null) {
                CompilerDirectives.transferToInterpreter();
                newTupleNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object fullTuple = newTupleNode.call(frame,
                    getContext().getCoreLibrary().getTupleClass(),
                    "create",
                    null, getFullBegin(matchData), getFullEnd(matchData));

            setFullTuple(matchData, fullTuple);

            return fullTuple;
        }
    }

    @CoreMethod(names = {"length", "size"})
    public abstract static class LengthNode extends CoreMethodArrayArgumentsNode {

        public LengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int length(RubyBasicObject matchData) {
            return getValues(matchData).length;
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
        public Object preMatch(RubyBasicObject matchData) {
            return taintResultNode.maybeTaint(getSource(matchData), getPre(matchData));
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
        public Object postMatch(RubyBasicObject matchData) {
            return taintResultNode.maybeTaint(getSource(matchData), getPost(matchData));
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toA(RubyBasicObject matchData) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), getValues(matchData));
        }
    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyBasicObject matchData) {
            CompilerDirectives.transferToInterpreter();

            final ByteList bytes = StringNodes.getByteList(getGlobal(matchData)).dup();
            return createString(bytes);
        }
    }

    @CoreMethod(names = "regexp")
    public abstract static class RegexpNode extends CoreMethodArrayArgumentsNode {

        public RegexpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject regexp(RubyBasicObject matchData) {
            return getRegexp(matchData);
        }
    }

    @RubiniusOnly
    @NodeChild(value = "self")
    public abstract static class RubiniusSourceNode extends RubyNode {

        public RubiniusSourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject rubiniusSource(RubyBasicObject matchData) {
            return getSource(matchData);
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
