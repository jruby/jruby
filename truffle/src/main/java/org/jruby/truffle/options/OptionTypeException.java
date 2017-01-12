/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.options;

public class OptionTypeException extends UnsupportedOperationException {

    private static final long serialVersionUID = 9479324724903L;

    private final String name;

    public OptionTypeException(String name, String value) {
        super(String.format("Unsupported value '%s' for option %s", value, name));
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
