/*
 * RubyFixnum.java - No description
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
public class RubyFixnum extends RubyInteger {
    private long value;

    public RubyFixnum(Ruby ruby) {
        this(ruby, 0);
    }
    
    public RubyFixnum(Ruby ruby, long value) {
        super(ruby, ruby.getFixnumClass());
        this.value = value;
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
    
    // Methods of the Fixnum Class (fix_*):
    
    public static RubyFixnum m_newFixnum(Ruby ruby, long value) {
        // Cache for Fixnums (Performance)
        
        return new RubyFixnum(ruby, value);
    }
    
    public RubyFixnum op_plus(RubyFixnum other) {
        return m_newFixnum(getRuby(), getValue() + other.getValue());
    }
    
    public RubyFixnum op_minus(RubyFixnum other) {
        return m_newFixnum(getRuby(), getValue() - other.getValue());
    }
    
    public RubyFixnum op_mul(RubyFixnum other) {
        return m_newFixnum(getRuby(), getValue() * other.getValue());
    }
    
    public RubyFixnum op_div(RubyFixnum other) {
        return m_newFixnum(getRuby(), getValue() / other.getValue());
    }
    
    public RubyFixnum op_mod(RubyFixnum other) {
        return m_newFixnum(getRuby(), getValue() % other.getValue());
    }
    
    public RubyFixnum op_exp(RubyFixnum other) {
        return m_newFixnum(getRuby(), (long)Math.pow(getValue(), other.getValue()));
    }
    
    public RubyBoolean op_equal(RubyFixnum other) {
        return getValue() == other.getValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyFixnum op_cmp(RubyFixnum other) {
        if (getValue() == other.getValue()) {
            return m_newFixnum(getRuby(), 0);
        } else if (getValue() > other.getValue()) {
            return m_newFixnum(getRuby(), 1);
        } else {
            return m_newFixnum(getRuby(), -1);
        }
    }
    
    public RubyBoolean op_gt(RubyFixnum other) {
        return getValue() > other.getValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_ge(RubyFixnum other) {
        return getValue() >= other.getValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_lt(RubyFixnum other) {
        return getValue() < other.getValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyBoolean op_le(RubyFixnum other) {
        return getValue() <= other.getValue() ? getRuby().getTrue() : getRuby().getFalse();
    }
    
    public RubyString m_to_s() {
        return RubyString.m_newString(getRuby(), String.valueOf(getValue()));
    }
}