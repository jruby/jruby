/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.ArrayUtils;

import java.util.Arrays;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportGuards(ArrayGuards.class)
public abstract class ArrayGetTailNode extends RubyNode {

    final int index;

    public ArrayGetTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    public ArrayGetTailNode(ArrayGetTailNode prev) {
        super(prev);
        index = prev.index;
    }

    @Specialization(guards = "isNull")
    public RubyArray getTailNull(RubyArray array) {
        notDesignedForCompilation();

        return new RubyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = "isIntegerFixnum")
    public RubyArray getTailIntegerFixnum(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((int[]) array.getStore(), index, array.getSize()), array.getSize() - index);
        }
    }

    @Specialization(guards = "isLongFixnum")
    public RubyArray getTailLongFixnum(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((long[]) array.getStore(), index, array.getSize()), array.getSize() - index);
        }
    }

    @Specialization(guards = "isFloat")
    public RubyArray getTailFloat(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((double[]) array.getStore(), index, array.getSize()), array.getSize() - index);
        }
    }

    @Specialization(guards = "isObject")
    public RubyArray getTailObject(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((Object[]) array.getStore(), index, array.getSize()), array.getSize() - index);
        }
    }

}
