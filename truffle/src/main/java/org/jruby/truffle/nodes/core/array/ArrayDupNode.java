/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;

import java.util.Arrays;

/**
 * Dup an array, without using any method lookup. This isn't a call - it's an operation on a core class.
 */
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDupNode extends RubyNode {

    @Child private AllocateArrayNode allocatorNode;

    public ArrayDupNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        allocatorNode = AllocateArrayNodeGen.create(context, sourceSection, null);
    }

    public abstract DynamicObject executeDup(VirtualFrame frame, DynamicObject array);

    @Specialization(guards = {"isRubyArray(from)", "isNullArray(from)"})
    public DynamicObject dupNull(DynamicObject from) {
        return allocatorNode.allocateEmptyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = {"isRubyArray(from)", "isIntArray(from)"})
    public DynamicObject dupIntegerFixnum(DynamicObject from) {
        return allocatorNode.allocateArray(
                getContext().getCoreLibrary().getArrayClass(),
                Arrays.copyOf((int[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)),
                ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isLongArray(from)"})
    public DynamicObject dupLongFixnum(DynamicObject from) {
        return allocatorNode.allocateArray(
                getContext().getCoreLibrary().getArrayClass(),
                Arrays.copyOf((long[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)),
                ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isDoubleArray(from)"})
    public DynamicObject dupFloat(DynamicObject from) {
        return allocatorNode.allocateArray(
                getContext().getCoreLibrary().getArrayClass(),
                Arrays.copyOf((double[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)),
                ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isObjectArray(from)"})
    public DynamicObject dupObject(DynamicObject from) {
        return allocatorNode.allocateArray(
                getContext().getCoreLibrary().getArrayClass(),
                Arrays.copyOf((Object[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)),
                ArrayNodes.getSize(from));
    }

}
