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
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.objectstorage.ObjectLayout;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class ReadMissingObjectFieldNode extends ReadSpecializedObjectFieldNode {

    public ReadMissingObjectFieldNode(String name, ObjectLayout objectLayout, RespecializeHook hook) {
        super(name, objectLayout, hook);
    }

    @Override
    public Object execute(ObjectStorage object) {
        if (!object.getObjectLayout().contains(objectLayout)) {
            CompilerDirectives.transferToInterpreter();
            return readAndRespecialize(object);
        }

        return NilPlaceholder.INSTANCE;
    }
}
