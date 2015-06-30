/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.coerce.ToFNode;
import org.jruby.truffle.nodes.coerce.ToFNodeGen;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Convert a value to a {@code double}.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToDoubleNode extends PackNode {

    @Child private ToFNode toFNode;

    public ToDoubleNode(RubyContext context) {
        super(context);
    }

    public abstract double executeToDouble(VirtualFrame frame, Object object);

    @Specialization
    public double toDouble(VirtualFrame frame, int value) {
        return value;
    }

    @Specialization
    public double toDouble(VirtualFrame frame, long value) {
        return value;
    }

    @Specialization
    public double toDouble(VirtualFrame frame, double value) {
        return value;
    }

    @Specialization
    public Object toDouble(RubyBasicObject value) {
        if (toFNode == null) {
            CompilerDirectives.transferToInterpreter();
            toFNode = insert(ToFNodeGen.create(getContext(), getSourceSection(), null));
        }

        final VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, value, null, new Object[]{}), new FrameDescriptor());
        return toFNode.doDouble(frame, value);
    }
}
