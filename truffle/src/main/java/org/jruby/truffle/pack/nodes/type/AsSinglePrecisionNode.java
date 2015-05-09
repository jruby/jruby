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
 * Convert a {@code double} value to a {@code float}.
 */
@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class AsSinglePrecisionNode extends PackNode {

    @Specialization
    public float asFloat(double object) {
        return (float) object;
    }

}
