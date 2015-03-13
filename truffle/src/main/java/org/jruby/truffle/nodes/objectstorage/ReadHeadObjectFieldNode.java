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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ReadHeadObjectFieldNode extends Node {

    private final Object name;
    @Child private ReadObjectFieldNode first;

    public ReadHeadObjectFieldNode(Object name) {
        this.name = name;
        first = new UninitializedReadObjectFieldNode(name);
    }

    public boolean executeBoolean(RubyBasicObject object) throws UnexpectedResultException {
        return first.executeBoolean(object);
    }

    public int executeInteger(RubyBasicObject object) throws UnexpectedResultException {
        return first.executeInteger(object);
    }

    public long executeLong(RubyBasicObject object) throws UnexpectedResultException {
        return first.executeLong(object);
    }

    public double executeDouble(RubyBasicObject object) throws UnexpectedResultException {
        return first.executeDouble(object);
    }

    public Object execute(RubyBasicObject object) {
        return first.execute(object);
    }

    public Object getName() {
        return name;
    }

    public boolean isSet(RubyBasicObject object) {
        return first.isSet(object);
    }

}
