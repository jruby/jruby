/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.runtime.DebugOperations;
import org.jruby.truffle.runtime.RubyContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StringFormatter {

    @CompilerDirectives.TruffleBoundary
    public static String format(RubyContext context, String format, List<Object> values) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

        final PrintStream printStream;

        try {
            printStream = new PrintStream(byteArray, false, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        format(context, printStream, format, values);

        try {
            return byteArray.toString(StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @CompilerDirectives.TruffleBoundary
    public static void format(RubyContext context, PrintStream stream, String format, List<Object> values) {
        /*
         * See http://www.ruby-doc.org/core-1.9.3/Kernel.html#method-i-sprintf.
         * 
         * At the moment we just do the basics that we need. We will need a proper lexer later on.
         * Or better than that we could compile to Truffle nodes if the format string is constant! I
         * don't think we can easily translate to Java's format syntax, otherwise JRuby would do
         * that and they don't.
         */

        int n = 0;
        int v = 0;

        int lengthModifier;

        while (n < format.length()) {
            final char c = format.charAt(n);
            n++;

            if (c == '%') {
                // %[flags][width][.precision]type

                final String flagChars = "0";

                boolean zeroPad = false;

                while (n < format.length() && flagChars.indexOf(format.charAt(n)) != -1) {
                    switch (format.charAt(n)) {
                        case '0':
                            zeroPad = true;
                            break;
                        default:
                            break;
                    }

                    n++;
                }

                int width;

                if (n < format.length() && Character.isDigit(format.charAt(n))) {
                    final int widthStart = n;

                    while (Character.isDigit(format.charAt(n))) {
                        n++;
                    }

                    width = Integer.parseInt(format.substring(widthStart, n));
                } else {
                    width = 0;
                }

                int precision;

                if (format.charAt(n) == '.') {
                    n++;

                    final int precisionStart = n;

                    while (Character.isDigit(format.charAt(n))) {
                        n++;
                    }

                    precision = Integer.parseInt(format.substring(precisionStart, n));
                } else {
                    precision = 5;
                }

                if (format.charAt(n) == ' ') {
                    n++;
                    final int lengthStart = n;

                    while (Character.isDigit(format.charAt(n))) {
                        n++;
                    }

                    lengthModifier = Integer.parseInt(format.substring(lengthStart, n));
                } else {
                    lengthModifier = 0;
                }

                final char type = format.charAt(n);
                n++;

                final StringBuilder formatBuilder = new StringBuilder();

                formatBuilder.append("%");

                if (width > 0) {
                    if (zeroPad) {
                        formatBuilder.append("0");
                    }

                    formatBuilder.append(width);
                }

                switch (type) {
                    case '%': {
                        stream.print("%");
                        break;
                    }
                    case 's': {
                        formatBuilder.append("s");
                        stream.printf(formatBuilder.toString(), DebugOperations.send(context, values.get(v), "to_s", null));
                        break;
                    }

                    case 'x':
                    case 'X':
                    case 'd': {
                        if (lengthModifier != 0) {
                            formatBuilder.append(" ");
                            formatBuilder.append(lengthModifier);
                        }

                        formatBuilder.append(type);
                        final Object value = values.get(v);
                        final long longValue;
                        if (value instanceof Integer) {
                            longValue = (int) value;
                        } else if (value instanceof Long) {
                            longValue = (long) value;
                        } else {
                            throw new UnsupportedOperationException();
                        }

                        stream.printf(formatBuilder.toString(), longValue);
                        break;
                    }

                    case 'f': {
                        formatBuilder.append(".");
                        formatBuilder.append(precision);
                        formatBuilder.append("f");
                        final double value = CoreLibrary.toDouble(values.get(v));
                        stream.printf(formatBuilder.toString(), value);
                        break;
                    }

                    default:
                        throw new RuntimeException("Kernel#sprintf error -- unknown format: " + type);
                }

                // Showing a literal '%' would not reference any particular value, so don't increment the value lookup index.
                if (type != '%') {
                    v++;
                }
            } else {
                stream.print(c);
            }
        }
    }
}
