/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayWriteNormalizedNode extends RubyNode {

    @Child private EnsureCapacityArrayNode ensureCapacityNode;
    @Child private GeneralizeArrayNode generalizeNode;

    public ArrayWriteNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);

        // TODO CS 9-Feb-15 make this lazy later on
        ensureCapacityNode = EnsureCapacityArrayNodeGen.create(context, sourceSection, null, null);
        generalizeNode = GeneralizeArrayNodeGen.create(context, sourceSection, null, null);
    }

    public abstract Object executeWrite(VirtualFrame frame, RubyArray array, int index, Object value);

    // Writing at index 0 into a null array creates a new array of the most specific type

    @Specialization(
            guards={"isNullArray(array)", "isIndex0(array, index)"}
    )
    public boolean writeNull0(RubyArray array, int index, boolean value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray(array)", "isIndex0(array, index)"}
    )
    public int writeNull0(RubyArray array, int index, int value) {
        ArrayNodes.setStore(array, new int[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray(array)", "isIndex0(array, index)"}
    )
    public long writeNull0(RubyArray array, int index, long value) {
        ArrayNodes.setStore(array, new long[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray(array)", "isIndex0(array, index)"}
    )
    public double writeNull0(RubyArray array, int index, double value) {
        ArrayNodes.setStore(array, new double[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray(array)", "isIndex0(array, index)"}
    )
    public RubyBasicObject writeNull0(RubyArray array, int index, RubyBasicObject value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return value;
    }

    // Writing beyond index 0 in a null array creates an Object[] as we need to fill the rest with nil

    @Specialization(
            guards={"isNullArray(array)", "!isIndex0(array, index)"}
    )
    public Object writeNullBeyond(RubyArray array, int index, Object value) {
        final Object[] store = new Object[index + 1];

        for (int n = 0; n < index; n++) {
            store[n] = nil();
        }

        store[index] = value;
        ArrayNodes.setStore(array, store, store.length);
        return value;
    }

    // Writing within an existing array with a compatible type

    @Specialization(
            guards={"isObjectArray(array)", "isInBounds(array, index)"}
    )
    public boolean writeWithin(RubyArray array, int index, boolean value) {
        final Object[] store = (Object[]) ArrayNodes.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isIntArray(array)", "isInBounds(array, index)"}
    )
    public int writeWithin(RubyArray array, int index, int value) {
        final int[] store = (int[]) ArrayNodes.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isLongArray(array)", "isInBounds(array, index)"}
    )
    public int writeWithinIntIntoLong(RubyArray array, int index, int value) {
        writeWithin(array, index, (long) value);
        return value;
    }

    @Specialization(
            guards={"isLongArray(array)", "isInBounds(array, index)"}
    )
    public long writeWithin(RubyArray array, int index, long value) {
        final long[] store = (long[]) ArrayNodes.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isDoubleArray(array)", "isInBounds(array, index)"}
    )
    public double writeWithin(RubyArray array, int index, double value) {
        final double[] store = (double[]) ArrayNodes.getStore(array);
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isObjectArray(array)", "isInBounds(array, index)"}
    )
    public Object writeWithin(RubyArray array, int index, Object value) {
        final Object[] store = (Object[]) ArrayNodes.getStore(array);
        store[index] = value;
        return value;
    }

    // Writing within an existing array with an incompatible type - need to generalise

    @Specialization(
            guards={"isIntArray(array)", "isInBounds(array, index)"}
    )
    public long writeWithinInt(RubyArray array, int index, long value) {
        final int[] intStore = (int[]) ArrayNodes.getStore(array);
        final long[] longStore = new long[ArrayNodes.getSize(array)];

        for (int n = 0; n < ArrayNodes.getSize(array); n++) {
            longStore[n] = intStore[n];
        }

        longStore[index] = value;
        ArrayNodes.setStore(array, longStore, ArrayNodes.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isIntArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinInt(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((int[]) ArrayNodes.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, ArrayNodes.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isLongArray(array)", "isInBounds(array, index)", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinLong(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((long[]) ArrayNodes.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, ArrayNodes.getSize(array));
        return value;
    }

    @Specialization(
            guards={"isDoubleArray(array)", "isInBounds(array, index)", "!isDouble(value)"}
    )
    public Object writeWithinDouble(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((double[]) ArrayNodes.getStore(array));
        objectStore[index] = value;
        ArrayNodes.setStore(array, objectStore, ArrayNodes.getSize(array));
        return value;
    }

    // Extending an array of compatible type by just one

    @Specialization(
            guards={"isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public boolean writeExtendByOne(VirtualFrame frame, RubyArray array, int index, boolean value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isIntArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeExtendByOne(VirtualFrame frame, RubyArray array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((int[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isLongArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeExtendByOneIntIntoLong(VirtualFrame frame, RubyArray array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isLongArray(array)", "isExtendingByOne(array, index)"}
    )
    public long writeExtendByOne(VirtualFrame frame, RubyArray array, int index, long value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isDoubleArray(array)", "isExtendingByOne(array, index)"}
    )
    public double writeExtendByOne(VirtualFrame frame, RubyArray array, int index, double value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((double[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public RubyBasicObject writeExtendByOne(VirtualFrame frame, RubyArray array, int index, RubyBasicObject value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
        guards={"isObjectArray(array)", "isExtendingByOne(array, index)"}
    )
    public int writeObjectExtendByOne(VirtualFrame frame, RubyArray array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) ArrayNodes.getStore(array))[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    // Writing beyond the end of an array - may need to generalise to Object[] or otherwise extend

    @Specialization(
            guards={"!isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)"}
    )
    public Object writeBeyondPrimitive(VirtualFrame frame, RubyArray array, int index, Object value) {
        generalizeNode.executeGeneralize(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) ArrayNodes.getStore(array));

        for (int n = ArrayNodes.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    @Specialization(
            guards={"isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)"}
    )
    public Object writeBeyondObject(VirtualFrame frame, RubyArray array, int index, Object value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) ArrayNodes.getStore(array));

        for (int n = ArrayNodes.getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        ArrayNodes.setStore(array, ArrayNodes.getStore(array), index + 1);
        return value;
    }

    // Guards

    protected static boolean isInBounds(RubyArray array, int index) {
        return index >= 0 && index < ArrayNodes.getSize(array);
    }

    protected static boolean isExtendingByOne(RubyArray array, int index) {
        return index == ArrayNodes.getSize(array);
    }

    protected static boolean isIndex0(RubyArray array, int index) {
        return index == 0;
    }

}
