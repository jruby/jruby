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
import org.jruby.truffle.runtime.objectstorage.DoubleStorageLocation;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class ReadDoubleObjectFieldNode extends ReadSpecializedObjectFieldNode {

    private final DoubleStorageLocation storageLocation;

    public ReadDoubleObjectFieldNode(String name, ObjectLayout objectLayout, DoubleStorageLocation storageLocation, RespecializeHook hook) {
        super(name, objectLayout, hook);
        this.storageLocation = storageLocation;
    }

    @Override
    public double executeDouble(ObjectStorage object) throws UnexpectedResultException {
        final ObjectLayout receiverLayout = object.getObjectLayout();

        final boolean condition = receiverLayout == objectLayout;

        if (condition) {
            assert receiverLayout != null;

            return storageLocation.readDouble(object, condition);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(readAndRespecialize(object));
        }
    }

    @Override
    public Object execute(ObjectStorage object) {
        try {
            return executeDouble(object);
        } catch (UnexpectedResultException e) {
            return e.getResult();
        }
    }

}
