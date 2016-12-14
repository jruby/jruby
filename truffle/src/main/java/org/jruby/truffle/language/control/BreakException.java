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

public final class BreakException extends ControlFlowException {

    private static final long serialVersionUID = -8260344464830705773L;

    private final BreakID breakID;
    private final Object result;

    public BreakException(BreakID breakID, Object result) {
        this.breakID = breakID;
        this.result = result;
    }

    public BreakID getBreakID() {
        return breakID;
    }

    public Object getResult() {
        return result;
    }

}
