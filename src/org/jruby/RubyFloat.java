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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyFloat extends RubyNumeric {

    public static RubyClass createFloatClass(IRuby runtime) {
        RubyClass floatc = runtime.defineClass("Float", runtime.getClass("Numeric"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFloat.class);
        floatc.getSingletonClass().undefineMethod("allocate");
        floatc.getSingletonClass().undefineMethod("new");

        floatc.defineFastSingletonMethod("induced_from", callbackFactory.getFastSingletonMethod(
                "induced_from", IRubyObject.class));
        floatc.includeModule(runtime.getModule("Precision"));

        // Java Doubles are 64 bit long:            
        floatc.defineConstant("ROUNDS", RubyFixnum.newFixnum(runtime, 1));
        floatc.defineConstant("RADIX", RubyFixnum.newFixnum(runtime, 2));
        floatc.defineConstant("MANT_DIG", RubyFixnum.newFixnum(runtime, 53));
        floatc.defineConstant("DIG", RubyFixnum.newFixnum(runtime, 15));
        // Double.MAX_EXPONENT since Java 1.6
        floatc.defineConstant("MIN_EXP", RubyFixnum.newFixnum(runtime, -1021));
        // Double.MAX_EXPONENT since Java 1.6            
        floatc.defineConstant("MAX_EXP", RubyFixnum.newFixnum(runtime, 1024));
        floatc.defineConstant("MIN_10_EXP", RubyFixnum.newFixnum(runtime, -307));
        floatc.defineConstant("MAX_10_EXP", RubyFixnum.newFixnum(runtime, -308));
        floatc.defineConstant("MIN", RubyFloat.newFloat(runtime, Double.MIN_VALUE));
        floatc.defineConstant("MAX", RubyFloat.newFloat(runtime, Double.MAX_VALUE));
        floatc.defineConstant("EPSILON", RubyFloat.newFloat(runtime, 2.2204460492503131e-16));

        floatc.defineFastMethod("to_s", callbackFactory.getFastMethod("to_s"));
        floatc.defineFastMethod("coerce", callbackFactory.getFastMethod("coerce", IRubyObject.class));
        floatc.defineFastMethod("-@", callbackFactory.getFastMethod("uminus"));
        floatc.defineFastMethod("+", callbackFactory.getFastMethod("plus", IRubyObject.class));
        floatc.defineFastMethod("-", callbackFactory.getFastMethod("minus", IRubyObject.class));
        floatc.defineFastMethod("*", callbackFactory.getFastMethod("mul", IRubyObject.class));
        floatc.defineFastMethod("/", callbackFactory.getFastMethod("fdiv", IRubyObject.class));
        floatc.defineFastMethod("%", callbackFactory.getFastMethod("mod", IRubyObject.class));
        floatc.defineFastMethod("modulo", callbackFactory.getFastMethod("mod", IRubyObject.class));
        floatc.defineFastMethod("divmod", callbackFactory.getFastMethod("divmod", IRubyObject.class));
        floatc.defineFastMethod("**", callbackFactory.getFastMethod("pow", IRubyObject.class));
        floatc.defineFastMethod("==", callbackFactory.getFastMethod("equal", IRubyObject.class));
        floatc.defineFastMethod("<=>", callbackFactory.getFastMethod("cmp", IRubyObject.class));
        floatc.defineFastMethod(">", callbackFactory.getFastMethod("gt", IRubyObject.class));
        floatc.defineFastMethod(">=", callbackFactory.getFastMethod("ge", IRubyObject.class));
        floatc.defineFastMethod("<", callbackFactory.getFastMethod("lt", IRubyObject.class));
        floatc.defineFastMethod("<=", callbackFactory.getFastMethod("le", IRubyObject.class));
        floatc.defineFastMethod("eql?", callbackFactory.getFastMethod("eql_p", IRubyObject.class));
        floatc.defineFastMethod("hash", callbackFactory.getFastMethod("hash"));
        floatc.defineFastMethod("to_f", callbackFactory.getFastMethod("to_f"));
        floatc.defineFastMethod("abs", callbackFactory.getFastMethod("abs"));
        floatc.defineFastMethod("zero?", callbackFactory.getFastMethod("zero_p"));

        floatc.defineFastMethod("to_i", callbackFactory.getFastMethod("truncate"));
        floatc.defineFastMethod("to_int", callbackFactory.getFastMethod("truncate"));
        floatc.defineFastMethod("floor", callbackFactory.getFastMethod("floor"));
        floatc.defineFastMethod("ceil", callbackFactory.getFastMethod("ceil"));
        floatc.defineFastMethod("round", callbackFactory.getFastMethod("round"));
        floatc.defineFastMethod("truncate", callbackFactory.getFastMethod("truncate"));

        floatc.defineFastMethod("nan?", callbackFactory.getFastMethod("nan_p"));
        floatc.defineFastMethod("infinite?", callbackFactory.getFastMethod("infinite_p"));
        floatc.defineFastMethod("finite?", callbackFactory.getFastMethod("finite_p"));

        return floatc;
    }

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

    protected int compareValue(RubyNumeric other) {
        double otherVal = other.getDoubleValue();
        return getValue() > otherVal ? 1 : getValue() < otherVal ? -1 : 0;
    }

    public static RubyFloat newFloat(IRuby runtime, double value) {
        return new RubyFloat(runtime, value);
    }

    /*  ================
     *  Instance Methods
     *  ================ 
     */

    /** rb_flo_induced_from
     * 
     */
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject number) {
        if (number instanceof RubyFixnum || number instanceof RubyBignum) {
            return number.callMethod(recv.getRuntime().getCurrentContext(), "to_f");
    }
        if (number instanceof RubyFloat) {
            return number;
        }
        throw recv.getRuntime().newTypeError(
                "failed to convert " + number.getMetaClass() + " into Float");
    }

    /** flo_to_s
     * 
     */
    public IRubyObject to_s() {
        if (Double.isInfinite(value)) {
            return RubyString.newString(getRuntime(), value < 0 ? "-Infinity" : "Infinity");
    }

        if (Double.isNaN(value)) {
            return RubyString.newString(getRuntime(), "NaN");
        }

        // TODO: needs formatting "%#.15g" and "%#.14e"
        return RubyString.newString(getRuntime(), "" + value);
        }

    /** flo_coerce
     * 
     */
    public IRubyObject coerce(IRubyObject other) {
        // MRI doesn't type check here either
        return getRuntime().newArray(
                newFloat(getRuntime(), ((RubyNumeric) other).getDoubleValue()), this);
        }

    /** flo_uminus
     * 
     */
    public IRubyObject uminus() {
        return RubyFloat.newFloat(getRuntime(), -value);
        }

    /** flo_plus
     * 
     */
    public IRubyObject plus(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value + ((RubyNumeric) other).getDoubleValue());
        }
        return coerceBin("+", other);
    }

    /** flo_minus
     * 
     */
    public IRubyObject minus(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value - ((RubyNumeric) other).getDoubleValue());
    }
        return coerceBin("-", other);
    }

    /** flo_mul
     * 
     */
    public IRubyObject mul(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value * ((RubyNumeric) other).getDoubleValue());
    }
        return coerceBin("*", other);
    }
    
    /** flo_div
     * 
     */
    public IRubyObject fdiv(IRubyObject other) { // don't override Numeric#div !
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value / ((RubyNumeric) other).getDoubleValue());
    	}
        return coerceBin("div", other);
    }

    /** flo_mod
     * 
     */
    public IRubyObject mod(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            double y = ((RubyNumeric) other).getDoubleValue();
            // Modelled after c ruby implementation (java /,% not same as ruby)
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            if (y * mod < 0) {
                mod += y;
            }

            return RubyFloat.newFloat(getRuntime(), mod);
    	}
        return coerceBin("%", other);
    }

    /** flo_divmod
     * 
     */
    public IRubyObject divmod(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            double y = ((RubyNumeric) other).getDoubleValue();
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            double div = (x - mod) / y;

            if (y * mod < 0) {
                mod += y;
                div -= 1.0;
    	}
            final IRuby runtime = getRuntime();
            IRubyObject car = dbl2num(runtime, div);
            RubyFloat cdr = RubyFloat.newFloat(runtime, mod);
            return RubyArray.newArray(runtime, car, cdr);
    }
        return coerceBin("%", other);
    	}
    	
    /** flo_pow
     * 
     */
    public IRubyObject pow(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyNumeric) other)
                    .getDoubleValue()));
    	}
        return coerceBin("/", other);
    }

    /** flo_eq
     * 
     */
    public IRubyObject equal(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
    }
        if (other instanceof RubyNumeric) {
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyNumeric) other)
                    .getDoubleValue());
    }
        // Numeric.equal            
        return super.equal(other);

    }

    /** flo_cmp
     * 
     */
    public IRubyObject cmp(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return dbl_cmp(getRuntime(), value, b);
    	}
        return coerceCmp("<=>", other);
    }

    /** flo_gt
     * 
     */
    public IRubyObject gt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value > b);
        }
        return coerceRelOp(">", other);
    }

    /** flo_ge
     * 
     */
    public IRubyObject ge(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value >= b);
        }
        return coerceRelOp(">=", other);
        }

    /** flo_lt
     * 
     */
    public IRubyObject lt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value < b);
    }
        return coerceRelOp("<", other);
    }

    /** flo_le
     * 
     */
    public IRubyObject le(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value <= b);
		}
        return coerceRelOp("<=", other);
	}
	
    /** flo_eql
     * 
     */
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyFloat) {
            double b = ((RubyFloat) other).value;
            if (Double.isNaN(value) || Double.isNaN(b)) {
            return getRuntime().getFalse();
        }
            if (value == b) {
                return getRuntime().getTrue();
    }
            }
            return getRuntime().getFalse();
        }

    /** flo_hash
     * 
     */
    public RubyFixnum hash() {
        long l = Double.doubleToLongBits(value);
        return getRuntime().newFixnum((long) (l ^ l >>> 32));
            }
            
    /** flo_fo 
     * 
     */
    public IRubyObject to_f() {
        return this;
        }
        
    /** flo_abs
     * 
     */
    public IRubyObject abs() {
        if (value < 0) {
            return RubyFloat.newFloat(getRuntime(), Math.abs(value));
        }
        return this;
    }
    
    /** flo_zero_p
     * 
     */
    public IRubyObject zero_p() {
        return RubyBoolean.newBoolean(getRuntime(), value == 0.0);
        }

    /** flo_truncate
     * 
     */
    public IRubyObject truncate() {
        double f = value;
        if (f > 0.0) {
            f = Math.floor(f);
            }
        if (f > 0.0) {
            f = Math.ceil(f);
        }
        return dbl2num(getRuntime(), f);
        }
        
    /** loor
     * 
     */
    public IRubyObject floor() {
        return dbl2num(getRuntime(), Math.floor(value));
    }

    /** flo_ceil
     * 
     */
    public IRubyObject ceil() {
        return dbl2num(getRuntime(), Math.ceil(value));
        }

    /** flo_round
     * 
     */
    public IRubyObject round() {
        double f = value;
        if (f > 0.0) {
            f = Math.floor(f + 0.5);
            }
        if (f < 0.0) {
            f = Math.ceil(f - 0.5);
        }
        return dbl2num(getRuntime(), f);
        }
        
    /** flo_is_nan_p
     * 
     */
    public IRubyObject nan_p() {
        return RubyBoolean.newBoolean(getRuntime(), Double.isNaN(value));
    }

    /** flo_is_infinite_p
     * 
     */
    public IRubyObject infinite_p() {
        if (Double.isInfinite(value)) {
            return RubyFixnum.newFixnum(getRuntime(), value < 0 ? -1 : 1);
        }
        return getRuntime().getNil();
            }
            
    /** flo_is_finite_p
     * 
     */
    public IRubyObject finite_p() {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return getRuntime().getFalse();
        }
        return getRuntime().getTrue();
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
        return RubyFloat.newFloat(input.getRuntime(), Double.parseDouble(input.unmarshalString()));
    }
    
}
