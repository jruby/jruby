/*
 ***** BEGIN LICENSE BLOCK *****
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
  * A representation of a float object
 */
public class RubyFloat extends RubyNumeric {

    public static RubyClass createFloatClass(Ruby runtime) {
        RubyClass floatc = runtime.defineClass("Float", runtime.getNumeric(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setFloat(floatc);
        floatc.index = ClassIndex.FLOAT;
        floatc.kindOf = new RubyModule.KindOf() {
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyFloat;
            }
        };        
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyFloat.class);
        floatc.getSingletonClass().undefineMethod("new");

        floatc.getMetaClass().defineFastMethod("induced_from", callbackFactory.getFastSingletonMethod(
                "induced_from", RubyKernel.IRUBY_OBJECT));
        floatc.includeModule(runtime.getPrecision());

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
        
        floatc.defineAnnotatedMethods(RubyFloat.class);
        floatc.dispatcher = callbackFactory.createDispatcher(floatc);

        return floatc;
    }

    private final double value;
    
    public int getNativeTypeIndex() {
        return ClassIndex.FLOAT;
    }

    public RubyFloat(Ruby runtime) {
        this(runtime, 0.0);
    }

    public RubyFloat(Ruby runtime, double value) {
        super(runtime, runtime.getFloat());
        this.value = value;
    }

    public Class<?> getJavaClass() {
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

    public static RubyFloat newFloat(Ruby runtime, double value) {
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
            return number.callMethod(recv.getRuntime().getCurrentContext(), MethodIndex.TO_F, "to_f");
    }
        if (number instanceof RubyFloat) {
            return number;
        }
        throw recv.getRuntime().newTypeError(
                "failed to convert " + number.getMetaClass() + " into Float");
    }

    private final static DecimalFormat FORMAT = new DecimalFormat("##############0.0##############",
            new DecimalFormatSymbols(Locale.ENGLISH));

    /** flo_to_s
     * 
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
        if (Double.isInfinite(value)) {
            return RubyString.newString(getRuntime(), value < 0 ? "-Infinity" : "Infinity");
        }

        if (Double.isNaN(value)) {
            return RubyString.newString(getRuntime(), "NaN");
        }

        String val = ""+value;

        if(val.indexOf('E') != -1) {
            String v2 = FORMAT.format(value);
            int ix = v2.length()-1;
            while(v2.charAt(ix) == '0' && v2.charAt(ix-1) != '.') {
                ix--;
            }
            if(ix > 15 || "0.0".equals(v2.substring(0,ix+1))) {
                val = val.replaceFirst("E(\\d)","e+$1").replaceFirst("E-","e-");
            } else {
                val = v2.substring(0,ix+1);
            }
        }

        return RubyString.newString(getRuntime(), val);
    }

    /** flo_coerce
     * 
     */
    @JRubyMethod(name = "coerce", required = 1)
    public IRubyObject coerce(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newArray(
                    newFloat(getRuntime(), ((RubyNumeric) other).getDoubleValue()), this);
        }
        throw getRuntime().newTypeError(other, getRuntime().getNumeric());
    }

    /** flo_uminus
     * 
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus() {
        return RubyFloat.newFloat(getRuntime(), -value);
        }

    /** flo_plus
     * 
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value + ((RubyNumeric) other).getDoubleValue());
        }
        return coerceBin("+", other);
    }

    /** flo_minus
     * 
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value - ((RubyNumeric) other).getDoubleValue());
    }
        return coerceBin("-", other);
    }

    /** flo_mul
     * 
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value * ((RubyNumeric) other).getDoubleValue());
    }
        return coerceBin("*", other);
    }
    
    /** flo_div
     * 
     */
    @JRubyMethod(name = "/", required = 1)
    public IRubyObject op_fdiv(IRubyObject other) { // don't override Numeric#div !
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), value / ((RubyNumeric) other).getDoubleValue());
    	}
        return coerceBin("div", other);
    }

    /** flo_mod
     * 
     */
    @JRubyMethod(name = "%", name2 = "modulo", required = 1)
    public IRubyObject op_mod(IRubyObject other) {
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
    @JRubyMethod(name = "divmod", required = 1)
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
            final Ruby runtime = getRuntime();
            IRubyObject car = dbl2num(runtime, div);
            RubyFloat cdr = RubyFloat.newFloat(runtime, mod);
            return RubyArray.newArray(runtime, car, cdr);
    }
        return coerceBin("%", other);
    	}
    	
    /** flo_pow
     * 
     */
    @JRubyMethod(name = "**", required = 1)
    public IRubyObject op_pow(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyNumeric) other)
                    .getDoubleValue()));
    	}
        return coerceBin("/", other);
    }

    /** flo_eq
     * 
     */
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
    }
        if (other instanceof RubyNumeric) {
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyNumeric) other)
                    .getDoubleValue());
    }
        // Numeric.equal            
        return super.op_equal(other);

    }

    /** flo_cmp
     * 
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return dbl_cmp(getRuntime(), value, b);
    	}
        return coerceCmp("<=>", other);
    }

    /** flo_gt
     * 
     */
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value > b);
        }
        return coerceRelOp(">", other);
    }

    /** flo_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value >= b);
        }
        return coerceRelOp(">=", other);
        }

    /** flo_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value < b);
    }
        return coerceRelOp("<", other);
    }

    /** flo_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value <= b);
		}
        return coerceRelOp("<=", other);
	}
	
    /** flo_eql
     * 
     */
    @JRubyMethod(name = "eql?", required = 1)
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
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    public final int hashCode() {
        long l = Double.doubleToLongBits(value);
        return (int)(l ^ l >>> 32);
    }    

    /** flo_fo 
     * 
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return this;
    }
        
    /** flo_abs
     * 
     */
    @JRubyMethod(name = "abs")
    public IRubyObject abs() {
        if (value < 0) {
            return RubyFloat.newFloat(getRuntime(), Math.abs(value));
        }
        return this;
    }
    
    /** flo_zero_p
     * 
     */
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p() {
        return RubyBoolean.newBoolean(getRuntime(), value == 0.0);
        }

    /** flo_truncate
     * 
     */
    @JRubyMethod(name = "truncate", name2 = "to_i", name3 = "to_int")
    public IRubyObject truncate() {
        double f = value;
        if (f > 0.0) f = Math.floor(f);
        if (f < 0.0) f = Math.ceil(f);

        return dbl2num(getRuntime(), f);
    }
        
    /** floor
     * 
     */
    @JRubyMethod(name = "floor")
    public IRubyObject floor() {
        return dbl2num(getRuntime(), Math.floor(value));
    }

    /** flo_ceil
     * 
     */
    @JRubyMethod(name = "ceil")
    public IRubyObject ceil() {
        return dbl2num(getRuntime(), Math.ceil(value));
        }

    /** flo_round
     * 
     */
    @JRubyMethod(name = "round")
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
    @JRubyMethod(name = "nan?")
    public IRubyObject nan_p() {
        return RubyBoolean.newBoolean(getRuntime(), Double.isNaN(value));
    }

    /** flo_is_infinite_p
     * 
     */
    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p() {
        if (Double.isInfinite(value)) {
            return RubyFixnum.newFixnum(getRuntime(), value < 0 ? -1 : 1);
        }
        return getRuntime().getNil();
            }
            
    /** flo_is_finite_p
     * 
     */
    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p() {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return getRuntime().getFalse();
        }
        return getRuntime().getTrue();
    }

    public static void marshalTo(RubyFloat aFloat, MarshalStream output) throws java.io.IOException {
        String strValue = aFloat.toString();
    
        if (Double.isInfinite(aFloat.value)) {
            strValue = aFloat.value < 0 ? "-inf" : "inf";
        } else if (Double.isNaN(aFloat.value)) {
            strValue = "nan";
        }
        output.writeString(strValue);
    }
        
    public static RubyFloat unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyFloat result = RubyFloat.newFloat(input.getRuntime(), org.jruby.util.Convert.byteListToDouble(input.unmarshalString(),false));
        input.registerLinkTarget(result);
        return result;
    }
    
}
