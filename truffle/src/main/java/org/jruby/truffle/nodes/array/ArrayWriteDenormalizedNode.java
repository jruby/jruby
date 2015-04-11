/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="value", type=RubyNode.class)
})
public abstract class ArrayWriteDenormalizedNode extends RubyNode {

    @Child private ArrayWriteNormalizedNode writeNode;

    public ArrayWriteDenormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeWrite(VirtualFrame frame, RubyArray array, int index, Object value);

    @Specialization
    public Object write(VirtualFrame frame, RubyArray array, int index, Object value) {
        if (writeNode == null) {
            CompilerDirectives.transferToInterpreter();
            writeNode = insert(ArrayWriteNormalizedNodeFactory.create(getContext(), getSourceSection(), null, null, null));
        }

        final int normalizedIndex = array.normalizeIndex(index);

        return writeNode.executeWrite(frame, array, normalizedIndex, value);
    }

}
