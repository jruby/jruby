/*
 * RubyFloat.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
        super(ruby, ruby.getFloatClass());
        this.value = value;
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
    
    // Float methods (flo_*)
    
    /**
     *
     */
    public static RubyFloat m_newFloat(Ruby ruby, double value) {
        return new RubyFloat(ruby, value);
    }
    
    public RubyArray m_coerce(RubyNumeric other) {
        return RubyArray.m_newArray(getRuby(), this, m_newFloat(getRuby(), other.getDoubleValue()));
    }
    
    public RubyNumeric op_uminus() {
        return RubyFloat.m_newFloat(getRuby(), -value);
    }
    
    public RubyNumeric op_plus(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), getDoubleValue() + getDoubleValue());
    }
    
    public RubyNumeric op_minus(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), getDoubleValue() - getDoubleValue());
    }
    
    public RubyNumeric op_mul(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), getDoubleValue() * getDoubleValue());
    }
    
    public RubyNumeric op_div(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), getDoubleValue() / getDoubleValue());
    }
    
    public RubyNumeric op_mod(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), getDoubleValue() % getDoubleValue());
    }
    
    public RubyNumeric op_pow(RubyNumeric other) {
        return RubyFloat.m_newFloat(getRuby(), Math.pow(getDoubleValue(), getDoubleValue()));
    }
    
    public RubyNumeric op_cmp(RubyNumeric other) {
        if (getDoubleValue() == other.getDoubleValue()) {
            return RubyFixnum.m_newFixnum(getRuby(), 0);
        } else if (getDoubleValue() > other.getDoubleValue()) {
            return RubyFixnum.m_newFixnum(getRuby(), 1);
        } else if (getDoubleValue() < other.getDoubleValue()) {
            return RubyFixnum.m_newFixnum(getRuby(), 1);
        }
        // HACK +++
        throw new RuntimeException();
        // HACK ---
    }
    
    public RubyBoolean op_gt(RubyNumeric other) {
        return getDoubleValue() > other.getDoubleValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_ge(RubyNumeric other) {
        return getDoubleValue() >= other.getDoubleValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_lt(RubyNumeric other) {
        return getDoubleValue() < other.getDoubleValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_le(RubyNumeric other) {
        return getDoubleValue() <= other.getDoubleValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), Double.toString(getValue()));
    }
}