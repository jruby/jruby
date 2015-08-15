/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.objects.AllocateObjectNode;
import org.jruby.truffle.runtime.RubyContext;

@NodeChildren
public abstract class AllocateArrayNode extends AllocateObjectNode {

    public AllocateArrayNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public DynamicObject allocateEmptyArray(DynamicObject arrayClass) {
        return executeAllocate(arrayClass);
    }

    public DynamicObject allocateArray(DynamicObject arrayClass, Object store, int size) {
        final DynamicObject array = executeAllocate(arrayClass);
        ArrayNodes.setStore(array, store, size);
        return array;
    }

    @Override
    protected DynamicObject newInstance(DynamicObjectFactory instanceFactory) {
        return instanceFactory.newInstance(null, 0);
    }

}
