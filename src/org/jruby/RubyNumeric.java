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

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.util.Numeric.f_abs;
import static org.jruby.util.Numeric.f_arg;
import static org.jruby.util.Numeric.f_mul;
import static org.jruby.util.Numeric.f_negative_p;

import java.math.BigInteger;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Convert;
import org.jruby.util.Convert2;

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

        numeric.kindOf = new RubyModule.KindOf() {
            @Override
            public boolean isKindOf(IRubyObject obj, RubyModule type) {
                return obj instanceof RubyNumeric;
            }
        };

        numeric.includeModule(runtime.getComparable());
        numeric.defineAnnotatedMethods(RubyNumeric.class);

        return numeric;
    }

    protected static final ObjectAllocator NUMERIC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNumeric(runtime, klass);
        }
    };

    public static double DBL_EPSILON=2.2204460492503131e-16;

    private static IRubyObject convertToNum(double val, Ruby runtime) {

        if (val >= (double) RubyFixnum.MAX || val < (double) RubyFixnum.MIN) {
            return RubyBignum.newBignum(runtime, val);
        }
        return RubyFixnum.newFixnum(runtime, (long) val);
    }
    
    public RubyNumeric(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyNumeric(RubyClass metaClass) {
        super(metaClass);
    }

    public RubyNumeric(Ruby runtime, RubyClass metaClass, boolean useObjectSpace) {
        super(runtime, metaClass, useObjectSpace);
    }    

    public RubyNumeric(Ruby runtime, RubyClass metaClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, metaClass, useObjectSpace, canBeTainted);
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
        } else {
            return;
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
            String value = ((RubyString) arg).toString();

            if (value != null && value.length() > 0) return (byte) value.charAt(0);
        } 

        return (byte) num2int(arg);
    }

    /** rb_num2long and FIX2LONG (numeric.c)
     * 
     */
    public static long num2long(IRubyObject arg) {
        if (arg instanceof RubyFixnum) {
            return ((RubyFixnum) arg).getLongValue();
        } else {
            return other2long(arg);
        }
    }

    private static long other2long(IRubyObject arg) throws RaiseException {
        if (arg.isNil()) {
            throw arg.getRuntime().newTypeError("no implicit conversion from nil to integer");
        } else if (arg instanceof RubyFloat) {
            return float2long((RubyFloat)arg);
        } else if (arg instanceof RubyBignum) {
            return RubyBignum.big2long((RubyBignum) arg);
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

    /** rb_dbl2big + LONG2FIX at once (numeric.c)
     * 
     */
    /** rb_dbl2big + LONG2FIX at once (numeric.c)
     * 
     */
    public static IRubyObject dbl2num(Ruby runtime, double val) {
        if (Double.isInfinite(val)) {
            throw runtime.newFloatDomainError(val < 0 ? "-Infinity" : "Infinity");
        }
        if (Double.isNaN(val)) {
            throw runtime.newFloatDomainError("NaN");
        }
        return convertToNum(val,runtime);
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
        return RubyKernel.new_float(arg, arg).getDoubleValue();
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
        return str2inum(runtime,str,base,false);
    }

    public static RubyNumeric int2fix(Ruby runtime, long val) {
        return RubyFixnum.newFixnum(runtime,val);
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
        return Convert2.byteListToInum(runtime, s, base, strict);
    }

    public static RubyFloat str2fnum(Ruby runtime, RubyString arg) {
        return str2fnum(runtime,arg,false);
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
        final double ZERO = 0.0;
        
        try {
            return new RubyFloat(runtime,Convert.byteListToDouble(arg.getByteList(),strict));
        } catch (NumberFormatException e) {
            if (strict) {
                throw runtime.newArgumentError("invalid value for Float(): "
                        + arg.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return new RubyFloat(runtime,ZERO);
        }
    }
    
    /** Numeric methods. (num_*)
     *
     */
    
    protected IRubyObject[] getCoerced(ThreadContext context, IRubyObject other, boolean error) {
        IRubyObject result;
        
        try {
            result = other.callMethod(context, "coerce", this);
        } catch (RaiseException e) {
            if (error) {
                throw getRuntime().newTypeError(
                        other.getMetaClass().getName() + " can't be coerced into " + getMetaClass().getName());
            }
             
            return null;
        }
        
        if (!(result instanceof RubyArray) || ((RubyArray)result).getLength() != 2) {
            throw getRuntime().newTypeError("coerce must return [x, y]");
        }
        
        return ((RubyArray)result).toJavaArray();
    }

    protected IRubyObject callCoerced(ThreadContext context, String method, IRubyObject other, boolean err) {
        IRubyObject[] args = getCoerced(context, other, err);
        if(args == null) {
            return getRuntime().getNil();
        }
        return args[0].callMethod(context, method, args[1]);
    }

    protected IRubyObject callCoerced(ThreadContext context, String method, IRubyObject other) {
        IRubyObject[] args = getCoerced(context, other, false);
        if(args == null) {
            return getRuntime().getNil();
        }
        return args[0].callMethod(context, method, args[1]);
    }
    
    // beneath are rewritten coercions that reflect MRI logic, the aboves are used only by RubyBigDecimal

    /** coerce_body
     *
     */
    protected final IRubyObject coerceBody(ThreadContext context, IRubyObject other) {
        return other.callMethod(context, "coerce", this);
    }

    /** do_coerce
     * 
     */
    protected final RubyArray doCoerce(ThreadContext context, IRubyObject other, boolean err) {
        IRubyObject result;
        try {
            result = coerceBody(context, other);
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
        return (RubyArray) result;
    }

    /** rb_num_coerce_bin
     *  coercion taking two arguments
     */
    protected final IRubyObject coerceBin(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, true);
        return (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
    }
    
    /** rb_num_coerce_cmp
     *  coercion used for comparisons
     */
    protected final IRubyObject coerceCmp(ThreadContext context, String method, IRubyObject other) {
        RubyArray ary = doCoerce(context, other, false);
        if (ary == null) {
            return getRuntime().getNil(); // MRI does it!
        } 
        return (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
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
    
    private final IRubyObject unwrapCoerced(ThreadContext context, String method, IRubyObject other, RubyArray ary) {
        IRubyObject result = (ary.eltInternal(0)).callMethod(context, method, ary.eltInternal(1));
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
    public IRubyObject sadded(IRubyObject name) {
        throw getRuntime().newTypeError("can't define singleton method " + name + " for " + getType().getName());
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
        if (getMetaClass() == other.getMetaClass()) return getRuntime().newArray(other, this);

        IRubyObject cdr = RubyKernel.new_float(this, this);
        IRubyObject car = RubyKernel.new_float(this, other);

        return getRuntime().newArray(car, cdr);
    }

    /** num_uplus
     *
     */
    @JRubyMethod(name = "+@")
    public IRubyObject op_uplus() {
        return this;
    }

    /** num_uminus
     *
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus(ThreadContext context) {
        RubyArray ary = RubyFixnum.zero(context.getRuntime()).doCoerce(context, this, true);
        return ary.eltInternal(0).callMethod(context, "-", ary.eltInternal(1));
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
        if (getClass() != other.getClass()) return getRuntime().getFalse();
        return equalInternal(context, this, other) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** num_quo
     *
     */
    @JRubyMethod(name = "quo")
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        return callMethod(context, "/", other);
    }
    
    /** num_quo
    *
    */
    @JRubyMethod(name = "quo", compat = CompatVersion.RUBY1_9)
    public IRubyObject quo_19(ThreadContext context, IRubyObject other) {
        return RubyRational.newRationalRaw(context.getRuntime(), this).callMethod(context, "/", other);
    }

    /** num_div
     * 
     */
    @JRubyMethod(name = "div")
    public IRubyObject div(ThreadContext context, IRubyObject other) {
        return callMethod(context, "/", other).convertToFloat().floor();
    }

    /** num_divmod
     * 
     */
    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        return RubyArray.newArray(getRuntime(), div(context, other), modulo(context, other));
    }
    
    /** num_fdiv (1.9) */
    @JRubyMethod(name = "fdiv", compat = CompatVersion.RUBY1_9)
    public IRubyObject fdiv(ThreadContext context, IRubyObject other) {
        return RuntimeHelpers.invoke(context, this.convertToFloat(), "/", other);
    }

    /** num_modulo
     *
     */
    @JRubyMethod(name = "modulo")
    public IRubyObject modulo(ThreadContext context, IRubyObject other) {
        return callMethod(context, "%", other);
    }

    /** num_remainder
     *
     */
    @JRubyMethod(name = "remainder")
    public IRubyObject remainder(ThreadContext context, IRubyObject dividend) {
        IRubyObject z = callMethod(context, "%", dividend);
        IRubyObject x = this;
        RubyFixnum zero = RubyFixnum.zero(getRuntime());

        if (!equalInternal(context, z, zero) &&
                ((x.callMethod(context, "<", zero).isTrue() &&
                dividend.callMethod(context, ">", zero).isTrue()) ||
                (x.callMethod(context, ">", zero).isTrue() &&
                dividend.callMethod(context, "<", zero).isTrue()))) {
            return z.callMethod(context, "-", dividend);
        } else {
            return z;
        }
    }

    /** num_abs
     *
     */
    @JRubyMethod(name = "abs")
    public IRubyObject abs(ThreadContext context) {
        if (callMethod(context, "<", RubyFixnum.zero(getRuntime())).isTrue()) {
            return callMethod(context, "-@");
        }
        return this;
    }

    /** num_abs/1.9
     * 
     */
    @JRubyMethod(name = "magnitude", compat = CompatVersion.RUBY1_9)
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    /** num_to_int
     * 
     */
    @JRubyMethod(name = "to_int")
    public IRubyObject to_int(ThreadContext context) {
        return RuntimeHelpers.invoke(context, this, "to_i");
    }

    /** num_real_p
    *
    */
    @JRubyMethod(name = "real?", compat = CompatVersion.RUBY1_9)
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
        return equalInternal(context, this, RubyFixnum.zero(getRuntime())) ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    /** num_nonzero_p
     *
     */
    @JRubyMethod(name = "nonzero?")
    public IRubyObject nonzero_p(ThreadContext context) {
        if (callMethod(context, "zero?").isTrue()) {
            return getRuntime().getNil();
        }
        return this;
    }

    /** num_floor
     *
     */
    @JRubyMethod(name = "floor")
    public IRubyObject floor() {
        return convertToFloat().floor();
    }
        
    /** num_ceil
     *
     */
    @JRubyMethod(name = "ceil")
    public IRubyObject ceil() {
        return convertToFloat().ceil();
    }

    /** num_round
     *
     */
    @JRubyMethod(name = "round")
    public IRubyObject round() {
        return convertToFloat().round();
    }

    /** num_truncate
     *
     */
    @JRubyMethod(name = "truncate")
    public IRubyObject truncate() {
        return convertToFloat().truncate();
    }

    @Deprecated
    public IRubyObject step(ThreadContext context, IRubyObject[] args, Block block) {
        switch (args.length) {
        case 0: throw context.getRuntime().newArgumentError(0, 1);
        case 1: return step(context, args[0], block);
        case 2: return step(context, args[0], args[1], block);
        default: throw context.getRuntime().newArgumentError(args.length, 2);
        }
    }

    public IRubyObject step(ThreadContext context, IRubyObject arg0, Block block) {
        return step(context, arg0, RubyFixnum.one(context.getRuntime()), block);
    }

    public IRubyObject step(ThreadContext context, IRubyObject to, IRubyObject step, Block block) {
        Ruby runtime = context.getRuntime();
        if (this instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            fixnumStep(context, runtime, ((RubyFixnum)this).getLongValue(),
                                         ((RubyFixnum)to).getLongValue(),
                                         ((RubyFixnum)step).getLongValue(),
                                          block);
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            floatStep(context, runtime, this, to, step, block);
        } else {
            duckStep(context, runtime, this, to, step, block);
        }
        return this;
    }

    @JRubyMethod(name = "step", frame = true)
    public IRubyObject step19(ThreadContext context, IRubyObject arg0, Block block) {
        return block.isGiven() ? stepCommon19(context, arg0, RubyFixnum.one(context.getRuntime()), block) : enumeratorize(context.getRuntime(), this, "step", arg0);
    }

    @JRubyMethod(name = "step", frame = true)
    public IRubyObject step19(ThreadContext context, IRubyObject to, IRubyObject step, Block block) {
        return block.isGiven() ? stepCommon19(context, to, step, block) : enumeratorize(context.getRuntime(), this, "step", new IRubyObject[] {to, step});
    }

    private IRubyObject stepCommon19(ThreadContext context, IRubyObject to, IRubyObject step, Block block) {
        Ruby runtime = context.getRuntime();
        if (this instanceof RubyFixnum && to instanceof RubyFixnum && step instanceof RubyFixnum) {
            fixnumStep(context, runtime, ((RubyFixnum)this).getLongValue(),
                                         ((RubyFixnum)to).getLongValue(),
                                         ((RubyFixnum)step).getLongValue(),
                                          block);
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            floatStep19(context, runtime, this, to, step, false, block);
        } else {
            duckStep(context, runtime, this, to, step, block);
        }
        return this;
    }

    private static void fixnumStep(ThreadContext context, Ruby runtime, long value, long end, long diff, Block block) {
        if (diff == 0) throw runtime.newArgumentError("step cannot be 0");
        if (diff > 0) {
            for (long i = value; i <= end; i += diff) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        } else {
            for (long i = value; i >= end; i += diff) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        }
    }

    protected static void floatStep(ThreadContext context, Ruby runtime, IRubyObject from, IRubyObject to, IRubyObject step, Block block) { 
        double beg = num2dbl(from);
        double end = num2dbl(to);
        double unit = num2dbl(step);

        if (unit == 0) throw runtime.newArgumentError("step cannot be 0");

        double n = (end - beg)/unit;
        double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;

        if (err > 0.5) err = 0.5;            
        n = Math.floor(n + err) + 1;

        for (long i = 0; i < n; i++) {
            block.yield(context, RubyFloat.newFloat(runtime, i * unit + beg));
        }
    }

    static void floatStep19(ThreadContext context, Ruby runtime, IRubyObject from, IRubyObject to, IRubyObject step, boolean excl, Block block) { 
        double beg = num2dbl(from);
        double end = num2dbl(to);
        double unit = num2dbl(step);

        // TODO: remove
        if (unit == 0) throw runtime.newArgumentError("step cannot be 0");

        double n = (end - beg)/unit;
        double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;

        if (Double.isInfinite(unit)) {
            if (unit > 0) block.yield(context, RubyFloat.newFloat(runtime, beg));
        } else {
            if (err > 0.5) err = 0.5;            
            n = Math.floor(n + err);
            if (!excl) n++;
            for (long i = 0; i < n; i++){
                block.yield(context, RubyFloat.newFloat(runtime, i * unit + beg));
            }
        }
    }

    private static void duckStep(ThreadContext context, Ruby runtime, IRubyObject from, IRubyObject to, IRubyObject step, Block block) {
        IRubyObject i = from;
        String cmpString = step.callMethod(context, ">", RubyFixnum.zero(runtime)).isTrue() ? ">" : "<";

        while (true) {
            if (i.callMethod(context, cmpString, to).isTrue()) break;
            block.yield(context, i);
            i = i.callMethod(context, "+", step);
        }
    }

    /** num_equal, doesn't override RubyObject.op_equal
     *
     */
    protected final IRubyObject op_num_equal(ThreadContext context, IRubyObject other) {
        // it won't hurt fixnums
        if (this == other)  return getRuntime().getTrue();

        return other.callMethod(context, "==", this);
    }

    /** num_numerator
     * 
     */
    @JRubyMethod(name = "numerator", compat = CompatVersion.RUBY1_9)
    public IRubyObject numerator(ThreadContext context) {
        return RubyRational.newRationalConvert(context, this).callMethod(context, "numerator");
    }
    
    /** num_denominator
     * 
     */
    @JRubyMethod(name = "denominator", compat = CompatVersion.RUBY1_9)
    public IRubyObject denominator(ThreadContext context) {
        return RubyRational.newRationalConvert(context, this).callMethod(context, "denominator");
    }

    /** numeric_to_c
     * 
     */
    @JRubyMethod(name = "to_c", compat = CompatVersion.RUBY1_9)
    public IRubyObject to_c(ThreadContext context) {
        return RubyComplex.newComplexCanonicalize(context, this);
    }

    /** numeric_real
     * 
     */
    @JRubyMethod(name = "real", compat = CompatVersion.RUBY1_9)
    public IRubyObject real(ThreadContext context) {
        return this;
    }

    /** numeric_image
     * 
     */
    @JRubyMethod(name = {"imaginary", "imag"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject image(ThreadContext context) {
        return RubyFixnum.zero(context.getRuntime());
    }

    /** numeric_abs2
     * 
     */
    @JRubyMethod(name = "abs2", compat = CompatVersion.RUBY1_9)
    public IRubyObject abs2(ThreadContext context) {
        return f_mul(context, this, this);
    }

    /** numeric_arg
     * 
     */
    @JRubyMethod(name = {"arg", "angle", "phase"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject arg(ThreadContext context) {
        if (!f_negative_p(context, this)) return RubyFixnum.zero(context.getRuntime());
        return context.getRuntime().getMath().fastFetchConstant("PI");
    }    

    /** numeric_rect
     * 
     */
    @JRubyMethod(name = {"rectangular", "rect"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject rect(ThreadContext context) {
        return context.getRuntime().newArray(this, RubyFixnum.zero(context.getRuntime()));
    }    

    /** numeric_polar
     * 
     */
    @JRubyMethod(name = "polar", compat = CompatVersion.RUBY1_9)
    public IRubyObject polar(ThreadContext context) {
        return context.getRuntime().newArray(f_abs(context, this), f_arg(context, this));
    }    

    /** numeric_real
     * 
     */
    @JRubyMethod(name = {"conjugate", "conj"}, compat = CompatVersion.RUBY1_9)
    public IRubyObject conjugate(ThreadContext context) {
        return this;
    }

    @Override
    public Object toJava(Class target) {
        return JavaUtil.getNumericConverter(target).coerce(this, target);
    }

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
}
