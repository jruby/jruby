/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.objectstorage.LongStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class ReadLongObjectFieldNode extends ReadObjectFieldChainNode {

    private final ObjectLayout objectLayout;
    private final LongStorageLocation storageLocation;

    public ReadLongObjectFieldNode(ObjectLayout objectLayout, LongStorageLocation storageLocation, ReadObjectFieldNode next) {
        super(next);
        this.objectLayout = objectLayout;
        this.storageLocation = storageLocation;
    }

    @Override
    public long executeLong(ObjectStorage object) throws UnexpectedResultException {
        final boolean condition = object.getObjectLayout() == objectLayout;

        if (condition) {
            return storageLocation.readLong(object, condition);
        } else {
            return next.executeLong(object);
        }
    }

    @Override
    public Object execute(ObjectStorage object) {
        final boolean condition = object.getObjectLayout() == objectLayout;

        if (condition) {
            return storageLocation.read(object, condition);
        } else {
            return next.execute(object);
        }
    }

}
