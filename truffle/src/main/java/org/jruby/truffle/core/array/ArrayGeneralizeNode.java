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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value = "array", type = RubyNode.class),
        @NodeChild(value = "requiredCapacity", type = RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayGeneralizeNode extends RubyNode {

    public ArrayGeneralizeNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static ArrayGeneralizeNode create(RubyContext context) {
        return ArrayGeneralizeNodeGen.create(context, null, null, null);
    }

    public abstract Object[] executeGeneralize(DynamicObject array, int requiredCapacity);

    @Specialization(guards = "isNullArray(array)")
    public Object[] generalizeNull(DynamicObject array, int requiredCapacity) {
        Object[] store = new Object[requiredCapacity];
        Layouts.ARRAY.setStore(array, store);
        return store;
    }

    @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
    public Object[] generalize(DynamicObject array, int requiredCapacity,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("createCountingProfile()") ConditionProfile extendProfile) {
        assert !ArrayGuards.isObjectArray(array);
        final ArrayMirror mirror = strategy.newMirror(array);
        final int capacity;
        if (extendProfile.profile(mirror.getLength() < requiredCapacity)) {
            capacity = ArrayUtils.capacity(getContext(), mirror.getLength(), requiredCapacity);
        } else {
            capacity = mirror.getLength();
        }
        final Object[] store = mirror.getBoxedCopy(capacity);
        Layouts.ARRAY.setStore(array, store);
        return store;
    }

}
