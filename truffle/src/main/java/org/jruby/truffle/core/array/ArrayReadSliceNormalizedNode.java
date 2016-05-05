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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;

@NodeChildren({
        @NodeChild(value = "array", type = RubyNode.class),
        @NodeChild(value = "index", type = RubyNode.class),
        @NodeChild(value = "length", type = RubyNode.class)
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

    @Specialization(guards = "!indexInBounds(array, index)")
    public DynamicObject readIndexOutOfBounds(DynamicObject array, int index, int length) {
        return nil();
    }

    @Specialization(guards = "!lengthPositive(length)")
    public DynamicObject readNegativeLength(DynamicObject array, int index, int length) {
        return nil();
    }

    // If these guards pass for a null array you can only get an empty array

    @Specialization(
            guards = { "indexInBounds(array, index)", "lengthPositive(length)", "isNullArray(array)" }
    )
    public DynamicObject readNull(DynamicObject array, int index, int length) {
        return createArrayOfSameClass(array, null, 0);
    }

    // Reading within bounds on an array with actual storage

    @Specialization(guards = {
            "indexInBounds(array, index)", "lengthPositive(length)", "endInBounds(array, index, length)",
            "strategy.matches(array)"
    }, limit = "ARRAY_STRATEGIES")
    public DynamicObject readInBounds(DynamicObject array, int index, int length,
            @Cached("of(array)") ArrayStrategy strategy) {
        final Object store = strategy.newMirror(array).extractRange(index, index + length).getArray();
        return createArrayOfSameClass(array, store, length);
    }

    // Reading beyond upper bounds on an array with actual storage needs clamping

    @Specialization(guards = {
            "indexInBounds(array, index)", "lengthPositive(length)", "!endInBounds(array, index, length)",
            "strategy.matches(array)"
    }, limit = "ARRAY_STRATEGIES")
    public DynamicObject readOutOfBounds(DynamicObject array, int index, int length,
            @Cached("of(array)") ArrayStrategy strategy) {
        final int end = getSize(array);
        final Object store = strategy.newMirror(array).extractRange(index, end).getArray();
        return createArrayOfSameClass(array, store, end - index);
    }

    // Guards

    protected DynamicObject createArrayOfSameClass(DynamicObject array, Object store, int size) {
        return allocateObjectNode.allocate(Layouts.BASIC_OBJECT.getLogicalClass(array), store, size);
    }

    protected static boolean indexInBounds(DynamicObject array, int index) {
        return index >= 0 && index <= getSize(array);
    }

    protected static boolean lengthPositive(int length) {
        return length >= 0;
    }

    protected static boolean endInBounds(DynamicObject array, int index, int length) {
        return index + length <= getSize(array);
    }

}
