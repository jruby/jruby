/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDropTailNode extends RubyNode {

    final int index;

    public ArrayDropTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = {"isRubyArray(array)", "isNullArray(array)"})
    public DynamicObject getHeadNull(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        return createEmptyArray();
    }

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject getHeadIntegerFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.ARRAY_LAYOUT.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((int[]) ArrayNodes.ARRAY_LAYOUT.getStore(array), 0, ArrayNodes.ARRAY_LAYOUT.getSize(array) - index), ArrayNodes.ARRAY_LAYOUT.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public DynamicObject geHeadLongFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.ARRAY_LAYOUT.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.ARRAY_LAYOUT.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((long[]) ArrayNodes.ARRAY_LAYOUT.getStore(array), 0, size), size);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject getHeadFloat(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.ARRAY_LAYOUT.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.ARRAY_LAYOUT.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((double[]) ArrayNodes.ARRAY_LAYOUT.getStore(array), 0, size), size);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject getHeadObject(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.ARRAY_LAYOUT.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.ARRAY_LAYOUT.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((Object[]) ArrayNodes.ARRAY_LAYOUT.getStore(array), 0, size), size);
        }
    }

}
