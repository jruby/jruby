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

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyBignum extends RubyInteger {
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

    public RubyNumeric op_plus(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_plus(other);
        } else {
            if (other instanceof RubyBignum) {
                return m_newBignum(getRuby(), getValue().add(((RubyBignum)other).getValue()));
            } else {
                return m_newBignum(getRuby(), getValue().add(BigInteger.valueOf(other.getLongValue())));
            }
        }
    }
    
    public RubyNumeric op_minus(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_minus(other);
        } else {
            if (other instanceof RubyBignum) {
                return m_newBignum(getRuby(), getValue().subtract(((RubyBignum)other).getValue()));
            } else {
                return m_newBignum(getRuby(), getValue().subtract(BigInteger.valueOf(other.getLongValue())));
            }
        }
    }
    
    public RubyNumeric op_mul(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mul(other);
        } else {
            if (other instanceof RubyBignum) {
                return m_newBignum(getRuby(), getValue().multiply(((RubyBignum)other).getValue()));
            } else {
                return m_newBignum(getRuby(), getValue().multiply(BigInteger.valueOf(other.getLongValue())));
            }
        }
    }
    
    public RubyNumeric op_div(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_div(other);
        } else {
            if (other instanceof RubyBignum) {
                return m_newBignum(getRuby(), getValue().divide(((RubyBignum)other).getValue()));
            } else {
                return m_newBignum(getRuby(), getValue().divide(BigInteger.valueOf(other.getLongValue())));
            }
        }
    }
    
    public RubyNumeric op_mod(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_mod(other);
        } else {
            if (other instanceof RubyBignum) {
                return m_newBignum(getRuby(), getValue().mod(((RubyBignum)other).getValue()));
            } else {
                return m_newBignum(getRuby(), getValue().mod(BigInteger.valueOf(other.getLongValue())));
            }
        }
    }
    
    public RubyNumeric op_pow(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.m_newFloat(getRuby(), getDoubleValue()).op_pow(other);
        } else {
            return m_newBignum(getRuby(), getValue().pow((int)other.getLongValue()));
        }
    }
}