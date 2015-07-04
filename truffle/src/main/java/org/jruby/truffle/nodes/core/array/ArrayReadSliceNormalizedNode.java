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

    public abstract Object executeReadSlice(VirtualFrame frame, RubyBasicObject array, int index, int length);

    // Index out of bounds or negative length always gives you nil

    @Specialization(
            guards={"isRubyArray(array)", "!indexInBounds(array, index)"}
    )
    public RubyBasicObject readIndexOutOfBounds(RubyBasicObject array, int index, int length) {
        return nil();
    }

    @Specialization(
            guards={"isRubyArray(array)", "!lengthPositive(length)"}
    )
    public RubyBasicObject readNegativeLength(RubyBasicObject array, int index, int length) {
        return nil();
    }

    // If these guards pass for a null array you can only get an empty array

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "isNullArray(array)"}
    )
    public RubyBasicObject readNull(RubyBasicObject array, int index, int length) {
        return ArrayNodes.createEmptyArray(array.getLogicalClass());
    }

    // Reading within bounds on an array with actual storage

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isIntArray(array)"}
    )
    public RubyBasicObject readIntInBounds(RubyBasicObject array, int index, int length) {
        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((int[]) ArrayNodes.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isLongArray(array)"}
    )
    public RubyBasicObject readLongInBounds(RubyBasicObject array, int index, int length) {
        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((long[]) ArrayNodes.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isDoubleArray(array)"}
    )
    public RubyBasicObject readDoubleInBounds(RubyBasicObject array, int index, int length) {
        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((double[]) ArrayNodes.getStore(array), index, index + length), length);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)", "isObjectArray(array)"}
    )
    public RubyBasicObject readObjectInBounds(RubyBasicObject array, int index, int length) {
        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((Object[]) ArrayNodes.getStore(array), index, index + length), length);
    }

    // Reading beyond upper bounds on an array with actual storage needs clamping

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isIntArray(array)"}
    )
    public RubyBasicObject readIntOutOfBounds(RubyBasicObject array, int index, int length) {
        final int clampedLength = Math.min(ArrayNodes.getSize(array), index + length) - index;

        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((int[]) ArrayNodes.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isLongArray(array)"}
    )
    public RubyBasicObject readLongOutOfBounds(RubyBasicObject array, int index, int length) {
        final int clampedLength = Math.min(ArrayNodes.getSize(array), index + length) - index;

        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((long[]) ArrayNodes.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isDoubleArray(array)"}
    )
    public RubyBasicObject readDoubleOutOfBounds(RubyBasicObject array, int index, int length) {
        final int clampedLength = Math.min(ArrayNodes.getSize(array), index + length) - index;

        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((double[]) ArrayNodes.getStore(array), index, index + clampedLength), clampedLength);
    }

    @Specialization(
            guards={"isRubyArray(array)", "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)", "isObjectArray(array)"}
    )
    public RubyBasicObject readObjectOutOfBounds(RubyBasicObject array, int index, int length) {
        final int clampedLength = Math.min(ArrayNodes.getSize(array), index + length) - index;

        return ArrayNodes.createArray(array.getLogicalClass(),
                Arrays.copyOfRange((Object[]) ArrayNodes.getStore(array), index, index + clampedLength), clampedLength);
    }

    // Guards

    protected static boolean indexInBounds(RubyBasicObject array, int index) {
        return index >= 0 && index <= ArrayNodes.getSize(array);
    }

    protected static boolean lengthPositive(int length) {
        return length >= 0;
    }

    protected static boolean endInBounds(RubyBasicObject array, int index, int length) {
        return index + length < ArrayNodes.getSize(array);
    }

}
