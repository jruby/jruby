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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChildren({
        @NodeChild(value="array", type=RubyNode.class),
        @NodeChild(value="index", type=RubyNode.class),
        @NodeChild(value="length", type=RubyNode.class)
})
public abstract class ArrayReadSliceDenormalizedNode extends RubyNode {

    @Child private ArrayReadSliceNormalizedNode readNode;

    public ArrayReadSliceDenormalizedNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        readNode = ArrayReadSliceNormalizedNodeGen.create(getContext(), getSourceSection(), null, null, null);
    }

    public abstract DynamicObject executeReadSlice(DynamicObject array, int index, int length);

    @Specialization
    public DynamicObject read(DynamicObject array, int index, int length,
            @Cached("createBinaryProfile()") ConditionProfile negativeIndexProfile) {
        final int normalizedIndex = ArrayOperations.normalizeIndex(Layouts.ARRAY.getSize(array), index, negativeIndexProfile);

        return readNode.executeReadSlice(array, normalizedIndex, length);
    }

}
