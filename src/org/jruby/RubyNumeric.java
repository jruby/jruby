/*
 * RubyNumeric.java - No description
 * Created on 10. September 2001, 17:49
 * 
 * Copyright (C) 2001,2002 Jan Arne Petersen, Stefan Matthias Aust, 
 *    Alan Moore, Benoit Cerrina
 * Copyright (C) 2002-2004 Thomas E. Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package org.jruby;

import java.math.BigInteger;

import org.jruby.exceptions.TypeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public abstract class RubyNumeric extends RubyObject {

    public RubyNumeric(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public abstract double getDoubleValue();
    public abstract long getLongValue();

    public long getTruncatedLongValue() {
        return getLongValue();
    }

    public static RubyClass createNumericClass(Ruby ruby) {
        RubyClass result = ruby.defineClass("Numeric", ruby.getClasses().getObjectClass());
        CallbackFactory callbackFactory = ruby.callbackFactory();

        result.includeModule(ruby.getClasses().getComparableModule());

        result.defineMethod("+@", callbackFactory.getMethod(RubyNumeric.class, "op_uplus"));
        result.defineMethod("-@", callbackFactory.getMethod(RubyNumeric.class, "op_uminus"));
        result.defineMethod("<=>", callbackFactory.getMethod(RubyBignum.class, "cmp", RubyNumeric.class));
        result.defineMethod("==", callbackFactory.getMethod(RubyNumeric.class, "equal", IRubyObject.class));
        result.defineMethod("equal?", callbackFactory.getMethod(RubyNumeric.class, "veryEqual", IRubyObject.class));
        result.defineMethod("===", callbackFactory.getMethod(RubyNumeric.class, "equal", IRubyObject.class));
        result.defineMethod("abs", callbackFactory.getMethod(RubyNumeric.class, "abs"));
        result.defineMethod("ceil", callbackFactory.getMethod(RubyNumeric.class, "ceil"));
        result.defineMethod("coerce", callbackFactory.getMethod(RubyNumeric.class, "coerce", RubyNumeric.class));
        result.defineMethod("clone", callbackFactory.getMethod(RubyNumeric.class, "rbClone"));
        result.defineMethod("divmod", callbackFactory.getMethod(RubyNumeric.class, "divmod", RubyNumeric.class));
        result.defineMethod("eql?", callbackFactory.getMethod(RubyNumeric.class, "eql", IRubyObject.class));
        result.defineMethod("floor", callbackFactory.getMethod(RubyNumeric.class, "floor"));
        result.defineMethod("integer?", callbackFactory.getMethod(RubyNumeric.class, "int_p"));
        result.defineMethod("modulo", callbackFactory.getMethod(RubyNumeric.class, "modulo", RubyNumeric.class));
        result.defineMethod("nonzero?", callbackFactory.getMethod(RubyNumeric.class, "nonzero_p"));
        result.defineMethod("remainder", callbackFactory.getMethod(RubyNumeric.class, "remainder", RubyNumeric.class));
        result.defineMethod("round", callbackFactory.getMethod(RubyNumeric.class, "round"));
        result.defineMethod("truncate", callbackFactory.getMethod(RubyNumeric.class, "truncate"));
        result.defineMethod("zero?", callbackFactory.getMethod(RubyNumeric.class, "zero_p"));
        
        return result;
    }

    public static long num2long(IRubyObject arg) {
        if (arg instanceof RubyNumeric) {
            return ((RubyNumeric) arg).getLongValue();
        }
        throw new TypeError(arg.getRuntime(), "argument is not numeric");
    }

    public static long fix2long(IRubyObject arg) {
        if (arg instanceof RubyFixnum) {
            return ((RubyFixnum) arg).getLongValue();
        }
        throw new TypeError(arg.getRuntime(), "argument is not a Fixnum");
    }

    public static int fix2int(IRubyObject arg) {
        long val = fix2long(arg);
        if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
            throw new TypeError(arg.getRuntime(), "argument value is too big to convert to int");
        }
        return (int) val;
    }

    public static final RubyNumeric numericValue(IRubyObject arg) {
        if (!(arg instanceof RubyNumeric)) {
            throw new TypeError(arg.getRuntime(), "argument not numeric");
        }
        return (RubyNumeric) arg;
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
     * @param ruby  the ruby runtime
     * @param str   the string to be converted
     * @param base  the expected base of the number (2, 8, 10 or 16), or 0 
     *              if the method should determine the base automatically 
     *              (defaults to 10).
     * @return  a RubyFixnum or (if necessary) a RubyBignum representing 
     *          the result of the conversion, which will be zero if the 
     *          conversion failed.
     */
    public static RubyInteger str2inum(Ruby ruby, RubyString str, int base) {
        StringBuffer sbuf = new StringBuffer(str.getValue().trim());
        if (sbuf.length() == 0) {
            return RubyFixnum.zero(ruby);
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
            return RubyFixnum.zero(ruby);
        }
        if (sbuf.charAt(pos) == '0') {
            sbuf.deleteCharAt(pos);
            if (pos == sbuf.length()) {
                return RubyFixnum.zero(ruby);
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
            return RubyFixnum.zero(ruby);
        }
        try {
            long l = Long.parseLong(sbuf.substring(0, pos), radix);
            return RubyFixnum.newFixnum(ruby, l);
        } catch (NumberFormatException ex) {
            BigInteger bi = new BigInteger(sbuf.substring(0, pos), radix);
            return new RubyBignum(ruby, bi);
        }
    }

    /**
     * Converts a string representation of a floating-point number to the 
     * numeric value.  Parsing starts at the beginning of the string (after 
     * leading and trailing whitespace have been removed), and stops at the 
     * end or at the first character that can't be part of a number.  If 
     * the string fails to parse as a number, 0.0 is returned.
     * 
     * @param ruby  the ruby runtime
     * @param arg   the string to be converted
     * @return  a RubyFloat representing the result of the conversion, which
     *          will be 0.0 if the conversion failed.
     */
    public static RubyFloat str2fnum(Ruby ruby, RubyString arg) {
        String str = arg.getValue().trim();
        double d = 0.0;
        int pos = str.length();
        for (int i = 0; i < pos; i++) {
            if ("0123456789eE+-.".indexOf(str.charAt(i)) == -1) {
                pos = i + 1;
                break;
            }
        }
        for (; pos > 0; pos--) {
            try {
                d = Double.parseDouble(str.substring(0, pos));
            } catch (NumberFormatException ex) {
                continue;
            }
            break;
        }
        return new RubyFloat(ruby, d);
    }

    /* Numeric methods. (num_*)
     *
     */

    /** num_coerce
     *
     */
    public RubyArray coerce(RubyNumeric other) {
        if (getMetaClass() == other.getMetaClass()) {
            return RubyArray.newArray(getRuntime(), other, this);
        } 
          
        return RubyArray.newArray(
                getRuntime(),
                RubyFloat.newFloat(getRuntime(), other.getDoubleValue()),
                RubyFloat.newFloat(getRuntime(), getDoubleValue()));
    }

    /**
     * !!!
     */
    public RubyNumeric[] getCoerce(RubyNumeric other) {
        if (getMetaClass() == other.getMetaClass()) {
            return new RubyNumeric[] { this, other };
        } else {
            return new RubyNumeric[] { RubyFloat.newFloat(getRuntime(), getDoubleValue()), RubyFloat.newFloat(getRuntime(), other.getDoubleValue())};
        }
    }

    /** num_clone
     *
     */
    public IRubyObject rbClone() {
        return this;
    }

    /** num_uplus
     *
     */
    public RubyNumeric op_uplus() {
        return this;
    }

    /** num_uminus
     *
     */
    public RubyNumeric op_uminus() {
        RubyNumeric[] coerce = getCoerce(RubyFixnum.zero(getRuntime()));

        return (RubyNumeric) coerce[1].callMethod("-", coerce[0]);
    }
    
    public RubyNumeric cmp(RubyNumeric other) {
        return RubyFixnum.newFixnum(getRuntime(), compareValue(other));
    }

    /** num_divmod
     *
     */
    public RubyArray divmod(RubyNumeric other) {
        RubyNumeric div = (RubyNumeric) callMethod("/", other);
        if (div instanceof RubyFloat) {
            double d = Math.floor(((RubyFloat) div).getValue());
            if (((RubyFloat) div).getValue() > d) {
                div = RubyFloat.newFloat(getRuntime(), d);
            }
        }

        return RubyArray.newArray(getRuntime(), div, modulo(other));
    }

    /** num_modulo
     *
     */
    public RubyNumeric modulo(RubyNumeric other) {
        return (RubyNumeric) callMethod("%", other);
    }

    /** num_remainder
     *
     */
    public RubyNumeric remainder(RubyNumeric other) {
        RubyNumeric mod = modulo(other);
        final RubyNumeric zero = RubyFixnum.zero(getRuntime());

        if (callMethod("<", zero).isTrue() && other.callMethod(">", zero).isTrue() || 
            callMethod(">", zero).isTrue() && other.callMethod("<", zero).isTrue()) {

            return (RubyNumeric) mod.callMethod("-", other);
        }

        return mod;
    }

    protected int compareValue(RubyNumeric other) {
        System.out.println("ALLYOURBASE");
        return -1;
    }
    
    /** num_equal
     *
     */
    public RubyBoolean veryEqual(IRubyObject other) {
        return super.equal(other); // +++ rb_equal
    }
    
    /** num_equal
     *
     */
    public RubyBoolean equal(IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyBoolean.newBoolean(getRuntime(), compareValue((RubyNumeric) other) == 0);
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
        return equal(other);
    }

    /** num_abs
     *
     */
    public RubyNumeric abs() {
        if (callMethod("<", RubyFixnum.zero(getRuntime())).isTrue()) {
            return (RubyNumeric) callMethod("-@");
        } else {
            return this;
        }
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
        return equal(RubyFixnum.zero(getRuntime()));
    }

    /** num_nonzero_p
     *
     */
    public IRubyObject nonzero_p() {
        if (callMethod("zero?").isTrue()) {
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

    public abstract RubyNumeric multiplyWith(RubyInteger value);

    public abstract RubyNumeric multiplyWith(RubyFloat value);

    public abstract RubyNumeric multiplyWith(RubyBignum value);
}
