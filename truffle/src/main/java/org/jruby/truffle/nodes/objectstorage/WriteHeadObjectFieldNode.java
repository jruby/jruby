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

import org.jruby.truffle.runtime.Options;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

public abstract class WriteHeadObjectFieldNode extends Node {

    private final Object name;

    public WriteHeadObjectFieldNode(Object name) {
        this.name = name;
    }

    public Object getName() {
        return name;
    }

    public abstract void execute(DynamicObject object, Object value);

    @Specialization(
            guards = {
                    "location != null",
                    "object.getShape() == cachedShape"
            },
            assumptions = { "newArray(cachedShape.getValidAssumption(), validLocation)" },
            limit = "getCacheLimit()")
    public void writeExistingField(DynamicObject object, Object value,
            @Cached("object.getShape()") Shape cachedShape,
            @Cached("getLocation(object, value)") Location location,
            @Cached("createAssumption()") Assumption validLocation) {
        try {
            location.set(object, value, cachedShape);
        } catch (IncompatibleLocationException | FinalLocationException e) {
            // remove this entry
            validLocation.invalidate();
            execute(object, value);
        }
    }

    @Specialization(
            guards = {
                    "!hasField",
                    "object.getShape() == oldShape" },
            assumptions = { "newArray(oldShape.getValidAssumption(), newShape.getValidAssumption(), validLocation)" },
            limit = "getCacheLimit()")
    public void writeNewField(DynamicObject object, Object value,
            @Cached("hasField(object, value)") boolean hasField,
            @Cached("object.getShape()") Shape oldShape,
            @Cached("transitionWithNewField(object, value)") Shape newShape,
            @Cached("getNewLocation(newShape)") Location location,
            @Cached("createAssumption()") Assumption validLocation) {
        try {
            location.set(object, value, oldShape, newShape);
        } catch (IncompatibleLocationException e) {
            // remove this entry
            validLocation.invalidate();
            execute(object, value);
        }
    }

    @TruffleBoundary
    @Specialization(guards = "object.updateShape()")
    public void updateShape(DynamicObject object, Object value) {
        execute(object, value);
    }

    @TruffleBoundary
    @Specialization
    public void writeUncached(DynamicObject object, Object value) {
        object.updateShape();
        final Shape shape = object.getShape();
        final Property property = shape.getProperty(name);

        if (property == null) {
            object.define(name, value, 0);
        } else {
            property.setGeneric(object, value, shape);
        }
    }

    protected Location getLocation(DynamicObject object, Object value) {
        object.updateShape();
        final Shape oldShape = object.getShape();
        final Property property = oldShape.getProperty(name);

        if (property != null && property.getLocation().canSet(object, value)) {
            return property.getLocation();
        } else {
            return null;
        }
    }

    protected boolean hasField(DynamicObject object, Object value) {
        return getLocation(object, value) != null;
    }

    protected Shape transitionWithNewField(DynamicObject object, Object value) {
        return object.getShape().defineProperty(name, value, 0);
    }

    protected Location getNewLocation(Shape newShape) {
        return newShape.getProperty(name).getLocation();
    }

    protected Assumption createAssumption() {
        return Truffle.getRuntime().createAssumption();
    }

    protected int getCacheLimit() {
        return Options.FIELD_LOOKUP_CACHE;
    }

    // workaround for DSL bug
    protected Assumption[] newArray(Assumption a1, Assumption a2, Assumption a3) {
        return new Assumption[] { a1, a2, a3 };
    }

    // workaround for DSL bug
    protected Assumption[] newArray(Assumption a1, Assumption a2) {
        return new Assumption[] { a1, a2 };
    }

}
