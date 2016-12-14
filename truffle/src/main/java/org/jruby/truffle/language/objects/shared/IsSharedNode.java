/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects.shared;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.language.objects.ShapeCachingGuards;

@ImportStatic(ShapeCachingGuards.class)
public abstract class IsSharedNode extends Node {

    protected static final int CACHE_LIMIT = 8;

    public abstract boolean executeIsShared(DynamicObject object);

    @Specialization(
            guards = "object.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "CACHE_LIMIT")
    protected boolean isShareCached(DynamicObject object,
            @Cached("object.getShape()") Shape cachedShape,
            @Cached("isShared(cachedShape)") boolean shared) {
        return shared;
    }

    @Specialization(guards = "updateShape(object)")
    public boolean updateShapeAndIsShared(DynamicObject object) {
        return executeIsShared(object);
    }

    @Specialization(contains = { "isShareCached", "updateShapeAndIsShared" })
    protected boolean isSharedUncached(DynamicObject object) {
        return SharedObjects.isShared(object);
    }

    protected boolean isShared(Shape shape) {
        return SharedObjects.isShared(shape);
    }

}
