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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;

public class RaiseException extends ControlFlowException {

    private static final long serialVersionUID = -4128190563044417424L;

    private final DynamicObject exception;

    public RaiseException(DynamicObject exception) {
        this.exception = exception;
    }

    public DynamicObject getException() {
        return exception;
    }

    @Override
    @TruffleBoundary
    public String getMessage() {
        Object message = Layouts.EXCEPTION.getMessage(exception);
        if (message != null) {
            return message.toString();
        } else {
            return null;
        }
    }

}
