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
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public abstract class ReadObjectFieldNode extends Node {

    public abstract Object execute(RubyBasicObject object);

    public boolean executeBoolean(RubyBasicObject object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Boolean) {
            return (boolean) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public int executeInteger(RubyBasicObject object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Integer) {
            return (int) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public long executeLong(RubyBasicObject object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Long) {
            return (long) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public double executeDouble(RubyBasicObject object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Double) {
            return (double) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public abstract boolean isSet(RubyBasicObject object);

}
