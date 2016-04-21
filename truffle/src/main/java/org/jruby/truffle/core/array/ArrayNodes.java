/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.CoreSourceSection;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayNodesFactory.MaxBlockNodeFactory;
import org.jruby.truffle.core.array.ArrayNodesFactory.MinBlockNodeFactory;
import org.jruby.truffle.core.array.ArrayNodesFactory.ReplaceNodeFactory;
import org.jruby.truffle.core.cast.ToAryNodeGen;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.format.BytesResult;
import org.jruby.truffle.core.format.FormatExceptionTranslator;
import org.jruby.truffle.core.format.exceptions.FormatException;
import org.jruby.truffle.core.format.pack.PackCompiler;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.locals.LocalVariableType;
import org.jruby.truffle.language.locals.ReadDeclarationVariableNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.language.yield.YieldNode;
import org.jruby.util.Memo;
import java.util.Arrays;
import static org.jruby.truffle.core.array.ArrayHelpers.createArray;
import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.getStore;
import static org.jruby.truffle.core.array.ArrayHelpers.setStoreAndSize;

@CoreClass(name = "Array")
public abstract class ArrayNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, null, 0);
        }

    }

    @CoreMethod(names = "+", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "a"),
        @NodeChild(type = RubyNode.class, value = "b")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class AddNode extends CoreMethodNode {

        @CreateCast("b") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(null, null, other);
        }

        // One array has null storage, just copy the other.

        @Specialization(guards = { "isNullArray(a)", "isNullArray(b)" })
        public DynamicObject addNullNull(DynamicObject a, DynamicObject b) {
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = { "isNullArray(a)", "!isNullArray(b)", "strategy.matches(b)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addNullOther(DynamicObject a, DynamicObject b,
                @Cached("of(b)") ArrayStrategy strategy) {
            final int size = getSize(b);
            final ArrayMirror mirror = strategy.newMirror(b).extractRange(0, size);
            return createArray(getContext(), mirror.getArray(), size);
        }

        @Specialization(guards = { "!isNullArray(a)", "isNullArray(b)", "strategy.matches(a)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addOtherNull(DynamicObject a, DynamicObject b,
                @Cached("of(a)") ArrayStrategy strategy) {
            final int size = getSize(a);
            final ArrayMirror mirror = strategy.newMirror(a).extractRange(0, size);
            return createArray(getContext(), mirror.getArray(), size);
        }

        // Same storage

        @Specialization(guards = { "strategy.matches(a)", "strategy.matches(b)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addSameType(DynamicObject a, DynamicObject b,
                @Cached("of(a)") ArrayStrategy strategy) {
            final int aSize = getSize(a);
            final int bSize = getSize(b);
            final int combinedSize = aSize + bSize;
            final ArrayMirror mirror = strategy.newArray(combinedSize);
            strategy.newMirror(a).copyTo(mirror, 0, 0, aSize);
            strategy.newMirror(b).copyTo(mirror, 0, aSize, bSize);
            return createArray(getContext(), mirror.getArray(), combinedSize);
        }

        // Generalizations

        @Specialization(guards = { "aStrategy.matches(a)", "bStrategy.matches(b)", "aStrategy != bStrategy" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addGeneralize(DynamicObject a, DynamicObject b,
                @Cached("of(a)") ArrayStrategy aStrategy,
                @Cached("of(b)") ArrayStrategy bStrategy,
                @Cached("aStrategy.generalize(bStrategy)") ArrayStrategy generalized) {
            final int aSize = getSize(a);
            final int bSize = getSize(b);
            final int combinedSize = aSize + bSize;
            final ArrayMirror mirror = generalized.newArray(combinedSize);
            aStrategy.newMirror(a).copyTo(mirror, 0, 0, aSize);
            bStrategy.newMirror(b).copyTo(mirror, 0, aSize, bSize);
            return createArray(getContext(), mirror.getArray(), combinedSize);
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToIntNode toIntNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        protected abstract Object executeMul(VirtualFrame frame, DynamicObject array, int count);

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mulEmpty(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative argument", this));
            }
            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
        }

        @Specialization(guards = { "strategy.matches(array)", "!isNullArray(array)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject mulIntegerFixnum(DynamicObject array, int count,
                @Cached("of(array)") ArrayStrategy strategy) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative argument", this));
            }

            final int size = getSize(array);
            final int newSize = size * count;
            final ArrayMirror store = strategy.newMirror(array);
            final ArrayMirror newStore = strategy.newArray(newSize);
            for (int n = 0; n < count; n++) {
                store.copyTo(newStore, 0, n * size, size);
            }
            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), newStore.getArray(), newSize);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object mulObject(VirtualFrame frame, DynamicObject array, DynamicObject string) {
            return ruby("join(sep)", "sep", string);
        }

        @Specialization(guards = { "!isInteger(object)", "!isRubyString(object)" })
        public Object mulObjectCount(VirtualFrame frame, DynamicObject array, Object object) {
            if (respondToToStr(frame, object)) {
                return ruby("join(sep.to_str)", "sep", object);
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int count = toIntNode.doInt(frame, object);
                return executeMul(frame, array, count);
            }
        }

        public boolean respondToToStr(VirtualFrame frame, Object object) {
            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
            return respondToToStrNode.doesRespondToString(frame, object, create7BitString("to_str", UTF8Encoding.INSTANCE), false);
        }

    }

    @CoreMethod(names = { "[]", "slice" }, required = 1, optional = 1, lowerFixnumParameters = { 0, 1 })
    public abstract static class IndexNode extends ArrayCoreMethodNode {

        @Child protected ArrayReadDenormalizedNode readNode;
        @Child protected ArrayReadSliceDenormalizedNode readSliceNode;
        @Child protected ArrayReadSliceNormalizedNode readNormalizedSliceNode;
        @Child protected CallDispatchHeadNode fallbackNode;
        @Child protected AllocateObjectNode allocateObjectNode;

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public Object index(DynamicObject array, int index, NotProvided length) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }
            return readNode.executeRead(array, index);
        }

        @Specialization
        public DynamicObject slice(VirtualFrame frame, DynamicObject array, int start, int length) {
            if (length < 0) {
                return nil();
            }

            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            return readSliceNode.executeReadSlice(array, start, length);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public DynamicObject slice(VirtualFrame frame, DynamicObject array, DynamicObject range, NotProvided len,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile) {
            final int size = getSize(array);
            final int normalizedIndex = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range), negativeBeginProfile);

            if (normalizedIndex < 0 || normalizedIndex > size) {
                return nil();
            } else {
                final int end = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), negativeEndProfile);
                final int exclusiveEnd = ArrayOperations.clampExclusiveIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end : end + 1);

                if (exclusiveEnd <= normalizedIndex) {
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
                }

                final int length = exclusiveEnd - normalizedIndex;

                if (readNormalizedSliceNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    readNormalizedSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }

                return readNormalizedSliceNode.executeReadSlice(array, normalizedIndex, length);
            }
        }

        @Specialization(guards = { "!isInteger(a)", "!isIntegerFixnumRange(a)" })
        public Object fallbackIndex(VirtualFrame frame, DynamicObject array, Object a, NotProvided length) {
            Object[] objects = new Object[] { a };
            return fallback(frame, array, createArray(getContext(), objects, objects.length));
        }

        @Specialization(guards = { "!isIntegerFixnumRange(a)", "wasProvided(b)" })
        public Object fallbackSlice(VirtualFrame frame, DynamicObject array, Object a, Object b) {
            Object[] objects = new Object[] { a, b };
            return fallback(frame, array, createArray(getContext(), objects, objects.length));
        }

        public Object fallback(VirtualFrame frame, DynamicObject array, DynamicObject args) {
            if (fallbackNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            InternalMethod method = RubyArguments.getMethod(frame);
            return fallbackNode.call(frame, array, "element_reference_fallback", null,
                    createString(StringOperations.encodeRope(method.getName(), UTF8Encoding.INSTANCE)), args);
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        @Child private ArrayReadNormalizedNode readNode;
        @Child private ArrayWriteNormalizedNode writeNode;
        @Child protected ArrayReadSliceNormalizedNode readSliceNode;
        @Child private ToIntNode toIntNode;

        public abstract Object executeSet(VirtualFrame frame, DynamicObject array, Object index, Object length, Object value);

        // array[index] = object

        @Specialization
        public Object set(DynamicObject array, int index, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(getSize(array), index, negativeIndexProfile);
            checkIndex(array, index, normalizedIndex);
            return write(array, normalizedIndex, value);
        }

        // array[index] = object with non-int index

        @Specialization(guards = { "!isInteger(indexObject)", "!isIntegerFixnumRange(indexObject)" })
        public Object set(VirtualFrame frame, DynamicObject array, Object indexObject, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int index = toInt(frame, indexObject);
            return set(array, index, value, unused, negativeIndexProfile);
        }

        // array[start, end] = object

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "strategy.specializesFor(value)" }, limit = "ARRAY_STRATEGIES")
        public Object setObject(VirtualFrame frame, DynamicObject array, int start, int length, Object value,
                @Cached("forValue(value)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            checkLengthPositive(length);

            final int size = getSize(array);
            final int begin = ArrayOperations.normalizeIndex(size, start, negativeIndexProfile);
            checkIndex(array, start, begin);

            // Passing a non-array as value is the same as assigning a single-element array
            ArrayMirror mirror = strategy.newArray(1);
            mirror.set(0, value);
            DynamicObject ary = createArray(getContext(), mirror.getArray(), 1);
            return executeSet(frame, array, start, length, ary);
        }

        // array[start, end] = other_array, with length == other_array.size

        @Specialization(guards = {
                "isRubyArray(replacement)",
                "length == getArraySize(replacement)"
        })
        public Object setOtherIntArraySameLength(DynamicObject array, int start, int length, DynamicObject replacement,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(getSize(array), start, negativeIndexProfile);
            checkIndex(array, start, normalizedIndex);

            for (int i = 0; i < length; i++) {
                write(array, normalizedIndex + i, read(replacement, i));
            }
            return replacement;
        }

        // array[start, end] = other_array, with length != other_array.size

        @Specialization(guards = {
                "isRubyArray(replacement)",
                "length != getArraySize(replacement)"
        })
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, int rawStart, int length, DynamicObject replacement,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("createBinaryProfile()") ConditionProfile needCopy,
                @Cached("createBinaryProfile()") ConditionProfile recursive) {
            checkLengthPositive(length);
            final int start = ArrayOperations.normalizeIndex(getSize(array), rawStart, negativeIndexProfile);
            checkIndex(array, rawStart, start);

            final int end = start + length;
            final int arraySize = getSize(array);
            final int replacementSize = getSize(replacement);
            final int endOfReplacementInArray = start + replacementSize;

            if (recursive.profile(array == replacement)) {
                final DynamicObject copy = readSlice(array, 0, arraySize);
                return executeSet(frame, array, start, length, copy);
            }

            // Make a copy of what's after "end", as it might be erased or at least needs to be moved
            final int tailSize = arraySize - end;
            DynamicObject tailCopy = null;
            final boolean needsTail = needCopy.profile(tailSize > 0);
            if (needsTail) {
                tailCopy = readSlice(array, end, tailSize);
            }

            // Append the replacement array
            for (int i = 0; i < replacementSize; i++) {
                write(array, start + i, read(replacement, i));
            }

            // Append the saved tail
            if (needsTail) {
                for (int i = 0; i < tailSize; i++) {
                    write(array, endOfReplacementInArray + i, read(tailCopy, i));
                }
            }

            // Set size
            if (needsTail) {
                Layouts.ARRAY.setSize(array, endOfReplacementInArray + tailSize);
            } else {
                Layouts.ARRAY.setSize(array, endOfReplacementInArray);
            }

            return replacement;
        }

        // array[start, end] = object_or_array with non-int start or end

        @Specialization(guards = { "!isInteger(startObject) || !isInteger(lengthObject)", "wasProvided(value)" })
        public Object setStartLengthNotInt(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, Object value,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            int start = toInt(frame, startObject);
            int length = toInt(frame, lengthObject);
            return executeSet(frame, array, start, length, value);
        }

        // array[start..end] = object_or_array

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object setRange(VirtualFrame frame, DynamicObject array, DynamicObject range, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile) {
            final int size = getSize(array);
            final int begin = Layouts.INTEGER_FIXNUM_RANGE.getBegin(range);
            final int start = ArrayOperations.normalizeIndex(size, begin, negativeBeginProfile);
            if (start < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().rangeError(range, this));
            }
            final int end = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), negativeEndProfile);
            int inclusiveEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end - 1 : end;
            if (inclusiveEnd < 0) {
                inclusiveEnd = -1;
            }
            final int length = inclusiveEnd - start + 1;
            return executeSet(frame, array, start, length, value);
        }

        // Helpers

        private void checkIndex(DynamicObject array, int index, int normalizedIndex) {
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().indexTooSmallError("array", index, getSize(array), this));
            }
        }

        public void checkLengthPositive(int length) {
            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().negativeLengthError(length, this));
            }
        }

        protected int getArraySize(DynamicObject array) {
            return getSize(array);
        }

        private Object read(DynamicObject array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadNormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }
            return readNode.executeRead(array, index);
        }

        private Object write(DynamicObject array, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return writeNode.executeWrite(array, index, value);
        }

        private DynamicObject readSlice(DynamicObject array, int start, int length) {
            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return readSliceNode.executeReadSlice(array, start, length);
        }

        private int toInt(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            return toIntNode.doInt(frame, indexObject);
        }

    }

    @CoreMethod(names = "at", required = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "index")
    })
    public abstract static class AtNode extends CoreMethodNode {

        @Child private ArrayReadDenormalizedNode readNode;

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return FixnumLowerNodeGen.create(null, null,
                    ToIntNodeGen.create(null, null, index));
        }

        @Specialization
        public Object at(DynamicObject array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }
            return readNode.executeRead(array, index);
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends ArrayCoreMethodNode {

        @Specialization(guards = "isRubyArray(array)")
        public DynamicObject clear(DynamicObject array) {
            setStoreAndSize(array, null, 0);
            return array;
        }

    }

    @CoreMethod(names = "compact")
    @ImportStatic(ArrayGuards.class)
    public abstract static class CompactNode extends ArrayCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object compactNull(DynamicObject array) {
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = { "!isObjectArray(array)", "strategy.matches(array)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject compactPrimitive(DynamicObject array,
                @Cached("of(array)") ArrayStrategy strategy) {
            final int size = getSize(array);
            Object store = strategy.newMirror(array).extractRange(0, size).getArray();
            return createArray(getContext(), store, size);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(DynamicObject array) {
            // TODO CS 9-Feb-15 by removing nil we could make this array suitable for a primitive array storage class

            final Object[] store = (Object[]) getStore(array);
            final Object[] newStore = new Object[store.length];
            final int size = getSize(array);

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != nil()) {
                    newStore[m] = store[n];
                    m++;
                }
            }

            return createArray(getContext(), newStore, m);
        }

    }

    @CoreMethod(names = "compact!", raiseIfFrozenSelf = true)
    public abstract static class CompactBangNode extends ArrayCoreMethodNode {

        @Specialization(guards = "!isObjectArray(array)")
        public DynamicObject compactNotObjects(DynamicObject array) {
            return nil();
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(DynamicObject array) {
            final Object[] store = (Object[]) getStore(array);
            final int size = getSize(array);

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != nil()) {
                    store[m] = store[n];
                    m++;
                }
            }

            setStoreAndSize(array, store, m);

            if (m == size) {
                return nil();
            } else {
                return array;
            }
        }

    }

    @CoreMethod(names = "concat", required = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class ConcatNode extends CoreMethodNode {

        @Child private ArrayAppendManyNode appendManyNode;

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendManyNode = ArrayAppendManyNodeGen.create(context, sourceSection, null, null);
        }

        @CreateCast("other") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(null, null, other);
        }

        @Specialization
        public DynamicObject concat(DynamicObject array, DynamicObject other) {
            appendManyNode.executeAppendMany(array, other);
            return array;
        }

    }

    @CoreMethod(names = "delete", required = 1)
    public abstract static class DeleteNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;
        @Child private IsFrozenNode isFrozenNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(new RubyNode[]{null,null});
        }

        @Specialization(guards = "isNullArray(array)")
        public Object deleteNull(VirtualFrame frame, DynamicObject array, Object value) {
            return nil();
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object delete(VirtualFrame frame, DynamicObject array, Object value,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                final Object stored = store.get(n);

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    checkFrozen(array);
                    found = stored;
                    continue;
                }

                if (i != n) {
                    store.set(i, store.get(n));
                }

                i++;
            }

            if (i != n) {
                setStoreAndSize(array, store.getArray(), i);
            }
            return found;
        }

        public void checkFrozen(Object object) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreter();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
            }
            isFrozenNode.raiseIfFrozen(object);
        }

    }

    @CoreMethod(names = "delete_at", required = 1, raiseIfFrozenSelf = true, lowerFixnumParameters = 0)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "index")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteAtNode extends CoreMethodNode {

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return ToIntNodeGen.create(null, null, index);
        }

        @Specialization(guards = "isEmptyArray(array)")
        public Object deleteAtNullOrEmpty(DynamicObject array, int index) {
            return nil();
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object deleteAt(DynamicObject array, int index,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("create()") BranchProfile notInBoundsProfile) {
            final int size = getSize(array);
            final int i = ArrayOperations.normalizeIndex(size, index, negativeIndexProfile);

            if (i < 0 || i >= size) {
                notInBoundsProfile.enter();
                return nil();
            } else {
                final ArrayMirror store = strategy.newMirror(array);
                final Object value = store.get(i);
                store.copyTo(store, i + 1, i, size - i - 1);
                setStoreAndSize(array, store.getArray(), size - 1);
                return value;
            }
        }

    }

    @CoreMethod(names = "each", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object eachNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object eachOther(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    yield(frame, block, store.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "each_with_index", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachWithIndexNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")

        public DynamicObject eachWithIndexNull(DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object eachWithIndexOther(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    yield(frame, block, store.get(n), n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "fill", rest = true, needsBlock = true, raiseIfFrozenSelf = true)
    public abstract static class FillNode extends ArrayCoreMethodNode {

        @Specialization(guards = { "args.length == 1", "strategy.matches(array)", "strategy.accepts(value(args))" }, limit = "ARRAY_STRATEGIES")
        protected DynamicObject fill(DynamicObject array, Object[] args, NotProvided block,
                @Cached("of(array, value(args))") ArrayStrategy strategy) {
            final Object value = args[0];
            final ArrayMirror store = strategy.newMirror(array);
            final int size = getSize(array);
            for (int i = 0; i < size; i++) {
                store.set(i, value);
            }
            return array;
        }

        protected Object value(Object[] args) {
            return args[0];
        }

        @Specialization
        protected Object fillFallback(VirtualFrame frame, DynamicObject array, Object[] args, NotProvided block,
                @Cached("createMethodCall()") CallDispatchHeadNode callFillInternal) {
            return callFillInternal.call(frame, array, "fill_internal", null, args);
        }

        @Specialization
        protected Object fillFallback(VirtualFrame frame, DynamicObject array, Object[] args, DynamicObject block,
                @Cached("createMethodCall()") CallDispatchHeadNode callFillInternal) {
            return callFillInternal.call(frame, array, "fill_internal", block, args);
        }

    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(new RubyNode[]{null,null});
        }

        @Specialization(guards = "isNullArray(array)")
        public boolean includeNull(VirtualFrame frame, DynamicObject array, Object value) {
            return false;
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public boolean include(VirtualFrame frame, DynamicObject array, Object value,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            for (int n = 0; n < getSize(array); n++) {
                final Object stored = store.get(n);

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 2, raiseIfFrozenSelf = true, lowerFixnumParameters = 0)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeNode extends YieldingCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode toAryNode;
        @Child private KernelNodes.RespondToNode respondToToAryNode;
        
        public abstract DynamicObject executeInitialize(VirtualFrame frame, DynamicObject array, Object size, Object value, Object block);

        @Specialization
        public DynamicObject initializeNoArgs(DynamicObject array, NotProvided size, NotProvided unusedValue, NotProvided block) {
            setStoreAndSize(array, null, 0);
            return array;
        }

        @Specialization
        public DynamicObject initializeOnlyBlock(DynamicObject array, NotProvided size, NotProvided unusedValue, DynamicObject block) {
            setStoreAndSize(array, null, 0);
            return array;
        }

        @TruffleBoundary
        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegativeIntSize(DynamicObject array, int size, Object unusedValue, Object maybeBlock) {
            throw new RaiseException(coreExceptions().argumentError("negative array size", this));
        }

        @TruffleBoundary
        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegativeLongSize(DynamicObject array, long size, Object unusedValue, Object maybeBlock) {
            throw new RaiseException(coreExceptions().argumentError("negative array size", this));
        }

        protected static final long MAX_INT = Integer.MAX_VALUE;

        @TruffleBoundary
        @Specialization(guards = "size >= MAX_INT")
        public DynamicObject initializeSizeTooBig(DynamicObject array, long size, NotProvided unusedValue, NotProvided block) {
            throw new RaiseException(coreExceptions().argumentError("array size too big", this));
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initializeWithSizeNoValue(DynamicObject array, int size, NotProvided unusedValue, NotProvided block) {
            final Object[] store = new Object[size];
            Arrays.fill(store, nil());
            setStoreAndSize(array, store, size);
            return array;
        }

        @Specialization(guards = { "size >= 0", "wasProvided(value)", "strategy.specializesFor(value)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject initializeWithSizeAndValue(DynamicObject array, int size, Object value, NotProvided block,
                @Cached("forValue(value)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile needsFill) {
            final ArrayMirror store = strategy.newArray(size);
            if (needsFill.profile(size > 0 && store.get(0) != value)) {
                for (int i = 0; i < size; i++) {
                    store.set(i, value);
                }
            }
            setStoreAndSize(array, store.getArray(), size);
            return array;
        }

        @Specialization(guards = { "wasProvided(sizeObject)", "!isInteger(sizeObject)", "!isLong(sizeObject)", "wasProvided(value)" })
        public DynamicObject initializeSizeOther(VirtualFrame frame, DynamicObject array, Object sizeObject, Object value, NotProvided block) {
            int size = toInt(frame, sizeObject);
            return executeInitialize(frame, array, size, value, block);
        }

        // With block

        @Specialization(guards = "size >= 0")
        public Object initializeBlock(VirtualFrame frame, DynamicObject array, int size, Object unusedValue, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            Object store = arrayBuilder.start(size);

            int n = 0;
            try {
                for (; n < size; n++) {
                    store = arrayBuilder.appendValue(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
                setStoreAndSize(array, arrayBuilder.finish(store, n), n);
            }

            return array;
        }

        @Specialization(guards = "isRubyArray(copy)")
        public DynamicObject initializeFromArray(DynamicObject array, DynamicObject copy, NotProvided unusedValue, Object maybeBlock,
                @Cached("createReplaceNode()") ReplaceNode replaceNode) {
            replaceNode.executeReplace(array, copy);
            return array;
        }

        @Specialization(guards = { "!isInteger(object)", "!isLong(object)", "wasProvided(object)", "!isRubyArray(object)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject array, Object object, NotProvided unusedValue, NotProvided block) {
            DynamicObject copy = null;
            if (respondToToAry(frame, object)) {
                Object toAryResult = callToAry(frame, object);
                if (RubyGuards.isRubyArray(toAryResult)) {
                    copy = (DynamicObject) toAryResult;
                }
            }

            if (copy != null) {
                return executeInitialize(frame, array, copy, NotProvided.INSTANCE, NotProvided.INSTANCE);
            } else {
                int size = toInt(frame, object);
                return executeInitialize(frame, array, size, NotProvided.INSTANCE, NotProvided.INSTANCE);
            }
        }

        public boolean respondToToAry(VirtualFrame frame, Object object) {
            if (respondToToAryNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToAryNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
            return respondToToAryNode.doesRespondToString(frame, object, create7BitString("to_ary", UTF8Encoding.INSTANCE), true);
        }

        protected Object callToAry(VirtualFrame frame, Object object) {
            if (toAryNode == null) {
                CompilerDirectives.transferToInterpreter();
                toAryNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }
            return toAryNode.call(frame, object, "to_ary", null);
        }

        protected int toInt(VirtualFrame frame, Object value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            return toIntNode.doInt(frame, value);
        }

        protected ReplaceNode createReplaceNode() {
            return ReplaceNodeFactory.create(null, null);
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "from")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeCopyNode extends CoreMethodNode {

        @CreateCast("from") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(null, null, other);
        }

        @Specialization
        public DynamicObject initializeCopy(DynamicObject self, DynamicObject from,
                @Cached("createReplaceNode()") ReplaceNode replaceNode) {
            if (self == from) {
                return self;
            }
            replaceNode.executeReplace(self, from);
            return self;
        }

        protected ReplaceNode createReplaceNode() {
            return ReplaceNodeFactory.create(null, null);
        }

    }

    @CoreMethod(names = { "inject", "reduce" }, needsBlock = true, optional = 2)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode dispatch;

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.CALL_METHOD_MISSING);
        }

        // With block

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectEmptyArray(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return initial;
        }

        @Specialization(guards = "isEmptyArray(array)")
        public Object injectEmptyArrayNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = { "strategy.matches(array)", "!isEmptyArray(array)", "wasProvided(initial)" }, limit = "ARRAY_STRATEGIES")
        public Object injectWithInitial(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);
            return injectBlockHelper(frame, array, block, store, initial, 0);
        }

        @Specialization(guards = { "strategy.matches(array)", "!isEmptyArray(array)" }, limit = "ARRAY_STRATEGIES")
        public Object injectNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);
            return injectBlockHelper(frame, array, block, store, store.get(0), 1);
        }

        public Object injectBlockHelper(VirtualFrame frame, DynamicObject array, DynamicObject block, ArrayMirror store, Object initial, int start) {
            Object accumulator = initial;
            int n = start;
            try {
                for (; n < getSize(array); n++) {
                    accumulator = yield(frame, block, accumulator, store.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return accumulator;
        }

        // With Symbol

        @Specialization(guards = { "isRubySymbol(symbol)", "isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolEmptyArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return initial;
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isEmptyArray(array)" })
        public Object injectSymbolEmptyArrayNoInitial(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            return nil();
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "strategy.matches(array)", "!isEmptyArray(array)", "wasProvided(initial)" }, limit = "ARRAY_STRATEGIES")
        public Object injectSymbolWithInitial(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);
            return injectSymbolHelper(frame, array, symbol, store, initial, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "strategy.matches(array)", "!isEmptyArray(array)" }, limit = "ARRAY_STRATEGIES")
        public Object injectSymbolNoInitial(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);
            return injectSymbolHelper(frame, array, symbol, store, store.get(0), 1);
        }

        public Object injectSymbolHelper(VirtualFrame frame, DynamicObject array, DynamicObject symbol, ArrayMirror store, Object initial, int start) {
            Object accumulator = initial;
            int n = start;

            try {
                for (; n < getSize(array); n++) {
                    accumulator = dispatch.call(frame, accumulator, symbol, null, store.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }
            return accumulator;
        }

    }

    @CoreMethod(names = { "map", "collect" }, needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mapNull(DynamicObject array, DynamicObject block) {
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object map(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final ArrayMirror store = strategy.newMirror(array);
            final int size = getSize(array);
            Object mappedStore = arrayBuilder.start(size);

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    final Object mappedValue = yield(frame, block, store.get(n));
                    mappedStore = arrayBuilder.appendValue(mappedStore, n, mappedValue);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return createArray(getContext(), arrayBuilder.finish(mappedStore, size), size);
        }

    }

    @CoreMethod(names = { "map!", "collect!" }, needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child private ArrayWriteNormalizedNode writeNode;

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mapInPlaceNull(DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object map(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("createWriteNode()") ArrayWriteNormalizedNode writeNode) {
            final ArrayMirror store = strategy.newMirror(array);

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    writeNode.executeWrite(array, n, yield(frame, block, store.get(n)));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return array;
        }

        protected ArrayWriteNormalizedNode createWriteNode() {
            return ArrayWriteNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null);
        }

    }

    // TODO: move into Enumerable?

    @CoreMethod(names = "max", needsBlock = true)
    public abstract static class MaxNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MaxBlock maxBlock;

        public MaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            maxBlock = context.getCoreLibrary().getArrayMaxBlock();
        }

        @Specialization
        public Object max(VirtualFrame frame, DynamicObject array, NotProvided blockNotProvided) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> maximum = new Memo<>();

            final InternalMethod method = RubyArguments.getMethod(frame);
            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(
                    RubyArguments.pack(null, null, method, DeclarationContext.BLOCK, null, array, null, new Object[]{}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final DynamicObject block = ProcOperations.createRubyProc(coreLibrary().getProcFactory(), ProcType.PROC,
                    maxBlock.getSharedMethodInfo(), maxBlock.getCallTarget(), maxBlock.getCallTarget(),
                    maximumClosureFrame.materialize(), method, array, null);

            eachNode.call(frame, array, "each", block);

            if (maximum.get() == null) {
                return nil();
            } else {
                return maximum.get();
            }
        }

        @Specialization
        public Object max(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return ruby("array.max_internal(&block)", "array", array, "block", block);
        }

    }

    public abstract static class MaxBlockNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode compareNode;

        public MaxBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public DynamicObject max(VirtualFrame frame, Object maximumObject, Object value) {
            @SuppressWarnings("unchecked")
            final Memo<Object> maximum = (Memo<Object>) maximumObject;

            final Object current = maximum.get();

            if (current == null) {
                maximum.set(value);
            } else {
                final Object compared = compareNode.call(frame, value, "<=>", null, current);

                if (compared instanceof Integer) {
                    if ((int) compared > 0) {
                        maximum.set(value);
                    }
                } else {
                    CompilerDirectives.transferToInterpreter();
                    // Should be the actual type and object in this string - but this method should go away soon
                    throw new RaiseException(coreExceptions().argumentError("comparison of X with Y failed", this));
                }
            }

            return nil();
        }

    }

    public static class MaxBlock {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MaxBlock(RubyContext context) {
            final SourceSection sourceSection = CoreSourceSection.createCoreSourceSection("Array", "max");

            frameDescriptor = new FrameDescriptor(context.getCoreLibrary().getNilObject());
            frameSlot = frameDescriptor.addFrameSlot("maximum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "max", false, null, false, false, false);

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(context, sourceSection, null, sharedMethodInfo, MaxBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                                        new ReadDeclarationVariableNode(context, sourceSection, LocalVariableType.FRAME_LOCAL, 1, frameSlot),
                                        new ReadPreArgumentNode(0, MissingArgumentBehavior.RUNTIME_ERROR)
                                }), false));
        }

        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "min", needsBlock = true)
    public abstract static class MinNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MinBlock minBlock;

        public MinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            minBlock = context.getCoreLibrary().getArrayMinBlock();
        }

        @Specialization
        public Object min(VirtualFrame frame, DynamicObject array, NotProvided blockNotProvided) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> minimum = new Memo<>();

            final InternalMethod method = RubyArguments.getMethod(frame);
            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(
                    RubyArguments.pack(null, null, method, DeclarationContext.BLOCK, null, array, null, new Object[]{}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final DynamicObject block = ProcOperations.createRubyProc(coreLibrary().getProcFactory(), ProcType.PROC,
                    minBlock.getSharedMethodInfo(), minBlock.getCallTarget(), minBlock.getCallTarget(),
                    minimumClosureFrame.materialize(), method, array, null);

            eachNode.call(frame, array, "each", block);

            if (minimum.get() == null) {
                return nil();
            } else {
                return minimum.get();
            }
        }

        @Specialization
        public Object min(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return ruby("array.min_internal(&block)", "array", array, "block", block);
        }

    }

    public abstract static class MinBlockNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode compareNode;

        public MinBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        @Specialization
        public DynamicObject min(VirtualFrame frame, Object minimumObject, Object value) {
            @SuppressWarnings("unchecked")
            final Memo<Object> minimum = (Memo<Object>) minimumObject;

            final Object current = minimum.get();

            if (current == null) {
                minimum.set(value);
            } else {
                final Object compared = compareNode.call(frame, value, "<=>", null, current);

                if (compared instanceof Integer) {
                    if ((int) compared < 0) {
                        minimum.set(value);
                    }
                } else {
                    CompilerDirectives.transferToInterpreter();
                    // Should be the actual type and object in this string - but this method should go away soon
                    throw new RaiseException(coreExceptions().argumentError("comparison of X with Y failed", this));
                }
            }

            return nil();
        }

    }

    public static class MinBlock {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MinBlock(RubyContext context) {
            final SourceSection sourceSection = CoreSourceSection.createCoreSourceSection("Array", "min");

            frameDescriptor = new FrameDescriptor(context.getCoreLibrary().getNilObject());
            frameSlot = frameDescriptor.addFrameSlot("minimum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "min", false, null, false, false, false);

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(context, sourceSection, null, sharedMethodInfo, MinBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                                        new ReadDeclarationVariableNode(context, sourceSection, LocalVariableType.FRAME_LOCAL, 1, frameSlot),
                                        new ReadPreArgumentNode(0, MissingArgumentBehavior.RUNTIME_ERROR)
                                }), false));
        }

        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "pack", required = 1, taintFromParameter = 0)
    @ImportStatic(StringCachingGuards.class)
    public abstract static class PackNode extends ArrayCoreMethodNode {

        @Child private RopeNodes.MakeLeafRopeNode makeLeafRopeNode;
        @Child private TaintNode taintNode;

        private final BranchProfile exceptionProfile = BranchProfile.create();
        private final ConditionProfile resizeProfile = ConditionProfile.createBinaryProfile();

        @Specialization(
                guards = {
                        "isRubyString(format)",
                        "ropesEqual(format, cachedFormat)"
                },
                limit = "getCacheLimit()"
        )
        public DynamicObject packCached(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject format,
                @Cached("privatizeRope(format)") Rope cachedFormat,
                @Cached("ropeLength(cachedFormat)") int cachedFormatLength,
                @Cached("create(compileFormat(format))") DirectCallNode callPackNode) {
            final BytesResult result;

            try {
                result = (BytesResult) callPackNode.call(frame,
                        new Object[] { getStore(array), getSize(array) });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishPack(cachedFormatLength, result);
        }

        @Specialization(contains = "packCached", guards = "isRubyString(format)")
        public DynamicObject packUncached(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject format,
                @Cached("create()") IndirectCallNode callPackNode) {
            final BytesResult result;

            try {
                result = (BytesResult) callPackNode.call(frame, compileFormat(format),
                        new Object[] { getStore(array), getSize(array) });
            } catch (FormatException e) {
                exceptionProfile.enter();
                throw FormatExceptionTranslator.translate(this, e);
            }

            return finishPack(Layouts.STRING.getRope(format).byteLength(), result);
        }

        private DynamicObject finishPack(int formatLength, BytesResult result) {
            byte[] bytes = result.getOutput();

            if (resizeProfile.profile(bytes.length != result.getOutputLength())) {
                bytes = Arrays.copyOf(bytes, result.getOutputLength());
            }

            if (makeLeafRopeNode == null) {
                CompilerDirectives.transferToInterpreter();
                makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null));
            }

            final DynamicObject string = createString(makeLeafRopeNode.executeMake(
                    bytes,
                    result.getEncoding().getEncodingForLength(formatLength),
                    result.getStringCodeRange(),
                    result.getStringLength()));

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
                }

                taintNode.executeTaint(string);
            }

            return string;
        }

        @Specialization(guards = {
                "!isRubyString(format)",
                "!isBoolean(format)",
                "!isInteger(format)",
                "!isLong(format)",
                "!isNil(format)"
        })
        public Object pack(DynamicObject array, Object format) {
            return ruby("pack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            return new PackCompiler(getContext(), this).compile(format.toString());
        }

        protected int getCacheLimit() {
            return getContext().getOptions().PACK_CACHE;
        }

    }

    @CoreMethod(names = "pop", raiseIfFrozenSelf = true, optional = 1)
    public abstract static class PopNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private ArrayPopOneNode popOneNode;

        @Specialization
        public Object pop(DynamicObject array, NotProvided n) {
            if (popOneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                popOneNode = insert(ArrayPopOneNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return popOneNode.executePopOne(array);
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object popNilWithNum(VirtualFrame frame, DynamicObject array, Object object) {
            if (object instanceof Integer && ((Integer) object) < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int n = toIntNode.doInt(frame, object);
                if (n < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().argumentError("negative array size", this));
                }
            }
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popLongFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object popLongFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popFloatInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(contains = "popFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object popFloatWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object popObjectWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject popIntegerFixnumInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNumObj", guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popIntegerFixnumWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject popLongFixnumInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popLongFixnumInBoundsWithNumObj", guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popLongFixnumWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;            }
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject popFloatInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(contains = "popFloatInBoundsWithNumObj", guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popFloatWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(guards = { "isObjectArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popObjectWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStoreAndSize(array, store, getSize(array) - numPop);
                return result;
            }
        }


    }

    @CoreMethod(names = "<<", raiseIfFrozenSelf = true, required = 1)
    public abstract static class LeftShiftNode extends ArrayCoreMethodNode {

        @Child private ArrayAppendOneNode appendOneNode;

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendOneNode = ArrayAppendOneNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject leftShift(DynamicObject array, Object value) {
            return appendOneNode.executeAppendOne(array, value);
        }

    }

    @CoreMethod(names = { "push", "__append__" }, rest = true, optional = 1, raiseIfFrozenSelf = true)
    public abstract static class PushNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        @Specialization(guards = { "isNullArray(array)", "values.length == 0" })
        public DynamicObject pushNullEmptySingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            setStoreAndSize(array, new int[] { value }, 1);
            return array;
        }

        @Specialization(guards = { "isNullArray(array)", "values.length == 0" })
        public DynamicObject pushNullEmptySingleIntegerLong(DynamicObject array, long value, Object[] values) {
            setStoreAndSize(array, new long[] { value }, 1);
            return array;
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject pushNullEmptyObjects(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.getArguments(frame);
            setStoreAndSize(array, values, values.length);
            return array;
        }

        @Specialization(guards = { "!isNullArray(array)", "isEmptyArray(array)" })
        public DynamicObject pushEmptySingleIntegerFixnum(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 20-Apr-15 in reality might be better reusing any current storage, but won't worry about that for now
            final Object[] values = RubyArguments.getArguments(frame);
            setStoreAndSize(array, values, values.length);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "values.length == 0" })
        public DynamicObject pushIntegerFixnumSingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, newSize));
            }

            store[oldSize] = value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "wasProvided(value)", "values.length == 0", "!isInteger(value)", "!isLong(value)" })
        public DynamicObject pushIntegerFixnumSingleOther(DynamicObject array, Object value, Object[] values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            int[] oldStore = (int[]) getStore(array);
            final Object[] store;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(getContext(), oldStore.length, newSize) - oldStore.length);
            } else {
                store = ArrayUtils.box(oldStore);
            }

            store[oldSize] = value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "wasProvided(value)", "rest.length != 0" })
        public DynamicObject pushIntegerFixnum(VirtualFrame frame, DynamicObject array, Object value, Object[] rest) {
            final Object[] values = RubyArguments.getArguments(frame);

            final int oldSize = getSize(array);
            final int newSize = oldSize + values.length;

            int[] oldStore = (int[]) getStore(array);
            final Object[] store;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(getContext(), oldStore.length, newSize) - oldStore.length);
            } else {
                store = ArrayUtils.box(oldStore);
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isLongArray(array)", "values.length == 0" })
        public DynamicObject pushLongFixnumSingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, newSize));
            }

            store[oldSize] = (long) value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isLongArray(array)", "values.length == 0" })
        public DynamicObject pushLongFixnumSingleLongFixnum(DynamicObject array, long value, Object[] values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, newSize));
            }

            store[oldSize] = value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject pushFloat(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store
            if (getSize(array) != 0) {
                throw new UnsupportedOperationException();
            }

            final Object[] values = RubyArguments.getArguments(frame);
            setStoreAndSize(array, values, values.length);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject pushObject(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.getArguments(frame);

            final int oldSize = getSize(array);
            final int newSize = oldSize + values.length;

            Object[] store = (Object[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.grow(store, ArrayUtils.capacity(getContext(), store.length, newSize));
            }
            ;
            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            setStoreAndSize(array, store, newSize);
            return array;
        }

    }

    // Not really a core method - used internally

    public abstract static class PushOneNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject pushEmpty(DynamicObject array, Object value) {
            setStoreAndSize(array, new Object[] { value }, 1);
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public DynamicObject pushIntegerFixnumIntegerFixnum(DynamicObject array, int value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                Object store1 = store = Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, newSize));
                setStoreAndSize(array, store1, getSize(array));
            }

            store[oldSize] = value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(value)" })
        public DynamicObject pushIntegerFixnumObject(DynamicObject array, Object value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            final int[] oldStore = (int[]) getStore(array);
            final Object[] newStore;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                newStore = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(getContext(), oldStore.length, newSize) - oldStore.length);
            } else {
                newStore = ArrayUtils.box(oldStore);
            }

            newStore[oldSize] = value;
            setStoreAndSize(array, newStore, newSize);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject pushObjectObject(DynamicObject array, Object value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                Object store1 = store = ArrayUtils.grow(store, ArrayUtils.capacity(getContext(), store.length, newSize));
                setStoreAndSize(array, store1, getSize(array));
            }

            store[oldSize] = value;
            setStoreAndSize(array, store, newSize);
            return array;
        }

    }

    @CoreMethod(names = "reject", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final Object[] store = (Object[]) getStore(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    CompilerDirectives.transferToInterpreter();

                    if (! yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return createArray(getContext(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final int[] store = (int[]) getStore(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    CompilerDirectives.transferToInterpreter();

                    if (! yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return createArray(getContext(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "delete_if" , needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteIfNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final int[] filler = new int[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final long[] filler = new long[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final double[] filler = new double[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final Object[] filler = new Object[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
            }
            return array;
        }

    }


    @CoreMethod(names = "reject!", needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final int[] filler = new int[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final long[] filler = new long[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final double[] filler = new double[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) getStore(array);

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if (i != n) {
                final Object[] filler = new Object[n - i];
                System.arraycopy(filler, 0, store, i, n - i);
                setStoreAndSize(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "replace", required = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "other")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class ReplaceNode extends CoreMethodNode {

        public abstract DynamicObject executeReplace(DynamicObject array, DynamicObject other);

        @CreateCast("other") public RubyNode coerceOtherToAry(RubyNode index) {
            return ToAryNodeGen.create(null, null, index);
        }

        @Specialization(guards = {"isRubyArray(other)", "isNullArray(other)"})
        public DynamicObject replace(DynamicObject array, DynamicObject other) {
            setStoreAndSize(array, null, 0);
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isIntArray(other)"})
        public DynamicObject replaceIntegerFixnum(DynamicObject array, DynamicObject other) {
            final int[] store = (int[]) getStore(other);
            setStoreAndSize(array, store.clone(), getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isLongArray(other)"})
        public DynamicObject replaceLongFixnum(DynamicObject array, DynamicObject other) {
            final long[] store = (long[]) getStore(other);
            setStoreAndSize(array, store.clone(), getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isDoubleArray(other)"})
        public DynamicObject replaceFloat(DynamicObject array, DynamicObject other) {
            final double[] store = (double[]) getStore(other);
            setStoreAndSize(array, store.clone(), getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isObjectArray(other)"})
        public DynamicObject replaceObject(DynamicObject array, DynamicObject other) {
            final Object[] store = (Object[]) getStore(other);
            setStoreAndSize(array, store.clone(), getSize(other));
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final Object[] store = (Object[]) getStore(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return createArray(getContext(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final int[] store = (int[]) getStore(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, count);
                }
            }

            return createArray(getContext(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true, optional = 1)
    public abstract static class ShiftNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;

        public abstract Object executeShift(VirtualFrame frame, DynamicObject array, Object n);

        @Specialization(guards = "isEmptyArray(array)")
        public Object shiftNil(VirtualFrame frame, DynamicObject array, NotProvided n) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public int shiftIntegerFixnumInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int[] store = ((int[]) getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftIntegerFixnumInBounds", guards = "isIntArray(array)")
        public Object shiftIntegerFixnum(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int[] store = ((int[]) getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public long shiftLongFixnumInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final long[] store = ((long[]) getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBounds", guards = "isLongArray(array)")
        public Object shiftLongFixnum(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final long[] store = ((long[]) getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public double shiftFloatInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final double[] store = ((double[]) getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftFloatInBounds", guards = "isDoubleArray(array)")
        public Object shiftFloat(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final double[] store = ((double[]) getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObject(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final Object[] store = ((Object[]) getStore(array));
                final Object value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final Object[] filler = new Object[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStoreAndSize(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object shiftNilWithNum(VirtualFrame frame, DynamicObject array, Object object) {
            if (object instanceof Integer && ((Integer) object) < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int n = toIntNode.doInt(frame, object);
                if (n < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().argumentError("negative array size", this));
                }
            }
            return createArray(getContext(), null, 0);
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftLongFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object shiftLongFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftFloatInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object shiftFloatWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObjectWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftIntegerFixnumInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftIntegerFixnumInBoundsWithNumObj", guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftIntegerFixnumWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftLongFixnumInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBoundsWithNumObj", guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftLongFixnumWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;          }
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftFloatInBoundsWithNumObj(VirtualFrame frame, DynamicObject array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftFloatInBoundsWithNumObj", guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftFloatWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isObjectArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftObjectWithNumObj(VirtualFrame frame, DynamicObject array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final DynamicObject result = createArray(getContext(), Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStoreAndSize(array, store, getSize(array) - numShift);
                return result;
            }
        }
    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends ArrayCoreMethodNode {

        @Specialization
        public int size(DynamicObject array) {
            return getSize(array);
        }

    }

    @CoreMethod(names = "sort", needsBlock = true)
    public abstract static class SortNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode compareDispatchNode;
        @Child private YieldNode yieldNode;

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = DispatchHeadNodeFactory.createMethodCall(context);
            yieldNode = new YieldNode(context);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject sortNull(DynamicObject array, Object unusedBlock) {
            return createArray(getContext(), null, 0);
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortIntegerFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final int[] store = (int[]) getStore(array);
            final int[] newStore = new int[store.length];

            final int size = getSize(array);

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < getContext().getOptions().ARRAY_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < getContext().getOptions().ARRAY_SMALL; j++) {
                        if (j < size) {
                            if (castSortValue(compareDispatchNode.call(frame, store[j], "<=>", null, store[i])) < 0) {
                                final int temp = store[j];
                                store[j] = store[i];
                                store[i] = temp;
                            }
                        }
                    }
                    newStore[i] = store[i];
                }
            }

            return createArray(getContext(), newStore, size);
        }

        @ExplodeLoop
        @Specialization(guards = {"isLongArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortLongFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final long[] store = (long[]) getStore(array);
            final long[] newStore = new long[store.length];

            final int size = getSize(array);

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < getContext().getOptions().ARRAY_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < getContext().getOptions().ARRAY_SMALL; j++) {
                        if (j < size) {
                            if (castSortValue(compareDispatchNode.call(frame, store[j], "<=>", null, store[i])) < 0) {
                                final long temp = store[j];
                                store[j] = store[i];
                                store[i] = temp;
                            }
                        }
                    }
                    newStore[i] = store[i];
                }
            }

            return createArray(getContext(), newStore, size);
        }

        @Specialization(guards = {"isObjectArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortObject(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final Object[] oldStore = (Object[]) getStore(array);
            final Object[] store = ArrayUtils.copy(oldStore);

            // Insertion sort

            final int size = getSize(array);

            for (int i = 1; i < size; i++) {
                final Object x = store[i];
                int j = i;
                // TODO(CS): node for this cast
                while (j > 0 && castSortValue(compareDispatchNode.call(frame, store[j - 1], "<=>", null, x)) > 0) {
                    store[j] = store[j - 1];
                    j--;
                }
                store[j] = x;
            }

            return createArray(getContext(), store, size);
        }

        public static final String SNIPPET = "sorted = dup; Rubinius.privately { sorted.isort_block!(0, right, block) }; sorted";
        public static final String RIGHT = "right";
        public static final String BLOCK = "block";

        @Specialization(guards = { "!isNullArray(array)" })
        public Object sortUsingRubinius(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject block,
                @Cached("new(SNIPPET, RIGHT, BLOCK)") SnippetNode snippet) {
            return snippet.execute(frame, getSize(array), block);
        }

        @Specialization(guards = { "!isNullArray(array)", "!isSmall(array)" })
        public Object sortUsingRubinius(VirtualFrame frame, DynamicObject array, NotProvided block) {
            return ruby("sorted = dup; Rubinius.privately { sorted.isort!(0, right) }; sorted", "right", getSize(array));
        }

        private int castSortValue(Object value) {
            if (value instanceof Integer) {
                return (int) value;
            }

            CompilerDirectives.transferToInterpreter();

            // TODO CS 14-Mar-15 - what's the error message here?
            throw new RaiseException(coreExceptions().argumentError("expecting a Fixnum to sort", this));
        }

        protected boolean isSmall(DynamicObject array) {
            return getSize(array) <= getContext().getOptions().ARRAY_SMALL;
        }

    }

    @CoreMethod(names = "unshift", rest = true, raiseIfFrozenSelf = true)
    public abstract static class UnshiftNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject unshift(DynamicObject array, Object... args) {
            CompilerDirectives.transferToInterpreter();

            assert RubyGuards.isRubyArray(array);
            final Object[] newStore = new Object[getSize(array) + args.length];
            System.arraycopy(args, 0, newStore, 0, args.length);
            ArrayUtils.copy(getStore(array), newStore, args.length, getSize(array));
            setStoreAndSize(array, newStore, newStore.length);
            return array;
        }

    }

    @CoreMethod(names = "zip", rest = true, required = 1, needsBlock = true)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode zipInternalCall;

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isIntArray(other)", "others.length == 0" })
        public DynamicObject zipObjectIntegerFixnum(DynamicObject array, DynamicObject other, Object[] others, NotProvided block,
                @Cached("createBinaryProfile()") ConditionProfile sameLengthProfile) {
            final Object[] a = (Object[]) getStore(array);

            final int[] b = (int[]) getStore(other);
            final int bLength = getSize(other);

            final int zippedLength = getSize(array);
            final Object[] zipped = new Object[zippedLength];

            if (sameLengthProfile.profile(zippedLength == bLength)) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = createArray(getContext(), new Object[] { a[n], b[n] }, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = createArray(getContext(), new Object[] { a[n], b[n] }, 2);
                    } else {
                        zipped[n] = createArray(getContext(), new Object[] { a[n], nil() }, 2);
                    }
                }
            }

            return createArray(getContext(), zipped, zippedLength);
        }

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isObjectArray(other)", "others.length == 0" })
        public DynamicObject zipObjectObject(DynamicObject array, DynamicObject other, Object[] others, NotProvided block,
                @Cached("createBinaryProfile()") ConditionProfile sameLengthProfile) {
            final Object[] a = (Object[]) getStore(array);

            final Object[] b = (Object[]) getStore(other);
            final int bLength = getSize(other);

            final int zippedLength = getSize(array);
            final Object[] zipped = new Object[zippedLength];

            if (sameLengthProfile.profile(zippedLength == bLength)) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = createArray(getContext(), new Object[] { a[n], b[n] }, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = createArray(getContext(), new Object[] { a[n], b[n] }, 2);
                    } else {
                        zipped[n] = createArray(getContext(), new Object[] { a[n], nil() }, 2);
                    }
                }
            }


            return createArray(getContext(), zipped, zippedLength);
        }

        @Specialization(guards = { "isRubyArray(other)", "fallback(array, other, others)" })
        public Object zipObjectObjectNotSingleObject(VirtualFrame frame, DynamicObject array, DynamicObject other, Object[] others, NotProvided block) {
            return zipRuby(frame, array, null);
        }

        @Specialization(guards = { "!isRubyArray(other)" })
        public Object zipObjectObjectNotArray(VirtualFrame frame, DynamicObject array, DynamicObject other, Object[] others, NotProvided block) {
            return zipRuby(frame, array, null);
        }

        @Specialization
        public Object zipBlock(VirtualFrame frame, DynamicObject array, DynamicObject other, Object[] others, DynamicObject block) {
            return zipRuby(frame, array, block);
        }

        private Object zipRuby(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            if (zipInternalCall == null) {
                CompilerDirectives.transferToInterpreter();
                zipInternalCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object[] others = RubyArguments.getArguments(frame);

            return zipInternalCall.call(frame, array, "zip_internal", block, others);
        }

        protected static boolean fallback(DynamicObject array, DynamicObject other, Object[] others) {
            return !ArrayGuards.isObjectArray(array) || ArrayGuards.isNullArray(other) || ArrayGuards.isLongArray(other) || others.length > 0;
        }

    }

}
