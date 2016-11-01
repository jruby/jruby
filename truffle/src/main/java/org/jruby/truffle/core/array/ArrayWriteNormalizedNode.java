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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.setSize;
import static org.jruby.truffle.core.array.ArrayHelpers.setStoreAndSize;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayWriteNormalizedNode extends RubyNode {

    public ArrayWriteNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeWrite(DynamicObject array, int index, Object value);

    // Writing at index 0 into a null array creates a new array of the most specific type

    @Specialization(guards = { "isNullArray(array)", "index == 0", "strategy.specializesFor(value)" }, limit = "ARRAY_STRATEGIES")
    public Object writeNull0(DynamicObject array, int index, Object value,
            @Cached("forValue(value)") ArrayStrategy strategy) {
        final ArrayMirror storeMirror = strategy.newArray(1);
        storeMirror.set(0, value);
        setStoreAndSize(array, storeMirror.getArray(), 1);
        return value;
    }

    // Writing beyond index 0 in a null array creates an Object[] as we need to fill the rest with nil

    @Specialization(guards = { "isNullArray(array)", "index != 0" })
    public Object writeNullBeyond(DynamicObject array, int index, Object value) {
        final Object[] store = new Object[index + 1];

        for (int n = 0; n < index; n++) {
            store[n] = nil();
        }

        store[index] = value;
        setStoreAndSize(array, store, store.length);
        return value;
    }

    // Writing within an existing array with a compatible type

    @Specialization(guards = {
            "isInBounds(array, index)", "strategy.matches(array)", "strategy.accepts(value)"
    }, limit = "ARRAY_STRATEGIES")
    public Object writeWithin(DynamicObject array, int index, Object value,
            @Cached("of(array, value)") ArrayStrategy strategy) {
        strategy.newMirror(array).set(index, value);
        return value;
    }

    // Writing within an existing array with an incompatible type - need to generalise

    @Specialization(guards = {
                    "isInBounds(array, index)", "currentStrategy.matches(array)",
                    "!currentStrategy.accepts(value)", "generalizedStrategy.accepts(value)",
    }, limit = "ARRAY_STRATEGIES")
    public Object writeWithinGeneralize(DynamicObject array, int index, Object value,
            @Cached("of(array)") ArrayStrategy currentStrategy,
            @Cached("currentStrategy.generalizeFor(value)") ArrayStrategy generalizedStrategy) {
        final int size = getSize(array);
        final ArrayMirror currentMirror = currentStrategy.newMirror(array);
        final ArrayMirror storeMirror = generalizedStrategy.newArray(currentMirror.getLength());
        currentMirror.copyTo(storeMirror, 0, 0, size);
        storeMirror.set(index, value);
        Layouts.ARRAY.setStore(array, storeMirror.getArray());
        return value;
    }

    // Extending an array of compatible type by just one

    @Specialization(guards = "isExtendingByOne(array, index)")
    public Object writeExtendByOne(DynamicObject array, int index, Object value,
            @Cached("createArrayAppendOneNode()") ArrayAppendOneNode appendNode) {
        appendNode.executeAppendOne(array, value);
        return value;
    }

    protected ArrayAppendOneNode createArrayAppendOneNode() {
        return ArrayAppendOneNodeGen.create(getContext(), null, null, null);
    }

    // Writing beyond the end of an array - may need to generalize to Object[] or otherwise extend

    @Specialization(guards = {
            "!isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)",
    })
    public Object writeBeyondPrimitive(DynamicObject array, int index, Object value,
            @Cached("create(getContext())") ArrayGeneralizeNode generalizeNode) {
        final int newSize = index + 1;
        final Object[] objectStore = generalizeNode.executeGeneralize(array, newSize);
        for (int n = getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }
        objectStore[index] = value;
        setStoreAndSize(array, objectStore, newSize);
        return value;
    }

    @Specialization(guards = {
            "isObjectArray(array)", "!isInBounds(array, index)", "!isExtendingByOne(array, index)"
    })
    public Object writeBeyondObject(DynamicObject array, int index, Object value,
            @Cached("create(getContext())") ArrayEnsureCapacityNode ensureCapacityNode) {
        ensureCapacityNode.executeEnsureCapacity(array, index + 1);
        final Object[] objectStore = ((Object[]) Layouts.ARRAY.getStore(array));
        for (int n = getSize(array); n < index; n++) {
            objectStore[n] = nil();
        }
        objectStore[index] = value;
        setSize(array, index + 1);
        return value;
    }

    // Guards

    protected static boolean isInBounds(DynamicObject array, int index) {
        return index >= 0 && index < getSize(array);
    }

    protected static boolean isExtendingByOne(DynamicObject array, int index) {
        return index == getSize(array);
    }

}
