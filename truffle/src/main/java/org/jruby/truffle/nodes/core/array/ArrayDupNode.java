/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

import java.util.Arrays;

/**
 * Dup an array, without using any method lookup. This isn't a call - it's an operation on a core class.
 */
@NodeChildren({@NodeChild(value = "array", type = RubyNode.class)})
@ImportStatic(ArrayGuards.class)
public abstract class ArrayDupNode extends RubyNode {

    public ArrayDupNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract RubyArray executeDup(VirtualFrame frame, RubyArray array);

    @Specialization(guards = "isNullArray(from)")
    public RubyArray dupNull(RubyArray from) {
        return createEmptyArray();
    }

    @Specialization(guards = "isIntArray(from)")
    public RubyArray dupIntegerFixnum(RubyArray from) {
        return createArray(Arrays.copyOf((int[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = "isLongArray(from)")
    public RubyArray dupLongFixnum(RubyArray from) {
        return createArray(Arrays.copyOf((long[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = "isDoubleArray(from)")
    public RubyArray dupFloat(RubyArray from) {
        return createArray(Arrays.copyOf((double[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = "isObjectArray(from)")
    public RubyArray dupObject(RubyArray from) {
        return createArray(Arrays.copyOf((Object[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

}
