/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.pack.parser;

public class PackTokenizer {

    private final String format;
    private int position;

    public PackTokenizer(String format) {
        this.format = format;
    }

    public Object next() {
        consumeWhitespace();

        if (position == format.length()) {
            return null;
        }

        final char c = format.charAt(position);

        if ("NLXx*".indexOf(c) > -1) {
            position++;
            return c;
        }

        if (Character.isDigit(c)) {
            final int start = position;
            position++;
            while (position < format.length() && Character.isDigit(format.charAt(position))) {
                position++;
            }
            return Integer.parseInt(format.substring(start, position));
        }


        throw new UnsupportedOperationException(String.format("unexpected token %c", c));
    }

    private void consumeWhitespace() {
        while (position < format.length() && format.charAt(position) == ' ') {
            position++;
        }
    }

}
