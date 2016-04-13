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
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = "isNullArray(array)")
    public DynamicObject getTailNull(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
    }

    @Specialization(guards = "isIntArray(array)")
    public DynamicObject getTailIntegerFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= Layouts.ARRAY.getSize(array)) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((int[]) Layouts.ARRAY.getStore(array), index, Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array) - index);
        }
    }

    @Specialization(guards = "isLongArray(array)")
    public DynamicObject getTailLongFixnum(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= Layouts.ARRAY.getSize(array)) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((long[]) Layouts.ARRAY.getStore(array), index, Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array) - index);
        }
    }

    @Specialization(guards = "isDoubleArray(array)")
    public DynamicObject getTailFloat(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= Layouts.ARRAY.getSize(array)) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((double[]) Layouts.ARRAY.getStore(array), index, Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array) - index);
        }
    }

    @Specialization(guards = "isObjectArray(array)")
    public DynamicObject getTailObject(DynamicObject array) {
        CompilerDirectives.transferToInterpreter();

        if (index >= Layouts.ARRAY.getSize(array)) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), null, 0);
        } else {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), ArrayUtils.extractRange((Object[]) Layouts.ARRAY.getStore(array), index, Layouts.ARRAY.getSize(array)), Layouts.ARRAY.getSize(array) - index);
        }
    }

}
