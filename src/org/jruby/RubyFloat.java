/*
 * RubyFloat.java - No description
 * Created on 04. Juli 2001, 22:53
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina
 * Copyright (C) 2002-2004 Thomas E Enebo
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

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;

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
        RubyClass result = ruby.defineClass("Float", ruby.getClasses().getNumericClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();
        
        result.defineMethod("+", callbackFactory.getMethod(RubyFloat.class, "op_plus", RubyNumeric.class));
        result.defineMethod("-", callbackFactory.getMethod(RubyFloat.class, "op_minus", RubyNumeric.class));
        result.defineMethod("*", callbackFactory.getMethod(RubyFloat.class, "op_mul", IRubyObject.class));
        result.defineMethod("/", callbackFactory.getMethod(RubyFloat.class, "op_div", RubyNumeric.class));
        result.defineMethod("%", callbackFactory.getMethod(RubyFloat.class, "op_mod", RubyNumeric.class));
        result.defineMethod("**", callbackFactory.getMethod(RubyFloat.class, "op_pow", RubyNumeric.class));
        result.defineMethod("ceil", callbackFactory.getMethod(RubyFloat.class, "ceil"));
        result.defineMethod("finite?", callbackFactory.getMethod(RubyFloat.class, "finite_p"));
        result.defineMethod("floor", callbackFactory.getMethod(RubyFloat.class, "floor"));
        result.defineMethod("hash", callbackFactory.getMethod(RubyFloat.class, "hash"));
        result.defineMethod("infinite?", callbackFactory.getMethod(RubyFloat.class, "infinite_p"));
        result.defineMethod("nan?", callbackFactory.getMethod(RubyFloat.class, "nan_p"));
        result.defineMethod("round", callbackFactory.getMethod(RubyFloat.class, "round"));
        result.defineMethod("to_i", callbackFactory.getMethod(RubyFloat.class, "to_i"));
        result.defineMethod("to_f", callbackFactory.getMethod(RubyFloat.class, "to_f"));
        result.defineMethod("to_s", callbackFactory.getMethod(RubyFloat.class, "to_s"));
        result.defineMethod("truncate", callbackFactory.getMethod(RubyFloat.class, "truncate"));

        result.getMetaClass().undefineMethod("new");
        result.defineSingletonMethod("induced_from", callbackFactory.getSingletonMethod(RubyFloat.class, "induced_from", IRubyObject.class));
        return result;
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

    public RubyArray coerce(RubyNumeric other) {
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

    public RubyNumeric op_plus(RubyNumeric other) {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() + other.getDoubleValue());
    }

    public RubyNumeric op_minus(RubyNumeric other) {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() - other.getDoubleValue());
    }

    // TODO: Coercion messages needed for all ops...Does this sink Anders
    // dispatching optimization?
    public RubyNumeric op_mul(IRubyObject other) {
    	if (other instanceof RubyNumeric == false) {
    		throw new TypeError(getRuntime(), other.getMetaClass().getName() +
    			" can't be coerced into Float");
    	}
    	
        return ((RubyNumeric) other).multiplyWith(this);
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
    
    public RubyNumeric op_div(RubyNumeric other) {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue() / other.getDoubleValue());
    }

    public RubyNumeric op_mod(RubyNumeric other) {
        // Modelled after c ruby implementation (java /,% not same as ruby)
        double x = getDoubleValue();
        double y = other.getDoubleValue();
        double mod = x % y;

        if ((mod < 0 && y > 0) || (mod > 0 && y < 0)) {
            mod += y;
        }

        return RubyFloat.newFloat(getRuntime(), mod);
    }

    public RubyNumeric op_pow(RubyNumeric other) {
        return RubyFloat.newFloat(getRuntime(), 
                Math.pow(getDoubleValue(), other.getDoubleValue()));
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), "" + getValue());
    }

    public RubyFloat to_f() {
        return this;
    }

    public RubyInteger to_i() {
    	if (value > Integer.MAX_VALUE) {
    		return RubyBignum.newBignum(getRuntime(), getValue());
    	}
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
