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

public class BooleanOptionDescription extends OptionDescription {

    private final boolean defaultValue;

    public BooleanOptionDescription(String name, String description, boolean defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Object checkValue(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof Boolean) {
            return value;
        } else if (value instanceof String) {
            switch ((String) value) {
                case "true":
                    return true;
                case "false":
                    return false;
                default:
                    throw new OptionTypeException(getName(), value.toString());
            }
        } else {
            throw new OptionTypeException(getName(), value.toString());
        }
    }

}
