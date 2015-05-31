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
import org.jruby.truffle.runtime.core.RubyBasicObject;

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

    public abstract RubyBasicObject executeDup(VirtualFrame frame, RubyBasicObject array);

    @Specialization(guards = {"isRubyArray(from)", "isNullArray(from)"})
    public RubyBasicObject dupNull(RubyBasicObject from) {
        return createEmptyArray();
    }

    @Specialization(guards = {"isRubyArray(from)", "isIntArray(from)"})
    public RubyBasicObject dupIntegerFixnum(RubyBasicObject from) {
        return createArray(Arrays.copyOf((int[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isLongArray(from)"})
    public RubyBasicObject dupLongFixnum(RubyBasicObject from) {
        return createArray(Arrays.copyOf((long[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isDoubleArray(from)"})
    public RubyBasicObject dupFloat(RubyBasicObject from) {
        return createArray(Arrays.copyOf((double[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

    @Specialization(guards = {"isRubyArray(from)", "isObjectArray(from)"})
    public RubyBasicObject dupObject(RubyBasicObject from) {
        return createArray(Arrays.copyOf((Object[]) ArrayNodes.getStore(from), ArrayNodes.getSize(from)), ArrayNodes.getSize(from));
    }

}
