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
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = {"isRubyArray(array)", "isNullArray(array)"})
    public DynamicObject getTailNull(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        return createEmptyArray();
    }

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public DynamicObject getTailIntegerFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((int[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public DynamicObject getTailLongFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((long[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public DynamicObject getTailFloat(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((double[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public DynamicObject getTailObject(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((Object[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

}
