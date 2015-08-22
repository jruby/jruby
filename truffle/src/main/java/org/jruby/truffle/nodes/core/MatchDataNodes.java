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
import com.oracle.truffle.api.object.DynamicObject;
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
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.ByteList;
import org.jruby.util.CodeRangeable;
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

    public static Object begin(DynamicObject matchData, int index) {
        assert RubyGuards.isRubyMatchData(matchData);
        final int b = (Layouts.MATCH_DATA.getRegion(matchData) == null) ? Layouts.MATCH_DATA.getBegin(matchData) : Layouts.MATCH_DATA.getRegion(matchData).beg[index];

        if (b < 0) {
            return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(matchData)).getContext().getCoreLibrary().getNilObject();
        }

        updateCharOffset(matchData);

        return Layouts.MATCH_DATA.getCharOffsets(matchData).beg[index];
    }

    public static Object end(DynamicObject matchData, int index) {
        assert RubyGuards.isRubyMatchData(matchData);
        int e = (Layouts.MATCH_DATA.getRegion(matchData) == null) ? Layouts.MATCH_DATA.getEnd(matchData) : Layouts.MATCH_DATA.getRegion(matchData).end[index];

        if (e < 0) {
            return Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(matchData)).getContext().getCoreLibrary().getNilObject();
        }

        final CodeRangeable sourceWrapped = StringNodes.getCodeRangeable(Layouts.MATCH_DATA.getSource(matchData));
        if (!StringSupport.isSingleByteOptimizable(sourceWrapped, sourceWrapped.getByteList().getEncoding())) {
            updateCharOffset(matchData);
            e = Layouts.MATCH_DATA.getCharOffsets(matchData).end[index];
        }

        return e;
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

    public static void updateCharOffsetOnlyOneReg(DynamicObject matchData, ByteList value, Encoding encoding) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (Layouts.MATCH_DATA.getCharOffsetUpdated(matchData)) return;

        if (Layouts.MATCH_DATA.getCharOffsets(matchData) == null || Layouts.MATCH_DATA.getCharOffsets(matchData).numRegs < 1)
            Layouts.MATCH_DATA.setCharOffsets(matchData, new Region(1));

        if (encoding.maxLength() == 1) {
            Layouts.MATCH_DATA.getCharOffsets(matchData).beg[0] = Layouts.MATCH_DATA.getBegin(matchData);
            Layouts.MATCH_DATA.getCharOffsets(matchData).end[0] = Layouts.MATCH_DATA.getEnd(matchData);
            Layouts.MATCH_DATA.setCharOffsetUpdated(matchData, true);
            return;
        }

        Pair[] pairs = new Pair[2];
        if (Layouts.MATCH_DATA.getBegin(matchData) >= 0) {
            pairs[0] = new Pair();
            pairs[0].bytePos = Layouts.MATCH_DATA.getBegin(matchData);
            pairs[1] = new Pair();
            pairs[1].bytePos = Layouts.MATCH_DATA.getEnd(matchData);
        }

        updatePairs(value, encoding, pairs);

        if (Layouts.MATCH_DATA.getBegin(matchData) < 0) {
            Layouts.MATCH_DATA.getCharOffsets(matchData).beg[0] = Layouts.MATCH_DATA.getCharOffsets(matchData).end[0] = -1;
            return;
        }
        Pair key = new Pair();
        key.bytePos = Layouts.MATCH_DATA.getBegin(matchData);
        Layouts.MATCH_DATA.getCharOffsets(matchData).beg[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        key.bytePos = Layouts.MATCH_DATA.getEnd(matchData);
        Layouts.MATCH_DATA.getCharOffsets(matchData).end[0] = pairs[Arrays.binarySearch(pairs, key)].charPos;

        Layouts.MATCH_DATA.setCharOffsetUpdated(matchData, true);
    }

    public static void updateCharOffsetManyRegs(DynamicObject matchData, ByteList value, Encoding encoding) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (Layouts.MATCH_DATA.getCharOffsetUpdated(matchData)) return;

        final Region regs = Layouts.MATCH_DATA.getRegion(matchData);
        int numRegs = regs.numRegs;

        if (Layouts.MATCH_DATA.getCharOffsets(matchData) == null || Layouts.MATCH_DATA.getCharOffsets(matchData).numRegs < numRegs)
            Layouts.MATCH_DATA.setCharOffsets(matchData, new Region(numRegs));

        if (encoding.maxLength() == 1) {
            for (int i = 0; i < numRegs; i++) {
                Layouts.MATCH_DATA.getCharOffsets(matchData).beg[i] = regs.beg[i];
                Layouts.MATCH_DATA.getCharOffsets(matchData).end[i] = regs.end[i];
            }
            Layouts.MATCH_DATA.setCharOffsetUpdated(matchData, true);
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
                Layouts.MATCH_DATA.getCharOffsets(matchData).beg[i] = Layouts.MATCH_DATA.getCharOffsets(matchData).end[i] = -1;
                continue;
            }
            key.bytePos = regs.beg[i];
            Layouts.MATCH_DATA.getCharOffsets(matchData).beg[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
            key.bytePos = regs.end[i];
            Layouts.MATCH_DATA.getCharOffsets(matchData).end[i] = pairs[Arrays.binarySearch(pairs, key)].charPos;
        }

        Layouts.MATCH_DATA.setCharOffsetUpdated(matchData, true);
    }

    public static void updateCharOffset(DynamicObject matchData) {
        assert RubyGuards.isRubyMatchData(matchData);
        if (Layouts.MATCH_DATA.getCharOffsetUpdated(matchData)) return;

        ByteList value = Layouts.STRING.getByteList(Layouts.MATCH_DATA.getSource(matchData));
        Encoding enc = value.getEncoding();

        if (Layouts.MATCH_DATA.getRegion(matchData) == null) {
            updateCharOffsetOnlyOneReg(matchData, value, enc);
        } else {
            updateCharOffsetManyRegs(matchData, value, enc);
        }

        Layouts.MATCH_DATA.setCharOffsetUpdated(matchData, true);
    }

    @CoreMethod(names = "[]", required = 1, optional = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getIndex(DynamicObject matchData, int index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            final Object[] values = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, index);

            if ((normalizedIndex < 0) || (normalizedIndex >= values.length)) {
                return nil();
            } else {
                return values[normalizedIndex];
            }
        }

        @Specialization
        public Object getIndex(DynamicObject matchData, int index, int length) {
            CompilerDirectives.transferToInterpreter();
            // TODO BJF 15-May-2015 Need to handle negative indexes and lengths and out of bounds
            final Object[] values = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, index);
            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return createArray(store, length);
        }

        @Specialization(guards = "isRubySymbol(index)")
        public Object getIndexSymbol(DynamicObject matchData, DynamicObject index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                ByteList value = Layouts.SYMBOL.getByteList(index);
                final int i = Layouts.REGEXP.getRegex(Layouts.MATCH_DATA.getRegexp(matchData)).nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), Layouts.MATCH_DATA.getRegion(matchData));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            } catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                    getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", Layouts.SYMBOL.getString(index)), this));
            }
        }

        @Specialization(guards = "isRubyString(index)")
        public Object getIndexString(DynamicObject matchData, DynamicObject index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                ByteList value = Layouts.STRING.getByteList(index);
                final int i = Layouts.REGEXP.getRegex(Layouts.MATCH_DATA.getRegexp(matchData)).nameToBackrefNumber(value.getUnsafeBytes(), value.getBegin(), value.getBegin() + value.getRealSize(), Layouts.MATCH_DATA.getRegion(matchData));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            }
            catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", index.toString()), this));
            }
        }

        @Specialization(guards = {"!isRubySymbol(index)", "!isRubyString(index)", "!isIntegerFixnumRange(index)"})
        public Object getIndex(VirtualFrame frame, DynamicObject matchData, Object index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return getIndex(matchData, toIntNode.doInt(frame, index), NotProvided.INSTANCE);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object getIndex(DynamicObject matchData, DynamicObject range, NotProvided len) {
            final Object[] values = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);
            final int normalizedIndex = ArrayOperations.normalizeIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range));
            final int end = ArrayOperations.normalizeIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range));
            final int exclusiveEnd = ArrayOperations.clampExclusiveIndex(values.length, Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end : end + 1);
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
        public Object begin(DynamicObject matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= Layouts.MATCH_DATA.getRegion(matchData).numRegs))) {
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
        public DynamicObject toA(DynamicObject matchData) {
            CompilerDirectives.transferToInterpreter();

            DynamicObject arrayClass = getContext().getCoreLibrary().getArrayClass();
            Object[] objects = getCaptures(matchData);
            return ArrayNodes.createGeneralArray(arrayClass, objects, objects.length);
        }
    }

    @CoreMethod(names = "end", required = 1, lowerFixnumParameters = 1)
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile badIndexProfile = ConditionProfile.createBinaryProfile();

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object end(DynamicObject matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= Layouts.MATCH_DATA.getRegion(matchData).numRegs))) {
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
        public Object full(VirtualFrame frame, DynamicObject matchData) {
            if (Layouts.MATCH_DATA.getFullTuple(matchData) != null) {
                return Layouts.MATCH_DATA.getFullTuple(matchData);
            }

            if (newTupleNode == null) {
                CompilerDirectives.transferToInterpreter();
                newTupleNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object fullTuple = newTupleNode.call(frame,
                    getContext().getCoreLibrary().getTupleClass(),
                    "create",
                    null, Layouts.MATCH_DATA.getBegin(matchData), Layouts.MATCH_DATA.getEnd(matchData));

            Layouts.MATCH_DATA.setFullTuple(matchData, fullTuple);

            return fullTuple;
        }
    }

    @CoreMethod(names = {"length", "size"})
    public abstract static class LengthNode extends CoreMethodArrayArgumentsNode {

        public LengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int length(DynamicObject matchData) {
            return Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length).length;
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
            CompilerDirectives.transferToInterpreter();

            DynamicObject arrayClass = getContext().getCoreLibrary().getArrayClass();
            Object[] objects = Arrays.copyOf(Layouts.MATCH_DATA.getValues(matchData), Layouts.MATCH_DATA.getValues(matchData).length);
            return ArrayNodes.createGeneralArray(arrayClass, objects, objects.length);
        }
    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject toS(DynamicObject matchData) {
            CompilerDirectives.transferToInterpreter();

            final ByteList bytes = Layouts.STRING.getByteList(Layouts.MATCH_DATA.getGlobal(matchData)).dup();
            return createString(bytes);
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
