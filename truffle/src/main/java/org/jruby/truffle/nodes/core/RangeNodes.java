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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.core.array.ArrayBuilderNode;
import org.jruby.truffle.nodes.core.array.ArrayNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;

@CoreClass(name = "Range")
public abstract class RangeNodes {

    @Layout
    public interface IntegerFixnumRangeLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createIntegerFixnumRangeShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createIntegerFixnumRange(DynamicObjectFactory factory, boolean excludedEnd, int begin, int end);

        boolean isIntegerFixnumRange(DynamicObject object);

        boolean getExcludedEnd(DynamicObject object);

        int getBegin(DynamicObject object);

        int getEnd(DynamicObject object);

    }

    public static final IntegerFixnumRangeLayout INTEGER_FIXNUM_RANGE_LAYOUT = IntegerFixnumRangeLayoutImpl.INSTANCE;

    @Layout
    public interface LongFixnumRangeLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createLongFixnumRangeShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createLongFixnumRange(DynamicObjectFactory factory, boolean excludedEnd, long begin, long end);

        boolean isLongFixnumRange(DynamicObject object);

        boolean getExcludedEnd(DynamicObject object);

        long getBegin(DynamicObject object);

        long getEnd(DynamicObject object);

    }

    public static final LongFixnumRangeLayout LONG_FIXNUM_RANGE_LAYOUT = LongFixnumRangeLayoutImpl.INSTANCE;

    @Layout
    public interface ObjectRangeLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createObjectRangeShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createObjectRange(DynamicObjectFactory factory, boolean excludedEnd, @Nullable Object begin, @Nullable Object end);

        boolean isObjectRange(DynamicObject object);

        boolean getExcludedEnd(DynamicObject object);

        void setExcludedEnd(DynamicObject object, boolean value);

        @Nullable
        Object getBegin(DynamicObject object);

        void setBegin(DynamicObject object, Object value);

        @Nullable
        Object getEnd(DynamicObject object);

        void setEnd(DynamicObject object, Object value);

    }

    public static final ObjectRangeLayout OBJECT_RANGE_LAYOUT = ObjectRangeLayoutImpl.INSTANCE;

    @CoreMethod(names = {"collect", "map"}, needsBlock = true, lowerFixnumSelf = true)
    public abstract static class CollectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public CollectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isIntegerFixnumRange(range)", "isRubyProc(block)"})
        public DynamicObject collect(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            final int begin = INTEGER_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range));
            int result;
            if (INTEGER_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
            } else {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
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
        public Object eachInt(VirtualFrame frame, DynamicObject range, DynamicObject block) {
            int result;
            if (INTEGER_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
            } else {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
            }
            final int exclusiveEnd = result;

            int count = 0;

            try {
                for (int n = INTEGER_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range)); n < exclusiveEnd; n++) {
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
            if (LONG_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                result = LONG_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
            } else {
                result = LONG_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
            }
            final long exclusiveEnd = result;

            int count = 0;

            try {
                for (long n = LONG_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range)); n < exclusiveEnd; n++) {
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
            return INTEGER_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public boolean excludeEndLong(DynamicObject range) {
            return LONG_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public boolean excludeEndObject(DynamicObject range) {
            return OBJECT_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range));
        }

    }

    @CoreMethod(names = "begin")
    public abstract static class BeginNode extends CoreMethodArrayArgumentsNode {

        public BeginNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public int eachInt(DynamicObject range) {
            return INTEGER_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long eachLong(DynamicObject range) {
            return LONG_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object eachObject(DynamicObject range) {
            return OBJECT_RANGE_LAYOUT.getBegin(((DynamicObject) range));
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
            OBJECT_RANGE_LAYOUT.setExcludedEnd(range, excludeEnd);
            OBJECT_RANGE_LAYOUT.setBegin(range, begin);
            OBJECT_RANGE_LAYOUT.setEnd(range, end);
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
            return INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isLongFixnumRange(range)")
        public long lastLong(DynamicObject range) {
            return LONG_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
        }

        @Specialization(guards = "isObjectRange(range)")
        public Object lastObject(DynamicObject range) {
            return OBJECT_RANGE_LAYOUT.getEnd(((DynamicObject) range));
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
                if (INTEGER_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                    result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
                } else {
                    result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
                }
                for (int n = INTEGER_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range)); n < result; n += step) {
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
                if (LONG_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                    result = LONG_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
                } else {
                    result = LONG_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
                }
                for (long n = LONG_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range)); n < result; n += step) {
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
            final int begin = INTEGER_FIXNUM_RANGE_LAYOUT.getBegin(((DynamicObject) range));
            int result;
            if (INTEGER_FIXNUM_RANGE_LAYOUT.getExcludedEnd(((DynamicObject) range))) {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range));
            } else {
                result = INTEGER_FIXNUM_RANGE_LAYOUT.getEnd(((DynamicObject) range)) + 1;
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
            OBJECT_RANGE_LAYOUT.setBegin(range, begin);
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
            OBJECT_RANGE_LAYOUT.setEnd(range, end);
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
            OBJECT_RANGE_LAYOUT.setExcludedEnd(range, excludeEnd);
            return excludeEnd;
        }

    }

    public static class RangeAllocator implements Allocator {

        @Override
        public DynamicObject allocate(RubyContext context, DynamicObject rubyClass, Node currentNode) {
            return OBJECT_RANGE_LAYOUT.createObjectRange(ModuleNodes.getModel(rubyClass).getFactory(), false, context.getCoreLibrary().getNilObject(), context.getCoreLibrary().getNilObject());
        }

    }
}
