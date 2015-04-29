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

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.ArrayUtils;

@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDropTailNode extends RubyNode {

    final int index;

    public ArrayDropTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    @Specialization(guards = "isNull(array)")
    public RubyArray getHeadNull(RubyArray array) {
        notDesignedForCompilation();

        return new RubyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = "isIntegerFixnum(array)")
    public RubyArray getHeadIntegerFixnum(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((int[]) array.getStore(), 0, array.getSize() - index), array.getSize() - index);
        }
    }

    @Specialization(guards = "isLongFixnum(array)")
    public RubyArray geHeadLongFixnum(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((long[]) array.getStore(), 0, size), size);
        }
    }

    @Specialization(guards = "isFloat(array)")
    public RubyArray getHeadFloat(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((double[]) array.getStore(), 0, size), size);
        }
    }

    @Specialization(guards = "isObject(array)")
    public RubyArray getHeadObject(RubyArray array) {
        notDesignedForCompilation();

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((Object[]) array.getStore(), 0, size), size);
        }
    }

}
