/*
 * RubyBignum.java - description
 * Created on 15.03.2002, 16:53:36
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen, Benoit Cerrina
 * Copyright (C) 2002-2003 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.IOException;
import org.jruby.exceptions.RangeError;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.Asserts;
import org.jruby.internal.runtime.builtin.definitions.BignumDefinition;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyBignum extends RubyInteger {
    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    private final static BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    private final static BigInteger LONG_MIN = BigInteger.valueOf(-MAX-1);

    private final BigInteger value;

    public RubyBignum(Ruby ruby, BigInteger value) {
        super(ruby, ruby.getClass("Bignum"));
        this.value = value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public long getLongValue() {
        long result = getTruncatedLongValue();
        if (! BigInteger.valueOf(result).equals(value)) {
            throw new RangeError(runtime, "bignum too big to convert into 'int'");
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

    public static RubyClass createBignumClass(Ruby ruby) {
        return new BignumDefinition(ruby).getType();
    }

    public IRubyObject callIndexed(int index, IRubyObject[] args) {
        switch (index) {
        case BignumDefinition.OP_EQUAL:
            return op_equal(args[0]);
        case BignumDefinition.OP_INVERT:
            return op_invert();
        case BignumDefinition.OP_AND:
            return op_and(args[0]);
        case BignumDefinition.OP_LT:
            return op_lt(args[0]);
        case BignumDefinition.OP_LE:
            return op_le(args[0]);
        case BignumDefinition.OP_CMP:
            return op_cmp(args[0]);
        case BignumDefinition.OP_GT:
            return op_gt(args[0]);
        case BignumDefinition.OP_GE:
            return op_ge(args[0]);
        case BignumDefinition.OP_OR:
            return op_or(args[0]);
        case BignumDefinition.OP_XOR:
            return op_xor(args[0]);
        case BignumDefinition.OP_LSHIFT:
            return op_lshift(args[0]);
        case BignumDefinition.OP_RSHIFT:
            return op_rshift(args[0]);
        case BignumDefinition.OP_MOD:
            return op_mod(args[0]);
        case BignumDefinition.OP_DIV:
            return op_div(args[0]);
        case BignumDefinition.OP_PLUS:
            return op_plus(args[0]);
        case BignumDefinition.OP_MINUS:
            return op_minus(args[0]);
        case BignumDefinition.OP_MUL:
            return op_mul(args[0]);
        case BignumDefinition.OP_POW:
            return op_pow(args[0]);
        case BignumDefinition.COERCE:
            return coerce(args[0]);
        case BignumDefinition.REMAINDER:
            return remainder(args[0]);
        case BignumDefinition.OP_UMINUS:
            return op_uminus();
        case BignumDefinition.AREF:
            return aref(args[0]);
        case BignumDefinition.HASH:
            return hash();
        case BignumDefinition.SIZE:
            return size();
        case BignumDefinition.TO_F:
            return to_f();
        case BignumDefinition.TO_I:
            return to_i();
        case BignumDefinition.TO_S:
            return to_s();

	}

        return super.callIndexed(index, args);
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
            Asserts.notReached("argument must be an integer");
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
        return RubyFixnum.newFixnum(runtime, value.hashCode());
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
        return newBignum(runtime, value);
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
	    return bigNorm(getRuntime(), getValue().and(newBignum(getRuntime(), otherNumeric.getLongValue()).getValue()));
	    /*            return RubyFixnum.newFixnum(getRuntime(),
                                        getTruncatedLongValue() & otherNumeric.getLongValue());
	    */
        }
    }

    public RubyNumeric op_uminus() {
        return bigNorm(getRuntime(), getValue().negate());
    }

    public RubyNumeric op_invert() {
        return bigNorm(getRuntime(), getValue().not());
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
        return numericValue(num).multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return bigNorm(getRuntime(), getValue().multiply(bigIntValue(other)));
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
        return other.multiplyWith(RubyFloat.newFloat(getRuntime(), getDoubleValue()));
    }

    public RubyNumeric op_div(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(),
                                      getDoubleValue()).op_div(other);
        }

        BigInteger results[] =
                getValue().divideAndRemainder(bigIntValue(other));

        if (results[0].compareTo(BigInteger.ZERO) <= 0 &&
                results[1].compareTo(BigInteger.ZERO) != 0) {
            return bigNorm(getRuntime(), results[0].subtract(BigInteger.ONE));
        }


        return bigNorm(getRuntime(), results[0]);
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

    public RubyInteger op_or(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return newBignum(value.or(bigIntValue(other)));
    }

    public RubyInteger op_xor(IRubyObject num) {
        RubyNumeric other = numericValue(num);
        return newBignum(value.xor(bigIntValue(other)));
    }

    public RubyFixnum aref(IRubyObject other) {
	RubyNumeric pos = numericValue(other);
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
            throw new RangeError(runtime, "bignum too big to convert into `int'");
        return new RubyBignum(runtime, value.shiftLeft((int) shift));
    }

    public RubyBignum op_rshift(IRubyObject iNum) {
        long shift = numericValue(iNum).getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE)
            throw new RangeError(runtime, "bignum too big to convert into `int'");
        return new RubyBignum(runtime, value.shiftRight((int) shift));
    }

    public RubyFixnum size() {
        int byteLength = value.bitLength() / 8;
        if (value.bitLength() % 8 != 0) {
            byteLength++;
        }
        return RubyFixnum.newFixnum(runtime, byteLength);
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

        RubyBignum result = newBignum(input.getRuntime(), value);
        input.registerLinkTarget(result);
        return result;
    }
}
