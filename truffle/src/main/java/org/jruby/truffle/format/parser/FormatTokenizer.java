/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.format.parser;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.util.ByteList;

/**
 * Tokenizes a format expression into a stream of {@link String} and
 * {@link FormatDirective} objects.
 */
public class FormatTokenizer {

    private static final String TYPE_CHARS = "%-sdiuxXfgGeE";

    private final RubyContext context;
    private final ByteList format;
    private int position;

    /**
     * Construct a tokenizer.
     * @param format the pack expression
     */
    public FormatTokenizer(RubyContext context, ByteList format) {
        this.context = context;
        this.format = format;
    }

    public Object next() {
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

        Object key = null;

        if (format.charAt(position) == '{') {
            position++;
            key = readUntil('}');
            position++;
            return new FormatDirective(0, 0, false, 0, '{', key);
        } else if (format.charAt(position) == '<') {
            position++;
            key = readUntil('>');
            position++;
        }

        boolean leftJustified = false;

        if (position < format.length() && format.charAt(position) == '-') {
            leftJustified = true;
            position++;
        }

        int spacePadding;
        int zeroPadding;

        if (position < format.length() && format.charAt(position) == ' ') {
            position++;
            spacePadding = readInt();
            zeroPadding = FormatDirective.DEFAULT;
        } else if (position < format.length() && format.charAt(position) == '0') {
            spacePadding = FormatDirective.DEFAULT;
            zeroPadding = readInt();
        } else if (position < format.length() && Character.isDigit(format.charAt(position))) {
            spacePadding = readInt();
            zeroPadding = FormatDirective.DEFAULT;
        } else {
            spacePadding = FormatDirective.DEFAULT;

            if (position < format.length() && format.charAt(position) == '0') {
                position++;
                zeroPadding = readInt();
            } else {
                zeroPadding = FormatDirective.DEFAULT;
            }
        }

        final int precision;

        if (position < format.length() && format.charAt(position) == '.') {
            position++;
            precision = readInt();
        } else {
            precision = FormatDirective.DEFAULT;
        }

        if (position < format.length() && Character.isDigit(format.charAt(position))) {
            spacePadding = readInt();
        }

        char type;

        if (key != null && position >= format.length()) {
            type = 's';
        } else {
            type = format.charAt(position);

            if (key != null && Character.isWhitespace(type)) {
                type = 's';
            } else {
                if (TYPE_CHARS.indexOf(type) == -1) {
                    throw new UnsupportedOperationException("Unknown format type '" + format.charAt(position) + "'");
                }
            }

            position++;
        }


        return new FormatDirective(spacePadding, zeroPadding, leftJustified, precision, type, key);
    }

    private int readInt() {
        final int start = position;

        while (Character.isDigit(format.charAt(position))) {
            position++;
        }

        if (position == start && format.charAt(position) == '*') {
            position++;
            return FormatDirective.PADDING_FROM_ARGUMENT;
        }

        return Integer.parseInt(format.subSequence(start, position).toString());
    }

    private Object readUntil(char end) {
        final int start = position;

        while (format.charAt(position) != end) {
            position++;
        }

        return context.getSymbol((ByteList) format.subSequence(start, position));
    }

}
