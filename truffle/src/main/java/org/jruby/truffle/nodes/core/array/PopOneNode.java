/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;

@NodeChildren({
        @NodeChild("array")
})
@ImportStatic(ArrayGuards.class)
public abstract class PopOneNode extends RubyNode {

    public PopOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executePopOne(DynamicObject array);

    // Pop from an empty array

    @Specialization(guards = {"isRubyArray(array)", "isEmptyArray(array)"})
    public DynamicObject popOneEmpty(DynamicObject array) {
        return nil();
    }

    // Pop from a non-empty array

    @Specialization(guards = {"isRubyArray(array)", "!isEmptyArray(array)", "isIntArray(array)"})
    public Object popOneInteger(DynamicObject array) {
        return popOneGeneric(array, ArrayMirror.reflect((int[]) ArrayNodes.getStore(array)));
    }

    @Specialization(guards = {"isRubyArray(array)", "!isEmptyArray(array)", "isLongArray(array)"})
    public Object popOneLong(DynamicObject array) {
        return popOneGeneric(array, ArrayMirror.reflect((long[]) ArrayNodes.getStore(array)));
    }

    @Specialization(guards = {"isRubyArray(array)", "!isEmptyArray(array)", "isDoubleArray(array)"})
    public Object popOneDouble(DynamicObject array) {
        return popOneGeneric(array, ArrayMirror.reflect((double[]) ArrayNodes.getStore(array)));
    }

    @Specialization(guards = {"isRubyArray(array)", "!isEmptyArray(array)", "isObjectArray(array)"})
    public Object popOneObject(DynamicObject array) {
        return popOneGeneric(array, ArrayMirror.reflect((Object[]) ArrayNodes.getStore(array)));
    }

    private Object popOneGeneric(DynamicObject array, ArrayMirror storeMirror) {
        final int size = ArrayNodes.getSize(array);
        final Object value = storeMirror.get(size - 1);
        ArrayNodes.setSize(array, size - 1);
        return value;
    }

}
