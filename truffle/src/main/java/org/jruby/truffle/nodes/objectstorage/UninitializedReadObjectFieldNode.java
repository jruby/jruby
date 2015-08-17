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

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedReadObjectFieldNode extends ReadObjectFieldNode {

    private final Object name;

    public UninitializedReadObjectFieldNode(Object name) {
        this.name = name;
    }

    @Override
    public Object execute(DynamicObject object) {
        return rewrite(object).execute(object);
    }

    @Override
    public boolean isSet(DynamicObject object) {
        return rewrite(object).isSet(object);
    }

    private ReadObjectFieldNode rewrite(DynamicObject object) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (object.updateShape()) {
            ReadObjectFieldNode topNode = getTopNode();
            if (topNode != this) {
                // retry existing cache nodes
                return topNode;
            }
        }

        final Shape layout = object.getShape();
        final Property property = layout.getProperty(name);

        final ReadObjectFieldNode newNode;

        if (property == null) {
            newNode = new ReadMissingObjectFieldNode(layout, this);
        } else {
            final Location storageLocation = property.getLocation();

            assert storageLocation != null;

            if (storageLocation instanceof BooleanLocation) {
                newNode = new ReadBooleanObjectFieldNode(layout, (BooleanLocation) storageLocation, this);
            } else if (storageLocation instanceof IntLocation) {
                newNode = new ReadIntegerObjectFieldNode(layout, (IntLocation) storageLocation, this);
            } else if (storageLocation instanceof LongLocation) {
                newNode = new ReadLongObjectFieldNode(layout, (LongLocation) storageLocation, this);
            } else if (storageLocation instanceof DoubleLocation) {
                newNode = new ReadDoubleObjectFieldNode(layout, (DoubleLocation) storageLocation, this);
            } else {
                newNode = new ReadObjectObjectFieldNode(layout, storageLocation, this);
            }
        }

        replace(newNode, "adding new read object field node to chain");
        return newNode;
    }

    private ReadObjectFieldNode getTopNode() {
        ReadObjectFieldNode topNode = this;
        while (topNode.getParent() instanceof ReadObjectFieldNode) {
            topNode = (ReadObjectFieldNode) topNode.getParent();
        }
        return topNode;
    }
}
