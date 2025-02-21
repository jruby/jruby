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

import org.jcodings.specific.ASCIIEncoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Convert;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectMarshal;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.NewMarshal;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.TypeConverter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.BiFunction;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newRational;
import static org.jruby.api.Create.newSharedString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.invokedynamic;
import static org.jruby.runtime.invokedynamic.MethodNames.HASH;
import static org.jruby.util.Numeric.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.types;

/**
 *  complex.c as of revision: 20011
 */
@JRubyClass(name = "Complex", parent = "Numeric")
public class RubyComplex extends RubyNumeric {

    public static RubyClass createComplexClass(ThreadContext context, RubyClass Numeric) {
        return defineClass(context, "Complex", Numeric, RubyComplex::new).
                reifiedClass(RubyComplex.class).
                marshalWith(COMPLEX_MARSHAL).
                kindOf(new RubyModule.JavaClassKindOf(RubyComplex.class)).
                classIndex(ClassIndex.COMPLEX).
                defineMethods(context, RubyComplex.class).
                undefMethods(context, "<", "<=", ">", ">=", "between?", "clamp", "%", "div", "divmod", "floor", "ceil",
                        "modulo", "remainder", "round", "step", "truncate", "positive?", "negative?").
                tap(c -> c.singletonClass(context).undefMethods(context, "allocate", "new")).
                tap(c -> c.defineConstant(context, "I", RubyComplex.convert(context, c, asFixnum(context, 0), asFixnum(context, 1))));
    }

    private RubyComplex(Ruby runtime, RubyClass clazz, IRubyObject real, IRubyObject image) {
        super(runtime, clazz);
        this.real = real;
        this.image = image;
    }

    private RubyComplex(Ruby runtime, RubyClass clazz) {
        super(runtime, clazz);

        RubyFixnum zero = RubyFixnum.zero(runtime);

        this.real = zero;
        this.image = zero;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.COMPLEX;
    }

    /** internal
     *
     */
    private static RubyComplex newComplexInternal(Ruby runtime, RubyClass clazz, IRubyObject x, IRubyObject y) {
        RubyComplex ret = new RubyComplex(runtime, clazz, x, y);
        ret.setFrozen(true);
        return ret;
    }

    /** rb_complex_raw
     * 
     */
    public static RubyComplex newComplexRaw(Ruby runtime, IRubyObject x, IRubyObject y) {
        return newComplexInternal(runtime, runtime.getComplex(), x, y);
    }

    /** rb_complex_raw1
     * 
     */
    public static RubyComplex newComplexRaw(Ruby runtime, IRubyObject x) {
        return newComplexRaw(runtime, x, RubyFixnum.zero(runtime));
    }

    public static RubyComplex newComplexRawImage(Ruby runtime, IRubyObject image) {
        return newComplexRaw(runtime, RubyFixnum.zero(runtime), image);
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
        RubyNumeric ret = canonicalizeInternal(context, clazz, x, y);
        ret.setFrozen(true);
        return ret;
    }
    
    /** f_complex_new_bang2
     * 
     */
    static RubyComplex newComplexBang(ThreadContext context, RubyClass clazz, RubyNumeric x, RubyNumeric y) {
        return newComplexInternal(context.runtime, clazz, x, y);
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
        Arity.raiseArgumentError(context, args.length, 1, 2);
        return null;
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        return newComplexInternal(context.runtime, (RubyClass) recv, real, RubyFixnum.zero(context.runtime));
    }

    @JRubyMethod(name = "new!", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject newInstanceBang(ThreadContext context, IRubyObject recv, IRubyObject real, IRubyObject image) {
        if (!(real instanceof RubyNumeric)) real = f_to_i(context, real);
        if (!(image instanceof RubyNumeric)) image = f_to_i(context, image);
        return newComplexInternal(context.runtime, (RubyClass) recv, real, image);
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
            if (!(num instanceof RubyNumeric) || !f_real_p(context, num)) {
                if (raise) throw typeError(context, "not a real");
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
            return newComplexInternal(context.runtime, clazz, real, image);
        }
        if (!realComplex) {
            RubyComplex complex = (RubyComplex)image;
            return newComplexInternal(context.runtime, clazz,
                                   f_sub(context, real, complex.image),
                                   f_add(context, RubyFixnum.zero(context.runtime), complex.real));
        }
        if (!imageComplex) {
            RubyComplex complex = (RubyComplex)real;
            return newComplexInternal(context.runtime, clazz,
                                   complex.real,
                                   f_add(context, complex.image, image));
        } else {
            RubyComplex complex1 = (RubyComplex)real;
            RubyComplex complex2 = (RubyComplex)image;
            return newComplexInternal(context.runtime, clazz,
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
        Arity.raiseArgumentError(context, args.length, 1, 2);
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
    @JRubyMethod(name = "polar", meta = true, required = 1, optional = 1, checkArity = false)
    public static IRubyObject polar(ThreadContext context, IRubyObject clazz, IRubyObject... args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 2);

        IRubyObject abs = args[0];
        IRubyObject arg = argc < 2 ? RubyFixnum.zero(context.runtime) : args[1];

        realCheck(context, abs, true);
        realCheck(context, arg, true);

        if (abs instanceof RubyComplex) abs = ((RubyComplex) abs).getReal();
        if (arg instanceof RubyComplex) arg = ((RubyComplex) arg).getReal();

        return f_complex_polar(context, (RubyClass) clazz, abs, arg);
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
        Arity.raiseArgumentError(context, args.length, 1, 2);
        return null;
    }

    /** nucomp_s_convert
     * 
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        if (arg instanceof RubyComplex) return arg;
        return convertCommon(context, recv, arg, null, true);
    }

    /** nucomp_s_convert
     *
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2) {
        IRubyObject maybeKwargs = ArgsUtil.getOptionsArg(context.runtime, a2, false);
        if (maybeKwargs.isNil()) return convertCommon(context, recv, a1, a2, true);

        IRubyObject exception = ArgsUtil.extractKeywordArg(context, "exception", (RubyHash) maybeKwargs);
        if (exception instanceof RubyBoolean) {
            return a1 instanceof RubyComplex ? a1 : convertCommon(context, recv, a1, null, exception.isTrue());
        }

        throw argumentError(context, "'Complex': expected true or false as exception: " + exception);
    }

    /** nucomp_s_convert
     *
     */
    @JRubyMethod(name = "convert", meta = true, visibility = Visibility.PRIVATE)
    public static IRubyObject convert(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2, IRubyObject kwargs) {
        Ruby runtime = context.runtime;

        IRubyObject maybeKwargs = ArgsUtil.getOptionsArg(runtime, kwargs, false);
        if (maybeKwargs.isNil()) throw argumentError(context, 3, 1, 2);

        IRubyObject exception = ArgsUtil.extractKeywordArg(context, "exception", (RubyHash) maybeKwargs);
        if (exception instanceof RubyBoolean) {
            return convertCommon(context, recv, a1, a2, exception.isTrue());
        }

        throw argumentError(context, "'Complex': expected true or false as exception: " + exception);
    }

    // MRI: nucomp_s_convert
    private static IRubyObject convertCommon(ThreadContext context, IRubyObject recv, IRubyObject a1, IRubyObject a2, boolean raise) {
        final boolean singleArg = a2 == null;

        if (a1 == context.nil || a2 == context.nil) {
            if (raise) throw typeError(context, "can't convert nil into Complex");
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
            if (k_exact_zero_p(context, a1c.image)) a1 = a1c.real;
        }

        if (a2 instanceof RubyComplex) {
            RubyComplex a2c = (RubyComplex) a2;
            if (k_exact_zero_p(context, a2c.image)) a2 = a2c.real;
        }

        if (a1 instanceof RubyComplex) {
            if (singleArg || (k_exact_zero_p(context, a2))) return a1;
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

    // MRI: nucomp_real_p
    private boolean nucomp_real_p(ThreadContext context) {
        return f_zero_p(context, image);
    }

    @JRubyMethod(name="<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        if (nucomp_real_p(context) && k_numeric_p(other)) {
            if (other instanceof RubyComplex && ((RubyComplex) other).nucomp_real_p(context)) {
                return real.callMethod(context, "<=>", ((RubyComplex) other).real);
            } else if (f_real_p(context, other)) {
                return real.callMethod(context, "<=>", other);
            }
        }

        return context.nil;
    }

    @Deprecated(since = "10.0")
    public IRubyObject real() {
        return real(getCurrentContext());
    }

    // MRI: nucomp_real
    @JRubyMethod(name = "real")
    public IRubyObject real(ThreadContext context) {
        return real;
    }

    @Deprecated(since = "10.0")
    public IRubyObject image() {
        return image(getCurrentContext());
    }

    /** nucomp_image
     * 
     */
    @JRubyMethod(name = {"imaginary", "imag"})
    public IRubyObject image(ThreadContext context) {
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

    /**
     * MRI: nucomp_expt
     */
    @JRubyMethod(name = "**")
    public IRubyObject op_expt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric && k_exact_zero_p(context, other)) {
            return newComplexBang(context, getMetaClass(), asFixnum(context, 1));
        }

        if (other instanceof RubyRational otherRational && f_one_p(context, otherRational.getDenominator())) {
            other = f_numerator(context, other); 
        }

        if (other instanceof RubyComplex otherComplex && k_exact_zero_p(context, otherComplex.image)) {
            other = otherComplex.real;
        }

        if (other == RubyFixnum.one(context.runtime)) {
            return newComplex(context, metaClass, real, image);
        }

        IRubyObject result = complexPowForSpecialAngle(context, other);
        if (result != UNDEF) return result;

        if (other instanceof RubyComplex otherComplex) {
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
        } else if (other instanceof RubyFixnum otherFixnum) {
            long n = otherFixnum.asLong(context);
            if (n == 0) return newInstance(context, getMetaClass(), asFixnum(context, 1), asFixnum(context, 0));

            RubyComplex self = this;
            if (n < 0) {
                self = (RubyComplex) f_reciprocal(context, self);
                other = otherFixnum.op_uminus(context);
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
                            xi = f_mul(context, f_mul(context, asFixnum(context, 2), xr), xi);
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

            if (other instanceof RubyBignum) warn(context, "in a**b, b may be too big");

            r = f_abs(context, this);
            theta = f_arg(context, this);

            return f_complex_polar(context, getMetaClass(),
                    f_expt(context, r, other),
                    f_mul(context, theta, other));
        }
        return coerceBin(context, sites(context).op_exp, other);
    }

    // MRI: complex_pow_for_special_angle
    private IRubyObject complexPowForSpecialAngle(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyInteger integer)) {
            return UNDEF;
        }

        IRubyObject x = UNDEF;
        int dir;
        if (f_zero_p(context, image)) {
            x = real;
            dir = 0;
        }
        else if (f_zero_p(context, real)) {
            x = image;
            dir = 2;
        }
        else if (Numeric.f_eqeq_p(context, real, image)) {
            x = real;
            dir = 1;
        }
        else if (Numeric.f_eqeq_p(context, real, Numeric.f_negate(context, image))) {
            x = image;
            dir = 3;
        } else {
            dir = 0;
        }

        if (x == UNDEF) return x;

        if (f_negative_p(context, x)) {
            x = f_negate(context, x);
            dir += 4;
        }

        IRubyObject zx;
        if (dir % 2 == 0) {
            zx = num_pow(context, x, other);
        }
        else {
            RubyFixnum two = RubyFixnum.two(context.runtime);
            zx = num_pow(context,
                    sites(context).op_times.call(context, two.op_mul(context, x), x),
                    integer.div(context, two)
            );
            if (f_odd_p(context, other)) {
                zx = sites(context).op_times.call(context, zx, x);
            }
        }
        int dirs[][] = {
            {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}
        };
        int z_dir = toInt(context, asFixnum(context, dir).modulo(context, 8));

        IRubyObject zr = context.fals, zi = context.fals;
        switch (dirs[z_dir][0]) {
            case 0: zr = zero_for(context, zx); break;
            case 1: zr = zx; break;
            case -1: zr = f_negate(context, zx); break;
        }
        switch (dirs[z_dir][1]) {
            case 0: zi = zero_for(context, zx); break;
            case 1: zi = zx; break;
            case -1: zi = f_negate(context, zx); break;
        }
        return newComplex(context, metaClass, zr, zi);
    }

    private static IRubyObject zero_for(ThreadContext context, IRubyObject x) {
        if (x instanceof RubyFloat)
            return asFloat(context, 0);
        if (x instanceof RubyRational)
            return newRational(context, 0, 1);

        return asFixnum(context, 0);
    }

    /** nucomp_equal_p
     * 
     */
    @JRubyMethod(name = "==")
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex comp) {
            return asBoolean(context, f_equal(context, real, comp.real).isTrue() && f_equal(context, image, comp.image).isTrue());
        }

        if (other instanceof RubyNumeric num && f_real_p(context, num)) {
            return asBoolean(context, f_equal(context, real, num).isTrue() && f_zero_p(context, image));
        }
        
        return f_equal(context, other, this);
    }

    /** nucomp_coerce 
     * 
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyComplex) return newArray(context, other, this);

        if (other instanceof RubyNumeric numeric && f_real_p(context, other)) {
            return newArray(context, newComplexBang(context, getMetaClass(), numeric), this);
        }

        Ruby runtime = context.runtime;
        throw typeError(context, str(runtime, types(runtime, other.getMetaClass()), " can't be coerced into ", types(runtime, getMetaClass())));
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
        return newArray(context, real, image);
    }

    /** nucomp_polar 
     * 
     */
    @JRubyMethod(name = "polar")
    @Override
    public IRubyObject polar(ThreadContext context) {
        return newArray(context, f_abs(context, this), f_arg(context, this));
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
    public RubyFixnum hash(ThreadContext context) {
        long realHash = toLong(context, invokedynamic(context, real, HASH));
        long imageHash = toLong(context, invokedynamic(context, image, HASH));
        byte [] bytes = ByteBuffer.allocate(16).putLong(realHash).putLong(imageHash).array();
        return asFixnum(context, Helpers.multAndMix(context.runtime.getHashSeedK0(), Arrays.hashCode(bytes)));
    }

    @Override
    public int hashCode() {
        var context = getRuntime().getCurrentContext();
        final IRubyObject hash = hash(context);
        return hash instanceof RubyFixnum fixnum ?
                fixnum.asIntUnsafe(context) :
                nonFixnumHashCode(context, hash);
    }

    /** nucomp_eql_p
     * 
     */
    @JRubyMethod(name = "eql?")
    @Override
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        return asBoolean(context, equals(context, other));
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
    @JRubyMethod(name = "marshal_dump", visibility = Visibility.PRIVATE)
    public IRubyObject marshal_dump(ThreadContext context) {
        var dump = newArray(context, real, image);
        if (hasVariables()) dump.syncVariables(this);
        return dump;
    }

    /** nucomp_marshal_load
     * 
     */
    @JRubyMethod(name = "marshal_load", visibility = Visibility.PRIVATE)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        real = load.size() > 0 ? load.eltInternal(0) : context.nil;
        image = load.size() > 1 ? load.eltInternal(1) : context.nil;

        if (load.hasVariables()) syncVariables((IRubyObject)load);
        return this;
    }

    private static final ObjectMarshal COMPLEX_MARSHAL = new ObjectMarshal() {
        @Override
        public void marshalTo(Ruby runtime, Object obj, RubyClass type, MarshalStream marshalStream) {
            //do nothing
        }

        @Override
        public void marshalTo(Object obj, RubyClass type, NewMarshal marshalStream, ThreadContext context, NewMarshal.RubyOutputStream out) {
            //do nothing
        }

        @Override
        public Object unmarshalFrom(Ruby runtime, RubyClass type,
                                    UnmarshalStream unmarshalStream) throws IOException {
            RubyComplex c = (RubyComplex)RubyClass.DEFAULT_OBJECT_MARSHAL.unmarshalFrom(runtime, type, unmarshalStream);
            c.real = c.removeInstanceVariable("@real");
            c.image = c.removeInstanceVariable("@image");
            c.setFrozen(true);

            return c;
        }
    };

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
        checkValidRational(context, "Integer");
        return f_to_i(context, real);
    }

    /** nucomp_to_f
     * 
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f(ThreadContext context) {
        checkValidRational(context, "Float");
        return f_to_f(context, real);
    }

    /** nucomp_to_r
     * 
     */
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        if (image instanceof RubyFloat imageFloat && imageFloat.isZero(context)) {
            /* Do nothing here */
        } else if (!k_exact_zero_p(context, image)) {
            IRubyObject imag = Convert.checkToRational(context, image);
            if (imag.isNil() || !k_exact_zero_p(context, imag)) {
                throw rangeError(context, "can't convert " + this + " into Rational");
            }
        }

        return f_to_r(context, real);
    }

    /** nucomp_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1, checkArity = false)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        checkValidRational(context, "Rational");
        return real.callMethod(context, "rationalize", args);
    }

    private void checkValidRational(ThreadContext context, String type) {
        if (!k_exact_zero_p(context, image)) {
            throw rangeError(context, "can't convert " + f_to_s(context, this).convertToString() + " into " + type);
        }
    }

    @JRubyMethod(name = "finite?")
    @Override
    public IRubyObject finite_p(ThreadContext context) {
        return checkFinite(context, real) && checkFinite(context, image) ? context.tru : context.fals;
    }

    // MRI: f_finite_p
    public boolean checkFinite(ThreadContext context, IRubyObject value) {
        return checkInfinite(context, value).isTrue();
    }

    @JRubyMethod(name = "infinite?")
    @Override
    public IRubyObject infinite_p(ThreadContext context) {
        return checkInfinite(context, real).isNil() && checkInfinite(context, image).isNil() ?
            context.nil : asFixnum(context, 1);
    }

    public IRubyObject checkInfinite(ThreadContext context, IRubyObject value) {
        if (value instanceof RubyInteger || value instanceof RubyRational) return context.nil;

        return value instanceof RubyFloat flote ?
                flote.infinite_p(context) :
                sites(context).infinite.call(context, value, value);
    }

    private static final ByteList SEP = RubyFile.SLASH;
    private static final ByteList _eE = new ByteList(new byte[] { '.', 'e', 'E' }, false);

    static IRubyObject[] str_to_c_internal(ThreadContext context, RubyString str) {
        final Ruby runtime = context.runtime;
        final IRubyObject nil = context.nil;

        ByteList bytes = str.getByteList();

        if (bytes.getRealSize() == 0) return new IRubyObject[] { nil, str };

        IRubyObject sr, si, re;
        sr = si = re = nil;
        boolean po = false;
        IRubyObject m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat0).match_m(context, str, false);

        if (m != nil) {
            RubyMatchData match = (RubyMatchData)m;
            sr = match.at(context, 1);
            si = match.at(context, 2);
            re = match.post_match(context);
            po = true;
        }

        if (m == nil) {
            m = RubyRegexp.newDummyRegexp(runtime, Numeric.ComplexPatterns.comp_pat1).match_m(context, str, false);

            if (m != nil) {
                RubyMatchData match = (RubyMatchData)m;
                sr = nil;
                si = match.at(context, 1);
                if (si == nil) si = runtime.newString();
                IRubyObject t = match.at(context, 2);
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
            sr = match.at(context, 1);
            if (match.at(context, 2) == nil) {
                si = context.nil;
            } else {
                si = match.at(context, 3);
                IRubyObject t = match.at(context, 4);
                if (t == nil) t = runtime.newString(RubyInteger.singleCharByteList((byte) '1'));
                ((RubyString) (si = si.convertToString())).cat(t.convertToString());
            }
            re = match.post_match(context);
            po = false;
        }

        final RubyFixnum zero = RubyFixnum.zero(runtime);

        try {
            RubyNumeric r = convertString(context, sr, zero);
            RubyNumeric i = convertString(context, si, zero);

            return new IRubyObject[]{po ? newComplexPolar(context, r, i) : newComplexCanonicalize(context, r, i), re};
        } catch(RaiseException exception) {
            context.setErrorInfo(context.nil);
            return new IRubyObject[] {context.nil, str};
        }
    }

    private static RubyNumeric convertString(ThreadContext context, final IRubyObject s, RubyFixnum zero) {
        if (s == context.nil) return zero;

        if (s.callMethod(context, "include?", newSharedString(context, SEP)).isTrue()) {
            return (RubyNumeric) f_to_r(context, s);
        }
        if (f_gt_p(context, s.callMethod(context, "count", newSharedString(context, _eE)), zero)) {
            return (RubyNumeric) f_to_f(context, s);
        }
        return (RubyNumeric) ((RubyString) s).stringToInum(10, true);
    }

    // MRI: string_to_c_strict
    private static IRubyObject str_to_c_strict(ThreadContext context, RubyString str, boolean raise) {
        str.verifyAsciiCompatible();

        if (str.hasNul()) {
            if (!raise) return context.nil;

            throw argumentError(context, "string contains null byte");
        }

        IRubyObject[] ary = str_to_c_internal(context, str);
        if (ary[0] == context.nil || ary[1].convertToString().getByteList().length() > 0) {
            if (raise) {
                throw argumentError(context, str(context.runtime, "invalid value for convert(): ", str.callMethod(context, "inspect")));
            }

            return context.nil;

        }
        return ary[0]; // (RubyComplex)
    }

    private static JavaSites.ComplexSites sites(ThreadContext context) {
        return context.sites.Complex;
    }
}
