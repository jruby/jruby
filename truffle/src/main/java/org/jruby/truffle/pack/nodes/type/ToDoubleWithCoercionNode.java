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
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.coerce.ToFNode;
import org.jruby.truffle.nodes.coerce.ToFNodeGen;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Convert a value to a {@code double} with type coercion if necessary.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToDoubleWithCoercionNode extends PackNode {

    @Child private ToFNode toFNode;

    public ToDoubleWithCoercionNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public Object toDouble(VirtualFrame frame, Object value) {
        if (toFNode == null) {
            CompilerDirectives.transferToInterpreter();
            toFNode = insert(ToFNodeGen.create(getContext(), getSourceSection(), null));
        }

        return toFNode.doDouble(frame, value);
    }

}
