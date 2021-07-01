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
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
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
import static org.jruby.util.Numeric.f_gcd;
import static org.jruby.util.Numeric.f_lcm;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
@JRubyClass(name="Integer", parent="Numeric", overrides = {RubyFixnum.class, RubyBignum.class})
public abstract class RubyInteger extends RubyNumeric {

    public static RubyClass createIntegerClass(Ruby runtime) {
        RubyClass integer = runtime.defineClass("Integer", runtime.getNumeric(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

        integer.setClassIndex(ClassIndex.INTEGER);
        integer.setReifiedClass(RubyInteger.class);

        integer.kindOf = new RubyModule.JavaClassKindOf(RubyInteger.class);

        integer.getSingletonClass().undefineMethod("new");

        integer.defineAnnotatedMethods(RubyInteger.class);

        return integer;
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
    public IRubyObject isNegative(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isNegative());
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isPositive());
    }

    @Override
    public boolean isNegative() {
        return signum() < 0;
    }

    @Override
    public boolean isPositive() {
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
    public IRubyObject integer_p() {
        return metaClass.runtime.getTrue();
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
            }
            if (i <= to) {
                block.yield(context, nil);
            }
        } else {
            Ruby runtime = context.runtime;
            long i;
            for (i = from; i < to; i++) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i <= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
        }
    }

    static void duckUpto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject i = from;
        RubyFixnum one = RubyFixnum.one(runtime);
        while (true) {
            if (sites(context).op_gt.call(context, i, i, to).isTrue()) {
                break;
            }
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
            if (i >= to) {
                block.yield(context, nil);
            }
        } else {
            Ruby runtime = context.runtime;
            long i;
            for (i = from; i > to; i--) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
            if (i >= to) {
                block.yield(context, RubyFixnum.newFixnum(runtime, i));
            }
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
        return intervalStepSize(context, recv, args[0], RubyFixnum.newFixnum(context.runtime, -1), false);
    }

    @JRubyMethod
    public IRubyObject times(ThreadContext context, Block block) {
        if (block.isGiven()) {
            Ruby runtime = context.runtime;
            IRubyObject i = RubyFixnum.zero(runtime);
            RubyFixnum one = RubyFixnum.one(runtime);
            while (true) {
                if (!sites(context).op_lt.call(context, i, i, this).isTrue()) {
                    break;
                }
                block.yield(context, i);
                i = sites(context).op_plus.call(context, i, i, one);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "times", RubyInteger::timesSize);
        }
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

    static final ByteList[] SINGLE_CHAR_BYTELISTS;
    static {
        SINGLE_CHAR_BYTELISTS = new ByteList[256];
        for (int i = 0; i < 256; i++) {
            ByteList bytes = new ByteList(new byte[] { (byte) i }, false);
            SINGLE_CHAR_BYTELISTS[i] = bytes;
            bytes.setEncoding(i < 0x80 ? USASCIIEncoding.INSTANCE : ASCIIEncoding.INSTANCE);
        }
    }
    @Deprecated
    public static final ByteList[] SINGLE_CHAR_BYTELISTS19 = SINGLE_CHAR_BYTELISTS;

    public static ByteList singleCharByteList(final byte index) {
        return SINGLE_CHAR_BYTELISTS[index & 0xFF];
    }

    static final ByteList[] SINGLE_CHAR_UTF8_BYTELISTS;
    static {
        SINGLE_CHAR_UTF8_BYTELISTS = new ByteList[128];
        for (int i = 0; i < 128; i++) {
            ByteList bytes = new ByteList(new byte[] { (byte) i }, false);
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
        } else if ((b & 0xFF) < 0x80 && enc == RubyString.UTF8) {
            bytes = singleCharUTF8ByteList(b);
        } else {
            return new RubyString(runtime, meta, new ByteList(new byte[]{b}, enc));
        }

        // use shared for cached bytelists
        return RubyString.newStringShared(runtime, meta, bytes);
    }

    /** int_chr
     *
     */
    @JRubyMethod(name = "chr")
    public RubyString chr(ThreadContext context) {
        Ruby runtime = context.runtime;

        // rb_num_to_uint
        long i = getLongValue() & 0xFFFFFFFFL;
        int c = (int) i;

        Encoding enc;

        if (i > 0xff) {
            enc = runtime.getDefaultInternalEncoding();
            if (enc == null) {
                throw runtime.newRangeError(toString() + " out of char range");
            }
            return chrCommon(context, c, enc);
        }

        return RubyString.newStringShared(runtime, SINGLE_CHAR_BYTELISTS[c]);
    }

    @Deprecated
    public final RubyString chr19(ThreadContext context) {
        return chr(context);
    }

    @JRubyMethod(name = "chr")
    public RubyString chr(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;

        // rb_num_to_uint
        long i = getLongValue() & 0xFFFFFFFFL;

        Encoding enc;
        if (arg instanceof RubyEncoding) {
            enc = ((RubyEncoding)arg).getEncoding();
        } else {
            enc =  arg.convertToString().toEncoding(runtime);
        }
        return chrCommon(context, i, enc);
    }

    @Deprecated
    public RubyString chr19(ThreadContext context, IRubyObject arg) {
        return chr(context, arg);
    }

    private RubyString chrCommon(ThreadContext context, long value, Encoding enc) {
        if (value > 0xFFFFFFFFL) {
            throw context.runtime.newRangeError(this + " out of char range");
        }
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

    /** int_to_i
     *
     */
    @JRubyMethod(name = {"to_i", "to_int"})
    public IRubyObject to_i() {
        return this;
    }

    @JRubyMethod(name = "ceil")
    public IRubyObject ceil(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "ceil", required = 1)
    public abstract IRubyObject ceil(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "floor")
    public IRubyObject floor(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "floor", required = 1)
    public abstract IRubyObject floor(ThreadContext context, IRubyObject arg);

    @JRubyMethod(name = "truncate")
    public IRubyObject truncate(ThreadContext context){
        return this;
    }

    @JRubyMethod(name = "truncate", required = 1)
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
        Ruby runtime = context.runtime;

        // options (only "half" right now)
        IRubyObject opts = ArgsUtil.getOptionsArg(runtime, _opts);
        int ndigits = num2int(digits);

        RoundingMode roundingMode = getRoundingMode(context, opts);
        if (ndigits >= 0) {
            return this;
        }

        return roundShared(context, ndigits, roundingMode);
    }

    public IRubyObject round(ThreadContext context, int ndigits) {
        return roundShared(context, ndigits, RoundingMode.HALF_UP);
    }

    /*
     * MRI: rb_int_round
     */
    public RubyNumeric roundShared(ThreadContext context, int ndigits, RoundingMode roundingMode) {
        Ruby runtime = context.runtime;

        RubyNumeric f, h, n, r;

        if (int_round_zero_p(context, ndigits)) {
            return RubyFixnum.zero(runtime);
        }

        f = Numeric.int_pow(context, 10, -ndigits);
        if (this instanceof RubyFixnum && f instanceof RubyFixnum) {
            long x = fix2long(this), y = fix2long(f);
            boolean neg = x < 0;
            if (neg) x = -x;
            x = doRound(context, roundingMode, x, y);
            if (neg) x = -x;
            return RubyFixnum.newFixnum(runtime, x);
        }
        if (f instanceof RubyFloat) {
	        /* then int_pow overflow */
            return RubyFixnum.zero(runtime);
        }
        h = (RubyNumeric) f.idiv(context, 2);
        r = (RubyNumeric) this.op_mod(context, f);
        n = (RubyNumeric) this.op_minus(context, r);
        r = (RubyNumeric) r.op_cmp(context, h);
        if (r.isPositive(context).isTrue() ||
                (r.isZero() && doRoundCheck(context, roundingMode, this, n, f))) {
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
        throw context.runtime.newArgumentError("invalid rounding mode: " + roundingMode);
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
        throw context.runtime.newArgumentError("invalid rounding mode: " + roundingMode);
    }

    protected boolean int_round_zero_p(ThreadContext context, int ndigits) {
        long bytes = num2long(sites(context).size.call(context, this, this));
        return (-0.415241 * ndigits - 0.125 > bytes);
    }

    protected static long int_round_half_even(long x, long y)
    {
        long z = +(x + y / 2) / y;
        if ((z * y - x) * 2 == y) {
            z &= ~1;
        }
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
        return num.isPositive(context).isTrue();
    }

    protected static boolean int_half_p_half_down(ThreadContext context, RubyInteger num, RubyNumeric n, IRubyObject f) {
        return num.isNegative(context).isTrue();
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
    @JRubyMethod(name = "rationalize", optional = 1)
    public IRubyObject rationalize(ThreadContext context, IRubyObject[] args) {
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
        IRubyObject mask = TypeConverter.checkIntegerType(context, other);
        return ((RubyInteger) op_and(context, mask)).op_equal(context, mask);
    }

    @JRubyMethod(name = "anybits?")
    public IRubyObject anybits_p(ThreadContext context, IRubyObject other) {
        IRubyObject mask = TypeConverter.checkIntegerType(context, other);
        return ((RubyInteger) op_and(context, mask)).zero_p(context).isTrue() ? context.fals : context.tru;
    }

    @JRubyMethod(name = "nobits?")
    public IRubyObject nobits_p(ThreadContext context, IRubyObject other) {
        IRubyObject mask = TypeConverter.checkIntegerType(context, other);
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
        return fdivDouble(context, y);
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
        return context.runtime.newArray(f_gcd(context, this, otherInt), f_lcm(context, this, otherInt));
    }

    static RubyInteger intValue(ThreadContext context, IRubyObject num) {
        RubyInteger i;
        if (( i = RubyInteger.toInteger(context, num) ) == null) {
            throw context.runtime.newTypeError("not an integer");
        }
        return i;
    }

    static RubyInteger toInteger(ThreadContext context, IRubyObject num) {
        if (num instanceof RubyInteger) return (RubyInteger) num;
        if (num instanceof RubyNumeric && !integer_p(context).call(context, num, num).isTrue()) { // num.integer?
            return null;
        }
        if (num instanceof RubyString) return null; // do not want String#to_i
        return (RubyInteger) num.checkCallMethod(context, sites(context).to_i_checked);
    }

    @JRubyMethod(name = "digits")
    public RubyArray digits(ThreadContext context) {
        return digits(context, RubyFixnum.newFixnum(context.runtime, 10));
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
    public abstract RubyString to_s();

    @JRubyMethod(name = "to_s")
    public abstract RubyString to_s(IRubyObject x);

    @JRubyMethod(name = "-@")
    public abstract IRubyObject op_uminus(ThreadContext context);

    @JRubyMethod(name = "+")
    public abstract IRubyObject op_plus(ThreadContext context, IRubyObject other);

    public IRubyObject op_plus(ThreadContext context, long other) {
        return op_plus(context, RubyFixnum.newFixnum(context.runtime, other));
    }

    @JRubyMethod(name = "-")
    public abstract IRubyObject op_minus(ThreadContext context, IRubyObject other);

    public IRubyObject op_minus(ThreadContext context, long other) {
        return op_minus(context, RubyFixnum.newFixnum(context.runtime, other));
    }

    @JRubyMethod(name = "*")
    public abstract IRubyObject op_mul(ThreadContext context, IRubyObject other);

    public IRubyObject op_mul(ThreadContext context, long other) {
        return op_mul(context, RubyFixnum.newFixnum(context.runtime, other));
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
        return op_mod(context, RubyFixnum.newFixnum(context.runtime, other));
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
        // a == this
        Ruby runtime = context.runtime;

        boolean negaFlg = false;
        if (!(b instanceof RubyInteger)) {
            throw runtime.newTypeError("Integer#pow() 2nd argument not allowed unless a 1st argument is integer");
        }
        if (((RubyInteger) b).isNegative()) {
            throw runtime.newRangeError("Integer#pow() 1st argument cannot be negative when 2nd argument specified");
        }
        if (!(m instanceof RubyInteger)) {
            throw runtime.newTypeError("Integer#pow() 2nd argument not allowed unless all arguments are integers");
        }

        if (((RubyInteger) m).isNegative()) {
            m = ((RubyInteger) m).negate();
            negaFlg = true;
        }

        if (!((RubyInteger) m).isPositive()) throw runtime.newZeroDivisionError();

        if (m instanceof RubyFixnum) {
            long mm = ((RubyFixnum) m).value;
            RubyFixnum modulo = (RubyFixnum) modulo(context, m);
            if (mm <= HALF_LONG_MSB) {
                return modulo.intPowTmp1(context, (RubyInteger) b, mm, negaFlg);
            } else {
                return modulo.intPowTmp2(context, (RubyInteger) b, mm, negaFlg);
            }
        }
        if (m instanceof RubyBignum) {
            return ((RubyInteger) modulo(context, m)).intPowTmp3(context, (RubyInteger) b, (RubyBignum) m, negaFlg);
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
    public abstract IRubyObject op_aref(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "<<")
    public abstract IRubyObject op_lshift(ThreadContext context, IRubyObject other);

    public RubyInteger op_lshift(ThreadContext context, long other) {
        return (RubyInteger) op_lshift(context, RubyFixnum.newFixnum(context.runtime, other));
    }

    @JRubyMethod(name = ">>")
    public abstract IRubyObject op_rshift(ThreadContext context, IRubyObject other);

    public RubyInteger op_rshift(ThreadContext context, long other) {
        return (RubyInteger) op_rshift(context, RubyFixnum.newFixnum(context.runtime, other));
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

    public IRubyObject op_uminus() {
        return op_uminus(getRuntime().getCurrentContext());
    }

    public IRubyObject op_neg() {
        return to_f(getRuntime().getCurrentContext());
    }

    public IRubyObject op_aref(IRubyObject other) {
        return op_aref(getRuntime().getCurrentContext(), other);
    }

    @Deprecated // no longer used
    public IRubyObject op_lshift(IRubyObject other) {
        return op_lshift(getRuntime().getCurrentContext(), other);
    }

    @Deprecated // no longer used
    public IRubyObject op_rshift(IRubyObject other) {
        return op_rshift(getRuntime().getCurrentContext(), other);
    }

    public IRubyObject to_f() {
        return to_f(getRuntime().getCurrentContext());
    }

    public IRubyObject size() {
        return size(getRuntime().getCurrentContext());
    }

    private static CallSite integer_p(ThreadContext context) {
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
        if (other instanceof RubyFixnum || other instanceof RubyBignum) {
            return other;
        } else if (other instanceof RubyFloat || other instanceof RubyRational) {
            return other.callMethod(context, "to_i");
        } else {
            throw context.runtime.newTypeError(
                    "failed to convert " + other.getMetaClass().getName() + " into Integer");
        }
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
    public final IRubyObject round19() {
        return round(getRuntime().getCurrentContext());
    }

    @Deprecated
    public final IRubyObject round19(ThreadContext context, IRubyObject arg) {
        return round(context, arg);
    }

    @Deprecated
    public final IRubyObject op_idiv(ThreadContext context, IRubyObject arg) {
        return div(context, arg);
    }
}
