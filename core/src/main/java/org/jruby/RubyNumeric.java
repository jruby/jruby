/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Antti Karanta <Antti.Karanta@napa.fi>
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.ConvertDouble;
import org.jruby.util.TypeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.util.Numeric.*;

/**
 * Base class for all numerical types in ruby.
 */
// TODO: Numeric.new works in Ruby and it does here too.  However trying to use
//   that instance in a numeric operation should generate an ArgumentError. Doing
//   this seems so pathological I do not see the need to fix this now.
@JRubyClass(name="Numeric", include="Comparable")
public class RubyNumeric extends RubyObject {

    public static RubyClass createNumericClass(Ruby runtime) {
        RubyClass numeric = runtime.defineClass("Numeric", runtime.getObject(), NUMERIC_ALLOCATOR);
        runtime.setNumeric(numeric);

        numeric.setClassIndex(ClassIndex.NUMERIC);
        numeric.setReifiedClass(RubyNumeric.class);

        numeric.kindOf = new RubyModule.JavaClassKindOf(RubyNumeric.class);

        numeric.includeModule(runtime.getComparable());
        numeric.defineAnnotatedMethods(RubyNumeric.class);

        return numeric;
    }

    protected static final ObjectAllocator NUMERIC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNumeric(runtime, klass);
        }
    };

    public static final double DBL_EPSILON=2.2204460492503131e-16;

    public RubyNumeric(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyNumeric(RubyClass metaClass) {
        super(metaClass);
    }

    public RubyNumeric(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }

    @Deprecated
    public RubyNumeric(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, metaClass, useObjectSpace, canBeTainted);
    }

    public static RoundingMode getRoundingMode(ThreadContext context, IRubyObject opts) {
        IRubyObject halfArg = ArgsUtil.extractKeywordArg(context, "half", opts);

        if (halfArg.isNil()) {
            return RoundingMode.HALF_UP;
        } else if (halfArg instanceof RubySymbol) {
            String halfString = halfArg.toString();

            switch (halfString) {
                case "up":
                    return RoundingMode.HALF_UP;
                case "even":
                    return RoundingMode.HALF_EVEN;
                case "down":
                    return RoundingMode.HALF_DOWN;
            }
        }

        throw context.runtime.newArgumentError("invalid rounding mode: " + halfArg);
    }

    // The implementations of these are all bonus (see TODO above)  I was going
    // to throw an error from these, but it appears to be the wrong place to
    // do it.
    public double getDoubleValue() {
        return 0;
    }

    /**
     * Return the value of this numeric as a 64-bit long. If the value does not
     * fit in 64 bits, it will be truncated.
     */
    public long getLongValue() {
        return 0;
    }

    /**
     * Return the value of this numeric as a 32-bit long. If the value does not
     * fit in 32 bits, it will be truncated.
     */
    public int getIntValue() {
        return 0;
    }

    public BigInteger getBigIntegerValue() {
        return BigInteger.ZERO;
    }

    public static RubyNumeric newNumeric(Ruby runtime) {
    	return new RubyNumeric(runtime, runtime.getNumeric());
    }

    /*  ================
     *  Utility Methods
     *  ================
     */

    /** rb_num2int, NUM2INT
     *
     */
    public static int num2int(IRubyObject arg) {
        long num = num2long(arg);

        checkInt(arg, num);
        return (int)num;
    }

    /** check_int
     *
     */
    public static void checkInt(IRubyObject arg, long num){
        if (num < Integer.MIN_VALUE) {
            tooSmall(arg, num);
        } else if (num > Integer.MAX_VALUE) {
            tooBig(arg, num);
        }
    }

    private static void tooSmall(IRubyObject arg, long num) {
        throw arg.getRuntime().newRangeError("integer " + num + " too small to convert to `int'");
    }

    private static void tooBig(IRubyObject arg, long num) {
        throw arg.getRuntime().newRangeError("integer " + num + " too big to convert to `int'");
    }

    /**
     * NUM2CHR
     */
    public static byte num2chr(IRubyObject arg) {
        if (arg instanceof RubyString) {
            if (((RubyString) arg).size() > 0) {
                return (byte) ((RubyString) arg).getByteList().get(0);
            }
        }

        return (byte) num2int(arg);
    }

    /** rb_num2long and FIX2LONG (numeric.c)
     *
     */
    public static long num2long(IRubyObject arg) {
        return arg instanceof RubyFixnum ? ((RubyFixnum) arg).getLongValue() : other2long(arg);
    }

    private static long other2long(IRubyObject arg) throws RaiseException {
        if (arg instanceof RubyFloat) return float2long((RubyFloat) arg);
        if (arg instanceof RubyBignum) return RubyBignum.big2long((RubyBignum) arg);
        if (arg.isNil()) {
            throw arg.getRuntime().newTypeError("no implicit conversion from nil to integer");
        }
        return arg.convertToInteger().getLongValue();
    }

    private static long float2long(RubyFloat flt) {
        double aFloat = flt.getDoubleValue();
        if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) Long.MIN_VALUE) {
            return (long) aFloat;
        } else {
            // TODO: number formatting here, MRI uses "%-.10g", 1.4 API is a must?
            throw flt.getRuntime().newRangeError("float " + aFloat + " out of range of integer");
        }
    }

    /**
     * MRI: macro DBL2NUM
     */
    public static IRubyObject dbl2num(Ruby runtime, double val) {
        return RubyFloat.newFloat(runtime, val);
    }

    /**
     * MRI: macro DBL2IVAL
     */
    public static IRubyObject dbl2ival(Ruby runtime, double val) {
        if (fixable(runtime, val)) {
            return RubyFixnum.newFixnum(runtime, (long) val);
        }
        return RubyBignum.newBignorm(runtime, val);
    }

    /** rb_num2dbl and NUM2DBL
     *
     */
    public static double num2dbl(IRubyObject arg) {
        if (arg instanceof RubyFloat) {
            return ((RubyFloat) arg).getDoubleValue();
        }
        final Ruby runtime = arg.getRuntime();
        if (arg instanceof RubyFixnum && !runtime.isFixnumReopened()) {
            return ((RubyFixnum) arg).getDoubleValue();
        }
        if (arg instanceof RubyBignum && runtime.getBignum().searchMethod("to_f").isBuiltin()) {
            return ((RubyBignum) arg).getDoubleValue();
        }
        if (arg instanceof RubyRational && runtime.getRational().searchMethod("to_f").isBuiltin()) {
            return ((RubyRational) arg).getDoubleValue();
        }

        if (arg instanceof RubyBoolean || arg instanceof RubyString || arg.isNil()) {
            throw runtime.newTypeError("can't convert " + arg.inspect() + " into Float");
        }

        IRubyObject val = TypeConverter.convertToType(arg, runtime.getFloat(), "to_f");
        return ((RubyFloat) val).getDoubleValue();
    }

    /** rb_dbl_cmp (numeric.c)
     *
     */
    public static IRubyObject dbl_cmp(Ruby runtime, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) return runtime.getNil();
        return a == b ? RubyFixnum.zero(runtime) : a > b ?
                RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
    }

    public static long fix2long(IRubyObject arg) {
        return ((RubyFixnum) arg).getLongValue();
    }

    public static int fix2int(IRubyObject arg) {
        long num = arg instanceof RubyFixnum ? fix2long(arg) : num2long(arg);
        checkInt(arg, num);
        return (int) num;
    }

    public static int fix2int(RubyFixnum arg) {
        long num = arg.getLongValue();
        checkInt(arg, num);
        return (int) num;
    }

    public static RubyInteger str2inum(Ruby runtime, RubyString str, int base) {
        return str2inum(runtime, str, base, false);
    }

    public static RubyInteger int2fix(Ruby runtime, long val) {
        return RubyFixnum.newFixnum(runtime, val);
    }

    /** rb_num2fix
     *
     */
    public static IRubyObject num2fix(IRubyObject val) {
        if (val instanceof RubyFixnum) {
            return val;
        }
        if (val instanceof RubyBignum) {
            // any BigInteger is bigger than Fixnum and we don't have FIXABLE
            throw val.getRuntime().newRangeError("integer " + val + " out of range of fixnum");
        }
        return RubyFixnum.newFixnum(val.getRuntime(), num2long(val));
    }

    /**
     * Converts a string representation of an integer to the integer value.
     * Parsing starts at the beginning of the string (after leading and
     * trailing whitespace have been removed), and stops at the end or at the
     * first character that can't be part of an integer.  Leading signs are
     * allowed. If <code>base</code> is zero, strings that begin with '0[xX]',
     * '0[bB]', or '0' (optionally preceded by a sign) will be treated as hex,
     * binary, or octal numbers, respectively.  If a non-zero base is given,
     * only the prefix (if any) that is appropriate to that base will be
     * parsed correctly.  For example, if the base is zero or 16, the string
     * "0xff" will be converted to 256, but if the base is 10, it will come out
     * as zero, since 'x' is not a valid decimal digit.  If the string fails
     * to parse as a number, zero is returned.
     *
     * @param runtime  the ruby runtime
     * @param str   the string to be converted
     * @param base  the expected base of the number (for example, 2, 8, 10, 16),
     *              or 0 if the method should determine the base automatically
     *              (defaults to 10). Values 0 and 2-36 are permitted. Any other
     *              value will result in an ArgumentError.
     * @param strict if true, enforce the strict criteria for String encoding of
     *               numeric values, as required by Integer('n'), and raise an
     *               exception when those criteria are not met. Otherwise, allow
     *               lax expression of values, as permitted by String#to_i, and
     *               return a value in almost all cases (excepting illegal radix).
     *               TODO: describe the rules/criteria
     * @return  a RubyFixnum or (if necessary) a RubyBignum representing
     *          the result of the conversion, which will be zero if the
     *          conversion failed.
     */
    public static RubyInteger str2inum(Ruby runtime, RubyString str, int base, boolean strict) {
        ByteList s = str.getByteList();
        return ConvertBytes.byteListToInum(runtime, s, base, strict);
    }

    /**
     * Same as RubyNumeric.str2fnum passing false for strict.
     *
     * @param runtime  the ruby runtime
     * @param arg   the string to be converted
     * @return  a RubyFloat representing the result of the conversion, which
     *          will be 0.0 if the conversion failed.
     */
    public static RubyFloat str2fnum(Ruby runtime, RubyString arg) {
        return str2fnum(runtime, arg, false);
    }

    /**
     * Converts a string representation of a floating-point number to the
     * numeric value.  Parsing starts at the beginning of the string (after
     * leading and trailing whitespace have been removed), and stops at the
     * end or at the first character that can't be part of a number.  If
     * the string fails to parse as a number, 0.0 is returned.
     *
     * @param runtime  the ruby runtime
     * @param arg   the string to be converted
     * @param strict if true, enforce the strict criteria for String encoding of
     *               numeric values, as required by Float('n'), and raise an
     *               exception when those criteria are not met. Otherwise, allow
     *               lax expression of values, as permitted by String#to_f, and
     *               return a value in all cases.
     *               TODO: describe the rules/criteria
     * @return  a RubyFloat representing the result of the conversion, which
     *          will be 0.0 if the conversion failed.
     */
    public static RubyFloat str2fnum(Ruby runtime, RubyString arg, boolean strict) {
        try {
            double value = ConvertDouble.byteListToDouble19(arg.getByteList(), strict);
            return RubyFloat.newFloat(runtime, value);
        }
        catch (NumberFormatException e) {
            if (strict) {
                throw runtime.newArgumentError("invalid value for Float(): "
                        + arg.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return RubyFloat.newFloat(runtime, 0.0);
        }
    }


    /** Numeric methods. (num_*)
     *
     */

    protected final IRubyObject[] getCoerced(ThreadContext context, IRubyObject other, boolean error) {
        final Ruby runtime = context.runtime;
        final IRubyObject $ex = context.getErrorInfo();
        final IRubyObject result;
        try {
            result = sites(context).coerce.call(context, other, other, this);
        }
        catch (RaiseException e) { // e.g. NoMethodError: undefined method `coerce'
            context.setErrorInfo($ex); // restore $!

            if (error) {
                throw runtime.newTypeError(
                        other.getMetaClass().getName() + " can't be coerced into " + getMetaClass().getName());
            }
            return null;
        }

        return coerceResult(runtime, result, true).toJavaArrayMaybeUnsafe();
    }

    protected final IRubyObject callCoerced(ThreadContext context, String method, IRubyObject other, boolean err) {
        IRubyObject[] args = getCoerced(context, other, err);
        if (args == null) return context.nil;
        return args[0].callMethod(context, method, args[1]);
    }

    public final IRubyObject callCoerced(ThreadContext context, String method, IRubyObject other) {
        return callCoerced(context, method, other, false);
    }

    protected final IRubyObject callCoerced(ThreadContext context, CallSite site, IRubyObject other, boolean err) {
        IRubyObject[] args = getCoerced(context, other, err);
        if (args == null) return context.nil;
        IRubyObject car = args[0];
        return site.call(context, car, car, args[1]);
    }

    public final IRubyObject callCoerced(ThreadContext context, CallSite site, IRubyObject other) {
        return callCoerced(context, site, other, false);
    }

    // beneath are rewritten coercions that reflect MRI logic, the aboves are used only by RubyBigDecimal

    /** coerce_body
     *
     */
    protected final IRubyObject coerceBody(ThreadContext context, IRubyObject other) {
        return sites(context).coerce.call(context, other, other, this);
    }

    /** do_coerce
     *
     */
    protected final RubyArray doCoerce(ThreadContext context, IRubyObject other, boolean err) {
        if (!sites(context).respond_to_coerce.respondsTo(context, other, other)) {
            if (err) {
                coerceFailed(context, other);
            }
            return null;
        }
        final IRubyObject $ex = context.getErrorInfo();
        final IRubyObject result;
        try {
            result = coerceBody(context, other);
        }
        catch (RaiseException e) { // e.g. NoMethodError: undefined method `coerce'
            if (context.runtime.getStandardError().isInstance( e.getException() )) {
                context.setErrorInfo($ex); // restore $!
                RubyWarnings warnings = context.runtime.getWarnings();
                warnings.warn("Numerical comparison operators will no more rescue exceptions of #coerce");
                warnings.warn("in the next release. Return nil in #coerce if the coercion is impossible.");
                if (err) {
                    coerceFailed(context, other);
                }
                return null;
            }
            throw e;
        }

        return coerceResult(context.runtime, result, err);
    }

    private static RubyArray coerceResult(final Ruby runtime, final IRubyObject result, final boolean err) {
        if (!(result instanceof RubyArray) || ((RubyArray) result).getLength() != 2 ) {
            if (err) throw runtime.newTypeError("coerce must return [x, y]");

            if (!result.isNil()) {
                RubyWarnings warnings = runtime.getWarnings();
                warnings.warn("Bad return value for #coerce, called by numerical comparison operators.");
                warnings.warn("#coerce must return [x, y]. The next release will raise an error for this.");
            }
            return null;
        }

        return (RubyArray) result;
    }

    /** coerce_rescue
     *
     */
    protected final IRubyObject coerceRescue(ThreadContext context, IRubyObject other) {
        coerceFailed(context, other);
        return context.nil;
    }

    /** coerce_failed
     *
     */
    protected final void coerceFailed(ThreadContext context, IRubyObject other) {
        throw context.runtime.newTypeError(String.format("%s can't be coerced into %s",
                (other.isSpecialConst() ? other.inspect() : other.getMetaClass().getName()), getMetaClass()));
    }

    /** rb_num_coerce_bin
     *  coercion taking two arguments
     */
    @Deprecated
    protected final IRubyObject coerceBin(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, true);
        return (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
    }

    protected final IRubyObject coerceBin(ThreadContext context, CallSite site, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, true);
        IRubyObject car = ary.eltInternal(0);
        return site.call(context, car, car, ary.eltInternal(1));
    }

    /** rb_num_coerce_bit
     *  coercion taking two arguments
     */
    protected final IRubyObject coerceBit(ThreadContext context, String method, IRubyObject other) {
        if (!(other instanceof RubyFixnum) && !(other instanceof RubyBignum)) {
            RubyArray ary = doCoerce(context, other, true);
            IRubyObject x = ary.eltInternal(0);
            IRubyObject y = ary.eltInternal(1);

            if (!(x instanceof RubyFixnum) && !(x instanceof RubyBignum)
                    && !(y instanceof RubyFixnum) && !(y instanceof RubyBignum)) {
                coerceFailed(context, other);
            }
            return x.callMethod(context, method, y);
        }
        return callMethod(context, method, other);
    }

    protected final IRubyObject coerceBit(ThreadContext context, JavaSites.CheckedSites site, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, true);
        final IRubyObject x = ary.eltOk(0);
        IRubyObject y = ary.eltOk(1);
        IRubyObject ret = context.safeRecurse(new ThreadContext.RecursiveFunctionEx<JavaSites.CheckedSites>() {
            @Override
            public IRubyObject call(ThreadContext context, JavaSites.CheckedSites site, IRubyObject obj, boolean recur) {
                if (recur) {
                    throw context.runtime.newNameError("recursive call to " + site.methodName, site.methodName);
                }
                return x.getMetaClass().finvokeChecked(context, x, site, obj);
            }
        }, site, y, site.methodName, true);
        if (ret == null) {
            coerceFailed(context, other);
        }
        return ret;
    }

    /** rb_num_coerce_cmp
     *  coercion used for comparisons
     */
    protected final IRubyObject coerceCmp(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);
        if (ary == null) {
            return context.nil; // MRI does it!
        }
        return (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
    }

    protected final IRubyObject coerceCmp(ThreadContext context, CallSite site, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);
        if (ary == null) {
            return context.nil; // MRI does it!
        }
        IRubyObject car = ary.eltInternal(0);
        return site.call(context, car, car, ary.eltInternal(1));
    }

    /** rb_num_coerce_relop
     *  coercion used for relative operators
     */
    protected final IRubyObject coerceRelOp(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);
        if (ary == null) {
            return RubyComparable.cmperr(this, other);
        }

        return unwrapCoerced(context, method, other, ary);
    }

    protected final IRubyObject coerceRelOp(ThreadContext context, CallSite site, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);
        if (ary == null) {
            return RubyComparable.cmperr(this, other);
        }

        return unwrapCoerced(context, site, other, ary);
    }

    private IRubyObject unwrapCoerced(ThreadContext context, String method, IRubyObject other, RubyArray ary) {
        IRubyObject result = (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
        if (result.isNil()) {
            return RubyComparable.cmperr(this, other);
        }
        return result;
    }

    private IRubyObject unwrapCoerced(ThreadContext context, CallSite site, IRubyObject other, RubyArray ary) {
        IRubyObject car = ary.eltInternal(0);
        IRubyObject result = site.call(context, car, car, ary.eltInternal(1));
        if (result.isNil()) {
            return RubyComparable.cmperr(this, other);
        }
        return result;
    }

    public RubyNumeric asNumeric() {
        return this;
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    /** num_sadded
     *
     */
    @JRubyMethod(name = "singleton_method_added")
    public static IRubyObject sadded(IRubyObject self, IRubyObject name) {
        throw self.getRuntime().newTypeError("can't define singleton method " + name + " for " + self.getType().getName());
    }

    /** num_init_copy
     *
     */
    @Override
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject arg) {
        throw getRuntime().newTypeError("can't copy " + getType().getName());
    }

    /** num_coerce
     *
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(IRubyObject other) {
        final Ruby runtime = getRuntime();
        if (getMetaClass() == other.getMetaClass()) return runtime.newArray(other, this);

        IRubyObject cdr = RubyKernel.new_float(runtime, this);
        IRubyObject car = RubyKernel.new_float(runtime, other);

        return runtime.newArray(car, cdr);
    }

    /** num_uplus
     *
     */
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }

    /** num_imaginary
     *
     */
    @JRubyMethod(name = "i")
    public IRubyObject num_imaginary(ThreadContext context) {
        return RubyComplex.newComplexRaw(context.runtime, RubyFixnum.zero(context.runtime), this);
    }

    /** num_uminus
     *
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus(ThreadContext context) {
        RubyArray ary = RubyFixnum.zero(context.runtime).doCoerce(context, this, true);
        IRubyObject car = ary.eltInternal(0);
        return numFuncall(context, car, sites(context).op_minus, ary.eltInternal(1));
    }

    // MRI: rb_int_plus and others, handled by polymorphism
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        return coerceBin(context, sites(context).op_plus, other);
    }

    /** num_cmp
     *
     */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(IRubyObject other) {
        if (this == other) { // won't hurt fixnums
            return RubyFixnum.zero(getRuntime());
        }
        return getRuntime().getNil();
    }

    /** num_eql
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        if (getClass() != other.getClass()) return context.runtime.getFalse();
        return equalInternal(context, this, other) ? context.runtime.getTrue() : context.runtime.getFalse();
    }

    /** num_quo
     *
     */
    @JRubyMethod(name = "quo")
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        return RubyRational.numericQuo(context, this, other);
    }

    @Deprecated
    public final IRubyObject quo_19(ThreadContext context, IRubyObject other) {
        return quo(context, other);
    }

    /**
     * MRI: num_div
     */
    @JRubyMethod(name = "div")
    public IRubyObject div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric) {
            RubyNumeric numeric = (RubyNumeric) other;
            if (numeric.zero_p(context).isTrue()) {
                throw context.runtime.newZeroDivisionError();
            }
        }
        IRubyObject quotient = numFuncall(context, this, sites(context).op_quo, other);
        return sites(context).floor.call(context, quotient, quotient);
    }

    /**
     * MRI: rb_int_idiv and overrides
     */
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        return div(context, other);
    }

    /** num_divmod
     *
     */
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        return RubyArray.newArray(context.runtime, div(context, other), modulo(context, other));
    }

    /** num_fdiv */
    @JRubyMethod(name = "fdiv")
    public IRubyObject fdiv(ThreadContext context, IRubyObject other) {
        RubyFloat value = convertToFloat();
        return sites(context).op_quo.call(context, value, value, other);
    }

    /** num_modulo
     *
     */
    @JRubyMethod(name = "modulo")
    public IRubyObject modulo(ThreadContext context, IRubyObject other) {
        IRubyObject div = numFuncall(context, this, sites(context).div, other);
        IRubyObject product = sites(context).op_times.call(context, other, other, div);
        return sites(context).op_minus.call(context, this, this, product);
    }

    /** num_remainder
     *
     */
    @JRubyMethod(name = "remainder")
    public IRubyObject remainder(ThreadContext context, IRubyObject dividend) {
        IRubyObject z = numFuncall(context, this, sites(context).op_mod, dividend);
        RubyFixnum zero = RubyFixnum.zero(context.runtime);

        if (!equalInternal(context, z, zero) &&
                ((isNegative(context).isTrue() &&
                        positiveIntP(context, dividend).isTrue()) ||
                (isPositive(context).isTrue() &&
                        negativeIntP(context, dividend).isTrue()))) {
            return sites(context).op_minus.call(context, z, z, dividend);
        }
        return z;
    }

    /** num_abs
     *
     */
    @JRubyMethod(name = "abs")
    public IRubyObject abs(ThreadContext context) {
        if (sites(context).op_lt.call(context, this, this, RubyFixnum.zero(context.runtime)).isTrue()) {
            return sites(context).op_uminus.call(context, this, this);
        }
        return this;
    }

    /** num_abs/1.9
     *
     */
    @JRubyMethod(name = "magnitude")
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    /** num_to_int
     *
     */
    @JRubyMethod(name = "to_int")
    public IRubyObject to_int(ThreadContext context) {
        return numFuncall(context, this, sites(context).to_i);
    }

    /** num_real_p
    *
    */
    @JRubyMethod(name = "real?")
    public IRubyObject scalar_p() {
        return getRuntime().getTrue();
    }

    /** num_int_p
     *
     */
    @JRubyMethod(name = "integer?")
    public IRubyObject integer_p() {
        return getRuntime().getFalse();
    }

    /** num_zero_p
     *
     */
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p(ThreadContext context) {
        final Ruby runtime = context.runtime;
        return equalInternal(context, this, RubyFixnum.zero(runtime)) ? runtime.getTrue() : runtime.getFalse();
    }

    /** num_nonzero_p
     *
     */
    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p(ThreadContext context) {
        if (numFuncall(context, this, sites(context).zero).isTrue()) {
            return context.nil;
        }
        return this;
    }

    /**
     * MRI: num_floor
     */
    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context) {
        return convertToFloat().floor(context);
    }

    /**
     * MRI: num_ceil
     */
    @JRubyMethod(name = "ceil")
    public IRubyObject ceil(ThreadContext context) {
        return convertToFloat().ceil(context);
    }

    /**
     * MRI: num_round
     */
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context) {
        return convertToFloat().round(context);
    }

    /**
     * MRI: num_truncate
     */
    @JRubyMethod(name = "truncate")
    public IRubyObject truncate(ThreadContext context) {
        return convertToFloat().truncate(context);
    }

    // TODO: Fold kwargs into the @JRubyMethod decorator
    static final String[] validStepArgs = new String[] {"to", "by"};

    /**
     * num_step
     */
    @JRubyMethod(optional = 2)
    public IRubyObject step(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) {
            return enumeratorizeWithSize(context, this, "step", args, stepSizeFn(context, this, args));
        }

        IRubyObject[] scannedArgs = scanStepArgs(context, args);
        return stepCommon(context, scannedArgs[0], scannedArgs[1], block);
    }

    /** num_step_scan_args
     *
     */
    private IRubyObject[] scanStepArgs(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        IRubyObject to = context.nil;
        IRubyObject step = runtime.newFixnum(1);

        if (args.length >= 1) to = args[0];
        if (args.length >= 2) step = args[1];

        // TODO: Fold kwargs into the @JRubyMethod decorator
        IRubyObject[] kwargs = ArgsUtil.extractKeywordArgs(context, args, validStepArgs);

        if (kwargs != null) {
            to = kwargs[0];
            step = kwargs[1];

            if(!to.isNil() && args.length > 1) {
                throw runtime.newArgumentError("to is given twice");
            }
            if(!step.isNil() && args.length > 2) {
                throw runtime.newArgumentError("step is given twice");
            }
        } else {
            if (step.isNil()) {
                throw runtime.newTypeError("step must be numeric");
            }
            if (RubyBasicObject.equalInternal(context, step, RubyFixnum.zero(runtime))) {
                throw runtime.newArgumentError("step can't be 0");
            }
        }

        if (step.isNil()) {
            step = RubyFixnum.one(runtime);
        }

        boolean desc = numStepNegative(runtime, context, step);
        if (to.isNil()) {
            to = desc ?
                    RubyFloat.newFloat(runtime, Double.NEGATIVE_INFINITY) :
                    RubyFloat.newFloat(runtime, Double.POSITIVE_INFINITY);
        }

        return new IRubyObject[] {to, step};
    }

    private boolean numStepNegative(Ruby runtime, ThreadContext context, IRubyObject num) {
        if (num instanceof RubyFixnum) {
            if (sites(context).op_lt.isBuiltin(runtime.getInteger())) {
                return ((RubyFixnum) num).getLongValue() < 0;
            }
        }
        else if (num instanceof RubyBignum) {
            if (sites(context).op_lt.isBuiltin(runtime.getInteger())) {
                return ((RubyBignum) num).isNegative(context).isTrue();
            }
        }
        IRubyObject r = UNDEF;
        try {
            context.setExceptionRequiresBacktrace(false);
            r = stepCompareWithZero(context, num);
        } catch (RaiseException re) {
        } finally {
            context.setExceptionRequiresBacktrace(true);
        }
        if (r == UNDEF) {
            coerceFailed(context, num);
        }
        return !r.isTrue();
    }

    private IRubyObject stepCompareWithZero(ThreadContext context, IRubyObject num) {
        IRubyObject zero = RubyFixnum.zero(context.runtime);
        return Helpers.invokeChecked(context, num, sites(context).op_gt_checked, zero);
    }

    private IRubyObject stepCommon(ThreadContext context, IRubyObject to, IRubyObject step, Block block) {
        Ruby runtime = context.runtime;
        if (this instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            fixnumStep(context, runtime, (RubyFixnum) this,
                                         this.getLongValue(),
                                         ((RubyFixnum)to).getLongValue(),
                                         ((RubyFixnum)step).getLongValue(),
                                          block);
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            floatStep(context, runtime, this, to, step, false, block);
        } else {
            duckStep(context, runtime, this, to, step, block);
        }
        return this;
    }

    private static void fixnumStep(ThreadContext context, Ruby runtime, RubyFixnum fromObj, long from, long to, long step, Block block) {
        // We must avoid integer overflows in "i += step".
        if (step == 0) {
            for (;;) {
                block.yield(context, fromObj);
            }
        } else if (step > 0) {
            long tov = Long.MAX_VALUE - step;
            if (to < tov) tov = to;
            long i;
            for (i = from; i <= tov; i += step) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i <= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        } else {
            long tov = Long.MIN_VALUE - step;
            if (to > tov) tov = to;
            long i;
            for (i = from; i >= tov; i += step) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i >= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        }
    }

    static void floatStep(ThreadContext context, Ruby runtime, IRubyObject from, IRubyObject to, IRubyObject step, boolean excl, Block block) {
        double beg = num2dbl(from);
        double end = num2dbl(to);
        double unit = num2dbl(step);

        double n = floatStepSize(beg, end, unit, excl);

        if (Double.isInfinite(unit)) {
            if (n != 0) block.yield(context, from);
        } else if (unit == 0) {
            while(true) {
                block.yield(context, from);
            }
        } else {
            for (long i = 0; i < n; i++){
                double d = i * unit + beg;
                if (unit >= 0 ? end < d : d < end) {
                    d = end;
                }
                block.yield(context, RubyFloat.newFloat(runtime, d));
            }
        }
    }

    private static void duckStep(ThreadContext context, Ruby runtime, IRubyObject from, IRubyObject to, IRubyObject step, Block block) {
        IRubyObject i = from;

        CallSite cmpSite = sites(context).op_gt.call(context, step, step, RubyFixnum.newFixnum(context.runtime, 0)).isTrue() ? sites(context).op_gt : sites(context).op_lt;
        if(sites(context).op_equals.call(context, step, step, RubyFixnum.newFixnum(context.runtime, 0)).isTrue()) {
            cmpSite = sites(context).op_equals;
        }

        while (true) {
            if (cmpSite.call(context, i, i, to).isTrue()) break;
            block.yield(context, i);
            i = sites(context).op_plus.call(context, i, i, step);
        }
    }

    public static RubyNumeric intervalStepSize(ThreadContext context, IRubyObject from, IRubyObject to, IRubyObject step, boolean excludeLast) {
        Ruby runtime = context.runtime;

        if (from instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            long diff = ((RubyFixnum) step).getLongValue();
            if (diff == 0) {
                return RubyFloat.newFloat(runtime, Double.POSITIVE_INFINITY);
            }
            long toLong = ((RubyFixnum) to).getLongValue();
            long fromLong = ((RubyFixnum) from).getLongValue();
            long delta = toLong - fromLong;
            if (!Helpers.subtractionOverflowed(toLong, fromLong, delta)) {
                if (diff < 0) {
                    diff = -diff;
                    delta = -delta;
                }
                if (excludeLast) {
                    delta--;
                }
                if (delta < 0) {
                    return runtime.newFixnum(0);
                }

                long steps = delta / diff;
                long stepSize = steps + 1;
                if (stepSize != Long.MIN_VALUE) {
                    return new RubyFixnum(runtime, delta / diff + 1);
                } else {
                    return RubyBignum.newBignum(runtime, BigInteger.valueOf(steps).add(BigInteger.ONE));
                }
            }
            // fall through to duck-typed logic
        } else if (from instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            double n = floatStepSize(from.convertToFloat().getDoubleValue(), to.convertToFloat().getDoubleValue(), step.convertToFloat().getDoubleValue(), excludeLast);

            if (Double.isInfinite(n)) {
                return runtime.newFloat(n);
            } else {
                return runtime.newFloat(n).convertToInteger();
            }
        }

        JavaSites.NumericSites sites = sites(context);
        CallSite op_gt = sites.op_gt;
        CallSite op_lt = sites.op_lt;
        CallSite cmpSite = op_gt;

        RubyFixnum zero = RubyFixnum.zero(runtime);
        IRubyObject comparison = zero.coerceCmp(context, sites.op_cmp, step);

        switch (RubyComparable.cmpint(context, op_gt, op_lt, comparison, step, zero)) {
            case 0:
                return RubyFloat.newFloat(runtime, Float.POSITIVE_INFINITY);
            case 1:
                cmpSite = op_lt;
                break;
        }

        if (cmpSite.call(context, from, from, to).isTrue()) {
            return RubyFixnum.zero(runtime);
        }

        IRubyObject deltaObj = sites.op_minus.call(context, to, to, from);
        IRubyObject result = sites.div.call(context, deltaObj, deltaObj, step);
        IRubyObject timesPlus = sites.op_plus.call(context, from, from, sites.op_times.call(context, result, result, step));
        if (!excludeLast || cmpSite.call(context, timesPlus, timesPlus, to).isTrue()) {
            result = sites.op_plus.call(context, result, result, RubyFixnum.newFixnum(runtime, 1));
        }
        return (RubyNumeric) result;
    }

    private SizeFn stepSizeFn(final ThreadContext context, final IRubyObject from, final IRubyObject[] args) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                IRubyObject[] scannedArgs = scanStepArgs(context, args);
                return intervalStepSize(context, from, scannedArgs[0], scannedArgs[1], false);
            }
        };
    }

    /**
     * Returns the number of unit-sized steps between the given beg and end.
     *
     * NOTE: the returned value is either Double.POSITIVE_INFINITY, or a rounded value appropriate to be cast to a long
     */
    public static double floatStepSize(double beg, double end, double unit, boolean excludeLast) {
        double n = (end - beg)/unit;
        double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;

        if (Double.isInfinite(unit)) {
            if (unit > 0) {
                return beg <= end ? 1 : 0;
            } else {
                return end <= beg ? 1 : 0;
            }
        }

        if (unit == 0) {
            return Float.POSITIVE_INFINITY;
        }

        if (err > 0.5) err = 0.5;
        if (excludeLast) {
            if (n <= 0) {
                return 0;
            }
            if (n < 1) {
                n = 0;
            } else {
                n = Math.floor(n - err);
            }
        } else {
            if (n < 0) {
                return 0;
            }
            n = Math.floor(n + err);
        }
        return n + 1;
    }

    /** num_equal, doesn't override RubyObject.op_equal
     *
     */
    protected final IRubyObject op_num_equal(ThreadContext context, IRubyObject other) {
        // it won't hurt fixnums
        if (this == other)  return context.runtime.getTrue();

        return numFuncall(context, other, sites(context).op_equals, this);
    }

    /** num_numerator
     *
     */
    @JRubyMethod(name = "numerator")
    public IRubyObject numerator(ThreadContext context) {
        IRubyObject rational = RubyRational.newRationalConvert(context, this);
        return sites(context).numerator.call(context, rational, rational);
    }

    /** num_denominator
     *
     */
    @JRubyMethod(name = "denominator")
    public IRubyObject denominator(ThreadContext context) {
        IRubyObject rational = RubyRational.newRationalConvert(context, this);
        return sites(context).denominator.call(context, rational, rational);
    }

    /** numeric_to_c
     *
     */
    @JRubyMethod(name = "to_c")
    public IRubyObject to_c(ThreadContext context) {
        return RubyComplex.newComplexCanonicalize(context, this);
    }

    /** numeric_real
     *
     */
    @JRubyMethod(name = "real")
    public IRubyObject real(ThreadContext context) {
        return this;
    }

    /** numeric_image
     *
     */
    @JRubyMethod(name = {"imaginary", "imag"})
    public IRubyObject image(ThreadContext context) {
        return RubyFixnum.zero(context.runtime);
    }

    /** numeric_abs2
     *
     */
    @JRubyMethod(name = "abs2")
    public IRubyObject abs2(ThreadContext context) {
        return f_mul(context, this, this);
    }

    /** numeric_arg
     *
     */
    @JRubyMethod(name = {"arg", "angle", "phase"})
    public IRubyObject arg(ThreadContext context) {
        double value = this.getDoubleValue();
        if (Double.isNaN(value)) {
            return this;
        }
        if (f_negative_p(context, this) || (value == 0.0 && 1/value == Double.NEGATIVE_INFINITY)) {
            // negative or -0.0
            return context.runtime.getMath().getConstant("PI");
        }
        return RubyFixnum.zero(context.runtime);
    }

    /** numeric_rect
     *
     */
    @JRubyMethod(name = {"rectangular", "rect"})
    public IRubyObject rect(ThreadContext context) {
        return context.runtime.newArray(this, RubyFixnum.zero(context.runtime));
    }

    /** numeric_polar
     *
     */
    @JRubyMethod(name = "polar")
    public IRubyObject polar(ThreadContext context) {
        return context.runtime.newArray(f_abs(context, this), f_arg(context, this));
    }

    /** numeric_real
     *
     */
    @JRubyMethod(name = {"conjugate", "conj"})
    public IRubyObject conjugate(ThreadContext context) {
        return this;
    }

    @Override
    public Object toJava(Class target) {
        return JavaUtil.getNumericConverter(target).coerce(this, target);
    }

    @Deprecated // not-used
    public static class InvalidIntegerException extends NumberFormatException {
        private static final long serialVersionUID = 55019452543252148L;

        public InvalidIntegerException() {
            super();
        }
        public InvalidIntegerException(String message) {
            super(message);
        }
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    @Deprecated // not-used
    public static class NumberTooLargeException extends NumberFormatException {
        private static final long serialVersionUID = -1835120694982699449L;
        public NumberTooLargeException() {
            super();
        }
        public NumberTooLargeException(String message) {
            super(message);
        }
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    /** num_negative_p
     *
     */
    @JRubyMethod(name = "negative?")
    public IRubyObject isNegative(ThreadContext context) {
        return compareWithZero(context, this, sites(context).op_lt_checked);

    }
    /** num_positive_p
     *
     */
    @JRubyMethod(name = "positive?")
    public IRubyObject isPositive(ThreadContext context) {
        return compareWithZero(context, this, sites(context).op_gt_checked);
    }

    protected static IRubyObject negativeIntP(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyNumeric) {
            return ((RubyNumeric) obj).isNegative(context);
        }
        return compareWithZero(context, obj, sites(context).op_lt_checked);
    }

    protected static IRubyObject positiveIntP(ThreadContext context, IRubyObject obj) {
        if (obj instanceof RubyNumeric) {
            return ((RubyNumeric) obj).isPositive(context);
        }
        return compareWithZero(context, obj, sites(context).op_gt_checked);
    }

    protected static IRubyObject compareWithZero(ThreadContext context, IRubyObject num, JavaSites.CheckedSites site) {
        IRubyObject zero = RubyFixnum.zero(context.runtime);
        IRubyObject r = num.getMetaClass().finvokeChecked(context, num, site, zero);
        if (r == null) {
            RubyComparable.cmperr(num, zero);
        }
        return r;
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p(ThreadContext context) {
        return context.runtime.getTrue();
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p(ThreadContext context) {
        return context.runtime.getNil();
    }

    public static IRubyObject numFuncall(ThreadContext context, IRubyObject x, CallSite site) {
        return context.safeRecurse(new NumFuncall0(), site, x, site.methodName, true);
    }

    public static IRubyObject numFuncall(ThreadContext context, final IRubyObject x, CallSite site, final IRubyObject value) {
        return context.safeRecurse(new NumFuncall1(value), site, x, site.methodName, true);
    }

    // MRI: macro FIXABLE, RB_FIXABLE
    // Note: this does additional checks for inf and nan
    public static boolean fixable(Ruby runtime, double f) {
        if (Double.isNaN(f) || Double.isInfinite(f))  {
            throw runtime.newFloatDomainError(Double.toString(f));
        }
        long l = (long) f;
        if (l == RubyFixnum.MIN ||
                l == RubyFixnum.MAX){
            BigInteger bigint = BigDecimal.valueOf(f).toBigInteger();
            return posFixable(bigint) && negFixable(bigint);
        } else {
            return posFixable(f) && negFixable(f);
        }
    }

    // MRI: macro POSFIXABLE, RB_POSFIXABLE
    public static boolean posFixable(BigInteger f) {
        return f.compareTo(RubyBignum.LONG_MAX) <= 0;
    }

    // MRI: macro NEGFIXABLE, RB_NEGFIXABLE
    public static boolean negFixable(BigInteger f) {
        return f.compareTo(RubyBignum.LONG_MIN) >= 0;
    }

    // MRI: macro POSFIXABLE, RB_POSFIXABLE
    public static boolean posFixable(double l) {
        return l <= RubyFixnum.MAX;
    }

    // MRI: macro NEGFIXABLE, RB_NEGFIXABLE
    public static boolean negFixable(double l) {
        return l >= RubyFixnum.MIN;
    }

    private static class NumFuncall1 implements ThreadContext.RecursiveFunctionEx<CallSite> {
        private final IRubyObject value;

        public NumFuncall1(IRubyObject value) {
            this.value = value;
        }

        @Override
        public IRubyObject call(ThreadContext context, CallSite site, IRubyObject obj, boolean recur) {
            if (recur) {
                String name = site.methodName;
                if (name.length() > 0 && Character.isLetterOrDigit(name.charAt(0))) {
                    throw context.runtime.newNameError(name, obj, name);
                } else {
                    throw context.runtime.newNameError(name, obj, name);
                }
            }
            return site.call(context, obj, obj, value);
        }
    }

    private static class NumFuncall0 implements ThreadContext.RecursiveFunctionEx<CallSite> {
        @Override
        public IRubyObject call(ThreadContext context, CallSite site, IRubyObject obj, boolean recur) {
            if (recur) {
                String name = site.methodName;
                if (name.length() > 0 && Character.isLetterOrDigit(name.charAt(0))) {
                    throw context.runtime.newNameError(name, obj, name);
                } else if (name.length() == 2 && name.charAt(1) == '@') {
                    throw context.runtime.newNameError(name, obj, name.substring(0,1));
                } else {
                    throw context.runtime.newNameError(name, obj, name);
                }
            }
            return site.call(context, obj, obj);
        }
    }

    @Deprecated
    public IRubyObject floor() {
        return floor(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject ceil() {
        return ceil(getRuntime().getCurrentContext());
    }

    @Deprecated
    public IRubyObject round() {
        return round(getRuntime().getCurrentContext());
    }

    /** num_truncate
     *
     */
    @Deprecated
    public IRubyObject truncate() {
        return truncate(getRuntime().getCurrentContext());
    }

    private static JavaSites.NumericSites sites(ThreadContext context) {
        return context.sites.Numeric;
    }

    @Deprecated
    public static RubyFloat str2fnum19(Ruby runtime, RubyString arg, boolean strict) {
        return str2fnum(runtime, arg, strict);
    }

    @Deprecated
    public final IRubyObject div19(ThreadContext context, IRubyObject other) {
        return div(context, other);
    }

    @Deprecated
    public final IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        return divmod(context, other);
    }

    @Deprecated
    public final IRubyObject modulo19(ThreadContext context, IRubyObject other) {
        return modulo(context, other);
    }
}
