/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

public enum BigDecimalType {
    NEGATIVE_INFINITY("-Infinity"),
    POSITIVE_INFINITY("Infinity"),
    NAN("NaN"),
    NEGATIVE_ZERO("-0"),
    NORMAL(null);

    private final String representation;

    BigDecimalType(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        assert representation != null;
        return representation;
    }

}
