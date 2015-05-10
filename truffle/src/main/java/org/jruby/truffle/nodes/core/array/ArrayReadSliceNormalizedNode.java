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
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="length", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayReadSliceNormalizedNode extends RubyNode {

    public ArrayReadSliceNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeReadSlice(VirtualFrame frame, RubyArray array, int index, int length);

    // Index out of bounds or negative length always gives you nil

    @Specialization(
            guards={"!indexInBounds(array, index)"}
    )
    public RubyBasicObject readIndexOutOfBounds(RubyArray array, int index, int length) {
        return nil();
    }

    @Specialization(
            guards={"!lengthPositive(length)"}
    )
    public RubyBasicObject readNegativeLength(RubyArray array, int index, int length) {
        return nil();
    }

    // If these guards pass for a null array you can only get an empty array

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "isNullArray(array)"}
    )
    public RubyArray readNull(RubyArray array, int index, int length) {
        return new RubyArray(array.getLogicalClass(), null, 0);
    }

    // Reading within bounds on an array with actual storage

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isIntArray(array)"}
    )
    public RubyArray readIntInBounds(RubyArray array, int index, int length) {
        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((int[]) array.getStore(), index, index + length), length);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isLongArray(array)"}
    )
    public RubyArray readLongInBounds(RubyArray array, int index, int length) {
        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((long[]) array.getStore(), index, index + length), length);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isDoubleArray(array)"}
    )
    public RubyArray readDoubleInBounds(RubyArray array, int index, int length) {
        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((double[]) array.getStore(), index, index + length), length);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isObjectArray(array)"}
    )
    public RubyArray readObjectInBounds(RubyArray array, int index, int length) {
        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((Object[]) array.getStore(), index, index + length), length);
    }

    // Reading beyond upper bounds on an array with actual storage needs clamping

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isIntArray(array)"}
    )
    public RubyArray readIntOutOfBounds(RubyArray array, int index, int length) {
        final int clampedLength = Math.min(array.getSize(), index + length) - index;

        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((int[]) array.getStore(), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isLongArray(array)"}
    )
    public RubyArray readLongOutOfBounds(RubyArray array, int index, int length) {
        final int clampedLength = Math.min(array.getSize(), index + length) - index;

        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((long[]) array.getStore(), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isDoubleArray(array)"}
    )
    public RubyArray readDoubleOutOfBounds(RubyArray array, int index, int length) {
        final int clampedLength = Math.min(array.getSize(), index + length) - index;

        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((double[]) array.getStore(), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isObjectArray(array)"}
    )
    public RubyArray readObjectOutOfBounds(RubyArray array, int index, int length) {
        final int clampedLength = Math.min(array.getSize(), index + length) - index;

        return new RubyArray(array.getLogicalClass(),
                Arrays.copyOfRange((Object[]) array.getStore(), index, index + clampedLength), clampedLength);
    }

    // Guards

    protected static boolean indexInBounds(RubyArray array, int index) {
        return index >= 0 && index <= array.getSize();
    }

    protected static boolean lengthPositive(int length) {
        return length >= 0;
    }

    protected static boolean endInBounds(RubyArray array, int index, int length) {
        return index + length < array.getSize();
    }

}
