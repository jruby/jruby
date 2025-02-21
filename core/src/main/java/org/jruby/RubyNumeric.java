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

import org.jruby.RubyEnumerator.SizeFn;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.JRubyAPI;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.ConvertDouble;
import org.jruby.util.TypeConverter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.api.Access.floatClass;
import static org.jruby.api.Access.integerClass;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.util.Numeric.f_abs;
import static org.jruby.util.Numeric.f_arg;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negative_p;
import static org.jruby.util.Numeric.f_to_r;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;

/**
 * Base class for all numerical types in ruby.
 */
// TODO: Numeric.new works in Ruby and it does here too.  However trying to use
//   that instance in a numeric operation should generate an ArgumentError. Doing
//   this seems so pathological I do not see the need to fix this now.
@JRubyClass(name="Numeric", include="Comparable")
public class RubyNumeric extends RubyObject {
    public static RubyClass createNumericClass(ThreadContext context, RubyClass Object, RubyModule Comparable) {
        return defineClass(context, "Numeric", Object, RubyNumeric::new).
                reifiedClass(RubyNumeric.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyNumeric.class)).
                classIndex(ClassIndex.NUMERIC).
                include(context, Comparable).
                defineMethods(context, RubyNumeric.class);
    }

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

        if (halfArg == context.nil) return RoundingMode.HALF_UP;

        if (halfArg instanceof RubySymbol || halfArg instanceof RubyString) {
            RubyString asString = halfArg.asString();
            if (!asString.getEncoding().isAsciiCompatible()) {
                throw context.runtime.newEncodingCompatibilityError("ASCII incompatible encoding: " + asString.getEncoding());
            }

            switch (halfArg.asJavaString()) {
                case "up":
                    return RoundingMode.HALF_UP;
                case "even":
                    return RoundingMode.HALF_EVEN;
                case "down":
                    return RoundingMode.HALF_DOWN;
            }
        }

        throw argumentError(context, "invalid rounding mode: " + halfArg);
    }

    // The implementations of these are all bonus (see above)

    /**
     * Return the value of this numeric as a 64-bit long. If the value does not
     * fit in 64 bits, it will be truncated.
     * @deprecated Use {@link org.jruby.RubyNumeric#asLong(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public long getLongValue() {
        return asLong(getCurrentContext());
    }

    /**
     * Return the value of this numeric as a 32-bit long. If the value does not
     * fit in 32 bits, it will be truncated.
     * @deprecated Use {@link org.jruby.RubyNumeric#asInt(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public int getIntValue() { return asInt(getRuntime().getCurrentContext()); }


    /**
     * Return a BigInteger representation of this numerical value
     *
     * @param context the current thread context
     * @return a BigInteger
     */
    @JRubyAPI
    public BigInteger asBigInteger(ThreadContext context) {
        return BigInteger.valueOf(asLong(context));
    }

    /**
     * Return a double representation of this numerical value
     *
     * @param context the current thread context
     * @return a double
     */
    @JRubyAPI
    public double asDouble(ThreadContext context) {
        return asLong(context);
    }

    /**
     * Returns the value of this numeric and a java int.
     */
    @JRubyAPI
    public int asInt(ThreadContext context) {
        return (int) asLong(context);
    }

    /**
     * Return the value of this numeric as a 64-bit long.
     */
    @JRubyAPI
    public long asLong(ThreadContext context) {
        return 0;
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.RubyNumeric#asDouble(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public double getDoubleValue() {
        return asDouble(getCurrentContext());
    }

    @Deprecated(since = "10.0")
    public BigInteger getBigIntegerValue() {
        return asBigInteger(getCurrentContext());
    }

    public static RubyNumeric newNumeric(Ruby runtime) {
    	return new RubyNumeric(runtime, runtime.getNumeric());
    }

    /*  ================
     *  Utility Methods
     *  ================
     */

    /** rb_num2int, NUM2INT
     * if you know it is Integer use {@link org.jruby.api.Convert#toInt(ThreadContext, IRubyObject)}.
     */
    @Deprecated(since = "10.0")
    public static int num2int(IRubyObject arg) {
        long num = num2long(arg);

        checkInt(arg, num);
        return (int) num;
    }

    /** check_int
     *
     */
    public static int checkInt(final Ruby runtime, long num) {
        if (((int) num) != num) {
            checkIntFail(runtime, num);
        }
        return (int) num;
    }

    public static void checkInt(IRubyObject arg, long num) {
        if (((int) num) != num) {
            checkIntFail(arg.getRuntime(), num);
        }
    }

    private static void checkIntFail(Ruby runtime, long num) {
        if (num < Integer.MIN_VALUE) {
            throw runtime.newRangeError("integer " + num + " too small to convert to 'int'");
        } else {
            throw runtime.newRangeError("integer " + num + " too big to convert to 'int'");
        }
    }

    /**
     * NUM2CHR
     */
    @Deprecated(since = "10.0")
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
        return arg instanceof RubyFixnum ? ((RubyFixnum) arg).value : other2long(arg);
    }

    @Deprecated(since = "10.0")
    private static long other2long(IRubyObject arg) throws RaiseException {
        if (arg instanceof RubyFloat flote) return float2long(flote);
        if (arg instanceof RubyBignum bignum) return RubyBignum.big2long(bignum);

        var context = arg.getRuntime().getCurrentContext();
        if (arg.isNil()) throw typeError(context, "no implicit conversion from nil to integer");

        return ((RubyInteger) TypeConverter.convertToType(arg, integerClass(context), "to_int")).asLong(context);
    }

    @Deprecated(since = "10.0")
    public static long float2long(RubyFloat flt) {
        final double aFloat = flt.value;
        if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) Long.MIN_VALUE) {
            return (long) aFloat;
        }
        // TODO: number formatting here, MRI uses "%-.10g", 1.4 API is a must?
        throw flt.getRuntime().newRangeError("float " + aFloat + " out of range of integer");
    }

    /**
     * Convert the given value into an unsigned long, encoded as a signed long.
     *
     * Because we can't represent an unsigned long directly in Java, callers of this code must deal with the signed
     * long bits accordingly.
     *
     * @param arg the argument to convert
     * @return an unsigned long encoded as a signed long, or raise an error if out of range
     */
    public static long num2ulong(IRubyObject arg) {
        // loop until we have a Numeric
        while (true) {
            if (arg instanceof RubyFixnum) {
                return ((RubyFixnum) arg).value;
            } else if (arg instanceof RubyBignum) {
                return RubyBignum.big2ulong((RubyBignum) arg);
            } else if (arg instanceof RubyFloat) {
                return float2ulong((RubyFloat) arg);
            } else {
                if (arg.isNil()) throw typeError(arg.getRuntime().getCurrentContext(), "no implicit conversion from nil to integer");
                arg = arg.convertToInteger();
                // loop again
            }
        }
    }

    /**
     * Convert the given RubyFloat into an unsigned long, encoded as a signed long.
     *
     * Because we can't represent an unsigned long directly in Java, callers of this code must deal with the signed
     * long bits accordingly.
     *
     * @param flt the argument to convert
     * @return an unsigned long encoded as a signed long, or raise an error if out of range
     */
    public static long float2ulong(RubyFloat flt) {
        final double aFloat = flt.value;

        if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) 0) {
            BigDecimal bd = BigDecimal.valueOf(aFloat);
            BigInteger bi = bd.toBigInteger();
            return bi.longValue();
        }
        // TODO: number formatting here, MRI uses "%-.10g", 1.4 API is a must?
        throw flt.getRuntime().newRangeError("float " + aFloat + " out of range of integer");
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
    public static RubyInteger dbl2ival(Ruby runtime, double val) {
        // MRI: macro FIXABLE, RB_FIXABLE (inlined + adjusted) :
        if (Double.isNaN(val) || Double.isInfinite(val))  {
            throw runtime.newFloatDomainError(Double.toString(val));
        }

        final long fix = (long) val;
        if (fix == RubyFixnum.MIN || fix == RubyFixnum.MAX) {
            BigInteger big = BigDecimal.valueOf(val).toBigInteger();
            if (posFixable(big) && negFixable(big)) {
                return RubyFixnum.newFixnum(runtime, fix);
            }
        }
        else if (posFixable(val) && negFixable(val)) {
            return RubyFixnum.newFixnum(runtime, fix);
        }
        return RubyBignum.newBignorm(runtime, val);
    }

    public static double num2dbl(IRubyObject arg) {
        return num2dbl(arg.getRuntime().getCurrentContext(), arg);
    }

    /** rb_num2dbl and NUM2DBL
     *
     */
    public static double num2dbl(ThreadContext context, IRubyObject arg) {
        switch (((RubyBasicObject) arg).getNativeClassIndex()) {
            case FLOAT:
                return ((RubyFloat) arg).value;
            case FIXNUM:
                if (context.sites.Fixnum.to_f.isBuiltin(arg)) return ((RubyNumeric) arg).asDouble(context);
                break;
            case BIGNUM:
                if (context.sites.Bignum.to_f.isBuiltin(arg)) return ((RubyBignum) arg).asDouble(context);
                break;
            case RATIONAL:
                if (context.sites.Rational.to_f.isBuiltin(arg)) return ((RubyRational) arg).asDouble(context);
                break;
            case STRING:
                throw typeError(context, "no implicit conversion to float from string");
            case NIL:
                throw typeError(context, "no implicit conversion to float from nil");
            case TRUE:
                throw typeError(context, "no implicit conversion to float from true");
            case FALSE:
                throw typeError(context, "no implicit conversion to float from false");
        }
        IRubyObject val = TypeConverter.convertToType(arg, floatClass(context), "to_f");
        return ((RubyFloat) val).value;
    }

    /** rb_dbl_cmp (numeric.c)
     *
     */
    public static IRubyObject dbl_cmp(Ruby runtime, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) return runtime.getNil();
        return a == b ? RubyFixnum.zero(runtime) : a > b ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
    }

    /**
     * @param arg
     * @return
     * @deprecated Use {@link RubyFixnum#getValue()}
     */
    @Deprecated(since = "10.0")
    public static long fix2long(IRubyObject arg) {
        return ((RubyFixnum) arg).value;
    }

    @Deprecated(since = "10.0")
    public static int fix2int(IRubyObject arg) {
        long num = arg instanceof RubyFixnum fixnum ? fixnum.getValue() : num2long(arg);
        checkInt(arg, num);
        return (int) num;
    }

    @Deprecated(since = "10.0")
    public static int fix2int(RubyFixnum arg) {
        long num = arg.value;
        checkInt(arg, num);
        return (int) num;
    }

    @Deprecated(since = "10.0")
    public static RubyInteger str2inum(Ruby runtime, RubyString str, int base) {
        return (RubyInteger) str2inum(runtime, str, base, false, true);
    }

    /**
     * @param runtime
     * @param val
     * @return
     * @deprecated Use {@link org.jruby.api.Convert#asFixnum(ThreadContext, long)} instead.
     */
    @Deprecated(since = "10.0")
    public static RubyNumeric int2fix(Ruby runtime, long val) {
        return RubyFixnum.newFixnum(runtime, val);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject num2fix(IRubyObject val) {
        return num2fix(((RubyBasicObject) val).getCurrentContext(), val);
    }

    @Deprecated(since = "10.0")
    // mri: rb_num2fix (appears unused in current MRI source)
    public static IRubyObject num2fix(ThreadContext context, IRubyObject val) {
        return switch(val) {
            case RubyFixnum fixnum -> fixnum;
            case RubyBignum bignum -> throw rangeError(context, "integer " + bignum + " out of range of fixnum");
            default -> asFixnum(context, toLong(context, val));
        };
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
    public static IRubyObject str2inum(Ruby runtime, RubyString str, int base, boolean strict, boolean exception) {
        if (!str.getEncoding().isAsciiCompatible()) {
            throw runtime.newEncodingCompatibilityError("ASCII incompatible encoding: " + str.getEncoding());
        }
        ByteList s = str.getByteList();
        return ConvertBytes.byteListToInum(runtime, s, base, strict, exception);
    }

    @Deprecated(since = "10.0")
    public static RubyInteger str2inum(Ruby runtime, RubyString str, int base, boolean strict) {
        return (RubyInteger) str2inum(runtime, str, base, strict, true);
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
    public static IRubyObject str2fnum(Ruby runtime, RubyString arg, boolean strict, boolean exception) {
        var context = runtime.getCurrentContext();
        try {
            ByteList bytes = arg.getByteList();
            double value = ConvertDouble.byteListToDouble(bytes, strict);
            return asFloat(context, value);
        } catch (NumberFormatException e) {
            if (strict) {
                if (!exception) return context.nil;
                throw argumentError(context, "invalid value for Float(): " + arg.callMethod(context, "inspect").toString());
            }
            return RubyFloat.newFloat(runtime, 0.0);
        }
    }

    public static RubyFloat str2fnum(Ruby runtime, RubyString arg, boolean strict) {
        return (RubyFloat) str2fnum(runtime, arg, strict, true);
    }


    /** Numeric methods. (num_*)
     *
     */

    protected final IRubyObject[] getCoerced(ThreadContext context, IRubyObject other, boolean error) {
        final IRubyObject $ex = context.getErrorInfo();
        try {
            IRubyObject result = sites(context).coerce.call(context, other, other, this);
            return coerceResult(context, result, true).toJavaArrayMaybeUnsafe();
        } catch (RaiseException e) { // e.g. NoMethodError: undefined method `coerce'
            context.setErrorInfo($ex); // restore $!

            if (error) throw typeError(context, "", other, " can't be coerced into " + types(context.runtime, getMetaClass()));
            return null;
        }
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

    /** do_coerce
     *
     */
    protected final RubyArray doCoerce(ThreadContext context, IRubyObject other, boolean err) {
        IRubyObject ary = getMetaClass(other).finvokeChecked(context, other, sites(context).coerce_checked, this);

        if (ary == null) {
            if (err) {
                coerceFailed(context, other);
            }
            return null;
        }
        final IRubyObject $ex = context.getErrorInfo();

        return coerceResult(context, ary, err);
    }

    private static RubyArray coerceResult(ThreadContext context, final IRubyObject result, final boolean err) {
        if (result instanceof RubyArray && ((RubyArray) result).getLength() == 2) return (RubyArray) result;

        if (err || !result.isNil()) throw typeError(context, "coerce must return [x, y]");

        return null;
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
    protected final void coerceFailed(ThreadContext context, IRubyObject arg) {
        IRubyObject other = arg.isSpecialConst() || arg instanceof RubyFloat ?
                arg.inspect(context) :
                arg.getMetaClass().name(context);

        throw typeError(context, str(context.runtime, other, " can't be coerced into ", getMetaClass()));
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
        IRubyObject ret = context.safeRecurse(RubyNumeric::coerceBitRecursive, site, doCoerce(context, other, true), site.methodName, true);
        if (ret == null) {
            coerceFailed(context, other);
        }
        return ret;
    }

    private static IRubyObject coerceBitRecursive(ThreadContext context, JavaSites.CheckedSites site, IRubyObject _array, boolean recur) {
        if (recur) {
            String methodName = site.methodName;
            throw nameError(context, str(context.runtime, "recursive call to ", ids(context.runtime, methodName)), asSymbol(context, methodName));
        }

        RubyArray array = (RubyArray) _array;
        IRubyObject x = array.eltOk(0);
        IRubyObject y = array.eltOk(1);

        return getMetaClass(x).finvokeChecked(context, x, site, y);
    }

    /** rb_num_coerce_cmp
     *  coercion used for comparisons
     */

    @Deprecated // no longer used
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

    @Deprecated // no longer used
    protected final IRubyObject coerceRelOp(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);

        return ary == null ? RubyComparable.cmperr(context, this, other) : unwrapCoerced(context, method, other, ary);
    }

    protected final IRubyObject coerceRelOp(ThreadContext context, CallSite site, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);

        return ary == null ? RubyComparable.cmperr(context, this, other) : unwrapCoerced(context, site, other, ary);
    }

    private IRubyObject unwrapCoerced(ThreadContext context, String method, IRubyObject other, RubyArray ary) {
        IRubyObject result = (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));

        return result == context.nil ? RubyComparable.cmperr(context,this, other) : result;
    }

    private IRubyObject unwrapCoerced(ThreadContext context, CallSite site, IRubyObject other, RubyArray ary) {
        IRubyObject car = ary.eltInternal(0);
        IRubyObject result = site.call(context, car, car, ary.eltInternal(1));
        return result == context.nil ? RubyComparable.cmperr(context, this, other) : result;
    }

    public RubyNumeric asNumeric() {
        return this;
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    @JRubyMethod(name = "!")
    public IRubyObject op_not(ThreadContext context) {
        return context.fals;
    }

    /** num_sadded
     *
     */
    @JRubyMethod(name = "singleton_method_added")
    public static IRubyObject singleton_method_added(ThreadContext context, IRubyObject self, IRubyObject name) {
        Ruby runtime = context.runtime;
        throw typeError(context, str(runtime, "can't define singleton method \"", ids(runtime, name), "\" for ", types(runtime, self.getType())));
    }

    /** num_init_copy
     *
     */
    @Override
    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject arg) {
        if (arg == this) return arg; // It cannot init copy it will still work if it is itself...fun times.

        checkFrozen();
        throw typeError(context, "can't copy ", this, "");
    }

    /**
     * @param other
     * @return
     * @deprecated Use {@link org.jruby.RubyNumeric#coerce(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0")
    public IRubyObject coerce(IRubyObject other) {
        return coerce(getCurrentContext(), other);
    }

    /** num_coerce
     *
     */
    @JRubyMethod(name = "coerce")
    public IRubyObject coerce(ThreadContext context, IRubyObject other) {
        return metaClass == other.getMetaClass() ?
                newArray(context, other, this) :
                newArray(context, RubyKernel.new_float(context, other), RubyKernel.new_float(context, this));
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_uplus() {
        return op_uplus(getCurrentContext());
    }

    /** num_uplus
     *
     */
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus(ThreadContext context) {
        return this;
    }

    /** num_imaginary
     *
     */
    @JRubyMethod(name = "i")
    public IRubyObject num_imaginary(ThreadContext context) {
        return RubyComplex.newComplexRaw(context.runtime, asFixnum(context, 0), this);
    }

    /** num_uminus
     *
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus(ThreadContext context) {
        RubyArray ary = asFixnum(context, 0).doCoerce(context, this, true);
        IRubyObject car = ary.eltInternal(0);
        return numFuncall(context, car, sites(context).op_minus, ary.eltInternal(1));
    }

    // MRI: rb_int_plus and others, handled by polymorphism
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        return coerceBin(context, sites(context).op_plus, other);
    }

    // MRI: rb_int_minus
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        return coerceBin(context, sites(context).op_minus, other);
    }

    /** num_cmp
     *
     */
    @JRubyMethod(name = "<=>")
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return this == other ? asFixnum(context, 0) : context.nil;
    }

    /** num_eql
     *
     */
    @JRubyMethod(name = "eql?")
    public IRubyObject eql_p(ThreadContext context, IRubyObject other) {
        if (getClass() != other.getClass()) return context.fals;
        return equalInternal(context, this, other) ? context.tru : context.fals;
    }

    /** num_quo
     *
     */
    @JRubyMethod(name = "quo")
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        return RubyRational.numericQuo(context, this, other);
    }

    /**
     * MRI: num_div
     */
    @JRubyMethod(name = "div")
    public IRubyObject div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric num && num.isZero(context)) throw context.runtime.newZeroDivisionError();

        IRubyObject quotient = numFuncall(context, this, sites(context).op_quo, other);
        return sites(context).floor.call(context, quotient, quotient);
    }

    /**
     * MRI: rb_int_idiv and overrides
     */
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        return div(context, other);
    }

    public IRubyObject idiv(ThreadContext context, long other) {
        return idiv(context, asFixnum(context, other));
    }

    /** num_divmod
     *
     */
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        return newArray(context, div(context, other), modulo(context, other));
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
    @JRubyMethod(name = {"modulo", "%"})
    public IRubyObject modulo(ThreadContext context, IRubyObject other) {
        IRubyObject div = numFuncall(context, this, sites(context).div, other);
        IRubyObject product = sites(context).op_times.call(context, other, other, div);
        return sites(context).op_minus.call(context, this, this, product);
    }

    IRubyObject modulo(ThreadContext context, long other) {
        return modulo(context, asFixnum(context, other));
    }

    /** num_remainder
     *
     */
    @JRubyMethod(name = "remainder")
    public IRubyObject remainder(ThreadContext context, IRubyObject y) {
        return numRemainder(context, y);
    }

    public IRubyObject numRemainder(ThreadContext context, IRubyObject y) {
        RubyNumeric x = this;
        if (!(y instanceof RubyNumeric)) {
            RubyArray coerced = doCoerce(context, y, true);
            y = coerced.eltOk(1);
            x = (RubyNumeric) coerced.eltOk(0);
        }
        JavaSites.NumericSites sites = sites(context);
        IRubyObject z = sites.op_mod.call(context, this, this, y);

        if ((!Helpers.rbEqual(context, z, asFixnum(context, 0), sites.op_equal).isTrue()) &&
                ((x.isNegative(context) && RubyNumeric.positiveInt(context, y)) ||
                        (x.isPositive(context) && RubyNumeric.negativeInt(context, y)))) {
            if (y instanceof RubyFloat && Double.isInfinite(((RubyFloat)y).value)) {
                return x;
            }
            return sites.op_minus.call(context, z, z, y);
        }
        return z;
    }

    public static boolean positiveInt(ThreadContext context, IRubyObject num) {
        return num instanceof RubyNumeric numeric ?
                numeric.isPositive(context) :
                compareWithZero(context, num, sites(context).op_gt_checked).isTrue();
    }

    public static boolean negativeInt(ThreadContext context, IRubyObject num) {
        return num instanceof RubyNumeric numeric ?
                numeric.isNegative(context) :
                compareWithZero(context, num, sites(context).op_lt_checked).isTrue();
    }

    /** num_abs
     *
     */
    @JRubyMethod(name = "abs")
    public IRubyObject abs(ThreadContext context) {
        return sites(context).op_lt.call(context, this, this, asFixnum(context, 0)).isTrue() ?
            sites(context).op_uminus.call(context, this, this) : this;
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
    public IRubyObject real_p(ThreadContext context) {
        return asBoolean(context, isReal());
    }

    public boolean isReal() { return true; } // only RubyComplex isn't real

    @Deprecated
    public IRubyObject scalar_p() {
        return asBoolean(getCurrentContext(), isReal());
    }

    @Deprecated(since = "10.0")
    public IRubyObject integer_p() {
        return integer_p(getCurrentContext());
    }

    /** num_int_p
     *
     */
    @JRubyMethod(name = "integer?")
    public IRubyObject integer_p(ThreadContext context) {
        return context.fals;
    }

    /** num_zero_p
     *
     */
    @JRubyMethod(name = "zero?")
    public IRubyObject zero_p(ThreadContext context) {
        return equalInternal(context, this, asFixnum(context, 0)) ? context.tru : context.fals;
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.RubyNumeric#isZero(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public boolean isZero() {
        return isZero(getCurrentContext());
    }

    public boolean isZero(ThreadContext context) {
        return zero_p(context).isTrue();
    }

    /** num_nonzero_p
     *
     */
    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p(ThreadContext context) {
        return numFuncall(context, this, sites(context).zero).isTrue() ? context.nil : this;
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
    private static final String[] STEP_KEYS = new String[] {"to", "by"};

    /**
     * num_step
     */
    @JRubyMethod(optional = 2, checkArity = false)
    public IRubyObject step(ThreadContext context, IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(context, args, 0, 2);

        if (!block.isGiven()) {
            IRubyObject[] newArgs = new IRubyObject[3];
            numExtractStepArgs(context, args, newArgs);
            IRubyObject to = newArgs[0], step = newArgs[1], by = newArgs[2];

            if (!by.isNil()) step = by;

            if (step.isNil()) {
                step = asFixnum(context, 1);
            } else if (step.op_equal(context, asFixnum(context, 0)).isTrue()) {
                throw argumentError(context, "step can't be 0");
            }

            if ((to.isNil() || to instanceof RubyNumeric) && step instanceof RubyNumeric) {
                return RubyArithmeticSequence.newArithmeticSequence(context, this, "step", args, this, to, step, context.fals);
            }

            return enumeratorizeWithSize(context, this, "step", args, RubyNumeric::stepSize);
        }

        IRubyObject[] newArgs = new IRubyObject[2];
        boolean desc = scanStepArgs(context, args, newArgs);
        return stepCommon(context, newArgs[0], newArgs[1], desc, block);
    }

    /**
     * MRI: num_step_extract_args
     */
    private int numExtractStepArgs(ThreadContext context, IRubyObject[] args, IRubyObject[] newArgs) {
        newArgs[0] = newArgs[1] = newArgs[2] = context.nil;

        IRubyObject hash = ArgsUtil.getOptionsArg(context.runtime, args);
        int argc = hash.isNil() ? args.length : args.length - 1;

        switch (argc) {
            case 2:
                newArgs[1] = args[1];
            case 1:
                newArgs[0] = args[0];
        }
        if (!hash.isNil()) {
            IRubyObject[] values = ArgsUtil.extractKeywordArgs(context, (RubyHash) hash, STEP_KEYS);
            if (values[0] != null) {
                if (argc > 0) throw argumentError(context, "to is given twice");
                newArgs[0] = values[0];
            }
            if (values[1] != null) {
                if (argc > 1) throw argumentError(context, "step is given twice");
                newArgs[2] = values[1];
            }
        }

        return argc;
    }

    /**
     * MRI: num_step_check_fix_args
     */
    private boolean numStepCheckFixArgs(ThreadContext context, int argc, IRubyObject[] newArgs, IRubyObject by) {
        IRubyObject to = newArgs[0];
        IRubyObject step = newArgs[1];

        if (!by.isNil()) {
            newArgs[1] = step = by;
        } else {
            /* compatibility */
            if (argc > 1 && step.isNil()) throw typeError(context, "step must be numeric");

            if (step.op_equal(context, asFixnum(context, 0)).isTrue()) throw argumentError(context, "step can't be 0");
        }
        if (step.isNil()) newArgs[1] = step = asFixnum(context, 1);

        boolean desc = numStepNegative(context, step);
        if (to.isNil()) newArgs[0] = asFloat(context, desc ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
        return desc;
    }

    /**
     * MRI: num_step_scan_args
     */
    private boolean scanStepArgs(ThreadContext context, IRubyObject[] args, IRubyObject[] newArgs) {
        IRubyObject [] tmpNewArgs = new IRubyObject[3];
        int argc = numExtractStepArgs(context, args, tmpNewArgs);

        System.arraycopy(tmpNewArgs, 0, newArgs, 0, 2);
        IRubyObject by = tmpNewArgs[2];

        return numStepCheckFixArgs(context, argc, newArgs, by);
    }

    // MRI: num_step_negative_p
    private static boolean numStepNegative(ThreadContext context, IRubyObject num) {
        if (num instanceof RubyInteger in && context.sites.Integer.op_lt.isBuiltin(num)) return in.isNegative(context);

        RubyFixnum zero = asFixnum(context, 0);
        IRubyObject r = getMetaClass(num).finvokeChecked(context, num, sites(context).op_gt_checked, zero);
        if (r == null) ((RubyNumeric) num).coerceFailed(context, zero);
        return !r.isTrue();
    }

    private IRubyObject stepCommon(ThreadContext context, IRubyObject to, IRubyObject step, boolean desc, Block block) {
        if (step.op_equal(context, asFixnum(context, 0)).isTrue()) throw argumentError(context, "step can't be 0");

        boolean inf;

        if (to instanceof RubyFloat) {
            double f = ((RubyFloat) to).value;
            inf = Double.isInfinite(f) && (f <= -0.0 ? desc : !desc);
        } else {
            inf = false;
        }

        if (this instanceof RubyFixnum fix1 && (inf || to instanceof RubyFixnum) && step instanceof RubyFixnum fix2) {
            fixnumStep(context, fix1, to, fix2, inf, desc, block);
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            floatStep(context, this, to, step, false, false, block);
        } else {
            duckStep(context, this, to, step, inf, desc, block);
        }
        return this;
    }

    private static void fixnumStep(ThreadContext context, RubyFixnum from, IRubyObject to, RubyFixnum step,
                                   boolean inf, boolean desc, Block block) {
        long i = from.value;
        long diff = step.value;

        if (inf) {
            for (;; i += diff) {
                block.yield(context, asFixnum(context, i));
                context.pollThreadEvents();
            }
        } else {
            // We must avoid integer overflows in "i += step".
            long end = ((RubyFixnum) to).asLong(context);
            if (desc) {
                long tov = Long.MIN_VALUE - diff;
                if (end > tov) tov = end;
                for (; i >= tov; i += diff) {
                    block.yield(context, asFixnum(context, i));
                    context.pollThreadEvents();
                }
                if (i >= end) block.yield(context, asFixnum(context, i));
            } else {
                long tov = Long.MAX_VALUE - diff;
                if (end < tov) tov = end;
                for (; i <= tov; i += diff) {
                    block.yield(context, asFixnum(context, i));
                    context.pollThreadEvents();
                }
                if (i <= end) {
                    block.yield(context, asFixnum(context, i));
                }
            }
        }
    }

    private static void duckStep(ThreadContext context, IRubyObject from, IRubyObject to, IRubyObject step, boolean inf, boolean desc, Block block) {
        IRubyObject i = from;

        if (inf) {
            for (;; i = sites(context).op_plus.call(context, i, i, step)) {
                block.yield(context, i);
                context.pollThreadEvents();
            }
        } else {
            CallSite cmpSite = desc ? sites(context).op_lt : sites(context).op_gt;

            for (; !cmpSite.call(context, i, i, to).isTrue(); i = sites(context).op_plus.call(context, i, i, step)) {
                block.yield(context, i);
                context.pollThreadEvents();
            }
        }
    }

    // MRI: ruby_num_interval_step_size
    public static RubyNumeric intervalStepSize(ThreadContext context, IRubyObject from, IRubyObject to, IRubyObject step, boolean excl) {
        if (from instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            long delta, diff;

            diff = ((RubyFixnum) step).value;
            if (diff == 0) return asFloat(context, Double.POSITIVE_INFINITY);

            // overflow checking
            long toLong = ((RubyFixnum) to).value;
            long fromLong = ((RubyFixnum) from).value;
            delta = toLong - fromLong;
            if (!Helpers.subtractionOverflowed(toLong, fromLong, delta)) {
                if (diff < 0) {
                    diff = -diff;
                    delta = -delta;
                }
                if (excl) delta--;

                if (delta < 0) return asFixnum(context, 0);

                // overflow checking
                long steps = delta / diff;
                long stepSize = steps + 1;
                if (stepSize != Long.MIN_VALUE) {
                    return asFixnum(context, delta / diff + 1);
                } else {
                    return RubyBignum.newBignum(context.runtime, BigInteger.valueOf(steps).add(BigInteger.ONE));
                }
            }
            // fall through to duck-typed logic
        } else if (from instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            double n = floatStepSize(from.convertToFloat().value, to.convertToFloat().value, step.convertToFloat().value, excl);

            if (Double.isInfinite(n)) return asFloat(context, n);
            if (posFixable(n)) return toInteger(context, asFloat(context, n));

            return RubyBignum.newBignorm(context.runtime, n);
        }

        JavaSites.NumericSites sites = sites(context);
        CallSite op_gt = sites.op_gt;
        CallSite op_lt = sites.op_lt;
        CallSite cmpSite = op_gt;

        RubyFixnum zero = asFixnum(context, 0);
        IRubyObject comparison = zero.coerceCmp(context, sites.op_cmp, step);

        switch (RubyComparable.cmpint(context, op_gt, op_lt, comparison, step, zero)) {
            case 0:
                return asFloat(context, Float.POSITIVE_INFINITY);
            case 1:
                cmpSite = op_lt;
                break;
        }

        if (cmpSite.call(context, from, from, to).isTrue()) return asFixnum(context, 0);

        IRubyObject deltaObj = sites.op_minus.call(context, to, to, from);
        IRubyObject result = sites.div.call(context, deltaObj, deltaObj, step);
        IRubyObject timesPlus = sites.op_plus.call(context, from, from, sites.op_times.call(context, result, result, step));
        if (!excl || cmpSite.call(context, timesPlus, timesPlus, to).isTrue()) {
            result = sites.op_plus.call(context, result, result, asFixnum(context, 1));
        }
        return (RubyNumeric) result;
    }

    /**
     * A step size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * MRI: num_step_size
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject stepSize(ThreadContext context, RubyNumeric from, IRubyObject[] args) {
        IRubyObject[] newArgs = new IRubyObject[2];
        from.scanStepArgs(context, args, newArgs);
        return intervalStepSize(context, from, newArgs[0], newArgs[1], false);
    }
    
    // ruby_float_step
    public static boolean floatStep(ThreadContext context, IRubyObject from, IRubyObject to, IRubyObject step, boolean excl, boolean allowEndless, Block block) {
        if (from instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            double beg = num2dbl(from);
            double unit = num2dbl(step);
            double end = (allowEndless && to.isNil()) ? (unit < 0 ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY) : num2dbl(to);
            double n = floatStepSize(beg, end, unit, excl);

            if (Double.isInfinite(unit)) {
                /* if unit is infinity, i*unit+beg is NaN */
                if (n > 0) {
                    block.yield(context, dbl2num(context.runtime, beg));
                    context.pollThreadEvents();
                }
            } else if (unit == 0) {
                IRubyObject val = dbl2num(context.runtime, beg);
                for (;;) {
                    block.yield(context, val);
                    context.pollThreadEvents();
                }
            } else {
                for (long i=0; i < n; i++) {
                    double d = i * unit + beg;
                    if (unit >= 0 ? end < d : d < end) {
                        d = end;
                    }
                    block.yield(context, dbl2num(context.runtime, d));
                    context.pollThreadEvents();
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Returns the number of unit-sized steps between the given beg and end.
     *
     * NOTE: the returned value is either Double.POSITIVE_INFINITY, or a rounded value appropriate to be cast to a long
     *
     * MRI: ruby_float_step_size
     */
    public static double floatStepSize(double beg, double end, double unit, boolean excludeLast) {
        double n = (end - beg)/unit;
        double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;
        double d;

        if (Double.isInfinite(unit)) {
            if (unit > 0) {
                return beg <= end ? 1 : 0;
            } else {
                return end <= beg ? 1 : 0;
            }
        }

        if (unit == 0) return Float.POSITIVE_INFINITY;

        if (err > 0.5) err = 0.5;
        if (excludeLast) {
            if (n <= 0) return 0;

            n = n < 1 ? 0 : Math.floor(n - err);
            d = +((n + 1) * unit) + beg;
            if (beg < end) {
                if (d < end) n++;
            } else if (beg > end) {
                if (d > end) n++;
            }
        } else {
            if (n < 0) return 0;

            n = Math.floor(n + err);
        }
        return n + 1;
    }

    /** num_equal, doesn't override RubyObject.op_equal
     *
     */
    protected final IRubyObject op_num_equal(ThreadContext context, IRubyObject other) {
        // it won't hurt fixnums
        if (this == other) return context.tru;

        // nil is not equal to any number
        if (this.isNil()) return context.fals;

        return numFuncall(context, other, sites(context).op_equals, this);
    }

    /** numeric_numerator
     *
     */
    @JRubyMethod(name = "numerator")
    public IRubyObject numerator(ThreadContext context) {
        IRubyObject rational = f_to_r(context, this);
        return sites(context).numerator.call(context, rational, rational);
    }

    /** numeric_denominator
     *
     */
    @JRubyMethod(name = "denominator")
    public IRubyObject denominator(ThreadContext context) {
        IRubyObject rational = f_to_r(context, this);
        return sites(context).denominator.call(context, rational, rational);
    }

    /**
     * @return ""
     * @deprecated Use {@link RubyNumeric#convertToRational(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyRational convertToRational() {
        return convertToRational(getCurrentContext());
    }

    public RubyRational convertToRational(ThreadContext context) {
        return RubyRational.newRationalRaw(context.runtime, numerator(context), denominator(context));
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
        return asFixnum(context, 0);
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
        final double value = asDouble(context);
        if (Double.isNaN(value)) return this;

        return f_negative_p(context, this) || value == 0.0 && 1 / value == Double.NEGATIVE_INFINITY ?
                context.runtime.getMath().getConstant(context, "PI") :
                asFixnum(context, 0);
    }

    /** numeric_rect
     *
     */
    @JRubyMethod(name = {"rectangular", "rect"})
    public IRubyObject rect(ThreadContext context) {
        return newArray(context, this, asFixnum(context, 0));
    }

    /** numeric_polar
     *
     */
    @JRubyMethod(name = "polar")
    public IRubyObject polar(ThreadContext context) {
        return newArray(context, f_abs(context, this), f_arg(context, this));
    }

    /** numeric_real
     *
     */
    @JRubyMethod(name = {"conjugate", "conj"})
    public IRubyObject conjugate(ThreadContext context) {
        return this;
    }

    @Override
    public <T> T toJava(Class<T> target) {
        return JavaUtil.getNumericConverter(target).coerce(getRuntime().getCurrentContext(), this, target);
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
    public IRubyObject negative_p(ThreadContext context) {
        return compareWithZero(context, this, sites(context).op_lt_checked);
    }

    /** num_positive_p
     *
     */
    @JRubyMethod(name = "positive?")
    public IRubyObject positive_p(ThreadContext context) {
        return compareWithZero(context, this, sites(context).op_gt_checked);
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.RubyNumeric#isNegative(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public boolean isNegative() {
        return isNegative(getCurrentContext());
    }

    public boolean isNegative(ThreadContext context) {
        return negative_p(context).isTrue();
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.RubyNumeric#isPositive(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public boolean isPositive() {
        return isPositive(getCurrentContext());
    }

    public boolean isPositive(ThreadContext context) {
        return positive_p(context).isTrue();
    }

    protected static IRubyObject compareWithZero(ThreadContext context, IRubyObject num, JavaSites.CheckedSites site) {
        var zero = asFixnum(context, 0);
        IRubyObject r = getMetaClass(num).finvokeChecked(context, num, site, zero);
        if (r == null) RubyComparable.cmperr(context, num, zero);
        return r;
    }

    @JRubyMethod(name = "finite?")
    public IRubyObject finite_p(ThreadContext context) {
        return context.tru;
    }

    @JRubyMethod(name = "infinite?")
    public IRubyObject infinite_p(ThreadContext context) {
        return context.nil;
    }

    @Deprecated
    public final IRubyObject rbClone(IRubyObject[] args) {
        ThreadContext context = metaClass.runtime.getCurrentContext();
        switch (args.length) {
            case 0: return rbClone(context);
            case 1: return rbClone(context, args[0]);
        }
        throw argumentError(context, "wrong number of arguments (given " + args.length + ", expected 0)");
    }

    @JRubyMethod(name = "clone")
    public final IRubyObject rbClone(ThreadContext context) {
        return this;
    }

    @Override
    @JRubyMethod(name = "clone")
    public final IRubyObject rbClone(ThreadContext context, IRubyObject arg) {
        // BasicObject handles "special" objects like all numerics but we leave this because Ruby
        // has an explicit Numeric#clone binding.
        return super.rbClone(context, arg);
    }

    @Override
    public final IRubyObject rbClone() {
        return this;
    }

    @Override
    public IRubyObject dup() {
        return this;
    }

    public static IRubyObject numFuncall(ThreadContext context, IRubyObject x, CallSite site) {
        return context.safeRecurse(RubyNumeric::numFuncall0, site, x, site.methodName, true);
    }

    public static IRubyObject numFuncall(ThreadContext context, final IRubyObject x, CallSite site, final IRubyObject value) {
        return context.safeRecurse(new NumFuncall1(value), site, x, site.methodName, true);
    }

    private static IRubyObject numFuncall0(ThreadContext context, CallSite site, IRubyObject obj, boolean recur) {
        if (recur) {
            String name = site.methodName;
            if (name.length() > 0 && Character.isLetterOrDigit(name.charAt(0))) {
                throw context.runtime.newNameError(name, obj, name);
            } else if (name.length() == 2 && name.charAt(1) == '@') {
                throw context.runtime.newNameError(name, obj, name.substring(0, 1));
            } else {
                throw context.runtime.newNameError(name, obj, name);
            }
        }
        return site.call(context, obj, obj);
    }

    private static class NumFuncall1 implements ThreadContext.RecursiveFunctionEx<CallSite> {
        private final IRubyObject value;

        public NumFuncall1(IRubyObject value) {
            this.value = value;
        }

        @Override
        public IRubyObject call(ThreadContext context, CallSite site, IRubyObject obj, boolean recur) {
            if (recur) throw context.runtime.newNameError(site.methodName, obj, site.methodName);

            return site.call(context, obj, obj, value);
        }
    }

    // MRI: macro FIXABLE, RB_FIXABLE
    // Note: this does additional checks for inf and nan
    public static boolean fixable(Ruby runtime, double f) {
        if (Double.isNaN(f) || Double.isInfinite(f))  throw runtime.newFloatDomainError(Double.toString(f));

        long l = (long) f;
        if (l == RubyFixnum.MIN || l == RubyFixnum.MAX) {
            BigInteger bigint = BigDecimal.valueOf(f).toBigInteger();
            return posFixable(bigint) && negFixable(bigint);
        }
        return posFixable(f) && negFixable(f);
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
        return l < RubyFixnum.MAX + 1;
    }

    // MRI: macro NEGFIXABLE, RB_NEGFIXABLE
    public static boolean negFixable(double l) {
        return l >= RubyFixnum.MIN;
    }

    @Deprecated
    public IRubyObject floor() {
        return floor(getCurrentContext());
    }

    @Deprecated
    public IRubyObject ceil() {
        return ceil(getCurrentContext());
    }

    @Deprecated
    public IRubyObject round() {
        return round(getCurrentContext());
    }

    /** num_truncate
     *
     */
    @Deprecated
    public IRubyObject truncate() {
        return truncate(getCurrentContext());
    }

    private static JavaSites.NumericSites sites(ThreadContext context) {
        return context.sites.Numeric;
    }
}
