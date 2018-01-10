/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
import org.jruby.runtime.JavaSites;
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
        RubyClass bignum = runtime.getInteger();
        runtime.getObject().setConstant("Bignum", bignum);
        runtime.getObject().deprecateConstant(runtime, "Bignum");
        runtime.setBignum(bignum);

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
        setFrozen(true);
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

    /**
     * Return a Bignum for the given value, or raise FloatDomainError if it is out of range.
     *
     * Note this method may return Bignum that are in Fixnum range.
     */
    public static RubyBignum newBignum(Ruby runtime, double value) {
        try {
            return newBignum(runtime, new BigDecimal(value).toBigInteger());
        } catch (NumberFormatException nfe) {
            throw runtime.newFloatDomainError(Double.toString(value));
        }
    }

    /**
     * Return a Bignum or Fixnum (Integer) for the given value, or raise FloatDomainError if it is out of range.
     *
     * MRI: rb_dbl2big
     */
    public static RubyInteger newBignorm(Ruby runtime, double value) {
        try {
            return bignorm(runtime, new BigDecimal(value).toBigInteger());
        } catch (NumberFormatException nfe) {
            throw runtime.newFloatDomainError(Double.toString(value));
        }
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

    @Override
    public int signum() { return value.signum(); }

    @Override
    public RubyInteger negate() {
        return RubyBignum.newBignum(getRuntime(), value.negate());
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
        return (bi.compareTo(LONG_MIN) < 0 || bi.compareTo(LONG_MAX) > 0) ?
                    newBignum(runtime, bi) : runtime.newFixnum(bi.longValue());
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

    /** rb_big_ceil
     *
     */
    @Override
    public IRubyObject ceil(ThreadContext context, IRubyObject arg){
        int ndigits = arg.convertToInteger().getIntValue();
        BigInteger self = value;
        if (ndigits > 0){
            return convertToFloat();
        } else if (ndigits == 0){
            return this;
        } else {
            int posdigits = Math.abs(ndigits);
            BigInteger exp = BigInteger.TEN.pow(posdigits);
            BigInteger mod = self.mod(exp);
            BigInteger res = self;
            if (mod.compareTo(BigInteger.ZERO) != 0) {
                res = self.add( exp.subtract(mod) );// self + (exp - (mod));
            }
            return newBignum(context.runtime, res);
        }
    }

    /** rb_big_floor
     *
     */
    @Override
    public IRubyObject floor(ThreadContext context, IRubyObject arg){
        int ndigits = arg.convertToInteger().getIntValue();
        BigInteger self = value;
        if (ndigits > 0){
            return convertToFloat();
        } else if (ndigits == 0){
            return this;
        } else {
            int posdigits = Math.abs(ndigits);
            BigInteger exp = BigInteger.TEN.pow(posdigits);
            BigInteger res = self.subtract(self.mod(exp));
            return newBignum(context.runtime, res);
        }
    }

    /** rb_big_truncate
     *
     */
    @Override
    public IRubyObject truncate(ThreadContext context, IRubyObject arg){
        BigInteger self = value;
        if (self.compareTo(BigInteger.ZERO) == 1){
            return floor(context, arg);
        } else if (self.compareTo(BigInteger.ZERO) == -1){
            return ceil(context, arg);
        } else {
            return this;
        }
    }

    /** rb_big_digits
     *
     */
    @Override
    public RubyArray digits(ThreadContext context, IRubyObject base) {
        BigInteger self = value;
        Ruby runtime = context.runtime;
        if (self.compareTo(BigInteger.ZERO) == -1) {
            throw runtime.newMathDomainError("out of domain");
        }
        if (!(base instanceof RubyInteger)) {
            try {
                base = base.convertToInteger();
            } catch (ClassCastException e) {
                String cname = base.getMetaClass().getRealClass().getName();
                throw runtime.newTypeError("wrong argument type " + cname + " (expected Integer)");
            }
        }

        BigInteger bigBase;
        if (base instanceof RubyBignum) {
            bigBase = ((RubyBignum) base).getValue();
        } else {
            bigBase = long2big( ((RubyFixnum) base).getLongValue() );
        }

        if (bigBase.compareTo(BigInteger.ZERO) == -1) {
            throw runtime.newArgumentError("negative radix");
        }
        if (bigBase.compareTo(new BigInteger("2")) == -1) {
            throw runtime.newArgumentError("invalid radix: " + bigBase);
        }

        RubyArray res = RubyArray.newArray(context.runtime, 0);

        if (self.compareTo(BigInteger.ZERO) == 0) {
            res.append(RubyFixnum.newFixnum(context.getRuntime(), 0));
            return res;
        }

        while (self.compareTo(BigInteger.ZERO) > 0) {
            BigInteger q = self.mod(bigBase);
            res.append(RubyBignum.newBignum(context.getRuntime(), q));
            self = self.divide(bigBase);
        }

        return res;
    }

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

    @Override
    public RubyString to_s() {
        return RubyString.newUSASCIIString(getRuntime(), getValue().toString(10));
    }

    @Override
    public RubyString to_s(IRubyObject arg0) {
        int base = num2int(arg0);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        return RubyString.newUSASCIIString(getRuntime(), getValue().toString(base));
    }

    /** rb_big_coerce
     *
     */
    @Override
    public IRubyObject coerce(IRubyObject other) {
        final Ruby runtime = getRuntime();
        if (other instanceof RubyFixnum) {
            return runtime.newArray(newBignum(runtime, ((RubyFixnum) other).getLongValue()), this);
        }
        if (other instanceof RubyBignum) {
            return runtime.newArray(newBignum(runtime, ((RubyBignum) other).getValue()), this);
        }

        return RubyArray.newArray(runtime, RubyKernel.new_float(runtime, other), RubyKernel.new_float(runtime, this));
    }

    /** rb_big_uminus
     *
     */
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        return bignorm(getRuntime(), value.negate());
    }

    /** rb_big_plus
     *
     */
    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_plus(context, ((RubyFixnum) other).getLongValue());
        }
        if (other instanceof RubyBignum) {
            return op_plus(context, ((RubyBignum) other).value);
        }
        if (other instanceof RubyFloat) {
            return addFloat((RubyFloat) other);
        }
        return addOther(context, other);
    }

    public final IRubyObject op_plus(ThreadContext context, long other) {
        BigInteger result = value.add(BigInteger.valueOf(other));
        if (other > 0 && value.signum() > 0) return new RubyBignum(context.runtime, result);
        return bignorm(context.runtime, result);
    }

    public final IRubyObject op_plus(ThreadContext context, BigInteger other) {
        BigInteger result = value.add(other);
        if (value.signum() > 0 && other.signum() > 0) return new RubyBignum(context.runtime, result);
        return bignorm(context.runtime, result);
    }

    private IRubyObject addFloat(RubyFloat other) {
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) + other.getDoubleValue());
    }

    private IRubyObject addOther(ThreadContext context, IRubyObject other) {
        return coerceBin(context, sites(context).op_plus, other);
    }

    /** rb_big_minus
     *
     */
    @Override
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_minus(context, ((RubyFixnum)other).getLongValue());
        }
        if (other instanceof RubyBignum) {
            return op_minus(context, ((RubyBignum)other).value);
        }
        if (other instanceof RubyFloat) {
            return subtractFloat((RubyFloat)other);
        }
        return subtractOther(context, other);
    }

    public final IRubyObject op_minus(ThreadContext context, long other) {
        BigInteger result = value.subtract(BigInteger.valueOf(other));
        if (value.signum() < 0 && other > 0) return new RubyBignum(context.runtime, result);
        return bignorm(context.runtime, result);
    }

    public final IRubyObject op_minus(ThreadContext context, BigInteger other) {
        BigInteger result = value.subtract(other);
        if (value.signum() < 0 && other.signum() > 0) return new RubyBignum(context.runtime, result);
        return bignorm(context.runtime, result);
    }

    private IRubyObject subtractFloat(RubyFloat other) {
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) - other.getDoubleValue());
    }

    private IRubyObject subtractOther(ThreadContext context, IRubyObject other) {
        return coerceBin(context, sites(context).op_minus, other);
    }

    /** rb_big_mul
     *
     */
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_mul(context, ((RubyFixnum) other).getLongValue());
        }
        if (other instanceof RubyBignum) {
            return bignorm(context.runtime, value.multiply(((RubyBignum) other).value));
        }
        if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(context.runtime, big2dbl(this) * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, sites(context).op_times, other);
    }

    @Deprecated
    public final IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        return op_mul(context, other);
    }

    public final IRubyObject op_mul(ThreadContext context, long other) {
        return bignorm(context.runtime, value.multiply(long2big(other)));
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
                return RubyNumeric.dbl2ival(runtime, div);
            }
        } else {
            return coerceBin(context, slash ? sites(context).op_quo : sites(context).div, other);
        }

        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        final BigInteger result;
        if (value.signum() * otherValue.signum() == -1) {
            BigInteger[] results = value.divideAndRemainder(otherValue);
            result = results[1].signum() != 0 ? results[0].subtract(BigInteger.ONE) : results[0];
        } else {
            result = value.divide(otherValue);
        }
        return bignorm(runtime, result);
    }

    /** rb_big_div
     *
     */
    @Override
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        return op_divide(context, other, true);
    }

    /** rb_big_idiv
     *
     */
    @Override
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        return op_divide(context, other, false);
    }

    /** rb_big_divmod
     *
     */
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyFloat
                && ((RubyFloat)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }

        Ruby runtime = context.runtime;

        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            if (other instanceof RubyFloat && ((RubyFloat)other).getDoubleValue() == 0) {
                throw runtime.newZeroDivisionError();
            }
            return coerceBin(context, sites(context).divmod, other);
        }

        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        BigInteger[] results = value.divideAndRemainder(otherValue);
        if ((value.signum() * otherValue.signum()) == -1 && results[1].signum() != 0) {
            results[0] = results[0].subtract(BigInteger.ONE);
            results[1] = otherValue.add(results[1]);
        }
        return RubyArray.newArray(runtime, bignorm(runtime, results[0]), bignorm(runtime, results[1]));
    }

    /** rb_big_modulo
     *
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyFloat
                && ((RubyFloat)other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }

        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0) {
                throw context.runtime.newZeroDivisionError();
            }
            return coerceBin(context, sites(context).op_mod, other);
        }

        if (otherValue.signum() == 0) throw context.runtime.newZeroDivisionError();

        BigInteger result = value.mod(otherValue.abs());
        if (otherValue.signum() == -1 && result.signum() != 0) result = otherValue.add(result);
        return bignorm(context.runtime, result);
    }

    @Deprecated
    public IRubyObject op_mod19(ThreadContext context, IRubyObject other) {
        return op_mod(context, other);
    }

    /** rb_big_remainder
     *
     */
    @Override
    @JRubyMethod(name = "remainder", required = 1)
    public IRubyObject remainder(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }

        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big(((RubyFixnum) other));
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0) {
                throw context.runtime.newZeroDivisionError();
            }
            return coerceBin(context, sites(context).remainder, other);
        }
        if (otherValue.signum() == 0) throw context.runtime.newZeroDivisionError();
        return bignorm(context.runtime, value.remainder(otherValue));
    }

    @Deprecated
    public IRubyObject remainder19(ThreadContext context, IRubyObject other) {
        return remainder(context, other);
    }

    /** rb_big_quo
     *
     */
    @Override
    @JRubyMethod(name = "quo", required = 1)
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyInteger && ((RubyInteger) other).getDoubleValue() == 0) {
            throw context.runtime.newZeroDivisionError();
        }

        if (other instanceof RubyNumeric) {
            if (((RubyNumeric) other).getDoubleValue() == 0) {
                throw context.runtime.newZeroDivisionError();
            }
            return RubyFloat.newFloat(context.runtime, big2dbl(this) / ((RubyNumeric) other).getDoubleValue());
        } else {
            return coerceBin(context, sites(context).quo, other);
        }
    }

    @Deprecated
    public final IRubyObject quo19(ThreadContext context, IRubyObject other) {
        return quo(context, other);
    }

    /** rb_big_pow
     *
     */
    @Override
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other == RubyFixnum.zero(runtime)) return RubyFixnum.one(runtime);
        final double d;
        if (other instanceof RubyFloat) {
            d = ((RubyFloat) other).getDoubleValue();
            if (compareTo(RubyFixnum.zero(runtime)) == -1 && d != Math.round(d)) {
                RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                return sites(context).op_exp.call(context, complex, complex, other);
            }
        } else if (other instanceof RubyBignum) {
            d = ((RubyBignum) other).getDoubleValue();
            context.runtime.getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
        } else if (other instanceof RubyFixnum) {
            long yy = other.convertToInteger().getLongValue();
            if (yy < 0)
                return RubyRational.newRationalRaw(runtime, this).op_expt(context, other);
            else {
                int xbits = value.bitLength();
                int BIGLEN_LIMIT = 32*1024*1024;

                if ((xbits > BIGLEN_LIMIT) ||
                        (xbits * yy > BIGLEN_LIMIT)) {
                    runtime.getWarnings().warn("in a**b, b may be too big");
                    d = (double)yy;
                }
                else {
                    return newBignum(runtime, value.pow((int)yy));
                }
            }
        } else {
            return coerceBin(context, sites(context).op_exp, other);
        }
        double pow = Math.pow(big2dbl(this), d);
        if (Double.isInfinite(pow)) {
            return RubyFloat.newFloat(runtime, pow);
        }
        return RubyNumeric.dbl2ival(runtime, pow);
    }

    private void warnIfPowExponentTooBig(final ThreadContext context, final long other) {
        // MRI issuses warning here on (RBIGNUM(x)->len * SIZEOF_BDIGITS * yy > 1024*1024)
        if ( ((value.bitLength() + 7) / 8) * 4 * Math.abs((double) other) > 1024 * 1024 ) {
            context.runtime.getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
        }
    }

    @Deprecated
    public IRubyObject op_pow19(ThreadContext context, IRubyObject other) {
        return op_pow(context, other);
    }

    /** rb_big_and
     *
     */
    @Override
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(context.runtime, value.and(((RubyBignum) other).value));
        } else if (other instanceof RubyFixnum) {
            return bignorm(context.runtime, value.and(fix2big((RubyFixnum)other)));
        }
        return coerceBit(context, sites(context).checked_op_and, other);
    }

    @Deprecated
    public IRubyObject op_and19(ThreadContext context, IRubyObject other) {
        return op_and(context, other);
    }

    /** rb_big_or
     *
     */
    @Override
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(context.runtime, value.or(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) { // no bignorm here needed
            return bignorm(context.runtime, value.or(fix2big((RubyFixnum)other)));
        }
        return coerceBit(context, sites(context).checked_op_or, other);
    }

    @Override
    public IRubyObject bit_length(ThreadContext context) {
        return context.runtime.newFixnum(value.bitLength());
    }

    @Deprecated
    public IRubyObject op_or19(ThreadContext context, IRubyObject other) {
        return op_or(context, other);
    }

    /** rb_big_xor
     *
     */
    @Override
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return bignorm(context.runtime, value.xor(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) {
            return bignorm(context.runtime, value.xor(BigInteger.valueOf(((RubyFixnum) other).getLongValue())));
        }
        return coerceBit(context, sites(context).checked_op_xor, other);
    }

    @Deprecated
    public IRubyObject op_xor19(ThreadContext context, IRubyObject other) {
        return op_xor(context, other);
    }

    /** rb_big_neg
     *
     */
    @Override
    public IRubyObject op_neg(ThreadContext context) {
        return RubyBignum.newBignum(context.runtime, value.not());
    }

    /** rb_big_lshift
     *
     */
    @Override
    public IRubyObject op_lshift(ThreadContext context, IRubyObject other) {
        long shift;

        for (;;) {
            if (other instanceof RubyFixnum) {
                shift = ((RubyFixnum)other).getLongValue();
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum)other;
                if (otherBignum.value.signum() < 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(this);
                    if (!tmp.isNil()) return tmp;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }

        return bignorm(context.runtime, value.shiftLeft((int)shift));
    }

    /** rb_big_rshift
     *
     */
    @Override
    public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
        long shift;

        for (;;) {
            if (other instanceof RubyFixnum) {
                shift = ((RubyFixnum)other).getLongValue();
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum)other;
                if (otherBignum.value.signum() >= 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(this);
                    if (!tmp.isNil()) return tmp;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }

        return bignorm(context.runtime, value.shiftRight((int)shift));
    }

    /** rb_big_aref
     *
     */
    @Override
    public RubyFixnum op_aref(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            // Need to normalize first
            other = bignorm(context.runtime, ((RubyBignum) other).value);
            if (other instanceof RubyBignum) {
                // '!=' for negative value
                if ((((RubyBignum) other).value.signum() >= 0) != (value.signum() == -1)) {
                    return RubyFixnum.zero(context.runtime);
                }
                return RubyFixnum.one(context.runtime);
            }
        }
        long position = num2long(other);
        if (position < 0 || position > Integer.MAX_VALUE) {
            return RubyFixnum.zero(context.runtime);
        }

        return value.testBit((int)position) ? RubyFixnum.one(context.runtime) : RubyFixnum.zero(context.runtime);
    }

    @Override
    public final int compareTo(IRubyObject other) {
        if (other instanceof RubyBignum) {
            return value.compareTo(((RubyBignum)other).value);
        }
        ThreadContext context = getRuntime().getCurrentContext();
        return (int)coerceCmp(context, sites(context).op_cmp, other).convertToInteger().getLongValue();
    }

    /** rb_big_cmp
     *
     */
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            RubyFloat flt = (RubyFloat)other;
            if (flt.infinite_p().isTrue()) {
                if (flt.getDoubleValue() > 0.0) return RubyFixnum.minus_one(context.runtime);
                return RubyFixnum.one(context.runtime);
            }
            return dbl_cmp(context.runtime, big2dbl(this), flt.getDoubleValue());
        } else {
            return coerceCmp(context, sites(context).op_cmp, other);
        }

        // wow, the only time we can use the java protocol ;)
        return RubyFixnum.newFixnum(context.runtime, value.compareTo(otherValue));
    }

    /** rb_big_eq
     *
     */
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            double a = ((RubyFloat) other).getDoubleValue();
            if (Double.isNaN(a)) {
                return context.runtime.getFalse();
            }
            return RubyBoolean.newBoolean(context.runtime, a == big2dbl(this));
        } else {
            return other.op_eqq(context, this);
        }
        return RubyBoolean.newBoolean(context.runtime, value.compareTo(otherValue) == 0);
    }

    /** rb_big_eql
     *
     */
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        // '==' and '===' are the same, but they differ from 'eql?'.
        return op_equal(getRuntime().getCurrentContext(), other);
    }

    @Deprecated
    public IRubyObject eql_p19(IRubyObject other) {
        return eql_p(other);
    }

    /** rb_big_hash
     *
     */
    @Override
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
    }

    /** rb_big_to_f
     *
     */
    @Override
    public IRubyObject to_f(ThreadContext context) {
        return RubyFloat.newFloat(context.runtime, getDoubleValue());
    }

    public IRubyObject abs() {
        return abs(getRuntime().getCurrentContext());
    }

    /** rb_big_abs
     *
     */
    @Override
    public IRubyObject abs(ThreadContext context) {
        return RubyBignum.newBignum(context.runtime, value.abs());
    }

    /** rb_big_size
     *
     */
    @Override
    public IRubyObject size(ThreadContext context) {
        return context.runtime.newFixnum((value.bitLength() + 7) / 8);
    }

    @Override
    public IRubyObject zero_p(ThreadContext context) {
        return context.runtime.newBoolean(value.equals(BigInteger.ZERO));
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

    // MRI: rb_big_fdiv_double
    @Override
    public IRubyObject fdivDouble(ThreadContext context, IRubyObject y) {
        double dx, dy;

        dx = getDoubleValue();
        if (y instanceof RubyFixnum) {
            dy = (double)fix2long(y);
            if (Double.isInfinite(dx))
                return fdivInt(context, RubyBignum.newBignum(context.runtime, fix2long(y)));
        } else if (y instanceof RubyBignum) {
            dy = RubyBignum.big2dbl((RubyBignum) y);
            if (Double.isInfinite(dx) || Double.isInfinite(dy))
                return fdivInt(context, (RubyBignum) y);
        } else if (y instanceof RubyFloat) {
            dy = ((RubyFloat) y).getDoubleValue();
            if (Double.isNaN(dy)) {
                return context.runtime.newFloat(dy);
            }
            if (Double.isInfinite(dx)) {
                return fdivFloat(context, (RubyFloat) y);
            }
        } else {
            return coerceBin(context, sites(context).fdiv, y);
        }
        return context.runtime.newFloat(dx / dy);
    }

    // MRI: big_fdiv_int and big_fdiv
    public IRubyObject fdivInt(ThreadContext context, RubyBignum y) {
        return context.runtime.newFloat(new BigDecimal(value).divide(new BigDecimal(y.getValue())).doubleValue());
    }

    // MRI: big_fdiv_float
    public IRubyObject fdivFloat(ThreadContext context, RubyFloat y) {
        return context.runtime.newFloat(new BigDecimal(value).divide(new BigDecimal(y.getValue())).doubleValue());
    }

    @Override
    public IRubyObject isNegative(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (sites(context).basic_op_lt.retrieveCache(metaClass).method.isBuiltin()) {
            return runtime.newBoolean(value.signum() < 0);
        }
        return sites(context).basic_op_lt.call(context, this, this, RubyFixnum.zero(runtime));
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (sites(context).basic_op_gt.retrieveCache(metaClass).method.isBuiltin()) {
            return runtime.newBoolean(value.signum() > 0);
        }
        return sites(context).basic_op_gt.call(context, this, this, RubyFixnum.zero(runtime));
    }

    @Override
    protected boolean int_round_zero_p(ThreadContext context, int ndigits) {
        long bytes = value.bitLength() / 8 + 1;
        return (-0.415241 * ndigits - 0.125 > bytes);
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    @Deprecated
    public final IRubyObject op_pow(final ThreadContext context, final long other) {
        if (other >= 0) {
            if (other <= Integer.MAX_VALUE) { // only have BigInteger#pow(int)
                return bignorm(context.runtime, value.pow((int) other)); // num2int is also implemented
            }
            warnIfPowExponentTooBig(context, other);
            return RubyFloat.newFloat(context.runtime, Math.pow(big2dbl(this), (double) other));
        }
        // (other < 0)
        return RubyRational.newRationalRaw(context.runtime, this).op_expt(context, other);
    }

    private static JavaSites.BignumSites sites(ThreadContext context) {
        return context.sites.Bignum;
    }
}
