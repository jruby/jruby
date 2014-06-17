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

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.utilities.*;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.control.*;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @CoreMethod(names = "==", minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization(order = 1)
        public boolean equal(RubyRange.IntegerFixnumRange a, RubyRange.IntegerFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization(order = 2)
        public boolean equal(RubyRange.IntegerFixnumRange a, RubyRange.LongFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization(order = 3)
        public boolean equal(RubyRange.LongFixnumRange a, RubyRange.LongFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

        @Specialization(order = 4)
        public boolean equal(RubyRange.LongFixnumRange a, RubyRange.IntegerFixnumRange b) {
            notDesignedForCompilation();

            return a.doesExcludeEnd() == b.doesExcludeEnd() && a.getBegin() == b.getBegin() && a.getEnd() == b.getEnd();
        }

    }

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, maxArgs = 0, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child protected ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context, true);
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

            Object store = arrayBuilder.length(length);

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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(store), length);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, maxArgs = 0, lowerFixnumSelf = true)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = new BranchProfile();
        private final BranchProfile nextProfile = new BranchProfile();
        private final BranchProfile redoProfile = new BranchProfile();

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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return range;
        }

    }

    @CoreMethod(names = "exclude_end?", maxArgs = 0)
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

    @CoreMethod(names = "first", maxArgs = 0)
    public abstract static class FirstNode extends CoreMethodNode {

        public FirstNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FirstNode(FirstNode prev) {
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

    @CoreMethod(names = {"include?", "==="}, maxArgs = 1, lowerFixnumSelf = true, lowerFixnumParameters = 0)
    public abstract static class IncludeNode extends CoreMethodNode {

        @Child protected DispatchHeadNode callLess;
        @Child protected DispatchHeadNode callGreater;
        @Child protected DispatchHeadNode callGreaterEqual;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            callLess = new DispatchHeadNode(context, "<", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
            callGreater = new DispatchHeadNode(context, ">", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
            callGreaterEqual = new DispatchHeadNode(context, ">=", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
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

            if ((boolean) callLess.dispatch(frame, value, null, range.getBegin())) {
                return false;
            }

            if (range.doesExcludeEnd()) {
                if ((boolean) callGreaterEqual.dispatch(frame, value, null, range.getEnd())) {
                    return false;
                }
            } else {
                if ((boolean) callGreater.dispatch(frame, value, null, range.getEnd())) {
                    return false;
                }
            }

            return true;
        }
    }

    @CoreMethod(names = "last", maxArgs = 0)
    public abstract static class LastNode extends CoreMethodNode {

        public LastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LastNode(LastNode prev) {
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

    @CoreMethod(names = "step", needsBlock = true, minArgs = 1, maxArgs = 1)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = new BranchProfile();
        private final BranchProfile nextProfile = new BranchProfile();
        private final BranchProfile redoProfile = new BranchProfile();

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StepNode(StepNode prev) {
            super(prev);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.IntegerFixnumRange range, int step, RubyProc block) {
            notDesignedForCompilation();

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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return range;
        }

    }

    @CoreMethod(names = "to_a", maxArgs = 0, lowerFixnumSelf = true)
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

    @CoreMethod(names = "to_s", maxArgs = 0)
    public abstract static class ToSNode extends CoreMethodNode {

        @Child protected DispatchHeadNode toS;

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            toS = new DispatchHeadNode(context, "to_s", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
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
            final RubyString begin = (RubyString) toS.dispatch(frame, range.getBegin(), null);
            final RubyString end = (RubyString) toS.dispatch(frame, range.getBegin(), null);

            return getContext().makeString(begin + (range.doesExcludeEnd() ? "..." : "..") + end);
        }
    }

}
