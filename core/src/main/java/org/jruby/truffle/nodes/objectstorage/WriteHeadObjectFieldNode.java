/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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

public class WriteHeadObjectFieldNode extends Node {

    private final Object name;
    @Child private WriteObjectFieldNode first;

    public WriteHeadObjectFieldNode(Object name) {
        this.name = name;
        first = new UninitializedWriteObjectFieldNode(name);
    }

    public void execute(RubyBasicObject object, int value) {
        first.execute(object, value);
    }

    public void execute(RubyBasicObject object, long value) {
        first.execute(object, value);
    }

    public void execute(RubyBasicObject object, double value) {
        first.execute(object, value);
    }

    public void execute(RubyBasicObject object, Object value) {
        first.execute(object, value);
    }

    public Object getName() {
        return name;
    }

}
