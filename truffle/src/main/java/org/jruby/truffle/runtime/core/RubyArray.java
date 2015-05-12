/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.ForeignAccessFactory;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.util.cli.Options;

import java.util.Arrays;
import java.util.Random;

/**
 * Implements the Ruby {@code Array} class.
 */
public final class RubyArray extends RubyBasicObject {

    public static final int ARRAYS_SMALL = Options.TRUFFLE_ARRAYS_SMALL.load();

    private static final boolean RANDOMIZE_STORAGE_ARRAY = Options.TRUFFLE_RANDOMIZE_STORAGE_ARRAY.load();
    private static final Random random = new Random(Options.TRUFFLE_RANDOMIZE_SEED.load());

    private Object store;
    private int size;

    public RubyArray(RubyClass arrayClass) {
        this(arrayClass, null, 0);
    }

    public RubyArray(RubyClass arrayClass, Object store, int size) {
        super(arrayClass);
        setStore(store, size);
    }

    public static RubyArray fromObject(RubyClass arrayClass, Object object) {
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

        return new RubyArray(arrayClass, store, 1);
    }

    public static RubyArray fromObjects(RubyClass arrayClass, Object... objects) {
        return new RubyArray(arrayClass, storeFromObjects(arrayClass.getContext(), objects), objects.length);
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

    public Object[] slowToArray() {
        return Arrays.copyOf(ArrayUtils.box(store), size);
    }

    public Object slowShift() {
        CompilerAsserts.neverPartOfCompilation();

        if (size == 0) {
            return getContext().getCoreLibrary().getNilObject();
        } else {
            store = ArrayUtils.box(store);
            final Object value = ((Object[]) store)[0];
            System.arraycopy(store, 1, store, 0, size - 1);
            size--;
            return value;
        }
    }

    public void slowUnshift(Object... values) {
        final Object[] newStore = new Object[size + values.length];
        System.arraycopy(values, 0, newStore, 0, values.length);
        ArrayUtils.copy(store, newStore, values.length, size);
        setStore(newStore, newStore.length);
    }

    public void slowPush(Object value) {
        store = Arrays.copyOf(ArrayUtils.box(store), size + 1);
        ((Object[]) store)[size] = value;
        size++;
    }

    public int normalizeIndex(int index) {
        return normalizeIndex(size, index);
    }

    public static int normalizeIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public int clampExclusiveIndex(int index) {
        return clampExclusiveIndex(size, index);
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

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int size) {
        assert verifyStore(store, size);

        if (RANDOMIZE_STORAGE_ARRAY) {
            store = randomizeStorageStrategy(getContext(), store, size);
            assert verifyStore(store, size);
        }

        this.store = store;
        this.size = size;
    }

    public void setSize(int size) {
        assert verifyStore(store, size);
        this.size = size;
    }

    @CompilerDirectives.TruffleBoundary
    private static Object randomizeStorageStrategy(RubyContext context, Object store, int size) {
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

    public int getSize() {
        return size;
    }

    private boolean verifyStore(Object store, int size) {
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

    @Override
    public void visitObjectGraphChildren(ObjectSpaceManager.ObjectGraphVisitor visitor) {
        for (Object object : slowToArray()) {
            if (object instanceof RubyBasicObject) {
                ((RubyBasicObject) object).visitObjectGraph(visitor);
            }
        }
    }

    public static class ArrayAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyArray(rubyClass);
        }

    }

    @Override
    public ForeignAccessFactory getForeignAccessFactory() {
        return new ArrayForeignAccessFactory(getContext());
    }

}
