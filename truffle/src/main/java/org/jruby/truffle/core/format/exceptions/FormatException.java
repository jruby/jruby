/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.format.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

public class FormatException extends ControlFlowException {

    private static final long serialVersionUID = -6570764260422083237L;

    private final String message;

    public FormatException() {
        message = null;
    }

    public FormatException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
