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
import org.jruby.util.ByteList;

/**
 * Tokenizes a pack format expression into a stream of objects. All tokens
 * are represented as {@link Character} objects except for count tokens which
 * are {@link Integer}.
 */
public class FormatTokenizer {

    private static final String SIMPLE_TOKENS = "CSLIQcsliqnNvVAaZUXx*<>!_@DdFfEeGgPpHhMmuwBb";

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

        if (position >= format.length()) {
            return null;
        }

        final char c = format.charAt(position);

        if (c == '%') {
            position++;
            return c;
        }

        final int stringStart = position;

        position++;

        while (position < format.length() && format.charAt(position) != '%') {
            position++;
        }

        return (ByteList) format.subSequence(stringStart, position);
    }

}
