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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.CoreMethodNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.core.Hashing;
import org.jruby.truffle.core.array.ArrayNodesFactory.RejectInPlaceNodeFactory;
import org.jruby.truffle.core.array.ArrayNodesFactory.ReplaceNodeFactory;
import org.jruby.truffle.core.cast.ToAryNodeGen;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.cast.ToIntRangeNode;
import org.jruby.truffle.core.format.BytesResult;
import org.jruby.truffle.core.format.FormatExceptionTranslator;
import org.jruby.truffle.core.format.exceptions.FormatException;
import org.jruby.truffle.core.format.pack.PackCompiler;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.numeric.FixnumLowerNodeGen;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.IsFrozenNode;
import org.jruby.truffle.language.objects.IsFrozenNodeGen;
import org.jruby.truffle.language.objects.TaintNode;
import org.jruby.truffle.language.objects.TaintNodeGen;
import org.jruby.truffle.language.yield.YieldNode;

import java.util.Arrays;

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.getStore;
import static org.jruby.truffle.core.array.ArrayHelpers.setSize;
import static org.jruby.truffle.core.array.ArrayHelpers.setStoreAndSize;

@CoreClass("Array")
public abstract class ArrayNodes {

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNode.create();
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
            return createArray(null, 0);
        }

        @Specialization(guards = { "isNullArray(a)", "!isNullArray(b)", "strategy.matches(b)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addNullOther(DynamicObject a, DynamicObject b,
                @Cached("of(b)") ArrayStrategy strategy) {
            final int size = getSize(b);
            final ArrayMirror mirror = strategy.newMirror(b).extractRange(0, size);
            return createArray(mirror.getArray(), size);
        }

        @Specialization(guards = { "!isNullArray(a)", "isNullArray(b)", "strategy.matches(a)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject addOtherNull(DynamicObject a, DynamicObject b,
                @Cached("of(a)") ArrayStrategy strategy) {
            final int size = getSize(a);
            final ArrayMirror mirror = strategy.newMirror(a).extractRange(0, size);
            return createArray(mirror.getArray(), size);
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
            return createArray(mirror.getArray(), combinedSize);
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
            return createArray(mirror.getArray(), combinedSize);
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnum = 1, taintFrom = 0)
    public abstract static class MulNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToIntNode toIntNode;
        @Child private AllocateObjectNode allocateObjectNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        protected abstract Object executeMul(VirtualFrame frame, DynamicObject array, int count);

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mulEmpty(DynamicObject array, int count,
                @Cached("create()") BranchProfile errorProfile) {
            if (count < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentError("negative argument", this));
            }
            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
        }

        @Specialization(guards = { "strategy.matches(array)", "!isNullArray(array)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject mulOther(DynamicObject array, int count,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("create()") BranchProfile errorProfile) {
            if (count < 0) {
                errorProfile.enter();
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
        public Object mulObject(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject string,
                @Cached("createMethodCall()") CallDispatchHeadNode callNode) {
            return callNode.call(frame, array, "join", string);
        }

        @Specialization(guards = { "!isInteger(object)", "!isRubyString(object)" })
        public Object mulObjectCount(
                VirtualFrame frame,
                DynamicObject array,
                Object object,
                @Cached("new()") SnippetNode snippetNode) {
            if (respondToToStr(frame, object)) {
                return snippetNode.execute(frame, "join(sep.to_str)", "sep", object);
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    toIntNode = insert(ToIntNode.create());
                }
                final int count = toIntNode.doInt(frame, object);
                return executeMul(frame, array, count);
            }
        }

        public boolean respondToToStr(VirtualFrame frame, Object object) {
            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), null, null, null, null));
            }
            return respondToToStrNode.doesRespondToString(frame, object, create7BitString("to_str", UTF8Encoding.INSTANCE), false);
        }

    }

    @CoreMethod(names = { "[]", "slice" }, required = 1, optional = 1, lowerFixnum = { 1, 2 })
    public abstract static class IndexNode extends ArrayCoreMethodNode {

        @Child protected ArrayReadDenormalizedNode readNode;
        @Child protected ArrayReadSliceDenormalizedNode readSliceNode;
        @Child protected ArrayReadSliceNormalizedNode readNormalizedSliceNode;
        @Child protected CallDispatchHeadNode fallbackNode;
        @Child protected AllocateObjectNode allocateObjectNode;

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateObjectNode = AllocateObjectNode.create();
        }

        @Specialization
        public Object index(DynamicObject array, int index, NotProvided length) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), null, null, null));
            }
            return readNode.executeRead(array, index);
        }

        @Specialization
        public DynamicObject slice(VirtualFrame frame, DynamicObject array, int start, int length) {
            if (length < 0) {
                return nil();
            }

            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), null, null, null, null));
            }

            return readSliceNode.executeReadSlice(array, start, length);
        }

        @Specialization(guards = "isIntRange(range)")
        public DynamicObject slice(VirtualFrame frame, DynamicObject array, DynamicObject range, NotProvided len,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile) {
            final int size = getSize(array);
            final int normalizedIndex = ArrayOperations.normalizeIndex(size, Layouts.INT_RANGE.getBegin(range), negativeBeginProfile);

            if (normalizedIndex < 0 || normalizedIndex > size) {
                return nil();
            } else {
                final int end = ArrayOperations.normalizeIndex(size, Layouts.INT_RANGE.getEnd(range), negativeEndProfile);
                final int exclusiveEnd = ArrayOperations.clampExclusiveIndex(size, Layouts.INT_RANGE.getExcludedEnd(range) ? end : end + 1);

                if (exclusiveEnd <= normalizedIndex) {
                    return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
                }

                final int length = exclusiveEnd - normalizedIndex;

                if (readNormalizedSliceNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    readNormalizedSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), null, null, null, null));
                }

                return readNormalizedSliceNode.executeReadSlice(array, normalizedIndex, length);
            }
        }

        @Specialization(guards = { "!isInteger(a)", "!isIntRange(a)" })
        public Object fallbackIndex(VirtualFrame frame, DynamicObject array, Object a, NotProvided length) {
            Object[] objects = new Object[] { a };
            return fallback(frame, array, createArray(objects, objects.length));
        }

        @Specialization(guards = { "!isIntRange(a)", "wasProvided(b)" })
        public Object fallbackSlice(VirtualFrame frame, DynamicObject array, Object a, Object b) {
            Object[] objects = new Object[] { a, b };
            return fallback(frame, array, createArray(objects, objects.length));
        }

        public Object fallback(VirtualFrame frame, DynamicObject array, DynamicObject args) {
            if (fallbackNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                fallbackNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            InternalMethod method = RubyArguments.getMethod(frame);
            return fallbackNode.call(frame, array, "element_reference_fallback", createString(StringOperations.encodeRope(method.getName(), UTF8Encoding.INSTANCE)),
                    args);
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnum = 1, raiseIfFrozenSelf = true)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        @Child private ArrayReadNormalizedNode readNode;
        @Child private ArrayWriteNormalizedNode writeNode;
        @Child protected ArrayReadSliceNormalizedNode readSliceNode;
        @Child private ToIntNode toIntNode;

        private final BranchProfile negativeIndexProfile = BranchProfile.create();
        private final BranchProfile negativeLengthProfile = BranchProfile.create();

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

        @Specialization(guards = { "!isInteger(indexObject)", "!isRubyRange(indexObject)" })
        public Object set(VirtualFrame frame, DynamicObject array, Object indexObject, Object value, NotProvided unused) {
            final int index = toInt(frame, indexObject);
            return executeSet(frame, array, index, value, unused);
        }

        // array[start, length] = object

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "strategy.specializesFor(value)" }, limit = "ARRAY_STRATEGIES")
        public Object setObject(VirtualFrame frame, DynamicObject array, int start, int length, Object value,
                @Cached("forValue(value)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                                @Cached("new()") SnippetNode snippetNode) {
            checkLengthPositive(length);

            final int size = getSize(array);
            final int begin = ArrayOperations.normalizeIndex(size, start, negativeIndexProfile);
            checkIndex(array, start, begin);

            final DynamicObject ary;

            final Object maybeAry = snippetNode.execute(frame, "Array.try_convert(value)", "value", value);
            if (maybeAry != nil()) {
                ary = (DynamicObject) maybeAry;
            } else {
                // Passing a non-array as value is the same as assigning a single-element array
                ArrayMirror mirror = strategy.newArray(1);
                mirror.set(0, value);
                ary = createArray(mirror.getArray(), 1);
            }

            return executeSet(frame, array, start, length, ary);
        }

        // array[start, length] = other_array, with length == other_array.size

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

        // array[start, length] = other_array, with length != other_array.size

        @Specialization(guards = {
                "isRubyArray(replacement)",
                "length != getArraySize(replacement)"
        })
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, int rawStart, int length, DynamicObject replacement,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("createBinaryProfile()") ConditionProfile needCopy,
                @Cached("createBinaryProfile()") ConditionProfile recursive,
                @Cached("createBinaryProfile()") ConditionProfile emptyReplacement,
                @Cached("createBinaryProfile()") ConditionProfile grow) {
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
            } else if (emptyReplacement.profile(replacementSize == 0)) {
                // If no tail and the replacement is empty, the array will grow.
                // We need to append nil from index arraySize to index (start - 1).
                // E.g. a = [1,2,3]; a[5,1] = []; a == [1,2,3,nil,nil]
                if (grow.profile(arraySize < start)) {
                    for (int i = arraySize; i < start; i++) {
                        write(array, i, nil());
                    }
                }
            }

            // Set size
            if (needsTail) {
                setSize(array, endOfReplacementInArray + tailSize);
            } else {
                setSize(array, endOfReplacementInArray);
            }

            return replacement;
        }

        // array[start, length] = object_or_array with non-int start or length

        @Specialization(guards = { "!isInteger(startObject) || !isInteger(lengthObject)", "wasProvided(value)" })
        public Object setStartLengthNotInt(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, Object value,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            int start = toInt(frame, startObject);
            int length = toInt(frame, lengthObject);
            return executeSet(frame, array, start, length, value);
        }

        // array[start..end] = object_or_array

        @Specialization(guards = "isIntRange(range)")
        public Object setRange(VirtualFrame frame, DynamicObject array, DynamicObject range, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile,
                @Cached("create()") BranchProfile errorProfile) {
            final int size = getSize(array);
            final int begin = Layouts.INT_RANGE.getBegin(range);
            final int start = ArrayOperations.normalizeIndex(size, begin, negativeBeginProfile);
            if (start < 0) {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().rangeError(range, this));
            }
            final int end = ArrayOperations.normalizeIndex(size, Layouts.INT_RANGE.getEnd(range), negativeEndProfile);
            int inclusiveEnd = Layouts.INT_RANGE.getExcludedEnd(range) ? end - 1 : end;
            if (inclusiveEnd < 0) {
                inclusiveEnd = -1;
            }
            final int length = inclusiveEnd - start + 1;
            final int normalizeLength = length > -1 ? length : 0;
            return executeSet(frame, array, start, normalizeLength, value);
        }

        @Specialization(guards = { "!isIntRange(range)", "isRubyRange(range)" })
        public Object setOtherRange(VirtualFrame frame, DynamicObject array, DynamicObject range, Object value, NotProvided unused,
                @Cached("create()") ToIntRangeNode toIntRangeNode) {
            DynamicObject intRange = toIntRangeNode.executeToIntRange(frame, range);
            return executeSet(frame, array, intRange, value, unused);
        }

        // Helpers

        private void checkIndex(DynamicObject array, int index, int normalizedIndex) {
            if (normalizedIndex < 0) {
                negativeIndexProfile.enter();
                throw new RaiseException(coreExceptions().indexTooSmallError("array", index, getSize(array), this));
            }
        }

        public void checkLengthPositive(int length) {
            if (length < 0) {
                negativeLengthProfile.enter();
                throw new RaiseException(coreExceptions().negativeLengthError(length, this));
            }
        }

        protected int getArraySize(DynamicObject array) {
            return getSize(array);
        }

        private Object read(DynamicObject array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ArrayReadNormalizedNodeGen.create(getContext(), null, null, null));
            }
            return readNode.executeRead(array, index);
        }

        private Object write(DynamicObject array, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                writeNode = insert(ArrayWriteNormalizedNodeGen.create(getContext(), null, null, null, null));
            }
            return writeNode.executeWrite(array, index, value);
        }

        private DynamicObject readSlice(DynamicObject array, int start, int length) {
            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), null, null, null, null));
            }
            return readSliceNode.executeReadSlice(array, start, length);
        }

        private int toInt(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
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
            return FixnumLowerNodeGen.create(null, null, ToIntNodeGen.create(index));
        }

        @Specialization
        public Object at(DynamicObject array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), null, null, null));
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
            return createArray(null, 0);
        }

        @Specialization(guards = { "!isObjectArray(array)", "strategy.matches(array)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject compactPrimitive(DynamicObject array,
                @Cached("of(array)") ArrayStrategy strategy) {
            final int size = getSize(array);
            Object store = strategy.newMirror(array).extractRange(0, size).getArray();
            return createArray(store, size);
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

            return createArray(newStore, m);
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

    @CoreMethod(names = "delete", required = 1, needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteNode extends YieldingCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;
        @Child private IsFrozenNode isFrozenNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(null);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object deleteNull(VirtualFrame frame, DynamicObject array, Object value, NotProvided block) {
            return nil();
        }

        @Specialization(guards = "isNullArray(array)")
        public Object deleteNull(VirtualFrame frame, DynamicObject array, Object value, DynamicObject block) {
            return yield(frame, block, value);
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object delete(VirtualFrame frame, DynamicObject array, Object value, Object maybeBlock,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            while (n < getSize(array)) {
                final Object stored = store.get(n);

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    checkFrozen(array);
                    found = stored;
                    n++;
                } else {
                    if (i != n) {
                        store.set(i, store.get(n));
                    }

                    i++;
                    n++;
                }
            }

            if (i != n) {
                setStoreAndSize(array, store.getArray(), i);
                return found;
            } else {
                if (maybeBlock == NotProvided.INSTANCE) {
                    return nil();
                } else {
                    return yield(frame, (DynamicObject) maybeBlock, value);
                }
            }
        }

        public void checkFrozen(Object object) {
            if (isFrozenNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), null, null));
            }
            isFrozenNode.raiseIfFrozen(object);
        }

    }

    @CoreMethod(names = "delete_at", required = 1, raiseIfFrozenSelf = true, lowerFixnum = 1)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "index")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteAtNode extends CoreMethodNode {

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return ToIntNodeGen.create(index);
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

    @CoreMethod(names = "each", needsBlock = true, enumeratorSize = "size")
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

    @CoreMethod(names = "each_with_index", needsBlock = true, enumeratorSize = "size")
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
            return callFillInternal.call(frame, array, "fill_internal", args);
        }

        @Specialization
        protected Object fillFallback(VirtualFrame frame, DynamicObject array, Object[] args, DynamicObject block,
                @Cached("createMethodCall()") CallDispatchHeadNode callFillInternal) {
            return callFillInternal.callWithBlock(frame, array, "fill_internal", block, args);
        }

    }

    @CoreMethod(names = "hash_internal")
    public abstract static class HashNode extends ArrayCoreMethodNode {

        private static final int MURMUR_ARRAY_SEED = System.identityHashCode(ArrayNodes.class);

        @Child private ToIntNode toIntNode;

        @Specialization(guards = "isNullArray(array)")
        public long hashNull(DynamicObject array) {
            final int size = 0;
            long h = Hashing.start(size);
            h = Hashing.update(h, MURMUR_ARRAY_SEED);
            return Hashing.end(h);
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public long hash(VirtualFrame frame, DynamicObject array,
                         @Cached("of(array)") ArrayStrategy strategy,
                         @Cached("createMethodCall()") CallDispatchHeadNode toHashNode) {
            final int size = getSize(array);
            // TODO BJF Jul 4, 2016 Seed could be chosen in advance to avoid branching
            long h = Hashing.start(size);
            h = Hashing.update(h, MURMUR_ARRAY_SEED);
            final ArrayMirror store = strategy.newMirror(array);

            for (int n = 0; n < size; n++) {
                final Object value = store.get(n);
                final long valueHash = toLong(frame, toHashNode.call(frame, value, "hash"));
                h = Hashing.update(h, valueHash);
            }

            return Hashing.end(h);
        }

        private long toLong(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
            }
            final Object result = toIntNode.executeIntOrLong(frame, indexObject);
            if (result instanceof Integer) {
                return (long) (int) result;
            } else {
                return (long) result;
            }
        }

    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(null);
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

    @CoreMethod(names = "initialize", needsBlock = true, optional = 2, raiseIfFrozenSelf = true, lowerFixnum = 1)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeNode extends YieldingCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode toAryNode;
        @Child private KernelNodes.RespondToNode respondToToAryNode;

        public abstract DynamicObject executeInitialize(VirtualFrame frame, DynamicObject array, Object size, Object defaultValue, Object block);

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
            if (needsFill.profile(!strategy.isDefaultValue(value))) {
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                respondToToAryNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), null, null, null, null));
            }
            return respondToToAryNode.doesRespondToString(frame, object, create7BitString("to_ary", UTF8Encoding.INSTANCE), true);
        }

        protected Object callToAry(VirtualFrame frame, Object object) {
            if (toAryNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toAryNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }
            return toAryNode.call(frame, object, "to_ary");
        }

        protected int toInt(VirtualFrame frame, Object value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
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
                    accumulator = dispatch.call(frame, accumulator, symbol, store.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }
            return accumulator;
        }

    }

    @CoreMethod(names = { "map", "collect" }, needsBlock = true, enumeratorSize = "size")
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mapNull(DynamicObject array, DynamicObject block) {
            return createArray(null, 0);
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

            return createArray(arrayBuilder.finish(mappedStore, size), size);
        }

    }

    @CoreMethod(names = { "map!", "collect!" }, needsBlock = true, enumeratorSize = "size", raiseIfFrozenSelf = true)
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
            return ArrayWriteNormalizedNodeGen.create(getContext(), null, null, null, null);
        }

    }

    @CoreMethod(names = "pack", required = 1, taintFrom = 1)
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                makeLeafRopeNode = insert(RopeNodesFactory.MakeLeafRopeNodeGen.create(null, null, null, null));
            }

            final DynamicObject string = createString(makeLeafRopeNode.executeMake(
                    bytes,
                    result.getEncoding().getEncodingForLength(formatLength),
                    result.getStringCodeRange(),
                    result.getStringLength()));

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    taintNode = insert(TaintNodeGen.create(getContext(), null, null));
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
        public Object pack(
                VirtualFrame frame,
                DynamicObject array,
                Object format,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "pack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            return new PackCompiler(getContext(), this).compile(format.toString());
        }

        protected int getCacheLimit() {
            return getContext().getOptions().PACK_CACHE;
        }

    }

    @CoreMethod(names = "pop", raiseIfFrozenSelf = true, optional = 1, lowerFixnum = 1)
    public abstract static class PopNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private ArrayPopOneNode popOneNode;

        public abstract Object executePop(VirtualFrame frame, DynamicObject array, Object n);

        @Specialization
        public Object pop(DynamicObject array, NotProvided n) {
            if (popOneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                popOneNode = insert(ArrayPopOneNodeGen.create(getContext(), null, null));
            }

            return popOneNode.executePopOne(array);
        }

        @Specialization(guards = "n < 0")
        public Object popNNegative(VirtualFrame frame, DynamicObject array, int n) {
            throw new RaiseException(coreExceptions().argumentErrorNegativeArraySize(this));
        }

        @Specialization(guards = { "n >= 0", "isEmptyArray(array)" })
        public Object popEmpty(VirtualFrame frame, DynamicObject array, int n) {
            return createArray(null, 0);
        }

        @Specialization(guards = { "n == 0", "!isEmptyArray(array)" })
        public Object popZeroNotEmpty(DynamicObject array, int n) {
            return createArray(null, 0);
        }

        @Specialization(guards = { "n > 0", "!isEmptyArray(array)", "strategy.matches(array)" }, limit = "ARRAY_STRATEGIES")
        public Object popNotEmpty(DynamicObject array, int n,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile minProfile) {
            final int size = getSize(array);
            final int numPop = minProfile.profile(size < n) ? size : n;
            final ArrayMirror store = strategy.newMirror(array);

            // Extract values in a new array
            final ArrayMirror popped = store.extractRange(size - numPop, size);

            // Null out the popped values from the store
            final ArrayMirror filler = strategy.newArray(numPop);
            filler.copyTo(store, 0, size - numPop, numPop);
            setSize(array, size - numPop);

            return createArray(popped.getArray(), numPop);
        }

        @Specialization(guards = { "wasProvided(n)", "!isInteger(n)", "!isLong(n)" })
        public Object popNToInt(VirtualFrame frame, DynamicObject array, Object n) {
            return executePop(frame, array, toInt(frame, n));
        }

        private int toInt(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
            }
            return toIntNode.doInt(frame, indexObject);
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

        @Child private ArrayAppendOneNode appendOneNode;

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendOneNode = ArrayAppendOneNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = "rest.length == 0")
        public DynamicObject pushZero(DynamicObject array, NotProvided unusedValue, Object[] rest) {
            return array;
        }

        @Specialization(guards = { "rest.length == 0", "wasProvided(value)" })
        public DynamicObject pushOne(DynamicObject array, Object value, Object[] rest) {
            return appendOneNode.executeAppendOne(array, value);
        }

        @Specialization(guards = { "rest.length > 0", "wasProvided(value)" })
        public DynamicObject pushMany(VirtualFrame frame, DynamicObject array, Object value, Object[] rest) {
            // NOTE (eregon): Appending one by one here to avoid useless generalization to Object[]
            // if the arguments all fit in the current storage
            appendOneNode.executeAppendOne(array, value);
            for (int i = 0; i < rest.length; i++) {
                appendOneNode.executeAppendOne(array, rest[i]);
            }
            return array;
        }

    }

    @CoreMethod(names = "reject", needsBlock = true, enumeratorSize = "size")
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object rejectNull(DynamicObject array, DynamicObject block) {
            return createArray(null, 0);
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object rejectOther(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final ArrayMirror store = strategy.newMirror(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    final Object value = store.get(n);

                    if (!yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return createArray(arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "delete_if", needsBlock = true, enumeratorSize = "size", raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteIfNode extends YieldingCoreMethodNode {

        @Specialization
        public Object deleteIf(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("createRejectInPlaceNode()") RejectInPlaceNode rejectInPlaceNode) {
            rejectInPlaceNode.executeRejectInPlace(frame, array, block);
            return array;
        }

        protected RejectInPlaceNode createRejectInPlaceNode() {
            return RejectInPlaceNodeFactory.create(null);
        }

    }


    @CoreMethod(names = "reject!", needsBlock = true, enumeratorSize = "size", raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        public abstract Object executeRejectInPlace(VirtualFrame frame, DynamicObject array, DynamicObject block);

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(DynamicObject array, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object rejectInPlaceOther(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);

            int i = 0;
            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    final Object value = store.get(n);
                    if (yieldIsTruthy(frame, block, value)) {
                        continue;
                    }

                    if (i != n) {
                        store.set(i, store.get(n));
                    }

                    i++;
                }
            } finally {
                // Null out the elements behind the size
                final ArrayMirror filler = strategy.newArray(n - i);
                filler.copyTo(store, 0, i, n - i);
                setSize(array, i);

                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            if (i != n) {
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

        @Specialization(guards = "isNullArray(other)")
        public DynamicObject replace(DynamicObject array, DynamicObject other) {
            setStoreAndSize(array, null, 0);
            return array;
        }

        @Specialization(guards = "strategy.matches(other)", limit = "ARRAY_STRATEGIES")
        public DynamicObject replace(DynamicObject array, DynamicObject other,
                @Cached("of(other)") ArrayStrategy strategy) {
            final int size = getSize(other);
            final ArrayMirror copy = strategy.newMirror(other).copyArrayAndMirror();
            setStoreAndSize(array, copy.getArray(), size);
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true, enumeratorSize = "size")
    @ImportStatic(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(DynamicObject array, DynamicObject block) {
            return createArray(null, 0);
        }

        @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
        public Object selectOther(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final ArrayMirror store = strategy.newMirror(array);

            Object selectedStore = arrayBuilder.start(getSize(array));
            int selectedSize = 0;

            int n = 0;
            try {
                for (; n < getSize(array); n++) {
                    final Object value = store.get(n);

                    if (yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    LoopNode.reportLoopCount(this, n);
                }
            }

            return createArray(arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true, optional = 1)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "array"),
            @NodeChild(type = RubyNode.class, value = "n")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class ShiftNode extends CoreMethodNode {

        @Child private ToIntNode toIntNode;

        public abstract Object executeShift(VirtualFrame frame, DynamicObject array, Object n);

        // No n, just shift 1 element and return it

        @Specialization(guards = "isEmptyArray(array)")
        public Object shiftEmpty(DynamicObject array, NotProvided n) {
            return nil();
        }

        @Specialization(guards = { "strategy.matches(array)", "!isEmptyArray(array)" }, limit = "ARRAY_STRATEGIES")
        public Object shiftOther(DynamicObject array, NotProvided n,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror store = strategy.newMirror(array);
            final int size = getSize(array);
            final Object value = store.get(0);
            store.copyTo(store, 1, 0, size - 1);

            // Null out the element behind the size
            final ArrayMirror filler = strategy.newArray(1);
            filler.copyTo(store, 0, size - 1, 1);
            setSize(array, size - 1);

            return value;
        }

        // n given, shift the first n elements and return them as an Array

        @Specialization(guards = "n < 0")
        public Object shiftNegative(DynamicObject array, int n) {
            throw new RaiseException(coreExceptions().argumentErrorNegativeArraySize(this));
        }

        @Specialization(guards = "n == 0")
        public Object shiftZero(DynamicObject array, int n) {
            return createArray(null, 0);
        }

        @Specialization(guards = { "n > 0", "isEmptyArray(array)" })
        public Object shiftManyEmpty(DynamicObject array, int n) {
            return createArray(null, 0);
        }

        @Specialization(guards = { "n > 0", "strategy.matches(array)", "!isEmptyArray(array)" }, limit = "ARRAY_STRATEGIES")
        public Object shiftMany(DynamicObject array, int n,
                @Cached("of(array)") ArrayStrategy strategy,
                @Cached("createBinaryProfile()") ConditionProfile minProfile) {
            final int size = getSize(array);
            final int numShift = minProfile.profile(size < n) ? size : n;
            final ArrayMirror store = strategy.newMirror(array);

            // Extract values in a new array
            final ArrayMirror result = store.extractRange(0, numShift);

            // Move elements
            store.copyTo(store, numShift, 0, size - numShift);

            // Null out the element behind the size
            final ArrayMirror filler = strategy.newArray(numShift);
            filler.copyTo(store, 0, size - numShift, numShift);
            setSize(array, size - numShift);

            return createArray(result.getArray(), numShift);
        }

        @Specialization(guards = { "wasProvided(n)", "!isInteger(n)", "!isLong(n)" })
        public Object shiftNToInt(VirtualFrame frame, DynamicObject array, Object n) {
            return executeShift(frame, array, toInt(frame, n));
        }

        private int toInt(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                toIntNode = insert(ToIntNode.create());
            }
            return toIntNode.doInt(frame, indexObject);
        }

    }

    @CoreMethod(names = { "size", "length" })
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

        private final BranchProfile errorProfile = BranchProfile.create();

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = DispatchHeadNodeFactory.createMethodCall(context);
            yieldNode = new YieldNode(context);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject sortNull(DynamicObject array, Object unusedBlock) {
            return createArray(null, 0);
        }

        @ExplodeLoop
        @Specialization(guards = { "!isNullArray(array)", "isSmall(array)", "strategy.matches(array)" }, limit = "ARRAY_STRATEGIES")
        public DynamicObject sortVeryShort(VirtualFrame frame, DynamicObject array, NotProvided block,
                @Cached("of(array)") ArrayStrategy strategy) {
            final ArrayMirror originalStore = strategy.newMirror(array);
            final ArrayMirror store = strategy.newArray(getContext().getOptions().ARRAY_SMALL);
            final int size = getSize(array);

            // Copy with a exploded loop for PE

            for (int i = 0; i < getContext().getOptions().ARRAY_SMALL; i++) {
                if (i < size) {
                    store.set(i, originalStore.get(i));
                }
            }

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < getContext().getOptions().ARRAY_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < getContext().getOptions().ARRAY_SMALL; j++) {
                        if (j < size) {
                            final Object a = store.get(i);
                            final Object b = store.get(j);
                            if (castSortValue(compareDispatchNode.call(frame, b, "<=>", a)) < 0) {
                                store.set(j, a);
                                store.set(i, b);
                            }
                        }
                    }
                }
            }

            return createArray(store.getArray(), size);
        }

        @Specialization(guards = { "!isNullArray(array)", "!isSmall(array)" })
        public Object sortLargeArray(VirtualFrame frame, DynamicObject array, NotProvided block,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame,
                    "sorted = dup; Truffle.privately { sorted.mergesort! }; sorted");
        }

        @Specialization(guards = { "isObjectArray(array)" })
        public Object sortObjectWithBlock(DynamicObject array, DynamicObject block) {
            final int size = getSize(array);
            Object[] copy = ((Object[]) getStore(array)).clone();
            doSort(copy, size, block);
            return createArray(copy, size);
        }

        @TruffleBoundary
        private void doSort(Object[] copy, int size, DynamicObject block) {
            Arrays.sort(copy, 0, size, (a, b) -> castSortValue(ProcOperations.rootCall(block, a, b)));
        }

        @Specialization(guards = { "!isNullArray(array)", "!isObjectArray(array)" })
        public Object sortWithBlock(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("new()") SnippetNode snippet) {
            return snippet.execute(frame,
                    "sorted = dup; Truffle.privately { sorted.mergesort_block!(block) }; sorted",
                    "block", block);
        }

        private int castSortValue(Object value) {
            if (value instanceof Integer) {
                return (int) value;
            }

            errorProfile.enter();
            // TODO CS 14-Mar-15 - what's the error message here?
            throw new RaiseException(coreExceptions().argumentError("expecting a Fixnum to sort", this));
        }

        protected boolean isSmall(DynamicObject array) {
            return getSize(array) <= getContext().getOptions().ARRAY_SMALL;
        }

    }

    @CoreMethod(names = "zip", rest = true, required = 1, needsBlock = true)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode zipInternalCall;

        @Specialization(guards = {
                "isRubyArray(other)", "aStrategy.matches(array)", "bStrategy.matches(other)", "others.length == 0"
        }, limit = "ARRAY_STRATEGIES")
        public DynamicObject zipObjectIntegerFixnum(DynamicObject array, DynamicObject other, Object[] others, NotProvided block,
                @Cached("of(array)") ArrayStrategy aStrategy,
                @Cached("of(other)") ArrayStrategy bStrategy,
                @Cached("aStrategy.generalize(bStrategy)") ArrayStrategy generalized,
                @Cached("createBinaryProfile()") ConditionProfile bNotSmallerProfile) {
            final ArrayMirror a = aStrategy.newMirror(array);
            final ArrayMirror b = bStrategy.newMirror(other);

            final int bSize = getSize(other);
            final int zippedLength = getSize(array);
            final Object[] zipped = new Object[zippedLength];

            for (int n = 0; n < zippedLength; n++) {
                if (bNotSmallerProfile.profile(n < bSize)) {
                    final ArrayMirror pair = generalized.newArray(2);
                    pair.set(0, a.get(n));
                    pair.set(1, b.get(n));
                    zipped[n] = createArray(pair.getArray(), 2);
                } else {
                    zipped[n] = createArray(new Object[] { a.get(n), nil() }, 2);
                }
            }

            return createArray(zipped, zippedLength);
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                zipInternalCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object[] others = RubyArguments.getArguments(frame);

            return zipInternalCall.callWithBlock(frame, array, "zip_internal", block, others);
        }

        protected static boolean fallback(DynamicObject array, DynamicObject other, Object[] others) {
            return ArrayGuards.isNullArray(array) || ArrayGuards.isNullArray(other) || others.length > 0;
        }

    }

}
