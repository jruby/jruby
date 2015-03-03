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
import org.jruby.truffle.runtime.core.RubyBasicObject;

public abstract class WriteObjectFieldNode extends Node {

    public abstract void execute(RubyBasicObject object, Object value);

    public void execute(RubyBasicObject object, boolean value) {
        execute(object, (Object) value);
    }

    public void execute(RubyBasicObject object, int value) {
        execute(object, (Object) value);
    }

    public void execute(RubyBasicObject object, long value) {
        execute(object, (Object) value);
    }

    public void execute(RubyBasicObject object, double value) {
        execute(object, (Object) value);
    }

}
