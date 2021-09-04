/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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

package org.jruby.util;

import org.joni.Regex;
import org.joni.WarnCallback;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyComplex;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyInteger;
import org.jruby.RubyNumeric;
import org.jruby.RubyRational;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;

import java.math.BigInteger;

public class Numeric {
    public static final boolean CANON = true;

    /** f_add
     *
     */
    public static IRubyObject f_add(ThreadContext context, IRubyObject x, IRubyObject y) {
        CachingCallSite op_plus = sites(context).op_plus;

        if (op_plus.isBuiltin(x)) {
            if (x instanceof RubyInteger) {
                if (fixnumZero(x)) return y;
                if (fixnumZero(y)) return x;
                return ((RubyInteger) x).op_plus(context, y);
            } else if (x instanceof RubyFloat) {
                if (fixnumZero(y)) return x;
                return ((RubyFloat) x).op_plus(context, y);
            } else if (x instanceof RubyRational) {
                if (fixnumZero(y)) return x;
                return ((RubyRational) x).op_plus(context, y);
            }
        }

        return op_plus.call(context, x, x, y);
    }

    private static boolean fixnumZero(IRubyObject y) {
        return y instanceof RubyFixnum
                && ((RubyFixnum) y).getLongValue() == 0;
    }

    private static boolean fixnumOne(IRubyObject y) {
        return y instanceof RubyFixnum
                && ((RubyFixnum) y).getLongValue() == 1;
    }

    public static RubyInteger f_add(ThreadContext context, RubyInteger x, RubyInteger y) {
        return (RubyInteger) x.op_plus(context, y);
    }

    /** f_cmp
     *
     */
    public static IRubyObject f_cmp(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyInteger && y instanceof RubyInteger) {
            return f_cmp(context, (RubyInteger) x, (RubyInteger) y);
        }
        return sites(context).op_cmp.call(context, x, x, y);
    }

    public static RubyFixnum f_cmp(ThreadContext context, RubyInteger x, RubyInteger y) {
        final int cmp;
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            cmp = Long.compare(((RubyFixnum) x).getLongValue(), ((RubyFixnum) y).getLongValue());
        }
        else { // RubyBignum || RubyFixnum
            cmp = x.getBigIntegerValue().compareTo(y.getBigIntegerValue());
        }
        return RubyFixnum.newFixnum(context.runtime, cmp);
    }

    public static RubyFixnum f_cmp(ThreadContext context, RubyInteger x, long y) {
        final int cmp;
        if (x instanceof RubyFixnum) {
            cmp = Long.compare(((RubyFixnum) x).getLongValue(), y);
        }
        else { // RubyBignum
            cmp = x.getBigIntegerValue().compareTo(BigInteger.valueOf(y));
        }
        return RubyFixnum.newFixnum(context.runtime, cmp);
    }

    /** f_div
     *
     */
    public static IRubyObject f_div(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (y instanceof RubyFixnum && ((RubyFixnum) y).getLongValue() == 1) return x;
        return sites(context).op_quo.call(context, x, x, y);
    }

    /** f_gt_p
     *
     */
    public static boolean f_gt_p(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() > ((RubyFixnum)y).getLongValue();
        }
        return sites(context).op_gt.call(context, x, x, y).isTrue();
    }

    public static boolean f_gt_p(ThreadContext context, RubyInteger x, RubyInteger y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() > ((RubyFixnum)y).getLongValue();
        }
        return x.getBigIntegerValue().compareTo(y.getBigIntegerValue()) > 0;
    }

    /** f_lt_p
     *
     */
    public static boolean f_lt_p(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() < ((RubyFixnum)y).getLongValue();
        }
        return sites(context).op_lt.call(context, x, x, y).isTrue();
    }

    public static boolean f_lt_p(ThreadContext context, RubyInteger x, RubyInteger y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() < ((RubyFixnum)y).getLongValue();
        }
        return x.getBigIntegerValue().compareTo(y.getBigIntegerValue()) < 0;
    }

    /** f_mod
     *
     */
    public static IRubyObject f_mod(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).op_mod.call(context, x, x, y);
    }

    /** f_mul
     *
     */
    public static IRubyObject f_mul(ThreadContext context, IRubyObject x, IRubyObject y) {
        CachingCallSite op_times = sites(context).op_times;

        if (op_times.isBuiltin(x)) {
            if (x instanceof RubyInteger) {
                if (fixnumZero(y)) {
                    return y;
                }
                if (fixnumZero(x) && y instanceof RubyInteger) {
                    return x;
                }
                if (fixnumOne(x)) return y;
                if (fixnumOne(y)) return x;
                return ((RubyInteger) x).op_mul(context, y);
            } else if (x instanceof RubyFloat) {
                if (fixnumOne(y)) return x;
                return ((RubyFloat) x).op_mul(context, y);
            } else if (x instanceof RubyRational) {
                if (fixnumOne(y)) return x;
                return ((RubyRational) x).op_mul(context, y);
            } else {
                if (fixnumOne(y)) return x;
            }
        }

        return op_times.call(context, x, x, y);
    }

    public static RubyInteger f_mul(ThreadContext context, RubyInteger x, RubyInteger y) {
        return (RubyInteger) x.op_mul(context, y);
    }

    // MRI: safe_mul
    public static IRubyObject safe_mul(ThreadContext context, IRubyObject a, IRubyObject b, boolean az, boolean bz) {
        Ruby runtime = context.runtime;
        double v;
        if (!az && bz && a instanceof RubyFloat && !Double.isNaN(v = ((RubyFloat)a).getDoubleValue())) {
            a = v < 0.0d ? runtime.newFloat(-1.0d) : runtime.newFloat(1.0d);
        }
        if (!bz && az && b instanceof RubyFloat && !Double.isNaN(v = ((RubyFloat)b).getDoubleValue())) {
            b = v < 0.0d ? runtime.newFloat(-1.0) : runtime.newFloat(1.0);
        }
        return f_mul(context, a, b);
    }

    /** f_sub
     *
     */
    public static IRubyObject f_sub(ThreadContext context, IRubyObject x, IRubyObject y) {
        CachingCallSite op_minus = sites(context).op_minus;

        if (op_minus.isBuiltin(x) && fixnumZero(y)) {
            return x;
        }

        return op_minus.call(context, x, x, y);
    }

    public static RubyInteger f_sub(ThreadContext context, RubyInteger x, RubyInteger y) {
        return (RubyInteger) x.op_minus(context, y);
    }

    /** f_xor
     *
     */
    public static IRubyObject f_xor(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).op_xor.call(context, x, x, y);
    }

    public static IRubyObject f_xor(ThreadContext context, RubyInteger x, RubyInteger y) {
        return x.op_xor(context, y);
    }

    /** f_abs
     *
     */
    public static IRubyObject f_abs(ThreadContext context, IRubyObject x) {
        return sites(context).abs.call(context, x, x);
    }

    public static RubyInteger f_abs(ThreadContext context, RubyInteger x) {
        return (RubyInteger) x.abs(context);
    }

    public static RubyFloat f_abs(ThreadContext context, RubyFloat x) {
        return (RubyFloat) x.abs(context);
    }

    /** f_abs2
     *
     */
    public static IRubyObject f_abs2(ThreadContext context, IRubyObject x) {
        return sites(context).abs2.call(context, x, x);
    }

    /** f_arg
     *
     */
    public static IRubyObject f_arg(ThreadContext context, IRubyObject x) {
        return sites(context).arg.call(context, x, x);
    }

    /** f_conjugate
     *
     */
    public static IRubyObject f_conjugate(ThreadContext context, IRubyObject x) {
        return sites(context).conjugate.call(context, x, x);
    }

    /** f_denominator
     *
     */
    public static IRubyObject f_denominator(ThreadContext context, IRubyObject x) {
        return sites(context).denominator.call(context, x, x);
    }

    /** f_exact_p
     *
     */ // NOTE: not (really) used
    public static boolean f_exact_p(ThreadContext context, IRubyObject x) {
        return sites(context).exact.call(context, x, x).isTrue();
    }

    /** f_numerator
     *
     */
    public static IRubyObject f_numerator(ThreadContext context, IRubyObject x) {
        return sites(context).numerator.call(context, x, x);
    }

    /** f_polar
     *
     */
    public static IRubyObject f_polar(ThreadContext context, IRubyObject x) {
        return sites(context).polar.call(context, x, x);
    }

    /** f_real_p
     *
     */
    public static boolean f_real_p(ThreadContext context, IRubyObject x) {
        // NOTE: can not use instanceof RubyNumeric + ((RubyNumeric) x).isReal()
        // since Numeric is not a terminal type -> might get sub-classed by user
        switch (x.getMetaClass().getClassIndex()) {
            case FLOAT:
            case FIXNUM:
            case BIGNUM:
            case RATIONAL:
                return ((RubyNumeric) x).isReal(); // true
            case COMPLEX:
                return ((RubyComplex) x).isReal(); // false

        }
        return sites(context).real.call(context, x, x).isTrue();
    }

    /** f_integer_p
     *
     */
    public static boolean f_integer_p(ThreadContext context, IRubyObject x) {
        return sites(context).integer.call(context, x, x).isTrue();
    }

    public static boolean f_integer_p(ThreadContext context, RubyNumeric x) {
        switch (x.getMetaClass().getClassIndex()) {
            case FIXNUM:
            case BIGNUM:
                return true;
            case FLOAT:
            case RATIONAL:
            case COMPLEX:
                return false;
        }
        return sites(context).integer.call(context, x, x).isTrue();
    }

    /** f_divmod
     *
     */
    public static IRubyObject f_divmod(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).divmod.call(context, x, x, y);
    }

    public static IRubyObject f_divmod(ThreadContext context, RubyInteger x, IRubyObject y) {
        return x.divmod(context, y);
    }

    /** f_floor
     *
     */
    public static IRubyObject f_floor(ThreadContext context, IRubyObject x) {
        return sites(context).floor.call(context, x, x);
    }

    /** f_inspect
     *
     */
    public static IRubyObject f_inspect(ThreadContext context, IRubyObject x) {
        return sites(context).inspect.call(context, x, x);
    }

    /** f_negate
     *
     */
    public static IRubyObject f_negate(ThreadContext context, IRubyObject x) {
        return sites(context).op_uminus.call(context, x, x);
    }

    public static RubyInteger f_negate(ThreadContext context, RubyInteger x) {
        return x.negate();
    }

    /** f_to_f
     *
     */
    public static IRubyObject f_to_f(ThreadContext context, IRubyObject x) {
        return sites(context).to_f.call(context, x, x);
    }

    /** f_to_i
     *
     */
    public static IRubyObject f_to_i(ThreadContext context, IRubyObject x) {
        return sites(context).to_i.call(context, x, x);
    }

    /** f_to_r
     *
     */
    public static IRubyObject f_to_r(ThreadContext context, IRubyObject x) {
        return sites(context).to_r.call(context, x, x);
    }

    public static RubyNumeric f_to_r(ThreadContext context, RubyInteger x) {
        return (RubyNumeric) x.to_r(context);
    }

    /** f_to_s
     *
     */
    public static IRubyObject f_to_s(ThreadContext context, IRubyObject x) {
        return sites(context).to_s.call(context, x, x);
    }

    /** f_truncate
     *
     */
    public static IRubyObject f_truncate(ThreadContext context, IRubyObject x) {
        return sites(context).truncate.call(context, x, x);
    }

    /** f_equal
     *
     * Note: This may not return a value which is a boolean.  other.== can
     * return non-boolean (which unless it is nil it will be isTrue()).
     *
     */
    public static IRubyObject f_equal(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(context, ((RubyFixnum) x).getLongValue() == ((RubyFixnum) y).getLongValue());
        }

        return sites(context).op_equals.call(context, x, x, y);
    }

    public static IRubyObject f_equal(ThreadContext context, RubyInteger x, RubyInteger y) {
        return x.op_equal(context, y);
    }

    /** f_expt
     *
     */
    public static IRubyObject f_expt(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).op_exp.call(context, x, x, y);
    }

    public static RubyNumeric f_expt(ThreadContext context, RubyInteger x, RubyInteger y) {
        return (RubyNumeric) x.op_pow(context, y);
    }

    /** f_idiv
     *
     */
    public static IRubyObject f_idiv(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).div.call(context, x, x, y);
    }

    public static RubyInteger f_idiv(ThreadContext context, RubyInteger x, RubyInteger y) {
        return (RubyInteger) x.idiv(context, y);
    }

    /** f_quo
     *
     */
    public static IRubyObject f_quo(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).quo.call(context, x, x, y);
    }

    public static IRubyObject f_quo(ThreadContext context, RubyFloat x, RubyFloat y) {
        return x.quo(context, y);
    }

    /**
     * MRI: f_reciprocal
     */
    public static IRubyObject f_reciprocal(ThreadContext context, IRubyObject x) {
        return f_quo(context, RubyFixnum.one(context.runtime), x);
    }

    /** f_rshift
     *
     */
    public static IRubyObject f_rshift(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).op_rshift.call(context, x, x, y);
    }

    /** f_lshift
     *
     */
    public static IRubyObject f_lshift(ThreadContext context, IRubyObject x, IRubyObject y) {
        return sites(context).op_lshift.call(context, x, x, y);
    }

    /** f_negative_p
     *
     */
    public static boolean f_negative_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyInteger) return ((RubyInteger) x).signum() == -1;
        return sites(context).op_lt.call(context, x, x, RubyFixnum.zero(context.runtime)).isTrue();
    }

    public static boolean f_negative_p(ThreadContext context, RubyInteger x) {
        return x.signum() == -1;
    }

    public static boolean f_negative_p(ThreadContext context, RubyFloat x) {
        return x.signum() == -1;
    }

    /** f_zero_p
     *
     */
    public static boolean f_zero_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyInteger) return ((RubyInteger) x).isZero();
        if (x instanceof RubyFloat) return ((RubyFloat) x).signum() == 0;
        return sites(context).op_equals.call(context, x, x, RubyFixnum.zero(context.runtime)).isTrue();
    }

    public static boolean f_zero_p(ThreadContext context, RubyInteger x) {
        return x.isZero();
    }

    /** f_one_p
     *
     */
    public static boolean f_one_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFixnum) return ((RubyFixnum) x).getLongValue() == 1;
        return sites(context).op_equals.call(context, x, x, RubyFixnum.one(context.runtime)).isTrue();
    }

   /** f_minus_one_p
    *
    */
    public static boolean f_minus_one_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFixnum) return ((RubyFixnum) x).getLongValue() == -1;
        return sites(context).op_equals.call(context, x, x, RubyFixnum.minus_one(context.runtime)).isTrue();
    }

   /** f_odd_p
    *
    */
    public static boolean f_odd_p(ThreadContext context, IRubyObject i) {
        if (i instanceof RubyFixnum) return ((RubyFixnum) i).getLongValue() % 2 != 0;
        RubyFixnum two = RubyFixnum.two(context.runtime);
        return (((RubyFixnum) sites(context).op_mod.call(context, i, i, two)).getLongValue() != 0);
    }

    /**
     * MRI: int_odd_p
     */


    /** i_gcd
     *
     */
    public static long i_gcd(long x, long y) {
        long shift, uz, vz;
        if (x == Long.MIN_VALUE) {
            if (y == Long.MIN_VALUE)
                return x;
            return 1L << Long.numberOfTrailingZeros(Math.abs(y));
        }
        if (y == Long.MIN_VALUE) {
            return 1L << Long.numberOfTrailingZeros(Math.abs(x));
        }

        x = Math.abs(x);
        y = Math.abs(y);
        if (x == 0) {
            return y;
        }
        if (y == 0 || x == y) {
            return x;
        }
        uz = Long.numberOfTrailingZeros(x);
        vz = Long.numberOfTrailingZeros(y);
        shift = Math.min(uz, vz);

        x >>= uz;
        y >>= vz;

        while (x != y) {
            if (x > y) {
                x -= y;
                x >>= Long.numberOfTrailingZeros(x);
            } else {
                y -= x;
                y >>= Long.numberOfTrailingZeros(y);
            }
        }

        return x << shift;
    }

    /** f_gcd
     *
     */
    public static IRubyObject f_gcd(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum && isLongMinValue((RubyFixnum) x)) {
            return RubyFixnum.newFixnum(context.runtime, i_gcd(((RubyFixnum)x).getLongValue(), ((RubyFixnum)y).getLongValue()));
        }

        if (f_negative_p(context, x)) x = f_negate(context, x);
        if (f_negative_p(context, y)) y = f_negate(context, y);

        if (f_zero_p(context, x)) return y;
        if (f_zero_p(context, y)) return x;

        for (;;) {
            if (x instanceof RubyFixnum && y instanceof RubyFixnum && isLongMinValue((RubyFixnum) x)) {
                return RubyFixnum.newFixnum(context.runtime, i_gcd(((RubyFixnum)x).getLongValue(), ((RubyFixnum)y).getLongValue()));
            }
            IRubyObject z = x;
            x = f_mod(context, y, x);
            y = z;
        }
    }

    // 'fast' gcd version
    public static RubyInteger f_gcd(ThreadContext context, RubyInteger x, RubyInteger y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum && isLongMinValue((RubyFixnum) x))
            return RubyFixnum.newFixnum(context.runtime, i_gcd(x.getLongValue(), y.getLongValue()));

        BigInteger gcd = x.getBigIntegerValue().gcd(y.getBigIntegerValue());

        if (gcd.compareTo(RubyBignum.LONG_MAX) <= 0) { // gcd always positive
            return RubyFixnum.newFixnum(context.runtime, gcd.longValue());
        }
        return RubyBignum.newBignum(context.runtime, gcd);
    }

    /**
     * Check if the Fixnum passed is equal to Long.MAX_VALUE.
     *
     * @param x the Fixnum to compare
     * @return true if it is equal to Long.MAX_VALUE, false otherwise.
     */
    protected static boolean isLongMinValue(RubyFixnum x) {
        return x.getLongValue() != Long.MIN_VALUE;
    }

    /** f_lcm
     *
     */
    public static IRubyObject f_lcm(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (f_zero_p(context, x) || f_zero_p(context, y)) {
            return RubyFixnum.zero(context.runtime);
        }
        return f_abs(context, f_mul(context, f_div(context, x, f_gcd(context, x, y)), y));
    }

    public static long i_ilog2(ThreadContext context, IRubyObject x) {
        return i_ilog2(context, x.convertToInteger());
    }

    public static long i_ilog2(ThreadContext context, RubyInteger x) {
        long q = (x.size(context).convertToInteger().getLongValue() - 8) * 8 + 1;

        if (q > 0) {
            x = x.op_rshift(context, q);
        }

        long fx = x.getLongValue();
        long r = -1;

        while (fx != 0) {
            fx >>= 1;
            r += 1;
        }

        return q + r;
    }

    public static double ldexp(double f, long e) {
        return f * Math.pow(2.0, e);
    }

    public static double frexp(double mantissa, long[]e) {
        short sign = 1;
        long exponent = 0;

        if (Double.isInfinite(mantissa) || Double.isNaN(mantissa)) {
            return mantissa;
        }

        if (mantissa != 0.0) {
            if (mantissa < 0) {
                mantissa = -mantissa;
                sign = -1;
            }

            for (; mantissa < 0.5; mantissa *= 2.0, exponent -=1) { }
            for (; mantissa >= 1.0; mantissa *= 0.5, exponent +=1) { }
        }

        e[0] = exponent;
        return sign * mantissa;
    }

    private static final long SQRT_LONG_MAX = ((long)1) << ((8 * 8 - 1) / 2);
    static boolean fitSqrtLong(long n) {
        return n < SQRT_LONG_MAX && n >= -SQRT_LONG_MAX;
    }

    // MRI: rb_int_pow
    public static RubyNumeric int_pow(ThreadContext context, long x, long y) {
        boolean neg = x < 0;
        long z = 1;
        if (neg) x = -x;
        if ((y & 1) != 0) {
            z = x;
        } else {
            neg = false;
        }

        y &= ~1;
        Ruby runtime = context.runtime;

        do {
            while (y % 2 == 0) {
                if (!fitSqrtLong(x)) {
                    IRubyObject v = RubyBignum.newBignum(runtime, x).op_pow(context, y);
                    if (z != 1) v = RubyBignum.newBignum(runtime, neg ? -z : z).op_mul(context, v);
                    return (RubyNumeric) v;
                }
                x *= x;
                y >>= 1;
            }

            if (multiplyOverflows(x, z)) {
                IRubyObject v = RubyBignum.newBignum(runtime, x).op_pow(context, y);
                if (z != 1) v = RubyBignum.newBignum(runtime, neg ? -z : z).op_mul(context, v);
                return (RubyNumeric) v;
            }
            z = x * z;
        } while(--y != 0);
        if (neg) z = -z;
        return RubyFixnum.newFixnum(runtime, z);
    }

    // MRI: rb_num_pow
    public static IRubyObject num_pow(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyInteger) return ((RubyInteger) x).pow(context, y);
        if (x instanceof RubyFloat) return ((RubyFloat) x).op_pow(context, y);
//        if (SPECIAL_CONST_P(x)) return Qnil;
        if (x instanceof RubyComplex) return ((RubyComplex) x).op_expt(context, y);
        if (x instanceof RubyRational) return ((RubyRational) x).op_expt(context, y);
        return context.nil;
    }

    public static boolean multiplyOverflows(long a, long b) {
            return a == 0 ? false :
                    a == -1 ? b < -Long.MAX_VALUE :
                            a > 0 ? (b > 0 ? Long.MAX_VALUE / a < b : Long.MIN_VALUE / a > b) :
                                    (b > 0 ? Long.MIN_VALUE / a < b : Long.MAX_VALUE / a > b);
    }


    public static boolean k_exact_p(IRubyObject x) {
        return !(x instanceof RubyFloat);
    }

    public static boolean k_inexact_p(IRubyObject x) {
        return x instanceof RubyFloat;
    }

    public static boolean k_integer_p(IRubyObject x) {
        return x instanceof RubyInteger;
    }

    public static boolean k_numeric_p(IRubyObject x) {
        return x instanceof RubyNumeric;
    }

    /**
     * Rotate the given long bits left.
     *
     * @param bits the bits to rotate
     * @param rot how many bit positions to rotate
     * @return the rotated value
     */
    public static long rotl(long bits, int rot) {
        return (bits << (rot & 63)) | (bits >>> (-rot & 63));
    }

    /**
     * Rotate the given long bits right.
     *
     * @param bits the bits to rotate
     * @param rot how many bit positions to rotate
     * @return the rotated value
     */
    public static long rotr(long bits, int rot) {
        return (bits << (-rot & 63)) | (bits >>> (rot & 63));
    }

    public static final class ComplexPatterns {
        public static final Regex comp_pat0, comp_pat1, comp_pat2, underscores_pat;
        static {
            String WS = "\\s*";
            String DIGITS = "(?:\\d(?:_\\d|\\d)*)";
            String NUMERATOR = "(?:" + DIGITS + "?\\.)?" + DIGITS + "(?:[eE][-+]?" + DIGITS + ")?";
            String DENOMINATOR = DIGITS;
            String NUMBER = "[-+]?" + NUMERATOR + "(?:\\/" + DENOMINATOR + ")?";
            String NUMBERNOS = NUMERATOR + "(?:\\/" + DENOMINATOR + ")?";
            String PATTERN0 = "\\A" + WS + "(" + NUMBER + ")@(" + NUMBER + ")" + WS;
            String PATTERN1 = "\\A" + WS + "([-+])?(" + NUMBER + ")?[iIjJ]" + WS;
            String PATTERN2 = "\\A" + WS + "(" + NUMBER + ")(([-+])(" + NUMBERNOS + ")?[iIjJ])?" + WS;
            comp_pat0 = new Regex(PATTERN0.getBytes(), 0, PATTERN0.length(), 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
            comp_pat1 = new Regex(PATTERN1.getBytes(), 0, PATTERN1.length(), 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
            comp_pat2 = new Regex(PATTERN2.getBytes(), 0, PATTERN2.length(), 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
            underscores_pat = new Regex("_+".getBytes(), 0, 2, 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
        }
    }

    public static final class RationalPatterns {
        public static final Regex rat_pat, an_e_pat, a_dot_pat;
        static {
            String WS = "\\s*";
            String DIGITS = "(?:\\d(?:_\\d|\\d)*)";
            String NUMERATOR = "(?:" + DIGITS + "?\\.)?" + DIGITS + "(?:[eE][-+]?" + DIGITS + ")?";
            String DENOMINATOR = DIGITS;
            String PATTERN = "\\A" + WS + "([-+])?(" + NUMERATOR + ")(?:\\/(" + DENOMINATOR + "))?" + WS;
            rat_pat = new Regex(PATTERN.getBytes(), 0, PATTERN.length(), 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
            an_e_pat = new Regex("[Ee]".getBytes(), 0, 4, 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
            a_dot_pat = new Regex("\\.".getBytes(), 0, 2, 0, ASCIIEncoding.INSTANCE, WarnCallback.NONE);
        }
    }

    /*
    The algorithm here is the method described in CLISP.  Bruno Haible has
    graciously given permission to use this algorithm.  He says, "You can use
    it, if you present the following explanation of the algorithm."

    Algorithm (recursively presented):
        If x is a rational number, return x.
        If x = 0.0, return 0.
        If x < 0.0, return (- (rationalize (- x))).
        If x > 0.0:
        Call (integer-decode-float x). It returns a m,e,s=1 (mantissa,
        exponent, sign).
        If m = 0 or e >= 0: return x = m*2^e.
        Search a rational number between a = (m-1/2)*2^e and b = (m+1/2)*2^e
        with smallest possible numerator and denominator.
        Note 1: If m is a power of 2, we ought to take a = (m-1/4)*2^e.
            But in this case the result will be x itself anyway, regardless of
            the choice of a. Therefore we can simply ignore this case.
        Note 2: At first, we need to consider the closed interval [a,b].
            but since a and b have the denominator 2^(|e|+1) whereas x itself
            has a denominator <= 2^|e|, we can restrict the search to the open
            interval (a,b).
        So, for given a and b (0 < a < b) we are searching a rational number
        y with a <= y <= b.
        Recursive algorithm fraction_between(a,b):
            c := (ceiling a)
            if c < b
                then return c       ; because a <= c < b, c integer
                else
                    ; a is not integer (otherwise we would have had c = a < b)
                    k := c-1          ; k = floor(a), k < a < b <= k+1
                    return y = k + 1/fraction_between(1/(b-k), 1/(a-k))
                                      ; note 1 <= 1/(b-k) < 1/(a-k)

    You can see that we are actually computing a continued fraction expansion.

    Algorithm (iterative):
        If x is rational, return x.
        Call (integer-decode-float x). It returns a m,e,s (mantissa,
            exponent, sign).
        If m = 0 or e >= 0, return m*2^e*s. (This includes the case x = 0.0.)
        Create rational numbers a := (2*m-1)*2^(e-1) and b := (2*m+1)*2^(e-1)
        (positive and already in lowest terms because the denominator is a
        power of two and the numerator is odd).
        Start a continued fraction expansion
            p[-1] := 0, p[0] := 1, q[-1] := 1, q[0] := 0, i := 0.
        Loop
            c := (ceiling a)
            if c >= b
                then k := c-1, partial_quotient(k), (a,b) := (1/(b-k),1/(a-k)),
                    goto Loop
        finally partial_quotient(c).
        Here partial_quotient(c) denotes the iteration
            i := i+1, p[i] := c*p[i-1]+p[i-2], q[i] := c*q[i-1]+q[i-2].
        At the end, return s * (p[i]/q[i]).
        This rational number is already in lowest terms because
        p[i]*q[i-1]-p[i-1]*q[i] = (-1)^i.
    */
    public static IRubyObject[] nurat_rationalize_internal(ThreadContext context, IRubyObject a, IRubyObject b) {
        IRubyObject p, q;
        IRubyObject c, k, t, p0, p1, p2, q0, q1, q2;

        RubyFixnum zero = RubyFixnum.zero(context.runtime);
        RubyFixnum one = RubyFixnum.one(context.runtime);

        p0 = q1 = zero;
        p1 = q0 = one;

        while (true) {
            c = sites(context).ceil.call(context, a, a);
            if (f_lt_p(context, c, b)) {
                break;
            }
            k = f_sub(context, c, one);
            p2 = f_add(context, f_mul(context, k, p1), p0);
            q2 = f_add(context, f_mul(context, k, q1), q0);
            t = f_quo(context, one, f_sub(context, b, k));
            b = f_quo(context, one, f_sub(context, a, k));
            a = t;
            p0 = p1;
            q0 = q1;
            p1 = p2;
            q1 = q2;
        }
        p = f_add(context, f_mul(context, c, p1), p0);
        q = f_add(context, f_mul(context, c, q1), q0);

        return new IRubyObject[] { p, q };

    }

    public static IRubyObject[] nurat_rationalize_internal(ThreadContext context, IRubyObject[] ary) {
        return nurat_rationalize_internal(context, ary[0], ary[1]);
    }

    public static void checkInteger(ThreadContext context, IRubyObject obj) {
        if (!(obj instanceof RubyInteger)) {
            throw context.runtime.newTypeError("not an integer");
        }
    }

    private static JavaSites.NumericSites sites(ThreadContext context) {
        return context.sites.Numeric;
    }
}
