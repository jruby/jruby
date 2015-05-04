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

/*
 * A single format directive from a printf-style format string.
 *
 * %[space padding][zero padding][.precision]type
 */
public class FormatDirective {

    public static final int DEFAULT = -1;

    private final int spacePadding;
    private final int zeroPadding;
    private final int precision;
    private final char type;

    public FormatDirective(int spacePadding, int zeroPadding, int precision, char type) {
        this.spacePadding = spacePadding;
        this.zeroPadding = zeroPadding;
        this.precision = precision;
        this.type = type;
    }

    public int getSpacePadding() {
        return spacePadding;
    }

    public int getZeroPadding() {
        return zeroPadding;
    }

    public int getPrecision() {
        return precision;
    }

    public char getType() {
        return type;
    }

}
