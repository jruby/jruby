/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
import java.math.RoundingMode;
import java.util.Locale;

import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites.FloatSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertDouble;
import org.jruby.util.Sprintf;

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

/**
  * A representation of a float object
 */
@JRubyClass(name="Float", parent="Numeric")
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

    public int signum() {
        return (int) Math.signum(value); // NOTE: (int) NaN ?
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
        throw recv.getRuntime().newTypeError("failed to convert " + number.getMetaClass() + " into Float");
    }

    /** flo_to_s
     *
     */
    @JRubyMethod(name = "to_s")
    @Override
    public IRubyObject to_s() {
        final Ruby runtime = getRuntime();
        if (Double.isInfinite(value)) {
            return RubyString.newString(runtime, value < 0 ? "-Infinity" : "Infinity");
        }
        if (Double.isNaN(value)) {
            return RubyString.newString(runtime, "NaN");
        }

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
        final Ruby runtime = getRuntime();
        return runtime.newArray(RubyKernel.new_float(runtime, other), this);
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
        case INTEGER:
        case FLOAT:
            return RubyFloat.newFloat(context.runtime, value + ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, sites(context).op_plus, other);
        }
    }

    public IRubyObject op_plus(ThreadContext context, double other) {
        return RubyFloat.newFloat(context.runtime, value + other);
    }

    /** flo_minus
     *
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            return RubyFloat.newFloat(context.runtime, value - ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, sites(context).op_minus, other);
        }
    }

    public IRubyObject op_minus(ThreadContext context, double other) {
        return RubyFloat.newFloat(context.runtime, value - other);
    }

    /** flo_mul
     *
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            return RubyFloat.newFloat(context.runtime, value * ((RubyNumeric) other).getDoubleValue());
        default:
            return coerceBin(context, sites(context).op_times, other);
        }
    }

    public IRubyObject op_mul(ThreadContext context, double other) {
        return RubyFloat.newFloat(context.runtime, value * other);
    }

    /**
     * MRI: flo_div
     */
    @JRubyMethod(name = "/", required = 1)
    public IRubyObject op_div(ThreadContext context, IRubyObject other) { // don't override Numeric#div !
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            try {
                return RubyFloat.newFloat(context.runtime, value / ((RubyNumeric) other).getDoubleValue());
            } catch (NumberFormatException nfe) {
                throw context.runtime.newFloatDomainError(other.toString());
            }
        default:
            return coerceBin(context, sites(context).op_quo, other);
        }
    }

    public IRubyObject op_div(ThreadContext context, double other) { // don't override Numeric#div !
        return RubyFloat.newFloat(context.runtime, value / other);
    }

    /** flo_quo
    *
    */
    @JRubyMethod(name = "quo")
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        return numFuncall(context, this, sites(context).op_quo, other);
    }

    /** flo_mod
     *
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double y = ((RubyNumeric) other).getDoubleValue();
            if (y == 0) throw context.runtime.newZeroDivisionError();
            return op_mod(context, y);
        default:
            return coerceBin(context, sites(context).op_mod, other);
        }
    }

    public IRubyObject op_mod(ThreadContext context, double other) {
        // Modelled after c ruby implementation (java /,% not same as ruby)
        double x = value;

        double mod = Math.IEEEremainder(x, other);
        if (other * mod < 0) {
            mod += other;
        }

        return RubyFloat.newFloat(context.runtime, mod);
    }

    @Deprecated
    public final IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        return op_mod(context, other);
    }

    /** flo_divmod
     *
     */
    @Override
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double y = ((RubyNumeric) other).getDoubleValue();
            if (y == 0) throw context.runtime.newZeroDivisionError();
            double x = value;

            double mod = Math.IEEEremainder(x, y);
            // MRI behavior:
            if (Double.isNaN(mod)) {
                throw context.runtime.newFloatDomainError("NaN");
            }
            double div = Math.floor(x / y);

            if (y * mod < 0) {
                mod += y;
            }
            final Ruby runtime = context.runtime;
            IRubyObject car = dbl2ival(runtime, div);
            RubyFloat cdr = RubyFloat.newFloat(runtime, mod);
            return RubyArray.newArray(runtime, car, cdr);
        default:
            return coerceBin(context, sites(context).divmod, other);
        }
    }

    /** flo_pow
     *
     */
    @JRubyMethod(name = "**", required = 1)
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
            case INTEGER:
            case FLOAT:
                double d_other = ((RubyNumeric) other).getDoubleValue();
                if (value < 0 && (d_other != Math.round(d_other))) {
                    RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                    return sites(context).op_exp.call(context, complex, complex, other);
                }
                return RubyFloat.newFloat(context.runtime, Math.pow(value, d_other));
            default:
                return coerceBin(context, sites(context).op_exp, other);
        }
    }

    public IRubyObject op_pow(ThreadContext context, double other) {
        return RubyFloat.newFloat(context.runtime, Math.pow(value, other));
    }

    @Deprecated
    public IRubyObject op_pow19(ThreadContext context, IRubyObject other) {
        return op_pow(context, other);
    }

    /** flo_eq
     *
     */
    @JRubyMethod(name = "==", required = 1)
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (Double.isNaN(value)) {
            return context.runtime.getFalse();
        }
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            return RubyBoolean.newBoolean(context.runtime, value == ((RubyNumeric) other).getDoubleValue());
        default:
            // Numeric.equal
            return super.op_num_equal(context, other);
        }
    }

    public IRubyObject op_equal(ThreadContext context, double other) {
        if (Double.isNaN(value)) {
            return context.runtime.getFalse();
        }
        return RubyBoolean.newBoolean(context.runtime, value == other);
    }

    public boolean fastEqual(RubyFloat other) {
        if (Double.isNaN(value)) {
            return false;
        }
        return value == other.value;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
            case INTEGER:
        case FLOAT:
            return Double.compare(value, ((RubyNumeric) other).getDoubleValue());
        default:
            ThreadContext context = getRuntime().getCurrentContext();
            return (int) coerceCmp(context, sites(context).op_cmp, other).convertToInteger().getLongValue();
        }
    }

    /** flo_cmp
     *
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        final Ruby runtime = context.runtime;
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
            if (Double.isInfinite(value)) {
                return value > 0.0 ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
            }
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return dbl_cmp(runtime, value, b);
        default:
            FloatSites sites = sites(context);
            if (Double.isInfinite(value) && sites.respond_to_infinite.respondsTo(context, other, other, true)) {
                IRubyObject infinite = sites.infinite.call(context, other, other);
                if (infinite.isNil()) {
                    return value > 0.0 ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
                }
                long sign = RubyFixnum.fix2long(infinite);

                if (sign > 0) {
                    return value > 0.0 ? RubyFixnum.zero(runtime) : RubyFixnum.minus_one(runtime);
                } else {
                    return value < 0.0 ? RubyFixnum.zero(runtime) : RubyFixnum.one(runtime);
                }
            }
            return coerceCmp(context, sites.op_cmp, other);
        }
    }

    public IRubyObject op_cmp(ThreadContext context, double other) {
        return dbl_cmp(context.runtime, value, other);
    }

    /** flo_gt
     *
     */
    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(b) && value > b);
        default:
            return coerceRelOp(context, sites(context).op_gt, other);
        }
    }

    public IRubyObject op_gt(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(other) && value > other);
    }

    /** flo_ge
     *
     */
    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(b) && value >= b);
        default:
            return coerceRelOp(context, sites(context).op_ge, other);
        }
    }

    public IRubyObject op_ge(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(other) && value >= other);
    }

    /** flo_lt
     *
     */
    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(b) && value < b);
        default:
            return coerceRelOp(context, sites(context).op_lt, other);
		}
    }

    public IRubyObject op_lt(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(other) && value < other);
    }

    /** flo_le
     *
     */
    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        switch (other.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
            double b = ((RubyNumeric) other).getDoubleValue();
            return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(b) && value <= b);
        default:
            return coerceRelOp(context, sites(context).op_le, other);
		}
	}

    public IRubyObject op_le(ThreadContext context, double other) {
        return RubyBoolean.newBoolean(context.runtime, !Double.isNaN(other) && value <= other);
	}

    /** flo_eql
     *
     */
    @JRubyMethod(name = "eql?", required = 1)
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        return getRuntime().newBoolean( equals(other) );
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof RubyFloat) && equals((RubyFloat) other);
    }

    private boolean equals(RubyFloat that) {
        if ( Double.isNaN(this.value) || Double.isNaN(that.value) ) return false;
        final double val1 = this.value == -0.0 ? 0.0 : this.value;
        final double val2 = that.value == -0.0 ? 0.0 : that.value;
        return Double.doubleToLongBits(val1) == Double.doubleToLongBits(val2);
    }

    /** flo_hash
     *
     */
    @JRubyMethod(name = "hash")
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum( hashCode() );
    }

    @Override
    public final int hashCode() {
        final double val = value == 0.0 ? -0.0 : value;
        final long l = Double.doubleToLongBits(val);
        return (int) ( l ^ l >>> 32 );
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

    /**
     * MRI: flo_zero_p
     */
    @JRubyMethod(name = "zero?")
    @Override
    public IRubyObject zero_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, value == 0.0);
    }

    /**
     * MRI: flo_truncate
     */
    @JRubyMethod(name = {"truncate", "to_i", "to_int"})
    @Override
    public IRubyObject truncate(ThreadContext context) {
        if (value > 0.0) return floor(context);
        return ceil(context);
    }

    /**
     * MRI: flo_truncate
     */
    @JRubyMethod(name = {"truncate", "to_i", "to_int"})
    public IRubyObject truncate(ThreadContext context, IRubyObject n) {
        if (value > 0.0) return floor(context, n);
        return ceil(context, n);
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
        long[] exp = new long[1];
        double f = frexp(value, exp);
        f = ldexp(f, DBL_MANT_DIG);
        long n = exp[0] - DBL_MANT_DIG;

        Ruby runtime = context.runtime;

        IRubyObject rf = RubyNumeric.dbl2ival(runtime, f);
        IRubyObject rn = RubyFixnum.newFixnum(runtime, n);
        return f_mul(context, rf, f_expt(context, RubyFixnum.newFixnum(runtime, FLT_RADIX), rn));
    }

    /** float_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        if (f_negative_p(context, this)) {
            return f_negate(context, ((RubyFloat) f_abs(context, this)).rationalize(context, args));
        }

        final Ruby runtime = context.runtime;
        RubyFixnum one = RubyFixnum.one(runtime);

        IRubyObject eps, a, b;
        if (args.length != 0) {
            eps = f_abs(context, args[0]);
            a = f_sub(context, this, eps);
            b = f_add(context, this, eps);
        } else {
            IRubyObject flt;
            IRubyObject p, q;
            long[] exp = new long[1];

            // float_decode_internal
            double f = frexp(value, exp);
            f = ldexp(f, DBL_MANT_DIG);
            long n = exp[0] - DBL_MANT_DIG;

            RubyInteger rf = RubyBignum.newBignorm(runtime, f);
            RubyFixnum rn = RubyFixnum.newFixnum(runtime, n);

            if (rf.zero_p(context).isTrue() || fix2int(rn) >= 0) {
                return RubyRational.newRationalRaw(runtime, rf.op_lshift(context, rn));
            }

            RubyInteger two_times_f, den;

            RubyFixnum two = RubyFixnum.two(runtime);
            two_times_f = (RubyInteger) two.op_mul(context, rf);
            den = (RubyInteger) RubyFixnum.one(runtime).op_lshift(context, RubyFixnum.one(runtime).op_minus(context, n));

            a = RubyRational.newRationalRaw(runtime, two_times_f.op_minus(context, RubyFixnum.one(runtime)), den);
            b = RubyRational.newRationalRaw(runtime, two_times_f.op_plus(context, RubyFixnum.one(runtime)), den);
        }

        if (sites(context).op_equal.call(context, a, a, b).isTrue()) return f_to_r(context, this);

        IRubyObject[] ary = new IRubyObject[2];
        ary[0] = a;
        ary[1] = b;
        IRubyObject[] ans = nurat_rationalize_internal(context, ary);

        return RubyRational.newRationalRaw(runtime, ans[0], ans[1]);
    }

    /**
     * MRI: flo_floor
     */
    @Override
    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context) {
        return dbl2ival(context.runtime, Math.floor(value));
    }

    /**
     * MRI: flo_floor
     */
    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context, IRubyObject digits) {
        double number, f;
        int ndigits =  num2int(digits);

        if (ndigits < 0) {
            return ((RubyInteger) truncate(context)).floor(context, digits);
        }

        number = value;

        if (ndigits > 0) {
            RubyNumeric[] num = {this};
            Ruby runtime = context.runtime;
            if (floatInvariantRound(runtime, number, ndigits, num)) return num[0];
            f = Math.pow(10, ndigits);
            f = Math.floor(number * f) / f;
            return dbl2num(runtime, f);
        }

        return floor(context);
    }

     // MRI: float_invariant_round
     private static boolean floatInvariantRound(Ruby runtime, double number, int ndigits, RubyNumeric[] num)    {
        int float_dig = DIG+2;
        long[] binexp = {0L};

        frexp(number, binexp);

        /* Let `exp` be such that `number` is written as:"0.#{digits}e#{exp}",
           i.e. such that  10 ** (exp - 1) <= |number| < 10 ** exp
           Recall that up to float_dig digits can be needed to represent a double,
           so if ndigits + exp >= float_dig, the intermediate value (number * 10 ** ndigits)
           will be an integer and thus the result is the original number.
           If ndigits + exp <= 0, the result is 0 or "1e#{exp}", so
           if ndigits + exp < 0, the result is 0.
           We have:
                2 ** (binexp-1) <= |number| < 2 ** binexp
                10 ** ((binexp-1)/log_2(10)) <= |number| < 10 ** (binexp/log_2(10))
                If binexp >= 0, and since log_2(10) = 3.322259:
                   10 ** (binexp/4 - 1) < |number| < 10 ** (binexp/3)
                   floor(binexp/4) <= exp <= ceil(binexp/3)
                If binexp <= 0, swap the /4 and the /3
                So if ndigits + floor(binexp/(4 or 3)) >= float_dig, the result is number
                If ndigits + ceil(binexp/(3 or 4)) < 0 the result is 0
        */

        if (Double.isInfinite(number) || Double.isNaN(number) ||
                (ndigits >= float_dig - (binexp[0] > 0 ? binexp[0] / 4 : binexp[0] / 3 - 1))) {
            return true;
        }

        if (ndigits < - (binexp[0] > 0 ? binexp[0] / 3 + 1 : binexp[0] / 4)) {
            num[0] = RubyFixnum.zero(runtime);
            return true;
        }

        return false;
    }

    /**
     * MRI: flo_ceil
     */
    @JRubyMethod(name = "ceil")
    @Override
    public IRubyObject ceil(ThreadContext context) {
        return dbl2ival(context.runtime, Math.ceil(value));
    }

    /**
     * MRI: flo_ceil
     */
    @JRubyMethod(name = "ceil")
    public IRubyObject ceil(ThreadContext context, IRubyObject digits) {
        Ruby runtime = context.runtime;
        double number, f;

        int ndigits = num2int(digits);

        number = value;

        if (ndigits < 0) {
            return ((RubyInteger) dbl2ival(runtime, Math.ceil(number))).ceil(context, digits);
        }

        if (ndigits == 0) {
            return dbl2ival(runtime, Math.ceil(number));
        }

        RubyNumeric[] num = {this};
        if (floatInvariantRound(runtime, number, ndigits, num)) return num[0];

        f = Math.pow(10, ndigits);

        return RubyFloat.newFloat(runtime, Math.ceil(number * f) / f);
    }

    /**
     * MRI: flo_round
     */
    @Override
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context) {
        return roundShared(context, RoundingMode.HALF_UP, 0);
    }

    /**
     * MRI: flo_round
     */
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject arg0) {
        Ruby runtime = context.runtime;
        int digits = 0;

        // options (only "half" right now)
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, arg0);
        if (opts.isNil()) {
            digits = num2int(arg0);
        }

        RoundingMode roundingMode = getRoundingMode(context, opts);

        return roundShared(context, roundingMode, digits);
    }

    /**
     * MRI: flo_round
     */
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject _digits, IRubyObject _opts) {
        Ruby runtime = context.runtime;
        int digits = 0;

        // options (only "half" right now)
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, _opts);
        digits = num2int(_digits);

        RoundingMode roundingMode = getRoundingMode(context, opts);

        return roundShared(context, roundingMode, digits);
    }

    private IRubyObject roundShared(ThreadContext context, RoundingMode roundingMode, int ndigits) {
        double number, f, x;

        if (Double.isInfinite(value)) {
            if (ndigits <= 0) throw context.runtime.newFloatDomainError(value < 0 ? "-Infinity" : "Infinity");
            return this;
        }

        if (Double.isNaN(value)) {
            if (ndigits <= 0) throw context.runtime.newFloatDomainError("NaN");
            return this;
        }

        if (ndigits < 0) {
            return ((RubyInteger) truncate(context)).round(context, ndigits);
        }
        number = value;
        if (ndigits == 0) {
            x = doRound(context, roundingMode, number, 1.0);
            return dbl2ival(context.runtime, x);
        }
        RubyNumeric[] num = {this};
        if (floatInvariantRound(context.runtime, number, ndigits, num)) return num[0];
        f = Math.pow(10, ndigits);
        x = doRound(context, roundingMode, number, f);
        return dbl2num(context.runtime, x / f);
    }

    private static double doRound(ThreadContext context, RoundingMode roundingMode, double number, double scale) {
        switch (roundingMode) {
            case HALF_UP:
                return roundHalfUp(number, scale);
            case HALF_DOWN:
                return roundHalfDown(number, scale);
            case HALF_EVEN:
                return roundHalfEven(number, scale);
        }
        throw context.runtime.newArgumentError("invalid rounding mode: " + roundingMode);
    }

    private static double roundHalfUp(double x, double s) {
        double f, xs = x * s;

        int signum = x >= 0.0 ? 1 : -1;
        xs = xs * signum;
        f = roundHalfUp(xs);
        f = f * signum;
        if (s == 1.0) return f;
        if (x > 0) {
            if ((f + 0.5) / s <= x) f += 1;
            x = f;
        }
        else {
            if ((f - 0.5) / s >= x) f -= 1;
            x = f;
        }
        return x;
    }

    private static double roundHalfDown(double x, double s) {
        double f, xs = x * s;

        int signum = x >= 0.0 ? 1 : -1;
        xs = xs * signum;
        f = roundHalfUp(xs);;
        f = f * signum;
        if (x > 0) {
            if ((f - 0.5) / s >= x) f -= 1;
            x = f;
        }
        else {
            if ((f + 0.5) / s <= x) f += 1;
            x = f;
        }
        return x;
    }

    private static double roundHalfUp(double n) {
        double f = n;
        if (f >= 0.0) {
            f = Math.floor(f);

            if (n - f >= 0.5) {
                f += 1.0;
            }
        } else {
            f = Math.ceil(f);

            if (f - n >= 0.5) {
                f -= 1.0;
            }
        }
        return f;
    }

    private static double roundHalfEven(double x, double s) {
        double f, d, xs = x * s;

        if (x > 0.0) {
            f = Math.floor(xs);
            d = xs - f;
            if (d > 0.5)
                d = 1.0;
            else if (d == 0.5 || ((double)((f + 0.5) / s) <= x))
                d = f % 2.0;
            else
                d = 0.0;
            x = f + d;
        }
        else if (x < 0.0) {
            f = Math.ceil(xs);
            d = f - xs;
            if (d > 0.5)
                d = 1.0;
            else if (d == 0.5 || ((double)((f - 0.5) / s) >= x))
                d = -f % 2.0;
            else
                d = 0.0;
            x = f - d;
        }
        return x;
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

    @Deprecated
    public IRubyObject zero_p() {
        return zero_p(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject floor(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return floor(context);
            case 1:
                return floor(context, args[0]);
            default:
                throw context.runtime.newArgumentError("floor", args.length, 1);
        }
    }

    @Deprecated
    public IRubyObject round(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return round(context);
            case 1:
                return round(context, args[0]);
            case 2:
                return round(context, args[0], args[1]);
            default:
                throw context.runtime.newArgumentError("round", args.length, 2);
        }
    }

    private static FloatSites sites(ThreadContext context) {
        return context.sites.Float;
    }
}
