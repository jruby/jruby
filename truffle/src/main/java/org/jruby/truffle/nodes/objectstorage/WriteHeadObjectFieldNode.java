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

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import org.jruby.truffle.nodes.ShapeCachingGuards;
import org.jruby.truffle.runtime.Options;

@ImportStatic(ShapeCachingGuards.class)
public abstract class WriteHeadObjectFieldNode extends Node {

    protected static final int CACHE_LIMIT = Options.INSTANCE_VARIABLE_LOOKUP_CACHE;

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
            assumptions = { "cachedShape.getValidAssumption()", "validLocation" },
            limit = "CACHE_LIMIT")
    public void writeExistingField(DynamicObject object, Object value,
            @Cached("getLocation(object, value)") Location location,
            @Cached("object.getShape()") Shape cachedShape,
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
                    "location == null",
                    "object.getShape() == oldShape" },
            assumptions = { "oldShape.getValidAssumption()", "newShape.getValidAssumption()", "validLocation" },
            limit = "CACHE_LIMIT")
    public void writeNewField(DynamicObject object, Object value,
            @Cached("getLocation(object, value)") Location location,
            @Cached("object.getShape()") Shape oldShape,
            @Cached("defineProperty(oldShape, value)") Shape newShape,
            @Cached("getNewLocation(newShape)") Location newLocation,
            @Cached("createAssumption()") Assumption validLocation) {
        try {
            newLocation.set(object, value, oldShape, newShape);
        } catch (IncompatibleLocationException e) {
            // remove this entry
            validLocation.invalidate();
            execute(object, value);
        }
    }

    @Specialization(guards = "updateShape(object)")
    public void updateShapeAndWrite(DynamicObject object, Object value) {
        execute(object, value);
    }

    @TruffleBoundary
    @Specialization(contains = { "writeExistingField", "writeNewField", "updateShapeAndWrite" })
    public void writeUncached(DynamicObject object, Object value) {
        object.define(name, value, 0);
    }

    protected Location getLocation(DynamicObject object, Object value) {
        final Shape oldShape = object.getShape();
        final Property property = oldShape.getProperty(name);

        if (property != null && property.getLocation().canSet(object, value)) {
            return property.getLocation();
        } else {
            return null;
        }
    }

    protected Shape defineProperty(Shape oldShape, Object value) {
        return oldShape.defineProperty(name, value, 0);
    }

    protected Location getNewLocation(Shape newShape) {
        return newShape.getProperty(name).getLocation();
    }

    protected Assumption createAssumption() {
        return Truffle.getRuntime().createAssumption("object location is valid");
    }

}
