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
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
public class RubyBignum extends RubyInteger {
    public static RubyClass createBignumClass(Ruby runtime) {
        RubyClass bignum = runtime.defineClass("Bignum", runtime.getInteger(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setBignum(bignum);
        bignum.index = ClassIndex.BIGNUM;
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyBignum.class);
        
        bignum.defineAnnotatedMethods(RubyBignum.class);
        bignum.dispatcher = callbackFactory.createDispatcher(bignum);

        return bignum;
    }

    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L << (BIT_SIZE - 1)) - 1;
    private static final BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    private static final BigInteger LONG_MIN = BigInteger.valueOf(-MAX - 1);

    private final BigInteger value;

    public RubyBignum(Ruby runtime, BigInteger value) {
        super(runtime, runtime.getBignum());
        this.value = value;
    }
    
    public int getNativeTypeIndex() {
        return ClassIndex.BIGNUM;
    }

    public static RubyBignum newBignum(Ruby runtime, long value) {
        return newBignum(runtime, BigInteger.valueOf(value));
    }

    public static RubyBignum newBignum(Ruby runtime, double value) {
        return newBignum(runtime, new BigDecimal(value).toBigInteger());
    }

    public static RubyBignum newBignum(Ruby runtime, BigInteger value) {
        return new RubyBignum(runtime, value);
    }

    public static RubyBignum newBignum(Ruby runtime, String value) {
        return new RubyBignum(runtime, new BigInteger(value));
    }

    public double getDoubleValue() {
        return big2dbl(this);
    }

    public long getLongValue() {
        return big2long(this);
    }

    /** Getter for property value.
     * @return Value of property value.
     */
    public BigInteger getValue() {
        return value;
    }

    /*  ================
     *  Utility Methods
     *  ================ 
     */

    /* If the value will fit in a Fixnum, return one of those. */
    /** rb_big_norm
     * 
     */
    public static RubyInteger bignorm(Ruby runtime, BigInteger bi) {
        if (bi.compareTo(LONG_MIN) < 0 || bi.compareTo(LONG_MAX) > 0) {
            return newBignum(runtime, bi);
        }
        return runtime.newFixnum(bi.longValue());
    }

    /** rb_big2long
     * 
     */
    public static long big2long(RubyBignum value) {
        BigInteger big = value.getValue();

        if (big.compareTo(LONG_MIN) < 0 || big.compareTo(LONG_MAX) > 0) {
            throw value.getRuntime().newRangeError("bignum too big to convert into `long'");
    }
        return big.longValue();
        }

    /** rb_big2dbl
     * 
     */
    public static double big2dbl(RubyBignum value) {
        BigInteger big = value.getValue();
        double dbl = big.doubleValue();
        if (dbl == Double.NEGATIVE_INFINITY || dbl == Double.POSITIVE_INFINITY) {
            value.getRuntime().getWarnings().warn("Bignum out of Float range");
    }
        return dbl;
    }

    /** rb_int2big
     * 
     */
    public static BigInteger fix2big(RubyFixnum arg) {
        return BigInteger.valueOf(arg.getLongValue());
    }

    /*  ================
     *  Instance Methods
     *  ================ 
     */

    /** rb_big_to_s
     * 
     */
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 1);
        int base = args.length == 0 ? 10 : num2int(args[0]);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
    }
        return getRuntime().newString(getValue().toString(base));
    }

    /** rb_big_coerce
     * 
     */
    @JRubyMethod(name = "coerce", required = 1)
    public IRubyObject coerce(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return getRuntime().newArray(newBignum(getRuntime(), ((RubyFixnum) other).getLongValue()), this);
        } else if (other instanceof RubyBignum) {
            return getRuntime().newArray(newBignum(getRuntime(), ((RubyBignum) other).getValue()), this);
    }

        throw getRuntime().newTypeError("Can't coerce " + other.getMetaClass().getName() + " to Bignum");
    }

    /** rb_big_uminus
     * 
     */
    @JRubyMethod(name = "-@")
    public IRubyObject op_uminus() {
        return bignorm(getRuntime(), value.negate());
    }

    /** rb_big_plus
     * 
     */
    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.add(fix2big(((RubyFixnum) other))));
        }
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.add(((RubyBignum) other).value));
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) + ((RubyFloat) other).getDoubleValue());
    }
        return coerceBin("+", other);
    }

    /** rb_big_minus
     * 
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.subtract(fix2big(((RubyFixnum) other))));
    }
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.subtract(((RubyBignum) other).value));
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) - ((RubyFloat) other).getDoubleValue());
    }
        return coerceBin("-", other);
    }

    /** rb_big_mul
     * 
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.multiply(fix2big(((RubyFixnum) other))));
        }
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.multiply(((RubyBignum) other).value));
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin("*", other);
    }

    /** rb_big_div
     * 
     */
    @JRubyMethod(name = {"/", "div", "quo"}, required = 1)
    public IRubyObject op_div(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) / ((RubyFloat) other).getDoubleValue());
        } else {
            return coerceBin("/", other);
        }

        if (otherValue.equals(BigInteger.ZERO)) {
            throw getRuntime().newZeroDivisionError();
        }

        BigInteger[] results = value.divideAndRemainder(otherValue);

        if (results[0].signum() == -1 && results[1].signum() != 0) {
            return bignorm(getRuntime(), results[0].subtract(BigInteger.ONE));
        }
        return bignorm(getRuntime(), results[0]);
    }

    /** rb_big_divmod
     * 
     */
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin("divmod", other);
        }

        if (otherValue.equals(BigInteger.ZERO)) {
            throw getRuntime().newZeroDivisionError();
        }

        BigInteger[] results = value.divideAndRemainder(otherValue);

        if (results[0].signum() == -1 && results[1].signum() != 0) {
            return bignorm(getRuntime(), results[0].subtract(BigInteger.ONE));
    	}
        final Ruby runtime = getRuntime();
        return RubyArray.newArray(getRuntime(), bignorm(runtime, results[0]), bignorm(runtime, results[1]));
    }

    /** rb_big_modulo
     * 
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin("%", other);
        }
        if (otherValue.equals(BigInteger.ZERO)) {
            throw getRuntime().newZeroDivisionError();
        }
        BigInteger result = value.mod(otherValue.abs());
        if (otherValue.signum() == -1) {
            result = otherValue.add(result);
        }
        return bignorm(getRuntime(), result);

            }

    /** rb_big_remainder
     * 
     */
    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big(((RubyFixnum) other));
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin("remainder", other);
        }
        if (otherValue.equals(BigInteger.ZERO)) {
            throw getRuntime().newZeroDivisionError();
    }
        return bignorm(getRuntime(), value.remainder(otherValue));
    }

    /** rb_big_quo

     * 
     */
    @JRubyMethod(name = "quo", required = 1)
    public IRubyObject quo(IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) / ((RubyNumeric) other).getDoubleValue());
        } else {
            return coerceBin("quo", other);
    	}
    }

    /** rb_big_pow
     * 
     */
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow(IRubyObject other) {
        double d;
        if (other instanceof RubyFixnum) {
            RubyFixnum fix = (RubyFixnum) other;
            long fixValue = fix.getLongValue();
            // MRI issuses warning here on (RBIGNUM(x)->len * SIZEOF_BDIGITS * yy > 1024*1024)
            if (((value.bitLength() + 7) / 8) * 4 * Math.abs(fixValue) > 1024 * 1024) {
                getRuntime().getWarnings().warn("in a**b, b may be too big");
    	}
            if (fixValue >= 0) {
                return bignorm(getRuntime(), value.pow((int) fixValue)); // num2int is also implemented
            } else {
                return RubyFloat.newFloat(getRuntime(), Math.pow(big2dbl(this), (double)fixValue));
            }
        } else if (other instanceof RubyBignum) {
            getRuntime().getWarnings().warn("in a**b, b may be too big");
            d = ((RubyBignum) other).getDoubleValue();
        } else if (other instanceof RubyFloat) {
            d = ((RubyFloat) other).getDoubleValue();
        } else {
            return coerceBin("**", other);

    }
        return RubyFloat.newFloat(getRuntime(), Math.pow(big2dbl(this), d));
    }

    /** rb_big_and
     * 
     */
    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and(IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.and(((RubyBignum) other).value));
        } else if(other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.and(fix2big((RubyFixnum)other)));
        }
        return coerceBin("&", other);
    }

    /** rb_big_or
     * 
     */
    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or(IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.or(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) { // no bignorm here needed
            return bignorm(getRuntime(), value.or(fix2big((RubyFixnum)other)));
        }
        return coerceBin("|", other);
    }

    /** rb_big_xor
     * 
     */
    @JRubyMethod(name = "^", required = 1)
    public IRubyObject op_xor(IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.xor(((RubyBignum) other).value));
		}
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.xor(BigInteger.valueOf(((RubyFixnum) other).getLongValue())));
    }

        return coerceBin("^", other);
    }

    /** rb_big_neg     
     * 
     */
    @JRubyMethod(name = "~")
    public IRubyObject op_neg() {
        return RubyBignum.newBignum(getRuntime(), value.not());
    	}

    /** rb_big_lshift     
     * 
     */
    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject op_lshift(IRubyObject other) {
        int width = num2int(other);
        if (width < 0) {
            return op_rshift(RubyFixnum.newFixnum(getRuntime(), -width));
        }
    	
        return bignorm(getRuntime(), value.shiftLeft(width));
    }

    /** rb_big_rshift     
     * 
     */
    @JRubyMethod(name = ">>", required = 1)
    public IRubyObject op_rshift(IRubyObject other) {
        int width = num2int(other);

        if (width < 0) {
            return op_lshift(RubyFixnum.newFixnum(getRuntime(), -width));
        }
        return bignorm(getRuntime(), value.shiftRight(width));
    }

    /** rb_big_aref     
     * 
     */
    @JRubyMethod(name = "[]", required = 1)
    public RubyFixnum op_aref(IRubyObject other) {
        if (other instanceof RubyBignum) {
            if (((RubyBignum) other).value.signum() >= 0 || value.signum() == -1) {
                return RubyFixnum.zero(getRuntime());
            }
            return RubyFixnum.one(getRuntime());
        }
        long position = num2long(other);
        if (position < 0 || position > Integer.MAX_VALUE) {
            return RubyFixnum.zero(getRuntime());
        }
        
        return value.testBit((int)position) ? RubyFixnum.one(getRuntime()) : RubyFixnum.zero(getRuntime());
    }

    /** rb_big_cmp     
     * 
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            return dbl_cmp(getRuntime(), big2dbl(this), ((RubyFloat) other).getDoubleValue());
        } else {
            return coerceCmp("<=>", other);
        }

        // wow, the only time we can use the java protocol ;)        
        return RubyFixnum.newFixnum(getRuntime(), value.compareTo(otherValue));
    }

    /** rb_big_eq     
     * 
     */
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            double a = ((RubyFloat) other).getDoubleValue();
            if (Double.isNaN(a)) {
                return getRuntime().getFalse();
            }
            return RubyBoolean.newBoolean(getRuntime(), a == big2dbl(this));
        } else {
            return super.op_equal(other);
        }
        return RubyBoolean.newBoolean(getRuntime(), value.compareTo(otherValue) == 0);
    }

    /** rb_big_eql     
     * 
     */
    @JRubyMethod(name = {"eql?", "==="}, required = 1)
    public IRubyObject eql_p(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return value.compareTo(((RubyBignum)other).value) == 0 ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        return getRuntime().getFalse();
    }

    /** rb_big_hash
     * 
     */
    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
        }

    /** rb_big_to_f
     * 
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    /** rb_big_abs
     * 
     */
    @JRubyMethod(name = "abs")
    public IRubyObject abs() {
        return RubyBignum.newBignum(getRuntime(), value.abs());
    }

    /** rb_big_size
     * 
     */
    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return getRuntime().newFixnum((value.bitLength() + 7) / 8);
    }

    public static void marshalTo(RubyBignum bignum, MarshalStream output) throws IOException {
        output.write(bignum.value.signum() >= 0 ? '+' : '-');
        
        BigInteger absValue = bignum.value.abs();
        
        byte[] digits = absValue.toByteArray();
        
        boolean oddLengthNonzeroStart = (digits.length % 2 != 0 && digits[0] != 0);
        int shortLength = digits.length / 2;
        if (oddLengthNonzeroStart) {
            shortLength++;
        }
        output.writeInt(shortLength);
        
        for (int i = 1; i <= shortLength * 2 && i <= digits.length; i++) {
            output.write(digits[digits.length - i]);
        }
        
        if (oddLengthNonzeroStart) {
            // Pad with a 0
            output.write(0);
        }
    }

    public static RubyNumeric unmarshalFrom(UnmarshalStream input) throws IOException {
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

        RubyNumeric result = bignorm(input.getRuntime(), value);
        input.registerLinkTarget(result);
        return result;
    }
}
