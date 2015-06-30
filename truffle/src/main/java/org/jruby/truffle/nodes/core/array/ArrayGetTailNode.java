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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.array.ArrayUtils;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = {"isRubyArray(array)", "isNullArray(array)"})
    public RubyBasicObject getTailNull(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        return createEmptyArray();
    }

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public RubyBasicObject getTailIntegerFixnum(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((int[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public RubyBasicObject getTailLongFixnum(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((long[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public RubyBasicObject getTailFloat(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((double[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public RubyBasicObject getTailObject(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((Object[]) ArrayNodes.getStore(array), index, ArrayNodes.getSize(array)), ArrayNodes.getSize(array) - index);
        }
    }

}
