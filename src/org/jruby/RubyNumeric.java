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
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Convert;

/**
 * Base class for all numerical types in ruby.
 */
// TODO: Numeric.new works in Ruby and it does here too.  However trying to use
//   that instance in a numeric operation should generate an ArgumentError. Doing
//   this seems so pathological I do not see the need to fix this now.
public class RubyNumeric extends RubyObject {
    
    public static RubyClass createNumericClass(Ruby runtime) {
        RubyClass numeric = runtime.defineClass("Numeric", runtime.getObject(), NUMERIC_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyNumeric.class);
        numeric.defineFastMethod("singleton_method_added", callbackFactory.getFastMethod("sadded",
                RubyKernel.IRUBY_OBJECT));

        numeric.includeModule(runtime.getModule("Comparable"));

        numeric.defineFastMethod("initialize_copy", callbackFactory.getFastMethod("init_copy", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("coerce", callbackFactory.getFastMethod("coerce", RubyKernel.IRUBY_OBJECT));

        numeric.defineFastMethod("+@", callbackFactory.getFastMethod("uplus"));
        numeric.defineFastMethod("-@", callbackFactory.getFastMethod("uminus"));
        numeric.defineFastMethod("<=>", callbackFactory.getFastMethod("cmp", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("quo", callbackFactory.getFastMethod("quo", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("eql?", callbackFactory.getFastMethod("eql_p", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("div", callbackFactory.getFastMethod("div", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("divmod", callbackFactory.getFastMethod("divmod", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("modulo", callbackFactory.getFastMethod("modulo", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("remainder", callbackFactory.getFastMethod("remainder", RubyKernel.IRUBY_OBJECT));
        numeric.defineFastMethod("abs", callbackFactory.getFastMethod("abs"));
        numeric.defineFastMethod("to_int", callbackFactory.getFastMethod("to_int"));
        numeric.defineFastMethod("integer?", callbackFactory.getFastMethod("int_p"));
        numeric.defineFastMethod("zero?", callbackFactory.getFastMethod("zero_p"));
        numeric.defineFastMethod("nonzero?", callbackFactory.getFastMethod("nonzero_p"));
        numeric.defineFastMethod("floor", callbackFactory.getFastMethod("floor"));
        numeric.defineFastMethod("ceil", callbackFactory.getFastMethod("ceil"));
        numeric.defineFastMethod("round", callbackFactory.getFastMethod("round"));
        numeric.defineFastMethod("truncate", callbackFactory.getFastMethod("truncate"));
        numeric.defineMethod("step", callbackFactory.getOptMethod("step"));

        return numeric;
    }

    protected static ObjectAllocator NUMERIC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyNumeric(runtime, klass);
        }
    };

    public static double DBL_EPSILON=2.2204460492503131e-16;
    
    public RubyNumeric(Ruby runtime, RubyClass metaClass) {
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
    
    public static RubyNumeric newNumeric(Ruby runtime) {
    	return new RubyNumeric(runtime, runtime.getClass("Numeric"));
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
        String s;
        if (num < Integer.MIN_VALUE) {
            s = "small";
        } else if (num > Integer.MAX_VALUE) {
            s = "big";
        } else {
            return;
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
    public static IRubyObject dbl2num(Ruby runtime, double val) {
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
    public static IRubyObject dbl_cmp(Ruby runtime, double a, double b) {
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
            return ((RubyFixnum) arg).getLongValue();
        }

    public static int fix2int(IRubyObject arg) {
        long num = arg instanceof RubyFixnum ? fix2long(arg) : num2long(arg);

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
        if (base != 0 && (base < 2 || base > 36)) {
            throw runtime.newArgumentError("illegal radix " + base);
            }
        ByteList bytes = str.getByteList();
        try {
            return runtime.newFixnum(Convert.byteListToLong(bytes,base,strict));

        } catch (InvalidIntegerException e) {
            if (strict) {
                throw runtime.newArgumentError("invalid value for Integer: "
                        + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
            }
            return RubyFixnum.zero(runtime);
        } catch (NumberTooLargeException e) {
        try {
                BigInteger bi = Convert.byteListToBigInteger(bytes,base,strict);
                return new RubyBignum(runtime,bi);
            } catch (InvalidIntegerException e2) {
                if(strict) {
                    throw runtime.newArgumentError("invalid value for Integer: "
                            + str.callMethod(runtime.getCurrentContext(), "inspect").toString());
                }
                return RubyFixnum.zero(runtime);
            }
        }
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
        
        if (!(result instanceof RubyArray) || ((RubyArray)result).getLength() != 2) {
            throw getRuntime().newTypeError("coerce must return [x, y]");
        }
        
        return ((RubyArray)result).toJavaArray();
    }

    protected IRubyObject callCoerced(String method, IRubyObject other, boolean err) {
        IRubyObject[] args = getCoerced(other, err);
        if(args == null) {
            return getRuntime().getNil();
        }
        return args[0].callMethod(getRuntime().getCurrentContext(), method, args[1]);
    }

    protected IRubyObject callCoerced(String method, IRubyObject other) {
        IRubyObject[] args = getCoerced(other, false);
        if(args == null) {
            return getRuntime().getNil();
        }
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
    protected final RubyArray doCoerce(IRubyObject other, boolean err) {
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
        return (RubyArray) result;
    }

    /** rb_num_coerce_bin
     *  coercion taking two arguments
     */
    protected final IRubyObject coerceBin(String method, IRubyObject other) {
        RubyArray ary = doCoerce(other, true);
        return (ary.eltInternal(0)).callMethod(getRuntime().getCurrentContext(), method, ary.eltInternal(1));
    }
    
    /** rb_num_coerce_cmp
     *  coercion used for comparisons
     */
    protected final IRubyObject coerceCmp(String method, IRubyObject other) {
        RubyArray ary = doCoerce(other, false);
        if (ary == null) {
            return getRuntime().getNil(); // MRI does it!
        } 
        return (ary.eltInternal(0)).callMethod(getRuntime().getCurrentContext(), method, ary.eltInternal(1));
    }
        
    /** rb_num_coerce_relop
     *  coercion used for relative operators
     */
    protected final IRubyObject coerceRelOp(String method, IRubyObject other) {
        RubyArray ary = doCoerce(other, false);
        if (ary != null) {
            IRubyObject result = (ary.eltInternal(0)).callMethod(getRuntime().getCurrentContext(), method,ary.eltInternal(1));
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
        throw getRuntime().newTypeError("can't define singleton method " + name + " for " + getType().getName());
        } 
        
    /** num_init_copy
     *
     */
    public IRubyObject init_copy(IRubyObject arg) {
        throw getRuntime().newTypeError("can't copy " + getType().getName());
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
    
    public IRubyObject step(IRubyObject[] args, Block block) {
        IRubyObject to;
        IRubyObject step;
        
        if(args.length == 1){ 
            to = args[0];
            step = RubyFixnum.one(getRuntime());
        } else if (args.length == 2) {
            to = args[0];
            step = args[1];
        }else{
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
                    block.yield(context, RubyFixnum.newFixnum(getRuntime(), i));
                }
            } else {
                for (long i = value; i >= end; i += diff) {
                    block.yield(context, RubyFixnum.newFixnum(getRuntime(), i));
                }
            }
        } else if (this instanceof RubyFloat || to instanceof RubyFloat || step instanceof RubyFloat) {
            double beg = num2dbl(this);
            double end = num2dbl(to);
            double unit = num2dbl(step);

            if (unit == 0) {
                throw getRuntime().newArgumentError("step cannot be 0");
            }           
            
            double n = (end - beg)/unit;
            double err = (Math.abs(beg) + Math.abs(end) + Math.abs(end - beg)) / Math.abs(unit) * DBL_EPSILON;
            
            if (err>0.5) {
                err=0.5;            
            }
            n = Math.floor(n + err) + 1;
            
            for(double i = 0; i < n; i++){
                block.yield(context, RubyFloat.newFloat(getRuntime(), i * unit + beg));
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
                block.yield(context, i);
                i = (RubyNumeric) i.callMethod(context, "+", step);
            }
        }
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

    public static class InvalidIntegerException extends NumberFormatException {
        private static final long serialVersionUID = 55019452543252148L;
        
        public InvalidIntegerException() {
            super();
        }
        public InvalidIntegerException(String message) {
            super(message);
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
        
    }
    
}
