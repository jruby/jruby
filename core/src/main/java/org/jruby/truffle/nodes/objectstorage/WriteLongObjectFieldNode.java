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
import org.jruby.truffle.runtime.objectstorage.LongStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class WriteLongObjectFieldNode extends WriteSpecializedObjectFieldNode {

    private final LongStorageLocation storageLocation;

    public WriteLongObjectFieldNode(String name, ObjectLayout objectLayout, LongStorageLocation storageLocation, RespecializeHook hook) {
        super(name, objectLayout, hook);
        this.storageLocation = storageLocation;
    }

    @Override
    public void execute(ObjectStorage object, long value) {
        final ObjectLayout actualLayout = object.getObjectLayout();

        if (actualLayout == objectLayout) {
            storageLocation.writeLong(object, value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value, "layout changed");
        }
    }

    @Override
    public void execute(ObjectStorage object, Object value) {
        if (value instanceof Long) {
            execute(object, (long) value);
        } else {
            CompilerDirectives.transferToInterpreter();
            writeAndRespecialize(object, value, "unexpected value type");
        }
    }

}
