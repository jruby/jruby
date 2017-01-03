/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.language.RubyNode;

import static org.jruby.truffle.core.array.ArrayHelpers.setSize;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("other"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendManyNode extends RubyNode {

    public abstract DynamicObject executeAppendMany(DynamicObject array, DynamicObject other);

    // Append of a compatible type

    @Specialization(guards = { "strategy.matches(array)", "otherStrategy.matches(other)",
            "generalized.equals(strategy)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendManySameType(DynamicObject array, DynamicObject other,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("of(other)") ArrayStrategy otherStrategy,
            @Cached("strategy.generalize(otherStrategy)") ArrayStrategy generalized,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = strategy.getSize(array);
        final int otherSize = otherStrategy.getSize(other);
        final int newSize = oldSize + otherSize;
        final ArrayMirror storeMirror = strategy.newMirror(array);
        final ArrayMirror otherStoreMirror = otherStrategy.newMirror(other);

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final int capacity = ArrayUtils.capacity(getContext(), storeMirror.getLength(), newSize);
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(capacity);
            otherStoreMirror.copyTo(newStoreMirror, 0, oldSize, otherSize);
            strategy.setStoreAndSize(array, newStoreMirror.getArray(), newSize);
        } else {
            otherStoreMirror.copyTo(storeMirror, 0, oldSize, otherSize);
            setSize(array, newSize);
        }
        return array;
    }

    // Generalizations

    @Specialization(guards = { "strategy.matches(array)", "otherStrategy.matches(other)",
            "!generalized.equals(strategy)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendManyGeneralize(DynamicObject array, DynamicObject other,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("of(other)") ArrayStrategy otherStrategy,
            @Cached("strategy.generalize(otherStrategy)") ArrayStrategy generalized,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = strategy.getSize(array);
        final int otherSize = otherStrategy.getSize(other);
        final int newSize = oldSize + otherSize;
        final ArrayMirror newStoreMirror = generalized.newArray(newSize);
        strategy.newMirror(array).copyTo(newStoreMirror, 0, 0, oldSize);
        otherStrategy.newMirror(other).copyTo(newStoreMirror, 0, oldSize, otherSize);
        generalized.setStoreAndSize(array, newStoreMirror.getArray(), newSize);
        return array;
    }

}
