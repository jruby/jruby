/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayWriteNormalizedNode extends RubyNode {

    @Child private ArrayEnsureCapacityNode ensureCapacityNode;
    @Child private ArrayGeneralizeNode generalizeNode;

    public ArrayWriteNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);

        // TODO CS 9-Feb-15 make this lazy later on
        ensureCapacityNode = ArrayEnsureCapacityNodeGen.create(context, sourceSection, null, null);
        generalizeNode = ArrayGeneralizeNodeGen.create(context, sourceSection, null, null);
    }

    public abstract Object executeWrite(DynamicObject array, int index, Object value);

    // Writing at index 0 into a null array creates a new array of the most specific type

    @Specialization(
            guards = { "isNullArray(array)", "index == 0" }
    )
    public boolean writeNull0(DynamicObject array, int index, boolean value) {
        Layouts.ARRAY.setStore(array, new Object[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return value;
    }

    @Specialization(
            guards = { "isNullArray(array)", "index == 0" }
    )
    public int writeNull0(DynamicObject array, int index, int value) {
        Layouts.ARRAY.setStore(array, new int[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return value;
    }

    @Specialization(
            guards = { "isNullArray(array)", "index == 0" }
    )
    public long writeNull0(DynamicObject array, int index, long value) {
        Layouts.ARRAY.setStore(array, new long[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return value;
    }

    @Specialization(
            guards = { "isNullArray(array)", "index == 0" }
    )
    public double writeNull0(DynamicObject array, int index, double value) {
        Layouts.ARRAY.setStore(array, new double[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return value;
    }

    @Specialization(
            guards = { "isNullArray(array)", "index == 0" }
    )
    public DynamicObject writeNull0(DynamicObject array, int index, DynamicObject value) {
        Layouts.ARRAY.setStore(array, new Object[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return value;
    }

    // Writing beyond index 0 in a null array creates an Object[] as we need to fill the rest with nil

    @Specialization(
            guards = { "isNullArray(array)", "index != 0" }
    )
    public Object writeNullBeyond(DynamicObject array, int index, Object value) {
        final Object[] store = new Object[index + 1];

        for (int n = 0; n < index; n++) {
            store[n] = nil();
        }

        store[index] = value;
        Layouts.ARRAY.setStore(array, store);
        Layouts.ARRAY.setSize(array, store.length);
        return value;
    }

    // Writing within an existing array with a compatible type

    @Specialization(
            guards = { "isIntArray(array)", "isInBounds(array, index)" }
    )
    public int writeWithin(DynamicObject array, int index, int value) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards = { "isLongArray(array)", "isInBounds(array, index)" }
    )
    public long writeWithin(DynamicObject array, int index, long value) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards = { "isDoubleArray(array)", "isInBounds(array, index)" }
    )
    public double writeWithin(DynamicObject array, int index, double value) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards = { "isObjectArray(array)", "isInBounds(array, index)" }
    )
    public Object writeWithin(DynamicObject array, int index, Object value) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
        store[index] = value;
        return value;
    }

    // Writing within an existing array with an incompatible type - need to generalise

    @Specialization(
            guards = { "isIntArray(array)", "isInBounds(array, index)" }
    )
    public long writeWithinInt(DynamicObject array, int index, long value) {
        final int[] intStore = (int[]) Layouts.ARRAY.getStore(array);
        final long[] longStore = new long[Layouts.ARRAY.getSize(array)];

        for (int n = 0; n < Layouts.ARRAY.getSize(array); n++) {
            longStore[n] = intStore[n];
        }

        longStore[index] = value;
        Layouts.ARRAY.setStore(array, longStore);
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards = { "isIntArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)" }
    )
    public Object writeWithinInt(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((int[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        Layouts.ARRAY.setStore(array, objectStore);
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards = { "isLongArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)" }
    )
    public Object writeWithinLong(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((long[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        Layouts.ARRAY.setStore(array, objectStore);
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
        return value;
    }

    @Specialization(
            guards = { "isDoubleArray(array)", "isInBounds(array, index)", "!isDouble(value)" }
    )
    public Object writeWithinDouble(DynamicObject array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((double[]) Layouts.ARRAY.getStore(array));
        objectStore[index] = value;
        Layouts.ARRAY.setStore(array, objectStore);
        Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
        return value;
    }

    // Extending an array of compatible type by just one

    @Specialization(
            guards = { "isIntArray(array)", "isExtendingByOne(array, index)" }
    )
    public int writeExtendByOne(DynamicObject array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        ((int[]) Layouts.ARRAY.getStore(array))[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    @Specialization(
            guards = { "isLongArray(array)", "isExtendingByOne(array, index)" }
    )
    public long writeExtendByOne(DynamicObject array, int index, long value) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        ((long[]) Layouts.ARRAY.getStore(array))[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    @Specialization(
            guards = { "isDoubleArray(array)", "isExtendingByOne(array, index)" }
    )
    public double writeExtendByOne(DynamicObject array, int index, double value) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        ((double[]) Layouts.ARRAY.getStore(array))[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    @Specialization(
            guards = { "isObjectArray(array)", "isExtendingByOne(array, index)" }
    )
    public Object writeExtendByOne(DynamicObject array, int index, Object value) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        ((Object[]) Layouts.ARRAY.getStore(array))[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    // Writing beyond the end of an array - may need to generalise to Object[] or otherwise extend

    @Specialization(
            guards = { "!isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)" }
    )
    public Object writeBeyondPrimitive(DynamicObject array, int index, Object value) {
        generalizeNode.executeGeneralize(array, index + 1);
        final Object[] objectStore = ((Object[]) Layouts.ARRAY.getStore(array));

        for (int n = Layouts.ARRAY.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    @Specialization(
            guards = { "isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)" }
    )
    public Object writeBeyondObject(DynamicObject array, int index, Object value) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        final Object[] objectStore = ((Object[]) Layouts.ARRAY.getStore(array));

        for (int n = Layouts.ARRAY.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        Layouts.ARRAY.setSize(array, index + 1);
        return value;
    }

    // Guards

    protected static boolean isInBounds(DynamicObject array, int index) {
        return index >= 0 && index < Layouts.ARRAY.getSize(array);
    }

    protected static boolean isExtendingByOne(DynamicObject array, int index) {
        return index == Layouts.ARRAY.getSize(array);
    }

}
