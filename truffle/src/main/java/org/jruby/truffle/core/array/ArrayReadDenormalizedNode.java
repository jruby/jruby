/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class)
})
public abstract class ArrayReadDenormalizedNode extends RubyNode {

    @Child private ArrayReadNormalizedNode readNode;

    public ArrayReadDenormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        readNode = ArrayReadNormalizedNodeGen.create(getContext(), getSourceSection(), null, null);
    }

    public abstract Object executeRead(VirtualFrame frame, DynamicObject array, int index);

    @Specialization
    public Object read(VirtualFrame frame, DynamicObject array, int index,
            @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
        final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

        return readNode.executeRead(frame, array, normalizedIndex);
    }

}
