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
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyBignum extends RubyInteger {
    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L<<(BIT_SIZE - 2)) - 1;
    private static final BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(-MAX-1);

    private final BigInteger value;

    public RubyBignum(IRuby runtime, BigInteger value) {
        super(runtime, runtime.getClass("Bignum"));
        this.value = value;
    }

    public double getDoubleValue() {
        return value.doubleValue();
    }

    public long getLongValue() {
        long result = getTruncatedLongValue();
        if (! BigInteger.valueOf(result).equals(value)) {
            throw getRuntime().newRangeError("bignum too big to convert into 'int'");
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

    /* If the value will fit in a Fixnum, return one of those. */
    private static RubyInteger bigNorm(IRuby runtime, BigInteger bi) {
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

    public static RubyBignum newBignum(IRuby runtime, long value) {
        return newBignum(runtime, BigInteger.valueOf(value));
    }

    public static RubyBignum newBignum(IRuby runtime, double value) {
        return newBignum(runtime, new BigDecimal(value).toBigInteger());
    }

    public static RubyBignum newBignum(IRuby runtime, BigInteger value) {
        return new RubyBignum(runtime, value);
    }

    public IRubyObject remainder(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).remainder(other);
        } else if (other instanceof RubyNumeric) {
            return bigNorm(getRuntime(), getValue().remainder(bigIntValue((RubyNumeric) other)));
        }
        
        return callCoerced("remainder", other);
    }

    public RubyNumeric multiplyWith(RubyInteger other) {
        return bigNorm(getRuntime(), getValue().multiply(bigIntValue(other)));
    }

    public RubyNumeric multiplyWith(RubyFloat other) {
        return other.multiplyWith(RubyFloat.newFloat(getRuntime(), getDoubleValue()));
    }
    
    public RubyNumeric quo(IRubyObject other) {
        return RubyFloat.newFloat(getRuntime(), ((RubyNumeric) op_div(other)).getDoubleValue());
    }

    public IRubyObject op_and(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bigNorm(getRuntime(), value.and(((RubyBignum) other).value));
        } else if (other instanceof RubyNumeric) {
	        return bigNorm(getRuntime(), 
	            getValue().and(newBignum(getRuntime(), ((RubyNumeric) other).getLongValue()).getValue()));
        }
        
        return callCoerced("&", other);
    }

    public IRubyObject op_div(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_div(other);
        } else if (other instanceof RubyNumeric) {
        	BigInteger otherBig = bigIntValue((RubyNumeric) other);
        	
        	if (otherBig.equals(BigInteger.ZERO)) {
        		throw getRuntime().newZeroDivisionError();
        	}
        	
            BigInteger[] results = getValue().divideAndRemainder(otherBig);

            if (results[0].compareTo(BigInteger.ZERO) <= 0 &&
                results[1].compareTo(BigInteger.ZERO) != 0) {
                return bigNorm(getRuntime(), results[0].subtract(BigInteger.ONE));
            }

            return bigNorm(getRuntime(), results[0]);
        }
        
        return callCoerced("/", other);
    }

    public IRubyObject op_invert() {
        return bigNorm(getRuntime(), getValue().not());
    }

    public IRubyObject op_lshift(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            long shift = ((RubyNumeric ) other).getLongValue();
            if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE) {
			    throw getRuntime().newRangeError("bignum too big to convert into `int'");
		    }
            return new RubyBignum(getRuntime(), value.shiftLeft((int) shift));
    	}
    	
    	return callCoerced("<<", other);
    }

    public IRubyObject op_minus(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_minus(other);
        } else if (other instanceof RubyNumeric) {
            return bigNorm(getRuntime(), getValue().subtract(bigIntValue((RubyNumeric) other)));
        }
        
        return callCoerced("-", other);
    }

    public IRubyObject op_mod(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).modulo(other);
        } else if (other instanceof RubyNumeric) {
            BigInteger m = bigIntValue((RubyNumeric ) other);
            BigInteger result = getValue().mod(m.abs());
            if (m.compareTo(BigInteger.ZERO) < 0) {
                result = m.add(result);
            }

            return bigNorm(getRuntime(), result);
        }
        
        return callCoerced("%", other);
    }

    public IRubyObject op_mul(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return ((RubyNumeric ) other).multiplyWith(this);
    	}
    	
    	return callCoerced("*", other);
    }

    public IRubyObject op_or(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return new RubyBignum(getRuntime(), value.or(bigIntValue((RubyNumeric ) other)));
    	}
    	
    	return callCoerced("|", other);
    }

    public IRubyObject op_plus(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return ((RubyFloat)other).op_plus(this);
        } else if (other instanceof RubyNumeric) {
            return bigNorm(getRuntime(), getValue().add(bigIntValue((RubyNumeric ) other)));
        }
        
        return callCoerced("+", other);
    }

    public IRubyObject op_pow(IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), getDoubleValue()).op_pow(other);
        } else if (other instanceof RubyNumeric) {
            return bigNorm(getRuntime(), getValue().pow((int) ((RubyNumeric) other).getLongValue()));
        }
        
        return callCoerced("**", other);
    }

    public IRubyObject op_rshift(IRubyObject other) {
        long shift = ((RubyNumeric ) other).getLongValue();
        if (shift > Integer.MAX_VALUE || shift < Integer.MIN_VALUE) {
			throw getRuntime().newRangeError("bignum too big to convert into `int'");
		} else if (other instanceof RubyNumeric) {
            return new RubyBignum(getRuntime(), value.shiftRight((int) shift));
		}
        
        return callCoerced(">>", other);
    }

    public IRubyObject op_uminus() {
        return bigNorm(getRuntime(), getValue().negate());
    }

    public IRubyObject op_xor(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return bigNorm(getRuntime(), value.xor(bigIntValue((RubyNumeric) other)));
    	}
    	
    	return callCoerced("^", other);
    }

    public RubyFixnum aref(IRubyObject other) {
        long pos = other.convertToInteger().getLongValue();
        boolean isSet = getValue().testBit((int) pos);
        return getRuntime().newFixnum(isSet ? 1 : 0);
    }

    public IRubyObject to_s(IRubyObject[] args) {
    	checkArgumentCount(args, 0, 1);

    	int radix = args.length == 0 ? 10 : (int) args[0].convertToInteger().getLongValue();

        return getRuntime().newString(getValue().toString(radix));
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

    public IRubyObject coerce(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return getRuntime().newArray(newBignum(getRuntime(), ((RubyFixnum)other).getLongValue()), this);
        }
        throw getRuntime().newTypeError("Can't coerce " + other.getMetaClass().getName() + " to Bignum");
    }
}
