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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.math.BigInteger;
import java.util.List;

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base class for all numerical types in ruby.
 */
// TODO: Numeric.new works in Ruby and it does here too.  However trying to use
//   that instance in a numeric operation should generate an ArgumentError. Doing
//   this seems so pathological I do not see the need to fix this now.
public class RubyNumeric extends RubyObject {

    public static RubyClass createNumericClass(IRuby runtime) {
        RubyClass numeric = runtime.defineClass("Numeric", runtime.getObject(), NUMERIC_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyNumeric.class);
        numeric.defineFastMethod("singleton_method_added", callbackFactory.getMethod("sadded",
                IRubyObject.class));

        numeric.includeModule(runtime.getModule("Comparable"));

        numeric.defineFastMethod("initialize_copy", callbackFactory.getMethod("init_copy", IRubyObject.class));
        numeric.defineFastMethod("coerce", callbackFactory.getMethod("coerce", IRubyObject.class));

        numeric.defineFastMethod("+@", callbackFactory.getMethod("uplus"));
        numeric.defineFastMethod("-@", callbackFactory.getMethod("uminus"));
        numeric.defineFastMethod("<=>", callbackFactory.getMethod("cmp", IRubyObject.class));
        numeric.defineFastMethod("quo", callbackFactory.getMethod("quo", IRubyObject.class));
        numeric.defineFastMethod("eql?", callbackFactory.getMethod("eql_p", IRubyObject.class));
        numeric.defineFastMethod("div", callbackFactory.getMethod("div", IRubyObject.class));
        numeric.defineFastMethod("divmod", callbackFactory.getMethod("divmod", IRubyObject.class));
        numeric.defineFastMethod("modulo", callbackFactory.getMethod("modulo", IRubyObject.class));
        numeric.defineFastMethod("remainder", callbackFactory.getMethod("remainder", IRubyObject.class));
        numeric.defineFastMethod("abs", callbackFactory.getMethod("abs"));
        numeric.defineFastMethod("to_int", callbackFactory.getMethod("to_int"));
        numeric.defineFastMethod("integer?", callbackFactory.getMethod("int_p"));
        numeric.defineFastMethod("zero?", callbackFactory.getMethod("zero_p"));
        numeric.defineFastMethod("nonzero?", callbackFactory.getMethod("nonzero_p"));
        numeric.defineFastMethod("floor", callbackFactory.getMethod("floor"));
        numeric.defineFastMethod("ceil", callbackFactory.getMethod("ceil"));
        numeric.defineFastMethod("round", callbackFactory.getMethod("round"));
        numeric.defineFastMethod("truncate", callbackFactory.getMethod("truncate"));
        numeric.defineMethod("step", callbackFactory.getOptMethod("step"));

        return numeric;
    }

    protected static ObjectAllocator NUMERIC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(IRuby runtime, RubyClass klass) {
            return new RubyNumeric(runtime, klass);
        }
    };

    public static double DBL_EPSILON = 2.2204460492503131e-16;

    public RubyNumeric(IRuby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    // The implementations of these are all bonus (see TODO above)  I was going
    // to throw an error from these, but it appears to be the wrong place to
    // do it.
    public double getDoubleValue() {
        return 0;
    }

    public long getLongValue() {
        return 0;
    }

    public static RubyNumeric newNumeric(IRuby runtime) {
        return new RubyNumeric(runtime, runtime.getClass("Numeric"));
    }

    /*  ================
     *  Utility Methods
     *  ================ 
     */

    /** rb_num2int, check_int, NUM2INT
     * 
     */
    public static int num2int(IRubyObject arg) {
        long num;
        if (arg instanceof RubyFixnum) { // Fixnums can be bigger than int
            num = ((RubyFixnum) arg).getLongValue();
        } else {
            num = num2long(arg);
        }

        String s;
        if (num < Integer.MIN_VALUE) {
            s = "small";
        } else if (num > Integer.MAX_VALUE) {
            s = "big";
        } else {
            return (int) num;
        }
        throw arg.getRuntime().newRangeError("integer " + num + " too " + s + " to convert to `int'");
    }

    // TODO: Find all consumers and convert to correct conversion protocol <- done
    /** rb_num2long and FIX2LONG (numeric.c)
     * 
     */
    public static long num2long(IRubyObject arg) {
        if (arg instanceof RubyFixnum) {
            return ((RubyFixnum) arg).getLongValue();
        }
        if (arg.isNil()) {
            throw arg.getRuntime().newTypeError("no implicit conversion from nil to integer");
        }

        if (arg instanceof RubyFloat) {
            double aFloat = ((RubyFloat) arg).getDoubleValue();
            if (aFloat <= (double) Long.MAX_VALUE && aFloat >= (double) Long.MIN_VALUE) {
                return (long) aFloat;
            } else {
                // TODO: number formatting here, MRI uses "%-.10g", 1.4 API is a must?
                throw arg.getRuntime().newTypeError("float " + aFloat + "out of range of integer");
            }
        } else if (arg instanceof RubyBignum) {
            return RubyBignum.big2long((RubyBignum) arg);
        }
        return arg.convertToInteger().getLongValue();
    }

    /** rb_dbl2big + LONG2FIX at once (numeric.c)
     * 
     */
    public static IRubyObject dbl2num(IRuby runtime, double val) {
        if (Double.isInfinite(val)) {
            throw runtime.newFloatDomainError(val < 0 ? "-Infinity" : "Infinity");
        }
        if (Double.isNaN(val)) {
            throw runtime.newFloatDomainError("NaN");
        }

        if (val > (double) RubyFixnum.MAX || val < (double) RubyFixnum.MIN) {
            return RubyBignum.newBignum(runtime, val);
        }
        return RubyFixnum.newFixnum(runtime, (long) val);
    }

    /** rb_num2dbl and NUM2DBL
     * 
     */
    public static double num2dbl(IRubyObject arg) {
        if (arg instanceof RubyFloat) {
            return ((RubyFloat) arg).getDoubleValue();
        } else if (arg instanceof RubyString) {
            throw arg.getRuntime().newTypeError("no implicit conversion to float from string");
        } else if (arg == arg.getRuntime().getNil()) {
            throw arg.getRuntime().newTypeError("no implicit conversion to float from nil");
        }
        return arg.convertToFloat().getDoubleValue();
    }

    /** rb_dbl_cmp (numeric.c)
     * 
     */
    public static IRubyObject dbl_cmp(IRuby runtime, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return runtime.getNil();
        }
        if (a > b) {
            return RubyFixnum.one(runtime);
        }
        if (a < b) {
            return RubyFixnum.minus_one(runtime);
        }
        return RubyFixnum.zero(runtime);
    }

    public static long fix2long(IRubyObject arg) {
        if (arg instanceof RubyFixnum) {
            return ((RubyFixnum) arg).getLongValue();
        }
        throw arg.getRuntime().newTypeError("argument is not a Fixnum");
    }

    public static int fix2int(IRubyObject arg) {
        long val = fix2long(arg);
        if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
            throw arg.getRuntime().newTypeError("argument value is too big to convert to int");
        }
        return (int) val;
    }

    public static RubyInteger str2inum(IRuby runtime, RubyString str, int base) {
        return str2inum(runtime, str, base, false);
    }

    public static RubyNumeric int2fix(IRuby runtime, long val) {
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
     * @param base  the expected base of the number (2, 8, 10 or 16), or 0 
     *              if the method should determine the base automatically 
     *              (defaults to 10).
     * @param raise if the string is not a valid integer, raise error, otherwise return 0
     * @return  a RubyFixnum or (if necessary) a RubyBignum representing 
     *          the result of the conversion, which will be zero if the 
     *          conversion failed.
     */
    public static RubyInteger str2inum(IRuby runtime, RubyString str, int base, boolean raise) {
        StringBuffer sbuf = new StringBuffer(str.toString().trim());
        if (sbuf.length() == 0) {
            if (raise) {
                throw runtime.newArgumentError("invalid value for Integer: "
                        + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return RubyFixnum.zero(runtime);
        }
        int pos = 0;
        int radix = (base != 0) ? base : 10;
        boolean digitsFound = false;
        if (sbuf.charAt(pos) == '-') {
            pos++;
        } else if (sbuf.charAt(pos) == '+') {
            sbuf.deleteCharAt(pos);
        }
        if (pos == sbuf.length()) {
            if (raise) {
                throw runtime.newArgumentError("invalid value for Integer: "
                        + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return RubyFixnum.zero(runtime);
        }
        if (sbuf.charAt(pos) == '0') {
            sbuf.deleteCharAt(pos);
            if (pos == sbuf.length()) {
                return RubyFixnum.zero(runtime);
            }
            if (sbuf.charAt(pos) == 'x' || sbuf.charAt(pos) == 'X') {
                if (base == 0 || base == 16) {
                    radix = 16;
                    sbuf.deleteCharAt(pos);
                }
            } else if (sbuf.charAt(pos) == 'b' || sbuf.charAt(pos) == 'B') {
                if (base == 0 || base == 2) {
                    radix = 2;
                    sbuf.deleteCharAt(pos);
                }
            } else {
                radix = (base == 0) ? 8 : base;
            }
        }
        while (pos < sbuf.length()) {
            if (sbuf.charAt(pos) == '_') {
                sbuf.deleteCharAt(pos);
            } else if (Character.digit(sbuf.charAt(pos), radix) != -1) {
                digitsFound = true;
                pos++;
            } else {
                break;
            }
        }
        if (!digitsFound) {
            if (raise) {
                throw runtime.newArgumentError("invalid value for Integer: "
                        + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return RubyFixnum.zero(runtime);
        }
        try {
            long l = Long.parseLong(sbuf.substring(0, pos), radix);
            return runtime.newFixnum(l);
        } catch (NumberFormatException ex) {
            try {
                BigInteger bi = new BigInteger(sbuf.substring(0, pos), radix);
                return new RubyBignum(runtime, bi);
            } catch (NumberFormatException e) {
                if (raise) {
                    throw runtime.newArgumentError("invalid value for Integer: "
                            + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
                }
                return RubyFixnum.zero(runtime);
            }
        }
    }

    public static RubyFloat str2fnum(IRuby runtime, RubyString arg) {
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
     * @param raise if the string is not a valid float, raise error, otherwise return 0.0
     * @return  a RubyFloat representing the result of the conversion, which
     *          will be 0.0 if the conversion failed.
     */
    public static RubyFloat str2fnum(IRuby runtime, RubyString arg, boolean raise) {
        String str = arg.toString().trim();
        double d = 0.0;
        int pos = str.length();
        for (int i = 0; i < pos; i++) {
            if ("0123456789eE+-.".indexOf(str.charAt(i)) == -1) {
                if (raise) {
                    throw runtime.newArgumentError("invalid value for Float(): "
                            + arg.callMethod(runtime.getCurrentContext(), "inspect").toString());
                }
                pos = i + 1;
                break;
            }
        }
        for (; pos > 0; pos--) {
            try {
                d = Double.parseDouble(str.substring(0, pos));
            } catch (NumberFormatException ex) {
                if (raise) {
                    throw runtime.newArgumentError("invalid value for Float(): "
                            + arg.callMethod(runtime.getCurrentContext(), "inspect").toString());
                }
                continue;
            }
            break;
        }
        return new RubyFloat(runtime, d);
    }

    /** Numeric methods. (num_*)
     *
     */
    protected IRubyObject[] getCoerced(IRubyObject other, boolean error) {
        IRubyObject result;

        try {
            result = other.callMethod(getRuntime().getCurrentContext(), "coerce", this);
        } catch (RaiseException e) {
            if (error) {
                throw getRuntime().newTypeError(
                        other.getMetaClass().getName() + " can't be coerced into " + getMetaClass().getName());
            }
            return null;
        }

        if (!(result instanceof RubyArray) || ((RubyArray) result).getLength() != 2) {
            throw getRuntime().newTypeError("coerce must return [x, y]");
        }

        return ((RubyArray) result).toJavaArray();
    }

    protected IRubyObject callCoerced(String method, IRubyObject other, boolean err) {
        IRubyObject[] args = getCoerced(other, err);
        return args[0].callMethod(getRuntime().getCurrentContext(), method, args[1]);
    }

    protected IRubyObject callCoerced(String method, IRubyObject other) {
        IRubyObject[] args = getCoerced(other, true);
        return args[0].callMethod(getRuntime().getCurrentContext(), method, args[1]);
    }

    // beneath are rewritten coercions that reflect MRI logic, the aboves are used only by RubyBigDecimal

    /** coerce_body
     * 
     */
    protected final IRubyObject coerceBody(IRubyObject other) {
        return other.callMethod(getRuntime().getCurrentContext(), "coerce", this);
    }

    /** do_coerce
     * 
     */
    protected final List doCoerce(IRubyObject other, boolean err) {
        IRubyObject result;
        try {
            result = coerceBody(other);
        } catch (RaiseException e) {
            if (err) {
                throw getRuntime().newTypeError(
                        other.getMetaClass().getName() + " can't be coerced into " + getMetaClass().getName());
            }
            return null;
        }

        if (!(result instanceof RubyArray) || ((RubyArray) result).getLength() != 2) {
            throw getRuntime().newTypeError("coerce must return [x, y]");
        }
        return ((RubyArray) result).getList();
    }

    /** rb_num_coerce_bin
     *  coercion taking two arguments
     */
    protected final IRubyObject coerceBin(String method, IRubyObject other) {
        List list = doCoerce(other, true);
        return ((RubyObject) list.get(0))
                .callMethod(getRuntime().getCurrentContext(), method, (RubyObject) list.get(1));
    }

    /** rb_num_coerce_cmp
     *  coercion used for comparisons
     */
    protected final IRubyObject coerceCmp(String method, IRubyObject other) {
        List list = doCoerce(other, false);
        if (list == null) {
            return getRuntime().getNil(); // MRI does it!
        }
        return ((RubyObject) list.get(0))
                .callMethod(getRuntime().getCurrentContext(), method, (RubyObject) list.get(1));
    }

    /** rb_num_coerce_relop
     *  coercion used for relative operators
     */
    protected final IRubyObject coerceRelOp(String method, IRubyObject other) {
        List list = doCoerce(other, false);
        if (list != null) {
            IRubyObject result = ((RubyObject) list.get(0)).callMethod(getRuntime().getCurrentContext(), method,
                    (RubyObject) list.get(1));
            if (!result.isNil()) {
                return result;
            }
        }

        RubyComparable.cmperr(this, other); // MRI also does it that way       
        return null; // not reachd as in MRI
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
    public IRubyObject sadded(IRubyObject name) {
        throw getRuntime().newTypeError("can't define singleton method " + name + " for " + getMetaClass().getName());
    }

    /** num_init_copy
     *
     */
    public IRubyObject init_copy(IRubyObject arg) {
        throw getRuntime().newTypeError("can't copy " + getMetaClass().getName());
    }

    /** num_coerce
     *
     */
    public IRubyObject coerce(IRubyObject other) {
        if (getMetaClass() == other.getMetaClass()) {
            return getRuntime().newArray(other, this);
        }

        return getRuntime().newArray(other.convertToFloat(), convertToFloat());
    }

    /** num_uplus
     *
     */
    public IRubyObject uplus() {
        return this;
    }

    /** num_uminus
     *
     */
    public IRubyObject uminus() {
        RubyFixnum zero = RubyFixnum.zero(getRuntime());
        List list = zero.doCoerce(this, true);
        return ((RubyObject) list.get(0)).callMethod(getRuntime().getCurrentContext(), "-", (RubyObject) list.get(1));
    }

    /** num_cmp
     *
     */
    public IRubyObject cmp(IRubyObject other) {
        if (this == other) { // won't hurt fixnums
            return RubyFixnum.zero(getRuntime());
        }
        return getRuntime().getNil();
    }

    /** num_eql
     *
     */
    public IRubyObject eql_p(IRubyObject other) {
        if (getMetaClass() != other.getMetaClass()) {
            return getRuntime().getFalse();
        }
        return super.equal(other);
    }

    /** num_quo
     *
     */
    public IRubyObject quo(IRubyObject other) {
        return callMethod(getRuntime().getCurrentContext(), "/", other);
    }

    /** num_div
     * 
     */
    public IRubyObject div(IRubyObject other) {
        return callMethod(getRuntime().getCurrentContext(), "/", other).convertToFloat().floor();
    }

    /** num_divmod
     * 
     */
    public IRubyObject divmod(IRubyObject other) {
        IRubyObject cdr = callMethod(getRuntime().getCurrentContext(), "%", other);
        IRubyObject car = div(other);
        return RubyArray.newArray(getRuntime(), car, cdr);

    }

    /** num_modulo
     *
     */
    public IRubyObject modulo(IRubyObject other) {
        return callMethod(getRuntime().getCurrentContext(), "%", other);
    }

    /** num_remainder
     *
     */
    public IRubyObject remainder(IRubyObject y) {
        ThreadContext context = getRuntime().getCurrentContext();
        IRubyObject z = callMethod(context, "%", y);
        IRubyObject x = this;
        RubyFixnum zero = RubyFixnum.zero(getRuntime());

        if (!(((RubyNumeric)z).equal(zero).isTrue())
            && ((x.callMethod(context, "<", zero)).isTrue()
                && (y.callMethod(context, ">", zero)).isTrue()) 
            || ((x.callMethod(context, ">", zero)).isTrue()
                && (y.callMethod(context, "<", zero)).isTrue())) {
            return z.callMethod(context, "-",y);
        }

        return z;
    }

    /** num_abs
     *
     */
    public IRubyObject abs() {
        ThreadContext context = getRuntime().getCurrentContext();
        if (callMethod(context, "<", RubyFixnum.zero(getRuntime())).isTrue()) {
            return (RubyNumeric) callMethod(context, "-@");
        }
        return this;
    }

    /** num_to_int
     * 
     */
    public IRubyObject to_int() {
        return callMethod(getRuntime().getCurrentContext(), "to_i", IRubyObject.NULL_ARRAY);
    }

    /** num_int_p
     *
     */
    public IRubyObject int_p() {
        return getRuntime().getFalse();
    }

    /** num_zero_p
     *
     */
    public IRubyObject zero_p() {
        // Will always return a boolean
        return equal(RubyFixnum.zero(getRuntime())).isTrue() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** num_nonzero_p
     *
     */
    public IRubyObject nonzero_p() {
        if (callMethod(getRuntime().getCurrentContext(), "zero?").isTrue()) {
            return getRuntime().getNil();
        }
        return this;
    }

    /** num_floor
     *
     */
    public IRubyObject floor() {
        return convertToFloat().floor();
    }

    /** num_ceil
     *
     */
    public IRubyObject ceil() {
        return convertToFloat().ceil();
    }

    /** num_round
     *
     */
    public IRubyObject round() {
        return convertToFloat().round();
    }

    /** num_truncate
     *
     */
    public IRubyObject truncate() {
        return convertToFloat().truncate();
    }

    /** num_step
     * 
     */
    public IRubyObject step(IRubyObject[] args) {
        IRubyObject to;
        IRubyObject step;

        if (args.length == 1) {
            to = args[0];
            step = RubyFixnum.one(getRuntime());
        } else if (args.length == 2) {
            to = args[0];
            step = args[1];
        } else {
            throw getRuntime().newTypeError("wrong number of arguments");
        }
        ThreadContext context = getRuntime().getCurrentContext();
        if (this instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            long value = getLongValue();
            long end = ((RubyFixnum) to).getLongValue();
            long diff = ((RubyFixnum) step).getLongValue();

            if (diff == 0) {
                throw getRuntime().newArgumentError("step cannot be 0");
            }
            if (diff > 0) {
                for (long i = value; i <= end; i += diff) {
                    context.yield(RubyFixnum.newFixnum(getRuntime(), i));
                }
            } else {
                for (long i = value; i >= end; i += diff) {
                    context.yield(RubyFixnum.newFixnum(getRuntime(), i));
                }
            }
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            double beg = num2dbl(this);
            double end = num2dbl(to);
            double unit = num2dbl(step);

            if (unit == 0) {
                throw getRuntime().newArgumentError("step cannot be 0");
            }

            double n = (end - beg) / unit;
            double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;

            if (err > 0.5) {
                err = 0.5;
            }
            n = Math.floor(n + err) + 1;

            for (double i = 0; i < n; i++) {
                context.yield(RubyFloat.newFloat(getRuntime(), i * unit + beg));
            }

        } else {
            RubyNumeric i = this;

            String cmp;
            if (((RubyBoolean) step.callMethod(context, ">", RubyFixnum.zero(getRuntime()))).isTrue()) {
                cmp = ">";
            } else {
                cmp = "<";
            }

            while (true) {
                if (i.callMethod(context, cmp, to).isTrue()) {
                    break;
                }
                context.yield(i);
                i = (RubyNumeric) i.callMethod(context, "+", step);
            }
        }
        return this;
    }

    /** num_clone
     *
     */
    public IRubyObject rbClone() {
        return this;
    }

    //    /** num_equal
    //     *
    //     */
    //    public RubyBoolean veryEqual(IRubyObject other) {
    //        IRubyObject truth = super.equal(other);
    //
    //        return truth == getRuntime().getNil() ? getRuntime().getFalse() : (RubyBoolean) truth;
    //    }
    //
    /** num_equal
     *
     */
    public IRubyObject equal(IRubyObject other) {
        if (this == other) { // it won't hurt fixnums
            return getRuntime().getTrue();
        }

        return other.callMethod(getRuntime().getCurrentContext(), "==", this);
    }

    public boolean singletonMethodsAllowed() {
        return false;
    }
}
