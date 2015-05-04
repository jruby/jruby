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

import org.jruby.util.ByteList;

/**
 * Tokenizes a format expression into a stream of {@link String} and
 * {@link FormatDirective} objects.
 */
public class FormatTokenizer {

    private static final String TYPE_CHARS = "%sdiuxXfg";

    private final ByteList format;
    private int position;
    private Object peek;

    /**
     * Construct a tokenizer.
     * @param format the pack expression
     */
    public FormatTokenizer(ByteList format) {
        this.format = format;
    }

    public Object peek() {
        if (peek == null) {
            peek = next();
        }

        return peek;
    }

    public Object next() {
        if (peek != null) {
            final Object token = peek;
            peek = null;
            return token;
        }

        if (position >= format.length()) {
            return null;
        }

        final char c = format.charAt(position);

        if (c != '%') {
            final int stringStart = position;

            position++;

            while (position < format.length() && format.charAt(position) != '%') {
                position++;
            }

            return format.subSequence(stringStart, position);
        }

        position++;

        int spacePadding;
        int zeroPadding;

        if (format.charAt(position) == ' ') {
            position++;
            spacePadding = readInt();
            zeroPadding = FormatDirective.DEFAULT;
        } else {
            spacePadding = FormatDirective.DEFAULT;

            if (format.charAt(position) == '0') {
                position++;
                zeroPadding = readInt();
            } else {
                zeroPadding = FormatDirective.DEFAULT;
            }
        }

        final int precision;

        if (format.charAt(position) == '.') {
            position++;
            precision = readInt();
        } else {
            precision = FormatDirective.DEFAULT;
        }

        if (Character.isDigit(format.charAt(position))) {
            spacePadding = readInt();
        }

        final char type = format.charAt(position);

        if (TYPE_CHARS.indexOf(type) == -1) {
            throw new UnsupportedOperationException("Unknown format type '" + format.charAt(position) + "'");
        }

        position++;

        return new FormatDirective(spacePadding, zeroPadding, precision, type);
    }

    private int readInt() {
        final int start = position;

        while (Character.isDigit(format.charAt(position))) {
            position++;
        }

        return Integer.parseInt(format.subSequence(start, position).toString());
    }

}
