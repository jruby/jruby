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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StringArrayOptionDescription extends OptionDescription {

    private final String[] defaultValue;

    public StringArrayOptionDescription(String name, String description, String[] defaultValue) {
        super(name, description);
        this.defaultValue = defaultValue;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue.clone();
    }

    @Override
    public Object checkValue(Object value) {
        if (value instanceof String[]) {
            return value;
        } else if (value instanceof Collection<?>) {
            final Collection<?> collection = (Collection<?>) value;
            final String[] strings = new String[collection.size()];
            int n = 0;
            for (Object item : collection) {
                strings[n] = item.toString();
                n++;
            }
            return strings;
        } else if (value instanceof String) {
            return parseStringArray((String) value);
        } else {
            throw new OptionTypeException(getName(), value.toString());
        }
    }

    // Allows input such as foo,bar,baz. You can escape commas.

    private String[] parseStringArray(String string) {
        final List<String> values = new ArrayList<>();

        final int startOfString = 0;
        final int withinString = 1;
        final int escape = 2;

        int state = startOfString;

        final StringBuilder builder = new StringBuilder();

        for (int n = 0; n < string.length(); n++) {
            switch (state) {
                case startOfString:
                    builder.setLength(0);
                    builder.append(string.charAt(n));
                    state = withinString;
                    break;

                case withinString:
                    switch (string.charAt(n)) {
                        case ',':
                            values.add(builder.toString());
                            state = startOfString;
                            break;

                        case '\\':
                            state = escape;
                            break;

                        default:
                            builder.append(string.charAt(n));
                            break;
                    }
                    break;

                case escape:
                    if (string.charAt(n) != ',') {
                        throw new OptionTypeException(getName(), string);
                    }
                    state = withinString;
                    builder.append(string.charAt(n));
                    break;
            }
        }

        switch (state) {
            case withinString:
                values.add(builder.toString());
                break;

            case escape:
                throw new OptionTypeException(getName(), string);
        }

        return values.toArray(new String[values.size()]);
    }

    @Override
    public String toString(Object value) {
        return String.join(",", (String[]) value);
    }

}
