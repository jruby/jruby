/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value = "array", type = RubyNode.class)
@ImportStatic(ArrayGuards.class)
public abstract class ArrayToObjectArrayNode extends RubyNode {

    public Object[] unsplat(Object[] arguments) {
        assert arguments.length == 1;
        assert RubyGuards.isRubyArray(arguments[0]);
        return executeToObjectArray((DynamicObject) arguments[0]);
    }

    public abstract Object[] executeToObjectArray(DynamicObject array);

    @Specialization(guards = "strategy.matches(array)", limit = "ARRAY_STRATEGIES")
    public Object[] toObjectArrayOther(DynamicObject array,
            @Cached("of(array)") ArrayStrategy strategy) {
        final int size = strategy.getSize(array);
        return strategy.newMirror(array).getBoxedCopy(size);
    }

}
