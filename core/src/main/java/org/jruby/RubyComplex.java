/***** BEGIN LICENSE BLOCK *****
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

import static org.jruby.util.Numeric.f_abs;
import static org.jruby.util.Numeric.f_abs2;
import static org.jruby.util.Numeric.f_add;
import static org.jruby.util.Numeric.f_arg;
import static org.jruby.util.Numeric.f_conjugate;
import static org.jruby.util.Numeric.f_denominator;
import static org.jruby.util.Numeric.f_div;
import static org.jruby.util.Numeric.f_divmod;
import static org.jruby.util.Numeric.f_equal;
import static org.jruby.util.Numeric.f_exact_p;
import static org.jruby.util.Numeric.f_expt;
import static org.jruby.util.Numeric.f_gt_p;
import static org.jruby.util.Numeric.f_inspect;
import static org.jruby.util.Numeric.f_lcm;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negate;
import static org.jruby.util.Numeric.f_negative_p;
import static org.jruby.util.Numeric.f_numerator;
import static org.jruby.util.Numeric.f_one_p;
import static org.jruby.util.Numeric.f_polar;
import static org.jruby.util.Numeric.f_quo;
import static org.jruby.util.Numeric.f_real_p;
import static org.jruby.util.Numeric.f_sub;
import static org.jruby.util.Numeric.f_to_f;
import static org.jruby.util.Numeric.f_to_i;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.Numeric.f_to_s;
import static org.jruby.util.Numeric.f_xor;
import static org.jruby.util.Numeric.f_zero_p;
import static org.jruby.util.Numeric.k_exact_p;
import static org.jruby.util.Numeric.k_inexact_p;

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.util.Numeric.safe_mul;

/**
 *  1.9 complex.c as of revision: 20011
 */

@JRubyClass(name = "Complex", parent = "Numeric")
public class RubyComplex extends RubyNumeric {

    private static final String[] UNDEFINED = new String[]{
            "<", "<=", "<=>", ">", ">=", "between?", "divmod", "floor", "ceil", "modulo", "round", "step",
            "truncate", "positive?", "negative?"
    };

    public static RubyClass createComplexClass(Ruby runtime) {
        RubyClass complexc = runtime.defineClass("Complex", runtime.getNumeric(), COMPLEX_ALLOCATOR);
        runtime.setComplex(complexc);

        complexc.setClassIndex(ClassIndex.COMPLEX);
        complexc.setReifiedClass(RubyComplex.class);
        
        complexc.kindOf = new RubyModule.JavaClassKindOf(RubyComplex.class);

        complexc.defineAnnotatedMethods(RubyComplex.class);

        complexc.getSingletonClass().undefineMethod("allocate");
        complexc.getSingletonClass().undefineMethod("new");

        for (String undef : UNDEFINED) {
            complexc.undefineMethod(undef);
        }

        complexc.defineConstant("I", RubyComplex.newComplexConvert(runtime.getCurrentContext(), RubyFixnum.zero(runtime), RubyFixnum.one(runtime)));

        return complexc;
    }

    private static ObjectAllocator COMPLEX_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyFixnum zero = RubyFixnum.zero(runtime);
            return new RubyComplex(runtime, klass, zero, zero);
        }
    };

    /** internal
     * 
     */
    private RubyComplex(Ruby runtime, IRubyObject clazz, IRubyObject real, IRubyObject image) {
        super(runtime, (RubyClass)clazz);
        this.real = real;
        this.image = image;
    }

    /** rb_complex_raw
     * 
     */
    public static RubyComplex newComplexRaw(Ruby runtime, IRubyObject x, IRubyObject y) {
        return new RubyComplex(runtime, runtime.getComplex(), x, y);
    }

    /** rb_complex_raw1
     * 
     */
    public static RubyComplex newComplexRaw(Ruby runtime, IRubyObject x) {
        return new RubyComplex(runtime, runtime.getComplex(), x, RubyFixnum.zero(runtime));
    }

    /** rb_complex_new1
     * 
     */
    public static IRubyObject newComplexCanonicalize(ThreadContext context, IRubyObject x) {
        return newComplexCanonicalize(context, x, RubyFixnum.zero(context.runtime));
    }
    
    /** rb_complex_new
     * 
     */
    public static IRubyObject newComplexCanonicalize(ThreadContext context, IRubyObject x, IRubyObject y) {
        return canonicalizeInternal(context, context.runtime.getComplex(), x, y);
    }

    /** rb_complex_polar
     * 
     */
    static IRubyObject newComplexPolar(ThreadContext context, IRubyObject x, IRubyObject y) {
        return f_complex_polar(context, context.runtime.getComplex(), x, y);
    }

    /** f_complex_new1
     * 
     */
    static IRubyObject newComplex(ThreadContext context, IRubyObject clazz, IRubyObject x) {
        return newComplex(context, clazz, x, RubyFixnum.zero(context.runtime));
    }

    /** f_complex_new2
     * 
     */
    static IRubyObject newComplex(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyComplex);
        return canonicalizeInternal(context, clazz, x, y);
    }
    
    /** f_complex_new_bang2
     * 
     */
    static RubyComplex newComplexBang(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) {
// FIXME: what should these really be? Numeric?       assert x instanceof RubyComplex && y instanceof RubyComplex;
        return new RubyComplex(context.runtime, clazz, x, y);
    }

    /** f_complex_new_bang1
     * 
     */
    public static RubyComplex newComplexBang(ThreadContext context, IRubyObject clazz, IRubyObject x) {
// FIXME: what should this really be?       assert x instanceof RubyComplex;
        return newComplexBang(context, clazz, x, RubyFixnum.zero(context.runtime));
    }

    private IRubyObject real;
    private IRubyObject image;
    
    IRubyObject getImage() {
        return image;
    }

    IRubyObject getReal() {
        return real;
    }

    /** m_cos
     * 
     */
    private static IRubyObject m_cos(ThreadContext context, IRubyObject x) {
        if (f_real_p(context, x).isTrue()) return RubyMath.cos(context, x, x);
        RubyComplex complex = (RubyComplex)x;
        return newComplex(context, context.runtime.getComplex(),
                          f_mul(context, RubyMath.cos(context, x, complex.real), RubyMath.cosh(context, x, complex.image)),
                          f_mul(context, f_negate(context, RubyMath.sin(context, x, complex.real)), RubyMath.sinh(context, x, complex.image)));
    }

    /** m_sin
     * 
     */
    private static IRubyObject m_sin(ThreadContext context, IRubyObject x) {
        if (f_real_p(context, x).isTrue()) return RubyMath.sin(context, x, x);
        RubyComplex complex = (RubyComplex)x;
        return newComplex(context, context.runtime.getComplex(),
                          f_mul(context, RubyMath.sin(context, x, complex.real), RubyMath.cosh(context, x, complex.image)),
                          f_mul(context, RubyMath.cos(context, x, complex.real), RubyMath.sinh(context, x, complex.image)));
    }    
    
    /** m_sqrt
     * 
     */
    private static IRubyObject m_sqrt(ThreadContext context, IRubyObject x) {
        if (f_real_p(context, x).isTrue()) {
            if (!f_negative_p(context, x)) return RubyMath.sqrt(context, x, x);
            return newComplex(context, context.runtime.getComplex(),
                              RubyFixnum.zero(context.runtime),
                              RubyMath.sqrt(context, x, f_negate(context, x)));
        } else {
            RubyComplex complex = (RubyComplex)x;
            if (f_negative_p(context, complex.image)) {
                return f_conjugate(context, m_sqrt(context, f_conjugate(context, x)));
            } else {
                IRubyObject a = f_abs(context, x);
                IRubyObject two = RubyFixnum.two(context.runtime);
                return newComplex(context, context.runtime.getComplex(),
                                  RubyMath.sqrt(context, x, f_div(context, f_add(context, a, complex.real), two)),
                                  RubyMath.sqrt(context, x, f_div(context, f_sub(context, a, complex.real), two)));
            }
        }
    }

    /** nucomp_s_new_bang
     *
     */
    @Deprecated
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject[]args) {
        switch (args.length) {
        case 1: return newInstanceBang(context, recv, args[0]);
        case 2: return newInstanceBang(context, recv, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.runtime, args.length, 1, 1);
        return null;
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        return new RubyComplex(context.runtime, recv, real, RubyFixnum.zero(context.runtime));
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        if (!(image instanceof RubyNumeric)) image = f_to_i(context, image);
        return new RubyComplex(context.runtime, recv, real, image);
    }

    /** nucomp_canonicalization
     * 
     */
    private static boolean canonicalization = false;
    public static void setCanonicalization(boolean canonical) {
        canonicalization = canonical;
    }

    /** nucomp_real_check (might go to bimorphic)
     * 
     */
    private static void realCheck(ThreadContext context, IRubyObject num) {
        switch (num.getMetaClass().getClassIndex()) {
        case FIXNUM:
        case BIGNUM:
        case FLOAT:
        case RATIONAL:
            break;
        default:
             if (!(num instanceof RubyNumeric ) || !f_real_p(context, num).isTrue()) {
                 throw context.runtime.newTypeError("not a real");
             }
        }
    }

    /** nucomp_s_canonicalize_internal
     * 
     */
    private static final boolean CL_CANON = Numeric.CANON;
    private static IRubyObject canonicalizeInternal(ThreadContext context, IRubyObject clazz, IRubyObject real, IRubyObject image) {
        if (Numeric.CANON) {
            if (f_zero_p(context, image) &&
                    (!CL_CANON || k_exact_p(image)) &&
                    canonicalization)
                    return real;
        }
        if (f_real_p(context, real).isTrue() &&
                   f_real_p(context, image).isTrue()) {
            return new RubyComplex(context.runtime, clazz, real, image);
        } else if (f_real_p(context, real).isTrue()) {
            RubyComplex complex = (RubyComplex)image;
            return new RubyComplex(context.runtime, clazz,
                                   f_sub(context, real, complex.image),
                                   f_add(context, RubyFixnum.zero(context.runtime), complex.real));
        } else if (f_real_p(context, image).isTrue()) {
            RubyComplex complex = (RubyComplex)real;
            return new RubyComplex(context.runtime, clazz,
                                   complex.real,
                                   f_add(context, complex.image, image));
        } else {
            RubyComplex complex1 = (RubyComplex)real;
            RubyComplex complex2 = (RubyComplex)image;
            return new RubyComplex(context.runtime, clazz,
                                   f_sub(context, complex1.real, complex2.image),
                                   f_add(context, complex1.image, complex2.real));
        }
    }
    
    /** nucomp_s_new
     * 
     */
    @Deprecated
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[]args) {
        switch (args.length) {
        case 1: return newInstance(context, recv, args[0]);
        case 2: return newInstance(context, recv, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.runtime, args.length, 1, 1);
        return null;
    }

    // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceNew(ThreadContext context, IRubyObject recv, IRubyObject real) {
        return newInstance(context, recv, real);
    }

    @JRubyMethod(name = {"rect", "rectangular"}, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real) {
        realCheck(context, real);
        return canonicalizeInternal(context, recv, real, RubyFixnum.zero(context.runtime));
    }

    // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceNew(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        return newInstance(context, recv, real, image);
    }

    @JRubyMethod(name = {"rect", "rectangular"}, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        realCheck(context, real);
        realCheck(context, image);
        return canonicalizeInternal(context, recv, real, image);
    }

    /** f_complex_polar
     * 
     */
    private static IRubyObject f_complex_polar(ThreadContext context, IRubyObject clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyComplex) && !(y instanceof RubyComplex);
        return canonicalizeInternal(context, clazz,
                                             f_mul(context, x, m_cos(context, y)),
                                             f_mul(context, x, m_sin(context, y)));
    }

    /** nucomp_s_polar
     * 
     */
    public static IRubyObject polar(ThreadContext context, IRubyObject clazz, IRubyObject abs, IRubyObject arg) {
        return polar19(context, clazz, new IRubyObject[]{abs, arg});
    }

    /** nucomp_s_polar
     *
     */
    @JRubyMethod(name = "polar", meta = true, required = 1, optional = 1)
    public static IRubyObject polar19(ThreadContext context, IRubyObject clazz, IRubyObject[] args) {
        IRubyObject abs = args[0];
        IRubyObject arg;
        if (args.length < 2) {
            arg = RubyFixnum.zero(context.runtime);
        } else {
            arg = args[1];
        }
        realCheck(context, abs);
        realCheck(context, arg);
        return f_complex_polar(context, clazz, abs, arg);
    }

    /** rb_Complex1
     * 
     */
    public static IRubyObject newComplexConvert(ThreadContext context, IRubyObject x) {
        return newComplexConvert(context, x, RubyFixnum.zero(context.runtime));
    }

    /** rb_Complex/rb_Complex2
     * 
     */
    public static IRubyObject newComplexConvert(ThreadContext context, IRubyObject x, IRubyObject y) {
        return convert(context, context.runtime.getComplex(), x, y);
    }

    @Deprecated
    public static IRubyObject convert(ThreadContext context, IRubyObject clazz, IRubyObject[]args) {
        switch (args.length) {
        case 1: return convert(context, clazz, args[0]);        
        case 2: return convert(context, clazz, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.runtime, args.length, 1, 1);
        return null;
    }

    /** nucomp_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1) {
        return convertCommon(context, recv, a1, context.runtime.getNil());
    }

    /** nucomp_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        return convertCommon(context, recv, a1, a2);
    }
    
    private static IRubyObject convertCommon(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        if (a1 instanceof RubyString) a1 = str_to_c_strict(context, a1);
        if (a2 instanceof RubyString) a2 = str_to_c_strict(context, a2);

        if (a1 instanceof RubyComplex) {
            RubyComplex a1Complex = (RubyComplex)a1;
            if (k_exact_p(a1Complex.image) && f_zero_p(context, a1Complex.image)) {
                a1 = a1Complex.real;
            }
        }

        if (a2 instanceof RubyComplex) {
            RubyComplex a2Complex = (RubyComplex)a2;
            if (k_exact_p(a2Complex.image) && f_zero_p(context, a2Complex.image)) {
                a2 = a2Complex.real;
            }
        }

        if (a1 instanceof RubyComplex) {
            if (a2.isNil() || (k_exact_p(a2) && f_zero_p(context, a2))) return a1;
        }

        if (a2.isNil()) {
            if (a1 instanceof RubyNumeric && !f_real_p(context, a1).isTrue()) return a1;
            return newInstance(context, recv, a1);
        } else {
            if (a1 instanceof RubyNumeric && a2 instanceof RubyNumeric &&
                (!f_real_p(context, a1).isTrue() || !f_real_p(context, a2).isTrue())) {
                Ruby runtime = context.runtime;
                return f_add(context, a1,
                             f_mul(context, a2, newComplexBang(context, runtime.getComplex(),
                                     RubyFixnum.zero(runtime), RubyFixnum.one(runtime))));
            }
            return newInstance(context, recv, a1, a2);
        }
    }

    /** nucomp_real
     * 
     */
    @JRubyMethod(name = "real")
    public IRubyObject real() {
        return real;
    }
    
    /** nucomp_image
     * 
     */
    @JRubyMethod(name = {"imaginary", "imag"})
    public IRubyObject image() {
        return image;
    }

    /** nucomp_negate
     * 
     */
    @JRubyMethod(name = "-@")
    public IRubyObject negate(ThreadContext context) {
        return newComplex(context, getMetaClass(), f_negate(context, real), f_negate(context, image));
    }

    /** nucomp_add
     * 
     */
    @JRubyMethod(name = "+")
    public IRubyObject op_add(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            return newComplex(context, getMetaClass(), 
                              f_add(context, real, otherComplex.real),
                              f_add(context, image, otherComplex.image));
        } else if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            return newComplex(context, getMetaClass(), f_add(context, real, other), image);
        }
        return coerceBin(context, sites(context).op_plus, other);
    }

    /** nucomp_sub
     * 
     */
    @JRubyMethod(name = "-")
    public IRubyObject op_sub(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            return newComplex(context, getMetaClass(), 
                              f_sub(context, real, otherComplex.real),
                              f_sub(context, image, otherComplex.image));
        } else if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            return newComplex(context, getMetaClass(), f_sub(context, real, other), image);
        }
        return coerceBin(context, sites(context).op_minus, other);
    }

    /** nucomp_mul
     * 
     */
    @JRubyMethod(name = "*")
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            boolean arzero = f_zero_p(context, real);
            boolean aizero = f_zero_p(context, image);
            boolean brzero = f_zero_p(context, otherComplex.real);
            boolean bizero = f_zero_p(context, otherComplex.image);
            IRubyObject realp = f_sub(context, 
                                safe_mul(context, real, otherComplex.real, arzero, brzero),
                                safe_mul(context, image, otherComplex.image, aizero, bizero));
            IRubyObject imagep = f_add(context,
                                safe_mul(context, real, otherComplex.image, arzero, bizero),
                                safe_mul(context, image, otherComplex.real, aizero, brzero));
            
            return newComplex(context, getMetaClass(), realp, imagep); 
        } else if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            return newComplex(context, getMetaClass(),
                    f_mul(context, real, other),
                    f_mul(context, image, other));
        }
        return coerceBin(context, sites(context).op_times, other);
    }
    
    /** nucomp_div / nucomp_quo
     * 
     */
    @JRubyMethod(name = {"/", "quo"})
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            if (real instanceof RubyFloat || image instanceof RubyFloat ||
                otherComplex.real instanceof RubyFloat || otherComplex.image instanceof RubyFloat) {
                IRubyObject magn = RubyMath.hypot(context, this, otherComplex.real, otherComplex.image);
                IRubyObject tmp = newComplexBang(context, getMetaClass(),
                                                 f_quo(context, otherComplex.real, magn),
                                                 f_quo(context, otherComplex.image, magn));
                return f_quo(context, f_mul(context, this, f_conjugate(context, tmp)), magn);
            }
            return f_quo(context, f_mul(context, this, f_conjugate(context, other)), f_abs2(context, other));
        } else if (other instanceof RubyNumeric) {
            if (f_real_p(context, other).isTrue()) {
                return newComplex(context, getMetaClass(),
                        f_quo(context, real, other),
                        f_quo(context, image, other));
            } else {
                RubyArray coercedOther = doCoerce(context, other, true);
                return RubyRational.newInstance(context, context.runtime.getRational(), coercedOther.first(), coercedOther.last());
            }
        }
        return coerceBin(context, sites(context).op_quo, other);
    }

    /** nucomp_fdiv
     *
     */
    @JRubyMethod(name = "fdiv")
    @Override
    public IRubyObject fdiv(ThreadContext context, IRubyObject other) {
        IRubyObject complex = newComplex(context, getMetaClass(),
                                         f_to_f(context, real),   
                                         f_to_f(context, image));

        return f_div(context, complex, other);
    }

    /** nucomp_expt
     * 
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_expt(ThreadContext context, IRubyObject other) {
        if (k_exact_p(other) && f_zero_p(context, other)) {
            return newComplexBang(context, getMetaClass(), RubyFixnum.one(context.runtime));
        } else if (other instanceof RubyRational && f_one_p(context, f_denominator(context, other))) {
            other = f_numerator(context, other); 
        } 

        if (other instanceof RubyComplex) {
            RubyArray a = f_polar(context, this).convertToArray();
            IRubyObject r = a.eltInternal(0);
            IRubyObject theta = a.eltInternal(1);
            RubyComplex otherComplex = (RubyComplex)other;
            IRubyObject nr = RubyMath.exp(context, this, f_sub(context, 
                    f_mul(context, otherComplex.real, RubyMath.log_19(context, this, new IRubyObject[] {r})),
                    f_mul(context, otherComplex.image, theta)));
            IRubyObject ntheta = f_add(context,
                    f_mul(context, theta, otherComplex.real),
                    f_mul(context, otherComplex.image, RubyMath.log_19(context, this, new IRubyObject[] {r})));
            return f_complex_polar(context, getMetaClass(), nr, ntheta);
        } else if (other instanceof RubyInteger) {
            IRubyObject one = RubyFixnum.one(context.runtime);
            if (f_gt_p(context, other, RubyFixnum.zero(context.runtime)).isTrue()) {
                IRubyObject x = this;
                IRubyObject z = x;
                IRubyObject n = f_sub(context, other, one);

                IRubyObject two = RubyFixnum.two(context.runtime);
                
                while (!f_zero_p(context, n)) {
                    
                    RubyArray a = f_divmod(context, n, two).convertToArray();

                    while (f_zero_p(context, a.eltInternal(1))) {
                        RubyComplex xComplex = (RubyComplex)x;
                        x = newComplex(context, getMetaClass(),
                                       f_sub(context, f_mul(context, xComplex.real, xComplex.real),
                                                      f_mul(context, xComplex.image, xComplex.image)),
                                       f_mul(context, f_mul(context, two, xComplex.real), xComplex.image));
                        
                        n = a.eltInternal(0);
                        a = f_divmod(context, n, two).convertToArray();
                    }
                    z = f_mul(context, z, x);
                    n = f_sub(context, n, one);
                }
                return z;
            }
            return f_expt(context, f_div(context, f_to_r(context, one), this), f_negate(context, other));
        } else if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            RubyArray a = f_polar(context, this).convertToArray();
            IRubyObject r = a.eltInternal(0);
            IRubyObject theta = a.eltInternal(1);
            return f_complex_polar(context, getMetaClass(), f_expt(context, r, other), f_mul(context, theta, other));
        }
        return coerceBin(context, sites(context).op_exp, other);
    }

    /** nucomp_equal_p
     * 
     */
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex) other;
            boolean test = f_equal(context, real, otherComplex.real).isTrue() &&
                    f_equal(context, image, otherComplex.image).isTrue();

            return context.runtime.newBoolean(test);
        }

        if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            boolean test = f_equal(context, real, other).isTrue() && f_zero_p(context, image);

            return context.runtime.newBoolean(test);
        }
        
        return f_equal(context, other, this);
    }

    /** nucomp_coerce 
     * 
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric && f_real_p(context, other).isTrue()) {
            return context.runtime.newArray(newComplexBang(context, getMetaClass(), other), this);
        }
        if (other instanceof RubyComplex) {
            return context.runtime.newArray(other, this);
        }
        throw context.runtime.newTypeError(other.getMetaClass().getName() + " can't be coerced into " + getMetaClass().getName());
    }

    /** nucomp_abs 
     * 
     */
    @JRubyMethod(name = {"abs", "magnitude"})
    @Override
    public IRubyObject abs(ThreadContext context) {
        return RubyMath.hypot(context, this, real, image);
    }

    /** nucomp_abs2 
     * 
     */
    @JRubyMethod(name = "abs2")
    @Override
    public IRubyObject abs2(ThreadContext context) {
        return f_add(context,
                     f_mul(context, real, real),
                     f_mul(context, image, image));
    }

    /** nucomp_arg 
     * 
     */
    @JRubyMethod(name = {"arg", "angle", "phase"})
    @Override
    public IRubyObject arg(ThreadContext context) {
        return RubyMath.atan2(context, this, image, real);
    }

    /** nucomp_rect
     * 
     */
    @JRubyMethod(name = {"rectangular", "rect"})
    @Override
    public IRubyObject rect(ThreadContext context) {
        return context.runtime.newArray(real, image);
    }

    /** nucomp_polar 
     * 
     */
    @JRubyMethod(name = "polar")
    @Override
    public IRubyObject polar(ThreadContext context) {
        return context.runtime.newArray(f_abs(context, this), f_arg(context, this));
    }

    /** nucomp_conjugate
     * 
     */
    @JRubyMethod(name = {"conjugate", "conj", "~"})
    @Override
    public IRubyObject conjugate(ThreadContext context) {
        return newComplex(context, getMetaClass(), real, f_negate(context, image));
    }

    /** nucomp_real_p
     * 
     */
    @JRubyMethod(name = "real?")
    public IRubyObject real_p(ThreadContext context) {
        return context.runtime.getFalse();
    }

    /** nucomp_complex_p
     * 
     */
    // @JRubyMethod(name = "complex?")
    public IRubyObject complex_p(ThreadContext context) {
        return context.runtime.getTrue();
    }

    /** nucomp_exact_p
     * 
     */
    // @JRubyMethod(name = "exact?")
    public IRubyObject exact_p(ThreadContext context) {
        return (f_exact_p(context, real).isTrue() && f_exact_p(context, image).isTrue()) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    /** nucomp_exact_p
     * 
     */
    // @JRubyMethod(name = "inexact?")
    public IRubyObject inexact_p(ThreadContext context) {
        return exact_p(context).isTrue() ? context.runtime.getFalse() : context.runtime.getTrue();
    }

    /** nucomp_denominator
     * 
     */
    @JRubyMethod(name = "denominator")
    public IRubyObject demoninator(ThreadContext context) {
        return f_lcm(context, f_denominator(context, real), f_denominator(context, image));
    }

    /** nucomp_numerator
     * 
     */
    @JRubyMethod(name = "numerator")
    @Override
    public IRubyObject numerator(ThreadContext context) {
        IRubyObject cd = callMethod(context, "denominator");
        return newComplex(context, getMetaClass(),
                          f_mul(context, 
                                f_numerator(context, real),
                                f_div(context, cd, f_denominator(context, real))),
                          f_mul(context,
                                f_numerator(context, image),
                                f_div(context, cd, f_denominator(context, image))));
    }

    /** nucomp_hash
     * 
     */
    @JRubyMethod(name = "hash")
    public IRubyObject hash(ThreadContext context) {
        return f_xor(context, invokedynamic(context, real, HASH), invokedynamic(context, image, HASH));
    }

    /** nucomp_eql_p
     * 
     */
    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            if (real.getMetaClass() == otherComplex.real.getMetaClass() &&
                image.getMetaClass() == otherComplex.image.getMetaClass() &&
                f_equal(context, this, otherComplex).isTrue()) {
                return context.runtime.getTrue();
            }
        }
        return context.runtime.getFalse();
    }

    /** f_signbit
     * 
     */
    private static boolean signbit(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFloat) {
            double value = ((RubyFloat)x).getDoubleValue();
            return !Double.isNaN(value) && Double.doubleToLongBits(value) < 0;
        }
        return f_negative_p(context, x);
    }

    /** f_tpositive_p
     * 
     */
    private static boolean tpositive_p(ThreadContext context, IRubyObject x) {
        return !signbit(context, x);
    }

    /** nucomp_to_s
     * 
     */
    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(ThreadContext context) {
        boolean impos = tpositive_p(context, image);

        RubyString str = f_to_s(context, real).convertToString();
        str.cat(impos ? (byte)'+' : (byte)'-');
        str.cat(f_to_s(context, f_abs(context, image)).convertToString().getByteList());
        if (!lastCharDigit(str)) str.cat((byte)'*');
        str.cat((byte)'i');
        return str;
    }

    /** nucomp_inspect
     * 
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        boolean impos = tpositive_p(context, image);
        RubyString str = context.runtime.newString();
        str.cat((byte)'(');
        str.cat(f_inspect(context, real).convertToString().getByteList());
        str.cat(impos ? (byte)'+' : (byte)'-');
        str.cat(f_inspect(context, f_abs(context, image)).convertToString().getByteList());
        if (!lastCharDigit(str)) str.cat((byte)'*');
        str.cat((byte)'i');
        str.cat((byte)')');
        return str;
    }

    private static boolean lastCharDigit(RubyString str) {
        ByteList bytes = str.getByteList();
        return ASCIIEncoding.INSTANCE.isDigit(bytes.getUnsafeBytes()[bytes.getBegin() + bytes.getRealSize() - 1]);
    }

    /** nucomp_marshal_dump
     * 
     */
    @JRubyMethod(name = "marshal_dump")
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyArray dump = context.runtime.newArray(real, image);
        if (hasVariables()) dump.syncVariables(this);
        return dump;
    }

    /** nucomp_marshal_load
     * 
     */
    @JRubyMethod(name = "marshal_load")
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        real = load.size() > 0 ? load.eltInternal(0) : context.runtime.getNil();
        image = load.size() > 1 ? load.eltInternal(1) : context.runtime.getNil();

        if (load.hasVariables()) syncVariables((IRubyObject)load);
        return this;
    }

    /** nucomp_to_c
     *
     */
    @JRubyMethod(name = "to_c")
    public IRubyObject to_c(ThreadContext context) {
        return this;
    }

    /** nucomp_to_i
     * 
     */
    @JRubyMethod(name = "to_i")
    public IRubyObject to_i(ThreadContext context) {
        if (k_inexact_p(image) || !f_zero_p(context, image)) {
            throw context.runtime.newRangeError("can't convert " + f_to_s(context, this).convertToString() + " into Integer");
        }
        return f_to_i(context, real);
    }

    /** nucomp_to_f
     * 
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f(ThreadContext context) {
        if (k_inexact_p(image) || !f_zero_p(context, image)) {
            throw context.runtime.newRangeError("can't convert " + f_to_s(context, this).convertToString() + " into Float");
        }
        return f_to_f(context, real);
    }

    /** nucomp_to_r
     * 
     */
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        if (k_inexact_p(image) || !f_zero_p(context, image)) {
            throw context.runtime.newRangeError("can't convert " + f_to_s(context, this).convertToString() + " into Rational");
        }
        return f_to_r(context, real);
    }

    /** nucomp_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        if (k_inexact_p(image) || !f_zero_p(context, image)) {
            throw context.runtime.newRangeError("can't convert " + f_to_s(context, this).convertToString() + " into Rational");
        }
        return real.callMethod(context, "rationalize", args);
    }
    
    static RubyArray str_to_c_internal(ThreadContext context, IRubyObject recv) {
        RubyString s = recv.convertToString();
        ByteList bytes = s.getByteList();

        Ruby runtime = context.runtime;
        if (bytes.getRealSize() == 0) return runtime.newArray(runtime.getNil(), recv);

        IRubyObject sr, si, re;
        sr = si = re = runtime.getNil();
        boolean po = false;
        IRubyObject m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat0).match_m19(context, s, false, Block.NULL_BLOCK);

        if (!m.isNil()) {
            RubyMatchData match = (RubyMatchData)m;
            sr = match.op_aref19(RubyFixnum.one(runtime));
            si = match.op_aref19(RubyFixnum.two(runtime));
            re = match.post_match(context);
            po = true;
        }

        if (m.isNil()) {
            m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat1).match_m19(context, s, false, Block.NULL_BLOCK);

            if (!m.isNil()) {
                RubyMatchData match = (RubyMatchData)m;
                sr = runtime.getNil();
                si = match.op_aref19(RubyFixnum.one(runtime));
                if (si.isNil()) si = runtime.newString();
                IRubyObject t = match.op_aref19(RubyFixnum.two(runtime));
                if (t.isNil()) t = runtime.newString(new ByteList(new byte[]{'1'}));
                si.convertToString().cat(t.convertToString().getByteList());
                re = match.post_match(context);
                po = false;
            }
        }

        if (m.isNil()) {
            m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat2).match_m19(context, s, false, Block.NULL_BLOCK);
            if (m.isNil()) return runtime.newArray(runtime.getNil(), recv);
            RubyMatchData match = (RubyMatchData)m;
            sr = match.op_aref19(RubyFixnum.one(runtime));
            if (match.op_aref19(RubyFixnum.two(runtime)).isNil()) {
                si = runtime.getNil();
            } else {
                si = match.op_aref19(RubyFixnum.three(runtime));
                IRubyObject t = match.op_aref19(RubyFixnum.four(runtime));
                if (t.isNil()) t = runtime.newString(RubyFixnum.SINGLE_CHAR_BYTELISTS19['1']);
                si.convertToString().cat(t.convertToString().getByteList());
            }
            re = match.post_match(context);
            po = false;
        }

        IRubyObject r = RubyFixnum.zero(runtime);
        IRubyObject i = r;

        if (!sr.isNil()) {
            if (sr.callMethod(context, "include?", runtime.newString(new ByteList(new byte[]{'/'}))).isTrue()) {
                r = f_to_r(context, sr);
            } else if (f_gt_p(context, sr.callMethod(context, "count", runtime.newString(".eE")), RubyFixnum.zero(runtime)).isTrue()) {
                r = f_to_f(context, sr); 
            } else {
                r = f_to_i(context, sr);
            }
        }

        if (!si.isNil()) {
            if (si.callMethod(context, "include?", runtime.newString(new ByteList(new byte[]{'/'}))).isTrue()) {
                i = f_to_r(context, si);
            } else if (f_gt_p(context, si.callMethod(context, "count", runtime.newString(".eE")), RubyFixnum.zero(runtime)).isTrue()) {
                i = f_to_f(context, si);
            } else {
                i = f_to_i(context, si);
            }
        }
        return runtime.newArray(po ? newComplexPolar(context, r, i) : newComplexCanonicalize(context, r, i), re);
    }
    
    private static IRubyObject str_to_c_strict(ThreadContext context, IRubyObject recv) {
        RubyArray a = str_to_c_internal(context, recv);
        if (a.eltInternal(0).isNil() || a.eltInternal(1).convertToString().getByteList().length() > 0) {
            IRubyObject s = recv.callMethod(context, "inspect");
            throw context.runtime.newArgumentError("invalid value for convert(): " + s.convertToString());
        }
        return a.eltInternal(0);
    }

    private static JavaSites.ComplexSites sites(ThreadContext context) {
        return context.sites.Complex;
    }
}
