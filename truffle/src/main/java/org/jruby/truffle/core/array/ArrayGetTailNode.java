/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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

import static org.jruby.truffle.core.array.ArrayHelpers.createArray;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject getTailNull(DynamicObject array) {
        return createArray(getContext(), null, 0);
    }

    @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
    public DynamicObject getTail(DynamicObject array,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("createBinaryProfile()") ConditionProfile indexLargerThanSize) {
        final int size = Layouts.ARRAY.getSize(array);
        if (indexLargerThanSize.profile(index >= size)) {
            return createArray(getContext(), null, 0);
        } else {
            final Object newStore = strategy.newMirror(array).extractRange(index, size).getArray();
            return createArray(getContext(), newStore, size - index);
        }
    }

}
