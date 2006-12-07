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

import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Base class for all numerical types in ruby.
 */
// TODO: Numeric.new works in Ruby and it does here too.  However trying to use
//   that instance in a numeric operation should generate an ArgumentError. Doing
//   this seems so pathological I do not see the need to fix this now.
public class RubyNumeric extends RubyObject {

    public RubyNumeric(IRuby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    // The implementations of these are all bonus (see TODO above)  I was going
    // to throw an error from these, but it appears to be the wrong place to
    // do it.
    public double getDoubleValue() { return 0; }
    public long getLongValue() { return 0; }
    public RubyNumeric multiplyWith(RubyInteger value) { return value; }
    public RubyNumeric multiplyWith(RubyFloat value) { return value; }
    public RubyNumeric multiplyWith(RubyBignum value) { return value; }

    public long getTruncatedLongValue() {
        return getLongValue();
    }
    
    public static RubyNumeric newNumeric(IRuby runtime) {
    	return new RubyNumeric(runtime, runtime.getClass("Numeric"));
    }

    // TODO: Find all consumers and convert to correct conversion protocol
    public static long num2long(IRubyObject arg) {
        if (arg instanceof RubyNumeric) {
            return ((RubyNumeric) arg).getLongValue();
        }
        throw arg.getRuntime().newTypeError("argument is not numeric");
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
        return str2inum(runtime,str,base,false);
    }

    public static RubyNumeric int2fix(IRuby runtime, long val) {
        return RubyFixnum.newFixnum(runtime,val);
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
            if(raise) {
                throw runtime.newArgumentError("invalid value for Integer: " + str.callMethod(runtime.getCurrentContext(),"inspect").toString());
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
            if(raise) {
                throw runtime.newArgumentError("invalid value for Integer: " + str.callMethod(runtime.getCurrentContext(),"inspect").toString());
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
            if(raise) {
                throw runtime.newArgumentError("invalid value for Integer: " + str.callMethod(runtime.getCurrentContext(),"inspect").toString());
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
            } catch(NumberFormatException e) {
                if(raise) {
                    throw runtime.newArgumentError("invalid value for Integer: " + str.callMethod(runtime.getCurrentContext(),"inspect").toString());
                }
                return RubyFixnum.zero(runtime);
            }
        }
    }

    public static RubyFloat str2fnum(IRuby runtime, RubyString arg) {
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
                if(raise) {
                    throw runtime.newArgumentError("invalid value for Float(): " + arg.callMethod(runtime.getCurrentContext(),"inspect").toString());
                }
                pos = i + 1;
                break;
            }
        }
        for (; pos > 0; pos--) {
            try {
                d = Double.parseDouble(str.substring(0, pos));
            } catch (NumberFormatException ex) {
                if(raise) {
                    throw runtime.newArgumentError("invalid value for Float(): " + arg.callMethod(runtime.getCurrentContext(),"inspect").toString());
                }
                continue;
            }
            break;
        }
        return new RubyFloat(runtime, d);
    }

    /* Numeric methods. (num_*)
     *
     */
    
    protected IRubyObject[] getCoerced(IRubyObject other, boolean error) {
        IRubyObject result;
        
        try {
            result = other.callMethod(getRuntime().getCurrentContext(), "coerce", this);
        } catch (RaiseException e) {
            if (error) {
                throw getRuntime().newTypeError(other.getMetaClass().getName() + 
                        " can't be coerced into " + getMetaClass().getName());
            }
             
            return null;
        }
        
        if (!(result instanceof RubyArray) || ((RubyArray)result).getLength() != 2) {
            throw getRuntime().newTypeError("coerce must return [x, y]");
        }
        
        return ((RubyArray)result).toJavaArray();
    }

    protected IRubyObject callCoerced(String method, IRubyObject other) {
        IRubyObject[] args = getCoerced(other, true);
        return args[0].callMethod(getRuntime().getCurrentContext(), method, args[1]);
    }
    
    public RubyNumeric asNumeric() {
    	return this;
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
    
    public IRubyObject to_int() {
        return callMethod(getRuntime().getCurrentContext(), "to_i", IRubyObject.NULL_ARRAY);
    }

    /** num_clone
     *
     */
    public IRubyObject rbClone() {
        return this;
    }
    
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newBoolean(compareValue((RubyNumeric) other) >= 0);
        } 
        
        return RubyComparable.op_ge(this, other);
    }
    
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newBoolean(compareValue((RubyNumeric) other) > 0);
        } 
        
        return RubyComparable.op_gt(this, other);
    }

    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newBoolean(compareValue((RubyNumeric) other) <= 0);
        } 
        
        return RubyComparable.op_le(this, other);
    }
    
    public IRubyObject op_lt(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newBoolean(compareValue((RubyNumeric) other) < 0);
        } 
        
        return RubyComparable.op_lt(this, other);
    }

    /** num_uplus
     *
     */
    public IRubyObject op_uplus() {
        return this;
    }

    /** num_uminus
     *
     */
    public IRubyObject op_uminus() {
        RubyArray coerce = (RubyArray) coerce(RubyFixnum.zero(getRuntime()));

        return (RubyNumeric) coerce.entry(0).callMethod(getRuntime().getCurrentContext(), "-", coerce.entry(1));
    }
    
    public IRubyObject cmp(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newFixnum(compareValue((RubyNumeric) other));
        }

        return other.respondsTo("to_int") ? callCoerced("<=>", other) : getRuntime().getNil();
    }

    public IRubyObject divmod(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
    	    RubyNumeric denominator = (RubyNumeric) other;
            RubyNumeric div = (RubyNumeric) callMethod(getRuntime().getCurrentContext(), "/", denominator);
            if (div instanceof RubyFloat) {
                double d = Math.floor(((RubyFloat) div).getValue());
                if (((RubyFloat) div).getValue() > d) {
                    div = RubyFloat.newFloat(getRuntime(), d);
                }
            }
            
            return getRuntime().newArray(div, modulo(denominator));
    	}

    	return callCoerced("divmod", other);
    }

    /** num_modulo
     *
     */
    public IRubyObject modulo(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return (RubyNumeric) callMethod(getRuntime().getCurrentContext(), "%", other);
    	}
    	
    	return callCoerced("modulo", other);
    }

    /** num_remainder
     *
     */
    public IRubyObject remainder(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            IRubyObject mod = modulo(other);
            final RubyNumeric zero = RubyFixnum.zero(getRuntime());
            ThreadContext context = getRuntime().getCurrentContext();

            if (callMethod(context, "<", zero).isTrue() && other.callMethod(context, ">", zero).isTrue() || 
                callMethod(context, ">", zero).isTrue() && other.callMethod(context, "<", zero).isTrue()) {

                return (RubyNumeric) mod.callMethod(context, "-", other);
            }

            return mod;
    	}
    	
    	return callCoerced("remainder", other);
    }

    protected int compareValue(RubyNumeric other) {
        return -1;
    }
    
    /** num_equal
     *
     */
    public RubyBoolean veryEqual(IRubyObject other) {
    	IRubyObject truth = super.equal(other);
    	
    	return truth == getRuntime().getNil() ? getRuntime().getFalse() : 
    		(RubyBoolean) truth;
    }
    
    /** num_equal
     *
     */
    public IRubyObject equal(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return getRuntime().newBoolean(compareValue((RubyNumeric) other) == 0);
        }
        return super.equal(other); // +++ rb_equal
    }

    /** num_eql
     *
     */
    public RubyBoolean eql(IRubyObject other) {
        // Two numbers of the same value, but different types are not
        // 'eql?'.
        if (getMetaClass() != other.getMetaClass()) {
            return getRuntime().getFalse();
        }
        
        // However, if they are the same type, then we try a regular
        // equal as a float with 1.0 may be two different ruby objects.
        // It will always return a boolean
        return (RubyBoolean) equal(other);
    }

    /** num_abs
     *
     */
    public RubyNumeric abs() {
        ThreadContext context = getRuntime().getCurrentContext();
        if (callMethod(context, "<", RubyFixnum.zero(getRuntime())).isTrue()) {
            return (RubyNumeric) callMethod(context, "-@");
        }
		return this;
    }

    /** num_int_p
     *
     */
    public RubyBoolean int_p() {
        return getRuntime().getFalse();
    }

    /** num_zero_p
     *
     */
    public RubyBoolean zero_p() {
    	// Will always return a boolean
        return (RubyBoolean) equal(RubyFixnum.zero(getRuntime()));
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
    public RubyInteger floor() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue()).floor();
    }

    /** num_ceil
     *
     */
    public RubyInteger ceil() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue()).ceil();
    }

    /** num_round
     *
     */
    public RubyInteger round() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue()).round();
    }

    /** num_truncate
     *
     */
    public RubyInteger truncate() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue()).truncate();
    }

    public RubyNumeric multiplyWith(RubyFixnum value) {
        return multiplyWith((RubyInteger) value);
    }

    public RubyFloat convertToFloat() {
        return getRuntime().newFloat(getDoubleValue());
    }
}
