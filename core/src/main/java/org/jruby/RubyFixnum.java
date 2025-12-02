/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2006 Antti Karanta <antti.karanta@napa.fi>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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

import java.math.BigInteger;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.api.Convert;
import org.jruby.api.Create;
import org.jruby.api.JRubyAPI;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.SimpleHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.marshal.MarshalLoader;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ConvertBytes;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;
import org.jruby.util.io.RubyInputStream;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asFloat;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newSharedString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Error.zeroDivisionError;
import static org.jruby.util.Numeric.f_odd_p;
import static org.jruby.util.Numeric.int_pow;

/**
 * Implementation of the Integer (Fixnum internal) class.
 */
public abstract class RubyFixnum extends RubyInteger implements Constantizable, Appendable, SimpleHash {

    public static RubyClass createFixnumClass(ThreadContext context, RubyClass fixnum) {
        var cache = context.runtime.fixnumCache;
        for (int i = 0; i < cache.length; i++) {
            cache[i] = newFixnumForCache(fixnum, i - CACHE_OFFSET);
        }

        return fixnum;
    }

    private static class LongFixnum extends RubyFixnum {
        final long value;
        public LongFixnum(RubyClass fixnum, long value) {
            super(fixnum);
            this.value = value;
        }
        @Override
        public long getValue() {
            return value;
        }
    }

    private static class IntFixnum extends RubyFixnum {
        final int value;
        public IntFixnum(RubyClass fixnum, int value) {
            super(fixnum);
            this.value = value;
        }
        @Override
        public long getValue() {
            return value;
        }
    }

    private static class ShortFixnum extends RubyFixnum {
        final short value;
        public ShortFixnum(RubyClass fixnum, short value) {
            super(fixnum);
            this.value = value;
        }
        @Override
        public long getValue() {
            return value;
        }
    }

    private static class ByteFixnum extends RubyFixnum {
        final byte value;
        public ByteFixnum(RubyClass fixnum, byte value) {
            super(fixnum);
            this.value = value;
        }
        @Override
        public long getValue() {
            return value;
        }
    }

    private static final int BIT_SIZE = 64;
    public static final long SIGN_BIT = Long.MIN_VALUE;
    public static final long MAX = Long.MAX_VALUE;
    public static final long MIN = Long.MIN_VALUE;
    private static final BigInteger MIN_NEGATED = BigInteger.valueOf(Long.MIN_VALUE).negate();
    public static final long MAX_MARSHAL_FIXNUM = (1L << 30) - 1; // 0x3fff_ffff
    public static final long MIN_MARSHAL_FIXNUM = - (1L << 30);   // -0x4000_0000
    public static final boolean USE_CACHE = Options.USE_FIXNUM_CACHE.load();
    public static final int CACHE_OFFSET;
    static {
        int cacheRange = 0;
        if (USE_CACHE) {
            cacheRange = Options.FIXNUM_CACHE_RANGE.load();
            if (cacheRange < 0) cacheRange = 0;
        }
        CACHE_OFFSET = cacheRange;
    }

    private static IRubyObject fixCoerce(ThreadContext context, IRubyObject x) {
        do {
            x = Convert.toInteger(context, x);
        } while (!(x instanceof RubyFixnum) && !(x instanceof RubyBignum));
        return x;
    }

    @Deprecated(since = "10.0.3.0", forRemoval = true)
    public RubyFixnum(Ruby runtime) {
        this(runtime, 0);
    }

    @Deprecated(since = "10.0.3.0", forRemoval = true)
    public RubyFixnum(Ruby runtime, long value) {
        super(runtime.getFixnum());
        this.setFrozen(true);
    }

    private RubyFixnum(RubyClass klazz) {
        super(klazz);
        this.setFrozen(true);
    }

    @Override
    public ClassIndex getNativeClassIndex() {
        return ClassIndex.FIXNUM;
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        Object constant = null;
        final long value = this.getValue();

        if (value < CACHE_OFFSET && value >= -CACHE_OFFSET) {
            Object[] fixnumConstants = metaClass.runtime.fixnumConstants;
            constant = fixnumConstants[(int) value + CACHE_OFFSET];

            if (constant == null) {
                constant = OptoFactory.newConstantWrapper(IRubyObject.class, this);
                fixnumConstants[(int) value + CACHE_OFFSET] = constant;
            }
        }

        return constant;
    }

    /**
     * short circuit for Fixnum key comparison
     */
    @Override
    public final boolean eql(IRubyObject other) {
        return other instanceof RubyFixnum && getValue() == ((RubyFixnum) other).getValue();
    }

    @Override
    public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
        long value = this.getValue();

        if (fixnumable(value)) return asBoolean(context, this == obj || eql(obj));

        return super.equal_p(context, obj);
    }

    /**
     * Determine whether the given long value is in fixnum range.
     *
     * @param value the value in question
     * @return true if the value is in fixnum range, false otherwise
     */
    private static boolean fixnumable(long value) {
        return value <= Long.MAX_VALUE / 2 && value >= Long.MIN_VALUE / 2;
    }

    @Override
    public boolean isImmediate() {
    	return true;
    }

    public RubyClass singletonClass(ThreadContext context) {
        throw typeError(context, "can't define singleton");
    }

    @Override
    public Class<?> getJavaClass() {
        return long.class;
    }

    @Override
    @JRubyAPI
    public BigInteger asBigInteger(ThreadContext context) {
        return BigInteger.valueOf(getValue());
    }

    @Override
    @JRubyAPI
    public double asDouble(ThreadContext context) {
        return getValue();
    }

    @Override
    @JRubyAPI
    public int asInt(ThreadContext context) { return (int) getValue(); }

    @Override
    @JRubyAPI
    public long asLong(ThreadContext context) {
        return getValue();
    }

    @Override
    @Deprecated(since = "10.0.0.0")
    public double getDoubleValue() {
        return getValue();
    }

    @Override
    @Deprecated(since = "10.0.0.0")
    public long getLongValue() {
        return getValue();
    }

    /**
     * When you know you are working directly with a Fixnum you can get
     * the long value it represents without using {@link org.jruby.RubyNumeric#asLong(ThreadContext)}.
     *
     * @return the long value
     */
    @JRubyAPI
    public abstract long getValue();

    @Override
    @JRubyAPI
    public int signum(ThreadContext context) { return Long.signum(getValue()); }

    @Override
    public RubyInteger negate(ThreadContext context) {
        return negate(context, getValue());
    }

    public static RubyFixnum newFixnum(Ruby runtime, long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            // long is never in cache range
            return new LongFixnum(runtime.getInteger(), value);
        }
        return newFixnum(runtime, (int) value);
    }

    public static RubyFixnum newFixnum(Ruby runtime, int value) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            // integer is never in cache range
            return new IntFixnum(runtime.getInteger(), value);
        }
        return newFixnum(runtime, (short) value);
    }

    public static RubyFixnum newFixnum(Ruby runtime, short value) {
        if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            return USE_CACHE && isInCacheRange(value) ? cachedFixnum(runtime, value) : new ShortFixnum(runtime.getInteger(), value);
        }
        return newFixnum(runtime, (byte) value);
    }

    public static RubyFixnum newFixnum(Ruby runtime, byte value) {
        // using ShortFixnum until there's value in using ByteFixnum
        return USE_CACHE && isInCacheRange(value) ? cachedFixnum(runtime, value) : new ShortFixnum(runtime.getInteger(), value);
    }

    private static RubyFixnum newFixnumForCache(RubyClass fixnum, int value) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            return new IntFixnum(fixnum, value);
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            return new ShortFixnum(fixnum, (short) value);
        }
        // using ShortFixnum until there's value in using ByteFixnum
        return new ShortFixnum(fixnum, (byte) value);
    }

    private static boolean isInCacheRange(int value) {
        return value <= CACHE_OFFSET - 1 && value >= -CACHE_OFFSET;
    }

    private static RubyFixnum cachedFixnum(Ruby runtime, int value) {
        // This truncates to int but we determine above that it's in cache range
        return runtime.fixnumCache[value + CACHE_OFFSET];
    }

    @Deprecated(since = "9.2.1.0") // not used
    public final RubyFixnum newFixnum(long newValue) {
        return newFixnum(getCurrentContext().runtime, newValue);
    }

    public static RubyFixnum zero(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 0 ? runtime.fixnumCache[CACHE_OFFSET] : new ShortFixnum(runtime.getInteger(), (byte) 0);
    }

    public static RubyFixnum one(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 1 ? runtime.fixnumCache[CACHE_OFFSET + 1] : new ShortFixnum(runtime.getInteger(), (byte) 1);
    }

    public static RubyFixnum two(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 2 ? runtime.fixnumCache[CACHE_OFFSET + 2] : new ShortFixnum(runtime.getInteger(), (byte) 2);
    }

    public static RubyFixnum three(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 3 ? runtime.fixnumCache[CACHE_OFFSET + 3] : new ShortFixnum(runtime.getInteger(), (byte) 3);
    }

    public static RubyFixnum four(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 4 ? runtime.fixnumCache[CACHE_OFFSET + 4] : new ShortFixnum(runtime.getInteger(), (byte) 4);
    }

    public static RubyFixnum five(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return CACHE_OFFSET > 5 ? runtime.fixnumCache[CACHE_OFFSET + 5] : new ShortFixnum(runtime.getInteger(), (byte) 5);
    }

    public static RubyFixnum minus_one(Ruby runtime) {
        // using ShortFixnum until there's value in using ByteFixnum
        return -CACHE_OFFSET <= -1 ? runtime.fixnumCache[CACHE_OFFSET - 1] : new ShortFixnum(runtime.getInteger(), (byte) -1);
    }

    public RubyFixnum hash(ThreadContext context) {
        return asFixnum(context, longHashCode());
    }

    @Override
    public final int hashCode() {
        return (int) longHashCode();
    }

    @Override
    public long longHashCode() {
        return fixHash(getRuntime(), getValue());
    }

    private static long fixHash(Ruby runtime, long value) {
        return Helpers.multAndMix(Ruby.getHashSeed0(), value);
    }

    /*  ================
     *  Instance Methods
     *  ================
     */
    @Override
    public IRubyObject times(ThreadContext context, Block block) {
        if (block.isGiven()) {
            final long value = this.getValue();
            boolean checkArity = block.type.checkArity;

            if (block.getSignature() == Signature.NO_ARGUMENTS) {
                if (checkArity) {
                    // must pass arg
                    final IRubyObject nil = context.nil;
                    for (long i = 0; i < value; i++) {
                        block.yieldSpecific(context, nil);
                    }
                } else {
                    // no arg needed
                    for (long i = 0; i < value; i++) {
                        block.yieldSpecific(context);
                    }
                }
            } else {
                for (long i = 0; i < value; i++) {
                    block.yield(context, asFixnum(context, i));
                }
            }
            return this;
        }
        return RubyEnumerator.enumeratorizeWithSize(context, this, "times", RubyInteger::timesSize);
    }
    /** rb_fix_ceil
     *
     */
    @Override
    public IRubyObject ceil(ThreadContext context, IRubyObject arg){
        long ndigits = toLong(context, arg);
        if (ndigits >= 0) return this;

        RubyNumeric f = int_pow(context, 10, -ndigits);
        if (f instanceof RubyFixnum fixnum) {
            long x = getValue(), y = fixnum.getValue();
            boolean neg = x < 0;
            if (neg) x = -x;
            else x += y - 1;
            x = (x / y) * y;
            if (neg) x = -x;
            return asFixnum(context, x);
        }

        return integerCeil(context, f);
    }

    /** rb_fix_floor
     *
     */
    @Override
    public IRubyObject floor(ThreadContext context, IRubyObject arg){
        long ndigits = toLong(context, arg);
        if (ndigits >= 0) return this;

        RubyInteger f = (RubyInteger) int_pow(context, 10, -ndigits);
        if (f instanceof RubyFixnum fixnum) {
            long x = getValue(), y = fixnum.getValue();
            boolean neg = x < 0;
            if (neg) x = -x + y - 1;
            x = x / y * y;
            if (neg) x = -x;
            return asFixnum(context, x);
        }

        return integerFloor(context, f);
    }

    /** rb_fix_truncate
     *
     */
    @Override
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        long self = this.getValue();
        if (self > 0) {
            return floor(context, arg);
        } else if (self < 0) {
            return ceil(context, arg);
        } else {
            return this;
        }
    }

    /** rb_fix_digits
     *
     */
    @Override
    public RubyArray digits(ThreadContext context, IRubyObject baseArg) {
        long value = this.getValue();
        if (value < 0) throw context.runtime.newMathDomainError("out of domain");

        var base = Convert.toInteger(context, baseArg);
        if (base instanceof RubyBignum bigBase) {
            return RubyBignum.newBignum(context.runtime, value).digits(context, bigBase);
        }

        long longBase = ((RubyFixnum) base).getValue();
        if (longBase < 0) throw argumentError(context, "negative radix");
        if (longBase < 2) throw argumentError(context, "invalid radix: " + longBase);

        if (value == 0) return newArray(context, asFixnum(context, 0));

        RubyArray<?> res = Create.allocArray(context, (int) (Math.log(value) / Math.log(longBase)) + 1);
        do {
            res.append(context, asFixnum(context, value % longBase));
            value /= longBase;
        } while (value > 0);

        return res;
    }


    /** fix_to_s
     *
     */
    @Deprecated(since = "10.0.0.0")
    public RubyString to_s(IRubyObject[] args) {
        var context = getCurrentContext();
        return switch (args.length) {
            case 0 -> to_s(context);
            case 1 -> to_s(context, args[0]);
            default -> throw argumentError(context, args.length, 1);
        };
    }

    @Override
    public RubyString to_s(ThreadContext context) {
        return longToString(context, getValue(), 10);
    }

    @Override
    public RubyString to_s(ThreadContext context, IRubyObject arg0) {
        int base = toInt(context, arg0);
        if (base < 2 || base > 36) throw argumentError(context, "illegal radix " + base);

        return longToString(context, getValue(), base);
    }

    private RubyString longToString(ThreadContext context, long value, int base) {
        return base == 10 && value >= -256 && value < 256 ?
                newSharedString(context, ConvertBytes.byteToSharedByteList((short) value), USASCIIEncoding.INSTANCE) :
                newString(context, ConvertBytes.longToByteList(value, base), USASCIIEncoding.INSTANCE);
    }

    /** fix_to_sym
     *
     */
    @Deprecated(since = "9.0.0.0")
    public IRubyObject to_sym() {
        var context = getCurrentContext();
        RubySymbol symbol = RubySymbol.getSymbolLong(context.runtime, getValue());

        return symbol != null ? symbol : context.nil;
    }

    /** fix_uminus
     *
     */
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        return negate(context, getValue());
    }

    private static RubyInteger negate(ThreadContext context, final long value) {
        return value == MIN ?
                RubyBignum.newBignum(context.runtime, MIN_NEGATED) :
                asFixnum(context, -value);
    }

    /** fix_plus
     *
     */
    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum fixnum) {
            return op_plus(context, fixnum.getValue());
        }
        return addOther(context, other);
    }

    @Override
    public IRubyObject op_plus(ThreadContext context, long other) {
        long value = this.getValue();
        long result = value + other;
        if (addOverflowed(other, value, result)) {
            return addAsBignum(context, value, other);
        }

        return asFixnum(context, result);
    }

    private static boolean addOverflowed(long other, long value, long result) {
        return ((value ^ result) & (other ^ result)) < 0;
    }

    public IRubyObject op_plus(ThreadContext context, double other) {
        return asFloat(context, (double) getValue() + other);
    }

    public IRubyObject op_plus_one(ThreadContext context) {
        long value = this.getValue();
        if (value == MAX) {
            return addAsBignum(context, value, BigInteger.ONE);
        }
        return asFixnum(context, value + 1);
    }

    public IRubyObject op_plus_two(ThreadContext context) {
        long value = this.getValue();
        if (value >= MAX - 2) {
            return addAsBignum(context, value, BigInteger.TWO);
        }
        return asFixnum(context, value + 2);
    }

    private static RubyInteger addAsBignum(ThreadContext context, long value, long other) {
        return RubyBignum.bignorm(context.runtime, BigInteger.valueOf(value).add(BigInteger.valueOf(other)));
    }

    private static RubyInteger addAsBignum(ThreadContext context, long value, BigInteger other) {
        return RubyBignum.bignorm(context.runtime, BigInteger.valueOf(value).add(other));
    }

    private IRubyObject addOther(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> bignum.op_plus(context, getValue());
            case RubyFloat flote -> op_plus(context, flote.value);
            default -> coerceBin(context, sites(context).op_plus, other);
        };
    }

    /** fix_minus
     *
     */
    @Override
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum fixnum) {
            return op_minus(context, fixnum.getValue());
        }
        return subtractOther(context, other);
    }

    @Override
    public IRubyObject op_minus(ThreadContext context, long other) {
        long value = this.getValue();
        long result = value - other;
        if (((value ^ other) & (value ^ result)) < 0) {
            return subtractAsBignum(context, other);
        }
        return asFixnum(context, result);
    }

    public IRubyObject op_minus(ThreadContext context, double other) {
        return asFloat(context, (double) getValue() - other);
    }

    /**
     * @param context
     * @return RubyInteger
     */
    public IRubyObject op_minus_one(ThreadContext context) {
        long value = this.getValue();
        if (value == MIN) {
            return subtractAsBignum(context, BigInteger.ONE);
        }
        return asFixnum(context, value - 1);
    }

    /**
     * @param context
     * @return RubyInteger
     */
    public IRubyObject op_minus_two(ThreadContext context) {
        long value = this.getValue();
        if (value <= MIN + 1) {
            return subtractAsBignum(context, 2);
        }
        return asFixnum(context, value - 2);
    }

    private RubyInteger subtractAsBignum(ThreadContext context, long other) {
        return RubyBignum.bignorm(context.runtime, BigInteger.valueOf(getValue()).subtract(BigInteger.valueOf(other)));
    }

    private RubyInteger subtractAsBignum(ThreadContext context, BigInteger other) {
        return RubyBignum.bignorm(context.runtime, BigInteger.valueOf(getValue()).subtract(other));
    }

    private IRubyObject subtractOther(ThreadContext context, IRubyObject other) {
        return switch(other) {
            case RubyBignum bignum -> RubyBignum.bignorm(context.runtime, BigInteger.valueOf(getValue()).subtract(((RubyBignum) other).value));
            case RubyFloat flote -> op_minus(context, flote.value);
            default -> coerceBin(context, sites(context).op_minus, other);
        };
    }

    /** fix_mul
     *
     */
    @Override
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum fixnum ?
                op_mul(context, fixnum.getValue()) :
                multiplyOther(context, other);
    }

    private IRubyObject multiplyOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum bignum) return bignum.op_mul(context, this.getValue());
        if (other instanceof RubyFloat flote) return op_mul(context, flote.value);

        return coerceBin(context, sites(context).op_times, other);
    }

    @Override
    public IRubyObject op_mul(ThreadContext context, long other) {
        long value = this.getValue();
        long low = value * other;
        long high = Math.multiplyHigh(value, other);
        if ((high == 0 && low >= 0) // result is zero or positive and < MAX
                || (high == -1 && low < 0)) { // result is negative and >= MIN
            return asFixnum(context, low);
        }

        // overflow, use Bignum
        return multiplyAsBignum(context, value, other);
    }

    private static RubyInteger multiplyAsBignum(ThreadContext context, long value, long other) {
        return RubyBignum.bignorm(context.runtime, BigInteger.valueOf(value).multiply(BigInteger.valueOf(other)));
    }

    public IRubyObject op_mul(ThreadContext context, double other) {
        return context.runtime.newFloat((double) getValue() * other);
    }

    /** fix_div
     * here is terrible MRI gotcha:
     * 1.div 3.0$ -&gt; 0
     * 1 / 3.0  $ -&gt; 0.3333333333333333
     *
     * MRI is also able to do it in one place by looking at current frame in rb_num_coerce_bin:
     * rb_funcall(x, ruby_frame-&gt;orig_func, 1, y);
     *
     * also note that RubyFloat doesn't override Numeric.div
     */
    @Override
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        checkZeroDivisionError(context, other);

        if (other instanceof RubyFixnum) {
            return idivLong(context, getValue(), ((RubyFixnum) other).getValue());
        }
        return coerceBin(context, sites(context).div, other);
    }

    @Override
    public IRubyObject idiv(ThreadContext context, long other) {
        return idivLong(context, getValue(), other);
    }

    @Override
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return idivLong(context, getValue(), ((RubyFixnum) other).getValue());
        }
        return coerceBin(context, sites(context).op_quo, other);
    }

    public IRubyObject op_div(ThreadContext context, long other) {
        return idivLong(context, getValue(), other);
    }

    @Override
    public RubyBoolean odd_p(ThreadContext context) {
        return (getValue() & 1) != 0 ? context.tru : context.fals;
    }

    @Override
    public RubyBoolean even_p(ThreadContext context) {
        return (getValue() & 1) == 0 ? context.tru : context.fals;
    }

    public IRubyObject pred(ThreadContext context) {
        return op_minus_one(context);
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject idiv(ThreadContext context, IRubyObject other, String method) {
        if (other instanceof RubyFixnum) {
            return idivLong(context, getValue(), ((RubyFixnum) other).getValue());
        }
        return coerceBin(context, method, other);
    }

    public IRubyObject idiv(ThreadContext context, IRubyObject other, CallSite site) {
        if (other instanceof RubyFixnum) {
            return idivLong(context, getValue(), ((RubyFixnum) other).getValue());
        }
        return coerceBin(context, site, other);
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject idiv(ThreadContext context, long y, String method) {
        long x = getValue();

        return idivLong(context, x, y);
    }

    private RubyInteger idivLong(ThreadContext context, long x, long y) {
        if (y == 0) throw context.runtime.newZeroDivisionError();

        long result;
        if (y > 0) {
            if (x >= 0) {
                result = x / y;          // x >= 0, y > 0;
            } else {
                result = (x + 1) / y - 1;  // x < 0, y > 0;  // OOPS "=" was omitted
            }
        } else if (x > 0) {
            result = (x - 1) / y - 1;    // x > 0, y < 0;
        } else if (y == -1) {
            if (x == MIN) return RubyBignum.newBignum(context.runtime, MIN_NEGATED);
            result = -x;
        } else {
            result = x / y;  // x <= 0, y < 0;
        }
        return asFixnum(context, result);
    }

    /** fix_mod
     *
     */
    @Override
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return moduloFixnum(context, (RubyFixnum) other);
        }
        checkZeroDivisionError(context, other);
        return coerceBin(context, sites(context).op_mod, other);
    }

    @Override
    public IRubyObject op_mod(ThreadContext context, long other) {
        return moduloFixnum(context, other);
    }

    @Override
    public IRubyObject modulo(ThreadContext context, IRubyObject other) {
        return op_mod(context, other);
    }

    @Override
    IRubyObject modulo(ThreadContext context, long other) {
        return op_mod(context, other);
    }

    private IRubyObject moduloFixnum(ThreadContext context, RubyFixnum other) {
        return moduloFixnum(context, other.getValue());
    }

    private IRubyObject moduloFixnum(ThreadContext context, long other) {
        // Java / and % are not the same as ruby
        long x = getValue();
        long y = other;
        if (y == 0) throw context.runtime.newZeroDivisionError();

        long mod = x % y;
        if (mod < 0 && y > 0 || mod > 0 && y < 0) {
            mod += y;
        }
        return asFixnum(context, mod);
    }

    /** fix_divmod
     *
     */
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return divmodFixnum(context, (RubyFixnum) other);
        }
        checkZeroDivisionError(context, other);
        return coerceBin(context, sites(context).divmod, other);
    }

    private IRubyObject divmodFixnum(ThreadContext context, RubyFixnum other) {
        final long x = this.getValue();
        final long y = other.getValue();
        if (y == 0) throw context.runtime.newZeroDivisionError();

        long mod; final RubyInteger integerDiv;
        if (y == -1) {
            if (x == MIN) {
                integerDiv = RubyBignum.newBignum(context.runtime, MIN_NEGATED);
            } else {
                integerDiv = Convert.asFixnum(context, -x);
            }
            mod = 0;
        } else {
            long div = x / y;
            // Next line avoids using the slow: mod = x % y,
            // and I believe there is no possibility of integer overflow.
            mod = x - y * div;
            if (mod < 0 && y > 0 || mod > 0 && y < 0) {
                div -= 1; // horrible sudden thought: might this overflow? probably not?
                mod += y;
            }
            integerDiv = Convert.asFixnum(context, div);
        }
        IRubyObject fixMod = Convert.asFixnum(context, mod);
        return newArray(context, integerDiv, fixMod);
    }

    /** fix_pow
     *
     */
    @Override
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return powerFixnum(context, (RubyFixnum) other);
        } else if (other instanceof RubyBignum) {
            return powerOther(context, other);
        } else if (other instanceof RubyFloat flote) {
            double d_other = flote.asDouble(context);
            if (getValue() < 0 && (d_other != Math.round(d_other))) {
                RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                return numFuncall(context, complex, sites(context).op_exp_complex, other);
            }
        }
        return coerceBin(context, sites(context).op_exp, other);
    }

    public IRubyObject op_pow(ThreadContext context, long other) {
        return powerFixnum(context, asFixnum(context, other));
    }

    private IRubyObject powerOther(ThreadContext context, IRubyObject other) {
        final Ruby runtime = context.runtime;
        final long a = this.getValue();
        if (other instanceof RubyBignum) {
            if (a == 1) return RubyFixnum.one(runtime);
            if (a == -1) {
                return ((RubyBignum) other).even_p(context).isTrue() ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
            }
            if (sites(context).op_lt_bignum.call(context, other, other, RubyFixnum.zero(runtime)).isTrue()) {
                RubyRational rational = RubyRational.newRationalRaw(runtime, this);
                return numFuncall(context, rational, sites(context).op_exp_rational, other);
            }
            if (a == 0) return RubyFixnum.zero(runtime);
            return RubyBignum.newBignum(runtime, RubyBignum.long2big(a)).op_pow(context, other);
        }
        if (other instanceof RubyFloat) {
            double b = ((RubyFloat) other).value;
            if (b == 0.0 || a == 1) return runtime.newFloat(1.0);
            if (a == 0) return runtime.newFloat(b < 0 ? 1.0 / 0.0 : 0.0);
            return RubyFloat.newFloat(runtime, Math.pow(a, b));
        }
        return coerceBin(context, sites(context).op_exp, other);
    }

    // MRI: fix_pow
    private RubyNumeric powerFixnum(ThreadContext context, RubyFixnum other) {
        long a = getValue();
        long b = other.getValue();

        if (a == 1) return asFixnum(context, 1);
        if (a == -1) return asFixnum(context, (b % 2) != 0 ? -1 : 1);
        if (b <  0) return fixPowInverted(context, -b);
        if (b == 0) return asFixnum(context, 1);
        if (b == 1) return this;
        if (a == 0) return asFixnum(context, 0);
        return int_pow(context, a, b);
    }

    // MRI: fix_pow_inverted
    private RubyNumeric fixPowInverted(ThreadContext context, long minusb) {
        if (getValue() == 0) {
            throw zeroDivisionError(context);
        } else {
            IRubyObject y = op_pow(context, minusb); // rb_int_pow but we know we are a fixnum

            if (y instanceof RubyFloat flote) {
                double d = Math.pow((double) getValue(), flote.value);
                return asFloat(context, 1.0 / d);
            } else {
                return RubyRational.newRationalRaw(context.runtime, asFixnum(context, 1), y);
            }
        }
    }

    // MRI: int_pow_tmp1
    protected IRubyObject intPowTmp1(ThreadContext context, RubyInteger y, long mm, boolean negaFlg) {
        long xx = this.getValue();
        long tmp = 1L;
        long yy;

        var one = asFixnum(context, 1);
        for (/*NOP*/; !(y instanceof RubyFixnum); y = (RubyInteger) sites(context).op_rshift.call(context, y, y, one)) {
            if (f_odd_p(context, y)) tmp = (tmp * xx) % mm;

            xx = (xx * xx) % mm;
        }
        for (yy = y.asLong(context); yy != 0L; yy >>= 1L) {
            if ((yy & 1L) != 0L) tmp = (tmp * xx) % mm;

            xx = (xx * xx) % mm;
        }

        if (negaFlg && (tmp != 0)) tmp -= mm;

        return asFixnum(context, tmp);
    }

    @Deprecated(since = "9.2.7.0")
    protected IRubyObject intPowTmp2(ThreadContext context, IRubyObject y, final long mm, boolean negaFlg) {
        return intPowTmp2(context, (RubyInteger) y, mm, negaFlg);
    }

    // MRI: int_pow_tmp2
    IRubyObject intPowTmp2(ThreadContext context, RubyInteger y, final long mm, boolean negaFlg) {
        long tmp = 1L;
        long yy;

        RubyFixnum tmp2 = asFixnum(context, tmp);
        RubyFixnum xx = this;

        for (/*NOP*/; !(y instanceof RubyFixnum); y = (RubyInteger) sites(context).op_rshift.call(context, y, y, RubyFixnum.one(context.runtime))) {
            if (f_odd_p(context, y)) {
                tmp2 = mulModulo(context, tmp2, xx, mm);
            }
            xx = mulModulo(context, xx, xx, mm);
        }
        for (yy = ((RubyFixnum) y).getValue(); yy != 0; yy >>= 1L) {
            if ((yy & 1L) != 0) {
                tmp2 = mulModulo(context, tmp2, xx, mm);
            }
            xx = mulModulo(context, xx, xx, mm);
        }

        tmp = tmp2.getValue();
        if (negaFlg && (tmp != 0)) {
            tmp -= mm;
        }
        return asFixnum(context, tmp);
    }

    // MRI: MUL_MODULO macro defined within int_pow_tmp2 in numeric.c
    private static RubyFixnum mulModulo(ThreadContext context, RubyFixnum a, RubyFixnum b, long c) {
        return (RubyFixnum) ((RubyInteger) a.op_mul(context, b.getValue())).modulo(context, c);
    }

    /** fix_abs
     *
     */
    @Override
    public IRubyObject abs(ThreadContext context) {
        if (getValue() < 0) {
            // A gotcha for Long.MIN_VALUE: value = -value
            if (getValue() == Long.MIN_VALUE) {
                return RubyBignum.newBignum(context.runtime, MIN_NEGATED);
            }
            return asFixnum(context, -getValue());
        }
        return this;
    }

    /** fix_abs/1.9
     *
     */
    @Override
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    /** fix_equal
     *
     */
    @Override
    public IRubyObject op_equal(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum ?
                op_equal(context, ((RubyFixnum) other).getValue()) : op_equalOther(context, other);
    }

    public IRubyObject op_equal(ThreadContext context, long other) {
        return asBoolean(context, getValue() == other);
    }

    public IRubyObject op_equal(ThreadContext context, double other) {
        return asBoolean(context, (double) getValue() == other);
    }

    /** fix_not_equal
     *
     */
    @Override
    public IRubyObject op_not_equal(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum ?
                op_not_equal(context, ((RubyFixnum) other).getValue()) : super.op_not_equal(context, other);
    }

    public IRubyObject op_not_equal(ThreadContext context, long other) {
        return asBoolean(context, getValue() != other);
    }

    public IRubyObject op_not_equal(ThreadContext context, double other) {
        return asBoolean(context, (double) getValue() != other);
    }

    public boolean op_equal_boolean(ThreadContext context, long other) {
        return getValue() == other;
    }

    public final boolean fastEqual(RubyFixnum other) {
        return getValue() == other.getValue();
    }

    private IRubyObject op_equalOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum bignum) {
            return asBoolean(context, BigInteger.valueOf(this.getValue()).compareTo(bignum.value) == 0);
        }
        if (other instanceof RubyFloat flote) {
            return op_equal(context, flote.value);
        }
        return super.op_num_equal(context, other);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof RubyFixnum) {
            return getValue() == ((RubyFixnum) other).getValue();
        }
        return false;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        if (other instanceof RubyFixnum fixnum) {
            long otherValue = fixnum.getValue();
            return getValue() == otherValue ? 0 : getValue() > otherValue ? 1 : -1;
        }
        return compareToOther(other);
    }

    private int compareToOther(IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> BigInteger.valueOf(getValue()).compareTo(bignum.value);
            case RubyFloat flote ->Double.compare((double) getValue(), flote.value);
            default -> {
                var context = getRuntime().getCurrentContext();
                yield toInt(context, coerceCmp(context, sites(context).op_cmp, other));
            }
        };
    }

    /** fix_cmp
     *
     */
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum fixnum ?
                op_cmp(context, fixnum.getValue()) : compareOther(context, other);
    }

    public IRubyObject op_cmp(ThreadContext context, long other) {
        return getValue() == other ? asFixnum(context, 0) : asFixnum(context, getValue() > other ? 1 : -1);

    }

    public IRubyObject op_cmp(ThreadContext context, double other) {
        return dbl_cmp(context.runtime, (double) getValue(), other);
    }

    private IRubyObject compareOther(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> asFixnum(context, BigInteger.valueOf(getValue()).compareTo(bignum.value));
            case RubyFloat flote -> dbl_cmp(context.runtime, (double) getValue(), flote.value);
            default -> coerceCmp(context, sites(context).op_cmp, other);
        };
    }

    /** fix_gt
     *
     */
    @Override
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum fixnum ?
                asBoolean(context, getValue() > fixnum.getValue()) :
                op_gtOther(context, other);
    }

    public IRubyObject op_gt(ThreadContext context, long other) {
        return asBoolean(context, getValue() > other);
    }

    public boolean op_gt_boolean(ThreadContext context, long other) {
        return getValue() > other;
    }

    private IRubyObject op_gtOther(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> asBoolean(context, BigInteger.valueOf(getValue()).compareTo(bignum.value) > 0);
            case RubyFloat flote -> asBoolean(context, (double) getValue() > flote.value);
            default -> coerceRelOp(context, sites(context).op_gt, other);
        };
    }

    /** fix_ge
     *
     */
    @Override
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum fixnum ?
                asBoolean(context, getValue() >= fixnum.getValue()) :
                op_geOther(context, other);
    }

    public IRubyObject op_ge(ThreadContext context, long other) {
        return asBoolean(context, getValue() >= other);
    }

    public boolean op_ge_boolean(ThreadContext context, long other) {
        return getValue() >= other;
    }

    private IRubyObject op_geOther(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> asBoolean(context, BigInteger.valueOf(getValue()).compareTo(bignum.value) >= 0);
            case RubyFloat flote -> asBoolean(context, (double) getValue() >= flote.value);
            default -> coerceRelOp(context, sites(context).op_ge, other);
        };
    }

    /** fix_lt
     *
     */
    @Override
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_lt(context, ((RubyFixnum) other).getValue());
        }
        return op_ltOther(context, other);
    }

    public IRubyObject op_lt(ThreadContext context, long other) {
        return asBoolean(context, getValue() < other);
    }

    public boolean op_lt_boolean(ThreadContext context, long other) {
        return getValue() < other;
    }

    private IRubyObject op_ltOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return asBoolean(context,
                    BigInteger.valueOf(getValue()).compareTo(((RubyBignum) other).value) < 0);
        }
        if (other instanceof RubyFloat) {
            return asBoolean(context, (double) getValue() < ((RubyFloat) other).value);
        }
        return coerceRelOp(context, sites(context).op_lt, other);
    }

    /** fix_le
     *
     */
    @Override
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return asBoolean(context, getValue() <= ((RubyFixnum) other).getValue());
        }
        return op_leOther(context, other);
    }

    public IRubyObject op_le(ThreadContext context, long other) {
        return asBoolean(context, getValue() <= other);
    }

    public boolean op_le_boolean(ThreadContext context, long other) {
        return getValue() <= other;
    }

    private IRubyObject op_leOther(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyBignum bignum -> asBoolean(context, BigInteger.valueOf(getValue()).compareTo(bignum.value) <= 0);
            case RubyFloat flote -> asBoolean(context, (double) getValue() <= flote.value);
            default -> coerceRelOp(context, sites(context).op_le, other);
        };
    }

    /** fix_rev
     *
     */
    @Override
    public IRubyObject op_neg(ThreadContext context) {
        return asFixnum(context, ~getValue());
    }

    /** fix_and
     *
     */
    @Override
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        return switch (other) {
            case RubyFixnum fixnum -> asFixnum(context, getValue() & fixnum.getValue());
            case RubyBignum bignum -> bignum.op_and(context, this);
            default -> coerceBit(context, sites(context).checked_op_and, other);
        };
    }

    public IRubyObject op_and(ThreadContext context, long other) {
        return asFixnum(context, getValue() & other);
    }

    /** fix_or
     *
     */
    @Override
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        return switch(other) {
            case RubyFixnum fixnum -> asFixnum(context, getValue() | fixnum.getValue());
            case RubyBignum bignum -> bignum.op_or(context, this);
            default -> coerceBit(context, sites(context).checked_op_or, other);
        };
    }

    public IRubyObject op_or(ThreadContext context, long other) {
        return asFixnum(context, getValue() | other);
    }

    /** fix_xor
     *
     */
    @Override
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        return switch(other) {
            case RubyFixnum fixnum -> asFixnum(context, getValue() ^ fixnum.getValue());
            case RubyBignum bignum -> bignum.op_xor(context, this);
            default -> coerceBit(context, sites(context).checked_op_xor, other);
        };
    }

    public IRubyObject op_xor(ThreadContext context, long other) {
        return asFixnum(context, getValue() ^ other);
    }

    /** rb_fix_aref
     *
     */
    @Override
    protected IRubyObject op_aref_subclass(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyFixnum) && !((other = fixCoerce(context, other)) instanceof RubyFixnum)) {
            BigInteger big = ((RubyBignum) other).value;
            other = RubyBignum.bignorm(context.runtime, big);
            if (!(other instanceof RubyFixnum)) return asFixnum(context, big.signum() == 0 || getValue() >= 0 ? 0 : 1);
        }

        long otherValue = ((RubyFixnum) other).getValue();

        if (otherValue < 0) return asFixnum(context, 0);
        if (BIT_SIZE - 1 < otherValue) return asFixnum(context, getValue() < 0 ? 1 : 0);

        return asFixnum(context, (getValue() & (1L << otherValue)) == 0 ? 0 : 1);
    }

    /** fix_lshift
     *
     */
    @Override
    public IRubyObject op_lshift(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyFixnum)) {
            return RubyBignum.newBignum(context.runtime, getValue()).op_lshift(context, other);
        }

        return op_lshift(context, ((RubyFixnum) other).getValue());
    }

    @Override
    public RubyInteger op_lshift(ThreadContext context, final long width) {
        return width < 0 ? rshift(context, -width) : lshift(context, width);
    }

    private RubyInteger lshift(ThreadContext context, final long width) {
        if (width > BIT_SIZE - 1 || ((~0L << BIT_SIZE - width - 1) & getValue()) != 0) {
            return RubyBignum.newBignum(context.runtime, getValue()).op_lshift(context, width);
        }
        return asFixnum(context, getValue() << width);
    }

    @Deprecated(since = "9.2.0.0") // no longer used
    public IRubyObject op_lshift(long width) {
        return op_lshift(getCurrentContext(), width);
    }

    /** fix_rshift
     *
     */
    @Override
    public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum fix ?
                op_rshift(context, fix.getValue()) :
                RubyBignum.newBignum(context.runtime, getValue()).op_rshift(context, other);
    }

    @Override
    public RubyInteger op_rshift(ThreadContext context, final long width) {
        if (width == 0) return this;

        return width < 0 ? lshift(context, -width) : rshift(context, width);
    }

    private RubyFixnum rshift(ThreadContext context, final long width) {
        return width >= BIT_SIZE - 1 ?
                asFixnum(context, getValue() < 0 ? -1 : 0) :
                asFixnum(context, getValue() >> width);
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject op_rshift(long width) {
        return op_rshift(getCurrentContext(), width);
    }

    /** fix_to_f
     *
     */
    @Override
    public IRubyObject to_f(ThreadContext context) {
        return RubyFloat.newFloat(context.runtime, (double) getValue());
    }

    @Override
    @Deprecated(since = "10.0.0.0")
    public IRubyObject to_f() {
        return RubyFloat.newFloat(metaClass.runtime, (double) getValue());
    }

    /** fix_size
     *
     */
    @Override
    public IRubyObject size(ThreadContext context) {
        return asFixnum(context, (long) ((BIT_SIZE + 7) / 8));
    }

    @Deprecated(since = "9.2.0.0")
    public IRubyObject zero_p() {
        return zero_p(getCurrentContext());
    }

    /** fix_zero_p
     *
     */
    @Override
    public IRubyObject zero_p(ThreadContext context) {
        return asBoolean(context, getValue() == 0);
    }

    @Override
    public final boolean isZero(ThreadContext context) {
        return getValue() == 0;
    }

    @Override
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero(context) ? context.nil : this;
    }

    @Override
    final boolean isOne(ThreadContext context) {
        return getValue() == 1;
    }

    @Override
    public IRubyObject succ(ThreadContext context) {
        return op_plus_one(context);
    }

    @Override
    public IRubyObject bit_length(ThreadContext context) {
        long tmpValue = getValue();
        if (tmpValue < 0) tmpValue = ~getValue();

        return asFixnum(context, 64 - Long.numberOfLeadingZeros(tmpValue));
    }

    @Deprecated(since = "10.0.3.0")
    @Override
    public IRubyObject id() {
        long value = this.getValue();

        if (fixnumable(value)) {
            return newFixnum(metaClass.runtime, 2 * value + 1);
        }

        return super.id();
    }

    @Override
    public RubyInteger __id__(ThreadContext context) {
        long value = this.getValue();

        if (fixnumable(value)) {
            return asFixnum(context, 2 * value + 1);
        }

        return super.__id__(context);
    }

    // Piece of mri rb_to_id
    @Override
    public String asJavaString() {
        throw typeError(getRuntime().getCurrentContext(), "", this, " is not a symbol");
    }

    @Deprecated(since = "10.0.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    public static RubyFixnum unmarshalFrom(org.jruby.runtime.marshal.UnmarshalStream input) throws java.io.IOException {
        return input.getRuntime().newFixnum(input.unmarshalInt());
    }

    public static RubyFixnum unmarshalFrom(ThreadContext context, RubyInputStream in, MarshalLoader input) {
        return asFixnum(context, input.unmarshalInt(context, in));
    }

    private void checkZeroDivisionError(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat && ((RubyFloat) other).value == 0.0d) {
            throw context.runtime.newZeroDivisionError();
        }
    }

    @Override
    public RubyInteger convertToInteger(String method) {
        return this;
    }

    // MRI: fix_fdiv_double
    @Override
    public IRubyObject fdivDouble(ThreadContext context, IRubyObject y) {
        if (y instanceof RubyFixnum yy) {
            return context.runtime.newFloat(((double) getValue()) / ((double) yy.getValue()));
        }
        if (y instanceof RubyBignum) {
            return RubyBignum.newBignum(context.runtime, getValue()).fdivDouble(context, (RubyBignum) y);
        }
        if (y instanceof RubyFloat) {
            return context.runtime.newFloat(((double) getValue()) / ((RubyFloat) y).value);
        }
        return coerceBin(context, sites(context).fdiv, y);
    }

    @Override
    public IRubyObject isNegative(ThreadContext context) {
        CachingCallSite op_lt_site = sites(context).basic_op_lt;
        if (op_lt_site.isBuiltin(metaClass)) {
            return asBoolean(context, getValue() < 0);
        }
        return op_lt_site.call(context, this, this, asFixnum(context, 0));
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        CachingCallSite op_gt_site = sites(context).basic_op_gt;
        if (op_gt_site.isBuiltin(metaClass)) {
            return asBoolean(context, getValue() > 0);
        }
        return op_gt_site.call(context, this, this, asFixnum(context, 0));
    }

    @Override
    protected boolean int_round_zero_p(ThreadContext context, int ndigits) {
        long bytes = 8; // sizeof(long)
        return (-0.415241 * ndigits - 0.125 > bytes);
    }

    @Override
    public IRubyObject numerator(ThreadContext context) {
        return this;
    }

    @Override
    public IRubyObject denominator(ThreadContext context) {
        return one(context.runtime);
    }

    @Override
    public RubyRational convertToRational(ThreadContext context) {
        return RubyRational.newRationalRaw(context.runtime, this, asFixnum(context, 1));
    }

    @Override
    public IRubyObject remainder(ThreadContext context, IRubyObject y) {
        return numRemainder(context, y);
    }

    // MRI: rb_int_s_isqrt, Fixnum portion
    @Override
    public IRubyObject sqrt(ThreadContext context) {
        if (isNegativeNumber(context)) throw context.runtime.newMathDomainError("Numerical argument is out of domain - isqrt");

        return asFixnum(context, floorSqrt(getValue()));
    }

    @Override
    public void appendIntoString(RubyString target) {
        if (target.getEncoding().isAsciiCompatible()) {
            // fast path for fixnum straight into ascii-compatible bytelist (modify check performed in here)
            ConvertBytes.longIntoString(target, getValue());
        } else {
            target.catWithCodeRange(ConvertBytes.longToByteListCached(getValue()), StringSupport.CR_7BIT);
        }
    }

    private static JavaSites.FixnumSites sites(ThreadContext context) {
        return context.sites.Fixnum;
    }

    @Deprecated(since = "9.1.3.0")
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.num2fix(recv.getRuntime().getCurrentContext(), other);
    }

    @Deprecated(since = "9.4.0.0")
    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }
}
