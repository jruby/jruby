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

import java.util.Arrays;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("otherSize"),
        @NodeChild("other"),
})
@ImportStatic(ArrayGuards.class)
public abstract class AppendManyNode extends RubyNode {

    public AppendManyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyArray executeAppendMany(RubyArray array, int otherSize, Object other);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = "isEmpty(array)")
    public RubyArray appendManyEmpty(RubyArray array, int otherSize, int[] other) {
        array.setStore(Arrays.copyOf(other, otherSize), otherSize);
        return array;
    }

    @Specialization(guards = "isEmpty(array)")
    public RubyArray appendManyEmpty(RubyArray array, int otherSize, long[] other) {
        array.setStore(Arrays.copyOf(other, otherSize), otherSize);
        return array;
    }

    @Specialization(guards = "isEmpty(array)")
    public RubyArray appendManyEmpty(RubyArray array, int otherSize, double[] other) {
        array.setStore(Arrays.copyOf(other, otherSize), otherSize);
        return array;
    }

    @Specialization(guards = "isEmpty(array)")
    public RubyArray appendManyEmpty(RubyArray array, int otherSize, Object[] other) {
        array.setStore(Arrays.copyOf(other, otherSize), otherSize);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendManySameType(RubyArray array, int otherSize, int[] other,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayMirror.reflect((int[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray appendManySameType(RubyArray array, int otherSize, long[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayMirror.reflect((long[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray appendManySameType(RubyArray array, int otherSize, double[] other,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayMirror.reflect((double[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray appendManySameType(RubyArray array, int otherSize, Object[] other,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManySameTypeGeneric(array, ArrayMirror.reflect((Object[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    public void appendManySameTypeGeneric(RubyArray array, ArrayMirror storeMirror,
                                          int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + otherSize;

        final ArrayMirror newStoreMirror;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        } else {
            newStoreMirror = storeMirror;
        }

        otherStoreMirror.copyTo(newStoreMirror, 0, oldSize, otherSize);
        array.setStore(newStoreMirror.getArray(), newSize);
    }

    // Append something else into an Object[]

    @Specialization(guards = "isObject(array)")
    public RubyArray appendManyBoxIntoObject(RubyArray array, int otherSize, int[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray appendManyBoxIntoObject(RubyArray array, int otherSize, long[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray appendManyBoxIntoObject(RubyArray array, int otherSize, double[] other,
                                        @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendManyBoxIntoObjectGeneric(array, otherSize, ArrayMirror.reflect(other), extendProfile);
        return array;
    }

    public void appendManyBoxIntoObjectGeneric(RubyArray array, int otherSize, ArrayMirror otherStoreMirror,
                                          ConditionProfile extendProfile) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + otherSize;

        final Object[] oldStore = (Object[]) array.getStore();
        final Object[] newStore;

        if (extendProfile.profile(newSize > oldStore.length)) {
            newStore = ArrayUtils.copyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));
        } else {
            newStore = oldStore;
        }

        otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
        array.setStore(newStore, newSize);
    }

    // Append forcing a generalization from int[] to long[]

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendManyLongIntoInteger(RubyArray array, int otherSize, long[] other) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + otherSize;

        final int[] oldStore = (int[]) array.getStore();
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));

        System.arraycopy(other, 0, newStore, oldSize, otherSize);

        array.setStore(newStore, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendManyGeneralizeIntegerDouble(RubyArray array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((int[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray appendManyGeneralizeIntegerDouble(RubyArray array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((int[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray appendManyGeneralizeLongDouble(RubyArray array, int otherSize, double[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((long[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray appendManyGeneralizeLongDouble(RubyArray array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((long[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray appendManyGeneralizeDoubleInteger(RubyArray array, int otherSize, int[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((double[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray appendManyGeneralizeDoubleLong(RubyArray array, int otherSize, long[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((double[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray appendManyGeneralizeDoubleObject(RubyArray array, int otherSize, Object[] other) {
        appendManyGeneralizeGeneric(array, ArrayMirror.reflect((double[]) array.getStore()),
                otherSize, ArrayMirror.reflect(other));
        return array;
    }

    public void appendManyGeneralizeGeneric(RubyArray array, ArrayMirror storeMirror, int otherSize, ArrayMirror otherStoreMirror) {
        final int oldSize = array.getSize();
        final int newSize = oldSize + otherSize;
        Object[] newStore = storeMirror.getBoxedCopy(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        otherStoreMirror.copyTo(newStore, 0, oldSize, otherSize);
        array.setStore(newStore, newSize);
    }

}
