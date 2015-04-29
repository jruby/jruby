/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.*;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedWriteObjectFieldNode extends WriteObjectFieldNode {

    private final Object name;
    public UninitializedWriteObjectFieldNode(Object name) {
        this.name = name;

    }

    @Override
    public void execute(RubyBasicObject object, Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final Shape currentShape = object.getDynamicObject().getShape();

        // If the current shape is obsolete, add a node to migrate
        if (object.getDynamicObject().updateShape()) {
            final MigrateNode migrateNode = new MigrateNode(currentShape, this);
            replace(migrateNode);
            migrateNode.execute(object, value);
            return;
        }

        final Shape newShape;
        Location location;

        final Property currentProperty = currentShape.getProperty(name);
        final Property newProperty;

        if (currentProperty != null && currentProperty.getLocation().canSet(object.getDynamicObject(), value)) {
            newShape = currentShape;
            newProperty = currentProperty;
            newProperty.setSafe(object.getDynamicObject(), value, null);
        } else {
            object.getOperations().setInstanceVariable(object, name, value);
            newShape = object.getDynamicObject().getShape();
            newProperty = newShape.getProperty(name);

            if (newProperty == null) {
                throw new IllegalStateException("Property missing from object's shape even after setting it");
            }
        }

        location = newProperty.getLocation();
        
        // MG: is this assertion misplaced?
        // assert location.canSet(object.getDynamicObject(), value);

        final WriteObjectFieldChainNode writeNode;

        if (location instanceof BooleanLocation) {
            writeNode = new WriteBooleanObjectFieldNode(currentShape, newShape, (BooleanLocation) location, this);
        } else if (location instanceof IntLocation) {
            writeNode = new WriteIntegerObjectFieldNode(currentShape, newShape, (IntLocation) location, this);
        } else if (location instanceof LongLocation) {
            writeNode = new WriteLongObjectFieldNode(currentShape, newShape, (LongLocation) location, this);
        } else if (location instanceof DoubleLocation) {
            writeNode = new WriteDoubleObjectFieldNode(currentShape, newShape, (DoubleLocation) location, this);
        } else {
            writeNode = new WriteObjectObjectFieldNode(currentShape, newShape, location, this);
        }

        replace(writeNode, "adding new write object field node to chain");
        // not executing, value is already set
    }

}
