/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

@NodeChild(value = "value", type = FormatNode.class)
public abstract class ToDoubleWithCoercionNode extends FormatNode {

    @Child private CallDispatchHeadNode floatNode;

    @Specialization
    public Object toDouble(VirtualFrame frame, Object value) {
        if (floatNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            floatNode = insert(DispatchHeadNodeFactory.createMethodCall(true));
        }

        return floatNode.call(frame, getContext().getCoreLibrary().getKernelModule(), "Float", value);
    }

}
