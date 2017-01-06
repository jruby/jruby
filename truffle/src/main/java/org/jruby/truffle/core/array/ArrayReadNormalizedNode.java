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
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value = "array", type = RubyNode.class),
        @NodeChild(value = "index", type = RubyNode.class)
})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayReadNormalizedNode extends RubyNode {

    public abstract Object executeRead(DynamicObject array, int index);

    // Read within the bounds of an array with actual storage

    @Specialization(guards = {"strategy.matches(array)", "isInBounds(array, index, strategy)"}, limit = "ARRAY_STRATEGIES")
    public Object readInBounds(DynamicObject array, int index,
            @Cached("of(array)") ArrayStrategy strategy) {
        return strategy.newMirror(array).get(index);
    }

    // Reading out of bounds is nil for any array

    @Specialization(guards = {"strategy.matches(array)", "!isInBounds(array, index, strategy)"}, limit = "ARRAY_STRATEGIES")
    public DynamicObject readOutOfBounds(DynamicObject array, int index,
                    @Cached("of(array)") ArrayStrategy strategy) {
        return nil();
    }

    // Guards

    protected static boolean isInBounds(DynamicObject array, int index, ArrayStrategy strategy) {
        return index >= 0 && index < strategy.getSize(array);
    }

}
