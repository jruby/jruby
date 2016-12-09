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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

import static org.jruby.truffle.core.array.ArrayHelpers.setSize;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("value"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendOneNode extends RubyNode {

    public static ArrayAppendOneNode create() {
        return ArrayAppendOneNodeGen.create(null, null);
    }

    public abstract DynamicObject executeAppendOne(DynamicObject array, Object value);

    // Append of the correct type

    @Specialization(guards = { "strategy.matches(array)", "strategy.accepts(value)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendOneSameType(DynamicObject array, Object value,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final ArrayMirror storeMirror = strategy.newMirror(array);
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacityForOneMore(getContext(), storeMirror.getLength()));
            newStoreMirror.set(oldSize, value);
            strategy.setStore(array, newStoreMirror.getArray());
            setSize(array, newSize);
        } else {
            storeMirror.set(oldSize, value);
            setSize(array, newSize);
        }
        return array;
    }

    // Append forcing a generalization

    @Specialization(guards = {
            "strategy.matches(array)", "!strategy.accepts(value)", "valueStrategy.specializesFor(value)",
    }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendOneGeneralize(DynamicObject array, Object value,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("forValue(value)") ArrayStrategy valueStrategy,
            @Cached("strategy.generalize(valueStrategy)") ArrayStrategy generalizedStrategy) {
        final int oldSize = strategy.getSize(array);
        final int newSize = oldSize + 1;
        final ArrayMirror currentMirror = strategy.newMirror(array);
        final int oldCapacity = currentMirror.getLength();
        final int newCapacity = newSize > oldCapacity ? ArrayUtils.capacityForOneMore(getContext(), oldCapacity) : oldCapacity;
        final ArrayMirror storeMirror = generalizedStrategy.newArray(newCapacity);
        currentMirror.copyTo(storeMirror, 0, 0, oldSize);
        storeMirror.set(oldSize, value);
        generalizedStrategy.setStore(array, storeMirror.getArray());
        setSize(array, newSize);
        return array;
    }

}
