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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

import static org.jruby.truffle.core.array.ArrayHelpers.setSize;

@NodeChildren({
        @NodeChild("array")
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayPopOneNode extends RubyNode {

    public abstract Object executePopOne(DynamicObject array);

    // Pop from an empty array

    @Specialization(guards = "isEmptyArray(array)")
    public DynamicObject popOneEmpty(DynamicObject array) {
        return nil();
    }

    // Pop from a non-empty array

    @Specialization(guards = { "strategy.matches(array)", "!isEmptyArray(array)" }, limit = "ARRAY_STRATEGIES")
    public Object popOne(DynamicObject array,
            @Cached("of(array)") ArrayStrategy strategy) {
        final int size = Layouts.ARRAY.getSize(array);
        final Object value = strategy.newMirror(array).get(size - 1);
        setSize(array, size - 1);
        return value;
    }

}
