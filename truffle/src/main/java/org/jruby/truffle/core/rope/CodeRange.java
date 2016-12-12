/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 *
 * Some of the code in this class is modified from org.jruby.util.StringSupport,
 * licensed under the same EPL1.0/GPL 2.0/LGPL 2.1 used throughout.
 */

package org.jruby.truffle.core.rope;

public enum CodeRange {
    CR_UNKNOWN(0),
    CR_7BIT(1),
    CR_VALID(2),
    CR_BROKEN(3);

    private final int value;

    CodeRange(int value) {
        this.value = value;
    }

    public int toInt() {
        return value;
    }

    public static CodeRange fromInt(int codeRange) {
        switch(codeRange) {
            case 0: return CR_UNKNOWN;
            case 1: return CR_7BIT;
            case 2: return CR_VALID;
            case 3: return CR_BROKEN;
            default: throw new UnsupportedOperationException("Don't know how to convert code range: " + codeRange);
        }
    }
}
