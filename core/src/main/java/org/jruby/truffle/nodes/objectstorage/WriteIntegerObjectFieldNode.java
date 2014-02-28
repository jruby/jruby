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
import org.jruby.truffle.runtime.objectstorage.IntegerStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class WriteIntegerObjectFieldNode extends WriteSpecializedObjectFieldNode {

    private final IntegerStorageLocation storageLocation;

    public WriteIntegerObjectFieldNode(String name, ObjectLayout objectLayout, IntegerStorageLocation storageLocation, RespecializeHook hook) {
        super(name, objectLayout, hook);
        this.storageLocation = storageLocation;
    }

    @Override
    public void execute(ObjectStorage object, int value) {
        final ObjectLayout actualLayout = object.getObjectLayout();

        if (actualLayout == objectLayout) {
            storageLocation.writeInteger(object, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value);
        }
    }

    @Override
    public void execute(ObjectStorage object, Object value) {
        if (value instanceof Integer) {
            execute(object, (int) value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value);
        }
    }

}
