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
import org.jruby.truffle.runtime.core.RubyArray;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDropTailNode extends RubyNode {

    final int index;

    public ArrayDropTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = "isNullArray(array)")
    public RubyArray getHeadNull(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        return createEmptyArray();
    }

    @Specialization(guards = "isIntArray(array)")
    public RubyArray getHeadIntegerFixnum(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((int[]) ArrayNodes.getStore(array), 0, ArrayNodes.getSize(array) - index), ArrayNodes.getSize(array) - index);
        }
    }

    @Specialization(guards = "isLongArray(array)")
    public RubyArray geHeadLongFixnum(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((long[]) ArrayNodes.getStore(array), 0, size), size);
        }
    }

    @Specialization(guards = "isDoubleArray(array)")
    public RubyArray getHeadFloat(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((double[]) ArrayNodes.getStore(array), 0, size), size);
        }
    }

    @Specialization(guards = "isObjectArray(array)")
    public RubyArray getHeadObject(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= ArrayNodes.getSize(array)) {
            return createEmptyArray();
        } else {
            final int size = ArrayNodes.getSize(array) - index;
            return createArray(ArrayUtils.extractRange((Object[]) ArrayNodes.getStore(array), 0, size), size);
        }
    }

}
