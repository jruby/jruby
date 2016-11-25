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
        } else if (value instanceof String) {
            return parseStringArray((String) value);
        } else {
            throw new OptionTypeException();
        }
    }

    // Allows input such as [foo, "bar", 'baz']. Doesn't support escape sequences.

    private String[] parseStringArray(String string) {
        final List<String> values = new ArrayList<>();

        final int start = 0;
        final int startOfString = 1;
        final int endOfString = 2;
        final int endOfArray = 3;

        int n = 0;
        int state = start;
        boolean array = false;

        while (n < string.length()) {
            while (n < string.length() && Character.isWhitespace(string.charAt(n))) {
                n++;
            }

            if (n == string.length() && array && state != start && state != endOfArray) {
                throw new OptionTypeException();
            }

            switch (state) {
                case start:
                    if (string.charAt(n) == '[') {
                        n++;
                        array = true;
                        state = startOfString;
                    } else {
                        array = false;
                        state = startOfString;
                    }
                    break;

                case startOfString:
                    final int startN;
                    final int endN;
                    if (string.charAt(n) == '"' || string.charAt(n) == '\'') {
                        final char quote = string.charAt(n);

                        n++;
                        startN = n;

                        while (n < string.length() && string.charAt(n) != quote) {
                            n++;
                        }

                        endN = n;

                        if (string.charAt(n) == quote){
                            n++;
                        } else {
                            throw new OptionTypeException();
                        }

                        state = endOfString;
                    } else {
                        startN = n;

                        while (n < string.length() && string.charAt(n) != ',') {
                            n++;
                        }

                        endN = n;

                        state = endOfString;
                    }
                    values.add(string.substring(startN, endN));
                    break;

                case endOfString:
                    if (string.charAt(n) == ',') {
                        n++;
                        state = startOfString;
                    } else if (array && string.charAt(n) == ']') {
                        n++;
                        state = endOfArray;
                    } else {
                        throw new OptionTypeException();
                    }
                    break;

                case endOfArray:
                    break;
            }
        }

        return values.toArray(new String[values.size()]);
    }

}
