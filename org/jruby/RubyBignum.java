/*
 * RubyBignum.java - No description
 * Created on 13. September 2001, 00:40
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import java.math.*;

import org.jruby.exceptions.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyBignum extends RubyInteger {
    private static BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private static BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private BigInteger value;

    public RubyBignum(Ruby ruby) {
        this(ruby, BigInteger.ZERO);
    }

    public RubyBignum(Ruby ruby, BigInteger value) {
        super(ruby, ruby.getClasses().getBignumClass());

        this.value = value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public long getLongValue() {
        return value.longValue();
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public BigInteger getValue() {
        return value;
    }

    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(BigInteger value) {
        this.value = value;
    }

    /* If the value will fit in a Fixnum, return one of those. */
    private static RubyInteger bigNorm(Ruby ruby, BigInteger bi) {
        if (bi.compareTo(LONG_MIN) < 0 || bi.compareTo(LONG_MAX) > 0) {
            return m_newBignum(ruby, bi);
        }
        return RubyFixnum.m_newFixnum(ruby, bi.longValue());
    }

    private BigInteger bigIntValue(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            throw new RubyBugException("argument must be an integer");
        }
        return (other instanceof RubyBignum)
               ? ((RubyBignum)other).getValue()
               : BigInteger.valueOf(other.getLongValue());
    }

    protected int compareValue(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            double otherVal = other.getDoubleValue();
            double thisVal = getDoubleValue();
            return thisVal > otherVal ? 1 : thisVal < otherVal ? -1 : 0;
        }

        return getValue().compareTo(bigIntValue(other));
    }

    // Bignum methods

    public static RubyBignum m_newBignum(Ruby ruby, long value) {
        return new RubyBignum(ruby, BigInteger.valueOf(value));
    }

    public static RubyBignum m_newBignum(Ruby ruby, BigInteger value) {
        return new RubyBignum(ruby, value);
    }

    public RubyFixnum m_hash() {
        return new RubyFixnum(getRuby(), value.hashCode());
    }

    public RubyNumeric op_plus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_plus(other);
        }
        return bigNorm(getRuby(), getValue().add(bigIntValue(other)));
    }

    public RubyNumeric op_minus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_minus(other);
        }
        return bigNorm(getRuby(), getValue().subtract(bigIntValue(other)));
    }

    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mul(other);
        }
        return bigNorm(getRuby(), getValue().multiply(bigIntValue(other)));
    }

    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_div(other);
        }
        return bigNorm(getRuby(), getValue().divide(bigIntValue(other)));
    }

    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mod(other);
        }
        return bigNorm(getRuby(), getValue().mod(bigIntValue(other)));
    }

    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_pow(other);
        } else {
            return bigNorm(getRuby(), getValue().pow((int)other.getLongValue()));
        }
    }

    public RubyBoolean op_equal(RubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuby().getFalse();
        } else {
            return RubyBoolean.newBoolean(getRuby(),
                compareValue((RubyNumeric)other) == 0);
        }
    }

    public RubyNumeric op_cmp(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.m_newFixnum(getRuby(), compareValue(other));
    }

    public RubyBoolean op_gt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) >= 0);
    }

    public RubyBoolean op_lt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuby(), compareValue(other) <= 0);
    }

    public RubyString m_to_s() {
        return RubyString.newString(getRuby(), getValue().toString());
    }
}