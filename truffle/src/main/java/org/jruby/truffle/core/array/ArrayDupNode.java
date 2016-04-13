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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

/**
 * Dup an array, without using any method lookup. This isn't a call - it's an operation on a core class.
 */
@NodeChild(value = "array", type = RubyNode.class)
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDupNode extends RubyNode {

    @Child private AllocateObjectNode allocateNode;

    public ArrayDupNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
    }

    public abstract DynamicObject executeDup(VirtualFrame frame, DynamicObject array);

    @Specialization(guards = "isNullArray(from)")
    public DynamicObject dupNull(DynamicObject from) {
        return allocateNode.allocateArray(coreLibrary().getArrayClass(), null, 0);
    }

    @Specialization(guards = "isIntArray(from)")
    public DynamicObject dupIntegerFixnum(DynamicObject from) {
        final int[] store = (int[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocateArray(
                coreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = "isLongArray(from)")
    public DynamicObject dupLongFixnum(DynamicObject from) {
        final long[] store = (long[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocateArray(
                coreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = "isDoubleArray(from)")
    public DynamicObject dupFloat(DynamicObject from) {
        final double[] store = (double[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocateArray(
                coreLibrary().getArrayClass(),
                store.clone(),
                Layouts.ARRAY.getSize(from));
    }

    @Specialization(guards = "isObjectArray(from)")
    public DynamicObject dupObject(DynamicObject from) {
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(from);
        return allocateNode.allocateArray(
                coreLibrary().getArrayClass(),
                ArrayUtils.copy(store),
                Layouts.ARRAY.getSize(from));
    }

}
