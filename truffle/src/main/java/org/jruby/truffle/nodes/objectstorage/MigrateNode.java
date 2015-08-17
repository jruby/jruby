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

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;

public class MigrateNode extends WriteObjectFieldChainNode {

    private final Shape expectedShape;

    public MigrateNode(Shape expectedShape, WriteObjectFieldNode next) {
        super(next);
        this.expectedShape = expectedShape;
    }

    @Override
    public void execute(DynamicObject object, Object value) {
        if (object.getShape() == expectedShape) {
            object.updateShape();
        }

        next.execute(object, value);
    }

}
