/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.MarshalStream;
import org.jruby.runtime.marshal.UnmarshalStream;

/**
 *
 * @author  jpetersen
 */
@JRubyClass(name="Bignum", parent="Integer")
public class RubyBignum extends RubyInteger {
    public static RubyClass createBignumClass(Ruby runtime) {
        RubyClass bignum = runtime.defineClass("Bignum", runtime.getInteger(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setBignum(bignum);

        bignum.setClassIndex(ClassIndex.BIGNUM);
        bignum.setReifiedClass(RubyBignum.class);

        bignum.defineAnnotatedMethods(RubyBignum.class);

        return bignum;
    }

    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L << (BIT_SIZE - 1)) - 1;
    public static final BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    public static final BigInteger LONG_MIN = BigInteger.valueOf(-MAX - 1);
    public static final BigInteger ULONG_MAX = BigInteger.valueOf(1).shiftLeft(BIT_SIZE).subtract(BigInteger.valueOf(1));

    private final BigInteger value;

    public RubyBignum(Ruby runtime, BigInteger value) {
        super(runtime, runtime.getBignum());
        this.value = value;
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.BIGNUM;
    }

    @Override
    public Class<?> getJavaClass() {
        return BigInteger.class;
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

    @Override
    public double getDoubleValue() {
        return big2dbl(this);
    }

    @Override
    public long getLongValue() {
        return big2long(this);
    }

    @Override
    public int getIntValue() {
        return (int)big2long(this);
    }

    @Override
    public BigInteger getBigIntegerValue() {
        return value;
    }

    @Override
    public RubyClass getSingletonClass() {
        throw getRuntime().newTypeError("can't define singleton");
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

    /** rb_big2ulong
     * This is here because for C extensions ulong can hold different values without throwing a RangeError
     */
    public static long big2ulong(RubyBignum value) {
        BigInteger big = value.getValue();

        if (big.compareTo(LONG_MIN) <= 0 || big.compareTo(ULONG_MAX) > 0) {
            throw value.getRuntime().newRangeError("bignum too big to convert into `ulong'");
        }
        return value.getValue().longValue();
    }

    /** rb_big2dbl
     *
     */
    public static double big2dbl(RubyBignum value) {
        BigInteger big = value.getValue();
        double dbl = convertToDouble(big);
        if (dbl == Double.NEGATIVE_INFINITY || dbl == Double.POSITIVE_INFINITY) {
            value.getRuntime().getWarnings().warn(ID.BIGNUM_FROM_FLOAT_RANGE, "Bignum out of Float range");
    }
        return dbl;
    }

    private IRubyObject checkShiftDown(RubyBignum other) {
        if (other.value.signum() == 0) return RubyFixnum.zero(getRuntime());
        if (value.compareTo(LONG_MIN) < 0 || value.compareTo(LONG_MAX) > 0) {
            return other.value.signum() >= 0 ? RubyFixnum.zero(getRuntime()) : RubyFixnum.minus_one(getRuntime());
        }
        return getRuntime().getNil();
    }

    /**
     * BigInteger#doubleValue is _really_ slow currently.
     * This is faster, and mostly correct (?)
     */
    static double convertToDouble(BigInteger bigint) {
        long signum = (bigint.signum() == -1) ? 1l << 63 : 0;
        bigint = bigint.abs();
        int len = bigint.bitLength();
        if (len == 0) return 0d;
        long exp = len + 1022; // 1023 (bias) - 1 (sign)
        long frac = 0;
        if (exp > 0x7ff) exp = 0x7ff; // inf, frac = 0;
        else {
            // Deep breath...
            // Shift bigint to get 52 significant bits, adjusting for sign bit
            // (-52 -1), but * 2, add 1, then / 2 to round correctly
            frac = ((bigint.shiftRight(len - 54).longValue() + 1l) >> 1);
            if (frac == 0x20000000000000l) { // corner case: rounding overflowed
                exp += 1l;
                if (exp > 0x7ff) exp = 0x7ff;
            }
        }
        return Double.longBitsToDouble(signum | (exp << 52) | frac & 0xfffffffffffffl);
    }


    /** rb_int2big
     *
     */
    public static BigInteger fix2big(RubyFixnum arg) {
        return long2big(arg.getLongValue());
    }

    public static BigInteger long2big(long arg) {
        return BigInteger.valueOf(arg);
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    /** rb_big_to_s
     *
     */
    public IRubyObject to_s(IRubyObject[] args) {
        switch (args.length) {
        case 0:
            return to_s();
        case 1:
            return to_s(args[0]);
        default:
            throw getRuntime().newArgumentError(args.length, 1);
        }
    }

    @JRubyMethod(name = "to_s", alias = "inspect")
    @Override
    public IRubyObject to_s() {
        int base = 10;
        return RubyString.newUSASCIIString(getRuntime(), getValue().toString(base));
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s(IRubyObject arg0) {
        int base = num2int(arg0);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        return RubyString.newUSASCIIString(getRuntime(), getValue().toString(base));
    }

    /** rb_big_coerce
     *
     */
    @JRubyMethod(name = "coerce", required = 1)
    @Override
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
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return addFixnum(((RubyFixnum)other).getLongValue());
        } else if (other instanceof RubyBignum) {
            return addBignum(((RubyBignum)other).value);
        } else if (other instanceof RubyFloat) {
            return addFloat((RubyFloat)other);
        }
        return addOther(context, other);
    }

    public IRubyObject op_plus(ThreadContext context, long other) {
        return addFixnum(other);
    }

    private IRubyObject addFixnum(long other) {
        BigInteger result = value.add(BigInteger.valueOf(other));
        if (other > 0 && value.signum() > 0) return new RubyBignum(getRuntime(), result);
        return bignorm(getRuntime(), result);
    }

    private IRubyObject addBignum(BigInteger other) {
        BigInteger result = value.add(other);
        if (value.signum() > 0 && other.signum() > 0) return new RubyBignum(getRuntime(), result);
        return bignorm(getRuntime(), result);
    }

    private IRubyObject addFloat(RubyFloat other) {
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) + other.getDoubleValue());
    }

    private IRubyObject addOther(ThreadContext context, IRubyObject other) {
        return coerceBin(context, "+", other);
    }

    /** rb_big_minus
     *
     */
    @JRubyMethod(name = "-", required = 1)
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return subtractFixnum(((RubyFixnum)other).getLongValue());
        } else if (other instanceof RubyBignum) {
            return subtractBignum(((RubyBignum)other).value);
        } else if (other instanceof RubyFloat) {
            return subtractFloat((RubyFloat)other);
        }
        return subtractOther(context, other);
    }

    public IRubyObject op_minus(ThreadContext context, long other) {
        return subtractFixnum(other);
    }

    private IRubyObject subtractFixnum(long other) {
        BigInteger result = value.subtract(BigInteger.valueOf(other));
        if (value.signum() < 0 && other > 0) return new RubyBignum(getRuntime(), result);
        return bignorm(getRuntime(), result);
    }

    private IRubyObject subtractBignum(BigInteger other) {
        BigInteger result = value.subtract(other);
        if (value.signum() < 0 && other.signum() > 0) return new RubyBignum(getRuntime(), result);
        return bignorm(getRuntime(), result);
    }

    private IRubyObject subtractFloat(RubyFloat other) {
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) - other.getDoubleValue());
    }

    private IRubyObject subtractOther(ThreadContext context, IRubyObject other) {
        return coerceBin(context, "-", other);
    }

    /** rb_big_mul
     *
     */
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return op_mul19(context, other);
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof RubyFixnum) {
            return bignorm(runtime, value.multiply(fix2big(((RubyFixnum) other))));
        } else if (other instanceof RubyBignum) {
            return bignorm(runtime, value.multiply(((RubyBignum)other).value));
        } else return opMulOther(context, other);
    }

    public IRubyObject op_mul(ThreadContext context, long other) {
        Ruby runtime = context.runtime;
        BigInteger result = value.multiply(long2big(other));
        return result.signum() == 0 ? RubyFixnum.zero(runtime) : new RubyBignum(runtime, result);
    }

    public IRubyObject opMulOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, "*", other);
    }

    /**
     * rb_big_divide. Shared part for both "/" and "div" operations.
     */
    private IRubyObject op_divide(ThreadContext context, IRubyObject other, boolean slash) {
        Ruby runtime = context.runtime;
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            double otherFloatValue = ((RubyFloat) other).getDoubleValue();
            if (!slash) {
                if (otherFloatValue == 0.0) throw runtime.newZeroDivisionError();
            }
            double div = big2dbl(this) / otherFloatValue;
            if (slash) {
                return RubyFloat.newFloat(runtime, div);
            } else {
                return RubyNumeric.dbl2num(runtime, div);
            }
        } else {
            return coerceBin(context, slash ? "/" : "div", other);
        }

        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        final BigInteger result;
        if (value.signum() * otherValue.signum() == -1) {
            BigInteger[] results = value.divideAndRemainder(otherValue);
            result = results[1].signum() != 0 ? results[0].subtract(BigInteger.ONE) : results[0];
        } else {
            result = value.divide(otherValue);
        }
        return bignorm(getRuntime(), result);
    }

    /** rb_big_div
     *
     */
    @JRubyMethod(name = {"/"}, required = 1)
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        return op_divide(context, other, true);
    }

    /** rb_big_idiv
     *
     */
    @JRubyMethod(name = {"div"}, required = 1)
    public IRubyObject op_idiv(ThreadContext context, IRubyObject other) {
        return op_divide(context, other, false);
    }

    /** rb_big_divmod
     *
     */
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin(context, "divmod", other);
        }

        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        BigInteger[] results = value.divideAndRemainder(otherValue);
        if ((value.signum() * otherValue.signum()) == -1 && results[1].signum() != 0) {
            results[0] = results[0].subtract(BigInteger.ONE);
            results[1] = otherValue.add(results[1]);
        }
        return RubyArray.newArray(runtime, bignorm(runtime, results[0]), bignorm(runtime, results[1]));
    }

    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyFloat
                && ((RubyFloat)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return divmod(context, other);
    }

    /** rb_big_modulo
     *
     */
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin(context, "%", other);
        }
        Ruby runtime = context.runtime;
        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        BigInteger result = value.mod(otherValue.abs());
        if (otherValue.signum() == -1 && result.signum() != 0) result = otherValue.add(result);
        return bignorm(runtime, result);
    }

    /** rb_big_modulo
     *
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyFloat
                && ((RubyFloat)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return op_mod(context, other);
    }

    /** rb_big_remainder
     *
     */
    @Override
    public IRubyObject remainder(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big(((RubyFixnum) other));
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin(context, "remainder", other);
        }
        Ruby runtime = context.runtime;
        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();
        return bignorm(runtime, value.remainder(otherValue));
    }

    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return remainder(context, other);
    }

    /** rb_big_quo

     *
     */
    @Override
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) / ((RubyNumeric) other).getDoubleValue());
        } else {
            return coerceBin(context, "quo", other);
        }
    }

    @JRubyMethod(name = "quo", required = 1)
    public IRubyObject quo19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyInteger && ((RubyInteger) other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return quo(context, other);
    }

    /** rb_big_pow
     *
     */
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        return op_pow19(context, other);
    }

    public IRubyObject op_pow(ThreadContext context, long other) {
        // MRI issuses warning here on (RBIGNUM(x)->len * SIZEOF_BDIGITS * yy > 1024*1024)
        if (((value.bitLength() + 7) / 8) * 4 * Math.abs(other) > 1024 * 1024) {
            getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
        }
        if (other >= 0) {
            return bignorm(getRuntime(), value.pow((int) other)); // num2int is also implemented
        } else {
            return RubyFloat.newFloat(getRuntime(), Math.pow(big2dbl(this), (double)other));
        }
    }

    /** rb_big_pow
     *
     */
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other == RubyFixnum.zero(runtime)) return RubyFixnum.one(runtime);
        double d;
        if (other instanceof RubyFixnum) {
            RubyFixnum fix = (RubyFixnum) other;
            long fixValue = fix.getLongValue();

            if (fixValue < 0) {
                return RubyRational.newRationalRaw(runtime, this).callMethod(context, "**", other);
            }
            // MRI issuses warning here on (RBIGNUM(x)->len * SIZEOF_BDIGITS * yy > 1024*1024)
            if (((value.bitLength() + 7) / 8) * 4 * Math.abs(fixValue) > 1024 * 1024) {
                getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
            }
            if (fixValue >= 0) {
                return bignorm(runtime, value.pow((int) fixValue)); // num2int is also implemented
            } else {
                return RubyFloat.newFloat(runtime, Math.pow(big2dbl(this), (double)fixValue));
            }
        } else if (other instanceof RubyBignum) {
            d = ((RubyBignum) other).getDoubleValue();
            getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
        } else if (other instanceof RubyFloat) {
            d = ((RubyFloat) other).getDoubleValue();
            if (this.compareTo(RubyFixnum.zero(runtime)) == -1
                    &&  d != Math.round(d)) {
                return RubyComplex.newComplexRaw(getRuntime(), this).callMethod(context, "**", other);
            }
        } else {
            return coerceBin(context, "**", other);

        }
        double pow = Math.pow(big2dbl(this), d);
        if (Double.isInfinite(pow)) {
            return RubyFloat.newFloat(runtime, pow);
        }
        return RubyNumeric.dbl2num(runtime, pow);
    }

    /** rb_big_and
     *
     */
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.and(((RubyBignum) other).value));
        } else if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.and(fix2big((RubyFixnum)other)));
        }
        return coerceBit(context, "&", other);
    }

    @JRubyMethod(name = "&", required = 1)
    public IRubyObject op_and19(ThreadContext context, IRubyObject other) {
        return op_and(context, other);
    }

    /** rb_big_or
     *
     */
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.or(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) { // no bignorm here needed
            return bignorm(getRuntime(), value.or(fix2big((RubyFixnum)other)));
        }
        return coerceBit(context, "|", other);
    }

    @JRubyMethod(name = "|", required = 1)
    public IRubyObject op_or19(ThreadContext context, IRubyObject other) {
        return op_or(context, other);
    }

    /** rb_big_xor
     *
     */
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.xor(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.xor(BigInteger.valueOf(((RubyFixnum) other).getLongValue())));
        }
        return coerceBit(context, "^", other);
    }
    
    @JRubyMethod(name = "^", required = 1)
    public IRubyObject op_xor19(ThreadContext context, IRubyObject other) {
        return op_xor(context, other);
    }

    private IRubyObject convertToInteger(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat) {
            throw context.runtime.newTypeError("can't convert Float into Integer");
        }
        return other.convertToInteger();
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
        long shift;
        boolean neg = false;

        for (;;) {
            if (other instanceof RubyFixnum) {
                shift = ((RubyFixnum)other).getLongValue();
                if (shift < 0) {
                    neg = true;
                    shift = -shift;
                }
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum)other;
                if (otherBignum.value.signum() < 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(this);
                    if (!tmp.isNil()) return tmp;
                    neg = true;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }

        return bignorm(getRuntime(), neg ? value.shiftRight((int)shift) : value.shiftLeft((int)shift));
    }

    /** rb_big_rshift
     *
     */
    @JRubyMethod(name = ">>", required = 1)
    public IRubyObject op_rshift(IRubyObject other) {
        long shift;
        boolean neg = false;

        for (;;) {
            if (other instanceof RubyFixnum) {
                shift = ((RubyFixnum)other).getLongValue();
                if (shift < 0) {
                    neg = true;
                    shift = -shift;
                }
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum)other;
                if (otherBignum.value.signum() >= 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(this);
                    if (!tmp.isNil()) return tmp;
                } else {
                    neg = true;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }
        return bignorm(getRuntime(), neg ? value.shiftLeft((int)shift) : value.shiftRight((int)shift));
    }

    /** rb_big_aref
     *
     */
    @JRubyMethod(name = "[]", required = 1)
    public RubyFixnum op_aref(IRubyObject other) {
        if (other instanceof RubyBignum) {
            // Need to normalize first
            other = bignorm(getRuntime(), ((RubyBignum) other).value);
            if (other instanceof RubyBignum) {
                // '!=' for negative value
                if ((((RubyBignum) other).value.signum() >= 0) != (value.signum() == -1)) {
                    return RubyFixnum.zero(getRuntime());
                }
                return RubyFixnum.one(getRuntime());
            }
        }
        long position = num2long(other);
        if (position < 0 || position > Integer.MAX_VALUE) {
            return RubyFixnum.zero(getRuntime());
        }

        return value.testBit((int)position) ? RubyFixnum.one(getRuntime()) : RubyFixnum.zero(getRuntime());
    }

    @Override
    public final int compareTo(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return value.compareTo(((RubyBignum)other).value);
        }
        return (int)coerceCmp(getRuntime().getCurrentContext(), "<=>", other).convertToInteger().getLongValue();
    }

    /** rb_big_cmp
     *
     */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            RubyFloat flt = (RubyFloat)other;
            if (flt.infinite_p().isTrue()) {
                if (flt.getDoubleValue() > 0.0) return RubyFixnum.minus_one(getRuntime());
                return RubyFixnum.one(getRuntime());
            }
            return dbl_cmp(getRuntime(), big2dbl(this), flt.getDoubleValue());
        } else {
            return coerceCmp(context, "<=>", other);
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
            return other.op_eqq(getRuntime().getCurrentContext(), this);
        }
        return RubyBoolean.newBoolean(getRuntime(), value.compareTo(otherValue) == 0);
    }

    /** rb_big_eql
     *
     */
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        return eql_p19(other);
    }

    /**
     * In ruby 1.9, '==' and '===' are the same, but they differ from 'eql?'.
     */
    @JRubyMethod(name = {"==="}, required = 1)
    public IRubyObject eql_p19(IRubyObject other) {
        return op_equal(other);
    }

    /** rb_big_hash
     *
     */
    @JRubyMethod(name = "hash")
    @Override
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
        output.registerLinkTarget(bignum);

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
