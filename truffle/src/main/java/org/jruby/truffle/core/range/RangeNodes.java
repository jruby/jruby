/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.range;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.array.ArrayBuilderNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.core.cast.BooleanCastWithDefaultNodeGen;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.objects.AllocateObjectNode;


@CoreClass("Range")
public abstract class RangeNodes {

    @CoreMethod(names = { "map", "collect" }, needsBlock = true)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isIntRange(range)")
        public DynamicObject map(
                VirtualFrame frame, DynamicObject range, DynamicObject block,
                @Cached("create()") ArrayBuilderNode arrayBuilder) {
            final int begin = Layouts.INT_RANGE.getBegin(range);
            final int end = Layouts.INT_RANGE.getEnd(range);
            final boolean excludedEnd = Layouts.INT_RANGE.getExcludedEnd(range);
            final int direction = begin < end ? +1 : -1;
            final int length = Math.abs((excludedEnd ? end : end + direction) - begin);

            Object store = arrayBuilder.start(length);
            int count = 0;

            try {
                for (int n = 0; n < length; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.appendValue(store, n, yield(frame, block, begin + direction * n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return createArray(arrayBuilder.finish(store, length), length);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, enumeratorSize = "size")
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode eachInternalCall;

        @Specialization(guards = "isIntRange(range)")
        public Object eachInt(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            int result;
            if (Layouts.INT_RANGE.getExcludedEnd(range)) {
                result = Layouts.INT_RANGE.getEnd(range);
            } else {
                result = Layouts.INT_RANGE.getEnd(range) + 1;
            }
            final int exclusiveEnd = result;

            int count = 0;

            try {
                for (int n = Layouts.INT_RANGE.getBegin(range); n < exclusiveEnd; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return range;
        }

        @Specialization(guards = "isLongRange(range)")
        public Object eachLong(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            long result;
            if (Layouts.LONG_RANGE.getExcludedEnd(range)) {
                result = Layouts.LONG_RANGE.getEnd(range);
            } else {
                result = Layouts.LONG_RANGE.getEnd(range) + 1;
            }
            final long exclusiveEnd = result;

            int count = 0;

            try {
                for (long n = Layouts.LONG_RANGE.getBegin(range); n < exclusiveEnd; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return range;
        }

        private Object eachInternal(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            if (eachInternalCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eachInternalCall = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            return eachInternalCall.callWithBlock(frame, range, "each_internal", block);
        }

        @Specialization(guards = "isLongRange(range)")
        public Object eachObject(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return eachInternal(frame, range, null);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object each(VirtualFrame frame, DynamicObject range, NotProvided block) {
            return eachInternal(frame, range, null);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object each(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            return eachInternal(frame, range, block);
        }

    }

    @CoreMethod(names = "exclude_end?")
    public abstract static class ExcludeEndNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isIntRange(range)")
        public boolean excludeEndInt(DynamicObject range) {
            return Layouts.INT_RANGE.getExcludedEnd(range);
        }

        @Specialization(guards = "isLongRange(range)")
        public boolean excludeEndLong(DynamicObject range) {
            return Layouts.LONG_RANGE.getExcludedEnd(range);
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean excludeEndObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getExcludedEnd(range);
        }

    }

    @CoreMethod(names = "begin")
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isIntRange(range)")
        public int eachInt(DynamicObject range) {
            return Layouts.INT_RANGE.getBegin(range);
        }

        @Specialization(guards = "isLongRange(range)")
        public long eachLong(DynamicObject range) {
            return Layouts.LONG_RANGE.getBegin(range);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object eachObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getBegin(range);
        }

    }

    @CoreMethod(names = { "dup", "clone" })
    public abstract static class DupNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateObjectNode = AllocateObjectNode.create();

        @Specialization(guards = "isIntRange(range)")
        public DynamicObject dupIntRange(DynamicObject range) {
            return Layouts.INT_RANGE.createIntRange(
                    coreLibrary().getIntRangeFactory(),
                    Layouts.INT_RANGE.getExcludedEnd(range),
                    Layouts.INT_RANGE.getBegin(range),
                    Layouts.INT_RANGE.getEnd(range));
        }

        @Specialization(guards = "isLongRange(range)")
        public DynamicObject dupLongRange(DynamicObject range) {
            return Layouts.LONG_RANGE.createLongRange(
                    coreLibrary().getIntRangeFactory(),
                    Layouts.LONG_RANGE.getExcludedEnd(range),
                    Layouts.LONG_RANGE.getBegin(range),
                    Layouts.LONG_RANGE.getEnd(range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public DynamicObject dup(DynamicObject range) {
            DynamicObject copy = allocateObjectNode.allocate(
                    Layouts.BASIC_OBJECT.getLogicalClass(range),
                    Layouts.OBJECT_RANGE.getExcludedEnd(range),
                    Layouts.OBJECT_RANGE.getBegin(range),
                    Layouts.OBJECT_RANGE.getEnd(range));
            return copy;
        }

    }

    @CoreMethod(names = "end")
    public abstract static class EndNode extends CoreMethodArrayArgumentsNode {

        @Specialization(guards = "isIntRange(range)")
        public int lastInt(DynamicObject range) {
            return Layouts.INT_RANGE.getEnd(range);
        }

        @Specialization(guards = "isLongRange(range)")
        public long lastLong(DynamicObject range) {
            return Layouts.LONG_RANGE.getEnd(range);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object lastObject(DynamicObject range) {
            return Layouts.OBJECT_RANGE.getEnd(range);
        }

    }

    @CoreMethod(names = "step", needsBlock = true, optional = 1, lowerFixnum = 1, returnsEnumeratorIfNoBlock = true)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode stepInternalCall;

        @Specialization(guards = { "isIntRange(range)", "step > 0" })
        public Object stepInt(VirtualFrame frame, DynamicObject range, int step, DynamicObject block) {
            int count = 0;

            try {
                int result;
                if (Layouts.INT_RANGE.getExcludedEnd(range)) {
                    result = Layouts.INT_RANGE.getEnd(range);
                } else {
                    result = Layouts.INT_RANGE.getEnd(range) + 1;
                }
                for (int n = Layouts.INT_RANGE.getBegin(range); n < result; n += step) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return range;
        }

        @Specialization(guards = { "isLongRange(range)", "step > 0" })
        public Object stepLong(VirtualFrame frame, DynamicObject range, int step, DynamicObject block) {
            int count = 0;

            try {
                long result;
                if (Layouts.LONG_RANGE.getExcludedEnd(range)) {
                    result = Layouts.LONG_RANGE.getEnd(range);
                } else {
                    result = Layouts.LONG_RANGE.getEnd(range) + 1;
                }
                for (long n = Layouts.LONG_RANGE.getBegin(range); n < result; n += step) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return range;
        }

        private Object stepInternal(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            return stepInternal(frame, range, 1, block);
        }

        private Object stepInternal(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            if (stepInternalCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stepInternalCall = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            return stepInternalCall.callWithBlock(frame, range, "step_internal", block, step);
        }

        @Specialization(guards = { "isIntRange(range)", "wasProvided(step)" })
        public Object stepFallbackInt(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = { "isLongRange(range)", "wasProvided(step)" })
        public Object stepFallbackLong(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = "isIntRange(range)")
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = "isIntRange(range)")
        public Object stepInt(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = {
                "isIntRange(range)",
                "!isInteger(step)",
                "!isLong(step)",
                "wasProvided(step)"
        })
        public Object stepInt(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

        @Specialization(guards = "isLongRange(range)")
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = "isLongRange(range)")
        public Object stepLong(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = { "isLongRange(range)", "wasProvided(step)" })
        public Object stepLong(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

        @Specialization(guards = { "isObjectRange(range)", "wasProvided(step)" })
        public Object stepObject(VirtualFrame frame, DynamicObject range, Object step, DynamicObject block) {
            return stepInternal(frame, range, step, block);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, NotProvided block) {
            return stepInternal(frame, range, null);
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object stepObject(VirtualFrame frame, DynamicObject range, NotProvided step, DynamicObject block) {
            return stepInternal(frame, range, block);
        }

        @Specialization(guards = { "isObjectRange(range)", "wasProvided(step)" })
        public Object step(VirtualFrame frame, DynamicObject range, Object step, NotProvided block) {
            return stepInternal(frame, range, step, null);
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToANode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode toAInternalCall;

        @Specialization(guards = "isIntRange(range)")
        public DynamicObject toA(DynamicObject range) {
            final int begin = Layouts.INT_RANGE.getBegin(range);
            int result;
            if (Layouts.INT_RANGE.getExcludedEnd(range)) {
                result = Layouts.INT_RANGE.getEnd(range);
            } else {
                result = Layouts.INT_RANGE.getEnd(range) + 1;
            }
            final int length = result - begin;

            if (length < 0) {
                return createArray(null, 0);
            } else {
                final int[] values = new int[length];

                for (int n = 0; n < length; n++) {
                    values[n] = begin + n;
                }

                return createArray(values, length);
            }
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object toA(VirtualFrame frame, DynamicObject range) {
            if (toAInternalCall == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toAInternalCall = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            return toAInternalCall.call(frame, range, "to_a_internal");
        }

    }

    // These 3 nodes replace ivar assignment in the common/range.rb Range#initialize
    @NonStandard
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "begin")
    })
    public abstract static class InternalSetBeginNode extends RubyNode {

        @Specialization(guards = "isObjectRange(range)")
        public Object setBegin(DynamicObject range, Object begin) {
            Layouts.OBJECT_RANGE.setBegin(range, begin);
            return begin;
        }
    }

    @NonStandard
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "end")
    })
    public abstract static class InternalSetEndNode extends RubyNode {

        @Specialization(guards = "isObjectRange(range)")
        public Object setEnd(DynamicObject range, Object end) {
            Layouts.OBJECT_RANGE.setEnd(range, end);
            return end;
        }
    }

    @NonStandard
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "excludeEnd")
    })
    public abstract static class InternalSetExcludeEndNode extends RubyNode {

        @CreateCast("excludeEnd")
        public RubyNode castToBoolean(RubyNode excludeEnd) {
            return BooleanCastNodeGen.create(excludeEnd);
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean setExcludeEnd(DynamicObject range, boolean excludeEnd) {
            Layouts.OBJECT_RANGE.setExcludedEnd(range, excludeEnd);
            return excludeEnd;
        }

    }

    @CoreMethod(names = "new", constructor = true, required = 2, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "rubyClass"),
            @NodeChild(type = RubyNode.class, value = "begin"),
            @NodeChild(type = RubyNode.class, value = "end"),
            @NodeChild(type = RubyNode.class, value = "excludeEnd")
    })
    public abstract static class NewNode extends CoreMethodNode {

        protected final DynamicObject rangeClass = getContext().getCoreLibrary().getRangeClass();

        @Child private CallDispatchHeadNode cmpNode;
        @Child private AllocateObjectNode allocateNode;

        @CreateCast("excludeEnd")
        public RubyNode coerceToBoolean(RubyNode excludeEnd) {
            return BooleanCastWithDefaultNodeGen.create(false, excludeEnd);
        }

        @Specialization(guards = "rubyClass == rangeClass")
        public DynamicObject intRange(DynamicObject rubyClass, int begin, int end, boolean excludeEnd) {
            return Layouts.INT_RANGE.createIntRange(
                    coreLibrary().getIntRangeFactory(),
                    excludeEnd,
                    begin,
                    end);
        }

        @Specialization(guards = { "rubyClass == rangeClass", "fitsIntoInteger(begin)", "fitsIntoInteger(end)" })
        public DynamicObject longFittingIntRange(DynamicObject rubyClass, long begin, long end, boolean excludeEnd) {
            return Layouts.INT_RANGE.createIntRange(
                    coreLibrary().getIntRangeFactory(),
                    excludeEnd,
                    (int) begin,
                    (int) end);
        }

        @Specialization(guards = { "rubyClass == rangeClass", "!fitsIntoInteger(begin) || !fitsIntoInteger(end)" })
        public DynamicObject longRange(DynamicObject rubyClass, long begin, long end, boolean excludeEnd) {
            return Layouts.LONG_RANGE.createLongRange(
                    coreLibrary().getLongRangeFactory(),
                    excludeEnd,
                    begin,
                    end);
        }

        @Specialization(guards = { "rubyClass != rangeClass || (!isIntOrLong(begin) || !isIntOrLong(end))" })
        public Object objectRange(
                VirtualFrame frame,
                DynamicObject rubyClass,
                Object begin,
                Object end,
                boolean excludeEnd) {
            if (cmpNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cmpNode = insert(DispatchHeadNodeFactory.createMethodCall());
            }
            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                allocateNode = insert(AllocateObjectNode.create());
            }

            final Object cmpResult;
            try {
                cmpResult = cmpNode.call(frame, begin, "<=>", end);
            } catch (RaiseException e) {
                throw new RaiseException(coreExceptions().argumentError("bad value for range", this));
            }

            if (cmpResult == nil()) {
                throw new RaiseException(coreExceptions().argumentError("bad value for range", this));
            }

            return allocateNode.allocate(rubyClass, excludeEnd, begin, end);
        }

        protected boolean fitsIntoInteger(long value) {
            return CoreLibrary.fitsIntoInteger(value);
        }

        protected boolean isIntOrLong(Object value) {
            return RubyGuards.isInteger(value) || RubyGuards.isLong(value);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, false, nil(), nil());
        }

    }

}
