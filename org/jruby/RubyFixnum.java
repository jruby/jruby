/*
 * RubyFixnum.java - Implementation of the Fixnum class.
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.runtime.*;
import org.jruby.marshal.*;

/** Implementation of the Fixnum class.
 *
 * @author jpetersen
 * @version $Revision$
 */
public class RubyFixnum extends RubyInteger {
    private long value;
    private static int BIT_SIZE = 63;

    public RubyFixnum(Ruby ruby) {
        this(ruby, 0);
    }

    public RubyFixnum(Ruby ruby, long value) {
        super(ruby, ruby.getClasses().getFixnumClass());
        this.value = value;
    }

    public static RubyClass createFixnumClass(Ruby ruby) {
        RubyClass fixnumClass = ruby.defineClass("Fixnum", ruby.getClasses().getIntegerClass());
        fixnumClass.includeModule(ruby.getClasses().getPrecisionModule());

        fixnumClass.defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyFixnum.class, "induced_from", RubyObject.class));

        fixnumClass.defineMethod("to_f", CallbackFactory.getMethod(RubyFixnum.class, "to_f"));
        fixnumClass.defineMethod("to_s", CallbackFactory.getMethod(RubyFixnum.class, "to_s"));
        fixnumClass.defineMethod("to_str", CallbackFactory.getMethod(RubyFixnum.class, "to_s"));
        fixnumClass.defineMethod("taint", CallbackFactory.getSelfMethod(0));
        fixnumClass.defineMethod("freeze", CallbackFactory.getSelfMethod(0));

        fixnumClass.defineMethod("<<", CallbackFactory.getMethod(RubyFixnum.class, "op_lshift", RubyObject.class));
        fixnumClass.defineMethod(">>", CallbackFactory.getMethod(RubyFixnum.class, "op_rshift", RubyObject.class));

        fixnumClass.defineMethod("+", CallbackFactory.getMethod(RubyFixnum.class, "op_plus", RubyObject.class));
        fixnumClass.defineMethod("-", CallbackFactory.getMethod(RubyFixnum.class, "op_minus", RubyObject.class));
        fixnumClass.defineMethod("*", CallbackFactory.getMethod(RubyFixnum.class, "op_mul", RubyObject.class));
        fixnumClass.defineMethod("/", CallbackFactory.getMethod(RubyFixnum.class, "op_div", RubyObject.class));
        fixnumClass.defineMethod("%", CallbackFactory.getMethod(RubyFixnum.class, "op_mod", RubyObject.class));
        fixnumClass.defineMethod("**", CallbackFactory.getMethod(RubyFixnum.class, "op_pow", RubyObject.class));

        fixnumClass.defineMethod("==", CallbackFactory.getMethod(RubyFixnum.class, "op_equal", RubyObject.class));
        fixnumClass.defineMethod("<=>", CallbackFactory.getMethod(RubyFixnum.class, "op_cmp", RubyObject.class));
        fixnumClass.defineMethod(">", CallbackFactory.getMethod(RubyFixnum.class, "op_gt", RubyObject.class));
        fixnumClass.defineMethod(">=", CallbackFactory.getMethod(RubyFixnum.class, "op_ge", RubyObject.class));
        fixnumClass.defineMethod("<", CallbackFactory.getMethod(RubyFixnum.class, "op_lt", RubyObject.class));
        fixnumClass.defineMethod("<=", CallbackFactory.getMethod(RubyFixnum.class, "op_le", RubyObject.class));
        fixnumClass.defineMethod("&", CallbackFactory.getMethod(RubyFixnum.class, "op_and", RubyObject.class));
        fixnumClass.defineMethod("|", CallbackFactory.getMethod(RubyFixnum.class, "op_or", RubyInteger.class));
        fixnumClass.defineMethod("^", CallbackFactory.getMethod(RubyFixnum.class, "op_xor", RubyInteger.class));
        fixnumClass.defineMethod("size", CallbackFactory.getMethod(RubyFixnum.class, "size"));
        fixnumClass.defineMethod("[]", CallbackFactory.getMethod(RubyFixnum.class, "aref", RubyInteger.class));

        return fixnumClass;
    }

    public Class getJavaClass() {
        return Long.TYPE;
    }

    public double getDoubleValue() {
        return (double) value;
    }

    public long getLongValue() {
        return value;
    }

    public static RubyFixnum zero(Ruby ruby) {
        return newFixnum(ruby, 0);
    }

    public static RubyFixnum one(Ruby ruby) {
        return newFixnum(ruby, 1);
    }

    public static RubyFixnum minus_one(Ruby ruby) {
        return newFixnum(ruby, -1);
    }

    protected int compareValue(RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).compareValue(this) * -1;
        } else if (other instanceof RubyFloat) {
            double otherVal = other.getDoubleValue();
            double thisVal = getDoubleValue();
            return thisVal > otherVal ? 1 : thisVal < otherVal ? -1 : 0;
        } else {
            long otherVal = other.getLongValue();
            return getLongValue() > otherVal ? 1 : getLongValue() < otherVal ? -1 : 0;
        }
    }

    public int hashCode() {
        return (((int) value) ^ (int) (value >> 32));
    }

    // Methods of the Fixnum Class (fix_*):

    public static RubyFixnum newFixnum(Ruby ruby, long value) {
        RubyFixnum fixnum;
        if (value >= 0 && value < ruby.fixnumCache.length) {
            fixnum = ruby.fixnumCache[(int) value];
            if (fixnum == null) {
                fixnum = new RubyFixnum(ruby, value);
                ruby.fixnumCache[(int) value] = fixnum;
            }
        } else {
            fixnum = new RubyFixnum(ruby, value);
        }
        return fixnum;
    }

    public RubyFixnum newFixnum(long value) {
        return newFixnum(ruby, value);
    }

    public static RubyInteger induced_from(Ruby ruby, RubyObject recv, RubyObject number) {
        if (number instanceof RubyFixnum) {
            return (RubyFixnum) number;
        } else if (number instanceof RubyFloat) {
            return ((RubyFloat) number).to_i();
        } else if (number instanceof RubyBignum) {
            return RubyFixnum.newFixnum(ruby, ((RubyBignum) number).getLongValue());
        }
        return (RubyFixnum) number.convertToType("Fixnum", "to_int", true);
    }

    public RubyNumeric op_plus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_plus(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), value).op_plus(other);
        } else {
            long otherValue = other.getLongValue();
            long result = value + otherValue;
            if ((value < 0 && otherValue < 0 && result > 0) || (value > 0 && otherValue > 0 && result < 0)) {
                return RubyBignum.newBignum(getRuby(), value).op_plus(other);
            }
            return newFixnum(result);
        }
    }

    public RubyNumeric op_minus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_minus(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), value).op_minus(other);
        } else {
            long otherValue = other.getLongValue();
            long result = value - otherValue;
            if ((value < 0 && otherValue > 0 && result > 0) || (value > 0 && otherValue < 0 && result < 0)) {
                return RubyBignum.newBignum(getRuby(), value).op_minus(other);
            }
            return newFixnum(result);
        }
    }

    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_mul(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_mul(other);
        } else {
            long otherValue = other.getLongValue();
            long result = value * otherValue;
            if (result / otherValue == value) {
                return newFixnum(result);
            } else {
                return RubyBignum.newBignum(getRuby(), getLongValue()).op_mul(other);
            }
        }
    }

    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_div(other);
        } else {
            return newFixnum(getRuby(), getLongValue() / other.getLongValue());
        }
    }

    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_mod(other);
        } else if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(getRuby(), getLongValue()).op_mod(other);
        } else {
            return newFixnum(getRuby(), getLongValue() % other.getLongValue());
        }
    }

    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_pow(other);
        } else {
            if (other.getLongValue() == 0) {
                return newFixnum(getRuby(), 1);
            } else if (other.getLongValue() == 1) {
                return this;
            } else if (other.getLongValue() > 1) {
                return RubyBignum.newBignum(getRuby(), getLongValue()).op_pow(other);
            } else {
                return RubyFloat.newFloat(getRuby(), getDoubleValue()).op_pow(other);
            }
        }
    }

    public RubyBoolean op_equal(RubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuby().getFalse();
        } else {
            return RubyBoolean.newBoolean(getRuby(), compareValue((RubyNumeric) other) == 0);
        }
    }

    public RubyNumeric op_cmp(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.newFixnum(getRuby(), compareValue(other));
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

    public RubyString to_s() {
        return RubyString.newString(getRuby(), String.valueOf(getLongValue()));
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuby(), getDoubleValue());
    }

    public RubyInteger op_lshift(RubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_rshift(other.op_uminus());
        if (width > BIT_SIZE || value >>> (BIT_SIZE - width) > 0) {
            RubyBignum lBigValue = new RubyBignum(ruby, RubyBignum.bigIntValue(this));
            return lBigValue.op_lshift(other);
        }
        return newFixnum(value << width);
    }

    public RubyInteger op_rshift(RubyObject num) {
        RubyNumeric other = numericValue(num);
        long width = other.getLongValue();
        if (width < 0)
            return op_lshift(other.op_uminus());
        return newFixnum(value >>> width);
    }

    public RubyNumeric op_and(RubyObject other) {
        RubyNumeric otherNumeric = numericValue(other);
        long otherLong = otherNumeric.getTruncatedLongValue();
        return newFixnum(value & otherLong);
    }

    public RubyInteger op_or(RubyInteger other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.funcall("|", this);
        }
        return newFixnum(value | other.getLongValue());
    }

    public RubyInteger op_xor(RubyInteger other) {
        if (other instanceof RubyBignum) {
            return (RubyInteger) other.funcall("^", this);
        }
        return newFixnum(value ^ other.getLongValue());
    }

    /**
     * @see RubyObject#equal(RubyObject)
     */
    public RubyBoolean equal(RubyObject obj) {
        return RubyBoolean.newBoolean(ruby, obj instanceof RubyFixnum &&
            ((RubyFixnum)obj).getLongValue() == getLongValue());
    }

    public RubyFixnum size() {
        return newFixnum(4);
    }

    public RubyFixnum aref(RubyInteger pos) {
        long mask = 1 << pos.getLongValue();
        return newFixnum((value & mask) == 0 ? 0 : 1);
    }

    public void marshalTo(MarshalStream output) throws java.io.IOException {
        if (value <= Integer.MAX_VALUE) {
            output.write('i');
            output.dumpInt((int) value);
        } else {
            output.dumpObject(RubyBignum.newBignum(ruby, value));
        }
    }

    public static RubyFixnum unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return RubyFixnum.newFixnum(input.getRuby(),
                                    input.unmarshalInt());
    }
}
