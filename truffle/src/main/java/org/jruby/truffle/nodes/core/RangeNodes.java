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
import org.jruby.Ruby;
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

    @CoreMethod(names = "each", needsBlock = true, lowerFixnumSelf = true, returnsEnumeratorIfNoBlock = true)
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

        @Specialization
        public Object each(VirtualFrame frame, RubyRange.LongFixnumRange range, RubyProc block) {
            final long exclusiveEnd = range.getExclusiveEnd();

            int count = 0;

            try {
                outer:
                for (long n = range.getBegin(); n < exclusiveEnd; n++) {
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

        @Specialization
        public Object each(VirtualFrame frame, RubyRange.LongFixnumRange range, UndefinedPlaceholder proc) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyRange.ObjectRange range, UndefinedPlaceholder proc) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyRange.ObjectRange range, RubyProc proc) {
            return ruby(frame, "each_internal(&block)", "block", proc);
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

    @CoreMethod(names = "begin")
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
        public long each(RubyRange.LongFixnumRange range) {
            return range.getBegin();
        }

        @Specialization
        public Object each(RubyRange.ObjectRange range) {
            return range.getBegin();
        }

    }

    @CoreMethod(names = "initialize_internal", required = 2, optional = 1)
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

    @CoreMethod(names = "end")
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
        public long last(RubyRange.LongFixnumRange range) {
            return range.getEnd();
        }

        @Specialization
        public Object last(RubyRange.ObjectRange range) {
            return range.getEnd();
        }

    }

    @CoreMethod(names = "step", needsBlock = true, optional = 1, returnsEnumeratorIfNoBlock = true)
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

        @Specialization(guards = "isStepValid(range, step, block)")
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

        @Specialization(guards = "isStepValid(range, step, block)")
        public Object step(VirtualFrame frame, RubyRange.LongFixnumRange range, int step, RubyProc block) {
            int count = 0;

            try {
                outer:
                for (long n = range.getBegin(); n < range.getExclusiveEnd(); n += step) {
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

        @Specialization(guards = {"!isStepValidInt(range, step, block)","!isUndefinedPlaceholder(step)"})
        public Object stepFallback(VirtualFrame frame, RubyRange.IntegerFixnumRange range, Object step, RubyProc block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = {"!isStepValidInt(range, step, block)","!isUndefinedPlaceholder(step)"})
        public Object stepFallback(VirtualFrame frame, RubyRange.LongFixnumRange range, Object step, RubyProc block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal");
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder step, RubyProc block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"!isInteger(step)","!isLong(step)","!isUndefinedPlaceholder(step)"})
        public Object step(VirtualFrame frame, RubyRange.IntegerFixnumRange range, Object step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.LongFixnumRange range, UndefinedPlaceholder step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal");
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.LongFixnumRange range, UndefinedPlaceholder step, RubyProc block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(step)")
        public Object step(VirtualFrame frame, RubyRange.LongFixnumRange range, Object step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(step)")
        public Object step(VirtualFrame frame, RubyRange.ObjectRange range, Object step, RubyProc block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.ObjectRange range, UndefinedPlaceholder step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal");
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyRange.ObjectRange range, UndefinedPlaceholder step, RubyProc block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = "!isUndefinedPlaceholder(step)")
        public Object step(VirtualFrame frame, RubyRange.ObjectRange range, Object step, UndefinedPlaceholder block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        public static boolean isStepValidInt(RubyRange.IntegerFixnumRange fixnumRange, Object step, RubyProc proc) {
            return step instanceof Integer && (int) step > 0;
        }

        public static boolean isStepValidInt(RubyRange.LongFixnumRange fixnumRange, Object step, RubyProc proc) {
            return step instanceof Integer && (int) step > 0;
        }

        public static boolean isStepValid(RubyRange.IntegerFixnumRange fixnumRange, int step, RubyProc proc) {
            return step > 0;
        }

        public static boolean isStepValid(RubyRange.LongFixnumRange fixnumRange, int step, RubyProc proc) {
            return step > 0;
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


        @Specialization
        public Object toA(VirtualFrame frame, RubyRange.ObjectRange range) {
            return ruby(frame, "to_a_internal");
        }

    }

}
