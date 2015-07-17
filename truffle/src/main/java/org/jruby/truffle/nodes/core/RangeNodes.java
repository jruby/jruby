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
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayBuilderNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject collect(VirtualFrame frame, RubyIntegerFixnumRange range, RubyBasicObject block) {
            final int begin = range.begin;
            int result;
            if (range.excludeEnd) {
                result = range.end;
            } else {
                result = range.end + 1;
            }
            final int exclusiveEnd = result;
            final int length = exclusiveEnd - begin;

            Object store = arrayBuilder.start(length);

            int count = 0;

            try {
                for (int n = 0; n < length; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.appendValue(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return ArrayNodes.createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(store, length), length);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, lowerFixnumSelf = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object each(VirtualFrame frame, RubyIntegerFixnumRange range, RubyBasicObject block) {
            int result;
            if (range.excludeEnd) {
                result = range.end;
            } else {
                result = range.end + 1;
            }
            final int exclusiveEnd = result;

            int count = 0;

            try {
                for (int n = range.begin; n < exclusiveEnd; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object each(VirtualFrame frame, RubyLongFixnumRange range, RubyBasicObject block) {
            long result;
            if (range.excludeEnd) {
                result = range.end;
            } else {
                result = range.end + 1;
            }
            final long exclusiveEnd = result;

            int count = 0;

            try {
                for (long n = range.begin; n < exclusiveEnd; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyLongFixnumRange range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization
        public Object each(VirtualFrame frame, RubyObjectRange range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object each(VirtualFrame frame, RubyObjectRange range, RubyBasicObject block) {
            return ruby(frame, "each_internal(&block)", "block", block);
        }

    }

    @CoreMethod(names = "exclude_end?")
    public abstract static class ExcludeEndNode extends CoreMethodArrayArgumentsNode {

        public ExcludeEndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public boolean excludeEndInt(RubyBasicObject range) {
            return ((RubyIntegerFixnumRange) range).excludeEnd;
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public boolean excludeEndLong(RubyBasicObject range) {
            return ((RubyLongFixnumRange) range).excludeEnd;
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean excludeEndObject(RubyBasicObject range) {
            return ((RubyObjectRange) range).excludeEnd;
        }

    }

    @CoreMethod(names = "begin")
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int each(RubyIntegerFixnumRange range) {
            return range.begin;
        }

        @Specialization
        public long each(RubyLongFixnumRange range) {
            return range.begin;
        }

        @Specialization
        public Object each(RubyObjectRange range) {
            return range.begin;
        }

    }

    @CoreMethod(names = "initialize_internal", required = 2, optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyObjectRange initialize(RubyObjectRange range, Object begin, Object end, NotProvided excludeEnd) {
            return initialize(range, begin, end, false);
        }

        @Specialization
        public RubyObjectRange initialize(RubyObjectRange range, Object begin, Object end, boolean excludeEnd) {
            range.begin = begin;
            range.end = end;
            range.excludeEnd = excludeEnd;
            return range;
        }

    }

    @CoreMethod(names = "end")
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int last(RubyIntegerFixnumRange range) {
            return range.end;
        }

        @Specialization
        public long last(RubyLongFixnumRange range) {
            return range.end;
        }

        @Specialization
        public Object last(RubyObjectRange range) {
            return range.end;
        }

    }

    @CoreMethod(names = "step", needsBlock = true, optional = 1, returnsEnumeratorIfNoBlock = true)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"step > 0", "isRubyProc(block)"})
        public Object step(VirtualFrame frame, RubyIntegerFixnumRange range, int step, RubyBasicObject block) {
            int count = 0;

            try {
                int result;
                if (range.excludeEnd) {
                    result = range.end;
                } else {
                    result = range.end + 1;
                }
                for (int n = range.begin; n < result; n += step) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

        @Specialization(guards = {"step > 0", "isRubyProc(block)"})
        public Object step(VirtualFrame frame, RubyLongFixnumRange range, int step, RubyBasicObject block) {
            int count = 0;

            try {
                long result;
                if (range.excludeEnd) {
                    result = range.end;
                } else {
                    result = range.end + 1;
                }
                for (long n = range.begin; n < result; n += step) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return range;
        }

        @Specialization(guards = {"wasProvided(step)", "isRubyProc(block)"})
        public Object stepFallback(VirtualFrame frame, RubyIntegerFixnumRange range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = {"wasProvided(step)", "isRubyProc(block)"})
        public Object stepFallback(VirtualFrame frame, RubyLongFixnumRange range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyIntegerFixnumRange range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object step(VirtualFrame frame, RubyIntegerFixnumRange range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = { "!isInteger(step)", "!isLong(step)", "wasProvided(step)" })
        public Object step(VirtualFrame frame, RubyIntegerFixnumRange range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyLongFixnumRange range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object step(VirtualFrame frame, RubyLongFixnumRange range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = "wasProvided(step)")
        public Object step(VirtualFrame frame, RubyLongFixnumRange range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = {"wasProvided(step)", "isRubyProc(block)"})
        public Object step(VirtualFrame frame, RubyObjectRange range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization
        public Object step(VirtualFrame frame, RubyObjectRange range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = "isRubyProc(block)")
        public Object step(VirtualFrame frame, RubyObjectRange range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = "wasProvided(step)")
        public Object step(VirtualFrame frame, RubyObjectRange range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

    }

    @CoreMethod(names = "to_a", lowerFixnumSelf = true)
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject toA(RubyIntegerFixnumRange range) {
            final int begin = range.begin;
            int result;
            if (range.excludeEnd) {
                result = range.end;
            } else {
                result = range.end + 1;
            }
            final int length = result - begin;

            if (length < 0) {
                return createEmptyArray();
            } else {
                final int[] values = new int[length];

                for (int n = 0; n < length; n++) {
                    values[n] = begin + n;
                }

                return createArray(values, length);
            }
        }


        @Specialization
        public Object toA(VirtualFrame frame, RubyObjectRange range) {
            return ruby(frame, "to_a_internal");
        }

    }

    @RubiniusOnly
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "begin")
    })
    public abstract static class InternalSetBeginNode extends RubyNode {

        public InternalSetBeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setBegin(RubyObjectRange range, Object begin) {
            range.begin = begin;

            return begin;
        }
    }

    @RubiniusOnly
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "end")
    })
    public abstract static class InternalSetEndNode extends RubyNode {

        public InternalSetEndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object setEnd(RubyObjectRange range, Object end) {
            range.end = end;

            return end;
        }
    }

    @RubiniusOnly
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "excludeEnd")
    })
    public abstract static class InternalSetExcludeEndNode extends RubyNode {

        public InternalSetExcludeEndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("excludeEnd") public RubyNode castToBoolean(RubyNode excludeEnd) {
            return BooleanCastNodeGen.create(getContext(), getSourceSection(), excludeEnd);
        }

        @Specialization
        public boolean setExcludeEnd(RubyObjectRange range, boolean excludeEnd) {
            range.excludeEnd = excludeEnd;

            return excludeEnd;
        }

    }

    public static class RangeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyObjectRange(rubyClass, context.getCoreLibrary().getNilObject(), context.getCoreLibrary().getNilObject(), false);
        }

    }
}
