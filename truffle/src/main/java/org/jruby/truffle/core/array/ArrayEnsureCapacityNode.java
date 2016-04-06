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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

import java.util.Arrays;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="requiredCapacity", type=RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayEnsureCapacityNode extends RubyNode {

    private final ConditionProfile allocateProfile = ConditionProfile.createCountingProfile();

    public ArrayEnsureCapacityNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeEnsureCapacity(DynamicObject array, int requiredCapacity);

    @Specialization(guards = "isIntArray(array)")
    public boolean ensureCapacityInt(DynamicObject array, int requiredCapacity) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            Layouts.ARRAY.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, requiredCapacity)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(guards = "isLongArray(array)")
    public boolean ensureCapacityLong(DynamicObject array, int requiredCapacity) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            Layouts.ARRAY.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, requiredCapacity)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(guards = "isDoubleArray(array)")
    public boolean ensureCapacityDouble(DynamicObject array, int requiredCapacity) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            Layouts.ARRAY.setStore(array, Arrays.copyOf(store, ArrayUtils.capacity(getContext(), store.length, requiredCapacity)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            return true;
        } else {
            return false;
        }
    }

    @Specialization(guards = "isObjectArray(array)")
    public boolean ensureCapacityObject(DynamicObject array, int requiredCapacity) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);

        if (allocateProfile.profile(store.length < requiredCapacity)) {
            Layouts.ARRAY.setStore(array, ArrayUtils.grow(store, ArrayUtils.capacity(getContext(), store.length, requiredCapacity)));
            Layouts.ARRAY.setSize(array, Layouts.ARRAY.getSize(array));
            return true;
        } else {
            return false;
        }
    }

}
