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

import org.jruby.util.StringSupport;

public enum CodeRange {
    CR_UNKNOWN(StringSupport.CR_UNKNOWN),
    CR_7BIT(StringSupport.CR_7BIT),
    CR_VALID(StringSupport.CR_VALID),
    CR_BROKEN(StringSupport.CR_BROKEN);

    private final int jrubyValue;

    CodeRange(int jrubyValue) {
        this.jrubyValue = jrubyValue;
    }

    public int toInt() {
        return jrubyValue;
    }

    public static CodeRange fromInt(int codeRange) {
        switch(codeRange) {
            case StringSupport.CR_UNKNOWN: return CR_UNKNOWN;
            case StringSupport.CR_7BIT: return CR_7BIT;
            case StringSupport.CR_VALID: return CR_VALID;
            case StringSupport.CR_BROKEN: return CR_BROKEN;
            default: throw new UnsupportedOperationException("Don't know how to convert code range: " + codeRange);
        }
    }
}
