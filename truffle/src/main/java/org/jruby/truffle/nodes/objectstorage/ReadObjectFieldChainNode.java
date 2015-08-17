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

import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.object.DynamicObject;

public abstract class ReadObjectFieldChainNode extends ReadObjectFieldNode {

    protected final Shape objectLayout;

    @Child protected ReadObjectFieldNode next;

    public ReadObjectFieldChainNode(Shape objectLayout, ReadObjectFieldNode next) {
        this.objectLayout = objectLayout;
        this.next = next;
    }

    @Override
    public boolean isSet(DynamicObject object) {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.isSet(object);
        }

        final boolean condition = object.getShape() == objectLayout;

        if (condition) {
            return true;
        } else {
            return next.isSet(object);
        }
    }

}
