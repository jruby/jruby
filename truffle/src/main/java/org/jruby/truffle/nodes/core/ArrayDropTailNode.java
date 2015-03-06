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
public abstract class ArrayDropTailNode extends RubyNode {

    final int index;

    public ArrayDropTailNode(RubyContext context, SourceSection sourceSection, int index) {
        super(context, sourceSection);
        this.index = index;
    }

    public ArrayDropTailNode(ArrayDropTailNode prev) {
        super(prev);
        index = prev.index;
    }

    @Specialization(guards = "isNull")
    public RubyArray getHeadNull(RubyArray array) {
        notDesignedForCompilation("9c291f2f102a4771940d411b95db7cbe");

        return new RubyArray(getContext().getCoreLibrary().getArrayClass());
    }

    @Specialization(guards = "isIntegerFixnum")
    public RubyArray getHeadIntegerFixnum(RubyArray array) {
        notDesignedForCompilation("d1e3cffe2d2d43a683d87321c62722c9");

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((int[]) array.getStore(), 0, array.getSize() - index), array.getSize() - index);
        }
    }

    @Specialization(guards = "isLongFixnum")
    public RubyArray geHeadLongFixnum(RubyArray array) {
        notDesignedForCompilation("bcdd1245344c462eabb3635ec2aca694");

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((long[]) array.getStore(), 0, size), size);
        }
    }

    @Specialization(guards = "isFloat")
    public RubyArray getHeadFloat(RubyArray array) {
        notDesignedForCompilation("3ace4793fe2e431b896868ab629f75b9");

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((double[]) array.getStore(), 0, size), size);
        }
    }

    @Specialization(guards = "isObject")
    public RubyArray getHeadObject(RubyArray array) {
        notDesignedForCompilation("19de3661b5d34abd8b4696f381b44684");

        if (index >= array.getSize()) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        } else {
            final int size = array.getSize() - index;
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.extractRange((Object[]) array.getStore(), 0, size), size);
        }
    }

}
