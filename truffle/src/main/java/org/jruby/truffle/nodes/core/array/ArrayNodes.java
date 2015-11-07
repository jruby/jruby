/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.StringCachingGuards;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.coerce.ToAryNodeGen;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.fixnum.FixnumLowerNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.locals.ReadDeclarationVariableNode;
import org.jruby.truffle.nodes.methods.DeclarationContext;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.format.parser.PackParser;
import org.jruby.truffle.format.runtime.PackResult;
import org.jruby.truffle.format.runtime.exceptions.*;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.array.ArrayReflector;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.ArrayOperations;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.core.StringOperations;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;

import java.util.Arrays;

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

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("b") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = {"isNullArray(a)", "isNullArray(b)"})
        public DynamicObject addNull(DynamicObject a, DynamicObject b) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = {"isObjectArray(a)", "isNullArray(b)"})
        public DynamicObject addObjectNull(DynamicObject a, DynamicObject b) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(a), Layouts.ARRAY.getSize(a)), Layouts.ARRAY.getSize(a));
        }

        @Specialization(guards = {"isIntArray(a)", "isIntArray(b)"})
        public DynamicObject addBothIntegerFixnum(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final int[] combined = new int[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isLongArray(a)", "isLongArray(b)"})
        public DynamicObject addBothLongFixnum(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final long[] combined = new long[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isDoubleArray(a)", "isRubyArray(b)", "isDoubleArray(b)"})
        public DynamicObject addBothFloat(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final double[] combined = new double[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addBothObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isIntArray(b)"})
        public DynamicObject addNullIntegerFixnum(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((int[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isLongArray(b)"})
        public DynamicObject addNullLongFixnum(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((long[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addNullObject(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"!isObjectArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addOtherObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(ArrayUtils.box(Layouts.ARRAY.getStore(a)), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "isRubyArray(b)", "!isObjectArray(b)"})
        public DynamicObject addObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(ArrayUtils.box(Layouts.ARRAY.getStore(b)), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), combined, combinedSize);
        }

        @Specialization(guards = {"isEmptyArray(a)", "isRubyArray(b)"})
        public DynamicObject addEmpty(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), ArrayUtils.box(Layouts.ARRAY.getStore(b)), size);
        }

        @Specialization(guards = {"isEmptyArray(b)", "isRubyArray(b)"})
        public DynamicObject addOtherEmpty(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(a);
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), ArrayUtils.box(Layouts.ARRAY.getStore(a)), size);
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

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mulEmpty(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
        }

        @Specialization(guards = "isIntArray(array)")
        public DynamicObject mulIntegerFixnum(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final int[] newStore = new int[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), newStore, newStoreLength);
        }

        @Specialization(guards = "isLongArray(array)")
        public DynamicObject mulLongFixnum(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final long[] newStore = new long[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), newStore, newStoreLength);
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject mulFloat(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final double[] newStore = new double[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), newStore, newStoreLength);
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject mulObject(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
            final int storeLength = Layouts.ARRAY.getSize(array);
            final int newStoreLength = storeLength * count;
            final Object[] newStore = new Object[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), newStore, newStoreLength);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object mulObject(VirtualFrame frame, DynamicObject array, DynamicObject string) {
            CompilerDirectives.transferToInterpreter();
            return ruby(frame, "join(sep)", "sep", string);
        }

        @Specialization(guards = {"!isRubyString(object)"})
        public Object mulObjectCount(VirtualFrame frame, DynamicObject array, Object object) {
            CompilerDirectives.transferToInterpreter();
            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
            if (respondToToStrNode.doesRespondToString(frame, object, create7BitString(StringOperations.encodeByteList("to_str", UTF8Encoding.INSTANCE)), false)) {
                return ruby(frame, "join(sep.to_str)", "sep", object);
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int count = toIntNode.doInt(frame, object);
                if (count < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
                }
                if (Layouts.ARRAY.getStore(array) instanceof int[]) {
                    return mulIntegerFixnum(array, count);
                } else if (Layouts.ARRAY.getStore(array) instanceof long[]) {
                    return mulLongFixnum(array, count);
                } else if (Layouts.ARRAY.getStore(array) instanceof double[]) {
                    return mulFloat(array, count);
                } else if (Layouts.ARRAY.getStore(array) == null) {
                    return mulEmpty(array, count);
                } else {
                    return mulObject(array, count);
                }

            }
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
        public Object index(VirtualFrame frame, DynamicObject array, int index, NotProvided length) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, (DynamicObject) array, index);
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
            final int size = Layouts.ARRAY.getSize(array);
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

        @Specialization(guards = {"!isInteger(a)", "!isIntegerFixnumRange(a)"})
        public Object fallbackIndex(VirtualFrame frame, DynamicObject array, Object a, NotProvided length) {
            Object[] objects = new Object[]{a};
            return fallback(frame, array, Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length));
        }

        @Specialization(guards = { "!isIntegerFixnumRange(a)", "wasProvided(b)" })
        public Object fallbackSlice(VirtualFrame frame, DynamicObject array, Object a, Object b) {
            Object[] objects = new Object[]{a, b};
            return fallback(frame, array, Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), objects, objects.length));
        }

        public Object fallback(VirtualFrame frame, DynamicObject array, DynamicObject args) {
            if (fallbackNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            return fallbackNode.call(frame, array, "element_reference_fallback", null,
                    createString(StringOperations.encodeByteList(method.getName(), UTF8Encoding.INSTANCE)), args);
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        @Child private ArrayWriteNormalizedNode writeNode;
        @Child protected ArrayReadSliceNormalizedNode readSliceNode;
        @Child private PopOneNode popOneNode;
        @Child private ToIntNode toIntNode;

        public IndexSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object set(DynamicObject array, int index, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + index + " too small for array; minimum: " + Integer.toString(-Layouts.ARRAY.getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }
            return write(array, normalizedIndex, value);
        }

        @Specialization(guards = { "!isInteger(indexObject)", "!isIntegerFixnumRange(indexObject)" })
        public Object set(VirtualFrame frame, DynamicObject array, Object indexObject, Object value, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int index = toInt(frame, indexObject);
            return set(array, index, value, unused, negativeIndexProfile);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(startObject) || !isInteger(lengthObject)" })
        public Object setObject(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, Object value,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            int length = toInt(frame, lengthObject);
            int start = toInt(frame, startObject);
            return setObject(array, start, length, value, negativeIndexProfile);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)" })
        public Object setObject(DynamicObject array, int start, int length, Object value,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int size = Layouts.ARRAY.getSize(array);
            final int begin = ArrayOperations.normalizeIndex(size, start, negativeIndexProfile);
            if (begin < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "index " + start + " too small for array; minimum: " + Integer.toString(-size);
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            if (begin < size && length == 1) {
                return write(array, begin, value);
            } else {
                if (size > (begin + length)) { // there is a tail, else other values discarded
                    if (readSliceNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        readSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                    }
                    DynamicObject endValues = readSliceNode.executeReadSlice(array, (begin + length), (size - begin - length));
                    write(array, begin, value);

                    CompilerDirectives.transferToInterpreter();
                    Object[] endValuesStore = ArrayUtils.box(Layouts.ARRAY.getStore(endValues));

                    int i = begin + 1;
                    for (Object obj : endValuesStore) {
                        write(array, i, obj);
                        i += 1;
                    }
                } else {
                    write(array, begin, value);
                }

                if (popOneNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    popOneNode = insert(PopOneNodeGen.create(getContext(), getSourceSection(), null));
                }
                int popLength = length - 1 < size ? length - 1 : size - 1;
                for (int i = 0; i < popLength; i++) { // TODO 3-15-2015 BF update when pop can pop multiple
                    popOneNode.executePopOne(array);
                }
                return value;
            }
        }

        @Specialization(guards = { "!isInteger(startObject)", "!isInteger(lengthObject) || isRubyArray(value)" })
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, DynamicObject value,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            int start = toInt(frame, startObject);
            int length = toInt(frame, lengthObject);
            return setOtherArray(array, start, length, value, negativeIndexProfile);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyArray(replacement)")
        public Object setOtherArray(DynamicObject array, int start, int length, DynamicObject replacement,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), start, negativeIndexProfile);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", start, Layouts.ARRAY.getSize(array), this));
            }

            final int replacementLength = Layouts.ARRAY.getSize(replacement);
            final Object[] replacementStore = ArrayOperations.toObjectArray(replacement);

            if (replacementLength == length) {
                for (int i = 0; i < length; i++) {
                    write(array, normalizedIndex + i, replacementStore[i]);
                }
            } else {
                final int arrayLength = Layouts.ARRAY.getSize(array);
                final int newLength;
                final boolean mustExpandArray = normalizedIndex > arrayLength;
                final boolean writeLastPart;

                if (mustExpandArray) {
                    newLength = normalizedIndex + replacementLength;
                    writeLastPart = false;
                } else {
                    if (normalizedIndex + length > arrayLength) {
                        newLength = normalizedIndex + replacementLength;
                        writeLastPart = false;
                    } else {
                        newLength = arrayLength - length + replacementLength;
                        writeLastPart = true;
                    }
                }

                final Object store = ArrayOperations.toObjectArray(array);
                final Object newStore[] = new Object[newLength];

                if (mustExpandArray) {
                    System.arraycopy(store, 0, newStore, 0, arrayLength);

                    final int nilPad = normalizedIndex - arrayLength;
                    for (int i = 0; i < nilPad; i++) {
                        newStore[arrayLength + i] = nil();
                    }
                } else {
                    System.arraycopy(store, 0, newStore, 0, normalizedIndex);
                }

                System.arraycopy(replacementStore, 0, newStore, normalizedIndex, replacementLength);

                if (writeLastPart) {
                    System.arraycopy(store, normalizedIndex + length, newStore, normalizedIndex + replacementLength, arrayLength - (normalizedIndex + length));
                }

                Layouts.ARRAY.setStore(array, newStore);
                Layouts.ARRAY.setSize(array, newLength);
            }

            return replacement;
        }

        @Specialization(guards = { "!isRubyArray(other)", "isIntegerFixnumRange(range)" })
        public Object setRange(DynamicObject array, DynamicObject range, Object other, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int size = Layouts.ARRAY.getSize(array);
            final int normalizedStart = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range), negativeBeginProfile);
            if (normalizedStart < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }
            final int end = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), negativeEndProfile);
            int inclusiveEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end - 1 : end;
            if (inclusiveEnd < 0) {
                inclusiveEnd = -1;
            }
            final int length = inclusiveEnd - normalizedStart + 1;
            return setObject(array, normalizedStart, length, other, negativeIndexProfile);
        }

        @Specialization(guards = { "isRubyArray(other)", "!isIntArray(array) || !isIntArray(other)", "isIntegerFixnumRange(range)" })
        public Object setRangeArray(DynamicObject array, DynamicObject range, DynamicObject other, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            final int size = Layouts.ARRAY.getSize(array);
            final int normalizedStart = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range), negativeBeginProfile);
            if (normalizedStart < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }
            final int end = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), negativeEndProfile);
            int inclusiveEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range) ? end - 1 : end;
            if (inclusiveEnd < 0) {
                inclusiveEnd = -1;
            }
            final int length = inclusiveEnd - normalizedStart + 1;
            return setOtherArray(array, normalizedStart, length, other, negativeIndexProfile);
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyArray(other)", "isIntArray(other)", "isIntegerFixnumRange(range)"})
        public Object setIntegerFixnumRange(DynamicObject array, DynamicObject range, DynamicObject other, NotProvided unused,
                @Cached("createBinaryProfile()") ConditionProfile negativeBeginProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeEndProfile,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
            if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(range)) {
                CompilerDirectives.transferToInterpreter();
                return setRangeArray(array, range, other, unused, negativeBeginProfile, negativeEndProfile, negativeIndexProfile);
            } else {
                final int size = Layouts.ARRAY.getSize(array);
                int normalizedBegin = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getBegin(range), negativeBeginProfile);
                int normalizedEnd = ArrayOperations.normalizeIndex(size, Layouts.INTEGER_FIXNUM_RANGE.getEnd(range), negativeEndProfile);
                if (normalizedEnd < 0) {
                    normalizedEnd = -1;
                }
                if (normalizedBegin == 0 && normalizedEnd == size - 1) {
                    Layouts.ARRAY.setStore(array, Arrays.copyOf((int[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)));
                    Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(other));
                } else {
                    CompilerDirectives.transferToInterpreter();
                    return setRangeArray(array, range, other, unused, negativeBeginProfile, negativeEndProfile, negativeIndexProfile);
                }
            }

            return other;
        }

        private Object write(DynamicObject array, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return writeNode.executeWrite(array, index, value);
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

        public AtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return FixnumLowerNodeGen.create(getContext(), getSourceSection(),
                    ToIntNodeGen.create(getContext(), getSourceSection(), index));
        }

        @Specialization
        public Object at(VirtualFrame frame, DynamicObject array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, (DynamicObject) array, index);
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends ArrayCoreMethodNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyArray(array)")
        public DynamicObject clear(DynamicObject array) {
            Layouts.ARRAY.setStore(array, Layouts.ARRAY.getStore(array));
            Layouts.ARRAY.setSize(array, 0);
            return array;
        }

    }

    @CoreMethod(names = "compact")
    @ImportStatic(ArrayGuards.class)
    public abstract static class CompactNode extends ArrayCoreMethodNode {

        public CompactNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isIntArray(array)")
        public DynamicObject compactInt(DynamicObject array) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((int[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
        }

        @Specialization(guards = "isLongArray(array)")
        public DynamicObject compactLong(DynamicObject array) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((long[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject compactDouble(DynamicObject array) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOf((double[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(DynamicObject array) {
            // TODO CS 9-Feb-15 by removing nil we could make this array suitable for a primitive array storage class

            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
            final Object[] newStore = new Object[store.length];
            final int size = Layouts.ARRAY.getSize(array);

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != nil()) {
                    newStore[m] = store[n];
                    m++;
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), newStore, m);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object compactNull(DynamicObject array) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

    }

    @CoreMethod(names = "compact!", raiseIfFrozenSelf = true)
    public abstract static class CompactBangNode extends ArrayCoreMethodNode {

        public CompactBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "!isObjectArray(array)")
        public DynamicObject compactNotObjects(DynamicObject array) {
            return nil();
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(DynamicObject array) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
            final int size = Layouts.ARRAY.getSize(array);

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != nil()) {
                    store[m] = store[n];
                    m++;
                }
            }

            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, m);

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

        @Child private AppendManyNode appendManyNode;

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendManyNode = AppendManyNodeGen.create(context, sourceSection, null, null, null);
        }

        @CreateCast("other") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = {"isRubyArray(other)", "isNullArray(other)"})
        public DynamicObject concatNull(DynamicObject array, DynamicObject other) {
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "!isNullArray(other)"})
        public DynamicObject concat(DynamicObject array, DynamicObject other) {
            appendManyNode.executeAppendMany((DynamicObject) array, Layouts.ARRAY.getSize(other), Layouts.ARRAY.getStore(other));
            return array;
        }

    }

    @CoreMethod(names = "delete", required = 1)
    public abstract static class DeleteNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;
        @Child private IsFrozenNode isFrozenNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        @Specialization(guards = "isIntArray(array)")
        public Object deleteIntegerFixnum(VirtualFrame frame, DynamicObject array, Object value) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
                    }
                    if (isFrozenNode.executeIsFrozen(array)) {
                        CompilerDirectives.transferToInterpreter();
                        throw new RaiseException(
                            getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(array)).getName(), this));
                    }
                    found = store[n];
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }
            if(i != n){
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return found;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object deleteObject(VirtualFrame frame, DynamicObject array, Object value) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
                    }
                    if (isFrozenNode.executeIsFrozen(array)) {
                        CompilerDirectives.transferToInterpreter();
                        throw new RaiseException(
                            getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(array)).getName(), this));
                    }
                    found = store[n];
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }

            if(i != n){
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return found;
        }

        @Specialization(guards = "isNullArray(array)")
        public Object deleteNull(VirtualFrame frame, DynamicObject array, Object value) {
            return nil();
        }

    }

    @CoreMethod(names = "delete_at", required = 1, raiseIfFrozenSelf = true, lowerFixnumParameters = 0)
    @NodeChildren({
        @NodeChild(type = RubyNode.class, value = "array"),
        @NodeChild(type = RubyNode.class, value = "index")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteAtNode extends CoreMethodNode {

        public DeleteAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return ToIntNodeGen.create(getContext(), getSourceSection(), index);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object deleteAtIntegerFixnum(DynamicObject array, int index,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("create()") BranchProfile notInBoundsProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

            if (normalizedIndex < 0 || normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                notInBoundsProfile.enter();
                return nil();
            } else {
                final int[] store = (int[]) Layouts.ARRAY.getStore(array);
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)")
        public Object deleteAtLongFixnum(DynamicObject array, int index,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("create()") BranchProfile notInBoundsProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

            if (normalizedIndex < 0 || normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                notInBoundsProfile.enter();
                return nil();
            } else {
                final long[] store = (long[]) Layouts.ARRAY.getStore(array);
                final long value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object deleteAtFloat(DynamicObject array, int index,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("create()") BranchProfile notInBoundsProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

            if (normalizedIndex < 0 || normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                notInBoundsProfile.enter();
                return nil();
            } else {
                final double[] store = (double[]) Layouts.ARRAY.getStore(array);
                final double value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object deleteAtObject(DynamicObject array, int index,
                @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile,
                @Cached("create()") BranchProfile notInBoundsProfile) {
            final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

            if (normalizedIndex < 0 || normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                notInBoundsProfile.enter();
                return nil();
            } else {
                final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
                final Object value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isEmptyArray(array)")
        public Object deleteAtNullOrEmpty(DynamicObject array, int index) {
            return nil();
        }


    }

    @CoreMethod(names = "each", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        private final DynamicObject eachSymbol;

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachSymbol = getSymbol("each");
        }

        @Specialization
        public Object eachEnumerator(VirtualFrame frame, DynamicObject array, NotProvided block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, array, "to_enum", null, eachSymbol);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object eachNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object eachIntegerFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object eachLongFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object eachFloat(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object eachObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "each_with_index", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachWithIndexNode extends YieldingCoreMethodNode {

        public EachWithIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject eachWithEmpty(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object eachWithIndexInt(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n], n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object eachWithIndexLong(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n], n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object eachWithIndexDouble(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n], n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object eachWithIndexObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    yield(frame, block, store[n], n);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization
        public Object eachWithIndexObject(VirtualFrame frame, DynamicObject array, NotProvided block) {
            return ruby(frame, "to_enum(:each_with_index)");
        }

    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        @Specialization(guards = "isNullArray(array)")
        public boolean includeNull(VirtualFrame frame, DynamicObject array, Object value) {
            return false;
        }

        @Specialization(guards = "isIntArray(array)")
        public boolean includeIntegerFixnum(VirtualFrame frame, DynamicObject array, Object value) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isLongArray(array)")
        public boolean includeLongFixnum(VirtualFrame frame, DynamicObject array, Object value) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);

            for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public boolean includeFloat(VirtualFrame frame, DynamicObject array, Object value) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);

            for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObjectArray(array)")
        public boolean includeObject(VirtualFrame frame, DynamicObject array, Object value) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                final Object stored = store[n];

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

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "!isInteger(object)", "!isLong(object)", "wasProvided(object)", "!isRubyArray(object)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject array, Object object, NotProvided defaultValue, NotProvided block) {

            DynamicObject copy = null;
            if (respondToToAryNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToAryNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
            if (respondToToAryNode.doesRespondToString(frame, object, create7BitString(StringOperations.encodeByteList("to_ary", UTF8Encoding.INSTANCE)), true)) {
                if (toAryNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toAryNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                }
                Object toAryResult = toAryNode.call(frame, object, "to_ary", null);
                if (RubyGuards.isRubyArray(toAryResult)) {
                    copy = (DynamicObject) toAryResult;
                }

            }

            if (copy != null) {
                return initialize(array, copy, NotProvided.INSTANCE, NotProvided.INSTANCE);
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                int size = toIntNode.doInt(frame, object);
                if (size < 0) {
                    return initializeNegative(array, size, NotProvided.INSTANCE, NotProvided.INSTANCE);
                } else {
                    return initialize(array, size, NotProvided.INSTANCE, NotProvided.INSTANCE);
                }

            }

        }
        
        @Specialization
        public DynamicObject initialize(DynamicObject array, NotProvided size, NotProvided defaultValue, NotProvided block) {
            return initialize(array, 0, nil(), block);
        }

        @Specialization
        public DynamicObject initialize(DynamicObject array, NotProvided size, NotProvided defaultValue, DynamicObject block) {
            return initialize(array, 0, nil(), NotProvided.INSTANCE);
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initialize(DynamicObject array, int size, NotProvided defaultValue, NotProvided block) {
            return initialize(array, size, nil(), block);
        }

        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegative(DynamicObject array, int size, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initialize(DynamicObject array, long size, NotProvided defaultValue, NotProvided block) {
            if (size > Integer.MAX_VALUE) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("array size too big", this));
            }
            return initialize(array, (int) size, nil(), block);
        }

        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegative(DynamicObject array, long size, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initialize(DynamicObject array, int size, int defaultValue, NotProvided block) {
            final int[] store = new int[size];
            Arrays.fill(store, defaultValue);
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, size);
            return array;
        }

        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegative(DynamicObject array, int size, int defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initialize(DynamicObject array, int size, long defaultValue, NotProvided block) {
            final long[] store = new long[size];
            Arrays.fill(store, defaultValue);
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, size);
            return array;
        }

        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegative(DynamicObject array, int size, long defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public DynamicObject initialize(DynamicObject array, int size, double defaultValue, NotProvided block) {
            final double[] store = new double[size];
            Arrays.fill(store, defaultValue);
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, size);
            return array;
        }

        @Specialization(guards = "size < 0")
        public DynamicObject initializeNegative(DynamicObject array, int size, double defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size >= 0" })
        public DynamicObject initialize(DynamicObject array, int size, Object defaultValue, NotProvided block) {
            final Object[] store = new Object[size];
            Arrays.fill(store, defaultValue);
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, size);
            return array;
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size < 0" })
        public DynamicObject initializeNegative(DynamicObject array, int size, Object defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = { "wasProvided(sizeObject)", "!isInteger(sizeObject)", "wasProvided(defaultValue)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject array, Object sizeObject, Object defaultValue, NotProvided block) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int size = toIntNode.doInt(frame, sizeObject);
            if (size < 0) {
                return initializeNegative(array, size, defaultValue, NotProvided.INSTANCE);
            } else {
                return initialize(array, size, defaultValue, NotProvided.INSTANCE);
            }

        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size >= 0" })
        public Object initialize(VirtualFrame frame, DynamicObject array, int size, Object defaultValue, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            return initializeBlock(frame, array, size, NotProvided.INSTANCE, block, arrayBuilder);
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size < 0" })
        public Object initializeNegative(VirtualFrame frame, DynamicObject array, int size, Object defaultValue, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public Object initializeBlock(VirtualFrame frame, DynamicObject array, int size, NotProvided defaultValue, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            Object store = arrayBuilder.start(size);

            int count = 0;
            int n = 0;
            try {
                for (; n < size; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.appendValue(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }

                Layouts.ARRAY.setStore(array, arrayBuilder.finish(store, n));
                Layouts.ARRAY.setSize(array, n);
            }

            return array;
        }

        @Specialization(guards = "size < 0")
        public Object initializeNegative(VirtualFrame frame, DynamicObject array, int size, NotProvided defaultValue, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "isRubyArray(copy)")
        public DynamicObject initialize(DynamicObject array, DynamicObject copy, NotProvided defaultValue, Object maybeBlock) {
            CompilerDirectives.transferToInterpreter();
            Layouts.ARRAY.setStore(array, ArrayOperations.toObjectArray(copy));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(copy));
            return array;
        }

    }

    @CoreMethod(names = "initialize_copy", required = 1, raiseIfFrozenSelf = true)
    @NodeChildren({
            @NodeChild(type = RubyNode.class, value = "self"),
            @NodeChild(type = RubyNode.class, value = "from")
    })
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeCopyNode extends CoreMethodNode {
        // TODO(cs): what about allocationSite ?

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("from") public RubyNode coerceOtherToAry(RubyNode other) {
            return ToAryNodeGen.create(getContext(), getSourceSection(), other);
        }

        @Specialization(guards = {"isRubyArray(from)", "isNullArray(from)"})
        public DynamicObject initializeCopyNull(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            Layouts.ARRAY.setStore(self, null);
            Layouts.ARRAY.setSize(self, 0);
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isIntArray(from)"})
        public DynamicObject initializeCopyIntegerFixnum(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            final int[] store = (int[]) Layouts.ARRAY.getStore(from);
            Layouts.ARRAY.setStore(self, store.clone());
            Layouts.ARRAY.setSize(self, Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isLongArray(from)"})
        public DynamicObject initializeCopyLongFixnum(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            final long[] store = (long[]) Layouts.ARRAY.getStore(from);
            Layouts.ARRAY.setStore(self, store.clone());
            Layouts.ARRAY.setSize(self, Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isDoubleArray(from)"})
        public DynamicObject initializeCopyFloat(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            final double[] store = (double[]) Layouts.ARRAY.getStore(from);
            Layouts.ARRAY.setStore(self, store.clone());
            Layouts.ARRAY.setSize(self, Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isObjectArray(from)"})
        public DynamicObject initializeCopyObject(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(from);
            Layouts.ARRAY.setStore(self, ArrayUtils.copy(store));
            Layouts.ARRAY.setSize(self, Layouts.ARRAY.getSize(from));
            return self;
        }

    }

    @CoreMethod(names = {"inject", "reduce"}, needsBlock = true, optional = 2)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode dispatch;

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.CALL_METHOD_MISSING);
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectEmptyArray(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return initial;
        }

        @Specialization(guards = "isEmptyArray(array)")
        public Object injectEmptyArrayNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = { "isIntArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectIntegerFixnum(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isIntArray(array)", "!isEmptyArray(array)" })
        public Object injectIntegerFixnumNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isLongArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectLongFixnum(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isLongArray(array)", "!isEmptyArray(array)" })
        public Object injectLongFixnumNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectFloat(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isEmptyArray(array)" })
        public Object injectFloatNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isObjectArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectObject(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isObjectArray(array)", "!isEmptyArray(array)" })
        public Object injectObjectNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isNullArray(array)", "wasProvided(initial)" })
        public Object injectNull(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return initial;
        }

        @Specialization(guards = "isNullArray(array)")
        public Object injectNullNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolEmptyArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return initial;
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isEmptyArray(array)" })
        public Object injectSymbolEmptyArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            return nil();
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isIntArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolIntArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isIntArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolIntArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isLongArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolLongArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isLongArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolLongArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isDoubleArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolDoubleArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isDoubleArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolDoubleArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isObjectArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolObjectArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isObjectArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolObjectArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        private Object injectHelper(VirtualFrame frame, ArrayMirror mirror, DynamicObject array, Object initial, DynamicObject block, int startIndex) {
            int count = 0;

            Object accumulator = initial;

            try {
                for (int n = startIndex; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, mirror.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }


        private Object injectSymbolHelper(VirtualFrame frame, ArrayMirror mirror, DynamicObject array, Object initial, DynamicObject symbol, int startIndex) {
            int count = 0;

            Object accumulator = initial;

            try {
                for (int n = startIndex; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = dispatch.call(frame, accumulator, symbol, null, mirror.get(n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

    }

    @CoreMethod(names = "insert", raiseIfFrozenSelf = true, rest = true, required = 1, optional = 1)
    public abstract static class InsertNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object insertMissingValue(VirtualFrame frame, DynamicObject array, Object idx, NotProvided value, Object[] values) {
            return array;
        }

        @Specialization(guards = { "isNullArray(array)", "wasProvided(value)", "values.length == 0" })
        public Object insertNull(DynamicObject array, int idx, Object value, Object[] values) {
            CompilerDirectives.transferToInterpreter();
            final int index = normalizeInsertIndex(array, idx);
            final Object[] store = new Object[index + 1];
            Arrays.fill(store, nil());
            store[index] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, index + 1);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "values.length == 0", "idx >= 0", "isIndexSmallerThanSize(idx,array)", "hasRoomForOneExtra(array)" })
        public Object insert(VirtualFrame frame, DynamicObject array, int idx, int value, Object[] values) {
            final int index = idx;
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            System.arraycopy(store, index, store, index + 1, Layouts.ARRAY.getSize(array) - index);
            store[index] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) + 1);
            return array;
        }

        @Specialization
        public Object insertBoxed(VirtualFrame frame, DynamicObject array, Object idxObject, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.extractUserArgumentsFrom(frame.getArguments(), 1);
            final int idx = toInt(frame, idxObject);

            CompilerDirectives.transferToInterpreter();
            final int index = normalizeInsertIndex(array, idx);

            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = (index < oldSize ? oldSize : index) + values.length;
            final Object[] store = ArrayUtils.boxExtra(Layouts.ARRAY.getStore(array), newSize - oldSize);

            if (index >= oldSize) {
                Arrays.fill(store, oldSize, index, nil());
            } else {
                final int dest = index + values.length;
                final int len = oldSize - index;
                System.arraycopy(store, index, store, dest, len);
            }

            System.arraycopy(values, 0, store, index, values.length);

            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);

            return array;
        }

        private int normalizeInsertIndex(DynamicObject array, int index) {
            final int normalizedIndex = normalizeInsertIndex(Layouts.ARRAY.getSize(array), index);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + index + " too small for array; minimum: " + Integer.toString(-Layouts.ARRAY.getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }
            return normalizedIndex;
        }

        private static int normalizeInsertIndex(int length, int index) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
                return length + index + 1;
            } else {
                return index;
            }
        }

        protected static boolean isIndexSmallerThanSize(int idx, DynamicObject array) {
            return idx <= Layouts.ARRAY.getSize(array);
        }

        protected static boolean hasRoomForOneExtra(DynamicObject array) {
            return ((int[]) Layouts.ARRAY.getStore(array)).length > Layouts.ARRAY.getSize(array);
        }

        private int toInt(VirtualFrame frame, Object indexObject) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            return toIntNode.doInt(frame, indexObject);
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mapNull(DynamicObject array, DynamicObject block) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object mapIntegerFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            final int arraySize = Layouts.ARRAY.getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.appendValue(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isLongArray(array)")
        public Object mapLongFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);
            final int arraySize = Layouts.ARRAY.getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.appendValue(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object mapFloat(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);
            final int arraySize = Layouts.ARRAY.getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.appendValue(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object mapObject(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
            final int arraySize = Layouts.ARRAY.getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.appendValue(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child private ArrayWriteNormalizedNode writeNode;

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mapInPlaceNull(DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object mapInPlaceFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    write(frame, array, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }


            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object mapInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    write(frame, array, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }


            return array;
        }

        private Object write(VirtualFrame frame, DynamicObject array, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return writeNode.executeWrite(array, index, value);
        }
    }

    // TODO: move into Enumerable?

    @CoreMethod(names = "max")
    public abstract static class MaxNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MaxBlock maxBlock;

        public MaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            maxBlock = context.getCoreLibrary().getArrayMaxBlock();
        }

        @Specialization
        public Object max(VirtualFrame frame, DynamicObject array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> maximum = new Memo<>();

            final InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(
                    RubyArguments.pack(method, null, null, array, null, DeclarationContext.BLOCK, new Object[] {}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final DynamicObject block = ProcNodes.createRubyProc(getContext().getCoreLibrary().getProcFactory(), ProcNodes.Type.PROC,
                    maxBlock.getSharedMethodInfo(), maxBlock.getCallTarget(), maxBlock.getCallTarget(),
                    maximumClosureFrame.materialize(), method, array, null);

            eachNode.call(frame, array, "each", block);

            if (maximum.get() == null) {
                return nil();
            } else {
                return maximum.get();
            }
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

            // TODO(CS): cast

            final Object current = maximum.get();

            if (current == null || (int) compareNode.call(frame, value, "<=>", null, current) > 0) {
                maximum.set(value);
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

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(
                    context, sourceSection, null, sharedMethodInfo,
                    ArrayNodesFactory.MaxBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                            new ReadDeclarationVariableNode(context, sourceSection, 1, frameSlot),
                            new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR)
                    })));
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

    @CoreMethod(names = "min")
    public abstract static class MinNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MinBlock minBlock;

        public MinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            minBlock = context.getCoreLibrary().getArrayMinBlock();
        }

        @Specialization
        public Object min(VirtualFrame frame, DynamicObject array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> minimum = new Memo<>();

            final InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(
                    RubyArguments.pack(method, null, null, array, null, DeclarationContext.BLOCK, new Object[] {}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final DynamicObject block = ProcNodes.createRubyProc(getContext().getCoreLibrary().getProcFactory(), ProcNodes.Type.PROC,
                    minBlock.getSharedMethodInfo(), minBlock.getCallTarget(), minBlock.getCallTarget(),
                    minimumClosureFrame.materialize(), method, array, null);

            eachNode.call(frame, array, "each", block);

            if (minimum.get() == null) {
                return nil();
            } else {
                return minimum.get();
            }
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

            // TODO(CS): cast

            final Object current = minimum.get();

            if (current == null || (int) compareNode.call(frame, value, "<=>", null, current) < 0) {
                minimum.set(value);
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

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(
                    context, sourceSection, null, sharedMethodInfo,
                    ArrayNodesFactory.MinBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                            new ReadDeclarationVariableNode(context, sourceSection, 1, frameSlot),
                            new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR)
                    })));
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

        @Child private TaintNode taintNode;

        public PackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyString(format)", "byteListsEqual(format, cachedFormat)"}, limit = "getCacheLimit()")
        public DynamicObject packCached(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject format,
                @Cached("privatizeByteList(format)") ByteList cachedFormat,
                @Cached("create(compileFormat(format))") DirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, new Object[]{Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishPack(cachedFormat, result);
        }

        @Specialization(contains = "packCached", guards = "isRubyString(format)")
        public DynamicObject packUncached(
                VirtualFrame frame,
                DynamicObject array,
                DynamicObject format,
                @Cached("create()") IndirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, compileFormat(format), new Object[]{Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishPack(StringOperations.getByteList(format), result);
        }

        private RuntimeException handleException(PackException exception) {
            try {
                throw exception;
            } catch (TooFewArgumentsException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("too few arguments", this));
            } catch (NoImplicitConversionException e) {
                return new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(e.getObject(), e.getTarget(), this));
            } catch (OutsideOfStringException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("X outside of string", this));
            } catch (CantCompressNegativeException e) {
                return new RaiseException(getContext().getCoreLibrary().argumentError("can't compress negative numbers", this));
            } catch (RangeException e) {
                return new RaiseException(getContext().getCoreLibrary().rangeError(e.getMessage(), this));
            } catch (CantConvertException e) {
                return new RaiseException(getContext().getCoreLibrary().typeError(e.getMessage(), this));
            }
        }

        private DynamicObject finishPack(ByteList format, PackResult result) {
            final DynamicObject string = createString(new ByteList(result.getOutput(), 0, result.getOutputLength()));

            if (format.length() == 0) {
                StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
            } else {
                switch (result.getEncoding()) {
                    case DEFAULT:
                    case ASCII_8BIT:
                        break;
                    case US_ASCII:
                        StringOperations.forceEncoding(string, USASCIIEncoding.INSTANCE);
                        break;
                    case UTF_8:
                        StringOperations.forceEncoding(string, UTF8Encoding.INSTANCE);
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }

            if (result.isTainted()) {
                if (taintNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    taintNode = insert(TaintNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
                }

                taintNode.executeTaint(string);
            }

            return string;
        }

        @Specialization
        public Object pack(VirtualFrame frame, DynamicObject array, boolean format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object pack(VirtualFrame frame, DynamicObject array, int format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object pack(VirtualFrame frame, DynamicObject array, long format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = "isNil(format)")
        public Object packNil(VirtualFrame frame, DynamicObject array, Object format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = {"!isRubyString(format)", "!isBoolean(format)", "!isInteger(format)", "!isLong(format)", "!isNil(format)"})
        public Object pack(VirtualFrame frame, DynamicObject array, Object format) {
            return ruby(frame, "pack(format.to_str)", "format", format);
        }

        @TruffleBoundary
        protected CallTarget compileFormat(DynamicObject format) {
            assert RubyGuards.isRubyString(format);
            try {
                return new PackParser(getContext()).parse(format.toString(), false);
            } catch (FormatException e) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError(e.getMessage(), this));
            }
        }

        protected int getCacheLimit() {
            return getContext().getOptions().PACK_CACHE;
        }

    }

    @CoreMethod(names = "pop", raiseIfFrozenSelf = true, optional = 1)
    public abstract static class PopNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private PopOneNode popOneNode;

        public PopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object pop(DynamicObject array, NotProvided n) {
            if (popOneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                popOneNode = insert(PopOneNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return popOneNode.executePopOne((DynamicObject) array);
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object popNilWithNum(VirtualFrame frame, DynamicObject array, Object object) {
            if (object instanceof Integer && ((Integer) object) < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int n = toIntNode.doInt(frame, object);
                if (n < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
                }
            }
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popLongFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object popLongFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popFloatInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;}
        }

        @Specialization(contains = "popFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object popFloatWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;}
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object popObjectWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final Object[] store = ((Object[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final Object[] store = ((Object[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numPop);
                return result;
            }
        }


    }

    @CoreMethod(names = "<<", raiseIfFrozenSelf = true, required = 1)
    public abstract static class LeftShiftNode extends ArrayCoreMethodNode {

        @Child private AppendOneNode appendOneNode;

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            appendOneNode = AppendOneNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject leftShift(DynamicObject array, Object value) {
            return appendOneNode.executeAppendOne(array, value);
        }

    }

    @CoreMethod(names = { "push", "__append__" }, rest = true, optional = 1, raiseIfFrozenSelf = true)
    public abstract static class PushNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isNullArray(array)", "values.length == 0" })
        public DynamicObject pushNullEmptySingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            Layouts.ARRAY.setStore(array, new int[] { value });
            Layouts.ARRAY.setSize(array, 1);
            return array;
        }

        @Specialization(guards = { "isNullArray(array)", "values.length == 0" })
        public DynamicObject pushNullEmptySingleIntegerLong(DynamicObject array, long value, Object[] values) {
            Layouts.ARRAY.setStore(array, new long[] { value });
            Layouts.ARRAY.setSize(array, 1);
            return array;
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject pushNullEmptyObjects(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            Layouts.ARRAY.setStore(array, values);
            Layouts.ARRAY.setSize(array, values.length);
            return array;
        }

        @Specialization(guards = { "!isNullArray(array)", "isEmptyArray(array)" })
        public DynamicObject pushEmptySingleIntegerFixnum(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 20-Apr-15 in reality might be better reusing any current storage, but won't worry about that for now
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            Layouts.ARRAY.setStore(array, values);
            Layouts.ARRAY.setSize(array, values.length);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "values.length == 0" })
        public DynamicObject pushIntegerFixnumSingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "wasProvided(value)", "values.length == 0", "!isInteger(value)", "!isLong(value)" })
        public DynamicObject pushIntegerFixnumSingleOther(DynamicObject array, Object value, Object[] values) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
            final Object[] store;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);
            } else {
                store = ArrayUtils.box(oldStore);
            }

            store[oldSize] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "wasProvided(value)", "rest.length != 0" })
        public DynamicObject pushIntegerFixnum(VirtualFrame frame, DynamicObject array, Object value, Object[] rest) {
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());

            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + values.length;

            int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
            final Object[] store;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);
            } else {
                store = ArrayUtils.box(oldStore);
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = { "isLongArray(array)", "values.length == 0" })
        public DynamicObject pushLongFixnumSingleIntegerFixnum(DynamicObject array, int value, Object[] values) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (long) value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = { "isLongArray(array)", "values.length == 0" })
        public DynamicObject pushLongFixnumSingleLongFixnum(DynamicObject array, long value, Object[] values) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject pushFloat(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store
            if (Layouts.ARRAY.getSize(array) != 0) {
                throw new UnsupportedOperationException();
            }

            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            Layouts.ARRAY.setStore(array, values);
            Layouts.ARRAY.setSize(array, values.length);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject pushObject(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());

            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + values.length;

            Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.grow(store, ArrayUtils.capacity(store.length, newSize));
            }
            ;
            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

    }

    // Not really a core method - used internally

    public abstract static class PushOneNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushOneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject pushEmpty(DynamicObject array, Object value) {
            Layouts.ARRAY.setStore(array, new Object[]{value});
            Layouts.ARRAY.setSize(array, 1);
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public DynamicObject pushIntegerFixnumIntegerFixnum(DynamicObject array, int value) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                Object store1 = store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
                Layouts.ARRAY.setStore(array, store1);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            }

            store[oldSize] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(value)" })
        public DynamicObject pushIntegerFixnumObject(DynamicObject array, Object value) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            final int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
            final Object[] newStore;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                newStore = ArrayUtils.boxExtra(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);
            } else {
                newStore = ArrayUtils.box(oldStore);
            }

            newStore[oldSize] = value;
            Layouts.ARRAY.setStore(array, newStore);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject pushObjectObject(DynamicObject array, Object value) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                Object store1 = store = ArrayUtils.grow(store, ArrayUtils.capacity(store.length, newSize));
                Layouts.ARRAY.setStore(array, store1);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            }

            store[oldSize] = value;
            Layouts.ARRAY.setStore(array, store);
            Layouts.ARRAY.setSize(array, newSize);
            return array;
        }

    }

    @CoreMethod(names = "reject", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectNode extends YieldingCoreMethodNode {

        public RejectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            Object selectedStore = arrayBuilder.start(Layouts.ARRAY.getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    CompilerDirectives.transferToInterpreter();

                    if (! yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            Object selectedStore = arrayBuilder.start(Layouts.ARRAY.getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
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
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "delete_if" , needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteIfNode extends YieldingCoreMethodNode {

        public DeleteIfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
            }
            return array;
        }

    }


    @CoreMethod(names = "reject!", needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        public RejectInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final double[] store = (double[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int i = 0;
            int n = 0;
            for (; n < Layouts.ARRAY.getSize(array); n++) {
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
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, i);
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

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("other") public RubyNode coerceOtherToAry(RubyNode index) {
            return ToAryNodeGen.create(getContext(), getSourceSection(), index);
        }

        @Specialization(guards = {"isRubyArray(other)", "isNullArray(other)"})
        public DynamicObject replace(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            Layouts.ARRAY.setStore(array, null);
            Layouts.ARRAY.setSize(array, 0);
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isIntArray(other)"})
        public DynamicObject replaceIntegerFixnum(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            Layouts.ARRAY.setStore(array, Arrays.copyOf((int[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isLongArray(other)"})
        public DynamicObject replaceLongFixnum(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            Layouts.ARRAY.setStore(array, Arrays.copyOf((long[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isDoubleArray(other)"})
        public DynamicObject replaceFloat(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            Layouts.ARRAY.setStore(array, Arrays.copyOf((double[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isObjectArray(other)"})
        public DynamicObject replaceObject(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            Layouts.ARRAY.setStore(array, Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(other));
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        public SelectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            Object selectedStore = arrayBuilder.start(Layouts.ARRAY.getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.appendValue(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block,
                @Cached("create(getContext())") ArrayBuilderNode arrayBuilder) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            Object selectedStore = arrayBuilder.start(Layouts.ARRAY.getSize(array));
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
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
                    getRootNode().reportLoopCount(count);
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "shift", raiseIfFrozenSelf = true, optional = 1)
    public abstract static class ShiftNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;

        public ShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeShift(VirtualFrame frame, DynamicObject array, Object n);

        @Specialization(guards = "isEmptyArray(array)")
        public Object shiftNil(VirtualFrame frame, DynamicObject array, NotProvided n) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public int shiftIntegerFixnumInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftIntegerFixnumInBounds", guards = "isIntArray(array)")
        public Object shiftIntegerFixnum(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public long shiftLongFixnumInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBounds", guards = "isLongArray(array)")
        public Object shiftLongFixnum(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public double shiftFloatInBounds(VirtualFrame frame, DynamicObject array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftFloatInBounds", guards = "isDoubleArray(array)")
        public Object shiftFloat(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObject(VirtualFrame frame, DynamicObject array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final Object[] store = ((Object[]) Layouts.ARRAY.getStore(array));
                final Object value = store[0];
                System.arraycopy(store, 1, store, 0, Layouts.ARRAY.getSize(array) - 1);
                final Object[] filler = new Object[1];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - 1, 1);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object shiftNilWithNum(VirtualFrame frame, DynamicObject array, Object object) {
            if (object instanceof Integer && ((Integer) object) < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                final int n = toIntNode.doInt(frame, object);
                if (n < 0) {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
                }
            }
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftLongFixnumInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object shiftLongFixnumWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public DynamicObject shiftFloatInBoundsWithNum(VirtualFrame frame, DynamicObject array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object shiftFloatWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObjectWithNum(VirtualFrame frame, DynamicObject array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final Object[] store = ((Object[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final int[] store = ((int[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final long[] store = ((long[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final double[] store = ((double[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, Layouts.ARRAY.getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = Layouts.ARRAY.getSize(array) < num ? Layouts.ARRAY.getSize(array) : num;
                final Object[] store = ((Object[]) Layouts.ARRAY.getStore(array));
                final DynamicObject result = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                Layouts.ARRAY.setStore(array, store);
                Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) - numShift);
                return result;
            }
        }
    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends ArrayCoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject array) {
            return Layouts.ARRAY.getSize(array);
        }

    }

    @CoreMethod(names = "sort", needsBlock = true)
    public abstract static class SortNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode compareDispatchNode;
        @Child private YieldDispatchHeadNode yieldNode;

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = DispatchHeadNodeFactory.createMethodCall(context);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject sortNull(DynamicObject array, Object unusedBlock) {
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), null, 0);
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortIntegerFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            final int[] newStore = new int[store.length];

            final int size = Layouts.ARRAY.getSize(array);

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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), newStore, size);
        }

        @ExplodeLoop
        @Specialization(guards = {"isLongArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortLongFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);
            final long[] newStore = new long[store.length];

            final int size = Layouts.ARRAY.getSize(array);

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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), newStore, size);
        }

        @Specialization(guards = {"isObjectArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortObject(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final Object[] oldStore = (Object[]) Layouts.ARRAY.getStore(array);
            final Object[] store = ArrayUtils.copy(oldStore);

            // Insertion sort

            final int size = Layouts.ARRAY.getSize(array);

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

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), store, size);
        }

        @Specialization(guards = { "!isNullArray(array)" })
        public Object sortUsingRubinius(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return ruby(frame, "sorted = dup; Rubinius.privately { sorted.isort_block!(0, right, block) }; sorted", "right", Layouts.ARRAY.getSize(array), "block", block);
        }

        @Specialization(guards = { "!isNullArray(array)", "!isSmall(array)" })
        public Object sortUsingRubinius(VirtualFrame frame, DynamicObject array, NotProvided block) {
            return ruby(frame, "sorted = dup; Rubinius.privately { sorted.isort!(0, right) }; sorted", "right", Layouts.ARRAY.getSize(array));
        }

        private int castSortValue(Object value) {
            if (value instanceof Integer) {
                return (int) value;
            }

            CompilerDirectives.transferToInterpreter();

            // TODO CS 14-Mar-15 - what's the error message here?
            throw new RaiseException(getContext().getCoreLibrary().argumentError("expecting a Fixnum to sort", this));
        }

        protected boolean isSmall(DynamicObject array) {
            return Layouts.ARRAY.getSize(array) <= getContext().getOptions().ARRAY_SMALL;
        }

    }

    @CoreMethod(names = "unshift", rest = true, raiseIfFrozenSelf = true)
    public abstract static class UnshiftNode extends CoreMethodArrayArgumentsNode {

        public UnshiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject unshift(DynamicObject array, Object... args) {
            CompilerDirectives.transferToInterpreter();

            assert RubyGuards.isRubyArray(array);
            final Object[] newStore = new Object[Layouts.ARRAY.getSize(array) + args.length];
            System.arraycopy(args, 0, newStore, 0, args.length);
            ArrayUtils.copy(Layouts.ARRAY.getStore(array), newStore, args.length, Layouts.ARRAY.getSize(array));
            Layouts.ARRAY.setStore(array, newStore);
            Layouts.ARRAY.setSize(array, newStore.length);
            return array;
        }

    }

    @CoreMethod(names = "zip", rest = true, required = 1, needsBlock = true)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode zipInternalCall;

        public ZipNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isIntArray(other)", "others.length == 0" })
        public DynamicObject zipObjectIntegerFixnum(DynamicObject array, DynamicObject other, Object[] others, NotProvided block) {
            final Object[] a = (Object[]) Layouts.ARRAY.getStore(array);

            final int[] b = (int[]) Layouts.ARRAY.getStore(other);
            final int bLength = Layouts.ARRAY.getSize(other);

            final int zippedLength = Layouts.ARRAY.getSize(array);
            final Object[] zipped = new Object[zippedLength];

            final boolean areSameLength = bLength == zippedLength;

            if (areSameLength) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], b[n]}, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], b[n]}, 2);
                    } else {
                        zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], nil()}, 2);
                    }
                }
            }

            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), zipped, zippedLength);
        }

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isObjectArray(other)", "others.length == 0" })
        public DynamicObject zipObjectObject(DynamicObject array, DynamicObject other, Object[] others, NotProvided block) {
            final Object[] a = (Object[]) Layouts.ARRAY.getStore(array);

            final Object[] b = (Object[]) Layouts.ARRAY.getStore(other);
            final int bLength = Layouts.ARRAY.getSize(other);

            final int zippedLength = Layouts.ARRAY.getSize(array);
            final Object[] zipped = new Object[zippedLength];

            final boolean areSameLength = bLength == zippedLength;

            if (areSameLength) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], b[n]}, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], b[n]}, 2);
                    } else {
                        zipped[n] = Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), new Object[]{a[n], nil()}, 2);
                    }
                }
            }


            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), zipped, zippedLength);
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

            final Object[] others = RubyArguments.extractUserArguments(frame.getArguments());

            return zipInternalCall.call(frame, array, "zip_internal", block, others);
        }

        protected static boolean fallback(DynamicObject array, DynamicObject other, Object[] others) {
            return !ArrayGuards.isObjectArray(array) || ArrayGuards.isNullArray(other) || ArrayGuards.isLongArray(other) || others.length > 0;
        }

    }

}
