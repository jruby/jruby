/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.format.nodes.PackNode;
import org.jruby.truffle.format.runtime.MissingValue;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class AsUnsignedNode extends PackNode {

    public AsUnsignedNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public MissingValue asUnsigned(VirtualFrame frame, MissingValue missingValue) {
        return missingValue;
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject asUnsigned(VirtualFrame frame, DynamicObject nil) {
        return nil;
    }

    @Specialization
    public int asUnsigned(VirtualFrame frame, short value) {
        return (int) value & 0xffff;
    }


}
