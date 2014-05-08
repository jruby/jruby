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

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.objectstorage.*;

public abstract class ReadObjectFieldNode extends Node {

    public abstract Object execute(ObjectStorage object);

    public int executeInteger(ObjectStorage object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Integer) {
            return (int) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public long executeLong(ObjectStorage object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Long) {
            return (long) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public double executeDouble(ObjectStorage object) throws UnexpectedResultException {
        Object result = execute(object);

        if (result instanceof Double) {
            return (double) result;
        } else {
            throw new UnexpectedResultException(result);
        }
    }

    public boolean isSet(ObjectStorage object) {
        return true;
    }

}
