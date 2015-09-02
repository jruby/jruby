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

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.BooleanLocation;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.IntLocation;
import com.oracle.truffle.api.object.LongLocation;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.source.SourceSection;

@NodeChild("receiver")
public abstract class ReadHeadObjectFieldNode extends RubyNode {
    private final Object defaultValue;
    protected final Object name;

    public ReadHeadObjectFieldNode(RubyContext context, SourceSection sourceSection, Object name, Object defaultValue) {
        super(context, sourceSection);
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public Object getName() {
        return name;
    }

    public abstract boolean executeBoolean(DynamicObject object) throws UnexpectedResultException;
    public abstract int executeInteger(DynamicObject object) throws UnexpectedResultException;
    public abstract long executeLong(DynamicObject object) throws UnexpectedResultException;
    public abstract double executeDouble(DynamicObject object) throws UnexpectedResultException;

    public abstract Object execute(DynamicObject object);

    @Specialization(
            guards = { "location != null", "receiver.getShape() == cachedShape" },
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected boolean readBooleanObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("getBooleanLocation(cachedShape)") BooleanLocation location) {
        return location.getBoolean(receiver, cachedShape);
    }

    @Specialization(
            guards = { "location != null", "receiver.getShape() == cachedShape" },
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected int readIntObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("getIntLocation(cachedShape)") IntLocation location) {
        return location.getInt(receiver, cachedShape);
    }

    @Specialization(
            guards = { "location != null", "receiver.getShape() == cachedShape" },
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected long readLongObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("getLongLocation(cachedShape)") LongLocation location) {
        return location.getLong(receiver, cachedShape);
    }

    @Specialization(
            guards = { "location != null", "receiver.getShape() == cachedShape" },
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()")
    protected double readDoubleObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("getDoubleLocation(cachedShape)") DoubleLocation location) {
        return location.getDouble(receiver, cachedShape);
    }

    @Specialization(
            guards = "receiver.getShape() == cachedShape",
            assumptions = "cachedShape.getValidAssumption()",
            limit = "getCacheLimit()",
            contains = { "readBooleanObjectFieldCached", "readIntObjectFieldCached", "readLongObjectFieldCached", "readDoubleObjectFieldCached" })
    protected Object readObjectFieldCached(DynamicObject receiver,
            @Cached("receiver.getShape()") Shape cachedShape,
            @Cached("cachedShape.getProperty(name)") Property property) {
        if (property != null) {
            return property.get(receiver, cachedShape);
        } else {
            return defaultValue;
        }
    }

    @TruffleBoundary
    @Specialization
    protected Object readObjectFieldUncached(DynamicObject receiver) {
        final Shape shape = receiver.getShape();
        final Property property = shape.getProperty(name);
        if (property != null) {
            return property.get(receiver, shape);
        } else {
            return defaultValue;
        }
    }

    protected BooleanLocation getBooleanLocation(Shape shape) {
        final Property property = shape.getProperty(name);
        if (property != null && property.getLocation() instanceof BooleanLocation) {
            return (BooleanLocation) property.getLocation();
        }
        return null;
    }

    protected IntLocation getIntLocation(Shape shape) {
        final Property property = shape.getProperty(name);
        if (property != null && property.getLocation() instanceof IntLocation) {
            return (IntLocation) property.getLocation();
        }
        return null;
    }

    protected LongLocation getLongLocation(Shape shape) {
        final Property property = shape.getProperty(name);
        if (property != null && property.getLocation() instanceof LongLocation) {
            return (LongLocation) property.getLocation();
        }
        return null;
    }

    protected DoubleLocation getDoubleLocation(Shape shape) {
        final Property property = shape.getProperty(name);
        if (property != null && property.getLocation() instanceof DoubleLocation) {
            return (DoubleLocation) property.getLocation();
        }
        return null;
    }

    protected int getCacheLimit() {
        return getContext().getOptions().FIELD_LOOKUP_CACHE;
    }

}
