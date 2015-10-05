/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.math.BigDecimal;
import java.math.BigInteger;
import static org.jruby.util.Numeric.f_abs;
import static org.jruby.util.Numeric.f_add;
import static org.jruby.util.Numeric.f_expt;
import static org.jruby.util.Numeric.f_lshift;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negate;
import static org.jruby.util.Numeric.f_negative_p;
import static org.jruby.util.Numeric.f_sub;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.Numeric.f_zero_p;
import static org.jruby.util.Numeric.frexp;
import static org.jruby.util.Numeric.ldexp;
import static org.jruby.util.Numeric.nurat_rationalize_internal;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.lang.Math;

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertDouble;
import org.jruby.util.Sprintf;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;

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
    public static final double INFINITY = Double.POSITIVE_INFINITY;
    public static final double NAN = Double.NaN;

    public static RubyClass createFloatClass(Ruby runtime) {
        RubyClass floatc = runtime.defineClass("Float", runtime.getNumeric(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setFloat(floatc);

        floatc.setClassIndex(ClassIndex.FLOAT);
        floatc.setReifiedClass(RubyFloat.class);
        
        floatc.kindOf = new RubyModule.JavaClassKindOf(RubyFloat.class);

        floatc.getSingletonClass().undefineMethod("new");

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

        floatc.defineConstant("INFINITY", RubyFloat.newFloat(runtime, INFINITY));
        floatc.defineConstant("NAN", RubyFloat.newFloat(runtime, NAN));

        floatc.defineAnnotatedMethods(RubyFloat.class);

        return floatc;
    }

    private final double value;
    
    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.FLOAT;
    }

    public RubyFloat(Ruby runtime) {
        this(runtime, 0.0);
    }

    public RubyFloat(Ruby runtime, double value) {
        super(runtime.getFloat());
        this.value = value;
        this.flags |= FROZEN_F;
    }

    @Override
    public RubyClass getSingletonClass() {
        throw getRuntime().newTypeError("can't define singleton");
    }

    @Override
    public Class<?> getJavaClass() {
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
    public int getIntValue() {
        return (int) value;
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
    @Deprecated
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
        // Under 1.9, use full-precision float formatting (JRUBY-4846).
        // Double-precision can represent around 16 decimal digits;
        // we use 20 to ensure full representation.
        Sprintf.sprintf(buf, Locale.US, "%#.20g", this);
        int e = buf.indexOf('e');
        if (e == -1) e = buf.getRealSize();
        ASCIIEncoding ascii = ASCIIEncoding.INSTANCE; 

        if (!ascii.isDigit(buf.get(e - 1))) {
            buf.setRealSize(0);
            Sprintf.sprintf(buf, Locale.US, "%#.14e", this);
            e = buf.indexOf('e');
            if (e == -1) e = buf.getRealSize();
        }

        int p = e;
        while (buf.get(p - 1) == '0' && ascii.isDigit(buf.get(p - 2))) p--;
        System.arraycopy(buf.getUnsafeBytes(), e, buf.getUnsafeBytes(), p, buf.getRealSize() - e);
        buf.setRealSize(p + buf.getRealSize() - e);

        buf.setEncoding(USASCIIEncoding.INSTANCE);

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
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyFloat.newFloat(getRuntime(), value + ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "+", other);
        }
    }

    public IRubyObject op_plus(ThreadContext context, double other) {
        return RubyFloat.newFloat(getRuntime(), value + other);
    }

    /** flo_minus
     * 
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyFloat.newFloat(getRuntime(), value - ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "-", other);
        }
    }

    public IRubyObject op_minus(ThreadContext context, double other) {
        return RubyFloat.newFloat(getRuntime(), value - other);
    }

    /** flo_mul
     * 
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyFloat.newFloat(
                    getRuntime(), value * ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "*", other);
        }
    }

    public IRubyObject op_mul(ThreadContext context, double other) {
        return RubyFloat.newFloat(
                getRuntime(), value * other);
    }
    
    /** flo_div
     * 
     */
    @JRubyMethod(name = "/", required = 1)
    public IRubyObject op_fdiv(ThreadContext context, IRubyObject other) { // don't override Numeric#div !
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyFloat.newFloat(getRuntime(), value / ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, "/", other);
        }
    }

    public IRubyObject op_fdiv(ThreadContext context, double other) { // don't override Numeric#div !
        return RubyFloat.newFloat(getRuntime(), value / other);
    }

    /** flo_quo
    *
    */
    @JRubyMethod(name = "quo")
        public IRubyObject magnitude(ThreadContext context, IRubyObject other) {
        return callMethod(context, "/", other);
    }

    /** flo_mod
     * 
     */
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            double y = ((RubyNumeric) other).getDoubleValue();
            return op_mod(context, y);
        default:
            return coerceBin(context, "%", other);
        }
    }

    public IRubyObject op_mod(ThreadContext context, double other) {
        // Modelled after c ruby implementation (java /,% not same as ruby)
        double x = value;

        double mod = Math.IEEEremainder(x, other);
        if (other * mod < 0) {
            mod += other;
        }

        return RubyFloat.newFloat(getRuntime(), mod);
    }

    /** flo_mod
     * 
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyNumeric
            && ((RubyNumeric)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return op_mod(context, other);
    }

    /** flo_divmod
     * 
     */
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
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
    	
    /** flo_divmod
     * 
     */
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyNumeric
            && ((RubyNumeric)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return divmod(context, other);
    }
    	
    /** flo_pow
     * 
     */
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyFloat.newFloat(getRuntime(), Math.pow(value, ((RubyNumeric) other)
                    .getDoubleValue()));
        default:
            return coerceBin(context, "**", other);
        }
    }

    public IRubyObject op_pow(ThreadContext context, double other) {
        return RubyFloat.newFloat(getRuntime(), Math.pow(value, other));
    }
    
    @JRubyMethod(name = "**", required = 1)
    public IRubyObject op_pow19(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
            case FIXNUM:
            case BIGNUM:
            case FLOAT:
                double d_other = ((RubyNumeric) other).getDoubleValue();
                if (value < 0 && (d_other != Math.round(d_other))) {
                    return RubyComplex.newComplexRaw(getRuntime(), this).callMethod(context, "**", other);
                } else {
                    return op_pow(context, other);
                }
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
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            return RubyBoolean.newBoolean(getRuntime(), value == ((RubyNumeric) other)
                    .getDoubleValue());
        default:
            // Numeric.equal            
            return super.op_num_equal(context, other);
        }
    }

    public IRubyObject op_equal(ThreadContext context, double other) {
        if (Double.isNaN(value)) {
            return getRuntime().getFalse();
        }
        return RubyBoolean.newBoolean(getRuntime(), value == other);
    }

    public boolean fastEqual(RubyFloat other) {
        if (Double.isNaN(value)) {
            return false;
        }
        return value == ((RubyFloat)other).value;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
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
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
            if (Double.isInfinite(value)) {
                return value > 0.0 ? RubyFixnum.one(getRuntime()) : RubyFixnum.minus_one(getRuntime());
            }
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return dbl_cmp(getRuntime(), value, b);
        default:
            if (Double.isInfinite(value) && other.respondsTo("infinite?")) {
                IRubyObject infinite = other.callMethod(context, "infinite?");
                if (infinite.isNil()) {
                    return value > 0.0 ? RubyFixnum.one(getRuntime()) : RubyFixnum.minus_one(getRuntime());
                } else {
                    int sign = RubyFixnum.fix2int(infinite);

                    if (sign > 0) {
                        if (value > 0.0) {
                            return RubyFixnum.zero(getRuntime());
                        } else {
                            return RubyFixnum.minus_one(getRuntime());
                        }
                    } else {
                        if (value < 0.0) {
                            return RubyFixnum.zero(getRuntime());
                        } else {
                            return RubyFixnum.one(getRuntime());
                        }
                    }
                }
            }
            return coerceCmp(context, "<=>", other);
        }
    }

    public IRubyObject op_cmp(ThreadContext context, double other) {
        return dbl_cmp(getRuntime(), value, other);
    }

    /** flo_gt
     * 
     */
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value > b);
        default:
            return coerceRelOp(context, ">", other);
        }
    }

    public IRubyObject op_gt(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(other) && value > other);
    }

    /** flo_ge
     * 
     */
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value >= b);
        default:
            return coerceRelOp(context, ">=", other);
        }
    }

    public IRubyObject op_ge(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(other) && value >= other);
    }

    /** flo_lt
     * 
     */
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value < b);
        default:
            return coerceRelOp(context, "<", other);
		}
    }

    public IRubyObject op_lt(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(other) && value < other);
    }

    /** flo_le
     * 
     */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(b) && value <= b);
        default:
            return coerceRelOp(context, "<=", other);
		}
	}

    public IRubyObject op_le(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(getRuntime(), !Double.isNaN(other) && value <= other);
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
            return RubyFloat.newFloat(context.runtime, Math.abs(value));
        }
        return this;
    }

    /** flo_abs/1.9
     * 
     */
    @JRubyMethod(name = "magnitude")
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
    @JRubyMethod(name = "numerator")
    @Override
    public IRubyObject numerator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) return this;
        return super.numerator(context);
    }

    /** flo_denominator
     * 
     */
    @JRubyMethod(name = "denominator")
    @Override
    public IRubyObject denominator(ThreadContext context) {
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            return RubyFixnum.one(context.runtime);
        }
        return super.denominator(context);
    }

    /** float_to_r, float_decode
     * 
     */
    static final int DBL_MANT_DIG = 53;
    static final int FLT_RADIX = 2;
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        long[]exp = new long[1]; 
        double f = frexp(value, exp);
        f = ldexp(f, DBL_MANT_DIG);
        long n = exp[0] - DBL_MANT_DIG;

        Ruby runtime = context.runtime;

        IRubyObject rf = RubyNumeric.dbl2num(runtime, f);
        IRubyObject rn = RubyFixnum.newFixnum(runtime, n);
        return f_mul(context, rf, f_expt(context, RubyFixnum.newFixnum(runtime, FLT_RADIX), rn));
    }

    /** float_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        if (f_negative_p(context, this))
            return f_negate(context, ((RubyFloat) f_abs(context, this)).rationalize(context, args));

        Ruby runtime = context.runtime;
        RubyFixnum one = RubyFixnum.one(runtime);
        RubyFixnum two = RubyFixnum.two(runtime);

        IRubyObject eps, a, b;
        if (args.length != 0) {
            eps = f_abs(context, args[0]);
            a = f_sub(context, this, eps);
            b = f_add(context, this, eps);
        } else {
            long[] exp = new long[1];
            double f = frexp(value, exp);
            f = ldexp(f, DBL_MANT_DIG);
            long n = exp[0] - DBL_MANT_DIG;


            IRubyObject rf = RubyNumeric.dbl2num(runtime, f);
            IRubyObject rn = RubyFixnum.newFixnum(runtime, n);

            if (f_zero_p(context, rf) || !(f_negative_p(context, rn) || f_zero_p(context, rn)))
                return RubyRational.newRationalRaw(runtime, f_lshift(context,rf,rn));

            a = RubyRational.newRationalRaw(runtime,
                    f_sub(context,f_mul(context, two, rf),one),
                    f_lshift(context, one, f_sub(context,one,rn)));
            b = RubyRational.newRationalRaw(runtime,
                    f_add(context,f_mul(context, two, rf),one),
                    f_lshift(context, one, f_sub(context,one,rn)));
        }

        if (invokedynamic(context, a, OP_EQUAL, b).isTrue()) return f_to_r(context, this);

        IRubyObject[] ary = new IRubyObject[2];
        ary[0] = a;
        ary[1] = b;
        IRubyObject[] ans = nurat_rationalize_internal(context, ary);

        return RubyRational.newRationalRaw(runtime, ans[0], ans[1]);

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
    @Override
    public IRubyObject round() {
        return dbl2num(getRuntime(), val2dbl());
    }
    
    @JRubyMethod(name = "round", optional = 1)
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        if (args.length == 0) return round();
        // truncate floats.
        double digits = num2long(args[0]);
        
        double magnifier = Math.pow(10.0, Math.abs(digits));
        double number = value;
        
        if (Double.isInfinite(value)) {
            if (digits <= 0) throw getRuntime().newFloatDomainError(value < 0 ? "-Infinity" : "Infinity");
            return this;
        }
        
        if (Double.isNaN(value)) {
            if (digits <= 0) throw getRuntime().newFloatDomainError("NaN");
            return this;
        }

        double binexp;
        // Missing binexp values for NaN and (-|+)Infinity.  frexp man page just says unspecified.
        if (value == 0) {
            binexp = 0;
        } else {
            binexp = Math.ceil(Math.log(value)/Math.log(2));
        }
        
        // MRI flo_round logic to deal with huge precision numbers.
        if (digits >= (DIG+2) - (binexp > 0 ? binexp / 4 : binexp / 3 - 1)) {
            return RubyFloat.newFloat(context.runtime, number);
        }
        if (digits < -(binexp > 0 ? binexp / 3 + 1 : binexp / 4)) {
            return dbl2num(context.runtime, (long) 0);
        }
        
        if (Double.isInfinite(magnifier)) {
            if (digits < 0) number = 0;
        } else {
            if (digits < 0) {
                number /= magnifier;
            } else {
                number *= magnifier;
            }

            number = Math.round(Math.abs(number))*Math.signum(number);
            if (digits < 0) {
                number *= magnifier;
            } else {
                number /= magnifier;
            }
        }
        
        if (digits > 0) {
            return RubyFloat.newFloat(context.runtime, number);
        } else {
            if (number > Long.MAX_VALUE || number < Long.MIN_VALUE) {
                // The only way to get huge precise values with BigDecimal is 
                // to convert the double to String first.
                BigDecimal roundedNumber = new BigDecimal(Double.toString(number));
                return RubyBignum.newBignum(context.runtime, roundedNumber.toBigInteger());
            }
            return dbl2num(context.runtime, (long)number);
        }
    }
    
    private double val2dbl() {
        double f = value;
        if (f > 0.0) {
            f = Math.floor(f);
            if (value - f >= 0.5) {
                f += 1.0;
            }
        } else if (f < 0.0) {
            f = Math.ceil(f);
            if (f - value >= 0.5) {
                f -= 1.0;
            }
        }
        
        return f;
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

    private ByteList marshalDump() {
        if (Double.isInfinite(value)) return value < 0 ? NEGATIVE_INFINITY_BYTELIST : INFINITY_BYTELIST;
        if (Double.isNaN(value)) return NAN_BYTELIST;

        ByteList byteList = new ByteList();
        // Always use US locale, to ensure "." separator. JRUBY-5918
        Sprintf.sprintf(byteList, Locale.US, "%.17g", RubyArray.newArray(getRuntime(), this));
        return byteList;
    }

    public static void marshalTo(RubyFloat aFloat, MarshalStream output) throws java.io.IOException {
        output.registerLinkTarget(aFloat);
        output.writeString(aFloat.marshalDump());
    }
        
    public static RubyFloat unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        ByteList value = input.unmarshalString();
        RubyFloat result;
        if (value.equals(NAN_BYTELIST)) {
            result = RubyFloat.newFloat(input.getRuntime(), RubyFloat.NAN);
        } else if (value.equals(NEGATIVE_INFINITY_BYTELIST)) {
            result = RubyFloat.newFloat(input.getRuntime(), Double.NEGATIVE_INFINITY);
        } else if (value.equals(INFINITY_BYTELIST)) {
            result = RubyFloat.newFloat(input.getRuntime(), Double.POSITIVE_INFINITY);
        } else {
            result = RubyFloat.newFloat(input.getRuntime(),
                    ConvertDouble.byteListToDouble19(value, false));
        }
        input.registerLinkTarget(result);
        return result;
    }

    private static final ByteList NAN_BYTELIST = new ByteList("nan".getBytes());
    private static final ByteList NEGATIVE_INFINITY_BYTELIST = new ByteList("-inf".getBytes());
    private static final ByteList INFINITY_BYTELIST = new ByteList("inf".getBytes());

    @JRubyMethod(name = "next_float")
    public IRubyObject next_float() {
        return RubyFloat.newFloat(getRuntime(), Math.nextAfter(value, Double.POSITIVE_INFINITY));
    }

    @JRubyMethod(name = "prev_float")
    public IRubyObject prev_float() {
        return RubyFloat.newFloat(getRuntime(), Math.nextAfter(value, Double.NEGATIVE_INFINITY));
    }
}
