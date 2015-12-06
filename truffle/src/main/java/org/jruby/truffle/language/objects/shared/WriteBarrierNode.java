/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
public abstract class WriteBarrierNode extends Node {

    protected static final int CACHE_LIMIT = 8;

    public abstract void executeWriteBarrier(Object value);

    @Specialization(
            guards = "value.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "CACHE_LIMIT")
    protected void writeBarrierCached(DynamicObject value,
            @Cached("value.getShape()") Shape cachedShape,
            @Cached("isShared(cachedShape)") boolean alreadyShared) {
        if (!alreadyShared) {
            SharedObjects.shareObject(value);
        }
    }

    @Specialization(guards = "updateShape(value)")
    public void updateShapeAndWriteBarrier(DynamicObject value) {
        executeWriteBarrier(value);
    }

    @Specialization(contains = { "writeBarrierCached", "updateShapeAndWriteBarrier" })
    protected void writeBarrierUncached(DynamicObject value) {
        SharedObjects.writeBarrier(value);
    }

    @Specialization(guards = "!isDynamicObject(value)")
    protected void noWriteBarrier(Object value) {
    }

    protected static boolean isDynamicObject(Object value) {
        return value instanceof DynamicObject;
    }

    protected static boolean isShared(Shape shape) {
        return SharedObjects.isShared(shape);
    }

}
