/*
 * RubyBignum.java - description
 * Created on 15.03.2002, 16:53:36
 *
 * Copyright (C) 2001-2002 Jan Arne Petersen, Benoit Cerrina
 * Copyright (C) 2002-2004 Thomas E Enebo
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.jruby.exceptions.RangeError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class RubyBignum extends RubyInteger {
    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    private static final BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(-MAX-1);

    private final BigInteger value;

    public RubyBignum(Ruby runtime, BigInteger value) {
        super(runtime, runtime.getClass("Bignum"));
        this.value = value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public long getLongValue() {
        long result = getTruncatedLongValue();
        if (! BigInteger.valueOf(result).equals(value)) {
            throw new RangeError(getRuntime(), "bignum too big to convert into 'int'");
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

    public static RubyClass createBignumClass(Ruby runtime) {
        RubyClass result = runtime.defineClass("Bignum", runtime.getClasses().getIntegerClass());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyBignum.class);
        
        result.defineMethod("~", callbackFactory.getMethod("op_invert"));
        result.defineMethod("&", callbackFactory.getMethod("op_and", RubyNumeric.class));
        result.defineMethod("<<", callbackFactory.getMethod("op_lshift", RubyNumeric.class));
        result.defineMethod("%", callbackFactory.getMethod("op_mod", RubyNumeric.class));
        result.defineMethod("+", callbackFactory.getMethod("op_plus", RubyNumeric.class));
        result.defineMethod("*", callbackFactory.getMethod("op_mul", RubyNumeric.class));
        result.defineMethod("**", callbackFactory.getMethod("op_pow", RubyNumeric.class));
        result.defineMethod("-", callbackFactory.getMethod("op_minus", RubyNumeric.class));
        result.defineMethod("modulo", callbackFactory.getMethod("op_mod", RubyNumeric.class));
        result.defineMethod("/", callbackFactory.getMethod("op_div", RubyNumeric.class));
        result.defineMethod(">>", callbackFactory.getMethod("op_rshift", RubyNumeric.class));
        result.defineMethod("|", callbackFactory.getMethod("op_or", RubyNumeric.class));
        result.defineMethod("^", callbackFactory.getMethod("op_xor", RubyNumeric.class));
        result.defineMethod("-@", callbackFactory.getMethod("op_uminus"));
        result.defineMethod("[]", callbackFactory.getMethod("aref", RubyNumeric.class));
        result.defineMethod("coerce", callbackFactory.getMethod("coerce", RubyNumeric.class));
        result.defineMethod("remainder", callbackFactory.getMethod("remainder", RubyNumeric.class));
        result.defineMethod("hash", callbackFactory.getMethod("hash"));
        result.defineMethod("size", callbackFactory.getMethod("size"));
        result.defineMethod("quo", callbackFactory.getMethod("quo", RubyNumeric.class));
        result.defineMethod("to_f", callbackFactory.getMethod("to_f"));
        result.defineMethod("to_i", callbackFactory.getMethod("to_i"));
        result.defineMethod("to_s", callbackFactory.getMethod("to_s"));
        
        return result;
    }

    /* If the value will fit in a Fixnum, return one of those. */
    private static RubyInteger bigNorm(Ruby runtime, BigInteger bi) {
        if (bi.compareTo(LONG_MIN) < 0 || bi.compareTo(LONG_MAX) > 0) {
            return newBignum(runtime, bi);
        }
        return runtime.newFixnum(bi.longValue());
    }

    public static BigInteger bigIntValue(RubyNumeric other) {
        assert !(other instanceof RubyFloat) : "argument must be an integer";

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
        return getRuntime().newFixnum(value.hashCode());
    }

    // Bignum methods

    public static RubyBignum newBignum(Ruby runtime, long value) {
        return newBignum(runtime, BigInteger.valueOf(value));
    }

    public static RubyBignum newBignum(Ruby runtime, double value) {
        return newBignum(runtime, new BigDecimal(value).toBigInteger());
    }

    public static RubyBignum newBignum(Ruby runtime, BigInteger value) {
        return new RubyBignum(runtime, value);
    }

    public RubyNumeric op_mod(RubyNumeric other) {
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

    public RubyNumeric op_and(RubyNumeric other) {
        if (other instanceof RubyBignum) {
            return bigNorm(getRuntime(),
                           value.and(((RubyBignum) other).value));
        }
        
	    return bigNorm(getRuntime(), 
	            getValue().and(newBignum(getRuntime(), other.getLongValue()).getValue()));
    }

    public RubyNumeric op_uminus() {
        return bigNorm(getRuntime(), getValue().negate());
    }

    public RubyNumeric op_invert() {
        return bigNorm(getRuntime(), getValue().not());
    }

    public RubyNumeric remainder(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).remainder(other);
        }
        return bigNorm(getRuntime(), getValue().remainder(bigIntValue(other)));
    }

    public RubyNumeric op_plus(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return ((RubyFloat)other).op_plus(this);
        }
        return bigNorm(getRuntime(), getValue().add(bigIntValue(other)));
    }

    public RubyNumeric op_minus(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_minus(other);
        }
        return bigNorm(getRuntime(), getValue().subtract(bigIntValue(other)));
    }

    public RubyNumeric op_mul(RubyNumeric other) {
        return other.multiplyWith(this);
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return bigNorm(getRuntime(), getValue().multiply(bigIntValue(other)));
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
        return other.multiplyWith(RubyFloat.newFloat(getRuntime(), getDoubleValue()));
    }
    
    public RubyNumeric quo(RubyNumeric other) {
        return RubyFloat.newFloat(getRuntime(), op_div(other).getDoubleValue());
    }

    public RubyNumeric op_div(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(),
                                      getDoubleValue()).op_div(other);
        }

        BigInteger[] results =
                getValue().divideAndRemainder(bigIntValue(other));

        if (results[0].compareTo(BigInteger.ZERO) <= 0 &&
                results[1].compareTo(BigInteger.ZERO) != 0) {
            return bigNorm(getRuntime(), results[0].subtract(BigInteger.ONE));
        }

        return bigNorm(getRuntime(), results[0]);
    }

    public RubyNumeric op_pow(RubyNumeric other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        }
        
        return bigNorm(getRuntime(), getValue().pow((int) other.getLongValue()));
    }

    public RubyInteger op_or(RubyNumeric other) {
        return new RubyBignum(getRuntime(), value.or(bigIntValue(other)));
    }

    public RubyInteger op_xor(RubyNumeric other) {
        return new RubyBignum(getRuntime(), value.xor(bigIntValue(other)));
    }

    public RubyFixnum aref(RubyNumeric pos) {
        boolean isSet = getValue().testBit((int) pos.getLongValue());
        return getRuntime().newFixnum(isSet ? 1 : 0);
    }

    public RubyString to_s() {
        return getRuntime().newString(getValue().toString());
    }

    public RubyFloat to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public RubyNumeric[] getCoerce(RubyNumeric other) {
        if (!(other instanceof RubyInteger)) {
            return new RubyNumeric[] { other, this };
        }
        return new RubyNumeric[] { RubyFloat.newFloat(getRuntime(), other.getDoubleValue()), RubyFloat.newFloat(getRuntime(), getDoubleValue())};
    }

    public RubyBignum op_lshift(RubyNumeric other) {
        long shift = other.getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE) {
			throw new RangeError(getRuntime(), "bignum too big to convert into `int'");
		}
        return new RubyBignum(getRuntime(), value.shiftLeft((int) shift));
    }

    public RubyBignum op_rshift(RubyNumeric other) {
        long shift = other.getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE) {
			throw new RangeError(getRuntime(), "bignum too big to convert into `int'");
		}
        return new RubyBignum(getRuntime(), value.shiftRight((int) shift));
    }

    public RubyFixnum size() {
        int byteLength = value.bitLength() / 8;
        if (value.bitLength() % 8 != 0) {
            byteLength++;
        }
        return getRuntime().newFixnum(byteLength);
    }

    public void marshalTo(MarshalStream output) throws IOException {
        output.write('l');
        output.write(value.signum() >= 0 ? '+' : '-');

        BigInteger absValue = value.abs();

        byte[] digits = absValue.toByteArray();

        boolean oddLengthNonzeroStart = (digits.length % 2 != 0 && digits[0] != 0);
        int shortLength = digits.length / 2;
        if (oddLengthNonzeroStart) {
			shortLength++;
		}
        output.dumpInt(shortLength);
        
        for (int i = 1; i <= shortLength * 2 && i <= digits.length; i++) {
        	output.write(digits[digits.length - i]);
        }
        
        if (oddLengthNonzeroStart) {
            // Pad with a 0
            output.write(0);
        }
    }
    public static RubyBignum unmarshalFrom(UnmarshalStream input) throws IOException {
        boolean positive = input.readUnsignedByte() == '+';
        int shortLength = input.unmarshalInt();

        // BigInteger required a sign byte in incoming array
        byte[] digits = new byte[shortLength * 2 + 1];

        for (int i = digits.length - 1; i >= 1; i--) {
        	digits[i] = input.readSignedByte();
        }

        BigInteger value = new BigInteger(digits);
        if (!positive) {
            value = value.negate();
        }

        RubyBignum result = newBignum(input.getRuntime(), value);
        input.registerLinkTarget(result);
        return result;
    }
}
