/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.nodes.ShapeCachingGuards;
import org.jruby.truffle.runtime.Options;

@ImportStatic(ShapeCachingGuards.class)
public abstract class ReadHeadObjectFieldNode extends Node {
    private final Object defaultValue;
    protected final Object name;

    public ReadHeadObjectFieldNode(Object name, Object defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public Object getName() {
        return name;
    }

    public abstract Object execute(DynamicObject object);

    @Specialization(
            guards = "receiver.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected Object readObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("cachedShape.getProperty(name)") Property property) {
        if (property != null) {
            return property.get(receiver, cachedShape);
        } else {
            return defaultValue;
        }
    }

    @Specialization(guards = "updateShape(object)")
    public Object updateShapeAndRead(DynamicObject object) {
        return execute(object);
    }

    @TruffleBoundary
    @Specialization(contains = { "readObjectFieldCached", "updateShapeAndRead" })
    protected Object readObjectFieldUncached(DynamicObject receiver) {
        return receiver.get(name, defaultValue);
    }

    protected int getCacheLimit() {
        return Options.FIELD_LOOKUP_CACHE;
    }

}
