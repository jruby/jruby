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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

public class JavaException extends RuntimeException {

    private static final long serialVersionUID = -5710714298554437748L;

    public JavaException(Throwable cause) {
        super(doGetMessage(cause), cause);
    }

    @TruffleBoundary
    private static String doGetMessage(Throwable cause) {
        return cause.getMessage();
    }

    @SuppressWarnings("sync-override")
    @Override
    public final Throwable fillInStackTrace() {
        return null;
    }

}
