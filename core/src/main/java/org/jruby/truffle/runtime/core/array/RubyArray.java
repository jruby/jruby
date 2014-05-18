/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ArrayUtils;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;

import java.util.Arrays;

/**
 * Implements the Ruby {@code Array} class.
 */
public final class RubyArray extends RubyObject {

    public static class RubyArrayClass extends RubyClass {

        public RubyArrayClass(RubyClass objectClass) {
            super(null, objectClass, "Array");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyArray(this);
        }

    }

    private Object store;
    private int size;

    public RubyArray(RubyClass arrayClass) {
        this(arrayClass, null, 0);
    }

    public RubyArray(RubyClass arrayClass, Object store, int size) {
        super(arrayClass);

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;

        this.store = store;
        this.size = size;
    }

    public static RubyArray fromObject(RubyClass arrayClass, Object object) {
        RubyNode.notDesignedForCompilation();

        final Object store;

        if (object instanceof Integer) {
            store = new int[]{(int) object};
        } else if (object instanceof RubyFixnum.IntegerFixnum) {
            store = new int[]{((RubyFixnum.IntegerFixnum) object).getValue()};
        } else if (object instanceof Long) {
            store = new long[]{(long) object};
        } else if (object instanceof RubyFixnum.LongFixnum) {
            store = new long[]{((RubyFixnum.LongFixnum) object).getValue()};
        } else if (object instanceof Double) {
            store = new double[]{(double) object};
        } else if (object instanceof RubyFloat) {
            store = new double[]{((RubyFloat) object).getValue()};
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
            } else if (object instanceof RubyFixnum.IntegerFixnum) {
                canUseDouble = false;
            } else if (object instanceof Long) {
                canUseInteger = canUseInteger && RubyFixnum.fitsIntoInteger((long) object);
                canUseDouble = false;
            } else if (object instanceof RubyFixnum.LongFixnum) {
                canUseInteger = canUseInteger && RubyFixnum.fitsIntoInteger(((RubyFixnum.LongFixnum) object).getValue());
                canUseDouble = false;
            } else if (object instanceof Double) {
                canUseInteger = false;
                canUseLong = false;
            } else if (object instanceof RubyFloat) {
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
                store[n] = RubyFixnum.toInt(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else if (canUseLong) {
            final long[] store = new long[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = RubyFixnum.toLong(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else if (canUseDouble) {
            final double[] store = new double[objects.length];

            for (int n = 0; n < objects.length; n++) {
                store[n] = RubyFloat.toDouble(objects[n]);
            }

            return new RubyArray(arrayClass, store, objects.length);
        } else {
            return new RubyArray(arrayClass, objects, objects.length);
        }
    }

    public Object[] slowToArray() {
        CompilerAsserts.neverPartOfCompilation();
        return Arrays.copyOf(ArrayUtils.box(store), size);
    }

    public Object slowShift() {
        CompilerAsserts.neverPartOfCompilation();

        if (size == 0) {
            return NilPlaceholder.INSTANCE;
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
        CompilerAsserts.neverPartOfCompilation();
        store = Arrays.copyOf(ArrayUtils.box(store), size + 1);
        ((Object[]) store)[size] = value;
        size++;
    }

    public int normaliseIndex(int index) {
        return normaliseIndex(size, index);
    }

    public int normaliseExclusiveIndex(int index) {
        return normaliseExclusiveIndex(size, index);
    }

    public static int normaliseIndex(int length, int index) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, index < 0)) {
            return length + index;
        } else {
            return index;
        }
    }

    public static int normaliseExclusiveIndex(int length, int exclusiveIndex) {
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, exclusiveIndex < 0)) {
            return length + exclusiveIndex + 1;
        } else {
            return exclusiveIndex;
        }
    }

    public Object getStore() {
        return store;
    }

    public void setStore(Object store, int size) {
        this.store = store;
        this.size = size;

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;

        assert store == null
                || store instanceof Object[]
                || store instanceof int[]
                || store instanceof long[]
                || store instanceof double[];

        assert !(store instanceof Object[]) || RubyContext.shouldObjectsBeVisible(size, (Object[]) store);
        assert !(store instanceof Object[]) || size <= ((Object[]) store).length;
        assert !(store instanceof int[]) || size <= ((int[]) store).length;
        assert !(store instanceof long[]) || size <= ((long[]) store).length;
        assert !(store instanceof double[]) || size <= ((double[]) store).length;
    }

    @Override
    public Object dup() {
        throw new UnsupportedOperationException();
    }


}
