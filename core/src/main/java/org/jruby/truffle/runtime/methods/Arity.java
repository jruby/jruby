/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

public class Arity {

    public static final int NO_MAXIMUM = Integer.MAX_VALUE;

    private final int required;
    private final int optional;
    private final int maximum;

    public Arity(int required, int optional, int maximum) {
        this.required = required;
        this.optional = optional;
        this.maximum = maximum;
    }

    public int getRequired() {
        return required;
    }

    public int getOptional() {
        return optional;
    }

    public int getMaximum() {
        return maximum;
    }

}
