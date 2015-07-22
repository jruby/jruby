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
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyIntegerFixnumRange;
import org.jruby.truffle.runtime.core.RubyLongFixnumRange;
import org.jruby.truffle.runtime.core.RubyObjectRange;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    public static boolean isExcludeEnd(RubyIntegerFixnumRange range) {
        return range.excludeEnd;
    }

    public static int getBegin(RubyIntegerFixnumRange range) {
        return range.begin;
    }

    public static int getEnd(RubyIntegerFixnumRange range) {
        return range.end;
    }

    public static boolean isExcludeEnd(RubyLongFixnumRange range) {
        return range.excludeEnd;
    }

    public static long getBegin(RubyLongFixnumRange range) {
        return range.begin;
    }

    public static long getEnd(RubyLongFixnumRange range) {
        return range.end;
    }

    public static boolean isExcludeEnd(RubyObjectRange range) {
        return range.excludeEnd;
    }

    public static Object getBegin(RubyObjectRange range) {
        return range.begin;
    }

    public static Object getEnd(RubyObjectRange range) {
        return range.end;
    }

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public RubyBasicObject collect(VirtualFrame frame, RubyBasicObject range, RubyBasicObject block) {
            final int begin = getBegin(((RubyIntegerFixnumRange) range));
            int result;
            if (isExcludeEnd(((RubyIntegerFixnumRange) range))) {
                result = getEnd(((RubyIntegerFixnumRange) range));
            } else {
                result = getEnd(((RubyIntegerFixnumRange) range)) + 1;
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

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public Object eachInt(VirtualFrame frame, RubyBasicObject range, RubyBasicObject block) {
            int result;
            if (isExcludeEnd(((RubyIntegerFixnumRange) range))) {
                result = getEnd(((RubyIntegerFixnumRange) range));
            } else {
                result = getEnd(((RubyIntegerFixnumRange) range)) + 1;
            }
            final int exclusiveEnd = result;

            int count = 0;

            try {
                for (int n = getBegin(((RubyIntegerFixnumRange) range)); n < exclusiveEnd; n++) {
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

        @Specialization(guards = {"isLongFixnumRange(range)", "isRubyProc(block)"})
        public Object eachLong(VirtualFrame frame, RubyBasicObject range, RubyBasicObject block) {
            long result;
            if (isExcludeEnd(((RubyLongFixnumRange) range))) {
                result = getEnd(((RubyLongFixnumRange) range));
            } else {
                result = getEnd(((RubyLongFixnumRange) range)) + 1;
            }
            final long exclusiveEnd = result;

            int count = 0;

            try {
                for (long n = getBegin(((RubyLongFixnumRange) range)); n < exclusiveEnd; n++) {
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

        @Specialization(guards = "isLongFixnumRange(range)")
        public Object eachObject(VirtualFrame frame, RubyBasicObject range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object each(VirtualFrame frame, RubyBasicObject range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization(guards = {"isObjectRange(range)", "isRubyProc(block)"})
        public Object each(VirtualFrame frame, RubyBasicObject range, RubyBasicObject block) {
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
            return isExcludeEnd(((RubyIntegerFixnumRange) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public boolean excludeEndLong(RubyBasicObject range) {
            return isExcludeEnd(((RubyLongFixnumRange) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean excludeEndObject(RubyBasicObject range) {
            return isExcludeEnd(((RubyObjectRange) range));
        }

    }

    @CoreMethod(names = "begin")
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public int eachInt(RubyBasicObject range) {
            return getBegin(((RubyIntegerFixnumRange) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long eachLong(RubyBasicObject range) {
            return getBegin(((RubyLongFixnumRange) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object eachObject(RubyBasicObject range) {
            return getBegin(((RubyObjectRange) range));
        }

    }

    @CoreMethod(names = "initialize_internal", required = 2, optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isObjectRange(range)")
        public RubyBasicObject initialize(RubyBasicObject range, Object begin, Object end, NotProvided excludeEnd) {
            return initialize(range, begin, end, false);
        }

        @Specialization(guards = "isObjectRange(range)")
        public RubyBasicObject initialize(RubyBasicObject range, Object begin, Object end, boolean excludeEnd) {
            ((RubyObjectRange) range).begin = begin;
            ((RubyObjectRange) range).end = end;
            ((RubyObjectRange) range).excludeEnd = excludeEnd;
            return range;
        }

    }

    @CoreMethod(names = "end")
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public int lastInt(RubyBasicObject range) {
            return getEnd(((RubyIntegerFixnumRange) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long lastLong(RubyBasicObject range) {
            return getEnd(((RubyLongFixnumRange) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object lastObject(RubyBasicObject range) {
            return getEnd(((RubyObjectRange) range));
        }

    }

    @CoreMethod(names = "step", needsBlock = true, optional = 1, returnsEnumeratorIfNoBlock = true)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "step > 0", "isRubyProc(block)"})
        public Object stepInt(VirtualFrame frame, RubyBasicObject range, int step, RubyBasicObject block) {
            int count = 0;

            try {
                int result;
                if (isExcludeEnd(((RubyIntegerFixnumRange) range))) {
                    result = getEnd(((RubyIntegerFixnumRange) range));
                } else {
                    result = getEnd(((RubyIntegerFixnumRange) range)) + 1;
                }
                for (int n = getBegin(((RubyIntegerFixnumRange) range)); n < result; n += step) {
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

        @Specialization(guards = {"isLongFixnumRange(range)", "step > 0", "isRubyProc(block)"})
        public Object stepLong(VirtualFrame frame, RubyBasicObject range, int step, RubyBasicObject block) {
            int count = 0;

            try {
                long result;
                if (isExcludeEnd(((RubyLongFixnumRange) range))) {
                    result = getEnd(((RubyLongFixnumRange) range));
                } else {
                    result = getEnd(((RubyLongFixnumRange) range)) + 1;
                }
                for (long n = getBegin(((RubyLongFixnumRange) range)); n < result; n += step) {
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

        @Specialization(guards = {"isIntegerFixnumRange(range)", "wasProvided(step)", "isRubyProc(block)"})
        public Object stepFallbackInt(VirtualFrame frame, RubyBasicObject range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "wasProvided(step)", "isRubyProc(block)"})
        public Object stepFallbackLong(VirtualFrame frame, RubyBasicObject range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object stepInt(VirtualFrame frame, RubyBasicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public Object stepInt(VirtualFrame frame, RubyBasicObject range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "!isInteger(step)", "!isLong(step)", "wasProvided(step)"})
        public Object stepInt(VirtualFrame frame, RubyBasicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public Object stepLong(VirtualFrame frame, RubyBasicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "isRubyProc(block)"})
        public Object stepLong(VirtualFrame frame, RubyBasicObject range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "wasProvided(step)"})
        public Object stepLong(VirtualFrame frame, RubyBasicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = {"isObjectRange(range)", "wasProvided(step)", "isRubyProc(block)"})
        public Object stepObject(VirtualFrame frame, RubyBasicObject range, Object step, RubyBasicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object stepObject(VirtualFrame frame, RubyBasicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isObjectRange(range)", "isRubyProc(block)"})
        public Object stepObject(VirtualFrame frame, RubyBasicObject range, NotProvided step, RubyBasicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isObjectRange(range)", "wasProvided(step)"})
        public Object step(VirtualFrame frame, RubyBasicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

    }

    @CoreMethod(names = "to_a", lowerFixnumSelf = true)
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public RubyBasicObject toA(RubyBasicObject range) {
            final int begin = getBegin(((RubyIntegerFixnumRange) range));
            int result;
            if (isExcludeEnd(((RubyIntegerFixnumRange) range))) {
                result = getEnd(((RubyIntegerFixnumRange) range));
            } else {
                result = getEnd(((RubyIntegerFixnumRange) range)) + 1;
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

        @Specialization(guards = "isObjectRange(range)")
        public Object toA(VirtualFrame frame, RubyBasicObject range) {
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

        @Specialization(guards = "isObjectRange(range)")
        public Object setBegin(RubyBasicObject range, Object begin) {
            ((RubyObjectRange) range).begin = begin;
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

        @Specialization(guards = "isObjectRange(range)")
        public Object setEnd(RubyBasicObject range, Object end) {
            ((RubyObjectRange) range).end = end;
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

        @Specialization(guards = "isObjectRange(range)")
        public boolean setExcludeEnd(RubyBasicObject range, boolean excludeEnd) {
            ((RubyObjectRange) range).excludeEnd = excludeEnd;
            return excludeEnd;
        }

    }

    public static class RangeAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyBasicObject rubyClass, Node currentNode) {
            return new RubyObjectRange(rubyClass, context.getCoreLibrary().getNilObject(), context.getCoreLibrary().getNilObject(), false);
        }

    }
}
