/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;

/**
 * Dup an array, without using any method lookup. This isn't a call - it's an operation on a core class.
 */
@NodeChild(value = "array", type = RubyNode.class)
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDupNode extends RubyNode {

    @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

    public abstract DynamicObject executeDup(VirtualFrame frame, DynamicObject array);

    @Specialization(guards = "strategy.matches(from)", limit = "ARRAY_STRATEGIES")
    public DynamicObject dup(DynamicObject from,
            @Cached("of(from)") ArrayStrategy strategy) {
        final int size = strategy.getSize(from);
        Object store = strategy.newMirror(from).copyArrayAndMirror().getArray();
        return allocateNode.allocateArray(coreLibrary().getArrayClass(), store, size);
    }

}
