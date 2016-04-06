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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

import java.util.Arrays;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("otherSize"),
        @NodeChild("other"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendManyNode extends RubyNode {

    public ArrayAppendManyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAppendMany(DynamicObject array, int otherSize, Object other);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = "isEmptyArray(array)")
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, int[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, long[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, double[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public DynamicObject appendManyEmpty(DynamicObject array, int otherSize, Object[] other) {
        Layouts.ARRAY.setStore(array, Arrays.copyOf(other, otherSize));
        Layouts.ARRAY.setSize(array, otherSize);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, int[] other,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, long[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, double[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject appendManySameType(DynamicObject array, int otherSize, Object[] other,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    public void appendManySameTypeGeneric(DynamicObject array, ArrayMirror storeMirror,
                                          int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacity(getContext(), storeMirror.getLength(), newSize));
            otherStoreMirror.copyTo(newStoreMirror, 0, oldSize, otherSize);
            Layouts.ARRAY.setStore(array, newStoreMirror.getArray());
            Layouts.ARRAY.setSize(array, newSize);
        } else {
            otherStoreMirror.copyTo(storeMirror, 0, oldSize, otherSize);
            Layouts.ARRAY.setSize(array, newSize);
        }
    }

    // Append something else into an Object[]

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, int[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, long[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject appendManyBoxIntoObject(DynamicObject array, int otherSize, double[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayReflector.reflect(other), extendProfile);
        return array;
    }

    public void appendManyBoxIntoObjectGeneric(DynamicObject array, int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        final Object[] oldStore = (Object[]) Layouts.ARRAY.getStore(array);

        if (extendProfile.profile(newSize > oldStore.length)) {
            final Object[] newStore = ArrayUtils.grow(oldStore, ArrayUtils.capacity(getContext(), oldStore.length, newSize));
            otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
            Layouts.ARRAY.setStore(array, newStore);
            Layouts.ARRAY.setSize(array, newSize);
        } else {
            otherStoreMirror.copyTo(oldStore, 0, oldSize, otherSize);
            Layouts.ARRAY.setSize(array, newSize);
        }
    }

    // Append forcing a generalization from int[] to long[]

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendManyLongIntoInteger(DynamicObject array, int otherSize, long[] other) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;

        final int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
        long[] newStore = ArrayUtils.longCopyOf(oldStore, newSize);

        System.arraycopy(other, 0, newStore, oldSize, otherSize);

        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendManyGeneralizeIntegerDouble(DynamicObject array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendManyGeneralizeIntegerDouble(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject appendManyGeneralizeLongDouble(DynamicObject array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject appendManyGeneralizeLongDouble(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject appendManyGeneralizeDoubleInteger(DynamicObject array, int otherSize, int[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject appendManyGeneralizeDoubleLong(DynamicObject array, int otherSize, long[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject appendManyGeneralizeDoubleObject(DynamicObject array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)),
                otherSize, ArrayReflector.reflect(other));
        return array;
    }

    public void appendManyGeneralizeGeneric(DynamicObject array, ArrayMirror storeMirror, int otherSize, ArrayMirror otherStoreMirror) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + otherSize;
        Object[] newStore = storeMirror.getBoxedCopy(newSize);
        otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
    }

}
