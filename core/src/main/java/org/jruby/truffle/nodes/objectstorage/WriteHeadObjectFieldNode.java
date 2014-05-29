/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objectstorage;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class WriteHeadObjectFieldNode extends Node {

    private final String name;
    @Child protected WriteObjectFieldNode first;

    public WriteHeadObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        first = new UninitializedWriteObjectFieldNode(name, hook);
    }

    public void execute(ObjectStorage object, int value) {
        first.execute(object, value);
    }

    public void execute(ObjectStorage object, long value) {
        first.execute(object, value);
    }

    public void execute(ObjectStorage object, double value) {
        first.execute(object, value);
    }

    public void execute(ObjectStorage object, Object value) {
        first.execute(object, value);
    }

    public String getName() {
        return name;
    }

}
