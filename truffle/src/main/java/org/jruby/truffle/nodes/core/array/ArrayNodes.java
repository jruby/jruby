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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.coerce.ToAryNodeGen;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.fixnum.FixnumLowerNode;
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
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.Random;

@CoreClass(name = "Array")
public abstract class ArrayNodes {

    public static class ArrayType extends BasicObjectType {

    }

    //public static final ArrayType ARRAY_TYPE = new ArrayType();

    //private static final HiddenKey STORE_IDENTIFIER = new HiddenKey("store");
    //private static final Property STORE_PROPERTY;

    //private static final HiddenKey SIZE_IDENTIFIER = new HiddenKey("size");
    //private static final Property SIZE_PROPERTY;

    //private static final DynamicObjectFactory ARRAY_FACTORY;

    static {
        //final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();

        //STORE_PROPERTY = Property.create(STORE_IDENTIFIER, allocator.locationForType(Object.class, EnumSet.of(LocationModifier.NonNull)), 0);
        //SIZE_PROPERTY = Property.create(SIZE_IDENTIFIER, allocator.locationForType(int.class, EnumSet.of(LocationModifier.NonNull)), 0);

        //final Shape shape = RubyBasicObject.LAYOUT.createShape(ARRAY_TYPE);
                //.addProperty(STORE_PROPERTY)
                //.addProperty(SIZE_PROPERTY);

        //ARRAY_FACTORY = shape.createFactory();
    }

    public static Object getStore(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        //assert array.getDynamicObject().getShape().hasProperty(STORE_IDENTIFIER);

        //return STORE_PROPERTY.get(array.getDynamicObject(), true);
        return ((RubyArray) array).store;
    }

    public static void setStore(RubyBasicObject array, Object store, int size) {
        assert RubyGuards.isRubyArray(array);
        //assert array.getDynamicObject().getShape().hasProperty(STORE_IDENTIFIER);
        //assert array.getDynamicObject().getShape().hasProperty(SIZE_IDENTIFIER);

        assert verifyStore(store, size);

        if (RANDOMIZE_STORAGE_ARRAY) {
            store = randomizeStorageStrategy(array.getContext(), store, size);
            assert verifyStore(store, size);
        }

        /*try {
            STORE_PROPERTY.set(array.getDynamicObject(), store, array.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }

        try {
            SIZE_PROPERTY.set(array.getDynamicObject(), size, array.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }*/

        ((RubyArray) array).store = store;
        ((RubyArray) array).size = size;
    }

    public static void setSize(RubyBasicObject array, int size) {
        assert RubyGuards.isRubyArray(array);
        //assert array.getDynamicObject().getShape().hasProperty(SIZE_IDENTIFIER);

        /*try {
            SIZE_PROPERTY.set(array.getDynamicObject(), size, array.getDynamicObject().getShape());
        } catch (IncompatibleLocationException | FinalLocationException e) {
            throw new UnsupportedOperationException(e);
        }*/

        ((RubyArray) array).size = size;
    }

    public static int getSize(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        //assert array.getDynamicObject().getShape().hasProperty(SIZE_IDENTIFIER);

        //return (int) SIZE_PROPERTY.get(array.getDynamicObject(), true);
        return ((RubyArray) array).size;
    }

    public static final int ARRAYS_SMALL = Options.TRUFFLE_ARRAYS_SMALL.load();
    public static final boolean RANDOMIZE_STORAGE_ARRAY = Options.TRUFFLE_RANDOMIZE_STORAGE_ARRAY.load();
    private static final Random random = new Random(Options.TRUFFLE_RANDOMIZE_SEED.load());

    public static RubyBasicObject fromObject(RubyClass arrayClass, Object object) {
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

    public static RubyBasicObject fromObjects(RubyClass arrayClass, Object... objects) {
        return createGeneralArray(arrayClass, storeFromObjects(arrayClass.getContext(), objects), objects.length);
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

    public static Object[] slowToArray(RubyBasicObject array) {
        assert RubyGuards.isRubyArray(array);
        return ArrayUtils.boxUntil(getStore(array), getSize(array));
    }

    public static void slowUnshift(RubyBasicObject array, Object... values) {
        assert RubyGuards.isRubyArray(array);
        final Object[] newStore = new Object[getSize(array) + values.length];
        System.arraycopy(values, 0, newStore, 0, values.length);
        ArrayUtils.copy(getStore(array), newStore, values.length, getSize(array));
        setStore(array, newStore, newStore.length);
    }

    public static void slowPush(RubyBasicObject array, Object value) {
        assert RubyGuards.isRubyArray(array);
        setStore(array, Arrays.copyOf(ArrayUtils.box(getStore(array)), getSize(array) + 1), getSize(array));
        ((Object[]) getStore(array))[getSize(array)] = value;
        setSize(array, getSize(array) + 1);
    }

    public static int normalizeIndex(RubyBasicObject array, int index) {
        assert RubyGuards.isRubyArray(array);
        return normalizeIndex(getSize(array), index);
    }

    public static int clampExclusiveIndex(RubyBasicObject array, int index) {
        assert RubyGuards.isRubyArray(array);
        return clampExclusiveIndex(getSize(array), index);
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

    public static RubyBasicObject createEmptyArray(RubyClass arrayClass) {
        return createGeneralArray(arrayClass, null, 0);
    }

    public static RubyBasicObject createArray(RubyClass arrayClass, int[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static RubyBasicObject createArray(RubyClass arrayClass, long[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static RubyBasicObject createArray(RubyClass arrayClass, double[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static RubyBasicObject createArray(RubyClass arrayClass, Object[] store, int size) {
        return createGeneralArray(arrayClass, store, size);
    }

    public static RubyArray createGeneralArray(RubyClass arrayClass, Object store, int size) {
        //return new RubyArray(arrayClass, store, size, ARRAY_FACTORY.newInstance(store, size));
        return new RubyArray(arrayClass, store, size, RubyBasicObject.LAYOUT.newInstance(RubyBasicObject.EMPTY_SHAPE));
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
        public RubyBasicObject addNull(RubyArray a, RubyArray b) {
            return createEmptyArray();
        }

        @Specialization(guards = {"isObjectArray(a)", "isNullArray(b)"})
        public RubyBasicObject addObjectNull(RubyArray a, RubyArray b) {
            return createArray(Arrays.copyOf((Object[]) getStore(a), getSize(a)), getSize(a));
        }

        @Specialization(guards = {"isIntArray(a)", "isIntArray(b)"})
        public RubyBasicObject addBothIntegerFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final int[] combined = new int[combinedSize];
            System.arraycopy(getStore(a), 0, combined, 0, getSize(a));
            System.arraycopy(getStore(b), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isLongArray(a)", "isLongArray(b)"})
        public RubyBasicObject addBothLongFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final long[] combined = new long[combinedSize];
            System.arraycopy(getStore(a), 0, combined, 0, getSize(a));
            System.arraycopy(getStore(b), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isDoubleArray(a)", "isDoubleArray(b)"})
        public RubyBasicObject addBothFloat(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final double[] combined = new double[combinedSize];
            System.arraycopy(getStore(a), 0, combined, 0, getSize(a));
            System.arraycopy(getStore(b), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "isObjectArray(b)"})
        public RubyBasicObject addBothObject(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(getStore(a), 0, combined, 0, getSize(a));
            System.arraycopy(getStore(b), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isNullArray(a)", "isIntArray(b)"})
        public RubyBasicObject addNullIntegerFixnum(RubyArray a, RubyArray b) {
            final int size = getSize(b);
            return createArray(Arrays.copyOf((int[]) getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isLongArray(b)"})
        public RubyBasicObject addNullLongFixnum(RubyArray a, RubyArray b) {
            final int size = getSize(b);
            return createArray(Arrays.copyOf((long[]) getStore(b), size), size);
        }

        @Specialization(guards = {"isNullArray(a)", "isObjectArray(b)"})
        public RubyBasicObject addNullObject(RubyArray a, RubyArray b) {
            final int size = getSize(b);
            return createArray(Arrays.copyOf((Object[]) getStore(b), size), size);
        }

        @Specialization(guards = {"!isObjectArray(a)", "isObjectArray(b)"})
        public RubyBasicObject addOtherObject(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(ArrayUtils.box(getStore(a)), 0, combined, 0, getSize(a));
            System.arraycopy(getStore(b), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = {"isObjectArray(a)", "!isObjectArray(b)"})
        public RubyBasicObject addObject(RubyArray a, RubyArray b) {
            final int combinedSize = getSize(a) + getSize(b);
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(getStore(a), 0, combined, 0, getSize(a));
            System.arraycopy(ArrayUtils.box(getStore(b)), 0, combined, getSize(a), getSize(b));
            return createArray(combined, combinedSize);
        }

        @Specialization(guards = "isEmptyArray(a)")
        public RubyBasicObject addEmpty(RubyArray a, RubyArray b) {
            final int size = getSize(b);
            return createArray(ArrayUtils.box(getStore(b)), size);
        }

        @Specialization(guards = "isEmptyArray(b)")
        public RubyBasicObject addOtherEmpty(RubyArray a, RubyArray b) {
            final int size = getSize(a);
            return createArray(ArrayUtils.box(getStore(a)), size);
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
        public RubyBasicObject mulEmpty(RubyArray array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            return ArrayNodes.createEmptyArray(array.getLogicalClass());
        }

        @Specialization(guards = "isIntArray(array)")
        public RubyBasicObject mulIntegerFixnum(RubyArray array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final int[] store = (int[]) getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final int[] newStore = new int[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return ArrayNodes.createArray(array.getLogicalClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isLongArray(array)")
        public RubyBasicObject mulLongFixnum(RubyArray array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final long[] store = (long[]) getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final long[] newStore = new long[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return ArrayNodes.createArray(array.getLogicalClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isDoubleArray(array)")
        public RubyBasicObject mulFloat(RubyArray array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final double[] store = (double[]) getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final double[] newStore = new double[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return ArrayNodes.createArray(array.getLogicalClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isObjectArray(array)")
        public RubyBasicObject mulObject(RubyArray array, int count) {
            if (count < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative argument", this));
            }
            final Object[] store = (Object[]) getStore(array);
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final Object[] newStore = new Object[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return ArrayNodes.createArray(array.getLogicalClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isRubyString(string)")
        public Object mulObject(VirtualFrame frame, RubyArray array, RubyString string) {
            CompilerDirectives.transferToInterpreter();
            return ruby(frame, "join(sep)", "sep", string);
        }

        @Specialization(guards = {"!isRubyString(object)"})
        public Object mulObjectCount(VirtualFrame frame, RubyArray array, Object object) {
            CompilerDirectives.transferToInterpreter();
            if (respondToToStrNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToStrNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null, null}));
            }
            if (respondToToStrNode.doesRespondTo(frame, object, (RubyString) createString("to_str"), false)) {
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
                if (getStore(array) instanceof int[]) {
                    return mulIntegerFixnum(array, count);
                } else if (getStore(array) instanceof long[]) {
                    return mulLongFixnum(array, count);
                } else if (getStore(array) instanceof double[]) {
                    return mulFloat(array, count);
                } else if (getStore(array) == null) {
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
        public Object index(VirtualFrame frame, RubyArray array, int index, NotProvided length) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, array, index);
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyArray array, int start, int length) {
            if (length < 0) {
                return nil();
            }

            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            return readSliceNode.executeReadSlice(frame, array, start, length);
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, NotProvided len) {
            final int normalizedIndex = normalizeIndex(array, range.getBegin());

            if (normalizedIndex < 0 || normalizedIndex > getSize(array)) {
                return nil();
            } else {
                final int end = normalizeIndex(array, range.getEnd());
                final int exclusiveEnd = clampExclusiveIndex(array, range.doesExcludeEnd() ? end : end + 1);

                if (exclusiveEnd <= normalizedIndex) {
                    return ArrayNodes.createEmptyArray(array.getLogicalClass());
                }

                final int length = exclusiveEnd - normalizedIndex;

                if (readNormalizedSliceNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    readNormalizedSliceNode = insert(ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }

                return readNormalizedSliceNode.executeReadSlice(frame, array, normalizedIndex, length);
            }
        }

        @Specialization(guards = {"!isInteger(a)", "!isIntegerFixnumRange(a)"})
        public Object fallbackIndex(VirtualFrame frame, RubyArray array, Object a, NotProvided length) {
            return fallback(frame, array, fromObjects(getContext().getCoreLibrary().getArrayClass(), a));
        }

        @Specialization(guards = { "!isIntegerFixnumRange(a)", "wasProvided(b)" })
        public Object fallbackSlice(VirtualFrame frame, RubyArray array, Object a, Object b) {
            return fallback(frame, array, fromObjects(getContext().getCoreLibrary().getArrayClass(), a, b));
        }

        public Object fallback(VirtualFrame frame, RubyBasicObject array, RubyBasicObject args) {
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
        public Object set(VirtualFrame frame, RubyArray array, Object indexObject, Object value, NotProvided unused) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int index = toIntNode.doInt(frame, indexObject);
            return set(frame, array, index, value, unused);
        }

        @Specialization
        public Object set(VirtualFrame frame, RubyArray array, int index, Object value, NotProvided unused) {
            final int normalizedIndex = normalizeIndex(array, index);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + index + " too small for array; minimum: " + Integer.toString(-getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }
            return writeNode.executeWrite(frame, array, index, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(lengthObject)" })
        public Object setObject(VirtualFrame frame, RubyArray array, int start, Object lengthObject, Object value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int length = toIntNode.doInt(frame, lengthObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(startObject)" })
        public Object setObject(VirtualFrame frame, RubyArray array, Object startObject, int length, Object value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int start = toIntNode.doInt(frame, startObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)", "!isInteger(startObject)", "!isInteger(lengthObject)" })
        public Object setObject(VirtualFrame frame, RubyArray array, Object startObject, Object lengthObject, Object value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int length = toIntNode.doInt(frame, lengthObject);
            int start = toIntNode.doInt(frame, startObject);
            return setObject(frame, array, start, length, value);
        }

        @Specialization(guards = { "!isRubyArray(value)", "wasProvided(value)" })
        public Object setObject(VirtualFrame frame, RubyArray array, int start, int length, Object value) {
            CompilerDirectives.transferToInterpreter();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int normalizedIndex = normalizeIndex(array, start);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "index " + start + " too small for array; minimum: " + Integer.toString(-getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int begin = normalizeIndex(array, start);

            if (begin < getSize(array) && length == 1) {
                if (writeNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }

                return writeNode.executeWrite(frame, array, begin, value);
            } else {
                if (getSize(array) > (begin + length)) { // there is a tail, else other values discarded
                    if (readSliceNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                    }
                    RubyArray endValues = (RubyArray) readSliceNode.executeReadSlice(frame, array, (begin + length), (getSize(array) - begin - length));
                    if (writeNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                    }
                    writeNode.executeWrite(frame, array, begin, value);
                    Object[] endValuesStore = ArrayUtils.box(getStore(endValues));

                    int i = begin + 1;
                    for (Object obj : endValuesStore) {
                        writeNode.executeWrite(frame, array, i, obj);
                        i += 1;
                    }
                } else {
                    writeNode.executeWrite(frame, array, begin, value);
                }
                if (popOneNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    popOneNode = insert(PopOneNodeGen.create(getContext(), getSourceSection(), null));
                }
                int popLength = length - 1 < getSize(array) ? length - 1 : getSize(array) - 1;
                for (int i = 0; i < popLength; i++) { // TODO 3-15-2015 BF update when pop can pop multiple
                    popOneNode.executePopOne(array);
                }
                return value;
            }
        }

        @Specialization(guards = {"!isInteger(startObject)"})
        public Object setOtherArray(VirtualFrame frame, RubyArray array, Object startObject, int length, RubyArray value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int start = toIntNode.doInt(frame, startObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization(guards = {"!isInteger(lengthObject)"})
        public Object setOtherArray(VirtualFrame frame, RubyArray array, int start, Object lengthObject, RubyArray value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int length = toIntNode.doInt(frame, lengthObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization(guards = {"!isInteger(startObject)", "!isInteger(lengthObject)"})
        public Object setOtherArray(VirtualFrame frame, RubyArray array, Object startObject, Object lengthObject, RubyArray value) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            int start = toIntNode.doInt(frame, startObject);
            int length = toIntNode.doInt(frame, lengthObject);
            return setOtherArray(frame, array, start, length, value);
        }

        @Specialization
        public Object setOtherArray(VirtualFrame frame, RubyArray array, int start, int length, RubyArray value) {
            CompilerDirectives.transferToInterpreter();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                final String errMessage = "negative length (" + length + ")";
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int normalizedIndex = normalizeIndex(array, start);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + start + " too small for array; minimum: " + Integer.toString(-getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            final int begin = normalizeIndex(array, start);
            if (getSize(value) == 0) {

                final int exclusiveEnd = begin + length;
                Object[] store = ArrayUtils.box(getStore(array));

                if (begin < 0) {
                    tooSmallBranch.enter();
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", start, getSize(array), this));
                } else if (exclusiveEnd > getSize(array)) {
                    throw new UnsupportedOperationException();
                }

                // TODO: This is a moving overlapping memory, should we use sth else instead?
                System.arraycopy(store, exclusiveEnd, store, begin, getSize(array) - exclusiveEnd);
                setStore(array, store, getSize(array) - length);

                return value;
            } else {
                if (writeNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                }
                Object[] values = slowToArray(value);
                if (getSize(value) == length || (begin + length + 1) > getSize(array)) {
                    int i = begin;
                    for (Object obj : values) {
                        writeNode.executeWrite(frame, array, i, obj);
                        i += 1;
                    }
                } else {
                    if (readSliceNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        readSliceNode = insert(ArrayReadSliceDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
                    }

                    final int newLength = (length + begin) > getSize(array) ? begin + values.length : getSize(array) + values.length - length;
                    final int popNum = newLength < getSize(array) ? getSize(array) - newLength : 0;

                    if (popNum > 0) {
                        if (popOneNode == null) {
                            CompilerDirectives.transferToInterpreter();
                            popOneNode = insert(PopOneNodeGen.create(getContext(), getSourceSection(), null));
                        }
                        for (int i = 0; i < popNum; i++) { // TODO 3-28-2015 BF update to pop multiple
                            popOneNode.executePopOne(array);
                        }
                    }

                    final int readLen = newLength - values.length - begin;
                    RubyArray endValues = null;
                    if (readLen > 0) {
                        endValues = (RubyArray) readSliceNode.executeReadSlice(frame, array, getSize(array) - readLen, readLen);
                    }

                    int i = begin;
                    for (Object obj : values) {
                        writeNode.executeWrite(frame, array, i, obj);
                        i += 1;
                    }
                    if (readLen > 0) {
                        final Object[] endValuesStore = ArrayUtils.box(getStore(endValues));
                        for (Object obj : endValuesStore) {
                            writeNode.executeWrite(frame, array, i, obj);
                            i += 1;
                        }
                    }

                }
                return value;
            }
        }

        @Specialization(guards = "!isRubyArray(other)")
        public Object setRange(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, Object other, NotProvided unused) {
            final int normalizedStart = normalizeIndex(array, range.getBegin());
            int normalizedEnd = range.doesExcludeEnd() ? normalizeIndex(array, range.getEnd()) - 1 : normalizeIndex(array, range.getEnd());
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

        @Specialization(guards = {"!isIntArray(array) || !isIntArray(other)"})
        public Object setRangeArray(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, RubyArray other, NotProvided unused) {
            final int normalizedStart = normalizeIndex(array, range.getBegin());
            if (normalizedStart < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError(range, this));
            }

            int normalizedEnd = range.doesExcludeEnd() ? normalizeIndex(array, range.getEnd()) - 1 : normalizeIndex(array, range.getEnd());
            if (normalizedEnd < 0) {
                normalizedEnd = -1;
            }
            final int length = normalizedEnd - normalizedStart + 1;

            return setOtherArray(frame, array, normalizedStart, length, other);
        }

        @Specialization(guards = {"isIntArray(array)", "isIntArray(other)"})
        public Object setIntegerFixnumRange(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, RubyArray other, NotProvided unused) {
            if (range.doesExcludeEnd()) {
                CompilerDirectives.transferToInterpreter();
                return setRangeArray(frame, array, range, other, unused);
            } else {
                int normalizedBegin = normalizeIndex(array, range.getBegin());
                int normalizedEnd = normalizeIndex(array, range.getEnd());
                if (normalizedEnd < 0) {
                    normalizedEnd = -1;
                }
                if (normalizedBegin == 0 && normalizedEnd == getSize(array) - 1) {
                    setStore(array, Arrays.copyOf((int[]) getStore(other), getSize(other)), getSize(other));
                } else {
                    CompilerDirectives.transferToInterpreter();
                    return setRangeArray(frame, array, range, other, unused);
                }
            }

            return other;
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
            return new FixnumLowerNode(ToIntNodeGen.create(getContext(), getSourceSection(), index));
        }

        @Specialization
        public Object at(VirtualFrame frame, RubyArray array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, array, index);
        }

    }

    @CoreMethod(names = "clear", raiseIfFrozenSelf = true)
    public abstract static class ClearNode extends ArrayCoreMethodNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject clear(RubyArray array) {
            setStore(array, getStore(array), 0);
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
        public RubyBasicObject compactInt(RubyArray array) {
            return createArray(Arrays.copyOf((int[]) getStore(array), getSize(array)), getSize(array));
        }

        @Specialization(guards = "isLongArray(array)")
        public RubyBasicObject compactLong(RubyArray array) {
            return createArray(Arrays.copyOf((long[]) getStore(array), getSize(array)), getSize(array));
        }

        @Specialization(guards = "isDoubleArray(array)")
        public RubyBasicObject compactDouble(RubyArray array) {
            return createArray(Arrays.copyOf((double[]) getStore(array), getSize(array)), getSize(array));
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(RubyArray array) {
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

        @Specialization(guards = "isNullArray(array)")
        public Object compactNull(RubyArray array) {
            return createEmptyArray();
        }

    }

    @CoreMethod(names = "compact!", raiseIfFrozenSelf = true)
    public abstract static class CompactBangNode extends ArrayCoreMethodNode {

        public CompactBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "!isObjectArray(array)")
        public RubyBasicObject compactNotObjects(RubyArray array) {
            return nil();
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(RubyArray array) {
            final Object[] store = (Object[]) getStore(array);
            final int size = getSize(array);

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

        @Specialization(guards = "isNullArray(other)")
        public RubyBasicObject concatNull(RubyArray array, RubyArray other) {
            return array;
        }

        @Specialization(guards = "!isNullArray(other)")
        public RubyBasicObject concat(RubyArray array, RubyArray other) {
            appendManyNode.executeAppendMany(array, getSize(other), getStore(other));
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
        public Object deleteIntegerFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final int[] store = (int[]) getStore(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
                    }
                    if (isFrozenNode.executeIsFrozen(array)) {
                        CompilerDirectives.transferToInterpreter();
                        throw new RaiseException(
                            getContext().getCoreLibrary().frozenError(array.getLogicalClass().getName(), this));
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
        public Object deleteObject(VirtualFrame frame, RubyArray array, Object value) {
            final Object[] store = (Object[]) getStore(array);

            Object found = nil();

            int i = 0;
            int n = 0;
            for (; n < getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    if (isFrozenNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        isFrozenNode = insert(IsFrozenNodeGen.create(getContext(), getSourceSection(), null));
                    }
                    if (isFrozenNode.executeIsFrozen(array)) {
                        CompilerDirectives.transferToInterpreter();
                        throw new RaiseException(
                            getContext().getCoreLibrary().frozenError(array.getLogicalClass().getName(), this));
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
        public Object deleteNull(VirtualFrame frame, RubyArray array, Object value) {
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
        public int deleteAtIntegerFixnumInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int[] store = (int[]) getStore(array);
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtIntegerFixnumInBounds", guards = "isIntArray(array)")
        public Object deleteAtIntegerFixnum(RubyArray array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final int[] store = (int[]) getStore(array);
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public long deleteAtLongFixnumInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final long[] store = (long[]) getStore(array);
                final long value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtLongFixnumInBounds", guards = "isLongArray(array)")
        public Object deleteAtLongFixnum(RubyArray array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final long[] store = (long[]) getStore(array);
                final long value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public double deleteAtFloatInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final double[] store = (double[]) getStore(array);
                final double value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtFloatInBounds", guards = "isDoubleArray(array)")
        public Object deleteAtFloat(RubyArray array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final double[] store = (double[]) getStore(array);
                final double value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)", rewriteOn = UnexpectedResultException.class)
        public Object deleteAtObjectInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalizedIndex = normalizeIndex(array, index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(nil());
            } else if (normalizedIndex >= getSize(array)) {
                throw new UnexpectedResultException(nil());
            } else {
                final Object[] store = (Object[]) getStore(array);
                final Object value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtObjectInBounds", guards = "isObjectArray(array)")
        public Object deleteAtObject(RubyArray array, int index) {
            CompilerDirectives.transferToInterpreter();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = getSize(array) + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                return nil();
            } else if (normalizedIndex >= getSize(array)) {
                beyondEndBranch.enter();
                return nil();
            } else {
                final Object[] store = (Object[]) getStore(array);
                final Object value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, getSize(array) - normalizedIndex - 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isEmptyArray(array)")
        public Object deleteAtNullOrEmpty(RubyArray array, int index) {
            return nil();
        }


    }

    @CoreMethod(names = "each", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        private final RubySymbol eachSymbol;

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachSymbol = getContext().getSymbol("each");
        }

        @Specialization
        public Object eachEnumerator(VirtualFrame frame, RubyArray array, NotProvided block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, array, "to_enum", null, eachSymbol);
        }

        @Specialization(guards = "isNullArray(array)")
        public Object eachNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)")
        public Object eachIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object eachLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object eachFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            final double[] store = (double[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object eachObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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

        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public EachWithIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public RubyBasicObject eachWithEmpty(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object eachWithIndexInt(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n], n);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object eachWithIndexLong(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n], n);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object eachWithIndexDouble(VirtualFrame frame, RubyArray array, RubyProc block) {
            final double[] store = (double[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n], n);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object eachWithIndexObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n], n);
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization
        public Object eachWithIndexObject(VirtualFrame frame, RubyArray array, NotProvided block) {
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
        public boolean includeNull(VirtualFrame frame, RubyArray array, Object value) {
            return false;
        }

        @Specialization(guards = "isIntArray(array)")
        public boolean includeIntegerFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final int[] store = (int[]) getStore(array);

            for (int n = 0; n < getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isLongArray(array)")
        public boolean includeLongFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final long[] store = (long[]) getStore(array);

            for (int n = 0; n < getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public boolean includeFloat(VirtualFrame frame, RubyArray array, Object value) {
            final double[] store = (double[]) getStore(array);

            for (int n = 0; n < getSize(array); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObjectArray(array)")
        public boolean includeObject(VirtualFrame frame, RubyArray array, Object value) {
            final Object[] store = (Object[]) getStore(array);

            for (int n = 0; n < getSize(array); n++) {
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
        public RubyBasicObject initialize(VirtualFrame frame, RubyArray array, Object object, NotProvided defaultValue, NotProvided block) {

            RubyArray copy = null;
            if (respondToToAryNode == null) {
                CompilerDirectives.transferToInterpreter();
                respondToToAryNode = insert(KernelNodesFactory.RespondToNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null, null}));
            }
            if (respondToToAryNode.doesRespondTo(frame, object, (RubyString) createString("to_ary"), true)) {
                if (toAryNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toAryNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                }
                Object toAryResult = toAryNode.call(frame, object, "to_ary", null);
                if (toAryResult instanceof RubyArray) {
                    copy = (RubyArray) toAryResult;
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
        public RubyBasicObject initialize(RubyArray array, NotProvided size, NotProvided defaultValue, NotProvided block) {
            return initialize(array, 0, nil(), block);
        }

        @Specialization
        public RubyBasicObject initialize(RubyArray array, NotProvided size, NotProvided defaultValue, RubyProc block) {
            return initialize(array, 0, nil(), NotProvided.INSTANCE);
        }

        @Specialization(guards = "size >= 0")
        public RubyBasicObject initialize(RubyArray array, int size, NotProvided defaultValue, NotProvided block) {
            return initialize(array, size, nil(), block);
        }

        @Specialization(guards = "size < 0")
        public RubyBasicObject initializeNegative(RubyArray array, int size, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public RubyBasicObject initialize(RubyArray array, long size, NotProvided defaultValue, NotProvided block) {
            if (size > Integer.MAX_VALUE) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("array size too big", this));
            }
            return initialize(array, (int) size, nil(), block);
        }

        @Specialization(guards = "size < 0")
        public RubyBasicObject initializeNegative(RubyArray array, long size, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public RubyBasicObject initialize(RubyArray array, int size, int defaultValue, NotProvided block) {
            final int[] store = new int[size];
            Arrays.fill(store, defaultValue);
            setStore(array, store, size);
            return array;
        }

        @Specialization(guards = "size < 0")
        public RubyBasicObject initializeNegative(RubyArray array, int size, int defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public RubyBasicObject initialize(RubyArray array, int size, long defaultValue, NotProvided block) {
            final long[] store = new long[size];
            Arrays.fill(store, defaultValue);
            setStore(array, store, size);
            return array;
        }

        @Specialization(guards = "size < 0")
        public RubyBasicObject initializeNegative(RubyArray array, int size, long defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public RubyBasicObject initialize(RubyArray array, int size, double defaultValue, NotProvided block) {
            final double[] store = new double[size];
            Arrays.fill(store, defaultValue);
            setStore(array, store, size);
            return array;
        }
        
        @Specialization(guards = "size < 0")
        public RubyBasicObject initializeNegative(RubyArray array, int size, double defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size >= 0" })
        public RubyBasicObject initialize(RubyArray array, int size, Object defaultValue, NotProvided block) {
            final Object[] store = new Object[size];
            Arrays.fill(store, defaultValue);
            setStore(array, store, size);
            return array;
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size < 0" })
        public RubyBasicObject initializeNegative(RubyArray array, int size, Object defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = { "!isInteger(sizeObject)", "wasProvided(defaultValue)" })
        public RubyBasicObject initialize(VirtualFrame frame, RubyArray array, Object sizeObject, Object defaultValue, NotProvided block) {
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
        public Object initialize(VirtualFrame frame, RubyArray array, int size, Object defaultValue, RubyProc block) {
            return initialize(frame, array, size, NotProvided.INSTANCE, block);
        }

        @Specialization(guards = { "wasProvided(defaultValue)", "size < 0" })
        public Object initializeNegative(VirtualFrame frame, RubyArray array, int size, Object defaultValue, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization(guards = "size >= 0")
        public Object initialize(VirtualFrame frame, RubyArray array, int size, NotProvided defaultValue, RubyProc block) {
            Object store = arrayBuilder.start();

            int count = 0;
            int n = 0;
            try {
                for (; n < size; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    arrayBuilder.ensure(store, n + 1);
                    store = arrayBuilder.append(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }

                setStore(array, arrayBuilder.finish(store, n), n);
            }

            return array;
        }

        @Specialization(guards = "size < 0")
        public Object initializeNegative(VirtualFrame frame, RubyArray array, int size, NotProvided defaultValue, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
        }

        @Specialization
        public RubyBasicObject initialize(RubyArray array, RubyArray copy, NotProvided defaultValue, NotProvided block) {
            CompilerDirectives.transferToInterpreter();
            setStore(array, slowToArray(copy), getSize(copy));
            return array;
        }

        @Specialization
        public RubyBasicObject initialize(RubyArray array, RubyArray copy, NotProvided defaultValue, RubyProc block) {
            CompilerDirectives.transferToInterpreter();
            setStore(array, slowToArray(copy), getSize(copy));
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

        @Specialization(guards = "isNullArray(from)")
        public RubyBasicObject initializeCopyNull(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            setStore(self, null, 0);
            return self;
        }

        @Specialization(guards = "isIntArray(from)")
        public RubyBasicObject initializeCopyIntegerFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((int[]) getStore(from), getSize(from)), getSize(from));
            return self;
        }

        @Specialization(guards = "isLongArray(from)")
        public RubyBasicObject initializeCopyLongFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((long[]) getStore(from), getSize(from)), getSize(from));
            return self;
        }

        @Specialization(guards = "isDoubleArray(from)")
        public RubyBasicObject initializeCopyFloat(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((double[]) getStore(from), getSize(from)), getSize(from));
            return self;
        }

        @Specialization(guards = "isObjectArray(from)")
        public RubyBasicObject initializeCopyObject(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            setStore(self, Arrays.copyOf((Object[]) getStore(from), getSize(from)), getSize(from));
            return self;
        }

    }

    @CoreMethod(names = {"inject", "reduce"}, needsBlock = true, optional = 1)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode dispatch;

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.CALL_METHOD_MISSING);
        }

        @Specialization(guards = "isIntArray(array)")
        public Object injectIntegerFixnum(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final int[] store = (int[]) getStore(array);

            Object accumulator = initial;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object injectLongFixnum(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final long[] store = (long[]) getStore(array);

            Object accumulator = initial;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object injectFloat(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final double[] store = (double[]) getStore(array);

            Object accumulator = initial;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object injectObject(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final Object[] store = (Object[]) getStore(array);

            Object accumulator = initial;

            try {
                for (int n = 0; n < getSize(array); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isNullArray(array)")
        public Object injectNull(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            return initial;
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, RubySymbol symbol, NotProvided block) {
            CompilerDirectives.transferToInterpreter();

            final Object[] store = slowToArray(array);

            if (store.length < 2) {
                if (store.length == 1) {
                    return store[0];
                } else {
                    return getContext().getCoreLibrary().getNilObject();
                }
            }

            Object accumulator = dispatch.call(frame, store[0], symbol, null, store[1]);

            for (int n = 2; n < getSize(array); n++) {
                accumulator = dispatch.call(frame, accumulator, symbol, null, store[n]);
            }

            return accumulator;
        }

    }

    @CoreMethod(names = "insert", required = 1, raiseIfFrozenSelf = true, argumentsAsArray = true)
    public abstract static class InsertNode extends ArrayCoreMethodNode {

        @Child private ToIntNode toIntNode;
        private final BranchProfile tooSmallBranch = BranchProfile.create();

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isNullArray(array)", "isIntIndexAndOtherSingleObjectArg(values)"})
        public Object insertNull(RubyArray array, Object[] values) {
            CompilerDirectives.transferToInterpreter();
            final int index = (int) values[0];
            if (index < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }
            final Object value = (Object) values[1];
            final Object[] store = new Object[index + 1];
            Arrays.fill(store, nil());
            store[index] = value;
            setStore(array, store, getSize(array) + 1);
            return array;
        }

        @Specialization(guards = "isArgsLengthTwo(values)", rewriteOn = {ClassCastException.class, IndexOutOfBoundsException.class})
        public Object insert(RubyArray array, Object[] values) {
            final int index = (int) values[0];
            final int value = (int) values[1];
            final int[] store = (int[]) getStore(array);
            System.arraycopy(store, index, store, index + 1, getSize(array) - index);
            store[index] = value;
            setStore(array, store, getSize(array) + 1);
            return array;
        }

        @Specialization(contains = {"insert", "insertNull"})
        public Object insertBoxed(VirtualFrame frame, RubyArray array, Object[] values) {
            CompilerDirectives.transferToInterpreter();
            if (values.length == 1) {
                return array;
            }

            int index;
            if (values[0] instanceof Integer) {
                index = (int) values[0];
            } else {
                if (toIntNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
                }
                index = toIntNode.doInt(frame, values[0]);
            }

            final int valuesLength = values.length - 1;
            final int normalizedIndex = index < 0 ? normalizeIndex(array, index) + 1 : normalizeIndex(array, index);
            if (normalizedIndex < 0) {
                CompilerDirectives.transferToInterpreter();
                String errMessage = "index " + index + " too small for array; minimum: " + Integer.toString(-getSize(array));
                throw new RaiseException(getContext().getCoreLibrary().indexError(errMessage, this));
            }

            Object[] store = ArrayUtils.box(getStore(array));
            final int newSize = normalizedIndex < getSize(array) ? getSize(array) + valuesLength : normalizedIndex + valuesLength;
            store = Arrays.copyOf(store, newSize);
            if (normalizedIndex >= getSize(array)) {
                for (int i = getSize(array); i < normalizedIndex; i++) {
                    store[i] = nil();
                }
            }
            final int dest = normalizedIndex + valuesLength;
            final int len = getSize(array) - normalizedIndex;
            if (normalizedIndex < getSize(array)) {
                System.arraycopy(store, normalizedIndex, store, dest, len);
            }
            System.arraycopy(values, 1, store, normalizedIndex, valuesLength);
            setStore(array, store, newSize);

            return array;
        }

        protected static boolean isArgsLengthTwo(Object[] others) {
            return others.length == 2;
        }
        
        protected static boolean isIntIndexAndOtherSingleObjectArg(Object[] others) {
            return others.length == 2 && others[0] instanceof Integer && others[1] instanceof Object;
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true, returnsEnumeratorIfNoBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        @Specialization(guards = "isNullArray(array)")
        public RubyBasicObject mapNull(RubyArray array, RubyProc block) {
            return createEmptyArray();
        }

        @Specialization(guards = "isIntArray(array)")
        public Object mapIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) getStore(array);
            final int arraySize = getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isLongArray(array)")
        public Object mapLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) getStore(array);
            final int arraySize = getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object mapFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            final double[] store = (double[]) getStore(array);
            final int arraySize = getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object mapObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) getStore(array);
            final int arraySize = getSize(array);
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;
            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return createGeneralArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true, returnsEnumeratorIfNoBlock = true, raiseIfFrozenSelf = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child private ArrayWriteDenormalizedNode writeNode;

        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public RubyBasicObject mapInPlaceNull(RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object mapInPlaceFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final int[] store = (int[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            writeNode.executeWrite(frame, array, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }


            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object mapInPlaceObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null));
            }

            final Object[] store = (Object[]) getStore(array);

            int count = 0;

            try {
                outer:
                for (int n = 0; n < getSize(array); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            writeNode.executeWrite(frame, array, n, yield(frame, block, store[n]));
                            continue outer;
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
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
        public Object max(VirtualFrame frame, RubyArray array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> maximum = new Memo<>();

            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
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
        public RubyBasicObject max(VirtualFrame frame, Object maximumObject, Object value) {
            final Memo<Object> maximum = (Memo<Object>) maximumObject;

            // TODO(CS): cast

            final Object current = maximum.get();

            if (current == null || (int) compareNode.call(frame, value, "<=>", null, current) < 0) {
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
        public Object min(VirtualFrame frame, RubyArray array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> minimum = new Memo<>();

            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
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
        public RubyBasicObject min(VirtualFrame frame, Object minimumObject, Object value) {
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
    public abstract static class PackNode extends ArrayCoreMethodNode {

        @Child private TaintNode taintNode;

        public PackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "byteListsEqual(format, cachedFormat)")
        public RubyBasicObject packCached(
                VirtualFrame frame,
                RubyArray array,
                RubyString format,
                @Cached("privatizeByteList(format)") ByteList cachedFormat,
                @Cached("create(compileFormat(format))") DirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, new Object[]{getStore(array), getSize(array)});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishPack(cachedFormat, result);
        }

        @Specialization(contains = "packCached")
        public RubyBasicObject packUncached(
                VirtualFrame frame,
                RubyArray array,
                RubyString format,
                @Cached("create()") IndirectCallNode callPackNode) {
            final PackResult result;

            try {
                result = (PackResult) callPackNode.call(frame, compileFormat(format), new Object[]{getStore(array), getSize(array)});
            } catch (PackException e) {
                CompilerDirectives.transferToInterpreter();
                throw handleException(e);
            }

            return finishPack(StringNodes.getByteList(format), result);
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

        private RubyBasicObject finishPack(ByteList format, PackResult result) {
            final RubyBasicObject string = createString(new ByteList(result.getOutput(), 0, result.getOutputLength()));

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
        public Object pack(VirtualFrame frame, RubyArray array, boolean format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object pack(VirtualFrame frame, RubyArray array, int format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization
        public Object pack(VirtualFrame frame, RubyArray array, long format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = "isNil(format)")
        public Object packNil(VirtualFrame frame, RubyArray array, Object format) {
            return ruby(frame, "raise TypeError");
        }

        @Specialization(guards = {"!isRubyString(format)", "!isBoolean(format)", "!isInteger(format)", "!isLong(format)", "!isNil(format)"})
        public Object pack(VirtualFrame frame, RubyArray array, Object format) {
            return ruby(frame, "pack(format.to_str)", "format", format);
        }

        protected ByteList privatizeByteList(RubyString string) {
            return StringNodes.getByteList(string).dup();
        }

        protected boolean byteListsEqual(RubyString string, ByteList byteList) {
            return StringNodes.getByteList(string).equal(byteList);
        }

        protected CallTarget compileFormat(RubyString format) {
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
        public Object pop(RubyArray array, NotProvided n) {
            if (popOneNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                popOneNode = insert(PopOneNodeGen.create(getContext(), getEncapsulatingSourceSection(), null));
            }

            return popOneNode.executePopOne(array);
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object popNilWithNum(VirtualFrame frame, RubyArray array, Object object) {
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
        public RubyBasicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject popLongFixnumInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object popLongFixnumWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject popFloatInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(contains = "popFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object popFloatWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object popObjectWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject popIntegerFixnumInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNumObj", guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popIntegerFixnumWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final int[] filler = new int[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject popLongFixnumInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;
            }
        }

        @Specialization(contains = "popLongFixnumInBoundsWithNumObj", guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popLongFixnumWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final long[] filler = new long[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;            }
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject popFloatInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(contains = "popFloatInBoundsWithNumObj", guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popFloatWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final double[] filler = new double[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
                return result;}
        }

        @Specialization(guards = { "isObjectArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object popObjectWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numPop = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, getSize(array) - numPop, getSize(array)), numPop);
                final Object[] filler = new Object[numPop];
                System.arraycopy(filler, 0, store, getSize(array) - numPop, numPop);
                setStore(array, store, getSize(array) - numPop);
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
        public RubyBasicObject leftShift(RubyArray array, Object value) {
            return appendOneNode.executeAppendOne(array, value);
        }

    }

    @CoreMethod(names = {"push", "__append__"}, argumentsAsArray = true, raiseIfFrozenSelf = true)
    public abstract static class PushNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isNullArray(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyBasicObject pushNullEmptySingleIntegerFixnum(RubyArray array, Object... values) {
            setStore(array, new int[]{(int) values[0]}, 1);
            return array;
        }

        @Specialization(guards = {"isNullArray(array)", "isSingleLongFixnum(array, values)"})
        public RubyBasicObject pushNullEmptySingleIntegerLong(RubyArray array, Object... values) {
            setStore(array, new long[]{(long) values[0]}, 1);
            return array;
        }

        @Specialization(guards = "isNullArray(array)")
        public RubyBasicObject pushNullEmptyObjects(RubyArray array, Object... values) {
            setStore(array, values, values.length);
            return array;
        }

        @Specialization(guards = {"!isNullArray(array)", "isEmptyArray(array)"})
        public RubyBasicObject pushEmptySingleIntegerFixnum(RubyArray array, Object... values) {
            // TODO CS 20-Apr-15 in reality might be better reusing any current storage, but won't worry about that for now
            setStore(array, values, values.length);
            return array;
        }

        @Specialization(guards = {"isIntArray(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyBasicObject pushIntegerFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (int) values[0];
            setStore(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "!isSingleIntegerFixnum(array, values)", "!isSingleLongFixnum(array, values)" })
        public RubyBasicObject pushIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + values.length;

            int[] oldStore = (int[]) getStore(array);
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

        @Specialization(guards = {"isLongArray(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyBasicObject pushLongFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (long) (int) values[0];
            setStore(array, store, newSize);
            return array;
        }

        @Specialization(guards = {"isLongArray(array)", "isSingleLongFixnum(array, values)"})
        public RubyBasicObject pushLongFixnumSingleLongFixnum(RubyArray array, Object... values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            long[] store = (long[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (long) values[0];
            setStore(array, store, newSize);
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public RubyBasicObject pushFloat(RubyArray array, Object... values) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store

            if (getSize(array) != 0) {
                throw new UnsupportedOperationException();
            }

            setStore(array, values, values.length);
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public RubyBasicObject pushObject(RubyArray array, Object... values) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + values.length;

            Object[] store = (Object[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            setStore(array, store, newSize);
            return array;
        }

        protected boolean isSingleIntegerFixnum(RubyArray array, Object... values) {
            return values.length == 1 && values[0] instanceof Integer;
        }

        protected boolean isSingleLongFixnum(RubyArray array, Object... values) {
            return values.length == 1 && values[0] instanceof Long;
        }

    }

    // Not really a core method - used internally

    public abstract static class PushOneNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushOneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNullArray(array)")
        public RubyBasicObject pushEmpty(RubyArray array, Object value) {
            setStore(array, new Object[]{value}, 1);
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public RubyBasicObject pushIntegerFixnumIntegerFixnum(RubyArray array, int value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            int[] store = (int[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                setStore(array, store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), getSize(array));
            }

            store[oldSize] = value;
            setStore(array, store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(value)" })
        public RubyBasicObject pushIntegerFixnumObject(RubyArray array, Object value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            final int[] oldStore = (int[]) getStore(array);
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
        public RubyBasicObject pushObjectObject(RubyArray array, Object value) {
            final int oldSize = getSize(array);
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) getStore(array);

            if (store.length < newSize) {
                extendBranch.enter();
                setStore(array, store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), getSize(array));
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

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return createEmptyArray();
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, RubyArray array, RubyProc block) {
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

                    if (! yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
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

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
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

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
            }
            return array;
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, RubyArray array, RubyProc block) {
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

        @Specialization(guards = "isNullArray(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)")
        public Object rejectInPlaceInt(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isLongArray(array)")
        public Object rejectInPlaceLong(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isDoubleArray(array)")
        public Object rejectInPlaceDouble(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                setStore(array, store, i);
                return array;
            } else {
                return nil();
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, RubyArray array, RubyProc block) {
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

        @Specialization(guards = "isNullArray(other)")
        public RubyBasicObject replace(RubyArray array, RubyArray other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, null, 0);
            return array;
        }

        @Specialization(guards = "isIntArray(other)")
        public RubyBasicObject replaceIntegerFixnum(RubyArray array, RubyArray other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((int[]) getStore(other), getSize(other)), getSize(other));
            return array;
        }

        @Specialization(guards = "isLongArray(other)")
        public RubyBasicObject replaceLongFixnum(RubyArray array, RubyArray other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((long[]) getStore(other), getSize(other)), getSize(other));
            return array;
        }

        @Specialization(guards = "isDoubleArray(other)")
        public RubyBasicObject replaceFloat(RubyArray array, RubyArray other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((double[]) getStore(other), getSize(other)), getSize(other));
            return array;
        }

        @Specialization(guards = "isObjectArray(other)")
        public RubyBasicObject replaceObject(RubyArray array, RubyArray other) {
            CompilerDirectives.transferToInterpreter();

            setStore(array, Arrays.copyOf((Object[]) getStore(other), getSize(other)), getSize(other));
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

        @Specialization(guards = "isNullArray(array)")
        public Object selectNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return createEmptyArray();
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object selectObject(VirtualFrame frame, RubyArray array, RubyProc block) {
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

                    if (yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
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

        @Specialization(guards = "isIntArray(array)")
        public Object selectFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
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
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
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

        public abstract Object executeShift(VirtualFrame frame, RubyArray array, Object n);

        @Specialization(guards = "isEmptyArray(array)")
        public Object shiftNil(VirtualFrame frame, RubyArray array, NotProvided n) {
            return nil();
        }

        @Specialization(guards = "isIntArray(array)", rewriteOn = UnexpectedResultException.class)
        public int shiftIntegerFixnumInBounds(VirtualFrame frame, RubyArray array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int[] store = ((int[]) getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftIntegerFixnumInBounds", guards = "isIntArray(array)")
        public Object shiftIntegerFixnum(VirtualFrame frame, RubyArray array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int[] store = ((int[]) getStore(array));
                final int value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final int[] filler = new int[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public long shiftLongFixnumInBounds(VirtualFrame frame, RubyArray array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final long[] store = ((long[]) getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBounds", guards = "isLongArray(array)")
        public Object shiftLongFixnum(VirtualFrame frame, RubyArray array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final long[] store = ((long[]) getStore(array));
                final long value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final long[] filler = new long[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public double shiftFloatInBounds(VirtualFrame frame, RubyArray array, NotProvided n) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final double[] store = ((double[]) getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(contains = "shiftFloatInBounds", guards = "isDoubleArray(array)")
        public Object shiftFloat(VirtualFrame frame, RubyArray array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final double[] store = ((double[]) getStore(array));
                final double value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final double[] filler = new double[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObject(VirtualFrame frame, RubyArray array, NotProvided n) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final Object[] store = ((Object[]) getStore(array));
                final Object value = store[0];
                System.arraycopy(store, 1, store, 0, getSize(array) - 1);
                final Object[] filler = new Object[1];
                System.arraycopy(filler, 0, store, getSize(array) - 1, 1);
                setStore(array, store, getSize(array) - 1);
                return value;
            }
        }

        @Specialization(guards = { "isEmptyArray(array)", "wasProvided(object)" })
        public Object shiftNilWithNum(VirtualFrame frame, RubyArray array, Object object) {
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
        public RubyBasicObject popIntegerFixnumInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBoundsWithNum", guards = "isIntArray(array)")
        public Object popIntegerFixnumWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isLongArray(array)", rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject shiftLongFixnumInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBoundsWithNum", guards = "isLongArray(array)")
        public Object shiftLongFixnumWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isDoubleArray(array)", rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject shiftFloatInBoundsWithNum(VirtualFrame frame, RubyArray array, int num) throws UnexpectedResultException {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftFloatInBoundsWithNum", guards = "isDoubleArray(array)")
        public Object shiftFloatWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object shiftObjectWithNum(VirtualFrame frame, RubyArray array, int num) {
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject shiftIntegerFixnumInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftIntegerFixnumInBoundsWithNumObj", guards = { "isIntArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftIntegerFixnumWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final int[] store = ((int[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final int[] filler = new int[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject shiftLongFixnumInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftLongFixnumInBoundsWithNumObj", guards = { "isLongArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftLongFixnumWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final long[] store = ((long[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, numShift), numShift);
                final long[] filler = new long[numShift];
                System.arraycopy(store, numShift, store, 0 , getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;          }
        }

        @Specialization(guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" }, rewriteOn = UnexpectedResultException.class)
        public RubyBasicObject shiftFloatInBoundsWithNumObj(VirtualFrame frame, RubyArray array, Object object) throws UnexpectedResultException {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                throw new UnexpectedResultException(nil());
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(contains = "shiftFloatInBoundsWithNumObj", guards = { "isDoubleArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftFloatWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final double[] store = ((double[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final double[] filler = new double[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
                return result;
            }
        }

        @Specialization(guards = { "isObjectArray(array)", "!isInteger(object)", "wasProvided(object)" })
        public Object shiftObjectWithNumObj(VirtualFrame frame, RubyArray array, Object object) {
            if (toIntNode == null) {
                CompilerDirectives.transferToInterpreter();
                toIntNode = insert(ToIntNodeGen.create(getContext(), getSourceSection(), null));
            }
            final int num = toIntNode.doInt(frame, object);
            if (num < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("negative array size", this));
            }
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, getSize(array) == 0)) {
                return nil();
            } else {
                final int numShift = getSize(array) < num ? getSize(array) : num;
                final Object[] store = ((Object[]) getStore(array));
                final RubyBasicObject result = createArray(Arrays.copyOfRange(store, 0, getSize(array) - numShift), numShift);
                final Object[] filler = new Object[numShift];
                System.arraycopy(store, numShift, store, 0, getSize(array) - numShift);
                System.arraycopy(filler, 0, store, getSize(array) - numShift, numShift);
                setStore(array, store, getSize(array) - numShift);
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
        public int size(RubyArray array) {
            return getSize(array);
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
        public RubyBasicObject sortNull(RubyArray array, Object block) {
            return createEmptyArray();
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntArray(array)", "isSmall(array)"})
        public RubyBasicObject sortVeryShortIntegerFixnum(VirtualFrame frame, RubyArray array, NotProvided block) {
            final int[] store = (int[]) getStore(array);
            final int[] newStore = new int[store.length];

            final int size = getSize(array);

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
        public RubyBasicObject sortVeryShortLongFixnum(VirtualFrame frame, RubyArray array, NotProvided block) {
            final long[] store = (long[]) getStore(array);
            final long[] newStore = new long[store.length];

            final int size = getSize(array);

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
        public RubyBasicObject sortVeryShortObject(VirtualFrame frame, RubyArray array, NotProvided block) {
            final Object[] oldStore = (Object[]) getStore(array);
            final Object[] store = Arrays.copyOf(oldStore, oldStore.length);

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

            return createArray(store, size);
        }

        @Specialization
        public Object sortUsingRubinius(VirtualFrame frame, RubyArray array, RubyProc block) {
            return sortUsingRubinius(frame, array, (Object) block);
        }

        @Specialization(guards = {"!isNullArray(array)", "!isSmall(array)"})
        public Object sortUsingRubinius(VirtualFrame frame, RubyArray array, Object block) {
            if (block == NotProvided.INSTANCE) {
                return ruby(frame, "sorted = dup; Rubinius.privately { sorted.isort!(0, right) }; sorted", "right", getSize(array));
            } else {
                return ruby(frame, "sorted = dup; Rubinius.privately { sorted.isort_block!(0, right, block) }; sorted", "right", getSize(array), "block", block);
            }
        }

        private int castSortValue(Object value) {
            if (value instanceof Integer) {
                return (int) value;
            }

            CompilerDirectives.transferToInterpreter();

            // TODO CS 14-Mar-15 - what's the error message here?
            throw new RaiseException(getContext().getCoreLibrary().argumentError("expecting a Fixnum to sort", this));
        }

        protected static boolean isSmall(RubyArray array) {
            return getSize(array) <= ARRAYS_SMALL;
        }

    }

    @CoreMethod(names = "unshift", argumentsAsArray = true, raiseIfFrozenSelf = true)
    public abstract static class UnshiftNode extends CoreMethodArrayArgumentsNode {

        public UnshiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject unshift(RubyArray array, Object... args) {
            CompilerDirectives.transferToInterpreter();

            slowUnshift(array, args);
            return array;
        }

    }

    @CoreMethod(names = "zip", required = 1, argumentsAsArray = true)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        public ZipNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isObjectArray(array)", "isSingleIntegerFixnumArray(others)"})
        public RubyBasicObject zipObjectIntegerFixnum(RubyArray array, Object[] others) {
            final RubyArray other = (RubyArray) others[0];
            final Object[] a = (Object[]) getStore(array);

            final int[] b = (int[]) getStore(other);
            final int bLength = getSize(other);

            final int zippedLength = getSize(array);
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

        @Specialization(guards = {"isObjectArray(array)", "isSingleObjectArray(others)"})
        public RubyBasicObject zipObjectObject(RubyArray array, Object[] others) {
            final RubyArray other = (RubyArray) others[0];
            final Object[] a = (Object[]) getStore(array);

            final Object[] b = (Object[]) getStore(other);
            final int bLength = getSize(other);

            final int zippedLength = getSize(array);
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

        @Specialization(guards = {"!isSingleObjectArray(others)"})
        public Object zipObjectObjectNotSingleObject(VirtualFrame frame, RubyArray array, Object[] others) {
            return zipRuby(frame, others);
        }

        @Specialization(guards = {"!isSingleIntegerFixnumArray(others)"})
        public Object zipObjectObjectNotSingleInteger(VirtualFrame frame, RubyArray array, Object[] others) {
            return zipRuby(frame, others);
        }

        @Specialization(guards = {"!isObjectArray(array)"})
        public Object zipObjectObjectNotObject(VirtualFrame frame, RubyArray array, Object[] others) {
            return zipRuby(frame, others);
        }

        private Object zipRuby(VirtualFrame frame, Object[] others) {
            RubyBasicObject proc = RubyArguments.getBlock(frame.getArguments());
            if (proc == null) {
                proc = nil();
            }
            return ruby(frame, "zip_internal(*others, &block)", "others", createArray(others, others.length), "block", proc);
        }

        protected static boolean isSingleIntegerFixnumArray(Object[] others) {
            return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof int[];
        }

        protected static boolean isSingleObjectArray(Object[] others) {
            return others.length == 1 && others[0] instanceof RubyArray && ArrayNodes.getStore(((RubyArray) others[0])) instanceof Object[];
        }

    }

    public static class ArrayAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createEmptyArray(rubyClass);
        }

    }
}
