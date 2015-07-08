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
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="requiredCapacity", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class GeneralizeArrayNode extends RubyNode {

    public GeneralizeArrayNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeGeneralize(VirtualFrame frame, RubyBasicObject array, int requiredCapacity);

    // TODO CS 9-Feb-15 should use ArrayUtils.capacity?

    @Specialization(
            guards={"isRubyArray(array)", "isNullArray(array)"}
    )
    public RubyBasicObject generalizeNull(RubyBasicObject array, int requiredCapacity) {
        ArrayNodes.setStore(array, new Object[requiredCapacity], ArrayNodes.getSize(array));
        return array;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)"}
    )
    public RubyBasicObject generalizeInt(RubyBasicObject array, int requiredCapacity) {
        final int[] intStore = (int[]) ArrayNodes.getStore(array);
        ArrayNodes.setStore(array, ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), ArrayNodes.getSize(array));
        return array;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)"}
    )
    public RubyBasicObject generalizeLong(RubyBasicObject array, int requiredCapacity) {
        final long[] intStore = (long[]) ArrayNodes.getStore(array);
        ArrayNodes.setStore(array, ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), ArrayNodes.getSize(array));
        return array;
    }

    @Specialization(
            guards={"isRubyArray(array)", "isDoubleArray(array)"}
    )
    public RubyBasicObject generalizeDouble(RubyBasicObject array, int requiredCapacity) {
        final double[] intStore = (double[]) ArrayNodes.getStore(array);
        ArrayNodes.setStore(array, ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), ArrayNodes.getSize(array));
        return array;
    }

}
