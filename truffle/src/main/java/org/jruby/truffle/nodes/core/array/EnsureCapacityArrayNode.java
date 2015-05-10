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
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.ArrayUtils;

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

    public abstract Object executeEnsureCapacity(VirtualFrame frame, RubyArray array, int requiredCapacity);

    @Specialization(
            guards={"isIntArray(array)"}
    )
    public boolean ensureCapacityInt(RubyArray array, int requiredCapacity) {
        final int[] store = (int[]) array.getStore();

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            array.setStore(Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), array.getSize());
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isLongArray(array)"}
    )
    public boolean ensureCapacityLong(RubyArray array, int requiredCapacity) {
        final long[] store = (long[]) array.getStore();

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            array.setStore(Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), array.getSize());
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isDoubleArray(array)"}
    )
    public boolean ensureCapacityDouble(RubyArray array, int requiredCapacity) {
        final double[] store = (double[]) array.getStore();

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            array.setStore(Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), array.getSize());
            return true;
        } else {
            return false;
        }
    }

    @Specialization(
            guards={"isObjectArray(array)"}
    )
    public boolean ensureCapacityObject(RubyArray array, int requiredCapacity) {
        final Object[] store = (Object[]) array.getStore();

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            array.setStore(Arrays.copyOf(store, ArrayUtils.capacity(store.length, requiredCapacity)), array.getSize());
            return true;
        } else {
            return false;
        }
    }

}
