/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.convert;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.cast.ToFNode;
import org.jruby.truffle.core.format.FormatNode;

@NodeChild(value = "value", type = FormatNode.class)
public abstract class ToDoubleWithCoercionNode extends FormatNode {

    @Child private ToFNode toFNode;

    public ToDoubleWithCoercionNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public Object toDouble(VirtualFrame frame, Object value) {
        if (toFNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            toFNode = insert(ToFNode.create());
        }

        return toFNode.doDouble(frame, value);
    }

}
