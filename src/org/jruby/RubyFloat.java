/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Don Schwartz <schwardo@users.sourceforge.net>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyFloat extends RubyNumeric {
    private final double value;

    public RubyFloat(IRuby runtime) {
        this(runtime, 0.0);
    }

    public RubyFloat(IRuby runtime, double value) {
        super(runtime, runtime.getClass("Float"));
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
    
    public RubyFloat convertToFloat() {
    	return this;
    }

    public static RubyClass createFloatClass(IRuby runtime) {
        RubyClass result = runtime.defineClass("Float", runtime.getClass("Numeric"));
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFloat.class);
        
        result.defineMethod("+", callbackFactory.getMethod("op_plus", IRubyObject.class));
        result.defineMethod("-", callbackFactory.getMethod("op_minus", IRubyObject.class));
        result.defineMethod("*", callbackFactory.getMethod("op_mul", IRubyObject.class));
        result.defineMethod("/", callbackFactory.getMethod("op_div", IRubyObject.class));
        result.defineMethod("%", callbackFactory.getMethod("op_mod", IRubyObject.class));
        result.defineMethod("**", callbackFactory.getMethod("op_pow", IRubyObject.class));
        // Although not explicitly documented in the Pickaxe, Ruby 1.8 Float
        // does have its own implementations of these relational operators.
        // These appear to be necessary if for no oher reason than proper NaN
        // handling.
        result.defineMethod("==", callbackFactory.getMethod("equal", IRubyObject.class));
        result.defineMethod("<=>", callbackFactory.getMethod("cmp",
                IRubyObject.class));
        result.defineMethod(">", callbackFactory.getMethod("op_gt",
                IRubyObject.class));
        result.defineMethod(">=", callbackFactory.getMethod("op_ge", IRubyObject.class));
        result.defineMethod("<", callbackFactory.getMethod("op_lt", IRubyObject.class));
        result.defineMethod("<=", callbackFactory.getMethod("op_le", IRubyObject.class));
        result.defineMethod("ceil", callbackFactory.getMethod("ceil"));
        result.defineMethod("finite?", callbackFactory.getMethod("finite_p"));
        result.defineMethod("floor", callbackFactory.getMethod("floor"));
        result.defineMethod("hash", callbackFactory.getMethod("hash"));
        result.defineMethod("infinite?", callbackFactory.getMethod("infinite_p"));
        result.defineMethod("nan?", callbackFactory.getMethod("nan_p"));
        result.defineMethod("round", callbackFactory.getMethod("round"));
        result.defineMethod("to_i", callbackFactory.getMethod("to_i"));
        result.defineAlias("to_int", "to_i");
        result.defineMethod("to_f", callbackFactory.getMethod("to_f"));
        result.defineMethod("to_s", callbackFactory.getMethod("to_s"));
        result.defineMethod("truncate", callbackFactory.getMethod("truncate"));

        result.getMetaClass().undefineMethod("new");
        result.defineSingletonMethod("induced_from", callbackFactory.getSingletonMethod("induced_from", IRubyObject.class));
        return result;
    }

    protected int compareValue(RubyNumeric other) {
        double otherVal = other.getDoubleValue();
        return getValue() > otherVal ? 1 : getValue() < otherVal ? -1 : 0;
    }

    public RubyFixnum hash() {
        return getRuntime().newFixnum(new Double(value).hashCode());
    }

    // Float methods (flo_*)

    public static RubyFloat newFloat(IRuby runtime, double value) {
        return new RubyFloat(runtime, value);
    }

    public static RubyFloat induced_from(IRubyObject recv, IRubyObject number) {
        if (number instanceof RubyFloat) {
            return (RubyFloat) number;
        } else if (number instanceof RubyInteger) {
            return (RubyFloat) number.callMethod(number.getRuntime().getCurrentContext(), "to_f");
        } else {
            throw recv.getRuntime().newTypeError("failed to convert " + number.getMetaClass() + " into Float");
        }
    }

    public RubyArray coerce(RubyNumeric other) {
        return getRuntime().newArray(newFloat(getRuntime(), other.getDoubleValue()), this);
    }

    public RubyInteger ceil() {
        double val = Math.ceil(getDoubleValue());

        if (val < RubyFixnum.MIN || val > RubyFixnum.MAX) {
            return RubyBignum.newBignum(getRuntime(), val);
        }
		return getRuntime().newFixnum((long) val);
    }

    public RubyInteger floor() {
        double val = Math.floor(getDoubleValue());

        if (val < Long.MIN_VALUE || val > Long.MAX_VALUE) {
            return RubyBignum.newBignum(getRuntime(), val);
        }
		return getRuntime().newFixnum((long) val);
    }

    public RubyInteger round() {
        double decimal = value % 1;
        double round = Math.round(value);

        // Ruby rounds differently than java for negative numbers.
        if (value < 0 && decimal == -0.5) {
            round -= 1;
        }

        if (value < RubyFixnum.MIN || value > RubyFixnum.MAX) {
            return RubyBignum.newBignum(getRuntime(), round);
        }
        return getRuntime().newFixnum((long) round);
    }

    public RubyInteger truncate() {
        if (value > 0.0) {
            return floor();
        } else if (value < 0.0) {
            return ceil();
        } else {
            return RubyFixnum.zero(getRuntime());
        }
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
    
    public IRubyObject op_div(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), 
                getDoubleValue() / ((RubyNumeric) other).getDoubleValue());
    	}
    	
    	return callCoerced("/", other);
    }

    public IRubyObject op_mod(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            // Modelled after c ruby implementation (java /,% not same as ruby)
            double x = getDoubleValue();
            double y = ((RubyNumeric) other).getDoubleValue();
            double mod = x % y;

            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                mod += y;
            }

            return RubyFloat.newFloat(getRuntime(), mod);
    	}
    	
    	return callCoerced("%", other);
    }

    public IRubyObject op_minus(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), 
                getDoubleValue() - ((RubyNumeric) other).getDoubleValue());
    	}

    	return callCoerced("-", other);
    }

    // TODO: Anders double-dispatch here does not seem like it has much benefit when we need
    // to dynamically check to see if we need to coerce first.
    public IRubyObject op_mul(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return ((RubyNumeric) other).multiplyWith(this);
    	}
    	
    	return callCoerced("*", other);
    }

    public IRubyObject op_plus(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(),
                getDoubleValue() + ((RubyNumeric) other).getDoubleValue());
    	}
    	
    	return callCoerced("+", other);
    }

    public IRubyObject op_pow(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), 
                Math.pow(getDoubleValue(), ((RubyNumeric) other).getDoubleValue()));
    	}
    	
    	return callCoerced("**", other);
    }

    public IRubyObject op_uminus() {
        return RubyFloat.newFloat(getRuntime(), -value);
    }

    public IRubyObject to_s() {
        return getRuntime().newString("" + getValue());
    }

    public RubyFloat to_f() {
        return this;
    }

    public RubyInteger to_i() {
    	if (value > Integer.MAX_VALUE) {
    		return RubyBignum.newBignum(getRuntime(), getValue());
    	}
        return getRuntime().newFixnum(getLongValue());
    }

    public IRubyObject infinite_p() {
        if (getValue() == Double.POSITIVE_INFINITY) {
            return getRuntime().newFixnum(1);
        } else if (getValue() == Double.NEGATIVE_INFINITY) {
            return getRuntime().newFixnum(-1);
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
        return getRuntime().newBoolean(Double.isNaN(getValue()));
    }

    public RubyBoolean zero_p() {
        return getRuntime().newBoolean(getValue() == 0);
    }

	public void marshalTo(MarshalStream output) throws java.io.IOException {
		output.write('f');
		String strValue = this.toString();

		if (Double.isInfinite(value)) {
			strValue = value < 0 ? "-inf" : "inf";
		} else if (Double.isNaN(value)) {
			strValue = "nan";
		}
		output.dumpString(strValue);
	}
	
    public static RubyFloat unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return RubyFloat.newFloat(input.getRuntime(),
                                    Double.parseDouble(input.unmarshalString()));
    }
    
    /* flo_eq */
    public IRubyObject equal(IRubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return other.callMethod(getRuntime().getCurrentContext(), "==", this);
        }

        double otherValue = ((RubyNumeric) other).getDoubleValue();
        
        if (other instanceof RubyFloat && Double.isNaN(otherValue)) {
            return getRuntime().getFalse();
        }
        
        return (value == otherValue) ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    

    /* flo_cmp */
    public IRubyObject cmp(IRubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            IRubyObject[] tmp = getCoerced(other, false);
            if (tmp == null) {
                return getRuntime().getNil();
            }
            return tmp[1].callMethod(getRuntime().getCurrentContext(), "<=>", tmp[0]);
        }

        return doubleCompare(((RubyNumeric) other).getDoubleValue());
    }

    
    private void cmperr(IRubyObject other) {
        String message = "comparison of " + this.getType() + " with " + other.getType() + " failed";

        throw this.getRuntime().newArgumentError(message);
    }

    /* flo_gt */
    public IRubyObject op_gt(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }

        if (!(other instanceof RubyNumeric)) {
            IRubyObject[] tmp = getCoerced(other, false);
            if (tmp == null) {
                cmperr(other);
            }
            
            return tmp[1].callMethod(getRuntime().getCurrentContext(), "<=>", tmp[0]);
        }
        
        double oth = ((RubyNumeric) other).getDoubleValue();
        
        if (other instanceof RubyFloat && Double.isNaN(oth)) { 
            return getRuntime().getFalse();
        }
        
        return value > oth ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    /* flo_ge */
    public IRubyObject op_ge(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }

        if (!(other instanceof RubyNumeric)) {
            IRubyObject[] tmp = getCoerced(other, false);
            if (tmp == null) {
                cmperr(other);
            }
            
            return tmp[1].callMethod(getRuntime().getCurrentContext(), "<=>", tmp[0]);
        }
        
        double oth = ((RubyNumeric) other).getDoubleValue();
        
        if (other instanceof RubyFloat && Double.isNaN(oth)) { 
            return getRuntime().getFalse();
        }
        
        return value >= oth ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /* flo_lt */
    public IRubyObject op_lt(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }

        if (!(other instanceof RubyNumeric)) {
            IRubyObject[] tmp = getCoerced(other, false);
            if (tmp == null) {
                cmperr(other);
            }
            
            return tmp[1].callMethod(getRuntime().getCurrentContext(), "<=>", tmp[0]);
        }
        
        double oth = ((RubyNumeric) other).getDoubleValue();
        
        if (other instanceof RubyFloat && Double.isNaN(oth)) { 
            return getRuntime().getFalse();
        }
        
        return value < oth ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    
    /* flo_le */
    public IRubyObject op_le(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }

        if (!(other instanceof RubyNumeric)) {
            IRubyObject[] tmp = getCoerced(other, false);
            if (tmp == null) {
                cmperr(other);
            }
            
            return tmp[1].callMethod(getRuntime().getCurrentContext(), "<=>", tmp[0]);
        }
        
        double oth = ((RubyNumeric) other).getDoubleValue();
        
        if (other instanceof RubyFloat && Double.isNaN(oth)) { 
            return getRuntime().getFalse();
        }
        
        return value <= oth ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    
    /* dbl_cmp */
    private IRubyObject doubleCompare(double oth) {
        if (Double.isNaN(value) || Double.isNaN(oth)) {
            return getRuntime().getNil();
        }
        
        if (value == oth) {
            return getRuntime().newFixnum(0);
        }
        
        return value > oth ? getRuntime().newFixnum(1) : getRuntime().newFixnum(-1);
    }
    
}
