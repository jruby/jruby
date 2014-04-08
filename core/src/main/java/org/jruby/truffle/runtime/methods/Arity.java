/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.methods;

import com.oracle.truffle.api.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;

/**
 * Represents the arity, or parameter contract, of a method.
 */
public class Arity {

    private final int minimum;
    public static final int NO_MINIMUM = 0;

    private final int maximum;
    public static final int NO_MAXIMUM = Integer.MAX_VALUE;

    public static final Arity NO_ARGS = new Arity(0, 0);
    public static final Arity ONE_ARG = new Arity(1, 1);

    public Arity(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    @Override
    public String toString() {
        return String.format("Arity(%d, %d)", minimum, maximum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Arity arity = (Arity) o;

        if (maximum != arity.maximum) return false;
        if (minimum != arity.minimum) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = minimum;
        result = 31 * result + maximum;
        return result;
    }
}
