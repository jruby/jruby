/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IncompatibleLocationException;
import com.oracle.truffle.api.object.Location;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.language.RubyBaseNode;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.objects.shared.SharedObjects;
import org.jruby.truffle.language.objects.shared.WriteBarrierNode;

@ImportStatic({ RubyGuards.class, ShapeCachingGuards.class })
public abstract class WriteObjectFieldNode extends RubyBaseNode {

    private final Object name;

    public WriteObjectFieldNode(Object name) {
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
            limit = "getCacheLimit()")
    public void writeExistingField(DynamicObject object, Object value,
            @Cached("getLocation(object, value)") Location location,
            @Cached("object.getShape()") Shape cachedShape,
            @Cached("createAssumption()") Assumption validLocation,
            @Cached("isShared(cachedShape)") boolean shared,
            @Cached("createWriteBarrierNode(shared)") WriteBarrierNode writeBarrierNode) {
        try {
            if (shared) {
                writeBarrierNode.executeWriteBarrier(value);
                synchronized (object) {
                    // Re-check the shape under the monitor as another thread might have changed it
                    // by adding a field (fine) or upgrading an existing field to Object storage
                    // (need to use the new storage)
                    if (object.getShape() != cachedShape) {
                        CompilerDirectives.transferToInterpreter();
                        execute(object, value);
                        return;
                    }
                    location.set(object, value, cachedShape);
                }
            } else {
                location.set(object, value, cachedShape);
            }
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
            limit = "getCacheLimit()")
    public void writeNewField(DynamicObject object, Object value,
            @Cached("getLocation(object, value)") Location location,
            @Cached("object.getShape()") Shape oldShape,
            @Cached("defineProperty(oldShape, value)") Shape newShape,
            @Cached("getNewLocation(newShape)") Location newLocation,
            @Cached("createAssumption()") Assumption validLocation,
            @Cached("isShared(oldShape)") boolean shared,
            @Cached("createWriteBarrierNode(shared)") WriteBarrierNode writeBarrierNode) {
        try {
            if (shared) {
                writeBarrierNode.executeWriteBarrier(value);
                synchronized (object) {
                    // Re-check the shape under the monitor as another thread might have changed it
                    // by adding a field or upgrading an existing field to Object storage
                    // (we need to make sure to have the right shape to add the new field)
                    if (object.getShape() != oldShape) {
                        CompilerDirectives.transferToInterpreter();
                        execute(object, value);
                        return;
                    }
                    newLocation.set(object, value, oldShape, newShape);
                }
            } else {
                newLocation.set(object, value, oldShape, newShape);
            }
        } catch (IncompatibleLocationException e) {
            // remove this entry
            validLocation.invalidate();
            newShape.getLastProperty().setGeneric(object, value, oldShape, newShape);
        }
    }

    @Specialization(guards = "updateShape(object)")
    public void updateShapeAndWrite(DynamicObject object, Object value) {
        execute(object, value);
    }

    @TruffleBoundary
    @Specialization(contains = { "writeExistingField", "writeNewField", "updateShapeAndWrite" })
    public void writeUncached(DynamicObject object, Object value) {
        final boolean shared = SharedObjects.isShared(object);
        if (shared) {
            SharedObjects.writeBarrier(value);
            synchronized (object) {
                Shape shape = object.getShape();
                Shape newShape = defineProperty(shape, value);
                newShape.getProperty(name).setSafe(object, value, shape, newShape);
            }
        } else {
            object.define(name, value);
        }
    }

    protected Location getLocation(DynamicObject object, Object value) {
        final Shape oldShape = object.getShape();
        final Property property = oldShape.getProperty(name);

        if (PropertyFlags.isDefined(property) && property.getLocation().canSet(object, value)) {
            return property.getLocation();
        } else {
            return null;
        }
    }

    protected Shape defineProperty(Shape oldShape, Object value) {
        Property property = oldShape.getProperty(name);
        if (property != null && PropertyFlags.isRemoved(property)) {
            // Do not reuse location of removed properties
            Location location = oldShape.allocator().locationForValue(value);
            return oldShape.replaceProperty(property, property.relocate(location).copyWithFlags(0));
        } else {
            return oldShape.defineProperty(name, value, 0);
        }
    }

    protected Location getNewLocation(Shape newShape) {
        return newShape.getProperty(name).getLocation();
    }

    protected Assumption createAssumption() {
        return Truffle.getRuntime().createAssumption("object location is valid");
    }

    protected int getCacheLimit() {
        return getContext().getOptions().INSTANCE_VARIABLE_CACHE;
    }

    protected boolean isShared(Shape shape) {
        return SharedObjects.isShared(shape);
    }

    protected WriteBarrierNode createWriteBarrierNode(boolean shared) {
        if (shared) {
            return WriteBarrierNode.create();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return name + " =";
    }

}
