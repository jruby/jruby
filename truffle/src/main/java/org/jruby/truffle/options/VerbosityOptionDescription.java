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

public class VerbosityOptionDescription extends OptionDescription {

    private final Verbosity defaultValue;

    public VerbosityOptionDescription(String name, String description, Verbosity defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    @Override
    public Verbosity getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Object checkValue(Object value) {
        if (value == null) {
            return Verbosity.NIL;
        } else if (value instanceof Boolean) {
            if ((boolean) value) {
                return Verbosity.TRUE;
            } else {
                return Verbosity.FALSE;
            }
        } else if (value instanceof Integer) {
            switch ((int) value) {
                case 0:
                    return Verbosity.NIL;
                case 1:
                    return Verbosity.FALSE;
                case 2:
                    return Verbosity.TRUE;
                default:
                    throw new OptionTypeException(getName(), value.toString());
            }
        } else if (value instanceof String) {
            switch ((String) value) {
                case "nil":
                    return Verbosity.NIL;
                case "false":
                    return Verbosity.FALSE;
                case "true":
                    return Verbosity.TRUE;
                case "0":
                    return Verbosity.NIL;
                case "1":
                    return Verbosity.FALSE;
                case "2":
                    return Verbosity.TRUE;
                default:
                    throw new OptionTypeException(getName(), value.toString());
            }
        } else if (value instanceof Verbosity) {
            return value;
        } else {
            throw new OptionTypeException(getName(), value.toString());
        }
    }

}
