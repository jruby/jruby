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

public abstract class OptionDescription {

    private final String name;
    private final String description;

    public OptionDescription(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public abstract Object getDefaultValue();

    public abstract Object checkValue(Object value);

    public String toString(Object value) {
        if (value == null) {
            return "null";
        } else {
            return value.toString();
        }
    }

}
