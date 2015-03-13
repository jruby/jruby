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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @CoreMethod(names = "==", required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        protected boolean equal(VirtualFrame frame, Object a, Object b) {
            if (equalNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                equalNode = insert(KernelNodesFactory.SameOrEqualNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null}));
            }
            return equalNode.executeSameOrEqual(frame, a, b);

        }

        @Specialization
        public boolean equal(RubyRange.IntegerFixnumRange a, RubyRange.IntegerFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization
        public boolean equal(RubyRange.IntegerFixnumRange a, RubyRange.LongFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization
        public boolean equal(RubyRange.LongFixnumRange a, RubyRange.LongFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization
        public boolean equal(RubyRange.LongFixnumRange a, RubyRange.IntegerFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization
        public boolean equal(VirtualFrame frame, RubyRange.ObjectRange a, RubyRange.ObjectRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() &&
                    equal(frame, a.getBegin(), b.getBegin()) &&
                    equal(frame, a.getEnd(), b.getEnd());
        }
    }

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public CollectNode(CollectNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization
        public RubyArray collect(VirtualFrame frame, RubyRange.IntegerFixnumRange range, RubyProc block) {
            final int begin = range.getBegin();
            final int exclusiveEnd = range.getExclusiveEnd();
            final int length = exclusiveEnd - begin;

            Object store = arrayBuilder.start(length);

            int count = 0;

            try {
                for (int n = 0; n < length; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.append(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(store, length), length);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, lowerFixnumSelf = true)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyRange.IntegerFixnumRange range, RubyProc block) {
            final int exclusiveEnd = range.getExclusiveEnd();

            int count = 0;

            try {
                outer:
                for (int n = range.getBegin(); n < exclusiveEnd; n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, n);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

    }

    @CoreMethod(names = "exclude_end?")
    public abstract static class ExcludeEndNode extends CoreMethodNode {

        public ExcludeEndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExcludeEndNode(ExcludeEndNode prev) {
            super(prev);
        }

        @Specialization
        public boolean excludeEnd(RubyRange range) {
            return range.doesExcludeEnd();
        }

    }

    @CoreMethod(names = { "begin", "first" })
    public abstract static class BeginNode extends CoreMethodNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BeginNode(BeginNode prev) {
            super(prev);
        }

        @Specialization
        public int each(RubyRange.IntegerFixnumRange range) {
            return range.getBegin();
        }

        @Specialization
        public Object each(RubyRange.ObjectRange range) {
            return range.getBegin();
        }

    }

    @CoreMethod(names = {"include?", "==="}, required = 1)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode callLess;
        @Child private CallDispatchHeadNode callGreater;
        @Child private CallDispatchHeadNode callGreaterEqual;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callLess = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            callGreater = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
            callGreaterEqual = DispatchHeadNodeFactory.createMethodCall(context, false, false, null);
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            callLess = prev.callLess;
            callGreater = prev.callGreater;
            callGreaterEqual = prev.callGreaterEqual;
        }

        @Specialization
        public boolean include(RubyRange.IntegerFixnumRange range, int value) {
            return value >= range.getBegin() && value < range.getExclusiveEnd();
        }

        @Specialization
        public boolean include(VirtualFrame frame, RubyRange.ObjectRange range, Object value) {
            notDesignedForCompilation();

            if (callLess.callBoolean(frame, value, "<", null, range.getBegin())) {
                return false;
            }

            if (range.doesExcludeEnd()) {
                if (callGreaterEqual.callBoolean(frame, value, ">=", null, range.getEnd())) {
                    return false;
                }
            } else {
                if (callGreater.callBoolean(frame, value, ">", null, range.getEnd())) {
                    return false;
                }
            }

            return true;
        }
    }

    @CoreMethod(names = "initialize", required = 2, optional = 1)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyRange.ObjectRange initialize(RubyRange.ObjectRange range, Object begin, Object end, UndefinedPlaceholder undefined) {
            return initialize(range, begin, end, false);
        }

        @Specialization
        public RubyRange.ObjectRange initialize(RubyRange.ObjectRange range, Object begin, Object end, boolean excludeEnd) {
            range.initialize(begin, end, excludeEnd);
            return range;
        }

    }

    @CoreMethod(names = { "end", "last" })
    public abstract static class EndNode extends CoreMethodNode {

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EndNode(EndNode prev) {
            super(prev);
        }

        @Specialization
        public int last(RubyRange.IntegerFixnumRange range) {
            return range.getEnd();
        }

        @Specialization
        public Object last(RubyRange.ObjectRange range) {
            return range.getEnd();
        }

    }

    @CoreMethod(names = "step", needsBlock = true, required = 1)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StepNode(StepNode prev) {
            super(prev);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.IntegerFixnumRange range, int step, RubyProc block) {
            int count = 0;

            try {
                outer:
                for (int n = range.getBegin(); n < range.getExclusiveEnd(); n += step) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, n);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

    }

    @CoreMethod(names = "to_a", lowerFixnumSelf = true)
    public abstract static class ToANode extends CoreMethodNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToANode(ToANode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray toA(RubyRange.IntegerFixnumRange range) {
            final int begin = range.getBegin();
            final int length = range.getExclusiveEnd() - begin;

            if (length < 0) {
                return new RubyArray(getContext().getCoreLibrary().getArrayClass());
            } else {
                final int[] values = new int[length];

                for (int n = 0; n < length; n++) {
                    values[n] = begin + n;
                }

                return new RubyArray(getContext().getCoreLibrary().getArrayClass(), values, length);
            }
        }

    }

    @CoreMethod(names = "to_s")
    public abstract static class ToSNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode toS;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
            toS = prev.toS;
        }

        @Specialization
        public RubyString toS(RubyRange.IntegerFixnumRange range) {
            notDesignedForCompilation();

            return getContext().makeString(range.getBegin() + (range.doesExcludeEnd() ? "..." : "..") + range.getEnd());
        }

        @Specialization
        public RubyString toS(VirtualFrame frame, RubyRange.ObjectRange range) {
            notDesignedForCompilation();

            // TODO(CS): cast?
            final RubyString begin = (RubyString) toS.call(frame, range.getBegin(), "to_s", null);
            final RubyString end = (RubyString) toS.call(frame, range.getBegin(), "to_s", null);

            return getContext().makeString(begin + (range.doesExcludeEnd() ? "..." : "..") + end);
        }
    }

}
