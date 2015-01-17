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

/**
 * Controls a break from a control structure or method.
 */
public final class BreakException extends ControlFlowException {

    private final Object result;

    public BreakException(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }

    private static final long serialVersionUID = -8650123232850256133L;

}
