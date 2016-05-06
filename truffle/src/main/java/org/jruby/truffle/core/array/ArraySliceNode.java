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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.RubyNode;

import static org.jruby.truffle.core.array.ArrayHelpers.createArray;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArraySliceNode extends RubyNode {

    final int from; // positive
    final int to; // negative, exclusive

    public ArraySliceNode(RubyContext context, SourceSection sourceSection, int from, int to) {
        super(context, sourceSection);
        assert from >= 0;
        assert to <= 0;
        this.from = from;
        this.to = to;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject sliceNull(DynamicObject array) {
        return createArray(getContext(), null, 0);
    }

    @Specialization(guards = { "strategy.matches(array)" }, limit = "ARRAY_STRATEGIES")
    public DynamicObject readInBounds(DynamicObject array,
            @Cached("of(array)") ArrayStrategy strategy,
            @Cached("createBinaryProfile()") ConditionProfile emptyArray) {
        final int to = Layouts.ARRAY.getSize(array) + this.to;

        if (emptyArray.profile(from >= to)) {
            return createArray(getContext(), null, 0);
        } else {
            final Object store = strategy.newMirror(array).extractRange(from, to).getArray();
            return createArray(getContext(), store, to - from);
        }

    }

}
