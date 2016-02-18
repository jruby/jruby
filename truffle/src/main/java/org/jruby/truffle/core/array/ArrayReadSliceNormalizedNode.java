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
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="length", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayReadSliceNormalizedNode extends RubyNode {

    @Child private AllocateObjectNode allocateObjectNode;

    public ArrayReadSliceNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
    }

    public abstract DynamicObject executeReadSlice(DynamicObject array, int index, int length);

    // Index out of bounds or negative length always gives you nil

    @Specialization(
            guards = "!indexInBounds(array, index)"
    )
    public DynamicObject readIndexOutOfBounds(DynamicObject array, int index, int length) {
        return nil();
    }

    @Specialization(
            guards = "!lengthPositive(length)"
    )
    public DynamicObject readNegativeLength(DynamicObject array, int index, int length) {
        return nil();
    }

    // If these guards pass for a null array you can only get an empty array

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "isNullArray(array)" }
    )
    public DynamicObject readNull(DynamicObject array, int index, int length) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), null, 0);
    }

    // Reading within bounds on an array with actual storage

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isIntArray(array)" }
    )
    public DynamicObject readIntInBounds(DynamicObject array, int index, int length) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((int[]) Layouts.ARRAY.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isLongArray(array)" }
    )
    public DynamicObject readLongInBounds(DynamicObject array, int index, int length) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((long[]) Layouts.ARRAY.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isDoubleArray(array)" }
    )
    public DynamicObject readDoubleInBounds(DynamicObject array, int index, int length) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((double[]) Layouts.ARRAY.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isObjectArray(array)" }
    )
    public DynamicObject readObjectInBounds(DynamicObject array, int index, int length) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((Object[]) Layouts.ARRAY.getStore(array), index, index + length), length);
    }

    // Reading beyond upper bounds on an array with actual storage needs clamping

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isIntArray(array)" }
    )
    public DynamicObject readIntOutOfBounds(DynamicObject array, int index, int length) {
        final int clampedLength = Math.min(Layouts.ARRAY.getSize(array), index + length) - index;

        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((int[]) Layouts.ARRAY.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isLongArray(array)" }
    )
    public DynamicObject readLongOutOfBounds(DynamicObject array, int index, int length) {
        final int clampedLength = Math.min(Layouts.ARRAY.getSize(array), index + length) - index;

        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((long[]) Layouts.ARRAY.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isDoubleArray(array)" }
    )
    public DynamicObject readDoubleOutOfBounds(DynamicObject array, int index, int length) {
        final int clampedLength = Math.min(Layouts.ARRAY.getSize(array), index + length) - index;

        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((double[]) Layouts.ARRAY.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isObjectArray(array)" }
    )
    public DynamicObject readObjectOutOfBounds(DynamicObject array, int index, int length) {
        final int clampedLength = Math.min(Layouts.ARRAY.getSize(array), index + length) - index;

        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), Arrays.copyOfRange((Object[]) Layouts.ARRAY.getStore(array), index, index + clampedLength), clampedLength);
    }

    // Guards

    protected static boolean indexInBounds(DynamicObject array, int index) {
        return index >= 0 && index <= Layouts.ARRAY.getSize(array);
    }

    protected static boolean lengthPositive(int length) {
        return length >= 0;
    }

    protected static boolean endInBounds(DynamicObject array, int index, int length) {
        return index + length < Layouts.ARRAY.getSize(array);
    }

}
