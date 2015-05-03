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

        final char type = format.charAt(position);
        position++;

        return new FormatDirective(type);
    }

}
