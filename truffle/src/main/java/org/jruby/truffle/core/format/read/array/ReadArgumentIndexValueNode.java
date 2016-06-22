/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.read.array;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.format.FormatNode;
import org.jruby.truffle.core.format.read.SourceNode;

@NodeChildren({
    @NodeChild(value = "source", type = SourceNode.class),
})
public abstract class ReadArgumentIndexValueNode extends FormatNode {

    private final int index;

    public ReadArgumentIndexValueNode(RubyContext context, int index) {
        super(context);
        this.index = index - 1;
    }

    @Specialization
    public Object read(VirtualFrame frame, Object[] source) {
        return source[this.index];
    }

}
