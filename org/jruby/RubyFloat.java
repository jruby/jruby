/*
 * RubyFloat.java - No description
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
public class RubyFloat extends RubyNumeric {
    private double value;

    public RubyFloat(Ruby ruby) {
        this(ruby, 0.0);
    }
    
    public RubyFloat(Ruby ruby, double value) {
        super(ruby, ruby.getClasses().getFloatClass());
        this.value = value;
    }

    public Class getJavaClass() {
        return Double.TYPE;
    }
    
    /** Getter for property value.
     * @return Value of property value.
     */
    public double getValue() {
        return this.value;
    }
    
    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(double value) {
        this.value = value;
    }
    
    public double getDoubleValue() {
        return value;
    }
    
    public long getLongValue() {
        return (long)value;
    }
    
    protected int compareValue(RubyNumeric other) {
        double otherVal = other.getDoubleValue();
        return getValue() > otherVal ? 1 : getValue() < otherVal ? -1 : 0;
    }

    // Float methods (flo_*)
    
    /**
     *
     */
    public static RubyFloat m_newFloat(Ruby ruby, double value) {
        return new RubyFloat(ruby, value);
    }
    
    public RubyFixnum m_hash() {
        return new RubyFixnum(getRuby(), new Double(value).hashCode());
    }

    public RubyArray m_coerce(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyArray.m_newArray(getRuby(), this, 
             m_newFloat(getRuby(), other.getDoubleValue()));
    }
    
    public RubyNumeric op_uminus() {
        return RubyFloat.m_newFloat(getRuby(), -value);
    }
    
    public RubyNumeric op_plus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(),
            getDoubleValue() + other.getDoubleValue());
    }
    
    public RubyNumeric op_minus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(),
            getDoubleValue() - other.getDoubleValue());
    }
    
    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(),
            getDoubleValue() * other.getDoubleValue());
    }
    
    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(),
            getDoubleValue() / other.getDoubleValue());
    }
    
    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(),
            getDoubleValue() % other.getDoubleValue());
    }
    
    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.m_newFloat(getRuby(), 
            Math.pow(getDoubleValue(), other.getDoubleValue()));
    }
    
    public RubyBoolean op_equal(RubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuby().getFalse();
        } else {
            return RubyBoolean.m_newBoolean(getRuby(),
                compareValue((RubyNumeric)other) == 0);
        }
    }

    public RubyNumeric op_cmp(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.m_newFixnum(getRuby(), compareValue(other));
    }

    public RubyBoolean op_gt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.m_newBoolean(getRuby(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.m_newBoolean(getRuby(), compareValue(other) >= 0);
    }

    public RubyBoolean op_lt(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.m_newBoolean(getRuby(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.m_newBoolean(getRuby(), compareValue(other) <= 0);
    }

    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), Double.toString(getValue()));
    }
    
    public RubyInteger m_to_i() {
        // HACK +++
        return RubyFixnum.m_newFixnum(getRuby(), getLongValue());
        // HACK ---
    }
}
