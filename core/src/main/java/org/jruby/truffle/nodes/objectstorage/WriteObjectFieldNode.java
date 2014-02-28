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

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.objectstorage.*;

public abstract class WriteObjectFieldNode extends Node {

    private final String name;
    private final RespecializeHook hook;

    public WriteObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        this.hook = hook;
    }

    public abstract void execute(ObjectStorage object, Object value);

    public void execute(ObjectStorage object, int value) {
        execute(object, (Object) value);
    }

    public void execute(ObjectStorage object, double value) {
        execute(object, (Object) value);
    }

    public void writeAndRespecialize(ObjectStorage object, Object value) {
        hook.hookWrite(object, name, value);

        final ObjectLayout layout = object.getObjectLayout();
        final StorageLocation storageLocation = layout.findStorageLocation(name);

        WriteObjectFieldNode newNode;

        if (storageLocation == null) {
            throw new RuntimeException("Storage location should be found at this point");
        } else if (storageLocation instanceof IntegerStorageLocation) {
            newNode = new WriteIntegerObjectFieldNode(name, layout, (IntegerStorageLocation) storageLocation, hook);
        } else if (storageLocation instanceof DoubleStorageLocation) {
            newNode = new WriteDoubleObjectFieldNode(name, layout, (DoubleStorageLocation) storageLocation, hook);
        } else {
            newNode = new WriteObjectObjectFieldNode(name, layout, (ObjectStorageLocation) storageLocation, hook);
        }

        replace(newNode);
        newNode.execute(object, value);
    }

    public String getName() {
        return name;
    }

}
