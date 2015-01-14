/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayAllocationSite;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.cli.Options;

import java.util.Arrays;

/**
 * Implements the Ruby {@code Array} class.
 */
public final class RubyArray extends RubyBasicObject {

    public static final int ARRAYS_SMALL = Options.TRUFFLE_ARRAYS_SMALL.load();

    private final ArrayAllocationSite allocationSite;
    private Object store;
    private int size;

    public RubyArray(RubyClass arrayClass) {
        this(arrayClass, null, 0);
    }

    public RubyArray(RubyClass arrayClass, Object store, int size) {
        this(arrayClass, null, store, size);
    }

    public RubyArray(RubyClass arrayClass, ArrayAllocationSite allocationSite, Object store, int size) {
        super(arrayClass);
        this.allocationSite = allocationSite;
        setStore(store, size);
    }

    public static RubyArray fromObject(RubyClass arrayClass, Object object) {
        RubyNode.notDesignedForCompilation();

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
        RubyNode.notDesignedForCompilation();

        if (objects.length == 0) {
            return new RubyArray(arrayClass);
        }

        if (objects.length == 1) {
            return fromObject(arrayClass, objects[0]);
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

            return new RubyArray(arrayClass, store, objects.length);
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

            return new RubyArray(arrayClass, store, objects.length);
        } else if (canUseDouble) {
            final double[] store = new double[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = CoreLibrary.toDouble(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else {
            return new RubyArray(arrayClass, objects, objects.length);
        }
    }

    public Object[] slowToArray() {
        RubyNode.notDesignedForCompilation();

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
        RubyNode.notDesignedForCompilation();

        final Object[] newStore = new Object[size + values.length];
        System.arraycopy(values, 0, newStore, 0, values.length);
        ArrayUtils.copy(store, newStore, values.length, size);
        setStore(newStore, newStore.length);
    }

    public void slowPush(Object value) {
        RubyNode.notDesignedForCompilation();

        store = Arrays.copyOf(ArrayUtils.box(store), size + 1);
        ((Object[]) store)[size] = value;
        size++;
    }

    public int normaliseIndex(int index) {
        return normaliseIndex(size, index);
    }

    public static int normaliseIndex(int length, int index) {
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
        this.store = store;
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        assert verifyStore(this.store, size);
        this.size = size;
    }

    private boolean verifyStore(Object store, int size) {
        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;
        return true;
    }

    public ArrayAllocationSite getAllocationSite() {
        return allocationSite;
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
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyArray(rubyClass);
        }

    }
}
