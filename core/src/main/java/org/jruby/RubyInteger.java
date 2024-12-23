/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
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

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Numeric;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;

import java.math.BigInteger;
import java.math.RoundingMode;

import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.*;
import static org.jruby.runtime.ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR;
import static org.jruby.util.Numeric.f_gcd;
import static org.jruby.util.Numeric.f_lcm;
import static org.jruby.util.Numeric.f_zero_p;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
@JRubyClass(name="Integer", parent="Numeric", overrides = {RubyFixnum.class, RubyBignum.class})
public abstract class RubyInteger extends RubyNumeric {

    private static final int BIT_SIZE = 64;
    private static final long MAX = (1L << (BIT_SIZE - 1)) - 1;

    public static RubyClass createIntegerClass(ThreadContext context, RubyClass Numeric) {
        return defineClass(context, "Integer", Numeric, NOT_ALLOCATABLE_ALLOCATOR).
                reifiedClass(RubyInteger.class).
                kindOf(new RubyModule.JavaClassKindOf(RubyInteger.class)).
                classIndex(ClassIndex.INTEGER).
                defineMethods(context, RubyInteger.class).
                tap(c-> c.singletonClass(context).undefMethods(context, "new"));
    }

    public RubyInteger(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public RubyInteger(RubyClass rubyClass) {
        super(rubyClass);
    }

    public RubyInteger(Ruby runtime, RubyClass rubyClass, boolean useObjectSpace) {
        super(runtime, rubyClass, useObjectSpace);
    }

    @Deprecated
    public RubyInteger(Ruby runtime, RubyClass rubyClass, boolean useObjectSpace, boolean canBeTainted) {
        super(runtime, rubyClass, useObjectSpace, canBeTainted);
    }

    @Override
    public RubyInteger convertToInteger() {
    	return this;
    }

    // conversion
    protected RubyFloat toFloat() {
        return RubyFloat.newFloat(metaClass.runtime, getDoubleValue());
    }

    public int signum() { return getBigIntegerValue().signum(); }

    public RubyInteger negate() { // abstract - Fixnum/Bignum do override
        ThreadContext context = metaClass.runtime.getCurrentContext();
        return sites(context).op_uminus.call(context, this, this).convertToInteger();
    }

    @Override
    public IRubyObject negative_p(ThreadContext context) {
        return asBoolean(context, isNegative(context));
    }

    @Override
    public IRubyObject positive_p(ThreadContext context) {
        return asBoolean(context, isPositive(context));
    }

    @Override
    public boolean isNegative(ThreadContext context) {
        return signum() < 0;
    }

    @Override
    public boolean isPositive(ThreadContext context) {
        return signum() > 0;
    }

    /*  =============
     *  Class Methods
     *  =============
     */

    /** rb_int_s_isqrt
     *
     */
    @JRubyMethod(meta = true)
    public static IRubyObject sqrt(ThreadContext context, IRubyObject self, IRubyObject num) {
        return num.convertToInteger().sqrt(context);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject try_convert(ThreadContext context, IRubyObject self, IRubyObject num) {
        return TypeConverter.checkIntegerType(context, num);
    }

    public abstract IRubyObject sqrt(ThreadContext context);

    // floorSqrt :: unsigned long -> unsigned long
    // Gives the exact floor of the square root of x, treated as unsigned.
    // Public domain code from http://www.codecodex.com/wiki/Calculate_an_integer_square_root
    public static final long floorSqrt(final long x) {
        if ((x & 0xfff0000000000000L) == 0L) return (long) StrictMath.sqrt(x);
        final long result = (long) StrictMath.sqrt(2.0d*(x >>> 1));  
        return result*result - x > 0L ? result - 1 : result;
    }

    // floorSqrt :: BigInteger -> BigInteger
    // Gives the exact floor of the square root of x, returning null (like Math.sqrt's NaN) if x is negative.
    //    // Public domain code from http://www.codecodex.com/wiki/Calculate_an_integer_square_root
    public static final BigInteger floorSqrt(final BigInteger x) {
        if (x == null) return null;

        final int zeroCompare = x.compareTo(BigInteger.ZERO);
        if (zeroCompare <  0) return null;
        if (zeroCompare == 0) return BigInteger.ZERO;

        int bit = Math.max(0, (x.bitLength() - 63) & 0xfffffffe); // last even numbered bit in first 64 bits
        BigInteger result = BigInteger.valueOf(floorSqrt(x.shiftRight(bit).longValue()) & 0xffffffffL);
        bit >>>= 1;
        result = result.shiftLeft(bit);
        while (bit != 0) {
            bit--;
            final BigInteger resultHigh = result.setBit(bit);
            if (resultHigh.multiply(resultHigh).compareTo(x) <= 0) result = resultHigh;
        }

        return result;
    }

    /*  ================
     *  Instance Methods
     *  ================
     */

    /** int_int_p
     *
     */
    @Override
    @JRubyMethod(name = "integer?")
    public IRubyObject integer_p(ThreadContext context) {
        return context.tru;
    }

    /** int_upto
     *
     */
    @JRubyMethod
    public IRubyObject upto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumUpto(context, ((RubyFixnum) this).value, ((RubyFixnum) to).value, block);
            } else {
                duckUpto(context, this, to, block);
            }
            return this;
        }
        // "from" is the same as "this", so we take advantage of that to reduce lambda size
        return enumeratorizeWithSize(context, this, "upto", new IRubyObject[] { to }, RubyInteger::uptoSize);
    }

    static void fixnumUpto(ThreadContext context, long from, long to, Block block) {
        // We must avoid "i++" integer overflow when (to == Long.MAX_VALUE).
        if (block.getSignature() == Signature.NO_ARGUMENTS) {
            IRubyObject nil = context.nil;
            long i;
            for (i = from; i < to; i++) {
                block.yield(context, nil);
                context.pollThreadEvents();
            }
            if (i <= to) block.yield(context, nil);
        } else {
            long i;
            for (i = from; i < to; i++) {
                block.yield(context, asFixnum(context, i));
                context.pollThreadEvents();
            }
            if (i <= to) block.yield(context, asFixnum(context, i));
        }
    }

    static void duckUpto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
        IRubyObject i = from;
        RubyFixnum one = asFixnum(context, 1);
        while (!sites(context).op_gt.call(context, i, i, to).isTrue()) {
            block.yield(context, i);
            i = sites(context).op_plus.call(context, i, i, one);
        }
    }

    /**
     * An upto size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject uptoSize(ThreadContext context, IRubyObject from, IRubyObject[] args) {
        return intervalStepSize(context, from, args[0], RubyFixnum.one(context.runtime), false);
    }

    /** int_downto
     *
     */
    @JRubyMethod
    public IRubyObject downto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumDownto(context, ((RubyFixnum) this).value, ((RubyFixnum) to).value, block);
            } else {
                duckDownto(context, this, to, block);
            }
            return this;
        }
        return enumeratorizeWithSize(context, this, "downto", new IRubyObject[] { to }, RubyInteger::downtoSize);
    }

    private static void fixnumDownto(ThreadContext context, long from, long to, Block block) {
        // We must avoid "i--" integer overflow when (to == Long.MIN_VALUE).
        if (block.getSignature() == Signature.NO_ARGUMENTS) {
            IRubyObject nil = context.nil;
            long i;
            for (i = from; i > to; i--) {
                block.yield(context, nil);
            }
            if (i >= to) block.yield(context, nil);
        } else {
            long i;
            for (i = from; i > to; i--) {
                block.yield(context, asFixnum(context, i));
            }
            if (i >= to) block.yield(context, asFixnum(context, i));
        }
    }

    private static void duckDownto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
        IRubyObject i = from;
        RubyFixnum one = RubyFixnum.one(context.runtime);
        while (true) {
            if (sites(context).op_lt.call(context, i, i, to).isTrue()) {
                break;
            }
            block.yield(context, i);
            i = sites(context).op_minus.call(context, i, i, one);
        }
    }

    /**
     * A downto size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    private static IRubyObject downtoSize(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return intervalStepSize(context, recv, args[0], asFixnum(context, -1), false);
    }

    @JRubyMethod
    public IRubyObject times(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorizeWithSize(context, this, "times", RubyInteger::timesSize);

        IRubyObject i = asFixnum(context, 0);
        RubyFixnum one = asFixnum(context, 1);
        while (((RubyInteger) i).op_lt(context, this).isTrue()) {
            block.yield(context, i);
            i = ((RubyInteger) i).op_plus(context, one);
        }
        return this;
    }

    /**
     * A times size method suitable for lambda method reference implementation of {@link SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])}
     *
     * @see SizeFn#size(ThreadContext, IRubyObject, IRubyObject[])
     */
    protected static IRubyObject timesSize(ThreadContext context, RubyInteger recv, IRubyObject[] args) {
        RubyFixnum zero = RubyFixnum.zero(context.runtime);
        if ((recv instanceof RubyFixnum && recv.getLongValue() < 0)
                || sites(context).op_lt.call(context, recv, recv, zero).isTrue()) {
            return zero;
        }

        return recv;
    }

    /** int_succ
     *
     */
    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        if (this instanceof RubyFixnum) {
            return ((RubyFixnum) this).op_plus_one(context);
        } else if (this instanceof RubyBignum) {
            return ((RubyBignum) this).op_plus(context, 1);
        } else {
            return numFuncall(context, this, sites(context).op_plus, RubyFixnum.one(context.runtime));
        }
    }

    static final byte[][] SINGLE_CHAR_BYTES;
    static {
        SINGLE_CHAR_BYTES = new byte[256][];
        for (int i = 0; i < 256; i++) {
            byte[] bytes = new byte[] { (byte) i };
            SINGLE_CHAR_BYTES[i] = bytes;
        }
    }

    static final ByteList[] SINGLE_CHAR_BYTELISTS;
    static {
        SINGLE_CHAR_BYTELISTS = new ByteList[256];
        for (int i = 0; i < 256; i++) {
            ByteList bytes = new ByteList(SINGLE_CHAR_BYTES[i], false);
            SINGLE_CHAR_BYTELISTS[i] = bytes;
            bytes.setEncoding(i < 0x80 ? USASCIIEncoding.INSTANCE : ASCIIEncoding.INSTANCE);
        }
    }

    public static ByteList singleCharByteList(final byte index) {
        return SINGLE_CHAR_BYTELISTS[index & 0xFF];
    }

    static final ByteList[] SINGLE_CHAR_UTF8_BYTELISTS;
    static {
        SINGLE_CHAR_UTF8_BYTELISTS = new ByteList[128];
        for (int i = 0; i < 128; i++) {
            ByteList bytes = new ByteList(SINGLE_CHAR_BYTES[i], false);
            SINGLE_CHAR_UTF8_BYTELISTS[i] = bytes;
            bytes.setEncoding(UTF8Encoding.INSTANCE);
        }
    }

    /**
     * Return a low ASCII single-character bytelist with UTF-8 encoding, using cached values.
     *
     * The resulting ByteList should not be modified.
     *
     * @param index the byte
     * @return a cached single-character ByteList
     */
    public static ByteList singleCharUTF8ByteList(final byte index) {
        return SINGLE_CHAR_UTF8_BYTELISTS[index & 0xFF];
    }

    /**
     * Return a single-character ByteList, possibly cached, corresponding to the given byte and encoding.
     *
     * Note this will return high ASCII non-UTF8 characters as ASCII-8BIT, rather than US-ASCII.
     *
     * @param b the byte
     * @param enc the encoding
     * @return a new single-character RubyString
     */
    public static RubyString singleCharString(Ruby runtime, byte b, RubyClass meta, Encoding enc) {
        ByteList bytes;
        if (enc == USASCIIEncoding.INSTANCE) {
            bytes = singleCharByteList(b);
        } else if (Byte.toUnsignedInt(b) < 0x80 && enc == RubyString.UTF8) {
            bytes = singleCharUTF8ByteList(b);
        } else {
            return RubyString.newStringShared(runtime, SINGLE_CHAR_BYTES[Byte.toUnsignedInt(b)], enc);
        }

        // use shared for cached bytelists
        return RubyString.newStringShared(runtime, meta, bytes);
    }

    /** int_chr
     *
     */
    @JRubyMethod(name = "chr")
    public RubyString chr(ThreadContext context) {
        long uint = toUnsignedInteger(context);

        if (uint > 0xff) {
            Encoding enc = context.runtime.getDefaultInternalEncoding();
            if (enc == null) throw rangeError(context, uint + " out of char range");
            return chrCommon(context, uint, enc);
        }

        return RubyString.newStringShared(context.runtime, SINGLE_CHAR_BYTELISTS[(int) uint]);
    }

    private long toUnsignedInteger(ThreadContext context) {
        // rb_num_to_uint
        long uintResult = numToUint(context, this);
        long uint = uintResult >>> 32;
        int ret = (int) (uintResult & 0xFFFFFFFF);
        if (ret != 0) {
            throw rangeError(context, this instanceof RubyFixnum ?
                    getLongValue() + " out of char range" : "bignum out of char range");
        }
        return uint;
    }

    public static final int NUMERR_TYPE = 1;
    public static final int NUMERR_NEGATIVE = 2;
    public static final int NUMERR_TOOLARGE = 3;

    /**
     * @param val
     * @return ""
     * @deprecated Use {@link org.jruby.RubyInteger#numToUint(ThreadContext, IRubyObject)} instead.
     */
    @Deprecated(since = "10.0")
    public static long numToUint(IRubyObject val) {
        return numToUint(((RubyBasicObject) val).getCurrentContext(), val);
    }

    /**
     * Simulate CRuby's rb_num_to_uint by returning a single long; the top 4 bytes will be the uint and the bottom
     * four bytes will be the result code. See {@link #NUMERR_TYPE}, {@link #NUMERR_NEGATIVE}, and {@link #NUMERR_TOOLARGE}.
     *
     * @param val the object to convert to a uint
     * @return the value and result code, with the top four bytes being the result code (zero if no error)
     */
    public static long numToUint(ThreadContext context, IRubyObject val) {
        if (val instanceof RubyFixnum) {
            long v = fix2long(val);
            if (v > 0xFFFFFFFFL) return NUMERR_TOOLARGE;
            if (v < 0) return NUMERR_NEGATIVE;
            return v << 32;
        }

        if (val instanceof RubyBignum bignum) {
            if (bignum.isNegative(context)) return NUMERR_NEGATIVE;
            /* long is 64bit */
            return NUMERR_TOOLARGE;
        }
        return NUMERR_TYPE;
    }

    @JRubyMethod(name = "chr")
    public RubyString chr(ThreadContext context, IRubyObject arg) {
        long uint = toUnsignedInteger(context);

        Encoding enc = arg instanceof RubyEncoding encArg ?
                encArg.getEncoding() : context.runtime.getEncodingService().findEncoding(arg.convertToString());

        return chrCommon(context, uint, enc);
    }

    private RubyString chrCommon(ThreadContext context, long value, Encoding enc) {
        if (value > 0xFFFFFFFFL) throw rangeError(context, this + " out of char range");

        int c = (int) value;
        if (enc == null) enc = ASCIIEncoding.INSTANCE;
        return EncodingUtils.encUintChr(context, c, enc);
    }

    /** int_ord
     *
     */
    @JRubyMethod(name = "ord")
    public IRubyObject ord(ThreadContext context) {
        return this;
    }


    @Deprecated(since = "10.0")
    public IRubyObject to_i() {
        return to_i(getCurrentContext());
    }

    // MRI: int_to_i
    @JRubyMethod(name = {"to_i", "to_int"})
    public IRubyObject to_i(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name = "ceil")
    public IRubyObject ceil(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "ceil")
    public abstract IRubyObject ceil(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "floor")
    public abstract IRubyObject floor(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "truncate")
    public IRubyObject truncate(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "truncate")
    public abstract IRubyObject truncate(ThreadContext context, IRubyObject arg);

    @Override
    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context) {
        return this;
    }

    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject _digits) {
        return round(context, _digits, context.nil);
    }

    @JRubyMethod(name = "round")
    public IRubyObject round(ThreadContext context, IRubyObject digits, IRubyObject _opts) {
        IRubyObject opts = ArgsUtil.getOptionsArg(context, _opts); // options (only "half" supported right now)
        int ndigits = num2int(digits);
        RoundingMode roundingMode = getRoundingMode(context, opts);
        if (ndigits >= 0) return this;

        return roundShared(context, ndigits, roundingMode);
    }

    public IRubyObject round(ThreadContext context, int ndigits) {
        return roundShared(context, ndigits, RoundingMode.HALF_UP);
    }

    /*
     * MRI: rb_int_round
     */
    public RubyNumeric roundShared(ThreadContext context, int ndigits, RoundingMode roundingMode) {
        if (int_round_zero_p(context, ndigits)) return asFixnum(context, 0);

        RubyNumeric f = Numeric.int_pow(context, 10, -ndigits);
        if (this instanceof RubyFixnum && f instanceof RubyFixnum) {
            long x = fix2long(this), y = fix2long(f);
            boolean neg = x < 0;
            if (neg) x = -x;
            x = doRound(context, roundingMode, x, y);
            if (neg) x = -x;
            return asFixnum(context, x);
        }

        if (f instanceof RubyFloat) return asFixnum(context, 0); // then int_pow overflow

        RubyNumeric h = (RubyNumeric) f.idiv(context, 2);
        RubyNumeric r = (RubyNumeric) this.op_mod(context, f);
        RubyNumeric n = (RubyNumeric) this.op_minus(context, r);
        r = (RubyNumeric) r.op_cmp(context, h);

        if (r.isPositive(context) ||
                (r.isZero(context) && doRoundCheck(context, roundingMode, this, n, f))) {
            n = (RubyNumeric) n.op_plus(context, f);
        }
        return n;
    }

    private static long doRound(ThreadContext context, RoundingMode roundingMode, long n, long f) {
        switch (roundingMode) {
            case HALF_UP:
                return int_round_half_up(n, f);
            case HALF_DOWN:
                return int_round_half_down(n, f);
            case HALF_EVEN:
                return int_round_half_even(n, f);
        }
        throw argumentError(context, "invalid rounding mode: " + roundingMode);
    }

    private static boolean doRoundCheck(ThreadContext context, RoundingMode roundingMode, RubyInteger num, RubyNumeric n, IRubyObject f) {
        switch (roundingMode) {
            case HALF_UP:
                return int_half_p_half_up(context, num, n, f);
            case HALF_DOWN:
                return int_half_p_half_down(context, num, n, f);
            case HALF_EVEN:
                return int_half_p_half_even(context, num, n, f);
        }
        throw argumentError(context, "invalid rounding mode: " + roundingMode);
    }

    protected boolean int_round_zero_p(ThreadContext context, int ndigits) {
        long bytes = numericToLong(context, sites(context).size.call(context, this, this));
        return (-0.415241 * ndigits - 0.125 > bytes);
    }

    protected static long int_round_half_even(long x, long y) {
        long z = +(x + y / 2) / y;
        if ((z * y - x) * 2 == y) z &= ~1;
        return z * y;
    }

    protected static long int_round_half_up(long x, long y) {
        return (x + y / 2) / y * y;
    }

    protected static long int_round_half_down(long x, long y) {
        return (x + y / 2 - 1) / y * y;
    }

    protected static boolean int_half_p_half_even(ThreadContext context, RubyInteger num, RubyNumeric n, IRubyObject f) {
        return n.div(context, f).convertToInteger().odd_p(context).isTrue();
    }

    protected static boolean int_half_p_half_up(ThreadContext context, RubyInteger num, RubyNumeric n, IRubyObject f) {
        return num.isPositive(context);
    }

    protected static boolean int_half_p_half_down(ThreadContext context, RubyInteger num, RubyNumeric n, IRubyObject f) {
        return num.isNegative(context);
    }

    /** integer_to_r
     *
     */
    @JRubyMethod(name = "to_r")
    public IRubyObject to_r(ThreadContext context) {
        return RubyRational.newRationalCanonicalize(context, this);
    }

    /** integer_rationalize
     *
     */
    @JRubyMethod(name = "rationalize", optional = 1, checkArity = false)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return to_r(context);
    }


    @JRubyMethod(name = "odd?")
    public RubyBoolean odd_p(ThreadContext context) {
        return (op_mod_two(context, this) != 0) ? context.tru : context.fals;
    }

    @JRubyMethod(name = "even?")
    public RubyBoolean even_p(ThreadContext context) {
        return (op_mod_two(context, this) == 0) ? context.tru : context.fals;
    }

    private static long op_mod_two(ThreadContext context, RubyInteger self) {
        return ((RubyInteger) sites(context).op_mod.call(context, self, self, RubyFixnum.two(context.runtime))).getLongValue();
    }

    @JRubyMethod(name = "allbits?")
    public IRubyObject allbits_p(ThreadContext context, IRubyObject other) {
        IRubyObject mask = checkToInteger(context, other);
        return ((RubyInteger) op_and(context, mask)).op_equal(context, mask);
    }

    @JRubyMethod(name = "anybits?")
    public IRubyObject anybits_p(ThreadContext context, IRubyObject other) {
        IRubyObject mask = checkToInteger(context, other);
        return ((RubyInteger) op_and(context, mask)).isZero(context) ? context.fals : context.tru;
    }

    @JRubyMethod(name = "nobits?")
    public IRubyObject nobits_p(ThreadContext context, IRubyObject other) {
        IRubyObject mask = checkToInteger(context, other);
        return ((RubyInteger) op_and(context, mask)).zero_p(context);
    }

    @JRubyMethod(name = "pred")
    public IRubyObject pred(ThreadContext context) {
        return numFuncall(context, this, sites(context).op_minus, RubyFixnum.one(context.runtime));
    }

    /** rb_gcd
     *
     */
    @JRubyMethod(name = "gcd")
    public IRubyObject gcd(ThreadContext context, IRubyObject other) {
        return f_gcd(context, this, RubyInteger.intValue(context, other));
    }

    // MRI: rb_int_fdiv_double and rb_int_fdiv in one
    @Override
    @JRubyMethod(name = "fdiv")
    public IRubyObject fdiv(ThreadContext context, IRubyObject y) {
        RubyInteger x = this;
        if (y instanceof RubyInteger && !f_zero_p(context, y)) {
            IRubyObject gcd = gcd(context, y);
            if (!f_zero_p(context, gcd)) {
                x = (RubyInteger)x.idiv(context, gcd);
                y = ((RubyInteger)y).idiv(context, gcd);
            }
        }
        return x.fdivDouble(context, y);
    }

    public abstract IRubyObject fdivDouble(ThreadContext context, IRubyObject y);

    /** rb_lcm
     *
     */
    @JRubyMethod(name = "lcm")
    public IRubyObject lcm(ThreadContext context, IRubyObject other) {
        return f_lcm(context, this, RubyInteger.intValue(context, other));
    }

    /** rb_gcdlcm
     *
     */
    @JRubyMethod(name = "gcdlcm")
    public IRubyObject gcdlcm(ThreadContext context, IRubyObject other) {
        final RubyInteger otherInt = RubyInteger.intValue(context, other);
        return newArray(context, f_gcd(context, this, otherInt), f_lcm(context, this, otherInt));
    }

    static RubyInteger intValue(ThreadContext context, IRubyObject num) {
        RubyInteger i = RubyInteger.toInteger(context, num);
        if (i == null) throw typeError(context, "not an integer");
        return i;
    }

    static RubyInteger toInteger(ThreadContext context, IRubyObject num) {
        if (num instanceof RubyInteger) return (RubyInteger) num;
        if (num instanceof RubyNumeric && !integer_p_site(context).call(context, num, num).isTrue()) { // num.integer?
            return null;
        }
        if (num instanceof RubyString) return null; // do not want String#to_i
        return (RubyInteger) num.checkCallMethod(context, sites(context).to_i_checked);
    }

    @JRubyMethod(name = "digits")
    public RubyArray digits(ThreadContext context) {
        return digits(context, asFixnum(context, 10));
    }

    @JRubyMethod(name = "digits")
    public abstract RubyArray digits(ThreadContext context, IRubyObject base);

    @Override
    @JRubyMethod(name = "numerator")
    public IRubyObject numerator(ThreadContext context) {
        return this;
    }

    @Override
    @JRubyMethod(name = "denominator")
    public IRubyObject denominator(ThreadContext context) {
        return RubyFixnum.one(context.runtime);
    }

    @Override
    @JRubyMethod(name = {"to_s", "inspect"})
    public abstract RubyString to_s(ThreadContext context);


    @Deprecated(since = "10.0")
    public RubyString to_s(IRubyObject x) {
        return to_s(getCurrentContext(), x);
    }

    @JRubyMethod(name = "to_s")
    public RubyString to_s(ThreadContext context, IRubyObject x) {
        throw context.runtime.newRuntimeError("integer type missing native to_s(ThreadContext, IRubyObject) impl");
    }

    @JRubyMethod(name = "-@")
    public abstract IRubyObject op_uminus(ThreadContext context);

    @JRubyMethod(name = "+")
    public abstract IRubyObject op_plus(ThreadContext context, IRubyObject other);

    public IRubyObject op_plus(ThreadContext context, long other) {
        return op_plus(context, asFixnum(context, other));
    }

    @JRubyMethod(name = "-")
    public abstract IRubyObject op_minus(ThreadContext context, IRubyObject other);

    public IRubyObject op_minus(ThreadContext context, long other) {
        return op_minus(context, asFixnum(context, other));
    }

    @JRubyMethod(name = "*")
    public abstract IRubyObject op_mul(ThreadContext context, IRubyObject other);

    public IRubyObject op_mul(ThreadContext context, long other) {
        return op_mul(context, asFixnum(context, other));
    }

    // MRI: rb_int_idiv, polymorphism handles fixnum vs bignum
    @JRubyMethod(name = "div")
    @Override
    public abstract IRubyObject idiv(ThreadContext context, IRubyObject other);

    public final IRubyObject div_div(ThreadContext context, IRubyObject other) {
        return div(context, other);
    }

    @JRubyMethod(name = "/")
    public abstract IRubyObject op_div(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = {"%", "modulo"})
    public abstract IRubyObject op_mod(ThreadContext context, IRubyObject other);

    public IRubyObject op_mod(ThreadContext context, long other) {
        return op_mod(context, asFixnum(context, other));
    }

    @JRubyMethod(name = "**")
    public abstract IRubyObject op_pow(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "pow")
    public IRubyObject pow(ThreadContext context, IRubyObject other) {
        return sites(context).op_pow.call(context, this, this, other);
    }

    private static final long HALF_LONG_MSB = 0x80000000L;

    // MRI: rb_int_powm
    @JRubyMethod(name = "pow")
    public IRubyObject pow(ThreadContext context, IRubyObject b, IRubyObject m) {
        boolean negaFlg = false;
        RubyInteger base = castAsInteger(context, b, "Integer#pow() 2nd argument not allowed unless a 1st argument is integer");
        if (base.isNegative(context)) {
            throw rangeError(context, "Integer#pow() 1st argument cannot be negative when 2nd argument specified");
        }

        RubyInteger pow = castAsInteger(context, m, "Integer#pow() 2nd argument not allowed unless all arguments are integers");

        if (pow.isNegative(context)) {
            pow = pow.negate();
            negaFlg = true;
        }

        if (!pow.isPositive(context)) throw context.runtime.newZeroDivisionError();

        if (pow instanceof RubyFixnum fixpow) {
            long mm = fixpow.value;
            if (mm == 1) return asFixnum(context, 0);
            RubyFixnum modulo = (RubyFixnum) modulo(context, fixpow);
            return mm <= HALF_LONG_MSB ?
                modulo.intPowTmp1(context, base, mm, negaFlg) : modulo.intPowTmp2(context, base, mm, negaFlg);
        }
        if (pow instanceof RubyBignum bigpow) {
            return ((RubyBignum) m).value == BigInteger.ONE ?
                    asFixnum(context, 0) : ((RubyInteger) modulo(context, m)).intPowTmp3(context, base, bigpow, negaFlg);
        }
        // not reached
        throw new AssertionError("BUG: unexpected type " + m.getType());
    }

    protected IRubyObject intPowTmp3(ThreadContext context, RubyInteger y, RubyBignum m, boolean negaFlg) {
        BigInteger xn, yn, mn, zn;

        xn = getBigIntegerValue();
        yn = y.getBigIntegerValue();
        mn = m.getBigIntegerValue();

        zn = xn.modPow(yn, mn);
        if (negaFlg & zn.signum() == 1) {
            zn = zn.negate();
        }
        return RubyBignum.bignorm(context.runtime, zn);
    }

    @JRubyMethod(name = "abs")
    public abstract IRubyObject abs(ThreadContext context);

    @JRubyMethod(name = "magnitude")
    @Override
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    @JRubyMethod(name = {"==", "==="})
    @Override
    public abstract IRubyObject op_equal(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "<=>")
    @Override
    public abstract IRubyObject op_cmp(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "~")
    public abstract IRubyObject op_neg(ThreadContext context);

    @JRubyMethod(name = "&")
    public abstract IRubyObject op_and(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "|")
    public abstract IRubyObject op_or(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "^")
    public abstract IRubyObject op_xor(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject index) {
        if (index instanceof RubyRange) {
            RubyRange range = (RubyRange) index;
            IRubyObject beg = range.begin(context);
            IRubyObject end = range.end(context);
            boolean isExclusive = range.isExcludeEnd();

            if (!end.isNil()) end = end.convertToInteger();

            if (beg.isNil()) {
                if (!negativeInt(context, end)) {
                    if (!isExclusive) end = ((RubyInteger) end).op_plus(context, asFixnum(context, 1));

                    RubyInteger mask = generateMask(context, end);
                    if (((RubyInteger) op_and(context, mask)).isZero(context)) return asFixnum(context, 0);

                    throw argumentError(context, "The beginless range for Integer#[] results in infinity");
                } else {
                    return asFixnum(context, 0);
                }
            }
            beg = beg.convertToInteger();
            IRubyObject num = op_rshift(context, beg);
            long cmp = compareIndexes(context, beg, end);
            if (!end.isNil() && cmp < 0) {
                IRubyObject length = ((RubyInteger) end).op_minus(context, beg);
                if (!isExclusive) {
                    length = ((RubyInteger) length).op_plus(context, asFixnum(context, 1));
                }
                RubyInteger mask = generateMask(context, length);
                num = (((RubyInteger) num).op_and(context, mask));
                return num;
            } else if (cmp == 0) {
                if (isExclusive) return asFixnum(context, 0);
                index = beg;
            } else {
                return num;
            }
        }

        return op_aref_subclass(context, index);
    }

    private static long compareIndexes(ThreadContext context, IRubyObject beg, IRubyObject end) {
        IRubyObject r = beg.callMethod(context, "<=>", end);

        if (r.isNil()) return MAX;

        JavaSites.IntegerSites sites = sites(context);
        return RubyComparable.cmpint(context, sites.op_gt, sites.op_lt, r, beg, end);
    }

    protected abstract IRubyObject op_aref_subclass(ThreadContext context, IRubyObject index);

    @JRubyMethod(name = "[]")
    public IRubyObject op_aref(ThreadContext context, IRubyObject index, IRubyObject length) {
        IRubyObject num = op_rshift(context, index);
        IRubyObject mask = generateMask(context, length);
        return ((RubyInteger) num).op_and(context, mask);
    }

    RubyInteger generateMask(ThreadContext context, IRubyObject length) {
        RubyFixnum one = asFixnum(context, 1);
        return (RubyInteger) ((RubyInteger) one.op_lshift(context, length)).op_minus(context, one);
    }

    @JRubyMethod(name = "<<")
    public abstract IRubyObject op_lshift(ThreadContext context, IRubyObject other);

    public RubyInteger op_lshift(ThreadContext context, long other) {
        return (RubyInteger) op_lshift(context, asFixnum(context, other));
    }

    @JRubyMethod(name = ">>")
    public abstract IRubyObject op_rshift(ThreadContext context, IRubyObject other);

    public RubyInteger op_rshift(ThreadContext context, long other) {
        return (RubyInteger) op_rshift(context, asFixnum(context, other));
    }

    @JRubyMethod(name = "to_f")
    public abstract IRubyObject to_f(ThreadContext context);

    @JRubyMethod(name = "size")
    public abstract IRubyObject size(ThreadContext context);

    @JRubyMethod(name = "zero?")
    public abstract IRubyObject zero_p(ThreadContext context);

    @JRubyMethod(name = "bit_length")
    public abstract IRubyObject bit_length(ThreadContext context);

    boolean isOne() {
        return getBigIntegerValue().equals(BigInteger.ONE);
    }

    @JRubyMethod(name = ">")
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_gt(context, this, other);
    }

    @JRubyMethod(name = "<")
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_lt(context, this, other);
    }

    @JRubyMethod(name = ">=")
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_ge(context, this, other);
    }

    @JRubyMethod(name = "<=")
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_le(context, this, other);
    }

    @JRubyMethod(name = "remainder")
    public IRubyObject remainder(ThreadContext context, IRubyObject dividend) {
        return context.nil;
    }

    @JRubyMethod(name = "divmod")
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_uminus() {
        return op_uminus(getCurrentContext());
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_neg() {
        return to_f(getCurrentContext());
    }

    @Deprecated(since = "10.0")
    public IRubyObject op_aref(IRubyObject other) {
        return op_aref(getCurrentContext(), other);
    }

    @Deprecated // no longer used
    public IRubyObject op_lshift(IRubyObject other) {
        return op_lshift(getCurrentContext(), other);
    }

    @Deprecated // no longer used
    public IRubyObject op_rshift(IRubyObject other) {
        return op_rshift(getCurrentContext(), other);
    }

    @Deprecated(since = "10.0")
    public IRubyObject to_f() {
        return to_f(getCurrentContext());
    }

    @Deprecated(since = "10.0")
    public IRubyObject size() {
        return size(getCurrentContext());
    }

    private static CallSite integer_p_site(ThreadContext context) {
        return context.sites.Numeric.integer;
    }

    private static JavaSites.IntegerSites sites(ThreadContext context) {
        return context.sites.Integer;
    }

    /** rb_int_induced_from
     *
     */
    @Deprecated
    public static IRubyObject induced_from(ThreadContext context, IRubyObject recv, IRubyObject other) {
        if (other instanceof RubyFixnum || other instanceof RubyBignum) return other;

        if (!(other instanceof RubyFloat) && !(other instanceof RubyRational)) {
            throw typeError(context, "failed to convert ", other, " into Integer");
        }

        return other.callMethod(context, "to_i");
    }

    @Deprecated
    public IRubyObject round() {
        return this;
    }

    @Deprecated
    public IRubyObject ceil(){
        return this;
    }

    @Deprecated
    public IRubyObject floor(){
        return this;
    }

    @Deprecated
    public IRubyObject truncate(){
        return this;
    }

    @Deprecated
    public final IRubyObject op_idiv(ThreadContext context, IRubyObject arg) {
        return div(context, arg);
    }
}
