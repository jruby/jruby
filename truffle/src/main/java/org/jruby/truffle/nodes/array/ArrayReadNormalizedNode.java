/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.ArrayGuards;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyNilClass;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayReadNormalizedNode extends RubyNode {

    public ArrayReadNormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public ArrayReadNormalizedNode(ArrayReadNormalizedNode prev) {
        super(prev);
    }

    public abstract Object executeRead(VirtualFrame frame, RubyArray array, int index);

    // Anything from a null array is nil

    @Specialization(
            guards="isNullArray(array)"
    )
    public RubyNilClass readNull(RubyArray array, int index) {
        return nil();
    }

    // Read within the bounds of an array with actual storage

    @Specialization(
            guards={"isInBounds(array, index)", "isIntArray(array)"}
    )
    public int readIntInBounds(RubyArray array, int index) {
        return ((int[]) array.getStore())[index];
    }

    @Specialization(
            guards={"isInBounds(array, index)", "isLongArray(array)"}
    )
    public long readLongInBounds(RubyArray array, int index) {
        return ((long[]) array.getStore())[index];
    }

    @Specialization(
            guards={"isInBounds(array, index)", "isDoubleArray(array)"}
    )
    public double readDoubleInBounds(RubyArray array, int index) {
        return ((double[]) array.getStore())[index];
    }

    @Specialization(
            guards={"isInBounds(array, index)", "isObjectArray(array)"}
    )
    public Object readObjectInBounds(RubyArray array, int index) {
        return ((Object[]) array.getStore())[index];
    }

    // Reading out of bounds is nil of any array is nil - cannot contain isNullArray

    @Specialization(
            guards="!isInBounds(array, index)"
    )
    public RubyNilClass readOutOfBounds(RubyArray array, int index) {
        return nil();
    }

    // Guards

    protected static boolean isInBounds(RubyArray array, int index) {
        return index >= 0 && index < array.getSize();
    }

}
