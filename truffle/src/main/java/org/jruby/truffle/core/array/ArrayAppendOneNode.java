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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild("array"),
        @NodeChild("value"),
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayAppendOneNode extends RubyNode {

    public ArrayAppendOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeAppendOne(DynamicObject array, Object value);

    // Append into an empty array

    @Specialization(guards = { "isNullArray(array)", "strategy.specializesFor(value)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendOneEmpty(DynamicObject array, Object value,
            @Cached("forValue(value)") ArrayStrategy strategy) {
        final ArrayMirror storeMirror = strategy.newArray(1);
        storeMirror.set(0, value);
        Layouts.ARRAY.setStore(array, storeMirror.getArray());
        Layouts.ARRAY.setSize(array, 1);
        return array;
    }

    // Append of the correct type

    @Specialization(guards = { "strategy.matches(array)", "strategy.accepts(value)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendOneSameType(DynamicObject array, Object value,
            @Cached("of(array, value)") ArrayStrategy strategy,
            @Cached("createBinaryProfile()") ConditionProfile extendProfile) {
        final ArrayMirror storeMirror = strategy.newMirror(array);
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;

        if (extendProfile.profile(newSize > storeMirror.getLength())) {
            final ArrayMirror newStoreMirror = storeMirror.copyArrayAndMirror(ArrayUtils.capacityForOneMore(getContext(), storeMirror.getLength()));
            newStoreMirror.set(oldSize, value);
            Layouts.ARRAY.setStore(array, newStoreMirror.getArray());
            Layouts.ARRAY.setSize(array, newSize);
        } else {
            storeMirror.set(oldSize, value);
            Layouts.ARRAY.setSize(array, newSize);
        }
        return array;
    }

    // Append forcing a generalization

    @Specialization(guards = {
            "currentStrategy.matches(array)", "!currentStrategy.accepts(value)", "generalizedStrategy.accepts(value)",
    }, limit = "ARRAY_STRATEGIES")
    public DynamicObject appendOneGeneralize(DynamicObject array, Object value,
            @Cached("of(array, value)") ArrayStrategy currentStrategy,
            @Cached("currentStrategy.generalizeFor(value)") ArrayStrategy generalizedStrategy) {
        final int oldSize = Layouts.ARRAY.getSize(array);
        final int newSize = oldSize + 1;
        final ArrayMirror currentMirror = currentStrategy.newMirror(array);
        final int oldCapacity = currentMirror.getLength();
        final int newCapacity = newSize > oldCapacity ? ArrayUtils.capacityForOneMore(getContext(), oldCapacity) : oldCapacity;
        final ArrayMirror storeMirror = generalizedStrategy.newArray(newCapacity);
        currentMirror.copyTo(storeMirror, 0, 0, oldSize);
        storeMirror.set(oldSize, value);
        Layouts.ARRAY.setStore(array, storeMirror.getArray());
        Layouts.ARRAY.setSize(array, newSize);
        return array;
    }

}
