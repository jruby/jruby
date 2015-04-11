/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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

    @Specialization(guards = "isNull(from)")
    public RubyArray dupNull(RubyArray from) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
    }

    @Specialization(guards = "isIntegerFixnum(from)")
    public RubyArray dupIntegerFixnum(RubyArray from) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((int[]) from.getStore(), from.getSize()), from.getSize());
    }

    @Specialization(guards = "isLongFixnum(from)")
    public RubyArray dupLongFixnum(RubyArray from) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((long[]) from.getStore(), from.getSize()), from.getSize());
    }

    @Specialization(guards = "isFloat(from)")
    public RubyArray dupFloat(RubyArray from) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((double[]) from.getStore(), from.getSize()), from.getSize());
    }

    @Specialization(guards = "isObject(from)")
    public RubyArray dupObject(RubyArray from) {
        return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((Object[]) from.getStore(), from.getSize()), from.getSize());
    }

}
