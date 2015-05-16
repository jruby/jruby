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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.math.BigInteger;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class ToIntegerNode extends PackNode {

    public ToIntegerNode(RubyContext context) {
        super(context);
    }

    public abstract Object executeToInteger(VirtualFrame frame, Object object);

    @Specialization
    public int toInteger(VirtualFrame frame, int value) {
        return value;
    }

    @Specialization
    public long toInteger(VirtualFrame frame, long value) {
        return value;
    }

    @Specialization(guards = "isRubyBignum(value)")
    public RubyBasicObject toInteger(VirtualFrame frame, RubyBasicObject value) {
        return value;
    }

    @Specialization
    public long toInteger(VirtualFrame frame, double value) {
        return (long) value;
    }

}
