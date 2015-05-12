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
import org.jruby.truffle.runtime.array.ArrayUtils;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="requiredCapacity", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class GeneralizeArrayNode extends RubyNode {

    public GeneralizeArrayNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeGeneralize(VirtualFrame frame, RubyArray array, int requiredCapacity);

    // TODO CS 9-Feb-15 should use ArrayUtils.capacity?

    @Specialization(
            guards={"isNullArray(array)"}
    )
    public RubyArray generalizeNull(RubyArray array, int requiredCapacity) {
        array.setStore(new Object[requiredCapacity], array.getSize());
        return array;
    }

    @Specialization(
            guards={"isIntArray(array)"}
    )
    public RubyArray generalizeInt(RubyArray array, int requiredCapacity) {
        final int[] intStore = (int[]) array.getStore();
        array.setStore(ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), array.getSize());
        return array;
    }

    @Specialization(
            guards={"isLongArray(array)"}
    )
    public RubyArray generalizeLong(RubyArray array, int requiredCapacity) {
        final long[] intStore = (long[]) array.getStore();
        array.setStore(ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), array.getSize());
        return array;
    }

    @Specialization(
            guards={"isDoubleArray(array)"}
    )
    public RubyArray generalizeDouble(RubyArray array, int requiredCapacity) {
        final double[] intStore = (double[]) array.getStore();
        array.setStore(ArrayUtils.boxExtra(intStore, requiredCapacity - intStore.length), array.getSize());
        return array;
    }

}
