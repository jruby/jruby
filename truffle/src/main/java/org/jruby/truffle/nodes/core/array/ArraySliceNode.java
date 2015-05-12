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
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.array.ArrayUtils;

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

    @Specialization(guards = "isNull(array)")
    public RubyArray sliceNull(RubyArray array) {
        CompilerDirectives.transferToInterpreter();

        return new RubyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray sliceIntegerFixnum(RubyArray array) {
        CompilerDirectives.transferToInterpreter();
        final int to = array.getSize() + this.to;

        if (from >= to) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((int[]) array.getStore(), from, to), to - from);
        }
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray sliceLongFixnum(RubyArray array) {
        CompilerDirectives.transferToInterpreter();
        final int to = array.getSize() + this.to;

        if (from >= to) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((long[]) array.getStore(), from, to), to - from);
        }
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray sliceFloat(RubyArray array) {
        CompilerDirectives.transferToInterpreter();
        final int to = array.getSize() + this.to;

        if (from >= to) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((double[]) array.getStore(), from, to), to - from);
        }
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray sliceObject(RubyArray array) {
        CompilerDirectives.transferToInterpreter();
        final int to = array.getSize() + this.to;

        if (from >= to) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((Object[]) array.getStore(), from, to), to - from);
        }
    }

}
