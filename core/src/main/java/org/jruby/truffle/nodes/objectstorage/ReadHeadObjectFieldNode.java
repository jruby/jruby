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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.objectstorage.ObjectStorage;

public class ReadHeadObjectFieldNode extends Node {

    private final String name;
    @Child protected ReadObjectFieldNode first;

    public ReadHeadObjectFieldNode(String name, RespecializeHook hook) {
        this.name = name;
        first = new UninitializedReadObjectFieldNode(name, hook);
    }

    public int executeInteger(ObjectStorage object) throws UnexpectedResultException {
        return first.executeInteger(object);
    }

    public long executeLong(ObjectStorage object) throws UnexpectedResultException {
        return first.executeLong(object);
    }

    public double executeDouble(ObjectStorage object) throws UnexpectedResultException {
        return first.executeDouble(object);
    }

    public Object execute(ObjectStorage object) {
        return first.execute(object);
    }

    public String getName() {
        return name;
    }

    public boolean isSet(ObjectStorage object) {
        return first.isSet(object);
    }

}
