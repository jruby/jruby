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

import com.oracle.truffle.api.CompilerDirectives;
import org.jruby.truffle.runtime.NilPlaceholder;

import java.math.*;

/**
 * Represents the Ruby {@code Fixnum} class.
 */
public class RubyFixnum extends RubyObject implements Unboxable {

    public static final int MIN_VALUE = Integer.MIN_VALUE;
    public static final int MAX_VALUE = Integer.MAX_VALUE;

    public static final BigInteger MIN_VALUE_BIG = BigInteger.valueOf(MIN_VALUE);
    public static final BigInteger MAX_VALUE_BIG = BigInteger.valueOf(MAX_VALUE);

    public static final int SIZE = Integer.SIZE;

    private final int value;

    public RubyFixnum(RubyClass fixnumClass, int value) {
        super(fixnumClass);
        this.value = value;
    }

    /**
     * Convert a value to a {@code Fixnum}, without doing any lookup.
     */
    public static int toFixnum(Object value) {
        assert value != null;

        if (value instanceof NilPlaceholder || value instanceof RubyNilClass) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof RubyFixnum) {
            return ((RubyFixnum) value).getValue();
        }

        if (value instanceof BigInteger) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof RubyBignum) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof Double) {
            return (int) (double) value;
        }

        if (value instanceof RubyFloat) {
            return (int) ((RubyFloat) value).getValue();
        }

        CompilerDirectives.transferToInterpreter();

        throw new UnsupportedOperationException(value.getClass().toString());
    }

    /**
     * Given a {@link java.math.BigInteger} value, produce either a {@code Fixnum} or {@code Bignum} .
     */
    public static Object fixnumOrBignum(BigInteger value) {
        assert value != null;

        if (value.compareTo(MIN_VALUE_BIG) >= 0 && value.compareTo(MAX_VALUE_BIG) <= 0) {
            return value.intValue();
        } else {
            return value;
        }
    }

    /**
     * Given a {@code long} value, produce either a {@code Fixnum} or {@code Bignum} .
     */
    public static Object fixnumOrBignum(long value) {
        if (value >= MIN_VALUE && value <= MAX_VALUE) {
            return (int) value;
        } else {
            return BigInteger.valueOf(value);
        }
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Integer) {
            return value == (int) other;
        } else if (other instanceof RubyFixnum) {
            return value == ((RubyFixnum) other).value;
        } else if (other instanceof BigInteger) {
            return ((BigInteger) other).equals(value);
        } else if (other instanceof RubyBignum) {
            return ((RubyBignum) other).getValue().equals(value);
        } else if (other instanceof Double) {
            return value == (double) other;
        } else if (other instanceof RubyFloat) {
            return value == ((RubyFloat) other).getValue();
        } else {
            return super.equals(other);
        }
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    public Object unbox() {
        return value;
    }

}
