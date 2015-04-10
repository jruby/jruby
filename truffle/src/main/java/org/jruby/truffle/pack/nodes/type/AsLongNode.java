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
import org.jruby.truffle.pack.nodes.PackNode;

/**
 * Re-interpret a value as a {@code long}.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class AsLongNode extends PackNode {

    @Specialization
    public long asLong(float object) {
        return (long) Float.floatToIntBits(object);
    }

    @Specialization
    public long asLong(double object) {
        return Double.doubleToLongBits(object);
    }

}
