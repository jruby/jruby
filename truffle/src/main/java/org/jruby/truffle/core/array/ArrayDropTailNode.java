/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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

@NodeChildren({ @NodeChild(value = "array", type = RubyNode.class) })
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDropTailNode extends RubyNode {

    final int index;

    public ArrayDropTailNode(int index) {
        this.index = index;
    }

    @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
    public DynamicObject dropTail(DynamicObject array,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("createBinaryProfile()") ConditionProfile indexLargerThanSize) {
        final int size = strategy.getSize(array);
        if (indexLargerThanSize.profile(index >= size)) {
            return createArray(null, 0);
        } else {
            final int newSize = size - index;
            final Object newStore = strategy.newMirror(array).extractRange(0, newSize).getArray();
            return createArray(newStore, newSize);
        }
    }

}
