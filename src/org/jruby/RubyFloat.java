/*
 * RubyFloat.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2002 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.exceptions.TypeError;
import org.jruby.internal.runtime.builtin.definitions.FloatDefinition;

/**
 *
 * @author  jpetersen
 */
public class RubyFloat extends RubyNumeric {
    private final double value;

    public RubyFloat(Ruby ruby) {
        this(ruby, 0.0);
    }

    public RubyFloat(Ruby ruby, double value) {
        super(ruby, ruby.getClass("Float"));
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

    public double getDoubleValue() {
        return value;
    }

    public long getLongValue() {
        return (long) value;
    }

    public static RubyClass createFloatClass(Ruby ruby) {
        return new FloatDefinition(ruby).getType();
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
        case FloatDefinition.CEIL:
            return ceil();
        case FloatDefinition.FINITE_P:
            return finite_p();
        case FloatDefinition.FLOOR:
            return floor();
        case FloatDefinition.INFINITE_P:
            return infinite_p();
        case FloatDefinition.NAN_P:
            return nan_p();
        case FloatDefinition.ROUND:
            return round();
        case FloatDefinition.TRUNCATE:
            return truncate();
        case FloatDefinition.TO_F:
            return to_f();
        case FloatDefinition.TO_I:
            return to_i();
        case FloatDefinition.TO_S:
            return to_s();
        case FloatDefinition.ZERO_P:
            return zero_p();
        case FloatDefinition.OP_LT:
            return op_lt(args[0]);
        case FloatDefinition.OP_LE:
            return op_le(args[0]);
        case FloatDefinition.OP_CMP:
            return op_cmp(args[0]);
        case FloatDefinition.OP_GT:
            return op_gt(args[0]);
        case FloatDefinition.OP_GE:
            return op_ge(args[0]);
        case FloatDefinition.OP_MOD:
            return op_mod(args[0]);
        case FloatDefinition.OP_DIV:
            return op_div(args[0]);
        case FloatDefinition.OP_PLUS:
            return op_plus(args[0]);
        case FloatDefinition.OP_MINUS:
            return op_minus(args[0]);
        case FloatDefinition.OP_MUL:
            return op_mul(args[0]);
        case FloatDefinition.OP_POW:
            return op_pow(args[0]);
        case FloatDefinition.OP_EQL:
            return op_eql(args[0]);
        case FloatDefinition.OP_EQUAL:
            return op_equal(args[0]);
        case FloatDefinition.HASH:
            return hash();
	}

        return super.callIndexed(index, args);
    }

    protected int compareValue(RubyNumeric other) {
        double otherVal = other.getDoubleValue();
        return getValue() > otherVal ? 1 : getValue() < otherVal ? -1 : 0;
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(runtime, new Double(value).hashCode());
    }

    // Float methods (flo_*)

    /**
     *
     */
    public static RubyFloat newFloat(Ruby ruby, double value) {
        return new RubyFloat(ruby, value);
    }

    public static RubyFloat induced_from(IRubyObject recv, IRubyObject number) {
        if (number instanceof RubyFloat) {
            return (RubyFloat) number;
        } else if (number instanceof RubyInteger) {
            return (RubyFloat) number.callMethod("to_f");
        } else {
            throw new TypeError(recv.getRuntime(), "failed to convert " + number.getMetaClass() + " into Float");
        }
    }

    public RubyArray coerce(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyArray.newArray(getRuntime(), newFloat(getRuntime(), other.getDoubleValue()), this);
    }

    public RubyInteger ceil() {
        double val = Math.ceil(getDoubleValue());

        if (val < RubyFixnum.MIN || val > RubyFixnum.MAX) {
            return RubyBignum.newBignum(getRuntime(), val);
        } else {
            return RubyFixnum.newFixnum(getRuntime(), (long) val);
        }
    }

    public RubyInteger floor() {
        double val = Math.floor(getDoubleValue());

        if (val < Long.MIN_VALUE || val > Long.MAX_VALUE) {
            return RubyBignum.newBignum(getRuntime(), val);
        } else {
            return RubyFixnum.newFixnum(getRuntime(), (long) val);
        }
    }

    public RubyInteger round() {
	double value = getDoubleValue();
	double decimal = value % 1;
	double round = Math.round(value);

	// Ruby rounds differently than java for negative numbers.
	if (value < 0 && decimal == -0.5) {
	    round -= 1;
	}

        if (value < RubyFixnum.MIN || value > RubyFixnum.MAX) {
            return RubyBignum.newBignum(getRuntime(), round);
        } else {
            return RubyFixnum.newFixnum(getRuntime(), (long) round);
        }
    }

    public RubyInteger truncate() {
        if (getDoubleValue() > 0.0) {
            return floor();
        } else if (getDoubleValue() < 0.0) {
            return ceil();
        } else {
            return RubyFixnum.zero(getRuntime());
        }
    }

    public RubyNumeric op_uminus() {
        return RubyFloat.newFloat(getRuntime(), -value);
    }

    public RubyNumeric op_plus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() + other.getDoubleValue());
    }

    public RubyNumeric op_minus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() - other.getDoubleValue());
    }

    public RubyNumeric op_mul(IRubyObject num) {
        return numericValue(num).multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyNumeric other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() * other.getDoubleValue());
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyBignum other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric op_div(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() / other.getDoubleValue());
    }

    public RubyNumeric op_mod(IRubyObject num) {
        RubyNumeric other = numericValue(num);

        // Modelled after c ruby implementation (java /,% not same as ruby)
        double x = getDoubleValue();
        double y = other.getDoubleValue();
        double mod = x % y;

        if ((mod < 0 && y > 0) || (mod > 0 && y < 0)) {
            mod += y;
        }

        return RubyFloat.newFloat(getRuntime(), mod);
    }

    public RubyNumeric op_pow(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFloat.newFloat(getRuntime(), Math.pow(getDoubleValue(), other.getDoubleValue()));
    }

    public RubyBoolean op_eql(IRubyObject other) {
        if (other instanceof RubyFloat == false) {
            return getRuntime().getFalse();
        } 
          
	return RubyBoolean.newBoolean(getRuntime(), 
				      compareValue((RubyNumeric) other) == 0);
    }

    public RubyBoolean op_equal(IRubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuntime().getFalse();
        } else {
            return RubyBoolean.newBoolean(getRuntime(), compareValue((RubyNumeric) other) == 0);
        }
    }

    public RubyNumeric op_cmp(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.newFixnum(getRuntime(), compareValue(other));
    }

    public RubyBoolean op_gt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) >= 0);
    }

    public RubyBoolean op_lt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) <= 0);
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), "" + getValue());
    }

    public RubyFloat to_f() {
        return this;
    }

    public RubyInteger to_i() {
        return RubyFixnum.newFixnum(getRuntime(), getLongValue());
    }

    public IRubyObject infinite_p() {
        if (getValue() == Double.POSITIVE_INFINITY) {
            return RubyFixnum.newFixnum(getRuntime(), 1);
        } else if (getValue() == Double.NEGATIVE_INFINITY) {
            return RubyFixnum.newFixnum(getRuntime(), -1);
        } else {
            return getRuntime().getNil();
        }
    }

    public RubyBoolean finite_p() {
        if (! infinite_p().isNil()) {
            return getRuntime().getFalse();
        }
        if (nan_p().isTrue()) {
            return getRuntime().getFalse();
        }
        return getRuntime().getTrue();
    }

    public RubyBoolean nan_p() {
        return RubyBoolean.newBoolean(getRuntime(), Double.isNaN(getValue()));
    }

    public RubyBoolean zero_p() {
        return RubyBoolean.newBoolean(getRuntime(), getValue() == 0);
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
