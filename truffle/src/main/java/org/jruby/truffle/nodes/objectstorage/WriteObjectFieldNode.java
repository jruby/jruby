/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

public abstract class WriteObjectFieldNode extends Node {

    public abstract void execute(DynamicObject object, Object value);

    public void execute(DynamicObject object, boolean value) {
        execute(object, (Object) value);
    }

    public void execute(DynamicObject object, int value) {
        execute(object, (Object) value);
    }

    public void execute(DynamicObject object, long value) {
        execute(object, (Object) value);
    }

    public void execute(DynamicObject object, double value) {
        execute(object, (Object) value);
    }

}
