/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

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

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject sliceNull(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
    }

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject sliceIntegerFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = Layouts.ARRAY.getSize(array) + this.to;

        if (from >= to) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((int[]) Layouts.ARRAY.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject sliceLongFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = Layouts.ARRAY.getSize(array) + this.to;

        if (from >= to) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((long[]) Layouts.ARRAY.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject sliceFloat(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = Layouts.ARRAY.getSize(array) + this.to;

        if (from >= to) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((double[]) Layouts.ARRAY.getStore(array), from, to), to - from);
        }
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject sliceObject(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();
        final int to = Layouts.ARRAY.getSize(array) + this.to;

        if (from >= to) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((Object[]) Layouts.ARRAY.getStore(array), from, to), to - from);
        }
    }

}
