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

import org.jruby.truffle.pack.runtime.exceptions.FormatException;

/**
 * Tokenizes a pack format expression into a stream of objects. All tokens
 * are represented as {@link Character} objects except for count tokens which
 * are {@link Integer}.
 */
public class PackTokenizer {

    private static final String SIMPLE_TOKENS = "CSLIQcsliqnNvVAaZUXx*<>!_@DdFfEeGgPpHhMmuwBb";

    private final String format;
    private final boolean extended;
    private int position;
    private Object peek;

    /**
     * Construct a tokenizer.
     * @param format the pack expression
     * @param extended whether to support the extended format with parens
     */
    public PackTokenizer(String format, boolean extended) {
        this.format = format;
        this.extended = extended;
    }

    /**
     * Peeks at the next token and returns if it is a given character.
     */
    public boolean peek(char c) {
        final Object peek = peek();

        if (peek == null) {
            return false;
        }

        return peek.equals(c);
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

        consumeWhitespace();

        if (position >= format.length()) {
            return null;
        }

        final char c = format.charAt(position);

        if (c == '%') {
            throw new FormatException("% is not supported");
        }

        final String chars;

        if (extended) {
            chars = SIMPLE_TOKENS + "()";
        } else {
            chars = SIMPLE_TOKENS;
        }

        if (chars.indexOf(c) > -1) {
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
        while (position < format.length()) {
            char c = format.charAt(position);

            if (c == '#') {
                position++;

                while (position < format.length()) {
                    c = format.charAt(position);

                    if (c == '\r' || c == '\n') {
                        break;
                    }

                    position++;
                }
            } else if (!Character.isWhitespace(c) && c != 0) {
                break;
            }

            position++;
        }
    }

}
