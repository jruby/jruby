/*
 * RubyFloat.java - No description
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

import java.text.*;
import java.util.*;

import org.jruby.runtime.*;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.exceptions.TypeError;

/**
 *
 * @author  jpetersen
 */
public class RubyFloat extends RubyNumeric {
    private static final NumberFormat usFormat = NumberFormat.getInstance(Locale.US);

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
        return (long) value;
    }

    public static RubyClass createFloatClass(Ruby ruby) {
        RubyClass floatClass = ruby.defineClass("Float", ruby.getClasses().getNumericClass());

        floatClass.defineSingletonMethod("induced_from", CallbackFactory.getSingletonMethod(RubyFloat.class, "induced_from", RubyObject.class));

        floatClass.defineMethod("to_i", CallbackFactory.getMethod(RubyFloat.class, "to_i"));
        floatClass.defineMethod("to_f", CallbackFactory.getMethod(RubyFloat.class, "to_f"));
        floatClass.defineMethod("to_s", CallbackFactory.getMethod(RubyFloat.class, "to_s"));

        floatClass.defineMethod("+", CallbackFactory.getMethod(RubyFloat.class, "op_plus", RubyObject.class));
        floatClass.defineMethod("-", CallbackFactory.getMethod(RubyFloat.class, "op_minus", RubyObject.class));
        floatClass.defineMethod("*", CallbackFactory.getMethod(RubyFloat.class, "op_mul", RubyObject.class));
        floatClass.defineMethod("/", CallbackFactory.getMethod(RubyFloat.class, "op_div", RubyObject.class));
        floatClass.defineMethod("%", CallbackFactory.getMethod(RubyFloat.class, "op_mod", RubyObject.class));
        floatClass.defineMethod("**", CallbackFactory.getMethod(RubyFloat.class, "op_pow", RubyObject.class));

        floatClass.defineMethod("==", CallbackFactory.getMethod(RubyFloat.class, "op_equal", RubyObject.class));
        floatClass.defineMethod("<=>", CallbackFactory.getMethod(RubyFloat.class, "op_cmp", RubyObject.class));
        floatClass.defineMethod(">", CallbackFactory.getMethod(RubyFloat.class, "op_gt", RubyObject.class));
        floatClass.defineMethod(">=", CallbackFactory.getMethod(RubyFloat.class, "op_ge", RubyObject.class));
        floatClass.defineMethod("<", CallbackFactory.getMethod(RubyFloat.class, "op_lt", RubyObject.class));
        floatClass.defineMethod("<=", CallbackFactory.getMethod(RubyFloat.class, "op_le", RubyObject.class));
		floatClass.defineMethod("hash", CallbackFactory.getMethod(RubyFloat.class, "hash"));

        floatClass.defineMethod("floor", CallbackFactory.getMethod(RubyFloat.class, "floor"));
        floatClass.defineMethod("ceil", CallbackFactory.getMethod(RubyFloat.class, "ceil"));
        floatClass.defineMethod("round", CallbackFactory.getMethod(RubyFloat.class, "round"));
        floatClass.defineMethod("truncate", CallbackFactory.getMethod(RubyFloat.class, "truncate"));

        return floatClass;
    }

    protected int compareValue(RubyNumeric other) {
        double otherVal = other.getDoubleValue();
        return getValue() > otherVal ? 1 : getValue() < otherVal ? -1 : 0;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, new Double(value).hashCode());
    }

    // Float methods (flo_*)

    /**
     *
     */
    public static RubyFloat newFloat(Ruby ruby, double value) {
        return new RubyFloat(ruby, value);
    }

    public static RubyFloat induced_from(Ruby ruby, RubyObject recv, RubyObject number) {
        if (number instanceof RubyFloat) {
            return (RubyFloat) number;
        } else if (number instanceof RubyInteger) {
            return (RubyFloat) number.callMethod("to_f");
        } else {
            throw new TypeError(ruby, "failed to convert " + number.getInternalClass() + "into Float");
        }
    }

    public RubyArray coerce(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyArray.newArray(getRuby(), newFloat(getRuby(), other.getDoubleValue()), this);
    }

    public RubyInteger ceil() {
        double val = Math.ceil(getDoubleValue());

        if (val < Long.MIN_VALUE || val > Long.MAX_VALUE) {
            return RubyBignum.newBignum(getRuby(), val);
        } else {
            return RubyFixnum.newFixnum(getRuby(), (long) val);
        }
    }

    public RubyInteger floor() {
        double val = Math.floor(getDoubleValue());

        if (val < Long.MIN_VALUE || val > Long.MAX_VALUE) {
            return RubyBignum.newBignum(getRuby(), val);
        } else {
            return RubyFixnum.newFixnum(getRuby(), (long) val);
        }
    }

    public RubyInteger round() {
        double val = Math.round(getDoubleValue());

        if (val < Long.MIN_VALUE || val > Long.MAX_VALUE) {
            return RubyBignum.newBignum(getRuby(), val);
        } else {
            return RubyFixnum.newFixnum(getRuby(), (long) val);
        }
    }

    public RubyInteger truncate() {
        if (getDoubleValue() > 0.0) {
            return floor();
        } else if (getDoubleValue() < 0.0) {
            return ceil();
        } else {
            return RubyFixnum.zero(getRuby());
        }
    }

    public RubyNumeric op_uminus() {
        return RubyFloat.newFloat(getRuby(), -value);
    }

    public final RubyNumeric op_plus(final RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), getDoubleValue() + other.getDoubleValue());
    }

    public RubyNumeric op_minus(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), getDoubleValue() - other.getDoubleValue());
    }

    public RubyNumeric op_mul(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), getDoubleValue() * other.getDoubleValue());
    }

    public RubyNumeric op_div(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), getDoubleValue() / other.getDoubleValue());
    }

    public RubyNumeric op_mod(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), getDoubleValue() % other.getDoubleValue());
    }

    public RubyNumeric op_pow(RubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuby(), Math.pow(getDoubleValue(), other.getDoubleValue()));
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
        return RubyString.newString(getRuby(), usFormat.format(getValue()));
    }

  public RubyFloat to_f() {
    return this;
  }

    public RubyInteger to_i() {
        // HACK +++
        return RubyFixnum.newFixnum(getRuby(), getLongValue());
        // HACK ---
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('f');
		String strValue = this.toString();
		double value = getValue();
		if (Double.isInfinite(value)) {
			strValue = (value < 0 ? "-inf" : "inf");
		} else if (Double.isNaN(value)) {
			strValue = "nan";
		}
		output.dumpString(strValue);
	}
}
