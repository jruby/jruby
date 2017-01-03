/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.collections.BoundaryIterable;
import org.jruby.truffle.core.array.ArrayGuards;
import org.jruby.truffle.core.queue.LinkedBlockingQueueLocksConditions;
import org.jruby.truffle.language.objects.ShapeCachingGuards;

/**
 * Share the internal field of an object, accessible by its Layout
 */
@ImportStatic({ ShapeCachingGuards.class, ArrayGuards.class })
public abstract class ShareInternalFieldsNode extends Node {

    protected static final int CACHE_LIMIT = 8;

    protected final int depth;

    public ShareInternalFieldsNode(int depth) {
        this.depth = depth;
    }

    public abstract void executeShare(DynamicObject object);

    @Specialization(
            guards = {"array.getShape() == cachedShape", "isArrayShape(cachedShape)", "isObjectArray(array)"},
            assumptions = "cachedShape.getValidAssumption()", limit = "CACHE_LIMIT")
    protected void shareCachedObjectArray(DynamicObject array,
            @Cached("array.getShape()") Shape cachedShape,
            @Cached("createWriteBarrierNode()") WriteBarrierNode writeBarrierNode) {
        final int size = Layouts.ARRAY.getSize(array);
        final Object[] store = (Object[]) Layouts.ARRAY.getStore(array);
        for (int i = 0; i < size; i++) {
            writeBarrierNode.executeWriteBarrier(store[i]);
        }
    }

    @Specialization(
            guards = {"array.getShape() == cachedShape", "isArrayShape(cachedShape)", "!isObjectArray(array)"},
            assumptions = "cachedShape.getValidAssumption()", limit = "CACHE_LIMIT")
    protected void shareCachedOtherArray(DynamicObject array,
            @Cached("array.getShape()") Shape cachedShape) {
        /* null, int[], long[] or double[] storage */
    }

    @Specialization(
            guards = { "object.getShape() == cachedShape", "isQueueShape(cachedShape)" },
            assumptions = "cachedShape.getValidAssumption()", limit = "CACHE_LIMIT")
    protected void shareCachedQueue(DynamicObject object,
            @Cached("object.getShape()") Shape cachedShape,
            @Cached("createBinaryProfile()") ConditionProfile profileEmpty,
            @Cached("createWriteBarrierNode()") WriteBarrierNode writeBarrierNode) {
        LinkedBlockingQueueLocksConditions<Object> queue = Layouts.QUEUE.getQueue(object);
        if (!profileEmpty.profile(queue.size() == 0)) {
            for (Object e : BoundaryIterable.wrap(queue)) {
                writeBarrierNode.executeWriteBarrier(e);
            }
        }
    }

    @Specialization(
            guards = { "object.getShape() == cachedShape", "isBasicObjectShape(cachedShape)" },
            assumptions = "cachedShape.getValidAssumption()", limit = "CACHE_LIMIT")
    protected void shareCachedBasicObject(DynamicObject object,
            @Cached("object.getShape()") Shape cachedShape) {
        /* No internal fields */
    }

    @Specialization(guards = "updateShape(object)")
    public void updateShapeAndShare(DynamicObject object) {
        executeShare(object);
    }

    @Specialization(contains = { "shareCachedObjectArray", "shareCachedOtherArray", "shareCachedQueue", "shareCachedBasicObject", "updateShapeAndShare" })
    protected void shareUncached(DynamicObject object) {
        SharedObjects.writeBarrier(object);
    }

    protected WriteBarrierNode createWriteBarrierNode() {
        return WriteBarrierNodeGen.create(depth);
    }

}
