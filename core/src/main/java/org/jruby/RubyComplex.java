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
import static org.jruby.util.Numeric.f_add;
import static org.jruby.util.Numeric.f_arg;
import static org.jruby.util.Numeric.f_conjugate;
import static org.jruby.util.Numeric.f_denominator;
import static org.jruby.util.Numeric.f_div;
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
import static org.jruby.util.Numeric.f_quo;
import static org.jruby.util.Numeric.f_real_p;
import static org.jruby.util.Numeric.f_reciprocal;
import static org.jruby.util.Numeric.f_sub;
import static org.jruby.util.Numeric.f_to_f;
import static org.jruby.util.Numeric.f_to_i;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.Numeric.f_to_s;
import static org.jruby.util.Numeric.f_xor;
import static org.jruby.util.Numeric.f_zero_p;
import static org.jruby.util.Numeric.k_exact_p;
import static org.jruby.util.Numeric.k_inexact_p;
import static org.jruby.util.Numeric.num_pow;
import static org.jruby.util.Numeric.safe_mul;

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.TypeConverter;

import java.util.function.BiFunction;

import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

/**
 *  complex.c as of revision: 20011
 */
@JRubyClass(name = "Complex", parent = "Numeric")
public class RubyComplex extends RubyNumeric {

    public static RubyClass createComplexClass(Ruby runtime) {
        final String[] UNDEFINED = new String[]{
                "<", "<=", "<=>", ">", ">=",
                "between?", "divmod", "floor", "ceil", "modulo",
                "round", "step", "truncate", "positive?", "negative?"
        };

        RubyClass complexc = runtime.defineClass("Complex", runtime.getNumeric(), RubyComplex::new);

        complexc.setClassIndex(ClassIndex.COMPLEX);
        complexc.setReifiedClass(RubyComplex.class);
        
        complexc.kindOf = new RubyModule.JavaClassKindOf(RubyComplex.class);

        complexc.defineAnnotatedMethods(RubyComplex.class);

        complexc.getSingletonClass().undefineMethod("allocate");
        complexc.getSingletonClass().undefineMethod("new");

        for (String undef : UNDEFINED) {
            complexc.undefineMethod(undef);
        }

        complexc.defineConstant("I", RubyComplex.convert(runtime.getCurrentContext(), complexc, RubyFixnum.zero(runtime), RubyFixnum.one(runtime)));

        return complexc;
    }

    /** internal
     * 
     */
    private RubyComplex(Ruby runtime, RubyClass clazz, IRubyObject real, IRubyObject image) {
        super(runtime, clazz);
        this.real = real;
        this.image = image;
        this.flags |= FROZEN_F;
    }

    private RubyComplex(Ruby runtime, RubyClass clazz) {
        super(runtime, clazz);

        RubyFixnum zero = RubyFixnum.zero(runtime);

        this.real = zero;
        this.image = zero;
        this.flags |= FROZEN_F;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.COMPLEX;
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

    public static RubyComplex newComplexRawImage(Ruby runtime, IRubyObject image) {
        return new RubyComplex(runtime, runtime.getComplex(), RubyFixnum.zero(runtime), image);
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
    static RubyNumeric newComplex(ThreadContext context, RubyClass clazz, IRubyObject x) {
        return newComplex(context, clazz, x, RubyFixnum.zero(context.runtime));
    }

    /** f_complex_new2
     * 
     */
    static RubyNumeric newComplex(ThreadContext context, RubyClass clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyComplex);
        return canonicalizeInternal(context, clazz, x, y);
    }
    
    /** f_complex_new_bang2
     * 
     */
    static RubyComplex newComplexBang(ThreadContext context, RubyClass clazz, RubyNumeric x, RubyNumeric y) {
        return new RubyComplex(context.runtime, clazz, x, y);
    }

    /** f_complex_new_bang1
     * 
     */
    static RubyComplex newComplexBang(ThreadContext context, RubyClass clazz, RubyNumeric x) {
        return newComplexBang(context, clazz, x, RubyFixnum.zero(context.runtime));
    }

    @Deprecated
    public static RubyComplex newComplexBang(ThreadContext context, RubyClass clazz, IRubyObject x) {
        return newComplexBang(context, clazz, (RubyNumeric) x);
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
        if (!(x instanceof RubyComplex)) return RubyMath.cos(context, x, x);
        RubyComplex complex = (RubyComplex)x;
        return newComplex(context, context.runtime.getComplex(),
                          f_mul(context, RubyMath.cos(context, x, complex.real), RubyMath.cosh(context, x, complex.image)),
                          f_mul(context, f_negate(context, RubyMath.sin(context, x, complex.real)), RubyMath.sinh(context, x, complex.image)));
    }

    /** m_sin
     * 
     */
    private static IRubyObject m_sin(ThreadContext context, IRubyObject x) {
        if (!(x instanceof RubyComplex)) return RubyMath.sin(context, x, x);
        RubyComplex complex = (RubyComplex)x;
        return newComplex(context, context.runtime.getComplex(),
                          f_mul(context, RubyMath.sin(context, x, complex.real), RubyMath.cosh(context, x, complex.image)),
                          f_mul(context, RubyMath.cos(context, x, complex.real), RubyMath.sinh(context, x, complex.image)));
    }    
    
    /** m_sqrt
     * 
     */
    private static IRubyObject m_sqrt(ThreadContext context, IRubyObject x) {
        if (f_real_p(context, x)) {
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
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 1: return newInstanceBang(context, recv, args[0]);
            case 2: return newInstanceBang(context, recv, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.runtime, args.length, 1, 2);
        return null;
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        return new RubyComplex(context.runtime, (RubyClass) recv, real, RubyFixnum.zero(context.runtime));
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        if (!(image instanceof RubyNumeric)) image = f_to_i(context, image);
        return new RubyComplex(context.runtime, (RubyClass) recv, real, image);
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
    private static boolean realCheck(ThreadContext context, IRubyObject num, boolean raise) {
        switch (num.getMetaClass().getClassIndex()) {
        case INTEGER:
        case FLOAT:
        case RATIONAL:
            break;
        default:
            if (!(num instanceof RubyNumeric ) || !f_real_p(context, num)) {
                if (raise) {
                    throw context.runtime.newTypeError("not a real");
                }

                return false;
            }
        }

        return true;
    }

    /** nucomp_s_canonicalize_internal
     * 
     */
    private static RubyNumeric canonicalizeInternal(ThreadContext context, RubyClass clazz, IRubyObject real, IRubyObject image) {
        if (Numeric.CANON) {
            if (f_zero_p(context, image) && k_exact_p(image) && canonicalization) return (RubyNumeric) real;
        }
        boolean realComplex = real instanceof RubyComplex;
        boolean imageComplex = image instanceof RubyComplex;
        if (!realComplex && !imageComplex) {
            return new RubyComplex(context.runtime, clazz, real, image);
        }
        if (!realComplex) {
            RubyComplex complex = (RubyComplex)image;
            return new RubyComplex(context.runtime, clazz,
                                   f_sub(context, real, complex.image),
                                   f_add(context, RubyFixnum.zero(context.runtime), complex.real));
        }
        if (!imageComplex) {
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
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 1: return newInstance(context, recv, args[0]);
            case 2: return newInstance(context, recv, args[0], args[1]);
        }
        Arity.raiseArgumentError(context.runtime, args.length, 1, 2);
        return null;
    }

    @Deprecated // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceNew(ThreadContext context, IRubyObject recv, IRubyObject real) {
        return newInstance(context, recv, real);
    }

    @JRubyMethod(name = {"rect", "rectangular"}, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real) {
        return newInstance(context, recv, real, true);
    }

    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real, boolean raise) {
        if (!realCheck(context, real, raise)) return context.nil;
        return canonicalizeInternal(context, (RubyClass) recv, real, RubyFixnum.zero(context.runtime));
    }

    @Deprecated // @JRubyMethod(name = "new", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceNew(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        return newInstance(context, recv, real, image);
    }

    @JRubyMethod(name = {"rect", "rectangular"}, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        return newInstance(context, recv, real, image, true);
    }

    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image, boolean raise) {
        if (!(realCheck(context, real, raise) && realCheck(context, image, raise))) return context.nil;

        return canonicalizeInternal(context, (RubyClass) recv, real, image);
    }

    /** f_complex_polar
     * 
     */
    private static IRubyObject f_complex_polar(ThreadContext context, RubyClass clazz, IRubyObject x, IRubyObject y) {
        assert !(x instanceof RubyComplex) && !(y instanceof RubyComplex);
        return canonicalizeInternal(context, clazz,
                                             f_mul(context, x, m_cos(context, y)),
                                             f_mul(context, x, m_sin(context, y)));
    }

    /** nucomp_s_polar
     *
     */
    @JRubyMethod(name = "polar", meta = true, required = 1, optional = 1)
    public static IRubyObject polar(ThreadContext context, IRubyObject clazz, IRubyObject... args) {
        IRubyObject abs = args[0];
        IRubyObject arg;
        if (args.length < 2) {
            arg = RubyFixnum.zero(context.runtime);
        } else {
            arg = args[1];
        }
        realCheck(context, abs, true);
        realCheck(context, arg, true);
        return f_complex_polar(context, (RubyClass) clazz, abs, arg);
    }

    @Deprecated
    public static IRubyObject polar19(ThreadContext context, IRubyObject clazz, IRubyObject[] args) {
        return polar(context, clazz, args);
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
        Arity.raiseArgumentError(context.runtime, args.length, 1, 2);
        return null;
    }

    /** nucomp_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return convertCommon(context, recv, arg, null, true);
    }

    /** nucomp_s_convert
     *
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        Ruby runtime = context.runtime;

        boolean raise = true;

        if (a2 != null) {
            IRubyObject maybeKwargs = ArgsUtil.getOptionsArg(runtime, a2, false);

            if (!maybeKwargs.isNil()) {
                a2 = null;

                IRubyObject exception = ArgsUtil.extractKeywordArg(context, "exception", (RubyHash) maybeKwargs);

                raise = exception.isNil() ? true : exception.isTrue();
            }
        }

        return convertCommon(context, recv, a1, a2, raise);
    }

    /** nucomp_s_convert
     *
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2, IRubyObject kwargs) {
        Ruby runtime = context.runtime;

        IRubyObject maybeKwargs = ArgsUtil.getOptionsArg(runtime, kwargs, false);
        boolean raise;

        if (maybeKwargs.isNil()) {
            throw runtime.newArgumentError("convert", 3, 1, 2);
        }

        IRubyObject exception = ArgsUtil.extractKeywordArg(context, "exception", (RubyHash) maybeKwargs);

        raise = exception.isNil() ? true : exception.isTrue();

        return convertCommon(context, recv, a1, a2, raise);
    }

    // MRI: nucomp_s_convert
    private static IRubyObject convertCommon(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2, boolean raise) {
        final boolean singleArg = a2 == null;

        if (a1 == context.nil || a2 == context.nil) {
            if (raise) {
                throw context.runtime.newTypeError("can't convert nil into Complex");
            }

            return context.nil;
        }

        if (a1 instanceof RubyString) {
            a1 = str_to_c_strict(context, (RubyString) a1, raise);

            if (a1.isNil()) return a1;
        }
        if (a2 instanceof RubyString) {
            a2 = str_to_c_strict(context, (RubyString) a2, raise);

            if (a2.isNil()) return a2;
        }

        if (a1 instanceof RubyComplex) {
            RubyComplex a1c = (RubyComplex) a1;
            if (k_exact_p(a1c.image) && f_zero_p(context, a1c.image)) a1 = a1c.real;
        }

        if (a2 instanceof RubyComplex) {
            RubyComplex a2c = (RubyComplex) a2;
            if (k_exact_p(a2c.image) && f_zero_p(context, a2c.image)) a2 = a2c.real;
        }

        if (a1 instanceof RubyComplex) {
            if (singleArg || (k_exact_p(a2) && f_zero_p(context, a2))) return a1;
        }

        if (singleArg) {
            if (a1 instanceof RubyNumeric) {
                if (!f_real_p(context, a1)) return a1;
            }
            else {
                try {
                    return TypeConverter.convertToType(context, a1, context.runtime.getComplex(), sites(context).to_c_checked, raise);
                } catch (RaiseException re) {
                    if (raise) throw re;
                    return context.nil;
                }
            }
            return newInstance(context, recv, a1, raise);
        }

        if (a1 instanceof RubyNumeric && a2 instanceof RubyNumeric &&
                (!f_real_p(context, a1) || !f_real_p(context, a2))) {
            Ruby runtime = context.runtime;
            return f_add(context, a1,
                    f_mul(context, a2, newComplexBang(context, runtime.getComplex(),
                            RubyFixnum.zero(runtime), RubyFixnum.one(runtime))));
        }

        if (a2.isNil()) {
            return newInstance(context, recv, a1);
        } else if (!raise && !(a2 instanceof RubyInteger) && !(a2 instanceof RubyFloat) && !(a2 instanceof RubyRational)) {
            return context.nil;
        }

        return newInstance(context, recv, a1, a2, true);
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
        } else if (other instanceof RubyNumeric && f_real_p(context, (RubyNumeric) other)) {
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
        } else if (other instanceof RubyNumeric && f_real_p(context, (RubyNumeric) other)) {
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
        } else if (other instanceof RubyNumeric && f_real_p(context, (RubyNumeric) other)) {
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
        return f_divide(context, this, other, (a, b) -> f_quo(context, a, b), sites(context).op_quo);
    }

    public static IRubyObject f_divide(ThreadContext context, RubyComplex self, IRubyObject other, BiFunction<IRubyObject, IRubyObject, IRubyObject> func, CallSite id) {
        Ruby runtime = context.runtime;

        IRubyObject selfReal = self.real;
        IRubyObject selfImage = self.image;

        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;

            IRubyObject otherReal = otherComplex.real;
            IRubyObject otherImage = otherComplex.image;
            boolean flo = (selfReal instanceof RubyFloat || selfImage instanceof RubyFloat ||
                    otherReal instanceof RubyFloat || otherImage instanceof RubyFloat);
            IRubyObject r, n, x, y;
            if (f_gt_p(context, f_abs(context, otherReal), f_abs(context, otherImage))) {
                r = func.apply(otherImage, otherReal);
                n = f_mul(context, otherReal, f_add(context, RubyFixnum.one(runtime), f_mul(context, r, r)));
                x = func.apply(f_add(context, selfReal, f_mul(context, selfImage, r)), n);
                y = func.apply(f_sub(context, selfImage, f_mul(context, selfReal, r)), n);
            } else {
                r = func.apply(otherReal, otherImage);
                n = f_mul(context, otherImage, f_add(context, RubyFixnum.one(runtime), f_mul(context, r, r)));
                x = func.apply(f_add(context, f_mul(context, selfReal, r), selfImage), n);
                y = func.apply(f_sub(context, f_mul(context, selfImage, r), selfReal), n);
            }
            if (!flo) {
                x = RubyRational.rationalCanonicalize(context, x);
                y = RubyRational.rationalCanonicalize(context, y);
            }
            return newComplex(context, self.getMetaClass(), x, y);
        } else if (other instanceof RubyNumeric && f_real_p(context, other)) {
            IRubyObject x, y;

            x = RubyRational.rationalCanonicalize(context, func.apply(selfReal, other));
            y = RubyRational.rationalCanonicalize(context, func.apply(selfImage, other));
            return newComplex(context, self.getMetaClass(), x, y);
        }
        return self.coerceBin(context, id, other);

    }

    /** nucomp_fdiv
     *
     */
    @JRubyMethod(name = "fdiv")
    @Override
    public IRubyObject fdiv(ThreadContext context, IRubyObject other) {
        CallSite fdiv = sites(context).fdiv;
        return f_divide(context, this, other, (a, b) -> fdiv.call(context, a, a, b), fdiv);
    }

    /** nucomp_expt
     * 
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_expt(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;

        if (k_exact_p(other) && f_zero_p(context, other)) {
            return newComplexBang(context, getMetaClass(), RubyFixnum.one(runtime));
        }

        if (other instanceof RubyRational && f_one_p(context, f_denominator(context, other))) {
            other = f_numerator(context, other); 
        }

        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex) other;
            if (f_zero_p(context, otherComplex.image)) {
                other = otherComplex.real;
            }
        }

        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;


            IRubyObject otherReal = otherComplex.real;
            IRubyObject otherImage = otherComplex.image;

            IRubyObject r = f_abs(context, this);
            IRubyObject theta = f_arg(context, this);

            IRubyObject nr = RubyMath.exp(context,
                    f_sub(context,
                            f_mul(context, otherReal, RubyMath.log(context, r)),
                            f_mul(context, otherImage, theta)));
            IRubyObject ntheta = f_add(context, f_mul(context, theta, otherReal),
                    f_mul(context, otherImage, RubyMath.log(context, r)));
            return f_complex_polar(context, getMetaClass(), nr, ntheta);
        } else if (other instanceof RubyFixnum) {
            long n = ((RubyFixnum) other).getLongValue();
            if (n == 0) {
                return newInstance(context, getMetaClass(), RubyFixnum.one(runtime), RubyFixnum.zero(runtime));
            }
            RubyComplex self = this;
            if (n < 0) {
                self = (RubyComplex) f_reciprocal(context, self);
                other = ((RubyFixnum) other).op_uminus();
                n = -n;
            }
            {
                IRubyObject selfReal = self.real;
                IRubyObject selfImage = self.image;

                IRubyObject xr = selfReal, xi = selfImage, zr = xr, zi = xi;

                if (f_zero_p(context, xi)) {
                    zr = num_pow(context, zr, other);
                }
                else if (f_zero_p(context, xr)) {
                    zi = num_pow(context, zi, other);
                    if ((n & 2) != 0) zi = f_negate(context, zi);
                    if ((n & 1) == 0) {
                        IRubyObject tmp = zr;
                        zr = zi;
                        zi = tmp;
                    }
                }
                else {
                    while ((--n) != 0) {
                        long q, r;

                        r = n % 2;
                        for (; r == 0; n = q) {
                            IRubyObject tmp = f_sub(context, f_mul(context, xr, xr), f_mul(context, xi, xi));
                            xi = f_mul(context, f_mul(context, RubyFixnum.two(runtime), xr), xi);
                            xr = tmp;
                            q = n / 2;
                            r = n % 2;
                        }

                        // This section is comp_mul but that has out variables
                        IRubyObject areal = zr, aimag = zi, breal = xr, bimag = xi;
                        boolean arzero = f_zero_p(context, areal);
                        boolean aizero = f_zero_p(context, aimag);
                        boolean brzero = f_zero_p(context, breal);
                        boolean bizero = f_zero_p(context, bimag);
                        zr = f_sub(context, safe_mul(context, areal, breal, arzero, brzero),
                                safe_mul(context, aimag, bimag, aizero, bizero));
                        zi = f_add(context, safe_mul(context, areal, bimag, arzero, bizero),
                                safe_mul(context, aimag, breal, aizero, brzero));
                    }
                }
                return newInstance(context, getMetaClass(), zr, zi);
            }
        } else if (other instanceof RubyNumeric && f_real_p(context, other)) {
            IRubyObject r, theta;

            if (other instanceof RubyBignum) {
                runtime.getWarnings().warn("in a**b, b may be too big");
            }

            r = f_abs(context, this);
            theta = f_arg(context, this);

            return f_complex_polar(context, getMetaClass(),
                    f_expt(context, r, other),
                    f_mul(context, theta, other));
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

            return RubyBoolean.newBoolean(context, test);
        }

        if (other instanceof RubyNumeric && f_real_p(context, (RubyNumeric) other)) {
            boolean test = f_equal(context, real, other).isTrue() && f_zero_p(context, image);

            return RubyBoolean.newBoolean(context, test);
        }
        
        return f_equal(context, other, this);
    }

    /** nucomp_coerce 
     * 
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric && f_real_p(context, other)) {
            return context.runtime.newArray(newComplexBang(context, getMetaClass(), (RubyNumeric) other), this);
        }

        if (other instanceof RubyComplex) return context.runtime.newArray(other, this);

        Ruby runtime = context.runtime;
        throw runtime.newTypeError(str(runtime, types(runtime, other.getMetaClass()), " can't be coerced into ", types(runtime, getMetaClass())));
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
    @Override
    public IRubyObject real_p(ThreadContext context) {
        return context.fals;
    }

    @Override
    public boolean isReal() { return false; }

    /** nucomp_complex_p
     * 
     */
    // @JRubyMethod(name = "complex?")
    public IRubyObject complex_p(ThreadContext context) {
        return context.tru;
    }

    /** nucomp_exact_p
     * 
     */
    // @JRubyMethod(name = "exact?")
    public IRubyObject exact_p(ThreadContext context) {
        return (f_exact_p(context, real) && f_exact_p(context, image)) ? context.tru : context.fals;
    }

    /** nucomp_exact_p
     * 
     */
    // @JRubyMethod(name = "inexact?")
    public IRubyObject inexact_p(ThreadContext context) {
        return exact_p(context) == context.tru ? context.fals : context.tru;
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

    @Override
    public int hashCode() {
        final IRubyObject hash = hash(getRuntime().getCurrentContext());
        if (hash instanceof RubyFixnum) return (int) RubyNumeric.fix2long(hash);
        return nonFixnumHashCode(hash);
    }

    /** nucomp_eql_p
     * 
     */
    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        return RubyBoolean.newBoolean(context, equals(context, other));
    }

    private boolean equals(ThreadContext context, Object other) {
        if (other instanceof RubyComplex) {
            RubyComplex otherComplex = (RubyComplex)other;
            if (real.getMetaClass() == otherComplex.real.getMetaClass() &&
                image.getMetaClass() == otherComplex.image.getMetaClass() &&
                f_equal(context, this, otherComplex).isTrue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return equals(getRuntime().getCurrentContext(), other);
    }

    /** f_signbit
     * 
     */
    private static boolean signbit(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFloat) {
            double value = ((RubyFloat) x).value;
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
        real = load.size() > 0 ? load.eltInternal(0) : context.nil;
        image = load.size() > 1 ? load.eltInternal(1) : context.nil;

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

    @JRubyMethod(name = "finite?")
    @Override
    public IRubyObject finite_p(ThreadContext context) {
        if (checkFinite(context, real) && checkFinite(context, image)) {
            return context.tru;
        }
        return context.fals;
    }

    // MRI: f_finite_p
    public boolean checkFinite(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyInteger || value instanceof RubyRational) {
            return true;
        }

        if (value instanceof RubyFloat) {
            return ((RubyFloat) value).finite_p().isTrue();
        }

        return sites(context).finite.call(context, value, value).isTrue();
    }

    @JRubyMethod(name = "infinite?")
    @Override
    public IRubyObject infinite_p(ThreadContext context) {
        if (checkInfinite(context, real).isNil() && checkInfinite(context, image).isNil()) {
            return context.nil;
        }
        return RubyFixnum.newFixnum(getRuntime(), 1);
    }

    public IRubyObject checkInfinite(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyInteger || value instanceof RubyRational) {
            return context.nil;
        }

        if (value instanceof RubyFloat) {
            return ((RubyFloat) value).infinite_p();
        }

        return sites(context).infinite.call(context, value, value);
    }

    private static final ByteList SEP = RubyFile.SLASH;
    private static final ByteList _eE = new ByteList(new byte[] { '.', 'e', 'E' }, false);

    static IRubyObject[] str_to_c_internal(ThreadContext context, RubyString str) {
        final Ruby runtime = context.runtime;
        final IRubyObject nil = context.nil;

        ByteList bytes = str.getByteList();

        if (bytes.getRealSize() == 0) return new IRubyObject[] { context.nil, str };

        IRubyObject sr, si, re;
        sr = si = re = nil;
        boolean po = false;
        IRubyObject m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat0).match_m(context, str, false);

        if (m != nil) {
            RubyMatchData match = (RubyMatchData)m;
            sr = match.at(1);
            si = match.at(2);
            re = match.post_match(context);
            po = true;
        }

        if (m == nil) {
            m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat1).match_m(context, str, false);

            if (m != nil) {
                RubyMatchData match = (RubyMatchData)m;
                sr = nil;
                si = match.at(1);
                if (si == nil) si = runtime.newString();
                IRubyObject t = match.at(2);
                if (t == nil) t = runtime.newString(RubyInteger.singleCharByteList((byte) '1'));
                ((RubyString) (si = si.convertToString())).cat(t.convertToString());
                re = match.post_match(context);
                po = false;
            }
        }

        if (m == nil) {
            m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat2).match_m(context, str, false);
            if (m == nil) return new IRubyObject[] { context.nil, str };
            RubyMatchData match = (RubyMatchData) m;
            sr = match.at(1);
            if (match.at(2) == nil) {
                si = context.nil;
            } else {
                si = match.at(3);
                IRubyObject t = match.at(4);
                if (t == nil) t = runtime.newString(RubyInteger.singleCharByteList((byte) '1'));
                ((RubyString) (si = si.convertToString())).cat(t.convertToString());
            }
            re = match.post_match(context);
            po = false;
        }

        final RubyFixnum zero = RubyFixnum.zero(runtime);

        RubyNumeric r = convertString(context, sr, zero);
        RubyNumeric i = convertString(context, si, zero);

        return new IRubyObject[] { po ? newComplexPolar(context, r, i) : newComplexCanonicalize(context, r, i), re };
    }

    private static RubyNumeric convertString(ThreadContext context, final IRubyObject s, RubyFixnum zero) {
        if (s == context.nil) return zero;
        final Ruby runtime = context.runtime;
        if (s.callMethod(context, "include?", RubyString.newStringShared(runtime, SEP)).isTrue()) {
            return (RubyNumeric) f_to_r(context, s);
        }
        if (f_gt_p(context, s.callMethod(context, "count", RubyString.newStringShared(runtime, _eE)), zero)) {
            return (RubyNumeric) f_to_f(context, s);
        }
        return (RubyNumeric) f_to_i(context, s);
    }

    // MRI: string_to_c_strict
    private static IRubyObject str_to_c_strict(ThreadContext context, RubyString str, boolean raise) {
        IRubyObject[] ary = str_to_c_internal(context, str);
        if (ary[0] == context.nil || ary[1].convertToString().getByteList().length() > 0) {
            if (raise) {
                throw context.runtime.newArgumentError(str(context.runtime, "invalid value for convert(): ", str.callMethod(context, "inspect")));
            }

            return context.nil;

        }
        return (RubyNumeric) ary[0]; // (RubyComplex)
    }

    private static JavaSites.ComplexSites sites(ThreadContext context) {
        return context.sites.Complex;
    }
}
