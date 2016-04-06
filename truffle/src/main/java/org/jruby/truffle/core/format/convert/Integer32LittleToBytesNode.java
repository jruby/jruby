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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;

@NodeChildren({
        @NodeChild(value = "value", type = FormatNode.class),
})
public abstract class Integer32LittleToBytesNode extends FormatNode {

    public Integer32LittleToBytesNode(RubyContext context) {
        super(context);
    }

    @Specialization
    public byte[] encode(long value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }

}
