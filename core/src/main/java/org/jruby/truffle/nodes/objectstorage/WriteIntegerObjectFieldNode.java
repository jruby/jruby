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

import org.jruby.truffle.runtime.objectstorage.IntegerStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class WriteIntegerObjectFieldNode extends WriteObjectFieldChainNode {

    private final ObjectLayout objectLayout;
    private final IntegerStorageLocation storageLocation;

    public WriteIntegerObjectFieldNode(ObjectLayout objectLayout, IntegerStorageLocation storageLocation, WriteObjectFieldNode next) {
        super(next);
        this.objectLayout = objectLayout;
        this.storageLocation = storageLocation;
    }

    @Override
    public void execute(ObjectStorage object, int value) {
        if (object.getObjectLayout() == objectLayout) {
            storageLocation.writeInteger(object, value);
        } else {
            next.execute(object, value);
        }
    }

    @Override
    public void execute(ObjectStorage object, Object value) {
        if (value instanceof Integer) {
            execute(object, (int) value);
        } else {
            next.execute(object, value);
        }
    }

}
