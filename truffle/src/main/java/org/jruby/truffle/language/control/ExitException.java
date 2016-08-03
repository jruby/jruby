/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.control;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ExitException extends ControlFlowException {

    private static final long serialVersionUID = 8152389017577849952L;

    private final int code;

    public ExitException(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
