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

import static org.jruby.truffle.core.array.ArrayHelpers.getSize;
import static org.jruby.truffle.core.array.ArrayHelpers.setStoreAndSize;
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
        @NodeChild("array"),
        @NodeChild("other"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendManyNode extends RubyNode {

    public ArrayAppendManyNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAppendMany(DynamicObject array, DynamicObject other);

    // Append into an empty array

    // TODO CS 12-May-15 differentiate between null and empty but possibly having enough space

    @Specialization(guards = { "isEmptyArray(array)", "isNullArray(other)" })
    public DynamicObject appendManyNullNull(DynamicObject array, DynamicObject other) {
        return array;
    }

    @Specialization(guards = { "isEmptyArray(array)", "strategy.matches(other)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendManyEmpty(DynamicObject array, DynamicObject other,
            @Cached("of(other)") ArrayStrategy strategy) {
        final int otherSize = getSize(other);
        Object store = strategy.newMirror(other).copyArrayAndMirror(otherSize).getArray();
        setStoreAndSize(array, store, otherSize);
        return array;
    }

    @Specialization(guards = "isEmptyArray(other)")
    public DynamicObject appendManyOtherEmpty(DynamicObject array, DynamicObject other) {
        return array;
    }

    // Append of a compatible type

    @Specialization(guards = { "strategy.matches(array)", "otherStrategy.matches(other)",
            "strategy.canStore(otherStrategy.type())" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendManySameType(DynamicObject array, DynamicObject other,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("of(other)") ArrayStrategy otherStrategy,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = getSize(array);
        final int otherSize = getSize(other);
        final int newSize = oldSize + otherSize;
        final ArrayMirror storeMirror = strategy.newMirror(array);
        final ArrayMirror otherStoreMirror = otherStrategy.newMirror(other);

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final int capacity = ArrayUtils.capacity(getContext(), storeMirror.getLength(), newSize);
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(capacity);
            otherStoreMirror.copyTo(newStoreMirror, 0, oldSize, otherSize);
            setStoreAndSize(array, newStoreMirror.getArray(), newSize);
        } else {
            otherStoreMirror.copyTo(storeMirror, 0, oldSize, otherSize);
            Layouts.ARRAY.setSize(array, newSize);
        }
        return array;
    }

    // Generalizations

    @Specialization(guards = { "strategy.matches(array)", "otherStrategy.matches(other)",
            "!strategy.canStore(otherStrategy.type())" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendManyGeneralize(DynamicObject array, DynamicObject other,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("of(other)") ArrayStrategy otherStrategy,
            @Cached("strategy.generalize(otherStrategy)") ArrayStrategy generalized,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final int oldSize = getSize(array);
        final int otherSize = getSize(other);
        final int newSize = oldSize + otherSize;
        final ArrayMirror newStoreMirror = generalized.newArray(newSize);
        strategy.newMirror(array).copyTo(newStoreMirror, 0, 0, oldSize);
        otherStrategy.newMirror(other).copyTo(newStoreMirror, 0, oldSize, otherSize);
        setStoreAndSize(array, newStoreMirror.getArray(), newSize);
        return array;
    }

}
