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

import static org.jruby.util.Numeric.f_add;
import static org.jruby.util.Numeric.f_cmp;
import static org.jruby.util.Numeric.f_div;
import static org.jruby.util.Numeric.f_equal_p;
import static org.jruby.util.Numeric.f_expt;
import static org.jruby.util.Numeric.f_floor;
import static org.jruby.util.Numeric.f_gcd;
import static org.jruby.util.Numeric.f_idiv;
import static org.jruby.util.Numeric.f_inspect;
import static org.jruby.util.Numeric.f_integer_p;
import static org.jruby.util.Numeric.f_lt_p;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negate;
import static org.jruby.util.Numeric.f_negative_p;
import static org.jruby.util.Numeric.f_one_p;
import static org.jruby.util.Numeric.f_rshift;
import static org.jruby.util.Numeric.f_sub;
import static org.jruby.util.Numeric.f_to_f;
import static org.jruby.util.Numeric.f_to_i;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.Numeric.f_to_s;
import static org.jruby.util.Numeric.f_truncate;
import static org.jruby.util.Numeric.f_xor;
import static org.jruby.util.Numeric.f_zero_p;
import static org.jruby.util.Numeric.i_gcd;
import static org.jruby.util.Numeric.i_ilog2;
import static org.jruby.util.Numeric.k_exact_p;
import static org.jruby.util.Numeric.ldexp;

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;

/**
 *  1.9 rational.c as of revision: 20011
 */

@JRubyClass(name = "Rational", parent = "Numeric", include = "Precision")
public class RubyRational extends RubyNumeric {
    
    public static RubyClass createRationalClass(Ruby runtime) {
        RubyClass rationalc = runtime.defineClass("Rational", runtime.getNumeric(), RATIONAL_ALLOCATOR);
        runtime.setRational(rationalc);

        rationalc.index = ClassIndex.RATIONAL;
        rationalc.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyRational;
            }
        };

        rationalc.defineAnnotatedMethods(RubyRational.class);

        rationalc.getSingletonClass().undefineMethod("allocate");
        rationalc.getSingletonClass().undefineMethod("new");

        return rationalc;
    }

    private static ObjectAllocator RATIONAL_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFixnum zero = RubyFixnum.zero(runtime);
            return new RubyRational(runtime, klass, zero, zero);
        }
    };

    /** internal
     * 
     */
    private RubyRational(Ruby runtime, IRubyObject clazz, IRubyObject num, IRubyObject den) {
        super(runtime, (RubyClass)clazz);
        this.num = num;
        this.den = den;
    }

    /** rb_rational_raw
     * 
     */
    static RubyRational newRationalRaw(Ruby runtime, IRubyObject x, RubyObject y) {
        return new RubyRational(runtime, runtime.getRational(), x, y);
    }

    /** rb_rational_raw1
     * 
     */
    static RubyRational newRationalRaw(Ruby runtime, IRubyObject x) {
        return new RubyRational(runtime, runtime.getRational(), x, RubyFixnum.one(runtime));
    }

    /** rb_rational_new1
     * 
     */
    static IRubyObject newRationalCanonicalize(ThreadContext context, IRubyObject x) {
        return newRationalCanonicalize(context, x, RubyFixnum.one(context.getRuntime()));
    }

    /** rb_rational_new
     * 
     */
    private static IRubyObject newRationalCanonicalize(ThreadContext context, IRubyObject x, IRubyObject y) {
        return canonicalizeInternal(context, context.getRuntime().getRational(), x, y);
    }
    
    /** f_rational_new2
     * 
     */
    private static IRubyObject newRational(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyRational) && !(y instanceof RubyRational);
        return canonicalizeInternal(context, clazz, x, y);
    }

    /** f_rational_new1
     * 
     */
    private static IRubyObject newRational(ThreadContext context, IRubyObject clazz, IRubyObject x) {
        assert !(x instanceof RubyRational);
        return canonicalizeInternal(context, clazz, x, RubyFixnum.one(context.getRuntime()));
    }

    /** f_rational_new_no_reduce2
     * 
     */
    private static IRubyObject newRationalNoReduce(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyRational) && !(y instanceof RubyRational);
        return canonicalizeInternalNoReduce(context, clazz, x, y);
    }

    /** f_rational_new_no_reduce1
     * 
     */
    private static IRubyObject newRationalNoReduce(ThreadContext context, IRubyObject clazz, IRubyObject x) {
        assert !(x instanceof RubyRational);
        return canonicalizeInternalNoReduce(context, clazz, x, RubyFixnum.one(context.getRuntime()));
    }

    /** f_rational_new_bang2
     * 
     */
    private static RubyRational newRationalBang(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) { 
        assert !f_negative_p(context, y) && !(f_zero_p(context, y));
        return new RubyRational(context.getRuntime(), clazz, x, y);
    }

    /** f_rational_new_bang1
     * 
     */
    private static RubyRational newRationalBang(ThreadContext context, IRubyObject clazz, IRubyObject x) { 
        return newRationalBang(context, clazz, x, RubyFixnum.one(context.getRuntime()));
    }
    
    private IRubyObject num;
    private IRubyObject den;

    /** nurat_s_new_bang
     * 
     */
    @Deprecated
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject[]args) {
        switch (args.length) {
        case 1: return newInstanceBang(context, recv, args[0]);
        case 2: return newInstanceBang(context, recv, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 1);
        return null;
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject num) {
        return newInstanceBang(context, recv, num, RubyFixnum.one(context.getRuntime()));
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_8)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject num, IRubyObject den) {
        if (!(num instanceof RubyInteger)) num = f_to_i(context, num);
        if (!(den instanceof RubyInteger)) den = f_to_i(context, den);
        
        Ruby runtime = context.getRuntime();
        IRubyObject res = f_cmp(context, den, RubyFixnum.zero(runtime));
        if (res == RubyFixnum.minus_one(runtime)) {
            num = f_negate(context, num);
            den = f_negate(context, den);
        } else if (res == RubyFixnum.zero(runtime)) {
            throw runtime.newZeroDivisionError();
        }

        return new RubyRational(runtime, recv, num, den);
    }
    
    /** nurat_int_check
     * 
     */
    static void intCheck(ThreadContext context, IRubyObject num) {
        if (num instanceof RubyFixnum || num instanceof RubyBignum) return;
        if (!(num instanceof RubyNumeric) || !num.callMethod(context, "integer?").isTrue()) {
            throw num.getRuntime().newArgumentError("not an integer");
        }
    }

    /** nurat_int_value
     * 
     */
    static IRubyObject intValue(ThreadContext context, IRubyObject num) {
        intCheck(context, num);
        if (!(num instanceof RubyInteger)) num = num.callMethod(context, "to_f");
        return num;
    }
    
    /** nurat_s_canonicalize_internal
     * 
     */
    private static boolean canonicalization = false;
    private static IRubyObject canonicalizeInternal(ThreadContext context, IRubyObject clazz, IRubyObject num, IRubyObject den) {
        Ruby runtime = context.getRuntime();
        IRubyObject res = f_cmp(context, den, RubyFixnum.zero(runtime));
        if (res == RubyFixnum.minus_one(runtime)) {
            num = f_negate(context, num);
            den = f_negate(context, den);
        } else if (res == RubyFixnum.zero(runtime)) {
            throw runtime.newZeroDivisionError();            
        }

        IRubyObject gcd = f_gcd(context, num, den);
        num = f_idiv(context, num, gcd);
        den = f_idiv(context, den, gcd);

        if (Numeric.CANON) {
            if (f_one_p(context, den) && canonicalization) return num;
        }

        return new RubyRational(context.getRuntime(), clazz, num, den);
    }
    
    /** nurat_s_canonicalize_internal_no_reduce
     * 
     */
    private static IRubyObject canonicalizeInternalNoReduce(ThreadContext context, IRubyObject clazz, IRubyObject num, IRubyObject den) {
        Ruby runtime = context.getRuntime();
        IRubyObject res = f_cmp(context, den, RubyFixnum.zero(runtime));
        if (res == RubyFixnum.minus_one(runtime)) {
            num = f_negate(context, num);
            den = f_negate(context, den);
        } else if (res == RubyFixnum.zero(runtime)) {
            throw runtime.newZeroDivisionError();            
        }        

        if (Numeric.CANON) {
            if (f_one_p(context, den) && canonicalization) return num;
        }

        return new RubyRational(context.getRuntime(), clazz, num, den);
    }
    
    /** nurat_s_new
     * 
     */
    @Deprecated
    public static IRubyObject newInstance(ThreadContext context, IRubyObject clazz, IRubyObject[]args) {
        switch (args.length) {
        case 1: return newInstance(context, clazz, args[0]);
        case 2: return newInstance(context, clazz, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 1);
        return null;
    }

    // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject clazz, IRubyObject num) {
        num = intValue(context, num);
        return canonicalizeInternal(context, clazz, num, RubyFixnum.one(context.getRuntime()));
    }

    // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject clazz, IRubyObject num, IRubyObject den) {
        num = intValue(context, num);
        den = intValue(context, den);
        return canonicalizeInternal(context, clazz, num, den);
    }
    
    /** rb_Rational1
     * 
     */
    public static IRubyObject newRationalConvert(ThreadContext context, IRubyObject x) {
        return newRationalConvert(context, x, RubyFixnum.one(context.getRuntime()));
    }

    /** rb_Rational/rb_Rational2
     * 
     */
    public static IRubyObject newRationalConvert(ThreadContext context, IRubyObject x, IRubyObject y) {
        return convert(context, context.getRuntime().getRational(), x, y);
    }
    
    @Deprecated
    public static IRubyObject convert(ThreadContext context, IRubyObject clazz, IRubyObject[]args) {
        switch (args.length) {
        case 1: return convert(context, clazz, args[0]);        
        case 2: return convert(context, clazz, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 1);
        return null;
    }

    /** nurat_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1) {
        return convertCommon(context, recv, a1, context.getRuntime().getNil());
    }

    /** nurat_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        return convertCommon(context, recv, a1, a2);
    }
    
    private static IRubyObject convertCommon(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        if (a1 instanceof RubyComplex) {
            RubyComplex a1Complex = (RubyComplex)a1;
            if (k_exact_p(a1Complex.getImage()) && f_zero_p(context, a1Complex.getImage())) a1 = a1Complex.getReal();            
        }

        if (a2 instanceof RubyComplex) {
            RubyComplex a2Complex = (RubyComplex)a2;
            if (k_exact_p(a2Complex.getImage()) && f_zero_p(context, a2Complex.getImage())) a2 = a2Complex.getReal();
        }
        
        DynamicScope scope = context.getCurrentScope();
        IRubyObject backref = scope.getBackRef(context.getRuntime());
        if (backref instanceof RubyMatchData) ((RubyMatchData)backref).use();
        
        if (a1 instanceof RubyFloat) {
            a1 = f_to_r(context, a1);
        } else if (a1 instanceof RubyString) {
            a1 = str_to_r_strict(context, a1);
        }
        
        if (a2 instanceof RubyFloat) {
            a2 = f_to_r(context, a2);
        } else if (a2 instanceof RubyString) {
            a2 = str_to_r_strict(context, a2);
        }
        
        scope.setBackRef(backref);

        if (a1 instanceof RubyRational) {
            if (a2.isNil() || (k_exact_p(a2) && f_one_p(context, a2))) return a1;
        }

        if (a2.isNil()) {
            if (a1 instanceof RubyNumeric && !f_integer_p(context, a1).isTrue()) return a1;
            return newInstance(context, recv, a1);
        } else {
            if (a1 instanceof RubyNumeric && a2 instanceof RubyNumeric &&
                (!f_integer_p(context, a1).isTrue() || !f_integer_p(context, a2).isTrue())) {
                return f_div(context, a1, a2);
            }
            return newInstance(context, recv, a1, a2);
        }
    }

    /** nurat_numerator
     * 
     */
    @JRubyMethod(name = "numerator")
    public IRubyObject numerator(ThreadContext context) {
        return num;
    }

    /** nurat_denominator
     * 
     */
    @JRubyMethod(name = "denominator")
    public IRubyObject denominator(ThreadContext context) {
        return den;
    }

    /** f_imul
     * 
     */
    private static IRubyObject f_imul(ThreadContext context, long a, long b) {
        Ruby runtime = context.getRuntime();
        if (a == 0 || b == 0) {
            return RubyFixnum.zero(runtime);
        } else if (a == 1) {
            return RubyFixnum.newFixnum(runtime, b);
        } else if (b == 1) {
            return RubyFixnum.newFixnum(runtime, a);
        }

        long c = a * b;
        if(c / a != b) {
            return RubyBignum.newBignum(runtime, a).op_mul(context, RubyBignum.newBignum(runtime, b));
        }
        return RubyFixnum.newFixnum(runtime, c);
    }
    
    /** f_addsub
     * 
     */
    private IRubyObject f_addsub(ThreadContext context, IRubyObject anum, IRubyObject aden, IRubyObject bnum, IRubyObject bden, boolean plus) {
        Ruby runtime = context.getRuntime();
        IRubyObject num, den, g, a, b;
        if (anum instanceof RubyFixnum && aden instanceof RubyFixnum &&
            bnum instanceof RubyFixnum && bden instanceof RubyFixnum) {
            long an = ((RubyFixnum)anum).getLongValue();
            long ad = ((RubyFixnum)aden).getLongValue();
            long bn = ((RubyFixnum)bnum).getLongValue();
            long bd = ((RubyFixnum)bden).getLongValue();
            long ig = i_gcd(ad, bd);

            g = RubyFixnum.newFixnum(runtime, ig);
            a = f_imul(context, an, bd / ig);
            b = f_imul(context, bn, ad / ig);
        } else {
            g = f_gcd(context, aden, bden);
            a = f_mul(context, anum, f_idiv(context, bden, g));
            b = f_mul(context, bnum, f_idiv(context, aden, g));
        }

        IRubyObject c = plus ? f_add(context, a, b) : f_sub(context, a, b);

        b = f_idiv(context, aden, g);
        g = f_gcd(context, c, g);
        num = f_idiv(context, c, g);
        a = f_idiv(context, bden, g);
        den = f_mul(context, a, b);
        
        return RubyRational.newRationalNoReduce(context, getMetaClass(), num, den);
    }
    
    /** nurat_add
     * 
     */
    @JRubyMethod(name = "+")
    public IRubyObject op_add(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return f_addsub(context, num, den, other, RubyFixnum.one(context.getRuntime()), true);
        } else if (other instanceof RubyFloat) {
            return f_add(context, f_to_f(context, this), other);
        } else if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            return f_addsub(context, num, den, otherRational.num, otherRational.den, true);
        }            
        return coerceBin(context, "+", other);
    }
    
    /** nurat_sub
     * 
     */
    @JRubyMethod(name = "-")
    public IRubyObject op_sub(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return f_addsub(context, num, den, other, RubyFixnum.one(context.getRuntime()), false);
        } else if (other instanceof RubyFloat) {
            return f_sub(context, f_to_f(context, this), other);
        } else if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            return f_addsub(context, num, den, otherRational.num, otherRational.den, false);
        }
        return coerceBin(context, "-", other);
    }
    
    /** f_muldiv
     * 
     */
    private IRubyObject f_muldiv(ThreadContext context, IRubyObject anum, IRubyObject aden, IRubyObject bnum, IRubyObject bden, boolean mult) {
        if (!mult) {
            if (f_negative_p(context, bnum)) {
                anum = f_negate(context, anum);
                bnum = f_negate(context, bnum);
            }
            IRubyObject tmp =  bnum;
            bnum = bden;
            bden = tmp;
        }
        
        final IRubyObject num, den;
        if (anum instanceof RubyFixnum && aden instanceof RubyFixnum &&
            bnum instanceof RubyFixnum && bden instanceof RubyFixnum) {
            long an = ((RubyFixnum)anum).getLongValue();
            long ad = ((RubyFixnum)aden).getLongValue();
            long bn = ((RubyFixnum)bnum).getLongValue();
            long bd = ((RubyFixnum)bden).getLongValue();
            long g1 = i_gcd(an, bd);
            long g2 = i_gcd(ad, bn);
            
            num = f_imul(context, an / g1, bn / g2);
            den = f_imul(context, ad / g2, bd / g1);
        } else {
            IRubyObject g1 = f_gcd(context, anum, bden); 
            IRubyObject g2 = f_gcd(context, aden, bnum);
            
            num = f_mul(context, f_idiv(context, anum, g1), f_idiv(context, bnum, g2));
            den = f_mul(context, f_idiv(context, aden, g2), f_idiv(context, bden, g1));
        }

        return RubyRational.newRationalNoReduce(context, getMetaClass(), num, den);
    }

    /** nurat_mul
     * 
     */
    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return f_muldiv(context, num, den, other, RubyFixnum.one(context.getRuntime()), true);
        } else if (other instanceof RubyFloat) {
            return f_mul(context, f_to_f(context, this), other);
        } else if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            return f_muldiv(context, num, den, otherRational.num, otherRational.den, true);
        }
        return coerceBin(context, "*", other);
    }
    
    /** nurat_div
     * 
     */
    @JRubyMethod(name = {"/", "quo"})
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            if (f_zero_p(context, other)) throw context.getRuntime().newZeroDivisionError();
            return f_muldiv(context, num, den, other, RubyFixnum.one(context.getRuntime()), false);
        } else if (other instanceof RubyFloat) {
            return f_to_f(context, this).callMethod(context, "/", other);
        } else if (other instanceof RubyRational) {
            if (f_zero_p(context, other)) throw context.getRuntime().newZeroDivisionError();
            RubyRational otherRational = (RubyRational)other;
            return f_muldiv(context, num, den, otherRational.num, otherRational.den, false);
        }
        return coerceBin(context, "/", other);
    }

    /** nurat_fdiv
     * 
     */
    @JRubyMethod(name = "fdiv")
    public IRubyObject op_fdiv(ThreadContext context, IRubyObject other) {
        return f_div(context, f_to_f(context, this), other);
    }

    /** nurat_expt
     * 
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_expt(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();

        if (k_exact_p(other) && f_zero_p(context, other)) {
            return RubyRational.newRationalBang(context, getMetaClass(), RubyFixnum.one(runtime));
        }

        if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            if (f_one_p(context, otherRational.den)) other = otherRational.num;
        }

        if (other instanceof RubyFixnum || other instanceof RubyBignum) {        
            final IRubyObject tnum, tden;
            IRubyObject res = f_cmp(context, other, RubyFixnum.zero(runtime));
            if (res == RubyFixnum.one(runtime)) {
                tnum = f_expt(context, num, other);
                tden = f_expt(context, den, other);
            } else if (res == RubyFixnum.minus_one(runtime)){
                tnum = f_expt(context, den, f_negate(context, other));
                tden = f_expt(context, num, f_negate(context, other));
            } else {
                tnum = tden = RubyFixnum.one(runtime);
            }
            return RubyRational.newRational(context, getMetaClass(), tnum, tden);
        } else if (other instanceof RubyFloat || other instanceof RubyRational) {
            return f_expt(context, f_to_f(context, this), other);
        }
        return coerceBin(context, "**", other);
    }

    
    /** nurat_cmp
     * 
     */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            if (den instanceof RubyFixnum && ((RubyFixnum)den).getLongValue() == 1) return f_cmp(context, num, other);
            return f_cmp(context, this, RubyRational.newRationalBang(context, getMetaClass(), other));
        } else if (other instanceof RubyFloat) {
            return f_cmp(context, f_to_f(context, this), other);
        } else if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            final IRubyObject num1, num2;
            if (num instanceof RubyFixnum && den instanceof RubyFixnum &&
                otherRational.num instanceof RubyFixnum && otherRational.den instanceof RubyFixnum) {
                num1 = f_imul(context, ((RubyFixnum)num).getLongValue(), ((RubyFixnum)otherRational.den).getLongValue());
                num2 = f_imul(context, ((RubyFixnum)otherRational.num).getLongValue(), ((RubyFixnum)den).getLongValue());
            } else {
                num1 = f_mul(context, num, otherRational.den);
                num2 = f_mul(context, otherRational.num, den);
            }
            return f_cmp(context, f_sub(context, num1, num2), RubyFixnum.zero(context.getRuntime()));
        }
        return coerceBin(context, "<=>", other);             
    }

    /** nurat_equal_p
     * 
     */
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            if (f_zero_p(context, num) && f_zero_p(context, other)) return runtime.getTrue();
            if (!(den instanceof RubyFixnum) || ((RubyFixnum)den).getLongValue() != 1) return runtime.getFalse();
            if (f_equal_p(context, num, other)) return runtime.getTrue();
            return runtime.getFalse();
        } else if (other instanceof RubyFloat) {
            return f_equal_p(context, f_to_f(context, this), other) ? runtime.getTrue() : runtime.getFalse();
        } else if (other instanceof RubyRational) {
            RubyRational otherRational = (RubyRational)other;
            if (f_zero_p(context, num) && f_zero_p(context, otherRational.num)) return runtime.getTrue();
            if (f_equal_p(context, num, otherRational.num) && f_equal_p(context, den, otherRational.den)) return runtime.getTrue();
            return runtime.getFalse();
        }
        return f_equal_p(context, other, this) ? runtime.getTrue() : runtime.getFalse();
    }

    /** nurat_coerce
     * 
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject op_coerce(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return runtime.newArray(RubyRational.newRationalBang(context, getMetaClass(), other), this);
        } else if (other instanceof RubyFloat) {
            return runtime.newArray(other, f_to_f(context, this));
        }
        throw runtime.newTypeError(other.getMetaClass() + " can't be coerced into " + getMetaClass());
    }

    /** nurat_idiv
     * 
     */
    @JRubyMethod(name = "div")
    public IRubyObject op_idiv(ThreadContext context, IRubyObject other) {
        return f_floor(context, f_div(context, this, other));
    }

    /** nurat_mod
     * 
     */
    @JRubyMethod(name = {"modulo", "%"})
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        IRubyObject val = f_floor(context, f_div(context, this, other));
        return f_sub(context, this, f_mul(context, other, val));
    }

    /** nurat_divmod
     * 
     */
    @JRubyMethod(name = "divmod")
    public IRubyObject op_divmod(ThreadContext context, IRubyObject other) {
        IRubyObject val = f_floor(context, f_div(context, this, other)); 
        return context.getRuntime().newArray(val, f_sub(context, this, f_mul(context, other, val)));
    }

    /** nurat_rem
     * 
     */
    @JRubyMethod(name = "remainder")
    public IRubyObject op_rem(ThreadContext context, IRubyObject other) {
        IRubyObject val = f_truncate(context, f_div(context, this, other));
        return f_sub(context, this, f_mul(context, other, val));
    }

    /** nurat_abs
     * 
     */
    @JRubyMethod(name = "abs")
    public IRubyObject op_abs(ThreadContext context) {
        if (!f_negative_p(context, this)) return this;
        return f_negate(context, this);
    }

    private IRubyObject op_roundCommonPre(ThreadContext context, IRubyObject n) {
        Ruby runtime = context.getRuntime();
        if (!(n instanceof RubyInteger)) throw runtime.newTypeError("not an integer");
        return f_expt(context, RubyFixnum.newFixnum(runtime, 10), n);
    }

    private IRubyObject op_roundCommonPost(ThreadContext context, IRubyObject s, IRubyObject n, IRubyObject b) {
        s = f_div(context, newRationalBang(context, getMetaClass(), s), b);
        if (f_lt_p(context, n, RubyFixnum.one(context.getRuntime())).isTrue()) s = f_to_i(context, s);
        return s;
    }

    /** nurat_floor
     * 
     */
    @JRubyMethod(name = "floor")
    public IRubyObject op_floor(ThreadContext context) {
        return f_idiv(context, num, den);
    }

    @JRubyMethod(name = "floor")
    public IRubyObject op_floor(ThreadContext context, IRubyObject n) {
        IRubyObject b = op_roundCommonPre(context, n);
        return op_roundCommonPost(context, ((RubyRational)f_mul(context, this, b)).op_floor(context), n, b);
    }

    /** nurat_ceil
     * 
     */
    @JRubyMethod(name = "ceil")
    public IRubyObject op_ceil(ThreadContext context) {
        return f_negate(context, f_idiv(context, f_negate(context, num), den));
    }

    @JRubyMethod(name = "ceil")
    public IRubyObject op_ceil(ThreadContext context, IRubyObject n) {
        IRubyObject b = op_roundCommonPre(context, n);
        return op_roundCommonPost(context, ((RubyRational)f_mul(context, this, b)).op_ceil(context), n, b);
    }

    /** nurat_truncate
     * 
     */
    @JRubyMethod(name = {"truncate", "to_i"})
    public IRubyObject op_truncate(ThreadContext context) {
        if (f_negative_p(context, num)) {
            return f_negate(context, f_idiv(context, f_negate(context, num), den));
        }
        return f_idiv(context, num, den);
    }

    @JRubyMethod(name = "truncate")
    public IRubyObject op_truncate(ThreadContext context, IRubyObject n) {
        IRubyObject b = op_roundCommonPre(context, n);
        return op_roundCommonPost(context, ((RubyRational)f_mul(context, this, b)).op_truncate(context), n, b);
    }

    /** nurat_round
     * 
     */
    @JRubyMethod(name = "round")
    public IRubyObject op_round(ThreadContext context) {
        IRubyObject num = this.num;
        boolean neg = f_negative_p(context, num);
        if (neg) num = f_negate(context, num);

        IRubyObject den = this.den;
        IRubyObject two = RubyFixnum.two(context.getRuntime());
        num = f_add(context, f_mul(context, num, two), den);
        den = f_mul(context, den, two);
        num = f_idiv(context, num, den);

        if (neg) num = f_negate(context, num);
        return num;
    }

    @JRubyMethod(name = "round")
    public IRubyObject op_round(ThreadContext context, IRubyObject n) {
        IRubyObject b = op_roundCommonPre(context, n);
        return op_roundCommonPost(context, ((RubyRational)f_mul(context, this, b)).op_round(context), n, b);
    }

    /** nurat_to_f
     * 
     */
    private static long ML = (long)(Math.log(Double.MAX_VALUE) / Math.log(2.0) - 1);
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        if (f_zero_p(context, num)) return runtime.newFloat(0);

        IRubyObject num = this.num;
        IRubyObject den = this.den;

        boolean minus = false;
        if (f_negative_p(context, num)) {
            num = f_negate(context, num);
            minus = true;
        }

        long nl = i_ilog2(context, num);
        long dl = i_ilog2(context, den);

        long ne = 0;
        if (nl > ML) {
            ne = nl - ML;
            num = f_rshift(context, num, RubyFixnum.newFixnum(runtime, ne));
        }

        long de = 0;
        if (dl > ML) {
            de = dl - ML;
            den = f_rshift(context, den, RubyFixnum.newFixnum(runtime, de));
        }

        long e = ne - de;

        if (e > 1023 || e < -1022) {
            runtime.getWarnings().warn(IRubyWarnings.ID.FLOAT_OUT_OF_RANGE, "out of Float range", getMetaClass());
            return runtime.newFloat(e > 0 ? Double.MAX_VALUE : 0);
        }

        double f = RubyNumeric.num2dbl(num) / RubyNumeric.num2dbl(den); 

        if (minus) f = -f;

        f = ldexp(f, e);

        if (Double.isInfinite(f) || Double.isNaN(f)) {
            runtime.getWarnings().warn(IRubyWarnings.ID.FLOAT_OUT_OF_RANGE, "out of Float range", getMetaClass());
        }

        return runtime.newFloat(f);
    }

    /** nurat_to_r
     * 
     */
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        return this;
    }
    
    /** nurat_to_r
     * 
     */
    @JRubyMethod(name = "hash")
    public IRubyObject hash(ThreadContext context) {
        return f_xor(context, num.callMethod(context, "hash"), den.callMethod(context, "hash"));
    }

    /** nurat_to_s
     * 
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        RubyString str = context.getRuntime().newString();
        str.append(f_to_s(context, num));
        str.cat((byte)'/');
        str.append(f_to_s(context, den));
        return str;
    }

    /** nurat_inspect
     * 
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        RubyString str = context.getRuntime().newString();
        str.cat((byte)'(');
        str.append(f_inspect(context, num));
        str.cat((byte)'/');
        str.append(f_inspect(context, den));
        str.cat((byte)')');
        return str;
    }

    /** nurat_marshal_dump
     * 
     */
    @JRubyMethod(name = "marshal_dump")
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyArray dump = context.getRuntime().newArray(num, den);
        if (hasVariables()) dump.syncVariables(getVariableList());
        return dump;
    }

    /** nurat_marshal_load
     * 
     */
    @JRubyMethod(name = "marshal_load")
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        num = load.size() > 0 ? load.eltInternal(0) : context.getRuntime().getNil();
        den = load.size() > 1 ? load.eltInternal(1) : context.getRuntime().getNil();

        if (f_zero_p(context, den)) throw context.getRuntime().newZeroDivisionError();
        if (load.hasVariables()) syncVariables(load.getVariableList());
        return this;
    }

    static RubyArray str_to_r_internal(ThreadContext context, IRubyObject recv) {
        RubyString s = recv.convertToString();
        ByteList bytes = s.getByteList();

        Ruby runtime = context.getRuntime();
        if (bytes.realSize == 0) return runtime.newArray(runtime.getNil(), recv);

        IRubyObject m = RubyRegexp.newDummyRegexp(runtime, Numeric.RationalPatterns.rat_pat).callMethod(context, "match", s);
        
        if (!m.isNil()) {
            IRubyObject si = m.callMethod(context, "[]", RubyFixnum.one(runtime));
            IRubyObject nu = m.callMethod(context, "[]", RubyFixnum.two(runtime));
            IRubyObject de = m.callMethod(context, "[]", RubyFixnum.three(runtime));
            IRubyObject re = m.callMethod(context, "post_match");
            
            RubyArray a = nu.callMethod(context, "split", RubyRegexp.newDummyRegexp(runtime, Numeric.RationalPatterns.an_e_pat)).convertToArray();
            IRubyObject ifp = a.eltInternal(0);
            IRubyObject exp = a.size() != 2 ? runtime.getNil() : a.eltInternal(1);
            
            a = ifp.callMethod(context, "split", RubyRegexp.newDummyRegexp(runtime, Numeric.RationalPatterns.a_dot_pat)).convertToArray();
            IRubyObject ip = a.eltInternal(0);
            IRubyObject fp = a.size() != 2 ? runtime.getNil() : a.eltInternal(1);
            
            IRubyObject v = RubyRational.newRationalCanonicalize(context, f_to_i(context, ip));
            
            if (!fp.isNil()) {
                bytes = fp.convertToString().getByteList();
                int count = 0;
                byte[]buf = bytes.bytes;
                int i = bytes.begin;
                int end = i + bytes.realSize;

                while (i < end) {
                    if (ASCIIEncoding.INSTANCE.isDigit(buf[i])) count++;
                    i++;
                }

                IRubyObject l = f_expt(context, RubyFixnum.newFixnum(runtime, 10), RubyFixnum.newFixnum(runtime, count));
                v = f_mul(context, v, l);
                v = f_add(context, v, f_to_i(context, fp));
                v = f_div(context, v, l);
            }

            if (!si.isNil()) {
                ByteList siBytes = si.convertToString().getByteList();
                if (siBytes.length() > 0 && siBytes.get(0) == '-') v = f_negate(context, v); 
            }

            if (!exp.isNil()) {
                v = f_mul(context, v, f_expt(context, RubyFixnum.newFixnum(runtime, 10), f_to_i(context, exp)));
            }

            if (!de.isNil()) {
                v = f_div(context, v, f_to_i(context, de));
            }
            return runtime.newArray(v, re);
        }
        return runtime.newArray(runtime.getNil(), recv);
    }
    
    private static IRubyObject str_to_r_strict(ThreadContext context, IRubyObject recv) {
        RubyArray a = str_to_r_internal(context, recv);
        if (a.eltInternal(0).isNil() || a.eltInternal(1).convertToString().getByteList().length() > 0) {
            IRubyObject s = recv.callMethod(context, "inspect");
            throw context.getRuntime().newArgumentError("invalid value for convert(): " + s.convertToString());
        }
        return a.eltInternal(0);
    }    
}
