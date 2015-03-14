/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.array;

import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.util.ArrayUtils;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
@ImportGuards(ArrayGuards.class)
public abstract class ArrayWriteNormalizedNode extends RubyNode {

    @Child private EnsureCapacityArrayNode ensureCapacityNode;
    @Child private GeneralizeArrayNode generalizeNode;

    public ArrayWriteNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);

        // TODO CS 9-Feb-15 make this lazy later on
        ensureCapacityNode = EnsureCapacityArrayNodeFactory.create(context, sourceSection, null, null);
        generalizeNode = GeneralizeArrayNodeFactory.create(context, sourceSection, null, null);
    }

    public ArrayWriteNormalizedNode(ArrayWriteNormalizedNode prev) {
        super(prev);
        ensureCapacityNode = prev.ensureCapacityNode;
        generalizeNode = prev.generalizeNode;
    }

    public abstract Object executeWrite(VirtualFrame frame, RubyArray array, int index, Object value);

    // Writing at index 0 into a null array creates a new array of the most specific type

    @Specialization(
            guards={"isNullArray", "isIndex0"}
    )
    public boolean writeNull0(RubyArray array, int index, boolean value) {
        array.setStore(new Object[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray", "isIndex0"}
    )
    public int writeNull0(RubyArray array, int index, int value) {
        array.setStore(new int[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray", "isIndex0"}
    )
    public long writeNull0(RubyArray array, int index, long value) {
        array.setStore(new long[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray", "isIndex0"}
    )
    public double writeNull0(RubyArray array, int index, double value) {
        array.setStore(new double[]{value}, 1);
        return value;
    }

    @Specialization(
            guards={"isNullArray", "isIndex0"}
    )
    public RubyBasicObject writeNull0(RubyArray array, int index, RubyBasicObject value) {
        array.setStore(new Object[]{value}, 1);
        return value;
    }

    // Writing beyond index 0 in a null array creates an Object[] as we need to fill the rest with nil

    @Specialization(
            guards={"isNullArray", "!isIndex0"}
    )
    public Object writeNullBeyond(RubyArray array, int index, Object value) {
        final Object[] store = new Object[index + 1];

        for (int n = 0; n < index; n++) {
            store[n] = nil();
        }

        store[index] = value;
        array.setStore(store, store.length);
        return value;
    }

    // Writing within an existing array with a compatible type

    @Specialization(
            guards={"isObjectArray", "isInBounds"}
    )
    public boolean writeWithin(RubyArray array, int index, boolean value) {
        final Object[] store = (Object[]) array.getStore();
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isIntArray", "isInBounds"}
    )
    public int writeWithin(RubyArray array, int index, int value) {
        final int[] store = (int[]) array.getStore();
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isLongArray", "isInBounds"}
    )
    public int writeWithinIntIntoLong(RubyArray array, int index, int value) {
        writeWithin(array, index, (long) value);
        return value;
    }

    @Specialization(
            guards={"isLongArray", "isInBounds"}
    )
    public long writeWithin(RubyArray array, int index, long value) {
        final long[] store = (long[]) array.getStore();
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isDoubleArray", "isInBounds"}
    )
    public double writeWithin(RubyArray array, int index, double value) {
        final double[] store = (double[]) array.getStore();
        store[index] = value;
        return value;
    }

    @Specialization(
            guards={"isObjectArray", "isInBounds"}
    )
    public Object writeWithin(RubyArray array, int index, Object value) {
        final Object[] store = (Object[]) array.getStore();
        store[index] = value;
        return value;
    }

    // Writing within an existing array with an incompatible type - need to generalise

    @Specialization(
            guards={"isIntArray", "isInBounds"}
    )
    public long writeWithinInt(RubyArray array, int index, long value) {
        final int[] intStore = (int[]) array.getStore();
        final long[] longStore = new long[array.getSize()];

        for (int n = 0; n < array.getSize(); n++) {
            longStore[n] = intStore[n];
        }

        longStore[index] = value;
        array.setStore(longStore, array.getSize());
        return value;
    }

    @Specialization(
            guards={"isIntArray", "isInBounds", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinInt(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((int[]) array.getStore());
        objectStore[index] = value;
        array.setStore(objectStore, array.getSize());
        return value;
    }

    @Specialization(
            guards={"isLongArray", "isInBounds", "!isInteger(value)", "!isLong(value)"}
    )
    public Object writeWithinLong(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((long[]) array.getStore());
        objectStore[index] = value;
        array.setStore(objectStore, array.getSize());
        return value;
    }

    @Specialization(
            guards={"isDoubleArray", "isInBounds", "!isDouble(value)"}
    )
    public Object writeWithinDouble(RubyArray array, int index, Object value) {
        final Object[] objectStore = ArrayUtils.box((double[]) array.getStore());
        objectStore[index] = value;
        array.setStore(objectStore, array.getSize());
        return value;
    }

    // Extending an array of compatible type by just one

    @Specialization(
            guards={"isObjectArray", "isExtendingByOne"}
    )
    public boolean writeExtendByOne(VirtualFrame frame, RubyArray array, int index, boolean value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isIntArray", "isExtendingByOne"}
    )
    public int writeExtendByOne(VirtualFrame frame, RubyArray array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((int[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isLongArray", "isExtendingByOne"}
    )
    public int writeExtendByOneIntIntoLong(VirtualFrame frame, RubyArray array, int index, int value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isLongArray", "isExtendingByOne"}
    )
    public long writeExtendByOne(VirtualFrame frame, RubyArray array, int index, long value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((long[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isDoubleArray", "isExtendingByOne"}
    )
    public double writeExtendByOne(VirtualFrame frame, RubyArray array, int index, double value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((double[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isObjectArray", "isExtendingByOne"}
    )
    public RubyBasicObject writeExtendByOne(VirtualFrame frame, RubyArray array, int index, RubyBasicObject value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        ((Object[]) array.getStore())[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    // Writing beyond the end of an array - may need to generalise to Object[] or otherwise extend

    @Specialization(
            guards={"!isObjectArray", "!isInBounds", "!isExtendingByOne"}
    )
    public Object writeBeyondPrimitive(VirtualFrame frame, RubyArray array, int index, Object value) {
        generalizeNode.executeGeneralize(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) array.getStore());

        for (int n = array.getSize(); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    @Specialization(
            guards={"isObjectArray", "!isInBounds", "!isExtendingByOne"}
    )
    public Object writeBeyondObject(VirtualFrame frame, RubyArray array, int index, Object value) {
        ensureCapacityNode.executeEnsureCapacity(frame, array, index + 1);
        final Object[] objectStore = ((Object[]) array.getStore());

        for (int n = array.getSize(); n < index; n++) {
            objectStore[n] = nil();
        }

        objectStore[index] = value;
        array.setStore(array.getStore(), index + 1);
        return value;
    }

    // Guards

    protected static boolean isInBounds(RubyArray array, int index) {
        return index >= 0 && index < array.getSize();
    }

    protected static boolean isExtendingByOne(RubyArray array, int index) {
        return index == array.getSize();
    }

    protected static boolean isIndex0(RubyArray array, int index) {
        return index == 0;
    }

}
