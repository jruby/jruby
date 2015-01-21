/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.object.LongLocation;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class ReadLongObjectFieldNode extends ReadObjectFieldChainNode {

    private final Shape objectLayout;
    private final LongLocation storageLocation;

    public ReadLongObjectFieldNode(Shape objectLayout, LongLocation storageLocation, ReadObjectFieldNode next) {
        super(next);
        this.objectLayout = objectLayout;
        this.storageLocation = storageLocation;
    }

    @Override
    public long executeLong(RubyBasicObject object) throws UnexpectedResultException {
        try {
            objectLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            return next.executeLong(object);
        }

        final boolean condition = object.getObjectLayout() == objectLayout;

        if (condition) {
            return storageLocation.getLong(object.getDynamicObject(), objectLayout);
        } else {
            return next.executeLong(object);
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
