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

import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DoubleLocation;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class ReadDoubleObjectFieldNode extends ReadObjectFieldChainNode {

    private final DoubleLocation storageLocation;

    public ReadDoubleObjectFieldNode(Shape objectLayout, DoubleLocation storageLocation, ReadObjectFieldNode next) {
        super(objectLayout, next);
        this.storageLocation = storageLocation;
    }

    @Override
    public double executeDouble(RubyBasicObject object) throws UnexpectedResultException {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.executeDouble(object);
        }

        final boolean condition = object.getObjectLayout() == objectLayout;

        if (condition) {
            return storageLocation.getDouble(object.getDynamicObject(), objectLayout);
        } else {
            return next.executeDouble(object);
        }
    }

    @Override
    public Object execute(RubyBasicObject object) {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.execute(object);
        }

        final boolean condition = object.getObjectLayout() == objectLayout;

        if (condition) {
            return storageLocation.get(object.getDynamicObject(), objectLayout);
        } else {
            return next.execute(object);
        }
    }

}
