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

    public abstract RubyBasicObject executeAppendOne(RubyBasicObject array, Object value);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public RubyBasicObject appendOneEmpty(RubyBasicObject array, int value) {
        ArrayNodes.setStore(array, new int[]{value}, 1);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public RubyBasicObject appendOneEmpty(RubyBasicObject array, long value) {
        ArrayNodes.setStore(array, new long[]{value}, 1);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public RubyBasicObject appendOneEmpty(RubyBasicObject array, double value) {
        ArrayNodes.setStore(array, new double[]{value}, 1);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public RubyBasicObject appendOneEmpty(RubyBasicObject array, Object value) {
        ArrayNodes.setStore(array, new Object[]{value}, 1);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public RubyBasicObject appendOneSameType(RubyBasicObject array, int value,
                                   @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((int[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public RubyBasicObject appendOneSameType(RubyBasicObject array, long value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((long[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public RubyBasicObject appendOneSameType(RubyBasicObject array, double value,
                                @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((double[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public RubyBasicObject appendOneSameType(RubyBasicObject array, Object value,
                                  @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        appendOneSameTypeGeneric(array, ArrayMirror.reflect((Object[]) ArrayNodes.getStore(array)), value, extendProfile);
        return array;
    }

    public void appendOneSameTypeGeneric(RubyBasicObject array, ArrayMirror storeMirror, Object value, ConditionProfile extendProfile) {
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

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public RubyBasicObject appendOneLongIntoInteger(RubyBasicObject array, long value) {
        final int oldSize = ArrayNodes.getSize(array);
        final int newSize = oldSize + 1;

        final int[] oldStore = (int[]) ArrayNodes.getStore(array);
        long[] newStore = ArrayUtils.longCopyOf(oldStore, ArrayUtils.capacity(oldStore.length, newSize));

        newStore[oldSize] = value;
        ArrayNodes.setStore(array, newStore, newSize);
        return array;
    }

    // Append forcing a generalization to Object[]

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyBasicObject appendOneGeneralizeInteger(RubyBasicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((int[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)", "!isInteger(value)", "!isLong(value)"})
    public RubyBasicObject appendOneGeneralizeLong(RubyBasicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((long[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)", "!isDouble(value)"})
    public RubyBasicObject appendOneGeneralizeDouble(RubyBasicObject array, Object value) {
        appendOneGeneralizeGeneric(array, ArrayMirror.reflect((double[]) ArrayNodes.getStore(array)), value);
        return array;
    }

    public void appendOneGeneralizeGeneric(RubyBasicObject array, ArrayMirror storeMirror, Object value) {
        final int oldSize = ArrayNodes.getSize(array);
        final int newSize = oldSize + 1;
        Object[] newStore = storeMirror.getBoxedCopy(ArrayUtils.capacity(storeMirror.getLength(), newSize));
        newStore[oldSize] = value;
        ArrayNodes.setStore(array, newStore, newSize);
    }

}
