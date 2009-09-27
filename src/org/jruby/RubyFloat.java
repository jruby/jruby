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
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
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

import java.math.BigInteger;
import static org.jruby.util.Numeric.f_expt;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.frexp;
import static org.jruby.util.Numeric.ldexp;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import java.util.regex.Pattern;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.Sprintf;

/**
  * A representation of a float object
 */
@JRubyClass(name="Float", parent="Numeric", include="Precision")
public class RubyFloat extends RubyNumeric {
    public static final int ROUNDS = 1;
    public static final int RADIX = 2;
    public static final int MANT_DIG = 53;
    public static final int DIG = 15;
    public static final int MIN_EXP = -1021;
    public static final int MAX_EXP = 1024;
    public static final int MAX_10_EXP = 308;
    public static final int MIN_10_EXP = -307;
    public static final double EPSILON = 2.2204460492503131e-16;

    public static RubyClass createFloatClass(Ruby runtime) {
        RubyClass floatc = runtime.defineClass("Float", runtime.getNumeric(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setFloat(floatc);
        floatc.index = ClassIndex.FLOAT;
        floatc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyFloat;
            }
        };        

        floatc.getSingletonClass().undefineMethod("new");

        if (!runtime.is1_9()) {
            floatc.includeModule(runtime.getPrecision());
        }

        // Java Doubles are 64 bit long:            
        floatc.defineConstant("ROUNDS", RubyFixnum.newFixnum(runtime, ROUNDS));
        floatc.defineConstant("RADIX", RubyFixnum.newFixnum(runtime, RADIX));
        floatc.defineConstant("MANT_DIG", RubyFixnum.newFixnum(runtime, MANT_DIG));
        floatc.defineConstant("DIG", RubyFixnum.newFixnum(runtime, DIG));
        // Double.MAX_EXPONENT since Java 1.6
        floatc.defineConstant("MIN_EXP", RubyFixnum.newFixnum(runtime, MIN_EXP));
        // Double.MAX_EXPONENT since Java 1.6            
        floatc.defineConstant("MAX_EXP", RubyFixnum.newFixnum(runtime, MAX_EXP));
        floatc.defineConstant("MIN_10_EXP", RubyFixnum.newFixnum(runtime, MIN_10_EXP));
        floatc.defineConstant("MAX_10_EXP", RubyFixnum.newFixnum(runtime, MAX_10_EXP));
        floatc.defineConstant("MIN", RubyFloat.newFloat(runtime, Double.MIN_VALUE));
        floatc.defineConstant("MAX", RubyFloat.newFloat(runtime, Double.MAX_VALUE));
        floatc.defineConstant("EPSILON", RubyFloat.newFloat(runtime, EPSILON));
        
        floatc.defineAnnotatedMethods(RubyFloat.class);

        return floatc;
    }

    private final double value;
    
    @Override
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

    @Override
    public Class<?> getJavaClass() {
        // this needs to be thought out more along with the changes in RubyFixnum
        // since "to Object" coercion will generally want to produce the same
        // type every time
//        if (value >= Float.MIN_VALUE && value <= Float.MAX_VALUE) {
//            return float.class;
//        }
        return double.class;
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public double getValue() {
        return this.value;
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    @Override
    public long getLongValue() {
        return (long) value;
    }

    @Override
    public BigInteger getBigIntegerValue() {
        return BigInteger.valueOf((long)value);
    }
    
    @Override
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
    @JRubyMethod(name = "induced_from", meta = true, compat = CompatVersion.RUBY1_8)
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject number) {
        if (number instanceof RubyFixnum || number instanceof RubyBignum || number instanceof RubyRational) {
            return number.callMethod(context, "to_f");
        } else if (number instanceof RubyFloat) {
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
    @Override
    public IRubyObject to_s() {
        Ruby runtime = getRuntime();
        if (Double.isInfinite(value)) return RubyString.newString(runtime, value < 0 ? "-Infinity" : "Infinity");
        if (Double.isNaN(value)) return RubyString.newString(runtime, "NaN");

        ByteList buf = new ByteList();
        Sprintf.sprintf(buf, Locale.US, "%#.15g", this);
        int e = buf.indexOf('e');
        if (e == -1) e = buf.realSize;
        ASCIIEncoding ascii = ASCIIEncoding.INSTANCE; 

        if (!ascii.isDigit(buf.get(e - 1))) {
            buf.realSize = 0;
            Sprintf.sprintf(buf, Locale.US, "%#.14e", this);
            e = buf.indexOf('e');
            if (e == -1) e = buf.realSize;
        }

        int p = e;
        while (buf.get(p - 1) == '0' && ascii.isDigit(buf.get(p - 2))) p--;
        System.arraycopy(buf.bytes, e, buf.bytes, p, buf.realSize - e);
        buf.realSize = p + buf.realSize - e;
        return runtime.newString(buf);
    }

    /** flo_coerce
     * 
     */
    @JRubyMethod(name = "coerce", required = 1)
    @Override
    public IRubyObject coerce(IRubyObject other) {
        return getRuntime().newArray(RubyKernel.new_float(this, other), this);
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
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyFloat.newFloat(getRuntime(), value + ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "+", other);
        }
    }

    /** flo_minus
     * 
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyFloat.newFloat(getRuntime(), value - ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "-", other);
        }
    }

    /** flo_mul
     * 
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyFloat.newFloat(
                    getRuntime(), value * ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "*", other);
        }
    }
    
    /** flo_div
     * 
     */
    @JRubyMethod(name = "/", required = 1)
    public IRubyObject op_fdiv(ThreadContext context, IRubyObject other) { // don't override Numeric#div !
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyFloat.newFloat(getRuntime(), value / ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "/", other);
        }
    }

    /** flo_quo
    *
    */
    @JRubyMethod(name = "quo", compat = CompatVersion.RUBY1_9)
        public IRubyObject magnitude(ThreadContext context, IRubyObject other) {
        return callMethod(context, "/", other);
    }

    /** flo_mod
     * 
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double y = ((RubyNumeric) other).getDoubleValue();
            // Modelled after c ruby implementation (java /,% not same as ruby)
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            if (y * mod < 0) {
                mod += y;
            }

            return RubyFloat.newFloat(getRuntime(), mod);
        default:
            return coerceBin(context, "%", other);
        }
    }

    /** flo_divmod
     * 
     */
    @JRubyMethod(name = "divmod", required = 1)
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double y = ((RubyNumeric) other).getDoubleValue();
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            // MRI behavior:
            if (Double.isNaN(mod)) {
                throw getRuntime().newFloatDomainError("NaN");
            }
            double div = Math.floor(x / y);

            if (y * mod < 0) {
                mod += y;
            }
            final Ruby runtime = getRuntime();
            IRubyObject car = dbl2num(runtime, div);
            RubyFloat cdr = RubyFloat.newFloat(runtime, mod);
            return RubyArray.newArray(runtime, car, cdr);
        default:
            return coerceBin(context, "divmod", other);
        }
    }
    	
    /** flo_pow
     * 
     */
    @JRubyMethod(name = "**", required = 1)
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyNumeric) other)
                    .getDoubleValue()));
        default:
            return coerceBin(context, "**", other);
        }
    }

    /** flo_eq
     * 
     */
    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyNumeric) other)
                    .getDoubleValue());
        default:
            // Numeric.equal            
            return super.op_num_equal(context, other);
        }
    }

    @Override
    public final int compareTo(IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            return Double.compare(value, ((RubyNumeric) other).getDoubleValue());
        default:
            return (int)coerceCmp(getRuntime().getCurrentContext(), "<=>", other).convertToInteger().getLongValue();
        }
    }

    /** flo_cmp
     * 
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
            if (Double.isInfinite(value)) {
                return value > 0.0 ? RubyFixnum.one(getRuntime()) : RubyFixnum.minus_one(getRuntime());
            }
        case ClassIndex.FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return dbl_cmp(getRuntime(), value, b);
        default:
            return coerceCmp(context, "<=>", other);
        }
    }

    /** flo_gt
     * 
     */
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value > b);
        default:
            return coerceRelOp(context, ">", other);
        }
    }

    /** flo_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value >= b);
        default:
            return coerceRelOp(context, ">=", other);
        }
    }

    /** flo_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value < b);
        default:
            return coerceRelOp(context, "<", other);
		}
    }

    /** flo_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().index) {
        case ClassIndex.FIXNUM:
        case ClassIndex.BIGNUM:
        case ClassIndex.FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value <= b);
        default:
            return coerceRelOp(context, "<=", other);
		}
	}
	
    /** flo_eql
     * 
     */
    @JRubyMethod(name = "eql?", required = 1)
    @Override
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
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(hashCode());
    }

    @Override
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
    @Override
    public IRubyObject abs(ThreadContext context) {
        if (Double.doubleToLongBits(value) < 0) {
            return RubyFloat.newFloat(context.getRuntime(), Math.abs(value));
        }
        return this;
    }

    /** flo_abs/1.9
     * 
     */
    @JRubyMethod(name = "magnitude", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
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
    @JRubyMethod(name = {"truncate", "to_i", "to_int"})
    @Override
    public IRubyObject truncate() {
        double f = value;
        if (f > 0.0) f = Math.floor(f);
        if (f < 0.0) f = Math.ceil(f);

        return dbl2num(getRuntime(), f);
    }

    /** flo_numerator
     * 
     */
    @JRubyMethod(name = "numerator", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject numerator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return this;
        return super.numerator(context);
    }

    /** flo_denominator
     * 
     */
    @JRubyMethod(name = "denominator", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject denominator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return RubyFixnum.one(context.getRuntime());
        return super.denominator(context);
    }

    /** float_to_r, float_decode
     * 
     */
    static final int DBL_MANT_DIG = 53;
    static final int FLT_RADIX = 2;
    @JRubyMethod(name = "to_r", compat = CompatVersion.RUBY1_9)
    public IRubyObject to_r(ThreadContext context) {
        long[]exp = new long[1]; 
        double f = frexp(value, exp);
        f = ldexp(f, DBL_MANT_DIG);
        long n = exp[0] - DBL_MANT_DIG;

        Ruby runtime = context.getRuntime();

        IRubyObject rf = RubyNumeric.dbl2num(runtime, f);
        IRubyObject rn = RubyFixnum.newFixnum(runtime, n);
        return f_mul(context, rf, f_expt(context, RubyFixnum.newFixnum(runtime, FLT_RADIX), rn));
    }

    /** floor
     * 
     */
    @JRubyMethod(name = "floor")
    @Override
    public IRubyObject floor() {
        return dbl2num(getRuntime(), Math.floor(value));
    }

    /** flo_ceil
     * 
     */
    @JRubyMethod(name = "ceil")
    @Override
    public IRubyObject ceil() {
        return dbl2num(getRuntime(), Math.ceil(value));
    }

    /** flo_round
     * 
     */
    @JRubyMethod(name = "round")
    @Override
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

    // CRuby uses sprintf(buf, "%.*g", FLOAT_DIG, d);
    // This pattern adjusts the output of String.pattern("%g") to mimic
    // the C version.
    private static final Pattern pattern = Pattern.compile("\\.?0+(e|$)");

    private static String formatDouble(double x) {
        return pattern.matcher(String.format("%.32g", x)).replaceFirst("$1");
    }

    private String marshalDump() {
        if (Double.isInfinite(value)) return value < 0 ? "-inf" : "inf";
        if (Double.isNaN(value)) return "nan";

        return formatDouble(value);
    }

    public static void marshalTo(RubyFloat aFloat, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(aFloat);
        output.writeString(aFloat.marshalDump());
    }
        
    public static RubyFloat unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyFloat result = RubyFloat.newFloat(input.getRuntime(), org.jruby.util.Convert.byteListToDouble(input.unmarshalString(),false));
        input.registerLinkTarget(result);
        return result;
    }
}
