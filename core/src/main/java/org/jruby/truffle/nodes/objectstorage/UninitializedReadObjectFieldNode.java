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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.objectstorage.*;

public class UninitializedReadObjectFieldNode extends ReadObjectFieldNode {

    private final String name;
    private final RespecializeHook hook;

    public UninitializedReadObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        this.hook = hook;
    }

    @Override
    public Object execute(ObjectStorage object) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        hook.hookRead(object, name);

        final ObjectLayout layout = object.getObjectLayout();
        final StorageLocation storageLocation = layout.findStorageLocation(name);

        ReadObjectFieldNode newNode;

        if (storageLocation == null) {
            newNode = new ReadMissingObjectFieldNode(layout, new UninitializedReadObjectFieldNode(name, hook));
        } else if (storageLocation instanceof IntegerStorageLocation) {
            newNode = new ReadIntegerObjectFieldNode(layout, (IntegerStorageLocation) storageLocation, new UninitializedReadObjectFieldNode(name, hook));
        } else if (storageLocation instanceof LongStorageLocation) {
            newNode = new ReadLongObjectFieldNode(layout, (LongStorageLocation) storageLocation, new UninitializedReadObjectFieldNode(name, hook));
        } else if (storageLocation instanceof DoubleStorageLocation) {
            newNode = new ReadDoubleObjectFieldNode(layout, (DoubleStorageLocation) storageLocation, new UninitializedReadObjectFieldNode(name, hook));
        } else {
            newNode = new ReadObjectObjectFieldNode(layout, (ObjectStorageLocation) storageLocation, new UninitializedReadObjectFieldNode(name, hook));
        }

        replace(newNode, "adding new read object field node to chain");
        return newNode.execute(object);
    }

    @Override
    public boolean isSet(ObjectStorage object) {
        return object.getObjectLayout().findStorageLocation(name) != null;

    }

}
