/*
 * RubyBignum.java - description
 * Created on 15.03.2002, 16:53:36
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

import java.math.*;
import java.io.IOException;
import org.jruby.exceptions.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.*;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyBignum extends RubyInteger {
    private final static BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    private final static BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    private BigInteger value;

    public RubyBignum(Ruby ruby) {
        this(ruby, BigInteger.ZERO);
    }

    public RubyBignum(Ruby ruby, BigInteger value) {
        super(ruby, ruby.getClasses().getBignumClass());

        this.value = value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public long getLongValue() {
        long result = getTruncatedLongValue();
        if (! BigInteger.valueOf(result).equals(value)) {
            throw new RangeError(ruby, "bignum too big to convert into 'int'");
        }
        return result;
    }

    public long getTruncatedLongValue() {
        return value.longValue();
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public BigInteger getValue() {
        return value;
    }

    /** Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(BigInteger value) {
        this.value = value;
    }

    public static RubyClass createBignumClass(Ruby ruby) {
        RubyClass bignumClass = ruby.defineClass("Bignum", ruby.getClasses().getIntegerClass());
        //rb_define_method(rb_cBignum, "-@", rb_big_uminus, 0);
        //rb_define_method(rb_cBignum, "divmod", rb_big_divmod, 1);
        //rb_define_method(rb_cBignum, "modulo", rb_big_modulo, 1);
        //rb_define_method(rb_cBignum, "remainder", rb_big_remainder, 1);
        //rb_define_method(rb_cBignum, "&", rb_big_and, 1);
        //    rb_define_method(rb_cBignum, "~", rb_big_neg, 0);
        //    rb_define_method(rb_cBignum, "[]", rb_big_aref, 1);
        //
        //rb_define_method(rb_cBignum, "===", rb_big_eq, 1);
        //    rb_define_method(rb_cBignum, "eql?", rb_big_eq, 1);

        bignumClass.defineMethod("-@", CallbackFactory.getMethod(RubyBignum.class, "op_uminus"));

        bignumClass.defineMethod("modulo", CallbackFactory.getMethod(RubyBignum.class, "op_mod", IRubyObject.class));
        bignumClass.defineMethod("remainder", CallbackFactory.getMethod(RubyBignum.class, "remainder", IRubyObject.class));

        bignumClass.defineMethod("to_s", CallbackFactory.getMethod(RubyBignum.class, "to_s"));
        bignumClass.defineMethod("coerce", CallbackFactory.getMethod(RubyBignum.class, "coerce", IRubyObject.class));
        bignumClass.defineMethod("to_f", CallbackFactory.getMethod(RubyBignum.class, "to_f"));

        bignumClass.defineMethod("+", CallbackFactory.getMethod(RubyBignum.class, "op_plus", IRubyObject.class));
        bignumClass.defineMethod("-", CallbackFactory.getMethod(RubyBignum.class, "op_minus", IRubyObject.class));
        bignumClass.defineMethod("*", CallbackFactory.getMethod(RubyBignum.class, "op_mul", IRubyObject.class));
        bignumClass.defineMethod("/", CallbackFactory.getMethod(RubyBignum.class, "op_div", IRubyObject.class));
        bignumClass.defineMethod("%", CallbackFactory.getMethod(RubyBignum.class, "op_mod", IRubyObject.class));
        bignumClass.defineMethod("&", CallbackFactory.getMethod(RubyBignum.class, "op_and", IRubyObject.class));
        bignumClass.defineMethod("**", CallbackFactory.getMethod(RubyBignum.class, "op_pow", IRubyObject.class));

        bignumClass.defineMethod("<<", CallbackFactory.getMethod(RubyBignum.class, "op_lshift", IRubyObject.class));
        bignumClass.defineMethod(">>", CallbackFactory.getMethod(RubyBignum.class, "op_rshift", IRubyObject.class));

        bignumClass.defineMethod("==", CallbackFactory.getMethod(RubyBignum.class, "op_equal", IRubyObject.class));
        bignumClass.defineMethod("<=>", CallbackFactory.getMethod(RubyBignum.class, "op_cmp", IRubyObject.class));
        bignumClass.defineMethod(">", CallbackFactory.getMethod(RubyBignum.class, "op_gt", IRubyObject.class));
        bignumClass.defineMethod(">=", CallbackFactory.getMethod(RubyBignum.class, "op_ge", IRubyObject.class));
        bignumClass.defineMethod("<", CallbackFactory.getMethod(RubyBignum.class, "op_lt", IRubyObject.class));
        bignumClass.defineMethod("<=", CallbackFactory.getMethod(RubyBignum.class, "op_le", IRubyObject.class));
        bignumClass.defineMethod("|", CallbackFactory.getMethod(RubyBignum.class, "op_or", RubyInteger.class));
        bignumClass.defineMethod("^", CallbackFactory.getMethod(RubyBignum.class, "op_xor", RubyInteger.class));
        bignumClass.defineMethod("[]", CallbackFactory.getMethod(RubyBignum.class, "aref", RubyInteger.class));
        bignumClass.defineMethod("size", CallbackFactory.getMethod(RubyBignum.class, "size"));
		bignumClass.defineMethod("hash", CallbackFactory.getMethod(RubyBignum.class, "hash"));

        return bignumClass;
    }

    /* If the value will fit in a Fixnum, return one of those. */
    private static RubyInteger bigNorm(Ruby ruby, BigInteger bi) {
        if (bi.compareTo(LONG_MIN) < 0 || bi.compareTo(LONG_MAX) > 0) {
            return newBignum(ruby, bi);
        }
        return RubyFixnum.newFixnum(ruby, bi.longValue());
    }

    static public BigInteger bigIntValue(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            throw new RubyBugException("argument must be an integer");
        }
        return (other instanceof RubyBignum) ? ((RubyBignum) other).getValue() : BigInteger.valueOf(other.getLongValue());
    }

    protected int compareValue(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            double otherVal = other.getDoubleValue();
            double thisVal = getDoubleValue();
            return thisVal > otherVal ? 1 : thisVal < otherVal ? -1 : 0;
        }

        return getValue().compareTo(bigIntValue(other));
    }

    public RubyFixnum hash() {
        return RubyFixnum.newFixnum(ruby, value.hashCode());
    }

    // Bignum methods

    public static RubyBignum newBignum(Ruby ruby, long value) {
        return newBignum(ruby, BigInteger.valueOf(value));
    }

    public static RubyBignum newBignum(Ruby ruby, double value) {
        return newBignum(ruby, new BigDecimal(value).toBigInteger());
    }

    public static RubyBignum newBignum(Ruby ruby, BigInteger value) {
        return new RubyBignum(ruby, value);
    }

    public RubyBignum newBignum(BigInteger value) {
        return newBignum(ruby, value);
    }

    public RubyNumeric op_mod(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).modulo(other);
        }

        BigInteger m = bigIntValue(other);
        BigInteger result = getValue().mod(m.abs());
        if (m.compareTo(BigInteger.ZERO) < 0) {
            result = m.add(result);
        }

        return bigNorm(getRuntime(), result);
    }

    public RubyNumeric op_and(IRubyObject other) {
        RubyNumeric otherNumeric = numericValue(other);
        if (otherNumeric instanceof RubyBignum) {
            return bigNorm(getRuntime(),
                           value.and(((RubyBignum) other).value));
        } else {
            return RubyFixnum.newFixnum(getRuntime(),
                                        getTruncatedLongValue() & otherNumeric.getLongValue());
        }
    }

    public RubyNumeric op_uminus() {
        return bigNorm(getRuntime(), getValue().negate());
    }

    public RubyNumeric remainder(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).remainder(other);
        }
        return bigNorm(getRuntime(), getValue().remainder(bigIntValue(other)));
    }

    public RubyNumeric op_plus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return ((RubyFloat)other).op_plus(this);
        }
        return bigNorm(getRuntime(), getValue().add(bigIntValue(other)));
    }

    public RubyNumeric op_minus(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_minus(other);
        }
        return bigNorm(getRuntime(), getValue().subtract(bigIntValue(other)));
    }

    public RubyNumeric op_mul(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_mul(other);
        }
        return bigNorm(getRuntime(), getValue().multiply(bigIntValue(other)));
    }

    public RubyNumeric op_div(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_div(other);
        }
        return bigNorm(getRuntime(), getValue().divide(bigIntValue(other)));
    }

    public RubyNumeric op_pow(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        } else {
            return bigNorm(getRuntime(), getValue().pow((int) other.getLongValue()));
        }
    }

    public RubyBoolean op_equal(IRubyObject other) {
        if (!(other instanceof RubyNumeric)) {
            return getRuntime().getFalse();
        } else {
            return RubyBoolean.newBoolean(getRuntime(), compareValue((RubyNumeric) other) == 0);
        }
    }

    public RubyNumeric op_cmp(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyFixnum.newFixnum(getRuntime(), compareValue(other));
    }

    public RubyBoolean op_gt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) > 0);
    }

    public RubyBoolean op_ge(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) >= 0);
    }

    public RubyBoolean op_lt(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) < 0);
    }

    public RubyBoolean op_le(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return RubyBoolean.newBoolean(getRuntime(), compareValue(other) <= 0);
    }

    public RubyInteger op_or(RubyInteger other) {
        return newBignum(value.or(bigIntValue(other)));
    }

    public RubyInteger op_xor(RubyInteger other) {
        return newBignum(value.xor(bigIntValue(other)));
    }

    public RubyFixnum aref(RubyInteger pos) {
        boolean isSet = getValue().testBit((int) pos.getLongValue());
        return RubyFixnum.newFixnum(getRuntime(), (isSet ? 1 : 0));
    }

    public RubyString to_s() {
        return RubyString.newString(getRuntime(), getValue().toString());
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public RubyNumeric[] coerce(RubyNumeric iNum) {
        RubyNumeric other = numericValue(iNum);
        if (!(iNum instanceof RubyInteger)) {

            return new RubyNumeric[] { other, this };
        }
        return new RubyNumeric[] { RubyFloat.newFloat(getRuntime(), other.getDoubleValue()), RubyFloat.newFloat(getRuntime(), getDoubleValue())};
    }

    public RubyBignum op_lshift(IRubyObject iNum) {
        long shift = numericValue(iNum).getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE)
            throw new RangeError(ruby, "bignum too big to convert into `int'");
        return new RubyBignum(ruby, value.shiftLeft((int) shift));
    }

    public RubyBignum op_rshift(IRubyObject iNum) {
        long shift = numericValue(iNum).getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE)
            throw new RangeError(ruby, "bignum too big to convert into `int'");
        return new RubyBignum(ruby, value.shiftRight((int) shift));
    }

    public RubyFixnum size() {
        int byteLength = value.bitLength() / 8;
        if (value.bitLength() % 8 != 0) {
            byteLength++;
        }
        return RubyFixnum.newFixnum(ruby, byteLength);
    }

    public void marshalTo(MarshalStream output) throws IOException {
        output.write('l');
        output.write(value.signum() >= 0 ? '+' : '-');

        BigInteger absValue = value.abs();

        int bitLength = absValue.bitLength();

        int shortLength = bitLength / 16;
        if (bitLength % 16 != 0) {
            shortLength++;
        }

        output.dumpInt(shortLength);

        byte[] digits = absValue.toByteArray();

        for (int i = digits.length - 1; i >= 0; i--) {
            if (i == 0 && digits[i] == 0 && (digits.length % 2 != 0)) {
                // Don't write last byte if the full length
                // would be odd and the digits end with a zero byte.
                break;
            }
            output.write(digits[i]);
        }
        if (digits[0] != 0 && (digits.length % 2 != 0)) {
            // Pad with a 0 if we've written all bytes and have an odd length.
            output.write(0);
        }
    }

    public static RubyBignum unmarshalFrom(UnmarshalStream input) throws IOException {
        int signum = (input.readUnsignedByte() == '+' ? 1 : -1);
        int shortLength = input.unmarshalInt();

        byte[] digits = new byte[shortLength * 2];
        for (int i = digits.length - 1; i >= 0; i--) {
            digits[i] = input.readSignedByte();
        }

        BigInteger value = new BigInteger(digits);
        if (signum == -1) {
            value = value.negate();
        }

        return newBignum(input.getRuby(), value);
    }
}
