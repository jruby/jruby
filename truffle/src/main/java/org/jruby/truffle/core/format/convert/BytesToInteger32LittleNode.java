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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.MissingValue;

@NodeChildren({
        @NodeChild(value = "bytes", type = FormatNode.class),
})
public abstract class BytesToInteger32LittleNode extends FormatNode {

    public BytesToInteger32LittleNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public MissingValue decode(MissingValue missingValue) {
        return missingValue;
    }

    @Specialization(guards = "isNil(nil)")
    public DynamicObject decode(DynamicObject nil) {
        return nil;
    }

    @Specialization
    public int decode(byte[] bytes) {
        int value = 0;
        value |= (bytes[3] & 0xff) << 24;
        value |= (bytes[2] & 0xff) << 16;
        value |= (bytes[1] & 0xff) << 8;
        value |=  bytes[0] & 0xff;
        return value;
    }

}
