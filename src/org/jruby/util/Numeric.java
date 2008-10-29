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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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
package org.jruby.util;

import org.joni.Regex;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Numeric {
    public static final boolean CANON = true;

    /** f_add
     * 
     */
    public static IRubyObject f_add(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (y instanceof RubyFixnum && ((RubyFixnum)y).getLongValue() == 0) return x;
        if (x instanceof RubyFixnum && ((RubyFixnum)x).getLongValue() == 0) return y;
        return x.callMethod(context, "+", y);
    }

    /** f_cmp
     * 
     */
    public static IRubyObject f_cmp(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            long c = ((RubyFixnum)x).getLongValue() - ((RubyFixnum)y).getLongValue();
            if (c > 0) {
                return RubyFixnum.one(context.getRuntime());
            } else if (c < 0) {
                return RubyFixnum.minus_one(context.getRuntime());
            }
            return RubyFixnum.zero(context.getRuntime());
        }
        return x.callMethod(context, "<=>", y);
    }

    /** f_div
     * 
     */
    public static IRubyObject f_div(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (y instanceof RubyFixnum && ((RubyFixnum)y).getLongValue() == 1) return x;
        return x.callMethod(context, "/", y);
    }

    /** f_gt_p 
     * 
     */
    public static IRubyObject f_gt_p(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() > ((RubyFixnum)y).getLongValue() ? context.getRuntime().getTrue() : context.getRuntime().getFalse(); 
        }
        return x.callMethod(context, ">", y);
    }

    /** f_lt_p 
     * 
     */
    public static IRubyObject f_lt_p(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() < ((RubyFixnum)y).getLongValue() ? context.getRuntime().getTrue() : context.getRuntime().getFalse(); 
        }
        return x.callMethod(context, "<", y);
    }
    
    /** f_mod
     * 
     */
    public static IRubyObject f_mod(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "%", y);
    }
    
    /** f_mul
     * 
     */
    public static IRubyObject f_mul(ThreadContext context, IRubyObject x, IRubyObject y) {
        Ruby runtime = context.getRuntime();
        if (y instanceof RubyFixnum) {
            long iy = ((RubyFixnum)y).getLongValue();
            if (iy == 0) {
                if (x instanceof RubyFixnum || x instanceof RubyBignum) return RubyFixnum.zero(runtime);
            } else if (iy == 1) {
                return x;
            }
        } else if (x instanceof RubyFixnum) {
            long ix = ((RubyFixnum)x).getLongValue();
            if (ix == 0) {
                if (y instanceof RubyFixnum || y instanceof RubyBignum) return RubyFixnum.zero(runtime);
            } else if (ix == 1) {
                return y;
            }
        }
        return x.callMethod(context, "*", y);
    }

    /** f_sub
     * 
     */
    public static IRubyObject f_sub(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (y instanceof RubyFixnum && ((RubyFixnum)y).getLongValue() == 0) return x;
        return x.callMethod(context, "-", y);
    }

    /** f_xor
     * 
     */
    public  static IRubyObject f_xor(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "^", y);
    }

    /** f_abs
     * 
     */
    public static IRubyObject f_abs(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "abs");
    }

    /** f_abs2
     * 
     */
    public static IRubyObject f_abs2(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "abs2");
    }

    /** f_arg
     * 
     */
    public static IRubyObject f_arg(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "arg");
    }
    
    /** f_conjugate
     * 
     */
    public static IRubyObject f_conjugate(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "conjugate");
    }

    /** f_denominator
     * 
     */
    public static IRubyObject f_denominator(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "denominator");
    }

    /** f_exact_p
     * 
     */
    public static IRubyObject f_exact_p(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "exact?");
    }

    /** f_numerator
     * 
     */
    public static IRubyObject f_numerator(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "numerator");
    }

    /** f_polar
     * 
     */
    public static IRubyObject f_polar(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "polar");
    }

    /** f_real_p
     * 
     */
    public static IRubyObject f_real_p(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "real?");
    }

    /** f_integer_p
     * 
     */
    public static IRubyObject f_integer_p(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "integer?");
    }

    /** f_divmod
     * 
     */
    public static IRubyObject f_divmod(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "divmod", y);
    }

    /** f_floor
     * 
     */
    public static IRubyObject f_floor(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "floor");
    }

    /** f_inspect
     * 
     */
    public static IRubyObject f_inspect(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "inspect");
    }

    /** f_negate
     * 
     */
    public static IRubyObject f_negate(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "-@");
    }
    
    /** f_to_f
     * 
     */
    public static IRubyObject f_to_f(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "to_f");
    }
    
    /** f_to_i
     * 
     */
    public static IRubyObject f_to_i(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "to_i");
    }
    
    /** f_to_r
     * 
     */
    public static IRubyObject f_to_r(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "to_r");
    }
    
    /** f_to_s
     * 
     */
    public static IRubyObject f_to_s(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "to_s");
    }
    
    /** f_truncate
     * 
     */
    public static IRubyObject f_truncate(ThreadContext context, IRubyObject x) {
        return x.callMethod(context, "truncate");
    }
    
    /** f_equal_p
     * 
     */
    public static boolean f_equal_p(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return ((RubyFixnum)x).getLongValue() == ((RubyFixnum)y).getLongValue();
        }
        return x.callMethod(context, "==", y).isTrue();
    }

    /** f_expt
     * 
     */
    public static IRubyObject f_expt(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "**", y);
    }
    
    /** f_idiv
     * 
     */
    public static IRubyObject f_idiv(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "div", y);
    }
    
    /** f_quo
     * 
     */
    public static IRubyObject f_quo(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "quo", y);
    }

    /** f_rshift
     * 
     */
    public static IRubyObject f_rshift(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, ">>", y);
    }

    /** f_lshift
     * 
     */
    public static IRubyObject f_lshift(ThreadContext context, IRubyObject x, IRubyObject y) {
        return x.callMethod(context, "<<", y);
    }

    /** f_negative_p
     * 
     */
    public static boolean f_negative_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFixnum) return ((RubyFixnum)x).getLongValue() < 0;
        return x.callMethod(context, "<", RubyFixnum.zero(context.getRuntime())).isTrue();
    }
    
    /** f_zero_p
     * 
     */
    public static boolean f_zero_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFixnum) return ((RubyFixnum)x).getLongValue() == 0;
        return x.callMethod(context, "==", RubyFixnum.zero(context.getRuntime())).isTrue();
    }
    
    /** f_one_p
     * 
     */
    public static boolean f_one_p(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFixnum) return ((RubyFixnum)x).getLongValue() == 1;
        return x.callMethod(context, "==", RubyFixnum.one(context.getRuntime())).isTrue();
    }
    
    /** i_gcd
     * 
     */
    public static long i_gcd(long x, long y) {
        if (x < 0) x = -x;
        if (y < 0) y = -y;

        if (x == 0) return y;
        if (y == 0) return x;

        while (x > 0) {
            long t = x;
            x = y % x;
            y = t;
        }

        return y;
    }
    
    /** f_gcd
     * 
     */
    public static IRubyObject f_gcd(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (x instanceof RubyFixnum && y instanceof RubyFixnum) {
            return RubyFixnum.newFixnum(context.getRuntime(), i_gcd(((RubyFixnum)x).getLongValue(), ((RubyFixnum)y).getLongValue()));
        }
        
        if (f_negative_p(context, x)) x = f_negate(context, x);
        if (f_negative_p(context, y)) y = f_negate(context, y);
        
        if (f_zero_p(context, x)) return y;
        if (f_zero_p(context, y)) return x;
        
        for (;;) {
            if (x instanceof RubyFixnum) {
                if (((RubyFixnum)x).getLongValue() == 0) return y;
                if (y instanceof RubyFixnum) {
                    return RubyFixnum.newFixnum(context.getRuntime(), i_gcd(((RubyFixnum)x).getLongValue(), ((RubyFixnum)y).getLongValue()));
                }
            }
            IRubyObject z = x;
            x = f_mod(context, y, x);
            y = z;
        }
    }
    
    /** f_lcm
     * 
     */
    public static IRubyObject f_lcm(ThreadContext context, IRubyObject x, IRubyObject y) {
        if (f_zero_p(context, x) || f_zero_p(context, y)) return RubyFixnum.zero(context.getRuntime());
        return f_abs(context, f_mul(context, f_div(context, x, f_gcd(context, x, y)), y));
    }
    
    public static long i_ilog2(ThreadContext context, IRubyObject x) {
        long q = (x.callMethod(context, "size").convertToInteger().getLongValue() - 8) * 8 + 1;

        if (q > 0) x = f_rshift(context, x, RubyFixnum.newFixnum(context.getRuntime(), q));

        long fx = x.convertToInteger().getLongValue();
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
    
    private static long SQRT_LONG_MAX = ((long)1) << ((8 * 8 - 1) / 2); 
    static boolean fitSqrtLong(long n) {
        return n < SQRT_LONG_MAX && n >= -SQRT_LONG_MAX;
    }

    public static IRubyObject int_pow(ThreadContext context, long x, long y) {
        boolean neg = x < 0;
        long z = 1;
        if (neg) x = -x;
        if ((y & 1) != 0) {
            z = x;
        } else {
            neg = false;
        }
        
        y &= ~1;
        Ruby runtime = context.getRuntime();
        
        do {
            while (y % 2 == 0) {
                if (!fitSqrtLong(x)) {
                    IRubyObject v = RubyBignum.newBignum(runtime, RubyBignum.fix2big(RubyFixnum.newFixnum(runtime, x))).op_pow(context, RubyFixnum.newFixnum(runtime, y));
                    if (z != 1) v = RubyBignum.newBignum(runtime, RubyBignum.fix2big(RubyFixnum.newFixnum(runtime, neg ? -z : z))).op_mul(context, v);
                    return v;
                }
                x *= x;
                y >>= 1;
            }
            
            long xz = x * x;
            if (xz  / x != z) {
                IRubyObject v = RubyBignum.newBignum(runtime, RubyBignum.fix2big(RubyFixnum.newFixnum(runtime, x))).op_pow(context, RubyFixnum.newFixnum(runtime, y));
                if (z != 1) v = RubyBignum.newBignum(runtime, RubyBignum.fix2big(RubyFixnum.newFixnum(runtime, neg ? -z : z))).op_mul(context, v);
                return v;
            }
            z = xz;
        } while(--y != 0);
        if (neg) z = -z;
        return RubyFixnum.newFixnum(runtime, z);
    }

    public static boolean k_exact_p(IRubyObject x) {
        return !(x instanceof RubyFloat);
    }

    public static boolean k_inexact_p(IRubyObject x) {
        return x instanceof RubyFloat;
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
            comp_pat0 = new Regex(PATTERN0.getBytes(), 0, PATTERN0.length(), 0, ASCIIEncoding.INSTANCE);
            comp_pat1 = new Regex(PATTERN1.getBytes(), 0, PATTERN1.length(), 0, ASCIIEncoding.INSTANCE);
            comp_pat2 = new Regex(PATTERN2.getBytes(), 0, PATTERN2.length(), 0, ASCIIEncoding.INSTANCE);
            underscores_pat = new Regex("_+".getBytes(), 0, 2, 0, ASCIIEncoding.INSTANCE);
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
            rat_pat = new Regex(PATTERN.getBytes(), 0, PATTERN.length(), 0, ASCIIEncoding.INSTANCE);
            an_e_pat = new Regex("[Ee]".getBytes(), 0, 4, 0, ASCIIEncoding.INSTANCE);
            a_dot_pat = new Regex("\\.".getBytes(), 0, 2, 0, ASCIIEncoding.INSTANCE);            
        }
    }
}
