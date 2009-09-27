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
        bignum.index = ClassIndex.BIGNUM;
        
        bignum.defineAnnotatedMethods(RubyBignum.class);

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
    
    @Override
    public int getNativeTypeIndex() {
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
    public BigInteger getBigIntegerValue() {
        return value;
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
        boolean negative = bigint.signum() == -1;
        if (negative) bigint = bigint.abs();
        byte[] arr = bigint.toByteArray();
        double res = 0;
        double acc = 1d;
        for (int i = arr.length - 1; i > 0 ; i--) {
            res += (double) (arr[i] & 0xff) * acc;
            acc *= 256;
        }
        if (arr[0] != 0) {
            res += (double) (arr[0] & 0xff) * acc;
        }
        return (negative) ? -res : res ;
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
    @JRubyMethod(name = "to_s", optional = 1)
    public IRubyObject to_s(IRubyObject[] args) {
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
    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
        if (other instanceof RubyFixnum) {
            BigInteger result = value.multiply(fix2big(((RubyFixnum) other)));
            return result.signum() == 0 ? RubyFixnum.zero(runtime) : new RubyBignum(runtime, result);
        }
        if (other instanceof RubyBignum) {
            BigInteger result = value.multiply(((RubyBignum)other).value);
            return result.signum() == 0 ? RubyFixnum.zero(runtime) : new RubyBignum(runtime, result);
        } else if (other instanceof RubyFloat) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, "*", other);
    }

    public IRubyObject op_mul(ThreadContext context, long other) {
        Ruby runtime = context.getRuntime();
        BigInteger result = value.multiply(long2big(other));
        return result.signum() == 0 ? RubyFixnum.zero(runtime) : new RubyBignum(runtime, result);
    }

    /**
     * rb_big_divide. Shared part for both "/" and "div" operations.
     */
    private IRubyObject op_divide(ThreadContext context, IRubyObject other, boolean slash) {
        Ruby runtime = context.getRuntime();
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else if (other instanceof RubyFloat) {
            double div = big2dbl(this) / ((RubyFloat) other).getDoubleValue();
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
    @JRubyMethod(name = "divmod", required = 1, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
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
    
    @JRubyMethod(name = "divmod", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject divmod19(ThreadContext context, IRubyObject other) {
        if (!other.isNil() && other instanceof RubyFloat
                && ((RubyFloat)other).getDoubleValue() == 0) {
            throw context.getRuntime().newZeroDivisionError(); 
        }
        return divmod(context, other);
    }

    /** rb_big_modulo
     * 
     */
    @JRubyMethod(name = {"%", "modulo"}, required = 1)
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        final BigInteger otherValue;
        if (other instanceof RubyFixnum) {
            otherValue = fix2big((RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            otherValue = ((RubyBignum) other).value;
        } else {
            return coerceBin(context, "%", other);
        }
        Ruby runtime = context.getRuntime();
        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();

        BigInteger result = value.mod(otherValue.abs());
        if (otherValue.signum() == -1 && result.signum() != 0) result = otherValue.add(result);
        return bignorm(runtime, result);

    }

    /** rb_big_remainder
     * 
     */
    @JRubyMethod(name = "remainder", required = 1, compat = CompatVersion.RUBY1_8)
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
        Ruby runtime = context.getRuntime();
        if (otherValue.signum() == 0) throw runtime.newZeroDivisionError();
        return bignorm(runtime, value.remainder(otherValue));
    }
    
    @JRubyMethod(name = "remainder", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject remainder19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0) {
            throw context.getRuntime().newZeroDivisionError();
        }
        return remainder(context, other);
    }

    /** rb_big_quo

     * 
     */
    @JRubyMethod(name = "quo", required = 1, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject quo(ThreadContext context, IRubyObject other) {
    	if (other instanceof RubyNumeric) {
            return RubyFloat.newFloat(getRuntime(), big2dbl(this) / ((RubyNumeric) other).getDoubleValue());
        } else {
            return coerceBin(context, "quo", other);
    	}
    }
    
    @JRubyMethod(name = "quo", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject quo19(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyInteger && ((RubyInteger) other).getDoubleValue() == 0) {
            throw context.getRuntime().newZeroDivisionError();
        }
        return quo(context, other);
    }

    /** rb_big_pow
     * 
     */
    @JRubyMethod(name = {"**", "power"}, required = 1)
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        double d;
        if (other instanceof RubyFixnum) {
            RubyFixnum fix = (RubyFixnum) other;
            long fixValue = fix.getLongValue();
            // MRI issuses warning here on (RBIGNUM(x)->len * SIZEOF_BDIGITS * yy > 1024*1024)
            if (((value.bitLength() + 7) / 8) * 4 * Math.abs(fixValue) > 1024 * 1024) {
                getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big", fixValue);
            }
            if (fixValue >= 0) {
                return bignorm(getRuntime(), value.pow((int) fixValue)); // num2int is also implemented
            } else {
                return RubyFloat.newFloat(getRuntime(), Math.pow(big2dbl(this), (double)fixValue));
            }
        } else if (other instanceof RubyBignum) {
            d = ((RubyBignum) other).getDoubleValue();
            getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big", d);
        } else if (other instanceof RubyFloat) {
            d = ((RubyFloat) other).getDoubleValue();
        } else {
            return coerceBin(context, "**", other);

        }
        return RubyFloat.newFloat(getRuntime(), Math.pow(big2dbl(this), d));
    }

    /** rb_big_pow
     * 
     */
    @JRubyMethod(name = {"**", "power"}, required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_pow19(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.getRuntime();
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
                getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big", fixValue);
            }
            if (fixValue >= 0) {
                return bignorm(runtime, value.pow((int) fixValue)); // num2int is also implemented
            } else {
                return RubyFloat.newFloat(runtime, Math.pow(big2dbl(this), (double)fixValue));
            }
        } else if (other instanceof RubyBignum) {
            d = ((RubyBignum) other).getDoubleValue();
            getRuntime().getWarnings().warn(ID.MAY_BE_TOO_BIG, "in a**b, b may be too big", d);
        } else if (other instanceof RubyFloat) {
            d = ((RubyFloat) other).getDoubleValue();
        } else {
            return coerceBin(context, "**", other);

        }
        return RubyNumeric.dbl2num(runtime, Math.pow(big2dbl(this), d));
    }    
    
    /** rb_big_and
     * 
     */
    @JRubyMethod(name = "&", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.and(((RubyBignum) other).value));
        } else if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.and(fix2big((RubyFixnum)other)));
        }
        return coerceBin(context, "&", other);
    }
    
    @JRubyMethod(name = "&", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_and19(ThreadContext context, IRubyObject other) {
        return op_and(context, convertToInteger(context, other));
    }

    /** rb_big_or
     * 
     */
    @JRubyMethod(name = "|", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.or(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) { // no bignorm here needed
            return bignorm(getRuntime(), value.or(fix2big((RubyFixnum)other)));
        }
        return coerceBin(context, "|", other);
    }
    
    @JRubyMethod(name = "|", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_or19(ThreadContext context, IRubyObject other) {
        return op_or(context, convertToInteger(context, other));
    }

    /** rb_big_xor
     * 
     */
    @JRubyMethod(name = "^", required = 1, compat = CompatVersion.RUBY1_8)
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        other = other.convertToInteger();
        if (other instanceof RubyBignum) {
            return bignorm(getRuntime(), value.xor(((RubyBignum) other).value));
        }
        if (other instanceof RubyFixnum) {
            return bignorm(getRuntime(), value.xor(BigInteger.valueOf(((RubyFixnum) other).getLongValue())));
        }
        return coerceBin(context, "^", other);
    }
    
    @JRubyMethod(name = "^", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_xor19(ThreadContext context, IRubyObject other) {
        return op_xor(context, convertToInteger(context, other));
    }
    
    private IRubyObject convertToInteger(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat) {
            throw context.getRuntime().newTypeError("can't convert Float into Integer");
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
    @JRubyMethod(name = {"eql?", "==="}, required = 1)
    @Override
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
