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
public abstract class RubyFixnum extends RubyObject implements Unboxable {

    public static final long MIN_VALUE = Long.MIN_VALUE;
    public static final long MAX_VALUE = Long.MAX_VALUE;

    public static final BigInteger MIN_VALUE_BIG = BigInteger.valueOf(MIN_VALUE);
    public static final BigInteger MAX_VALUE_BIG = BigInteger.valueOf(MAX_VALUE);

    public static final long SIZE = Long.SIZE;

    protected RubyFixnum(RubyClass fixnumClass) {
        super(fixnumClass);
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

        if (value instanceof IntegerFixnum) {
            return (int) value;
        }

        if (value instanceof LongFixnum) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        if (value instanceof IntegerFixnum) {
            return ((IntegerFixnum) value).getValue();
        }

        if (value instanceof BigInteger) {
            CompilerDirectives.transferToInterpreter();
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

        if (value instanceof IntegerFixnum) {
            return ((IntegerFixnum) value).getValue();
        }

        if (value instanceof LongFixnum) {
            return ((IntegerFixnum) value).getValue();
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

        // TODO(CS): make this a node?

        if (value.compareTo(MIN_VALUE_BIG) >= 0 && value.compareTo(MAX_VALUE_BIG) <= 0) {
            final long longValue = value.longValue();

            if (longValue >= IntegerFixnum.MIN_VALUE && longValue <= IntegerFixnum.MAX_VALUE) {
                return (int) longValue;
            } else {
                return value;
            }
        } else {
            return value;
        }
    }

    protected boolean equals(long value, Object other) {
        if (other instanceof Integer) {
            return value == (int) other;
        } else if (other instanceof Long) {
            return value == (long) other;
        } else if (other instanceof IntegerFixnum) {
            return value == ((IntegerFixnum) other).getValue();
        } else if (other instanceof LongFixnum) {
            return value == ((LongFixnum) other).getValue();
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

    public static class IntegerFixnum extends RubyFixnum {

        private final int value;

        public IntegerFixnum(RubyClass fixnumClass, int value) {
            super(fixnumClass);
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public Object unbox() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        @Override
        public boolean equals(Object other) {
            return equals(value, other);
        }

    }

    public static class LongFixnum extends RubyFixnum {

        private final long value;

        public LongFixnum(RubyClass fixnumClass, long value) {
            super(fixnumClass);
            this.value = value;
        }

        public long getValue() {
            return value;
        }

        @Override
        public Object unbox() {
            return value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public boolean equals(Object other) {
            return equals(value, other);
        }

    }

}
