/*
 * RubyFixnum.java - No description
 * Created on 04. Juli 2001, 22:53
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

/**
 *
 * @author  jpetersen
 */
public class RubyFixnum extends RubyInteger {
    private long value;

    public RubyFixnum(Ruby ruby) {
        this(ruby, 0);
    }

    public RubyFixnum(Ruby ruby, long value) {
        super(ruby, ruby.getClasses().getFixnumClass());
        this.value = value;
    }

    public Class getJavaClass() {
        return Long.TYPE;
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public long getValue() {
        return this.value;
    }

    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(long value) {
        this.value = value;
    }

    public double getDoubleValue() {
        return (double) value;
    }

    public long getLongValue() {
        return value;
    }

    protected boolean needBignumAdd(long otherVal) {
        if (!Ruby.AUTOMATIC_BIGNUM_CAST) {
            return false;
        }
        if ((value < 0) && (otherVal < 0)) {
            return (value + otherVal) >= 0;
        } else if ((value > 0) && (otherVal > 0)) {
            return (value + otherVal) < 0;
        }
        return false;
    }

    protected boolean needBignumMul(long value) {
        long product = getValue() * value;
        return (product / value) != getValue();
    }

    public static RubyFixnum zero(Ruby ruby) {
        return m_newFixnum(ruby, 0);
    }

    public static RubyFixnum one(Ruby ruby) {
        return m_newFixnum(ruby, 1);
    }

    public static RubyFixnum minus_one(Ruby ruby) {
        return m_newFixnum(ruby, -1);
    }

    // Methods of the Fixnum Class (fix_*):

    public static RubyFixnum m_newFixnum(Ruby ruby, long value) {
        // Cache for Fixnums (Performance)
        if ((value & ~Ruby.FIXNUM_CACHE_MAX) == 0) {
            return ruby.fixnumCache[(int) value];
        }

        return new RubyFixnum(ruby, value);
    }

    public RubyFixnum m_newFixnum(long value) {
        // Cache for Fixnums (Performance)
        if ((value & ~Ruby.FIXNUM_CACHE_MAX) == 0) {
            return getRuby().fixnumCache[(int) value];
        }

        return new RubyFixnum(getRuby(), value);
    }

    public RubyFixnum m_hash() {
        return new RubyFixnum(getRuby(), new Long(value).hashCode());
    }

    public RubyNumeric op_plus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_plus(other);
        } else if (
            other instanceof RubyBignum || needBignumAdd(other.getLongValue())) {
            return RubyBignum.m_newBignum(getRuby(), value).op_plus(other);
        } else {
            return m_newFixnum(value + other.getLongValue());
        }
    }

    public RubyNumeric op_minus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_minus(other);
        } else if (
            other instanceof RubyBignum || needBignumAdd(-other.getLongValue())) {
            return RubyBignum.m_newBignum(getRuby(), value).op_minus(other);
        } else {
            return m_newFixnum(value - other.getLongValue());
        }
    }

    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mul(other);
        } else if (
            other instanceof RubyBignum || needBignumMul(other.getLongValue())) {
            return RubyBignum.m_newBignum(getRuby(), getLongValue()).op_mul(other);
        } else {
            return m_newFixnum(getRuby(), getValue() * other.getLongValue());
        }
    }

    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.m_newBignum(getRuby(), getLongValue()).op_div(other);
        } else {
            return m_newFixnum(getRuby(), getValue() / other.getLongValue());
        }
    }

    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.m_newBignum(getRuby(), getLongValue()).op_mod(other);
        } else {
            return m_newFixnum(getRuby(), getValue() % other.getLongValue());
        }
    }

    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_pow(other);
        } else {
            if (other.getLongValue() == 0) {
                return m_newFixnum(getRuby(), 1);
            } else if (other.getLongValue() == 1) {
                return this;
            } else if (other.getLongValue() > 1) {
                return RubyBignum.m_newBignum(getRuby(), getLongValue()).op_pow(other);
            } else {
                return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_pow(other);
            }
        }
    }

    public RubyBoolean op_equal(RubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuby().getFalse();
        } else if (other instanceof RubyFloat) {
            return RubyBoolean.m_newBoolean(getRuby(),
                getDoubleValue() == ((RubyFloat) other).getDoubleValue());
        } else {
            return RubyBoolean.m_newBoolean(getRuby(),
                getLongValue() == ((RubyNumeric) other).getLongValue());
        }
    }

    public RubyNumeric op_cmp(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_cmp(other);
        } else if (getLongValue() == other.getLongValue()) {
            return m_newFixnum(getRuby(), 0);
        } else if (getLongValue() > other.getLongValue()) {
            return m_newFixnum(getRuby(), 1);
        } else {
            return m_newFixnum(getRuby(), -1);
        }
    }

    public RubyBoolean op_gt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_gt(other);
        } else {
            return getLongValue() > other.getLongValue()
                ? getRuby().getTrue()
                : getRuby().getFalse();
        }
    }

    public RubyBoolean op_ge(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_ge(other);
        } else {
            return getLongValue() >= other.getLongValue()
                ? getRuby().getTrue()
                : getRuby().getFalse();
        }
    }

    public RubyBoolean op_lt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_lt(other);
        } else {
            return getLongValue() < other.getLongValue()
                ? getRuby().getTrue()
                : getRuby().getFalse();
        }
    }

    public RubyBoolean op_le(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_le(other);
        } else {
            return getLongValue() <= other.getLongValue()
                ? getRuby().getTrue()
                : getRuby().getFalse();
        }
    }

    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), String.valueOf(getValue()));
    }
}