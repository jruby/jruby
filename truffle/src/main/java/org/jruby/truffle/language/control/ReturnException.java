/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ReturnException extends ControlFlowException {

    private static final long serialVersionUID = -45053969587014940L;

    private final ReturnID returnID;
    private final Object value;

    public ReturnException(ReturnID returnID, Object value) {
        this.returnID = returnID;
        this.value = value;
    }

    public ReturnID getReturnID() {
        return returnID;
    }

    public Object getValue() {
        return value;
    }

}
