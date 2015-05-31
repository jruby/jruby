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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("value"),
})
@ImportStatic(ArrayGuards.class)
public abstract class AppendOneNode extends RubyNode {

    public AppendOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyArray executeAppendOne(RubyArray array, Object value);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = "isEmptyArray(array)")
    public RubyBasicObject appendOneEmpty(RubyArray array, int value) {
        ArrayNodes.setStore(array, new int[]{value}, 1);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public RubyBasicObject appendOneEmpty(RubyArray array, long value) {
        ArrayNodes.setStore(array, new long[]{value}, 1);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public RubyBasicObject appendOneEmpty(RubyArray array, double value) {
        ArrayNodes.setStore(array, new double[]{value}, 1);
        return array;
    }

    @Specialization(guards = "isEmptyArray(array)")
    public RubyBasicObject appendOneEmpty(RubyArray array, Object value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = "isIntArray(array)")
    public RubyBasicObject appendOneSameType(RubyArray array, int value,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((int[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isLongArray(array)")
    public RubyBasicObject appendOneSameType(RubyArray array, long value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((long[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isDoubleArray(array)")
    public RubyBasicObject appendOneSameType(RubyArray array, double value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((double[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = "isObjectArray(array)")
    public RubyBasicObject appendOneSameType(RubyArray array, Object value,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((Object[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    public void appendOneSameTypeGeneric(RubyArray array, ArrayMirror storeMirror, Object value, ConditionProfile extendProfile) {
        final int oldSize = ArrayNodes.getSize(array);
        final int newSize = oldSize + 1;

        final ArrayMirror newStoreMirror;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        } else {
            newStoreMirror = storeMirror;
        }

        newStoreMirror.set(oldSize, value);
        ArrayNodes.setStore(array, newStoreMirror.getArray(), newSize);
    }

    // Append forcing a generalization from int[] to long[]

    @Specialization(guards = "isIntArray(array)")
    public RubyBasicObject appendOneLongIntoInteger(RubyArray array, long value) {
        final int oldSize = ArrayNodes.getSize(array);
        final int newSize = oldSize + 1;

        final int[] oldStore = (int[]) ArrayNodes.getStore(array);
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));

        newStore[oldSize] = value;
        ArrayNodes.setStore(array, newStore, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = {"isIntArray(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyBasicObject appendOneGeneralizeInteger(RubyArray array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((int[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    @Specialization(guards = {"isLongArray(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyBasicObject appendOneGeneralizeLong(RubyArray array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((long[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    @Specialization(guards = {"isDoubleArray(array)", "!isDouble(value)"})
    public RubyBasicObject appendOneGeneralizeDouble(RubyArray array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((double[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    public void appendOneGeneralizeGeneric(RubyArray array, ArrayMirror storeMirror, Object value) {
        final int oldSize = ArrayNodes.getSize(array);
        final int newSize = oldSize + 1;
        Object[] newStore = storeMirror.getBoxedCopy(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        newStore[oldSize] = value;
        ArrayNodes.setStore(array, newStore, newSize);
    }

}
