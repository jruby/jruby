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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="requiredCapacity", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class EnsureCapacityArrayNode extends RubyNode {

    private final ConditionProfile allocateProfile = ConditionProfile.createCountingProfile();

    public EnsureCapacityArrayNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeEnsureCapacity(VirtualFrame frame, DynamicObject array, int requiredCapacity);

    @Specialization(
            guards={"isRubyArray(array)", "isIntArray(array)"}
    )
    public boolean ensureCapacityInt(DynamicObject array, int requiredCapacity) {
        final int[] store = (int[]) ArrayNodes.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            ArrayNodes.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), ArrayNodes.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isRubyArray(array)", "isLongArray(array)"}
    )
    public boolean ensureCapacityLong(DynamicObject array, int requiredCapacity) {
        final long[] store = (long[]) ArrayNodes.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            ArrayNodes.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), ArrayNodes.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isRubyArray(array)", "isDoubleArray(array)"}
    )
    public boolean ensureCapacityDouble(DynamicObject array, int requiredCapacity) {
        final double[] store = (double[]) ArrayNodes.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            ArrayNodes.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), ArrayNodes.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isRubyArray(array)", "isObjectArray(array)"}
    )
    public boolean ensureCapacityObject(DynamicObject array, int requiredCapacity) {
        final Object[] store = (Object[]) ArrayNodes.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            ArrayNodes.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), ArrayNodes.getSize(array));
            return true;
        } else {
            return false;
        }
    }

}
