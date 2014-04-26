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
import org.jruby.truffle.runtime.NilPlaceholder;

import java.math.*;

/**
 * Represents the Ruby {@code Fixnum} class.
 */
public class RubyFixnum extends RubyObject implements Unboxable {

    public static final long MIN_VALUE = Integer.MIN_VALUE;
    public static final long MAX_VALUE = Integer.MAX_VALUE;

    public static final BigInteger INT_MIN_VALUE_BIG = BigInteger.valueOf(Integer.MIN_VALUE);
    public static final BigInteger INT_MAX_VALUE_BIG = BigInteger.valueOf(Integer.MAX_VALUE);

    public static final BigInteger MIN_VALUE_BIG = BigInteger.valueOf(MIN_VALUE);
    public static final BigInteger MAX_VALUE_BIG = BigInteger.valueOf(MAX_VALUE);

    public static final long SIZE = Integer.SIZE;

    private final long value;

    public RubyFixnum(RubyClass fixnumClass, long value) {
        super(fixnumClass);
        this.value = value;
    }

    public static int toInt(Object value) {
        // TODO(CS): stop using this in compilation - use a specialising node instead

        assert value != null;

        if (value instanceof NilPlaceholder || value instanceof RubyNilClass) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof RubyFixnum) {

        }

        if (value instanceof BigInteger) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof RubyBignum) {
            CompilerDirectives.transferToInterpreter();
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

    public static long toLong(Object value) {
        // TODO(CS): stop using this in compilation - use a specialising node instead

        assert value != null;

        if (value instanceof NilPlaceholder || value instanceof RubyNilClass) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        if (value instanceof RubyFixnum) {
            final RubyFixnum fixnum = (RubyFixnum) value;

            if (fixnum.isRepresentableAsInt()) {
                return fixnum.getIntValue();
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }
        }

        if (value instanceof BigInteger) {
            throw new UnsupportedOperationException();
        }

        if (value instanceof RubyBignum) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        if (value instanceof Double) {
            return (long) (double) value;
        }

        if (value instanceof RubyFloat) {
            return (long) ((RubyFloat) value).getValue();
        }

        CompilerDirectives.transferToInterpreter();

        throw new UnsupportedOperationException(value.getClass().toString());
    }

    /**
     * Given a {@link java.math.BigInteger} value, produce either a {@code Fixnum} or {@code Bignum} .
     */
    public static Object fixnumOrBignum(BigInteger value) {
        assert value != null;

        // TODO(CS): uses int range for the moment

        if (value.compareTo(INT_MIN_VALUE_BIG) >= 0 && value.compareTo(INT_MAX_VALUE_BIG) <= 0) {
            return value.intValue();
        } else {
            return value;
        }
    }

    /**
     * Given a {@code long} value, produce either a {@code Fixnum} or {@code Bignum} .
     */
    public static Object fixnumOrBignum(long value) {
        // TODO(CS): uses int range for the moment

        if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return BigInteger.valueOf(value);
        }
    }

    public boolean isRepresentableAsInt() {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    public int getIntValue() {
        assert isRepresentableAsInt();
        return (int) value;
    }

    public long getLongValue() {
        return value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Integer) {
            return value == (int) other;
        } else if (other instanceof Long) {
            return value == (long) other;
        } else if (other instanceof RubyFixnum) {
            return value == ((RubyFixnum) other).value;
        } else if (other instanceof BigInteger) {
            return other.equals(value);
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
        if (isRepresentableAsInt()) {
            return getIntValue();
        } else {
            return getLongValue();
        }
    }

}
