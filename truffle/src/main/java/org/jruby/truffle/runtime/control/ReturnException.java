/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.control;

import com.oracle.truffle.api.nodes.ControlFlowException;
import org.jruby.truffle.runtime.ReturnID;

/**
 * Controls an explicit return from a method.
 */
public final class ReturnException extends ControlFlowException {

    private final ReturnID returnID;
    private final Object value;

    public ReturnException(ReturnID returnID, Object value) {
        this.returnID = returnID;
        this.value = value;
    }

    /**
     * Return the return ID of this return that identifies where it intends to return to.
     */
    public ReturnID getReturnID() {
        return returnID;
    }

    /**
     * Get the value that has been returned.
     */
    public Object getValue() {
        return value;
    }

    private static final long serialVersionUID = -9177536212065610691L;

}
