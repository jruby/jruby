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
import com.oracle.truffle.api.object.FinalLocationException;
import com.oracle.truffle.api.object.IntLocation;
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class WriteIntegerObjectFieldNode extends WriteObjectFieldChainNode {

    private final Shape expectedLayout;
    private final Shape newLayout;
    private final IntLocation storageLocation;

    public WriteIntegerObjectFieldNode(Shape expectedLayout, Shape newLayout, IntLocation storageLocation, WriteObjectFieldNode next) {
        super(next);
        this.expectedLayout = expectedLayout;
        this.newLayout = newLayout;
        this.storageLocation = storageLocation;
    }

    @Override
    public void execute(RubyBasicObject object, int value) {
        try {
            expectedLayout.getValidAssumption().check();
            newLayout.getValidAssumption().check();
        } catch (InvalidAssumptionException e) {
            replace(next);
            next.execute(object, value);
            return;
        }

        if (object.getObjectLayout() == expectedLayout) {
            try {
                if (newLayout == expectedLayout) {
                    storageLocation.setInt(object.getDynamicObject(), value, expectedLayout);
                } else {
                    storageLocation.setInt(object.getDynamicObject(), value, expectedLayout, newLayout);
                }
            } catch (FinalLocationException e) {
                replace(next, "!final").execute(object, value);
            }
        } else {
            next.execute(object, value);
        }
    }

    @Override
    public void execute(RubyBasicObject object, Object value) {
        if (value instanceof Integer) {
            execute(object, (int) value);
        } else {
            next.execute(object, value);
        }
    }

}
