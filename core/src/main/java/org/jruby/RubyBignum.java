/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
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

        return bignum;
    }

    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L << (BIT_SIZE - 1)) - 1;
    public static final BigInteger LONG_MAX = BigInteger.valueOf(MAX);
    public static final BigInteger LONG_MIN = BigInteger.valueOf(-MAX - 1);
    public static final BigInteger ULONG_MAX = BigInteger.valueOf(1).shiftLeft(BIT_SIZE).subtract(BigInteger.valueOf(1));

    final BigInteger value;

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
            return newBignum(runtime, toBigInteger(value));
        } catch (NumberFormatException nfe) {
            throw runtime.newFloatDomainError(Double.toString(value));
        }
    }

    public static BigInteger toBigInteger(double value) {
        return new BigDecimal(value).toBigInteger();
    }

    /**
     * Return a Bignum or Fixnum (Integer) for the given value, or raise FloatDomainError if it is out of range.
     *
     * MRI: rb_dbl2big
     */
    public static RubyInteger newBignorm(Ruby runtime, double value) {
        try {
            return bignorm(runtime, toBigInteger(value));
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
        return bignorm(getRuntime(), value.negate());
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
    public static long big2long(RubyBignum val) {
        BigInteger big = val.value;

        if (big.compareTo(LONG_MIN) < 0 || big.compareTo(LONG_MAX) > 0) {
            throw val.getRuntime().newRangeError("bignum too big to convert into `long'");
        }
        return big.longValue();
    }

    /** rb_big2ulong
     * This is here because for C extensions ulong can hold different values without throwing a RangeError
     */
    public static long big2ulong(RubyBignum value) {
        Ruby runtime = value.getRuntime();

        BigInteger big = value.getValue();

        return big2ulong(runtime, big);
    }

    public static long big2ulong(Ruby runtime, BigInteger big) {
        if (big.compareTo(BigInteger.ZERO) < 0 || big.compareTo(ULONG_MAX) > 0) {
            throw runtime.newRangeError("bignum out of range for `ulong'");
        }

        return big.longValue();
    }

    /** rb_big2dbl
     *
     */
    public static double big2dbl(RubyBignum val) {
        BigInteger big = val.value;
        double dbl = convertToDouble(big);
        if (dbl == Double.NEGATIVE_INFINITY || dbl == Double.POSITIVE_INFINITY) {
            val.getRuntime().getWarnings().warn(ID.BIGNUM_FROM_FLOAT_RANGE, "Bignum out of Float range");
    }
        return dbl;
    }

    private RubyFixnum checkShiftDown(ThreadContext context, RubyBignum other) {
        if (other.value.signum() == 0) return RubyFixnum.zero(context.runtime);
        if (value.compareTo(LONG_MIN) < 0 || value.compareTo(LONG_MAX) > 0) {
            return other.value.signum() >= 0 ? RubyFixnum.zero(context.runtime) : RubyFixnum.minus_one(context.runtime);
        }
        return null;
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
        return long2big(arg.value);
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
        if (ndigits >= 0){
            return this;
        } else {
            int posdigits = Math.abs(ndigits);
            BigInteger exp = BigInteger.TEN.pow(posdigits);
            BigInteger mod = self.mod(exp);
            BigInteger res = self;
            if (mod.signum() != 0) {
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
        if (ndigits >= 0){
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
                String cname = getMetaClass(base).getRealClass().getName();
                throw runtime.newTypeError("wrong argument type " + cname + " (expected Integer)");
            }
        }

        BigInteger bigBase;
        if (base instanceof RubyBignum) {
            bigBase = ((RubyBignum) base).value;
        } else {
            bigBase = long2big( ((RubyFixnum) base).value );
        }

        if (bigBase.signum() == -1) {
            throw runtime.newArgumentError("negative radix");
        }
        if (bigBase.compareTo(BigInteger.valueOf(2)) == -1) {
            throw runtime.newArgumentError("invalid radix: " + bigBase);
        }

        RubyArray res = RubyArray.newArray(context.runtime, 0);

        if (self.signum() == 0) {
            res.append(RubyFixnum.newFixnum(context.getRuntime(), 0));
            return res;
        }

        while (self.signum() > 0) {
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
        return RubyString.newUSASCIIString(getRuntime(), value.toString(10));
    }

    @Override
    public RubyString to_s(IRubyObject arg0) {
        int base = num2int(arg0);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        return RubyString.newUSASCIIString(getRuntime(), value.toString(base));
    }

    /** rb_big_coerce
     *
     */
    @Override
    public IRubyObject coerce(IRubyObject other) {
        final Ruby runtime = getRuntime();
        if (other instanceof RubyFixnum) {
            return runtime.newArray(newBignum(runtime, ((RubyFixnum) other).value), this);
        }
        if (other instanceof RubyBignum) {
            return runtime.newArray(newBignum(runtime, ((RubyBignum) other).value), this);
        }

        return RubyArray.newArray(runtime, RubyKernel.new_float(runtime, other), RubyKernel.new_float(runtime, this));
    }

    /** rb_big_uminus
     *
     */
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        return bignorm(context.runtime, value.negate());
    }

    /** rb_big_plus
     *
     */
    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_plus(context, ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return op_plus(context, ((RubyBignum) other).value);
        }
        if (other instanceof RubyFloat) {
            return addFloat((RubyFloat) other);
        }
        return addOther(context, other);
    }

    @Override
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
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) + other.value);
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
            return op_minus(context, ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return op_minus(context, ((RubyBignum) other).value);
        }
        if (other instanceof RubyFloat) {
            return subtractFloat((RubyFloat) other);
        }
        return subtractOther(context, other);
    }

    @Override
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
        return RubyFloat.newFloat(getRuntime(), big2dbl(this) - other.value);
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
            return op_mul(context, ((RubyFixnum) other).value);
        } else if (other instanceof RubyBignum) {
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(context.runtime, big2dbl(this) * ((RubyFloat) other).value);
        } else {
            return coerceBin(context, sites(context).op_times, other);
        }
        return bignorm(context.runtime, value.multiply(((RubyBignum) other).value));
    }

    @Deprecated
    public final IRubyObject op_mul19(ThreadContext context, IRubyObject other) {
        return op_mul(context, other);
    }

    @Override
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
            double otherFloatValue = ((RubyFloat) other).value;
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

        return divideImpl(runtime, otherValue);
    }

    private RubyInteger divideImpl(Ruby runtime, BigInteger otherValue) {
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

    public IRubyObject op_div(ThreadContext context, long other) {
        return divideImpl(context.runtime, long2big(other));
    }

    /** rb_big_idiv
     *
     */
    @Override
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        return op_divide(context, other, false);
    }

    @Override
    public IRubyObject idiv(ThreadContext context, long other) {
        return divideImpl(context.runtime, long2big(other));
    }

    /** rb_big_divmod
     *
     */
    @Override
    @JRubyMethod(name = "divmod", required = 1)
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            if (other instanceof RubyFloat && ((RubyFloat) other).value == 0) {
                throw context.runtime.newZeroDivisionError();
            }
            return coerceBin(context, sites(context).divmod, other);
        }

        if (otherValue.signum() == 0) throw context.runtime.newZeroDivisionError();

        BigInteger[] results = value.divideAndRemainder(otherValue);
        if ((value.signum() * otherValue.signum()) == -1 && results[1].signum() != 0) {
            results[0] = results[0].subtract(BigInteger.ONE);
            results[1] = otherValue.add(results[1]);
        }

        Ruby runtime = context.runtime;
        return RubyArray.newArray(runtime, bignorm(runtime, results[0]), bignorm(runtime, results[1]));
    }

    /** rb_big_modulo
     *
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_mod(context, ((RubyFixnum) other).value);
        }

        final BigInteger otherValue;
        if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
            if (otherValue.signum() == 0) throw context.runtime.newZeroDivisionError();

            BigInteger result = value.mod(otherValue.abs());
            if (otherValue.signum() == -1 && result.signum() != 0) result = otherValue.add(result);
            return bignorm(context.runtime, result);
        }

        if (other instanceof RubyFloat && ((RubyFloat) other).value == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        return coerceBin(context, sites(context).op_mod, other);
    }

    @Override
    public IRubyObject op_mod(ThreadContext context, long other) {
        if (other == 0) throw context.runtime.newZeroDivisionError();

        BigInteger result = value.mod(long2big(other < 0 ? -other : other));
        if (other < 0 && result.signum() != 0) result = long2big(other).add(result);
        return bignorm(context.runtime, result);
    }

    @Override
    public IRubyObject modulo(ThreadContext context, IRubyObject other) {
        return op_mod(context, other);
    }

    @Override
    IRubyObject modulo(ThreadContext context, long other) {
        return op_mod(context, other);
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
        if (other instanceof RubyFloat && ((RubyFloat) other).value == 0) {
            throw context.runtime.newZeroDivisionError();
        }

        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big(((RubyFixnum) other));
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            if (other instanceof RubyFloat && ((RubyFloat) other).value == 0) {
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
            d = ((RubyFloat) other).value;
            if (compareTo(RubyFixnum.zero(runtime)) == -1 && d != Math.round(d)) {
                RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                return sites(context).op_exp.call(context, complex, complex, other);
            }
        } else if (other instanceof RubyBignum) {
            d = ((RubyBignum) other).getDoubleValue();
            context.runtime.getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big");
        } else if (other instanceof RubyFixnum) {
            return op_pow(context, ((RubyFixnum) other).value);
        } else {
            return coerceBin(context, sites(context).op_exp, other);
        }
        return pow(runtime, d);
    }

    private RubyNumeric pow(final Ruby runtime, final double d) {
        double pow = Math.pow(big2dbl(this), d);
        if (Double.isInfinite(pow)) {
            return RubyFloat.newFloat(runtime, pow);
        }
        return RubyNumeric.dbl2ival(runtime, pow);
    }

    private static final int BIGLEN_LIMIT = 32 * 1024 * 1024;

    public final IRubyObject op_pow(final ThreadContext context, final long other) {
        if (other < 0) {
            return RubyRational.newRationalRaw(context.runtime, this).op_expt(context, other);
        }
        final int xbits = value.bitLength();
        if ((xbits > BIGLEN_LIMIT) || (xbits * other > BIGLEN_LIMIT)) {
            context.runtime.getWarnings().warn("in a**b, b may be too big");
            return pow(context.runtime, (double) other);
        }
        else {
            return newBignum(context.runtime, value.pow((int) other));
        }
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
        }
        if (other instanceof RubyFixnum) {
            return op_and(context, (RubyFixnum) other);
        }
        return coerceBit(context, sites(context).checked_op_and, other);
    }

    final RubyInteger op_and(ThreadContext context, RubyFixnum other) {
        return bignorm(context.runtime, value.and(fix2big(other)));
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
            return op_or(context, (RubyFixnum) other);
        }
        return coerceBit(context, sites(context).checked_op_or, other);
    }

    final RubyInteger op_or(ThreadContext context, RubyFixnum other) {
        return bignorm(context.runtime, value.or(fix2big(other))); // no bignorm here needed
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
            return bignorm(context.runtime, value.xor(BigInteger.valueOf(((RubyFixnum) other).value)));
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
                shift = ((RubyFixnum) other).value;
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum) other;
                if (otherBignum.value.signum() < 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(context, this);
                    if (tmp != null) return tmp;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }

        return op_lshift(context, shift);
    }

    @Override
    public RubyInteger op_lshift(ThreadContext context, long shift) {
        return bignorm(context.runtime, value.shiftLeft((int) shift));
    }

    /** rb_big_rshift
     *
     */
    @Override
    public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
        long shift;

        for (;;) {
            if (other instanceof RubyFixnum) {
                shift = ((RubyFixnum) other).value;
                break;
            } else if (other instanceof RubyBignum) {
                RubyBignum otherBignum = (RubyBignum) other;
                if (otherBignum.value.signum() >= 0) {
                    IRubyObject tmp = otherBignum.checkShiftDown(context, this);
                    if (tmp != null) return tmp;
                }
                shift = big2long(otherBignum);
                break;
            }
            other = other.convertToInteger();
        }

        return op_rshift(context, shift);
    }

    @Override
    public RubyInteger op_rshift(ThreadContext context, long shift) {
        return bignorm(context.runtime, value.shiftRight((int) shift));
    }

    @Override
    public RubyBoolean odd_p(ThreadContext context) {
        return value.testBit(0) ? context.tru : context.fals;
    }

    @Override
    public RubyBoolean even_p(ThreadContext context) {
        return value.testBit(0) ? context.fals : context.tru;
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
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return (int)coerceCmp(context, sites(context).op_cmp, other).convertToInteger().getLongValue();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof RubyBignum) {
            return value.compareTo(((RubyBignum) other).value) == 0;
        }
        return false;
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
            RubyFloat flt = (RubyFloat) other;
            if (flt.isInfinite()) {
                if (flt.value > 0.0) return RubyFixnum.minus_one(context.runtime);
                return RubyFixnum.one(context.runtime);
            }
            return dbl_cmp(context.runtime, big2dbl(this), flt.value);
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
            double a = ((RubyFloat) other).value;
            if (Double.isNaN(a)) return context.fals;
            return RubyBoolean.newBoolean(context, a == big2dbl(this));
        } else {
            return other.op_eqq(context, this);
        }
        return RubyBoolean.newBoolean(context, value.compareTo(otherValue) == 0);
    }

    /** rb_big_eql
     *
     */
    @Override
    public IRubyObject eql_p(IRubyObject other) {
        // '==' and '===' are the same, but they differ from 'eql?'.
        return op_equal(metaClass.runtime.getCurrentContext(), other);
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
        Ruby runtime = metaClass.runtime;
        return RubyFixnum.newFixnum(runtime, bigHash(runtime, value));
    }

    @Override
    public int hashCode() {
        return (int) bigHash(getRuntime(), value);
    }

    private static long bigHash(Ruby runtime, BigInteger value) {
        return Helpers.multAndMix(runtime.getHashSeedK0(), value.hashCode());
    }

    /** rb_big_to_f
     *
     */
    @Override
    public IRubyObject to_f(ThreadContext context) {
        return RubyFloat.newFloat(context.runtime, getDoubleValue());
    }

    @Override
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    @Deprecated
    public IRubyObject abs() {
        return abs(metaClass.runtime.getCurrentContext());
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
        return RubyBoolean.newBoolean(context, isZero());
    }

    @Override
    public final boolean isZero() {
        return value.equals(BigInteger.ZERO);
    }

    @Override
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero() ? context.nil : this;
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
            long ly = ((RubyFixnum) y).value;
            if (Double.isInfinite(dx)) {
                return fdivInt(context.runtime, BigDecimal.valueOf(ly));
            }
            dy = (double) ly;
        } else if (y instanceof RubyBignum) {
            return fdivDouble(context, (RubyBignum) y);
        } else if (y instanceof RubyFloat) {
            dy = ((RubyFloat) y).value;
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

    final RubyFloat fdivDouble(ThreadContext context, RubyBignum y) {
        double dx = getDoubleValue();
        double dy = RubyBignum.big2dbl(y);
        if (Double.isInfinite(dx) || Double.isInfinite(dy)) {
            return (RubyFloat) fdivInt(context, y);
        }

        return context.runtime.newFloat(dx / dy);
    }

    // MRI: big_fdiv_int and big_fdiv
    public IRubyObject fdivInt(ThreadContext context, RubyBignum y) {
        return fdivInt(context.runtime, new BigDecimal(y.value));
    }

    private RubyFloat fdivInt(final Ruby runtime, BigDecimal y) {
        return runtime.newFloat(new BigDecimal(value).divide(y).doubleValue());
    }

    // MRI: big_fdiv_float
    public IRubyObject fdivFloat(ThreadContext context, RubyFloat y) {
        return context.runtime.newFloat(new BigDecimal(value).divide(new BigDecimal(y.value)).doubleValue());
    }

    @Override
    public IRubyObject isNegative(ThreadContext context) {
        CachingCallSite op_lt_site = sites(context).basic_op_lt;
        if (op_lt_site.isBuiltin(metaClass)) {
            return RubyBoolean.newBoolean(context, value.signum() < 0);
        }
        return op_lt_site.call(context, this, this, RubyFixnum.zero(context.runtime));
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        CachingCallSite op_gt_site = sites(context).basic_op_gt;
        if (op_gt_site.isBuiltin(metaClass)) {
            return RubyBoolean.newBoolean(context, value.signum() > 0);
        }
        return op_gt_site.call(context, this, this, RubyFixnum.zero(context.runtime));
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

    @Override
    public IRubyObject numerator(ThreadContext context) {
        return this;
    }

    @Override
    public IRubyObject denominator(ThreadContext context) {
        return RubyFixnum.one(context.runtime);
    }

    public RubyRational convertToRational() {
        final Ruby runtime = getRuntime();
        return RubyRational.newRationalRaw(runtime, this, RubyFixnum.one(runtime));
    }

    // MRI: rb_int_s_isqrt, Fixnum portion
    @Override
    public IRubyObject sqrt(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (isNegative()) {
            throw runtime.newMathDomainError("Numerical argument is out of domain - isqrt");
        }

        return bignorm(runtime, floorSqrt(value));
    }

    private static JavaSites.BignumSites sites(ThreadContext context) {
        return context.sites.Bignum;
    }
}
