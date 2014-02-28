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
import org.jruby.truffle.runtime.objectstorage.DoubleStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class WriteDoubleObjectFieldNode extends WriteSpecializedObjectFieldNode {

    private final DoubleStorageLocation storageLocation;

    public WriteDoubleObjectFieldNode(String name, ObjectLayout objectLayout, DoubleStorageLocation storageLocation, RespecializeHook hook) {
        super(name, objectLayout, hook);
        this.storageLocation = storageLocation;
    }

    @Override
    public void execute(ObjectStorage object, double value) {
        final ObjectLayout actualLayout = object.getObjectLayout();

        if (actualLayout == objectLayout) {
            storageLocation.writeDouble(object, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value);
        }
    }

    @Override
    public void execute(ObjectStorage object, Object value) {
        if (value instanceof Double) {
            execute(object, (double) value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value);
        }
    }

}
