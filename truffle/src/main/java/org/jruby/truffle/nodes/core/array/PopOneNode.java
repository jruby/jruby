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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayMirror;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChildren({
        @NodeChild("array")
})
@ImportStatic(ArrayGuards.class)
public abstract class PopOneNode extends RubyNode {

    public PopOneNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executePopOne(RubyArray array);

    // Pop from an empty array

    @Specialization(guards = "isEmpty(array)")
    public RubyBasicObject popOneEmpty(RubyArray array) {
        return nil();
    }

    // Pop from a non-empty array

    @Specialization(guards = {"!isEmpty(array)", "isIntegerFixnum(array)"})
    public Object popOneInteger(RubyArray array) {
        return popOneGeneric(array, ArrayMirror.reflect((int[]) array.getStore()));
    }

    @Specialization(guards = {"!isEmpty(array)", "isLongFixnum(array)"})
    public Object popOneLong(RubyArray array) {
        return popOneGeneric(array, ArrayMirror.reflect((long[]) array.getStore()));
    }

    @Specialization(guards = {"!isEmpty(array)", "isFloat(array)"})
    public Object popOneDouble(RubyArray array) {
        return popOneGeneric(array, ArrayMirror.reflect((double[]) array.getStore()));
    }

    @Specialization(guards = {"!isEmpty(array)", "isObject(array)"})
    public Object popOneObject(RubyArray array) {
        return popOneGeneric(array, ArrayMirror.reflect((Object[]) array.getStore()));
    }

    private Object popOneGeneric(RubyArray array, ArrayMirror storeMirror) {
        final int size = array.getSize();
        final Object value = storeMirror.get(size - 1);
        array.setSize(size - 1);
        return value;
    }

}
