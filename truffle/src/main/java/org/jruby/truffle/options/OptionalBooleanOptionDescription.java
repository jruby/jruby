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

public class OptionalBooleanOptionDescription extends OptionDescription {

    private final OptionalBoolean defaultValue;

    public OptionalBooleanOptionDescription(String name, String description, OptionalBoolean defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    @Override
    public OptionalBoolean getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Object checkValue(Object value) {
        if (value == null) {
            return OptionalBoolean.NULL;
        } else if (value instanceof Boolean) {
            if ((boolean) value) {
                return OptionalBoolean.TRUE;
            } else {
                return OptionalBoolean.FALSE;
            }
        } else if (value instanceof String) {
            switch ((String) value) {
                case "true":
                    return OptionalBoolean.TRUE;
                case "false":
                    return OptionalBoolean.FALSE;
                default:
                    throw new OptionTypeException(getName(), value.toString());
            }
        } else {
            throw new OptionTypeException(getName(), value.toString());
        }
    }

}
