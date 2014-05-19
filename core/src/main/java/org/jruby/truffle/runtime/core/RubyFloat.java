/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.NilPlaceholder;

import java.math.BigInteger;

/**
 * Represents the Ruby {@code Float} class.
 */
public class RubyFloat extends RubyObject implements Unboxable {

    private final double value;

    public RubyFloat(RubyClass floatClass, double value) {
        super(floatClass);
        this.value = value;
    }

    /**
     * Convert a value to a {@code Float}, without doing any lookup.
     */
    public static double toDouble(Object value) {
        RubyNode.notDesignedForCompilation();

        assert value != null;

        if (value instanceof NilPlaceholder || value instanceof RubyNilClass) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof RubyFixnum.IntegerFixnum) {
            return ((RubyFixnum.IntegerFixnum) value).getValue();
        }

        if (value instanceof RubyFixnum.LongFixnum) {
            return ((RubyFixnum.LongFixnum) value).getValue();
        }

        if (value instanceof BigInteger) {
            return ((BigInteger) value).doubleValue();
        }

        if (value instanceof RubyBignum) {
            return ((RubyBignum) value).getValue().doubleValue();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        if (value instanceof RubyFloat) {
            return ((RubyFloat) value).getValue();
        }

        CompilerDirectives.transferToInterpreter();

        throw new UnsupportedOperationException();
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        RubyNode.notDesignedForCompilation();

        if (other instanceof Integer) {
            return value == (int) other;
        } else if (other instanceof RubyFixnum.IntegerFixnum) {
            return value == ((RubyFixnum.IntegerFixnum) other).getValue();
        } else if (other instanceof RubyFixnum.LongFixnum) {
            return value == ((RubyFixnum.LongFixnum) other).getValue();
        } else if (other instanceof Double) {
            return value == (double) other;
        } else if (other instanceof RubyFloat) {
            return value == ((RubyFloat) other).value;
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object unbox() {
        return value;
    }

}
