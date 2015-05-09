/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.runtime.exceptions;

public class WrongArgumentTypeException extends PackException {

    private final String got;
    private final String expected;

    public WrongArgumentTypeException(String got, String expected) {
        this.got = got;
        this.expected = expected;
    }

    public String getGot() {
        return got;
    }

    public String getExpected() {
        return expected;
    }

}
