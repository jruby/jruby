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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayBuilderNode;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public DynamicObject collect(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            final int begin = Layouts.INTEGER_FIXNUM_RANGE.getBegin(range);
            int result;
            if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range)) {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(range);
            } else {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(range) + 1;
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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(store, length), length);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, lowerFixnumSelf = true, returnsEnumeratorIfNoBlock = true)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public Object eachInt(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            int result;
            if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range))) {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range));
            } else {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)) + 1;
            }
            final int exclusiveEnd = result;

            int count = 0;

            try {
                for (int n = Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)); n < exclusiveEnd; n++) {
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
        public Object eachLong(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            long result;
            if (Layouts.LONG_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range))) {
                result = Layouts.LONG_FIXNUM_RANGE.getEnd(((DynamicObject) range));
            } else {
                result = Layouts.LONG_FIXNUM_RANGE.getEnd(((DynamicObject) range)) + 1;
            }
            final long exclusiveEnd = result;

            int count = 0;

            try {
                for (long n = Layouts.LONG_FIXNUM_RANGE.getBegin(((DynamicObject) range)); n < exclusiveEnd; n++) {
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
        public Object eachObject(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object each(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return ruby(frame, "each_internal(&block)", "block", nil());
        }

        @Specialization(guards = {"isObjectRange(range)", "isRubyProc(block)"})
        public Object each(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            return ruby(frame, "each_internal(&block)", "block", block);
        }

    }

    @CoreMethod(names = "exclude_end?")
    public abstract static class ExcludeEndNode extends CoreMethodArrayArgumentsNode {

        public ExcludeEndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public boolean excludeEndInt(DynamicObject range) {
            return Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public boolean excludeEndLong(DynamicObject range) {
            return Layouts.LONG_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean excludeEndObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getExcludedEnd(((DynamicObject) range));
        }

    }

    @CoreMethod(names = "begin")
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public int eachInt(DynamicObject range) {
            return Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long eachLong(DynamicObject range) {
            return Layouts.LONG_FIXNUM_RANGE.getBegin(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object eachObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getBegin(((DynamicObject) range));
        }

    }

    @CoreMethod(names = "initialize_internal", required = 2, optional = 1)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isObjectRange(range)")
        public DynamicObject initialize(DynamicObject range, Object begin, Object end, NotProvided excludeEnd) {
            return initialize(range, begin, end, false);
        }

        @Specialization(guards = "isObjectRange(range)")
        public DynamicObject initialize(DynamicObject range, Object begin, Object end, boolean excludeEnd) {
            Layouts.OBJECT_RANGE.setExcludedEnd(range, excludeEnd);
            Layouts.OBJECT_RANGE.setBegin(range, begin);
            Layouts.OBJECT_RANGE.setEnd(range, end);
            return range;
        }

    }

    @CoreMethod(names = "end")
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        public EndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public int lastInt(DynamicObject range) {
            return Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long lastLong(DynamicObject range) {
            return Layouts.LONG_FIXNUM_RANGE.getEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object lastObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getEnd(((DynamicObject) range));
        }

    }

    @CoreMethod(names = "step", needsBlock = true, optional = 1, returnsEnumeratorIfNoBlock = true)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "step > 0", "isRubyProc(block)"})
        public Object stepInt(VirtualFrame frame, DynamicObject range, int step, DynamicObject block) {
            int count = 0;

            try {
                int result;
                if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range))) {
                    result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range));
                } else {
                    result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)) + 1;
                }
                for (int n = Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)); n < result; n += step) {
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
        public Object stepLong(VirtualFrame frame, DynamicObject range, int step, DynamicObject block) {
            int count = 0;

            try {
                long result;
                if (Layouts.LONG_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range))) {
                    result = Layouts.LONG_FIXNUM_RANGE.getEnd(((DynamicObject) range));
                } else {
                    result = Layouts.LONG_FIXNUM_RANGE.getEnd(((DynamicObject) range)) + 1;
                }
                for (long n = Layouts.LONG_FIXNUM_RANGE.getBegin(((DynamicObject) range)); n < result; n += step) {
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
        public Object stepFallbackInt(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "wasProvided(step)", "isRubyProc(block)"})
        public Object stepFallbackLong(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "!isInteger(step)", "!isLong(step)", "wasProvided(step)"})
        public Object stepInt(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "isRubyProc(block)"})
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isLongFixnumRange(range)", "wasProvided(step)"})
        public Object stepLong(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

        @Specialization(guards = {"isObjectRange(range)", "wasProvided(step)", "isRubyProc(block)"})
        public Object stepObject(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return ruby(frame, "step_internal(step, &block)", "step", step, "block", block);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return ruby(frame, "step_internal");
        }

        @Specialization(guards = {"isObjectRange(range)", "isRubyProc(block)"})
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return ruby(frame, "step_internal(&block)", "block", block);
        }

        @Specialization(guards = {"isObjectRange(range)", "wasProvided(step)"})
        public Object step(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return ruby(frame, "step_internal(step)", "step", step);
        }

    }

    @CoreMethod(names = "to_a", lowerFixnumSelf = true)
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public DynamicObject toA(DynamicObject range) {
            final int begin = Layouts.INTEGER_FIXNUM_RANGE.getBegin(range);
            int result;
            if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range)) {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(range);
            } else {
                result = Layouts.INTEGER_FIXNUM_RANGE.getEnd(range) + 1;
            }
            final int length = result - begin;

            if (length < 0) {
                return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
            } else {
                final int[] values = new int[length];

                for (int n = 0; n < length; n++) {
                    values[n] = begin + n;
                }

                return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), values, length);
            }
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object toA(VirtualFrame frame, DynamicObject range) {
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
        public Object setBegin(DynamicObject range, Object begin) {
            Layouts.OBJECT_RANGE.setBegin(range, begin);
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
        public Object setEnd(DynamicObject range, Object end) {
            Layouts.OBJECT_RANGE.setEnd(range, end);
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
        public boolean setExcludeEnd(DynamicObject range, boolean excludeEnd) {
            Layouts.OBJECT_RANGE.setExcludedEnd(range, excludeEnd);
            return excludeEnd;
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return Layouts.OBJECT_RANGE.createObjectRange(Layouts.CLASS.getInstanceFactory(rubyClass), false, nil(), nil());
        }

    }
}
