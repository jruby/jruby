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
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @CoreMethod(names = { "collect", "map" }, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "isRubyProc(block)" })
        public DynamicObject collect(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            final int begin = Layouts.INTEGER_FIXNUM_RANGE.getBegin(range);
            final int end = Layouts.INTEGER_FIXNUM_RANGE.getEnd(range);
            final boolean excludedEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range);
            final int length = (excludedEnd ? end : end + 1) - begin;

            Object store = arrayBuilder.start(length);
            int count = 0;

            try {
                for (int n = 0; n < length; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.appendValue(store, n, yield(frame, block, begin + n));
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

        @Child private CallDispatchHeadNode eachInternalCall;

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "isRubyProc(block)" })
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

        @Specialization(guards = { "isLongFixnumRange(range)", "isRubyProc(block)" })
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

        private Object eachInternal(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            if (eachInternalCall == null) {
                CompilerDirectives.transferToInterpreter();
                eachInternalCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return eachInternalCall.call(frame, range, "each_internal", block);
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public Object eachObject(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return eachInternal(frame, range, null);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object each(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return eachInternal(frame, range, null);
        }

        @Specialization(guards = { "isObjectRange(range)", "isRubyProc(block)" })
        public Object each(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            return eachInternal(frame, range, block);
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

        @Child private CallDispatchHeadNode stepInternalCall;

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "step > 0", "isRubyProc(block)" })
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

        @Specialization(guards = { "isLongFixnumRange(range)", "step > 0", "isRubyProc(block)" })
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

        private Object stepInternal(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            return stepInternal(frame, range, 1, block);
        }

        private Object stepInternal(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            if (stepInternalCall == null) {
                CompilerDirectives.transferToInterpreter();
                stepInternalCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return stepInternalCall.call(frame, range, "step_internal", block, step);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "wasProvided(step)", "isRubyProc(block)" })
        public Object stepFallbackInt(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = { "isLongFixnumRange(range)", "wasProvided(step)", "isRubyProc(block)" })
        public Object stepFallbackLong(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "isRubyProc(block)" })
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = { "isIntegerFixnumRange(range)", "!isInteger(step)", "!isLong(step)", "wasProvided(step)" })
        public Object stepInt(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = { "isLongFixnumRange(range)", "isRubyProc(block)" })
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = { "isLongFixnumRange(range)", "wasProvided(step)" })
        public Object stepLong(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

        @Specialization(guards = { "isObjectRange(range)", "wasProvided(step)", "isRubyProc(block)" })
        public Object stepObject(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = { "isObjectRange(range)", "isRubyProc(block)" })
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = { "isObjectRange(range)", "wasProvided(step)" })
        public Object step(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

    }

    @CoreMethod(names = "to_a", lowerFixnumSelf = true)
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toAInternalCall;

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
            if (toAInternalCall == null) {
                CompilerDirectives.transferToInterpreter();
                toAInternalCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toAInternalCall.call(frame, range, "to_a_internal", null);
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

        @CreateCast("excludeEnd")
        public RubyNode castToBoolean(RubyNode excludeEnd) {
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
