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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.objectstorage.*;

public abstract class ReadObjectFieldNode extends Node {

    private final String name;
    private final RespecializeHook hook;

    public ReadObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        this.hook = hook;
    }

    public abstract Object execute(ObjectStorage object);

    public int executeInteger(ObjectStorage object) throws UnexpectedResultException {
        throw new UnexpectedResultException(execute(object));
    }

    public double executeDouble(ObjectStorage object) throws UnexpectedResultException {
        throw new UnexpectedResultException(execute(object));
    }

    public Object readAndRespecialize(ObjectStorage object) {
        hook.hookRead(object, name);

        final ObjectLayout layout = object.getObjectLayout();
        final StorageLocation storageLocation = layout.findStorageLocation(name);

        ReadObjectFieldNode newNode;

        if (storageLocation == null) {
            newNode = new ReadMissingObjectFieldNode(name, layout, hook);
        } else if (storageLocation instanceof IntegerStorageLocation) {
            newNode = new ReadIntegerObjectFieldNode(name, layout, (IntegerStorageLocation) storageLocation, hook);
        } else if (storageLocation instanceof DoubleStorageLocation) {
            newNode = new ReadDoubleObjectFieldNode(name, layout, (DoubleStorageLocation) storageLocation, hook);
        } else {
            newNode = new ReadObjectObjectFieldNode(name, layout, (ObjectStorageLocation) storageLocation, hook);
        }

        replace(newNode);
        return newNode.execute(object);
    }

    public String getName() {
        return name;
    }
}
