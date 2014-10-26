/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.lexer.yacc;

public class DetailedSourcePosition extends SimpleSourcePosition {

    final int offset;
    final int length;

    public DetailedSourcePosition(String filename, int line, int offset, int length) {
        super(filename, line);
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return String.format("%s:%d:%d:%d", getFile(), getLine() + 1, offset, length);
    }

}
