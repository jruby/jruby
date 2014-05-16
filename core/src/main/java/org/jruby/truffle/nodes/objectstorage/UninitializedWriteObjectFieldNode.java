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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.runtime.objectstorage.*;

@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedWriteObjectFieldNode extends WriteObjectFieldNode {

    private final String name;
    private final RespecializeHook hook;

    public UninitializedWriteObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        this.hook = hook;
    }

    @Override
    public void execute(ObjectStorage object, Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        hook.hookWrite(object, name, value);

        final ObjectLayout layout = object.getObjectLayout();
        final StorageLocation storageLocation = layout.findStorageLocation(name);

        WriteObjectFieldNode newNode;

        if (storageLocation == null) {
            throw new RuntimeException("Storage location should be found at this point");
        } else if (storageLocation instanceof IntegerStorageLocation) {
            newNode = new WriteIntegerObjectFieldNode(layout, (IntegerStorageLocation) storageLocation, this);
        } else if (storageLocation instanceof LongStorageLocation) {
            newNode = new WriteLongObjectFieldNode(layout, (LongStorageLocation) storageLocation, this);
        } else if (storageLocation instanceof DoubleStorageLocation) {
            newNode = new WriteDoubleObjectFieldNode(layout, (DoubleStorageLocation) storageLocation, this);
        } else {
            newNode = new WriteObjectObjectFieldNode(layout, (ObjectStorageLocation) storageLocation, this);
        }

        replace(newNode, "adding new write object field node to chain");
        newNode.execute(object, value);
    }

}
