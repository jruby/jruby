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

public class NoImplicitConversionException extends FormatException {

    private static final long serialVersionUID = -2509958825294561087L;

    private final Object object;
    private final String target;

    public NoImplicitConversionException(Object object, String target) {
        this.object = object;
        this.target = target;
    }

    public Object getObject() {
        return object;
    }

    public String getTarget() {
        return target;
    }

}
