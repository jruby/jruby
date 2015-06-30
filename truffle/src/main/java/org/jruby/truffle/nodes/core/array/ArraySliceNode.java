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
public abstract class ArraySliceNode extends RubyNode {

    final int from; // positive
    final int to; // negative, exclusive

    public ArraySliceNode(RubyContext context, SourceSection sourceSection, int from, int to) {
        super(context, sourceSection);
        assert from >= 0;
        assert to <= 0;
        this.from = from;
        this.to = to;
    }

    @Specialization(guards = {"isRubyArray(array)", "isNullArray(array)"})
    public RubyBasicObject sliceNull(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();

        return createEmptyArray();
    }

    @Specialization(guards = {"isRubyArray(array)", "isIntArray(array)"})
    public RubyBasicObject sliceIntegerFixnum(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = ArrayNodes.getSize(array) + this.to;

        if (from >= to) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((int[]) ArrayNodes.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isLongArray(array)"})
    public RubyBasicObject sliceLongFixnum(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = ArrayNodes.getSize(array) + this.to;

        if (from >= to) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((long[]) ArrayNodes.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isDoubleArray(array)"})
    public RubyBasicObject sliceFloat(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = ArrayNodes.getSize(array) + this.to;

        if (from >= to) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((double[]) ArrayNodes.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = {"isRubyArray(array)", "isObjectArray(array)"})
    public RubyBasicObject sliceObject(RubyBasicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = ArrayNodes.getSize(array) + this.to;

        if (from >= to) {
            return createEmptyArray();
        } else {
            return createArray(ArrayUtils.extractRange((Object[]) ArrayNodes.getStore(array), from, to), to - from);
        }
    }

}
