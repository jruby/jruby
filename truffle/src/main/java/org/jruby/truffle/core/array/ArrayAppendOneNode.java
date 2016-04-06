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

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("value"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendOneNode extends RubyNode {

    public ArrayAppendOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAppendOne(DynamicObject array, Object value);

    // Append into an empty array

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject appendOneEmpty(DynamicObject array, int value) {
        Layouts.ARRAY.setStore(array, new int[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return array;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject appendOneEmpty(DynamicObject array, long value) {
        Layouts.ARRAY.setStore(array, new long[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return array;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject appendOneEmpty(DynamicObject array, double value) {
        Layouts.ARRAY.setStore(array, new double[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return array;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject appendOneEmpty(DynamicObject array, Object value) {
        Layouts.ARRAY.setStore(array, new Object[]{value});
        Layouts.ARRAY.setSize(array, 1);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendOneSameType(DynamicObject array, int value,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject appendOneSameType(DynamicObject array, long value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject appendOneSameType(DynamicObject array, double value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject appendOneSameType(DynamicObject array, Object value,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayReflector.reflect((Object[]) Layouts.ARRAY.getStore(array)), value, extendProfile);
        return array;
    }

    public void appendOneSameTypeGeneric(DynamicObject array, ArrayMirror storeMirror, Object value, ConditionProfile extendProfile) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacityForOneMore(getContext(), storeMirror.getLength()));
            newStoreMirror.set(oldSize, value);
            Layouts.ARRAY.setStore(array, newStoreMirror.getArray());
            Layouts.ARRAY.setSize(array, newSize);
        } else {
            storeMirror.set(oldSize, value);
            Layouts.ARRAY.setSize(array, newSize);
        }
    }

    // Append forcing a generalization from int[] to long[]

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject appendOneLongIntoInteger(DynamicObject array, long value) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;

        final int[] oldStore = (int[]) Layouts.ARRAY.getStore(array);
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(getContext(), oldStore.length, newSize));

        newStore[oldSize] = value;
        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = { "isIntArray(array)", "!isInteger(value)", "!isLong(value)" })
    public DynamicObject appendOneGeneralizeInteger(DynamicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayReflector.reflect((int[]) Layouts.ARRAY.getStore(array)), value);
        return array;
    }

    @Specialization(guards = { "isLongArray(array)", "!isInteger(value)", "!isLong(value)" })
    public DynamicObject appendOneGeneralizeLong(DynamicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayReflector.reflect((long[]) Layouts.ARRAY.getStore(array)), value);
        return array;
    }

    @Specialization(guards = { "isDoubleArray(array)", "!isDouble(value)" })
    public DynamicObject appendOneGeneralizeDouble(DynamicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayReflector.reflect((double[]) Layouts.ARRAY.getStore(array)), value);
        return array;
    }

    public void appendOneGeneralizeGeneric(DynamicObject array, ArrayMirror storeMirror, Object value) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;
        final int oldCapacity = storeMirror.getLength();
        final int newCapacity = newSize > oldCapacity ? ArrayUtils.capacityForOneMore(getContext(), storeMirror.getLength()) : oldCapacity;
        Object[] newStore = storeMirror.getBoxedCopy(newCapacity);
        newStore[oldSize] = value;
        Layouts.ARRAY.setStore(array, newStore);
        Layouts.ARRAY.setSize(array, newSize);
    }

}
