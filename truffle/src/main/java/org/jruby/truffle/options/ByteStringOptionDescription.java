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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ByteStringOptionDescription extends OptionDescription {

    private final byte[] defaultValue;

    public ByteStringOptionDescription(String name, String description, byte[] defaultValue) {
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
            return null;
        } else if (value instanceof String) {
            return ((String) value).getBytes(Charset.defaultCharset());
        } else if (value instanceof byte[]) {
            return value;
        } else {
            throw new OptionTypeException(getName(), value.toString());
        }
    }

    @Override
    public String toString(Object value) {
        return new String((byte[]) value, StandardCharsets.US_ASCII);
    }

}
