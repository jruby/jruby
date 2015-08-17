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
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.pack.parser.PackParser;
import org.jruby.truffle.pack.runtime.PackResult;
import org.jruby.truffle.pack.runtime.exceptions.*;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.CoreLibrary;
import org.jruby.truffle.runtime.core.CoreSourceSection;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.Random;

@CoreClass(name = "Array")
public abstract class ArrayNodes {

    public static void setStore(DynamicObject array, Object store, int size) {
        assert verifyStore(store, size);

        if (RANDOMIZE_STORAGE_ARRAY) {
            store = randomizeStorageStrategy(BasicObjectNodes.getContext(array), store, size);
            assert verifyStore(store, size);
        }

        Layouts.ARRAY.setStore(array, store);
        Layouts.ARRAY.setSize(array, size);
    }

    public static final int ARRAYS_SMALL = Options.TRUFFLE_ARRAYS_SMALL.load();
    public static final boolean RANDOMIZE_STORAGE_ARRAY = Options.TRUFFLE_RANDOMIZE_STORAGE_ARRAY.load();
    private static final Random random = new Random(Options.TRUFFLE_RANDOMIZE_SEED.load());

    public static DynamicObject fromObject(DynamicObject arrayClass, Object object) {
        final Object store;

        if (object instanceof Integer) {
            store = new int[]{(int) object};
        } else if (object instanceof Long) {
            store = new long[]{(long) object};
        } else if (object instanceof Double) {
            store = new double[]{(double) object};
        } else {
            store = new Object[]{object};
        }

        return createGeneralArray(arrayClass, store, 1);
    }

    public static DynamicObject fromObjects(DynamicObject arrayClass, Object... objects) {
        return createGeneralArray(arrayClass, storeFromObjects(BasicObjectNodes.getContext(arrayClass), objects), objects.length);
    }

    private static Object storeFromObjects(RubyContext context, Object... objects) {
        if (objects.length == 0) {
            return null;
        }

        boolean canUseInteger = true;
        boolean canUseLong = true;
        boolean canUseDouble = true;

        for (Object object : objects) {
            if (object instanceof Integer) {
                canUseDouble = false;
            } else if (object instanceof Long) {
                canUseInteger = canUseInteger && CoreLibrary.fitsIntoInteger((long) object);
                canUseDouble = false;
            } else if (object instanceof Double) {
                canUseInteger = false;
                canUseLong = false;
            } else {
                canUseInteger = false;
                canUseLong = false;
                canUseDouble = false;
            }
        }

        if (canUseInteger) {
            final int[] store = new int[objects.length];

            for (int n = 0; n < objects.length; n++) {
                final Object object = objects[n];
                if (object instanceof Integer) {
                    store[n] = (int) object;
                } else if (object instanceof Long) {
                    store[n] = (int) (long) object;
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            return store;
        } else if (canUseLong) {
            final long[] store = new long[objects.length];

            for (int n = 0; n < objects.length; n++) {
                final Object object = objects[n];
                if (object instanceof Integer) {
                    store[n] = (long) (int) object;
                } else if (object instanceof Long) {
                    store[n] = (long) object;
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            return store;
        } else if (canUseDouble) {
            final double[] store = new double[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = CoreLibrary.toDouble(objects[n], context.getCoreLibrary().getNilObject());
            }

            return store;
        } else {
            return objects;
        }
    }

    public static int normalizeIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public static int clampExclusiveIndex(int length, int index) {
        if (index < 0) {
            return 0;
        } else if (index > length) {
            return length;
        } else {
            return index;
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static Object randomizeStorageStrategy(RubyContext context, Object store, int size) {
        // Use any type for empty arrays

        if (size == 0) {
            switch (random.nextInt(5)) {
                case 0:
                    return null;
                case 1:
                    return new int[]{};
                case 2:
                    return new long[]{};
                case 3:
                    return new double[]{};
                case 4:
                    return new Object[]{};
                default:
                    throw new IllegalStateException();
            }
        }

        // Convert to the canonical store type first

        final Object[] boxedStore = ArrayUtils.box(store);
        final Object canonicalStore = storeFromObjects(context, boxedStore);

        // Then promote it at random

        if (canonicalStore instanceof int[]) {
            switch (random.nextInt(3)) {
                case 0:
                    return boxedStore;
                case 1:
                    return ArrayUtils.longCopyOf((int[]) canonicalStore);
                case 2:
                    return canonicalStore;
                default:
                    throw new IllegalStateException();
            }
        } else if (canonicalStore instanceof long[]) {
            if (random.nextBoolean()) {
                return boxedStore;
            } else {
                return canonicalStore;
            }
        } else if (canonicalStore instanceof double[]) {
            if (random.nextBoolean()) {
                return boxedStore;
            } else {
                return canonicalStore;
            }
        } else if (canonicalStore instanceof Object[]) {
            return canonicalStore;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static Object[] slowToArray(DynamicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayUtils.boxUntil(Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array));
    }

    public static void slowUnshift(DynamicObject array, Object... values) {
        assert RubyGuards.isRubyArray(array);
        final Object[] newStore = new Object[Layouts.ARRAY.getSize(array) + values.length];
        System.arraycopy(values, 0, newStore, 0, values.length);
        ArrayUtils.copy(Layouts.ARRAY.getStore(array), newStore, values.length, Layouts.ARRAY.getSize(array));
        setStore(array, newStore, newStore.length);
    }

    public static void slowPush(DynamicObject array, Object value) {
        assert RubyGuards.isRubyArray(array);
        setStore(array, Arrays.copyOf(ArrayUtils.box(Layouts.ARRAY.getStore(array)), Layouts.ARRAY.getSize(array) + 1), Layouts.ARRAY.getSize(array));
        ((Object[]) Layouts.ARRAY.getStore(array))[Layouts.ARRAY.getSize(array)] = value;
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array) + 1);
    }

    public static int normalizeIndex(DynamicObject array, int index) {
        assert RubyGuards.isRubyArray(array);
        return normalizeIndex(Layouts.ARRAY.getSize(array), index);
    }

    public static int clampExclusiveIndex(DynamicObject array, int index) {
        assert RubyGuards.isRubyArray(array);
        return clampExclusiveIndex(Layouts.ARRAY.getSize(array), index);
    }

    private static boolean verifyStore(Object store, int size) {
        assert size >= 0;

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;

        if (store instanceof Object[]) {
            for (int n = 0; n < size; n++) {
                assert ((Object[]) store)[n] != null : String.format("array of size %s had null at %d", size, n);
            }
        }

        return true;
    }

    public static DynamicObject createEmptyArray(DynamicObject arrayClass) {
        return createGeneralArray(arrayClass, null, 0);
    }

    public static DynamicObject createArray(DynamicObject arrayClass, int[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static DynamicObject createArray(DynamicObject arrayClass, long[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static DynamicObject createArray(DynamicObject arrayClass, double[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static DynamicObject createArray(DynamicObject arrayClass, Object[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static DynamicObject createGeneralArray(DynamicObject arrayClass, Object store, int size) {
        return Layouts.ARRAY.createArray(Layouts.CLASS.getInstanceFactory(arrayClass), store, size);
    }

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
            return createEmptyArray();
        }

        @Specialization(guards = {"isObjectArray(a)", "isNullArray(b)"})
        public DynamicObject addObjectNull(DynamicObject a, DynamicObject b) {
            return createArray(Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(a), Layouts.ARRAY.getSize(a)), Layouts.ARRAY.getSize(a));
        }

        @Specialization(guards = {"isIntArray(a)", "isIntArray(b)"})
        public DynamicObject addBothIntegerFixnum(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final int[] combined = new int[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isLongArray(a)", "isLongArray(b)"})
        public DynamicObject addBothLongFixnum(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final long[] combined = new long[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isDoubleArray(a)", "isRubyArray(b)", "isDoubleArray(b)"})
        public DynamicObject addBothFloat(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final double[] combined = new double[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addBothObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isIntArray(b)"})
        public DynamicObject addNullIntegerFixnum(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return createArray(Arrays.copyOf((int[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isLongArray(b)"})
        public DynamicObject addNullLongFixnum(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return createArray(Arrays.copyOf((long[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addNullObject(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return createArray(Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(b), size), size);
        }

        @Specialization(guards = {"!isObjectArray(a)", "isRubyArray(b)", "isObjectArray(b)"})
        public DynamicObject addOtherObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(ArrayUtils.box(Layouts.ARRAY.getStore(a)), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(Layouts.ARRAY.getStore(b), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "isRubyArray(b)", "!isObjectArray(b)"})
        public DynamicObject addObject(DynamicObject a, DynamicObject b) {
            final int combinedSize = Layouts.ARRAY.getSize(a) + Layouts.ARRAY.getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(Layouts.ARRAY.getStore(a), 0, combined, 0, Layouts.ARRAY.getSize(a));
            System.arraycopy(ArrayUtils.box(Layouts.ARRAY.getStore(b)), 0, combined, Layouts.ARRAY.getSize(a), Layouts.ARRAY.getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isEmptyArray(a)", "isRubyArray(b)"})
        public DynamicObject addEmpty(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(b);
            return createArray(ArrayUtils.box(Layouts.ARRAY.getStore(b)), size);
        }

        @Specialization(guards = {"isEmptyArray(b)", "isRubyArray(b)"})
        public DynamicObject addOtherEmpty(DynamicObject a, DynamicObject b) {
            final int size = Layouts.ARRAY.getSize(a);
            return createArray(ArrayUtils.box(Layouts.ARRAY.getStore(a)), size);
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0, taintFromSelf = true)
    public abstract static class MulNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.RespondToNode respondToToStrNode;
        @Child private ToIntNode toIntNode;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject mulEmpty(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            return ArrayNodes.createEmptyArray(BasicObjectNodes.getLogicalClass(array));
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

            return ArrayNodes.createArray(BasicObjectNodes.getLogicalClass(array), newStore, newStoreLength);
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

            return ArrayNodes.createArray(BasicObjectNodes.getLogicalClass(array), newStore, newStoreLength);
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

            return ArrayNodes.createArray(BasicObjectNodes.getLogicalClass(array), newStore, newStoreLength);
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject mulObject(DynamicObject array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final Object[] newStore = new Object[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return ArrayNodes.createArray(BasicObjectNodes.getLogicalClass(array), newStore, newStoreLength);
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
            if (respondToToStrNode.doesRespondToString(frame, object, createString("to_str"), false)) {
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

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
        public Object slice(VirtualFrame frame, DynamicObject array, int start, int length) {
            if (length < 0) {
                return nil();
            }

            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            return readSliceNode.executeReadSlice(frame, (DynamicObject) array, start, length);
        }

        @Specialization(guards = "isIntegerFixnumRange(range)")
        public Object slice(VirtualFrame frame, DynamicObject array, DynamicObject range, NotProvided len) {
            final int normalizedIndex = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)));

            if (normalizedIndex < 0 || normalizedIndex > Layouts.ARRAY.getSize(array)) {
                return nil();
            } else {
                final int end = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)));
                final int exclusiveEnd = clampExclusiveIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range)) ? end : end + 1);

                if (exclusiveEnd <= normalizedIndex) {
                    return ArrayNodes.createEmptyArray(BasicObjectNodes.getLogicalClass(array));
                }

                final int length = exclusiveEnd - normalizedIndex;

                if (readNormalizedSliceNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    readNormalizedSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }

                return readNormalizedSliceNode.executeReadSlice(frame, (DynamicObject) array, normalizedIndex, length);
            }
        }

        @Specialization(guards = {"!isInteger(a)", "!isIntegerFixnumRange(a)"})
        public Object fallbackIndex(VirtualFrame frame, DynamicObject array, Object a, NotProvided length) {
            return fallback(frame, array, fromObjects(getContext().getCoreLibrary().getArrayClass(), a));
        }

        @Specialization(guards = { "!isIntegerFixnumRange(a)", "wasProvided(b)" })
        public Object fallbackSlice(VirtualFrame frame, DynamicObject array, Object a, Object b) {
            return fallback(frame, array, fromObjects(getContext().getCoreLibrary().getArrayClass(), a, b));
        }

        public Object fallback(VirtualFrame frame, DynamicObject array, DynamicObject args) {
            if (fallbackNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            InternalMethod method = RubyArguments.getMethod(frame.getArguments());
            return fallbackNode.call(frame, array, "element_reference_fallback", null,
                    createString(method.getName()), args);
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        @Child private ArrayWriteDenormalizedNode writeNode;
        @Child protected ArrayReadSliceDenormalizedNode readSliceNode;
        @Child private PopOneNode popOneNode;
        @Child private ToIntNode toIntNode;

        private final BranchProfile tooSmallBranch = BranchProfile.create();

        public IndexSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"!isInteger(indexObject)", "!isIntegerFixnumRange(indexObject)"})
        public Object set(VirtualFrame frame, DynamicObject array, Object indexObject, Object value, NotProvided unused) {
            final int index = toInt(frame, indexObject);
            return set(frame, array, index, value, unused);
        }

        @Specialization
        public Object set(VirtualFrame frame, DynamicObject array, int index, Object value, NotProvided unused) {
            final int normalizedIndex = normalizeIndex(array, index);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + index + " too small for array; minimum: " + Integer.toString(-Layouts.ARRAY.getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }
            return write(frame, (DynamicObject) array, index, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(lengthObject)" })
        public Object setObject(VirtualFrame frame, DynamicObject array, int start, Object lengthObject, Object value) {
            int length = toInt(frame, lengthObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(startObject)" })
        public Object setObject(VirtualFrame frame, DynamicObject array, Object startObject, int length, Object value) {
            int start = toInt(frame, startObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(startObject)", "!isInteger(lengthObject)" })
        public Object setObject(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, Object value) {
            int length = toInt(frame, lengthObject);
            int start = toInt(frame, startObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)" })
        public Object setObject(VirtualFrame frame, DynamicObject array, int start, int length, Object value) {
            CompilerDirectives.transferToInterpreter();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int normalizedIndex = normalizeIndex(array, start);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "index " + start + " too small for array; minimum: " + Integer.toString(-Layouts.ARRAY.getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int begin = normalizeIndex(array, start);

            if (begin < Layouts.ARRAY.getSize(array) && length == 1) {
                return write(frame, array, begin, value);
            } else {
                if (Layouts.ARRAY.getSize(array) > (begin + length)) { // there is a tail, else other values discarded
                    if (readSliceNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                    }
                    DynamicObject endValues = (DynamicObject) readSliceNode.executeReadSlice(frame, (DynamicObject) array, (begin + length), (Layouts.ARRAY.getSize(array) - begin - length));
                    write(frame, array, begin, value);
                    Object[] endValuesStore = ArrayUtils.box(Layouts.ARRAY.getStore(endValues));

                    int i = begin + 1;
                    for (Object obj : endValuesStore) {
                        write(frame, array, i, obj);
                        i += 1;
                    }
                } else {
                    write(frame, array, begin, value);
                }
                if (popOneNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    popOneNode = insert(PopOneNodeGen.create(getContext(), getSourceSection(), null));
                }
                int popLength = length - 1 < Layouts.ARRAY.getSize(array) ? length - 1 : Layouts.ARRAY.getSize(array) - 1;
                for (int i = 0; i < popLength; i++) { // TODO 3-15-2015 BF update when pop can pop multiple
                    popOneNode.executePopOne(array);
                }
                return value;
            }
        }

        @Specialization(guards = {"!isInteger(startObject)", "isRubyArray(value)"})
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, Object startObject, int length, DynamicObject value) {
            int start = toInt(frame, startObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization(guards = {"!isInteger(lengthObject)", "isRubyArray(value)"})
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, int start, Object lengthObject, DynamicObject value) {
            int length = toInt(frame, lengthObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization(guards = {"!isInteger(startObject)", "!isInteger(lengthObject)", "isRubyArray(value)"})
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, Object startObject, Object lengthObject, DynamicObject value) {
            int start = toInt(frame, startObject);
            int length = toInt(frame, lengthObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization(guards = "isRubyArray(replacement)")
        public Object setOtherArray(VirtualFrame frame, DynamicObject array, int start, int length, DynamicObject replacement) {
            CompilerDirectives.transferToInterpreter();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int normalizedIndex = normalizeIndex(array, start);
            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", start, Layouts.ARRAY.getSize(array), this));
            }

            final int replacementLength = Layouts.ARRAY.getSize(replacement);
            final Object[] replacementStore = slowToArray(replacement);

            if (replacementLength == length) {
                for (int i = 0; i < length; i++) {
                    write(frame, array, start + i, replacementStore[i]);
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

                final Object store = slowToArray(array);
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

                setStore(array, newStore, newLength);
            }

            return replacement;
        }

        @Specialization(guards = {"!isRubyArray(other)", "isIntegerFixnumRange(range)"})
        public Object setRange(VirtualFrame frame, DynamicObject array, DynamicObject range, Object other, NotProvided unused) {
            final int normalizedStart = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)));
            int normalizedEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range)) ? normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range))) - 1 : normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)));
            if (normalizedEnd < 0) {
                normalizedEnd = -1;
            }
            final int length = normalizedEnd - normalizedStart + 1;
            if (normalizedStart < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }
            return setObject(frame, array, normalizedStart, length, other);
        }

        @Specialization(guards = {"isRubyArray(other)", "!isIntArray(array) || !isIntArray(other)", "isIntegerFixnumRange(range)"})
        public Object setRangeArray(VirtualFrame frame, DynamicObject array, DynamicObject range, DynamicObject other, NotProvided unused) {
            final int normalizedStart = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)));
            if (normalizedStart < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }

            int normalizedEnd = Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range)) ? normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range))) - 1 : normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)));
            if (normalizedEnd < 0) {
                normalizedEnd = -1;
            }
            final int length = normalizedEnd - normalizedStart + 1;

            return setOtherArray(frame, array, normalizedStart, length, other);
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyArray(other)", "isIntArray(other)", "isIntegerFixnumRange(range)"})
        public Object setIntegerFixnumRange(VirtualFrame frame, DynamicObject array, DynamicObject range, DynamicObject other, NotProvided unused) {
            if (Layouts.INTEGER_FIXNUM_RANGE.getExcludedEnd(((DynamicObject) range))) {
                CompilerDirectives.transferToInterpreter();
                return setRangeArray(frame, array, range, other, unused);
            } else {
                int normalizedBegin = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getBegin(((DynamicObject) range)));
                int normalizedEnd = normalizeIndex(array, Layouts.INTEGER_FIXNUM_RANGE.getEnd(((DynamicObject) range)));
                if (normalizedEnd < 0) {
                    normalizedEnd = -1;
                }
                if (normalizedBegin == 0 && normalizedEnd == Layouts.ARRAY.getSize(array) - 1) {
                    setStore(array, Arrays.copyOf((int[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)), Layouts.ARRAY.getSize(other));
                } else {
                    CompilerDirectives.transferToInterpreter();
                    return setRangeArray(frame, array, range, other, unused);
                }
            }

            return other;
        }

        private Object write(VirtualFrame frame, DynamicObject array, int index, Object value) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return writeNode.executeWrite(frame, array, index, value);
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
            setStore(array, Layouts.ARRAY.getStore(array), 0);
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
            return createArray(Arrays.copyOf((int[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
        }

        @Specialization(guards = "isLongArray(array)")
        public DynamicObject compactLong(DynamicObject array) {
            return createArray(Arrays.copyOf((long[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject compactDouble(DynamicObject array) {
            return createArray(Arrays.copyOf((double[]) Layouts.ARRAY.getStore(array), Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array));
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

            return createArray(newStore, m);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object compactNull(DynamicObject array) {
            return createEmptyArray();
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

            setStore(array, store, m);

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
                            getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(BasicObjectNodes.getLogicalClass(array)).getName(), this));
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
                setStore(array, store, i);
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
                            getContext().getCoreLibrary().frozenError(Layouts.MODULE.getFields(BasicObjectNodes.getLogicalClass(array)).getName(), this));
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
                setStore(array, store, i);
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

        private final BranchProfile tooSmallBranch = BranchProfile.create();
        private final BranchProfile beyondEndBranch = BranchProfile.create();

        public DeleteAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("index") public RubyNode coerceOtherToInt(RubyNode index) {
            return ToIntNodeGen.create(getContext(), getSourceSection(), index);
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public int deleteAtIntegerFixnumInBounds(DynamicObject array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int[] store = (int[]) Layouts.ARRAY.getStore(array);
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtIntegerFixnumInBounds", guards = "isIntArray(array)")
        public Object deleteAtIntegerFixnum(DynamicObject array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = Layouts.ARRAY.getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final int[] store = (int[]) Layouts.ARRAY.getStore(array);
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public long deleteAtLongFixnumInBounds(DynamicObject array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final long[] store = (long[]) Layouts.ARRAY.getStore(array);
                final long value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtLongFixnumInBounds", guards = "isLongArray(array)")
        public Object deleteAtLongFixnum(DynamicObject array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = Layouts.ARRAY.getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final long[] store = (long[]) Layouts.ARRAY.getStore(array);
                final long value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public double deleteAtFloatInBounds(DynamicObject array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final double[] store = (double[]) Layouts.ARRAY.getStore(array);
                final double value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtFloatInBounds", guards = "isDoubleArray(array)")
        public Object deleteAtFloat(DynamicObject array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = Layouts.ARRAY.getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final double[] store = (double[]) Layouts.ARRAY.getStore(array);
                final double value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)", rewriteOn = UnexpectedResultException.class)
        public Object deleteAtObjectInBounds(DynamicObject array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
                final Object value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtObjectInBounds", guards = "isObjectArray(array)")
        public Object deleteAtObject(DynamicObject array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = Layouts.ARRAY.getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= Layouts.ARRAY.getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
                final Object value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, Layouts.ARRAY.getSize(array) - normalizedIndex - 1);
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public Object eachNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isLongArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isDoubleArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public DynamicObject eachWithEmpty(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isLongArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isDoubleArray(array)", "isRubyProc(block)"})
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

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
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

    @CoreMethod(names = "initialize", needsBlock = true, optional = 2, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeNode extends YieldingCoreMethodNode {

        @Child private ToIntNode toIntNode;
        @Child private CallDispatchHeadNode toAryNode;
        @Child private KernelNodes.RespondToNode respondToToAryNode;
        @Child private ArrayBuilderNode arrayBuilder;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = { "!isInteger(object)", "!isLong(object)", "wasProvided(object)", "!isRubyArray(object)" })
        public DynamicObject initialize(VirtualFrame frame, DynamicObject array, Object object, NotProvided defaultValue, NotProvided block) {

            DynamicObject copy = null;
            if (respondToToAryNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToAryNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
            if (respondToToAryNode.doesRespondToString(frame, object, createString("to_ary"), true)) {
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

        @Specialization(guards = "isRubyProc(block)")
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
            setStore(array, store, size);
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
            setStore(array, store, size);
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
            setStore(array, store, size);
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
            setStore(array, store, size);
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

        @Specialization(guards = {"wasProvided(defaultValue)", "size >= 0", "isRubyProc(block)"})
        public Object initialize(VirtualFrame frame, DynamicObject array, int size, Object defaultValue, DynamicObject block) {
            return initialize(frame, array, size, NotProvided.INSTANCE, block);
        }

        @Specialization(guards = {"wasProvided(defaultValue)", "size < 0", "isRubyProc(block)"})
        public Object initializeNegative(VirtualFrame frame, DynamicObject array, int size, Object defaultValue, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = {"size >= 0", "isRubyProc(block)"})
        public Object initialize(VirtualFrame frame, DynamicObject array, int size, NotProvided defaultValue, DynamicObject block) {
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

                setStore(array, arrayBuilder.finish(store, n), n);
            }

            return array;
        }

        @Specialization(guards = {"size < 0", "isRubyProc(block)"})
        public Object initializeNegative(VirtualFrame frame, DynamicObject array, int size, NotProvided defaultValue, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "isRubyArray(copy)")
        public DynamicObject initialize(DynamicObject array, DynamicObject copy, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            setStore(array, slowToArray(copy), Layouts.ARRAY.getSize(copy));
            return array;
        }

        @Specialization(guards = {"isRubyArray(copy)", "isRubyProc(block)"})
        public DynamicObject initialize(DynamicObject array, DynamicObject copy, NotProvided defaultValue, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();
            setStore(array, slowToArray(copy), Layouts.ARRAY.getSize(copy));
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
            setStore(self, null, 0);
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isIntArray(from)"})
        public DynamicObject initializeCopyIntegerFixnum(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((int[]) Layouts.ARRAY.getStore(from), Layouts.ARRAY.getSize(from)), Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isLongArray(from)"})
        public DynamicObject initializeCopyLongFixnum(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((long[]) Layouts.ARRAY.getStore(from), Layouts.ARRAY.getSize(from)), Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isDoubleArray(from)"})
        public DynamicObject initializeCopyFloat(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((double[]) Layouts.ARRAY.getStore(from), Layouts.ARRAY.getSize(from)), Layouts.ARRAY.getSize(from));
            return self;
        }

        @Specialization(guards = {"isRubyArray(from)", "isObjectArray(from)"})
        public DynamicObject initializeCopyObject(DynamicObject self, DynamicObject from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(from), Layouts.ARRAY.getSize(from)), Layouts.ARRAY.getSize(from));
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

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectEmptyArray(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return initial;
        }

        @Specialization(guards = { "isEmptyArray(array)", "isRubyProc(block)" })
        public Object injectEmptyArrayNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = { "isIntArray(array)", "!isEmptyArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectIntegerFixnum(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayMirror.reflect((int[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isIntArray(array)", "!isEmptyArray(array)", "isRubyProc(block)" })
        public Object injectIntegerFixnumNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayMirror.reflect((int[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isLongArray(array)", "!isEmptyArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectLongFixnum(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayMirror.reflect((long[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isLongArray(array)", "!isEmptyArray(array)", "isRubyProc(block)" })
        public Object injectLongFixnumNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayMirror.reflect((long[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isEmptyArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectFloat(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayMirror.reflect((double[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isEmptyArray(array)", "isRubyProc(block)" })
        public Object injectFloatNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayMirror.reflect((double[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isObjectArray(array)", "!isEmptyArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectObject(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return injectHelper(frame, ArrayMirror.reflect((Object[]) Layouts.ARRAY.getStore(array)), array, initial, block, 0);
        }

        @Specialization(guards = { "isObjectArray(array)", "!isEmptyArray(array)", "isRubyProc(block)" })
        public Object injectObjectNoInitial(VirtualFrame frame, DynamicObject array, NotProvided initial, NotProvided unused, DynamicObject block) {
            final ArrayMirror mirror = ArrayMirror.reflect((Object[]) Layouts.ARRAY.getStore(array));

            return injectHelper(frame, mirror, array, mirror.get(0), block, 1);
        }

        @Specialization(guards = { "isNullArray(array)", "wasProvided(initial)", "isRubyProc(block)" })
        public Object injectNull(VirtualFrame frame, DynamicObject array, Object initial, NotProvided unused, DynamicObject block) {
            return initial;
        }

        @Specialization(guards = { "isNullArray(array)", "isRubyProc(block)" })
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
            return injectSymbolHelper(frame, ArrayMirror.reflect((int[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isIntArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolIntArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayMirror.reflect((int[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isLongArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolLongArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayMirror.reflect((long[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isLongArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolLongArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayMirror.reflect((long[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isDoubleArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolDoubleArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayMirror.reflect((double[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isDoubleArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolDoubleArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayMirror.reflect((double[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isObjectArray(array)", "!isEmptyArray(array)", "wasProvided(initial)" })
        public Object injectSymbolObjectArray(VirtualFrame frame, DynamicObject array, Object initial, DynamicObject symbol, NotProvided block) {
            return injectSymbolHelper(frame, ArrayMirror.reflect((Object[]) Layouts.ARRAY.getStore(array)), array, initial, symbol, 0);
        }

        @Specialization(guards = { "isRubySymbol(symbol)", "isObjectArray(array)", "!isEmptyArray(array)" })
        public Object injectSymbolObjectArray(VirtualFrame frame, DynamicObject array, DynamicObject symbol, NotProvided unused, NotProvided block) {
            final ArrayMirror mirror = ArrayMirror.reflect((Object[]) Layouts.ARRAY.getStore(array));

            return injectSymbolHelper(frame, mirror, array, mirror.get(0), symbol, 1);
        }

        private Object injectHelper(VirtualFrame frame, ArrayMirror mirror, DynamicObject array, Object initial, DynamicObject block, int startIndex) {
            assert RubyGuards.isRubyProc(block);

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
            setStore(array, store, index + 1);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "values.length == 0", "idx >= 0", "isIndexSmallerThanSize(idx,array)", "hasRoomForOneExtra(array)" })
        public Object insert(VirtualFrame frame, DynamicObject array, int idx, int value, Object[] values) {
            final int index = idx;
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            System.arraycopy(store, index, store, index + 1, Layouts.ARRAY.getSize(array) - index);
            store[index] = value;
            setStore(array, store, Layouts.ARRAY.getSize(array) + 1);
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

            setStore(array, store, newSize);

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

        @Child private ArrayBuilderNode arrayBuilder;

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public DynamicObject mapNull(DynamicObject array, DynamicObject block) {
            return createEmptyArray();
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
        public Object mapIntegerFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createArray(arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = {"isLongArray(array)", "isRubyProc(block)"})
        public Object mapLongFixnum(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createArray(arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = {"isDoubleArray(array)", "isRubyProc(block)"})
        public Object mapFloat(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createArray(arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
        public Object mapObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createArray(arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child private ArrayWriteDenormalizedNode writeNode;

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public DynamicObject mapInPlaceNull(DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
        public Object mapInPlaceFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final int[] store = (int[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    writeNode.executeWrite(frame, array, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }


            return array;
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
        public Object mapInPlaceObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            int count = 0;

            try {
                for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    writeNode.executeWrite(frame, array, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }


            return array;
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

            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final DynamicObject block = ProcNodes.createRubyProc(getContext().getCoreLibrary().getProcClass(), ProcNodes.Type.PROC,
                    maxBlock.getSharedMethodInfo(), maxBlock.getCallTarget(), maxBlock.getCallTarget(),
                    maxBlock.getCallTarget(), maximumClosureFrame.materialize(), null, array, null);

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
            final SourceSection sourceSection = new CoreSourceSection("Array", "max");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("maximum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "max", false, null, false);

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

            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final DynamicObject block = ProcNodes.createRubyProc(getContext().getCoreLibrary().getProcClass(), ProcNodes.Type.PROC,
                    minBlock.getSharedMethodInfo(), minBlock.getCallTarget(), minBlock.getCallTarget(),
                    minBlock.getCallTarget(), minimumClosureFrame.materialize(), null, array, null);

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
            final SourceSection sourceSection = new CoreSourceSection("Array", "min");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("minimum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "min", false, null, false);

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

        @Specialization(guards = {"isRubyString(format)", "byteListsEqual(format, cachedFormat)"})
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

            return finishPack(Layouts.STRING.getByteList(format), result);
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
                StringNodes.forceEncoding(string, USASCIIEncoding.INSTANCE);
            } else {
                switch (result.getEncoding()) {
                    case DEFAULT:
                    case ASCII_8BIT:
                        break;
                    case US_ASCII:
                        StringNodes.forceEncoding(string, USASCIIEncoding.INSTANCE);
                        break;
                    case UTF_8:
                        StringNodes.forceEncoding(string, UTF8Encoding.INSTANCE);
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
            return createEmptyArray();
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, Layouts.ARRAY.getSize(array) - numPop, Layouts.ARRAY.getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numPop, numPop);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numPop);
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
            setStore(array, new int[] { value }, 1);
            return array;
        }

        @Specialization(guards = { "isNullArray(array)", "values.length == 0" })
        public DynamicObject pushNullEmptySingleIntegerLong(DynamicObject array, long value, Object[] values) {
            setStore(array, new long[] { value }, 1);
            return array;
        }

        @Specialization(guards = "isNullArray(array)")
        public DynamicObject pushNullEmptyObjects(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            setStore(array, values, values.length);
            return array;
        }

        @Specialization(guards = { "!isNullArray(array)", "isEmptyArray(array)" })
        public DynamicObject pushEmptySingleIntegerFixnum(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 20-Apr-15 in reality might be better reusing any current storage, but won't worry about that for now
            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            setStore(array, values, values.length);
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
            setStore(array, store, newSize);
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
            setStore(array, store, newSize);
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

            setStore(array, store, newSize);
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
            setStore(array, store, newSize);
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
            setStore(array, store, newSize);
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public DynamicObject pushFloat(VirtualFrame frame, DynamicObject array, Object unusedValue, Object[] unusedRest) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store            
            if (Layouts.ARRAY.getSize(array) != 0) {
                throw new UnsupportedOperationException();
            }

            final Object[] values = RubyArguments.extractUserArguments(frame.getArguments());
            setStore(array, values, values.length);
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
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }
            ;
            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            setStore(array, store, newSize);
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
            setStore(array, new Object[]{value}, 1);
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public DynamicObject pushIntegerFixnumIntegerFixnum(DynamicObject array, int value) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                setStore(array, store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), Layouts.ARRAY.getSize(array));
            }

            store[oldSize] = value;
            setStore(array, store, newSize);
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
            setStore(array, newStore, newSize);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public DynamicObject pushObjectObject(DynamicObject array, Object value) {
            final int oldSize = Layouts.ARRAY.getSize(array);
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                setStore(array, store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), Layouts.ARRAY.getSize(array));
            }

            store[oldSize] = value;
            setStore(array, store, newSize);
            return array;
        }

    }

    @CoreMethod(names = "reject", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public RejectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return createEmptyArray();
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "delete_if" , needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class DeleteIfNode extends YieldingCoreMethodNode {

        public DeleteIfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return array;
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = {"isLongArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = {"isDoubleArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
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

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public Object rejectInPlaceNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return nil();
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = {"isLongArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = {"isDoubleArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
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
                setStore(array, store, i);
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

            setStore(array, null, 0);
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isIntArray(other)"})
        public DynamicObject replaceIntegerFixnum(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((int[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)), Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isLongArray(other)"})
        public DynamicObject replaceLongFixnum(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((long[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)), Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isDoubleArray(other)"})
        public DynamicObject replaceFloat(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((double[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)), Layouts.ARRAY.getSize(other));
            return array;
        }

        @Specialization(guards = {"isRubyArray(other)", "isObjectArray(other)"})
        public DynamicObject replaceObject(DynamicObject array, DynamicObject other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((Object[]) Layouts.ARRAY.getStore(other), Layouts.ARRAY.getSize(other)), Layouts.ARRAY.getSize(other));
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public SelectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = {"isNullArray(array)", "isRubyProc(block)"})
        public Object selectNull(VirtualFrame frame, DynamicObject array, DynamicObject block) {
            return createEmptyArray();
        }

        @Specialization(guards = {"isObjectArray(array)", "isRubyProc(block)"})
        public Object selectObject(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = {"isIntArray(array)", "isRubyProc(block)"})
        public Object selectFixnumInteger(VirtualFrame frame, DynamicObject array, DynamicObject block) {
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

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
                setStore(array, store, Layouts.ARRAY.getSize(array) - 1);
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
            return createEmptyArray();
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
                final DynamicObject result = createArray(Arrays.copyOfRange(store, 0, Layouts.ARRAY.getSize(array) - numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, Layouts.ARRAY.getSize(array) - numShift);
                System.arraycopy(filler, 0, store, Layouts.ARRAY.getSize(array) - numShift, numShift);
                setStore(array, store, Layouts.ARRAY.getSize(array) - numShift);
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
            return createEmptyArray();
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortIntegerFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final int[] store = (int[]) Layouts.ARRAY.getStore(array);
            final int[] newStore = new int[store.length];

            final int size = Layouts.ARRAY.getSize(array);

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < ARRAYS_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < ARRAYS_SMALL; j++) {
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

            return createArray(newStore, size);
        }

        @ExplodeLoop
        @Specialization(guards = {"isLongArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortLongFixnum(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final long[] store = (long[]) Layouts.ARRAY.getStore(array);
            final long[] newStore = new long[store.length];

            final int size = Layouts.ARRAY.getSize(array);

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < ARRAYS_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < ARRAYS_SMALL; j++) {
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

            return createArray(newStore, size);
        }

        @Specialization(guards = {"isObjectArray(array)", "isSmall(array)"})
        public DynamicObject sortVeryShortObject(VirtualFrame frame, DynamicObject array, NotProvided block) {
            final Object[] oldStore = (Object[]) Layouts.ARRAY.getStore(array);
            final Object[] store = Arrays.copyOf(oldStore, oldStore.length);

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

            return createArray(store, size);
        }

        @Specialization(guards = { "!isNullArray(array)", "isRubyProc(block)" })
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

        protected static boolean isSmall(DynamicObject array) {
            return Layouts.ARRAY.getSize(array) <= ARRAYS_SMALL;
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

            slowUnshift(array, args);
            return array;
        }

    }

    @CoreMethod(names = "zip", rest = true, required = 1)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        public ZipNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isIntArray(other)", "others.length == 0" })
        public DynamicObject zipObjectIntegerFixnum(DynamicObject array, DynamicObject other, Object[] others) {
            final Object[] a = (Object[]) Layouts.ARRAY.getStore(array);

            final int[] b = (int[]) Layouts.ARRAY.getStore(other);
            final int bLength = Layouts.ARRAY.getSize(other);

            final int zippedLength = Layouts.ARRAY.getSize(array);
            final Object[] zipped = new Object[zippedLength];

            final boolean areSameLength = bLength == zippedLength;

            if (areSameLength) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = createArray(new Object[]{a[n], b[n]}, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = createArray(new Object[]{a[n], b[n]}, 2);
                    } else {
                        zipped[n] = createArray(new Object[]{a[n], nil()}, 2);
                    }
                }
            }

            return createArray(zipped, zippedLength);
        }

        @Specialization(guards = { "isObjectArray(array)", "isRubyArray(other)", "isObjectArray(other)", "others.length == 0" })
        public DynamicObject zipObjectObject(DynamicObject array, DynamicObject other, Object[] others) {
            final Object[] a = (Object[]) Layouts.ARRAY.getStore(array);

            final Object[] b = (Object[]) Layouts.ARRAY.getStore(other);
            final int bLength = Layouts.ARRAY.getSize(other);

            final int zippedLength = Layouts.ARRAY.getSize(array);
            final Object[] zipped = new Object[zippedLength];

            final boolean areSameLength = bLength == zippedLength;

            if (areSameLength) {
                for (int n = 0; n < zippedLength; n++) {
                    zipped[n] = createArray(new Object[]{a[n], b[n]}, 2);
                }
            } else {
                for (int n = 0; n < zippedLength; n++) {
                    if (n < bLength) {
                        zipped[n] = createArray(new Object[]{a[n], b[n]}, 2);
                    } else {
                        zipped[n] = createArray(new Object[]{a[n], nil()}, 2);
                    }
                }
            }


            return createArray(zipped, zippedLength);
        }

        @Specialization(guards = { "isRubyArray(other)", "fallback(array, other, others)" })
        public Object zipObjectObjectNotSingleObject(VirtualFrame frame, DynamicObject array, DynamicObject other, Object[] others) {
            return zipRuby(frame);
        }

        @Specialization(guards = { "!isRubyArray(other)" })
        public Object zipObjectObjectNotArray(VirtualFrame frame, DynamicObject array, DynamicObject other, Object[] others) {
            return zipRuby(frame);
        }

        private Object zipRuby(VirtualFrame frame) {
            DynamicObject proc = RubyArguments.getBlock(frame.getArguments());
            if (proc == null) {
                proc = nil();
            }
            final Object[] others = RubyArguments.extractUserArguments(frame.getArguments());
            return ruby(frame, "zip_internal(*others, &block)", "others", createArray(others, others.length), "block", proc);
        }

        protected static boolean fallback(DynamicObject array, DynamicObject other, Object[] others) {
            return !ArrayGuards.isObjectArray(array) || ArrayGuards.isNullArray(other) || ArrayGuards.isLongArray(other) || others.length > 0;
        }

    }

}
