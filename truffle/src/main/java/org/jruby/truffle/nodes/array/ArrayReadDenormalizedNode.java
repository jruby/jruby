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
        @NodeChild(value="index", type=RubyNode.class)
})
public abstract class ArrayReadDenormalizedNode extends RubyNode {

    @Child private ArrayReadNormalizedNode readNode;

    public ArrayReadDenormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeRead(VirtualFrame frame, RubyArray array, int index);

    @Specialization
    public Object read(VirtualFrame frame, RubyArray array, int index) {
        if (readNode == null) {
            CompilerDirectives.transferToInterpreter();
            readNode = insert(ArrayReadNormalizedNodeFactory.create(getContext(), getSourceSection(), null, null));
        }

        final int normalizedIndex = array.normalizeIndex(index);

        return readNode.executeRead(frame, array, normalizedIndex);
    }

}
