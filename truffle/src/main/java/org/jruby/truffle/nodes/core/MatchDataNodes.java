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
import org.joni.exception.ValueException;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.util.ByteList;

import java.util.Arrays;

@CoreClass(name = "MatchData")
public abstract class MatchDataNodes {

    @CoreMethod(names = "[]", required = 1, optional = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class GetIndexNode extends CoreMethodArrayArgumentsNode {

        @Child private ToIntNode toIntNode;

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object getIndex(RubyMatchData matchData, int index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            final Object[] values = matchData.getValues();
            final int normalizedIndex = ArrayNodes.normalizeIndex(values.length, index);

            if ((normalizedIndex < 0) || (normalizedIndex >= values.length)) {
                return nil();
            } else {
                return values[normalizedIndex];
            }
        }

        @Specialization
        public Object getIndex(RubyMatchData matchData, int index, int length) {
            CompilerDirectives.transferToInterpreter();
            // TODO BJF 15-May-2015 Need to handle negative indexes and lengths and out of bounds
            final Object[] values = matchData.getValues();
            final int normalizedIndex = ArrayNodes.normalizeIndex(values.length, index);
            final Object[] store = Arrays.copyOfRange(values, normalizedIndex, normalizedIndex + length);
            return createArray(store, length);
        }

        @Specialization
        public Object getIndex(RubyMatchData matchData, RubySymbol index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                final int i = matchData.getBackrefNumber(index.getSymbolBytes());

                return getIndex(matchData, i, NotProvided.INSTANCE);
            } catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                    getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", index.toString()), this));
            }
        }

        @Specialization
        public Object getIndex(RubyMatchData matchData, RubyString index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            try {
                final int i = matchData.getBackrefNumber(StringNodes.getByteList(index));

                return getIndex(matchData, i, NotProvided.INSTANCE);
            }
            catch (final ValueException e) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("undefined group name reference: %s", index.toString()), this));
            }
        }

        @Specialization(guards = {"!isRubySymbol(index)", "!isRubyString(index)", "!isIntegerFixnumRange(index)"})
        public Object getIndex(VirtualFrame frame, RubyMatchData matchData, Object index, NotProvided length) {
            CompilerDirectives.transferToInterpreter();

            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }

            return getIndex(matchData, toIntNode.doInt(frame, index), NotProvided.INSTANCE);
        }

        @Specialization(guards = {"!isRubySymbol(range)", "!isRubyString(range)"})
        public Object getIndex(VirtualFrame frame, RubyMatchData matchData, RubyRange.IntegerFixnumRange range, NotProvided len) {
            final Object[] values = matchData.getValues();
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
        public Object begin(RubyMatchData matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= matchData.getNumberOfRegions()))) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("index %d out of matches", index), this));

            } else {
                return matchData.begin(index);
            }
        }
    }


    @CoreMethod(names = "captures")
    public abstract static class CapturesNode extends CoreMethodArrayArgumentsNode {

        public CapturesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toA(RubyMatchData matchData) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), matchData.getCaptures());
        }
    }

    @CoreMethod(names = "end", required = 1, lowerFixnumParameters = 1)
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile badIndexProfile = ConditionProfile.createBinaryProfile();

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object end(RubyMatchData matchData, int index) {
            CompilerDirectives.transferToInterpreter();

            if (badIndexProfile.profile((index < 0) || (index >= matchData.getNumberOfRegions()))) {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(
                        getContext().getCoreLibrary().indexError(String.format("index %d out of matches", index), this));

            } else {
                return matchData.end(index);
            }
        }
    }

    @CoreMethod(names = {"length", "size"})
    public abstract static class LengthNode extends CoreMethodArrayArgumentsNode {

        public LengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int length(RubyMatchData matchData) {
            return matchData.getValues().length;
        }

    }

    @CoreMethod(names = "pre_match")
    public abstract static class PreMatchNode extends CoreMethodArrayArgumentsNode {

        public PreMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject preMatch(RubyMatchData matchData) {
            return matchData.getPre();
        }

    }

    @CoreMethod(names = "post_match")
    public abstract static class PostMatchNode extends CoreMethodArrayArgumentsNode {

        public PostMatchNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject postMatch(RubyMatchData matchData) {
            return matchData.getPost();
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toA(RubyMatchData matchData) {
            CompilerDirectives.transferToInterpreter();

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), matchData.getValues());
        }
    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toS(RubyMatchData matchData) {
            CompilerDirectives.transferToInterpreter();

            final ByteList bytes = StringNodes.getByteList(matchData.getGlobal()).dup();
            return createString(bytes);
        }
    }

    @CoreMethod(names = "values_at", argumentsAsArray = true)
    public abstract static class ValuesAtNode extends CoreMethodArrayArgumentsNode {

        public ValuesAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject valuesAt(RubyMatchData matchData, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            final int[] indicies = new int[args.length];

            for (int n = 0; n < args.length; n++) {
                indicies[n] = (int) args[n];
            }

            return ArrayNodes.fromObjects(getContext().getCoreLibrary().getArrayClass(), matchData.valuesAt(indicies));
        }

    }

    // Not a core method, used to simulate Rubinius @source.
    @NodeChild(value = "self")
    public abstract static class RubiniusSourceNode extends RubyNode {

        public RubiniusSourceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject rubiniusSource(RubyMatchData matchData) {
            return matchData.getSource();
        }
    }

}
