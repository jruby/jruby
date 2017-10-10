/*
 **** BEGIN LICENSE BLOCK *****
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
import org.jruby.util.io.EncodingUtils;

import java.math.RoundingMode;

import static org.jruby.RubyEnumerator.enumeratorizeWithSize;
import static org.jruby.RubyEnumerator.SizeFn;
import static org.jruby.util.Numeric.*;

/** Implementation of the Integer class.
 *
 * @author  jpetersen
 */
@JRubyClass(name="Integer", parent="Numeric")
public abstract class RubyInteger extends RubyNumeric {

    public static RubyClass createIntegerClass(Ruby runtime) {
        RubyClass integer = runtime.defineClass("Integer", runtime.getNumeric(),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setInteger(integer);

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
        return RubyFloat.newFloat(getRuntime(), getDoubleValue());
    }

    public int signum() { return getBigIntegerValue().signum(); }

    public abstract RubyInteger negate() ;

    @Override
    public IRubyObject isNegative(ThreadContext context) {
        return context.runtime.newBoolean(signum() < 0);
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        return context.runtime.newBoolean(signum() > 0);
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
        return getRuntime().getTrue();
    }

    /** int_upto
     *
     */
    @JRubyMethod
    public IRubyObject upto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumUpto(context, ((RubyFixnum)this).getLongValue(), ((RubyFixnum)to).getLongValue(), block);
            } else {
                duckUpto(context, this, to, block);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "upto", new IRubyObject[] { to }, uptoSize(context, this, to));
        }
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

    private static void duckUpto(ThreadContext context, IRubyObject from, IRubyObject to, Block block) {
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

    private static SizeFn uptoSize(final ThreadContext context, final IRubyObject from, final IRubyObject to) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return intervalStepSize(context, from, to, RubyFixnum.one(context.runtime), false);
            }
        };
    }

    /** int_downto
     *
     */
    // TODO: Make callCoerced work in block context...then fix downto, step, and upto.
    @JRubyMethod
    public IRubyObject downto(ThreadContext context, IRubyObject to, Block block) {
        if (block.isGiven()) {
            if (this instanceof RubyFixnum && to instanceof RubyFixnum) {
                fixnumDownto(context, ((RubyFixnum)this).getLongValue(), ((RubyFixnum)to).getLongValue(), block);
            } else {
                duckDownto(context, this, to, block);
            }
            return this;
        } else {
            return enumeratorizeWithSize(context, this, "downto", new IRubyObject[] { to }, downToSize(context, this, to));
        }
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

    private static SizeFn downToSize(final ThreadContext context, final IRubyObject from, final IRubyObject to) {
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                return intervalStepSize(context, from, to, RubyFixnum.newFixnum(context.runtime, -1), false);
            }
        };
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
            return enumeratorizeWithSize(context, this, "times", timesSizeFn(context.runtime));
        }
    }

    protected SizeFn timesSizeFn(final Ruby runtime) {
        final RubyInteger self = this;
        return new SizeFn() {
            @Override
            public IRubyObject size(IRubyObject[] args) {
                RubyFixnum zero = RubyFixnum.zero(runtime);
                ThreadContext context = runtime.getCurrentContext();
                if ((self instanceof RubyFixnum && getLongValue() < 0)
                        || sites(context).op_lt.call(context, self, self, zero).isTrue()) {
                    return zero;
                }

                return self;
            }
        };
    }

    /** int_succ
     *
     */
    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ(ThreadContext context) {
        if (this instanceof RubyFixnum) {
            return ((RubyFixnum) this).op_plus_one(context);
        } else {
            return numFuncall(context, this, sites(context).op_plus, RubyFixnum.one(context.runtime));
        }
    }

    static final ByteList[] SINGLE_CHAR_BYTELISTS;
    @Deprecated
    public static final ByteList[] SINGLE_CHAR_BYTELISTS19;
    static {
        SINGLE_CHAR_BYTELISTS = new ByteList[256];
        for (int i = 0; i < 256; i++) {
            ByteList bytes = new ByteList(new byte[] { (byte) i }, false);
            SINGLE_CHAR_BYTELISTS[i] = bytes;
            bytes.setEncoding(i < 0x80 ? USASCIIEncoding.INSTANCE : ASCIIEncoding.INSTANCE);
        }
        SINGLE_CHAR_BYTELISTS19 = SINGLE_CHAR_BYTELISTS;
    }

    static ByteList singleCharByteList(final byte index) {
        return SINGLE_CHAR_BYTELISTS[index & 0xFF];
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

        if (0xff < i) {
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

    private RubyString chrCommon(ThreadContext context, long value, Encoding enc) {
        if (value > 0xFFFFFFFFL) {
            throw context.runtime.newRangeError(this + " out of char range");
        }
        int c = (int) value;
        if (enc == null) enc = ASCIIEncoding.INSTANCE;
        return EncodingUtils.encUintChr(context, c, enc);
    }

    @Deprecated
    public final RubyString chr19(ThreadContext context, IRubyObject arg) {
        return chr(context, arg);
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

        if (ndigits > 0) {
            return RubyKernel.new_float(runtime, this);
        }
        if (ndigits == 0) {
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
    protected IRubyObject roundShared(ThreadContext context, int ndigits, RoundingMode roundingMode) {
        Ruby runtime = context.runtime;

        RubyNumeric f, h, n, r;

        if (int_round_zero_p(context, ndigits)) {
            return RubyFixnum.zero(runtime);
        }

        f = (RubyNumeric) int_pow(context, 10, -ndigits);
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
        h = (RubyNumeric) f.idiv(context, int2fix(runtime, 2));
        r = (RubyNumeric) this.op_mod(context, f);
        n = (RubyNumeric) this.op_minus(context, r);
        r = (RubyNumeric) r.op_cmp(context, h);
        if (r.isPositive(context).isTrue() ||
                (r.zero_p(context).isTrue() && doRoundCheck(context, roundingMode, this, n, f))) {
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
        Ruby runtime = context.runtime;
        if (sites(context).op_mod.call(context, this, this, RubyFixnum.two(runtime)) != RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
    }

    @JRubyMethod(name = "even?")
    public RubyBoolean even_p(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (sites(context).op_mod.call(context, this, this, RubyFixnum.two(runtime)) == RubyFixnum.zero(runtime)) {
            return runtime.getTrue();
        }
        return runtime.getFalse();
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
    public IRubyObject fdiv(ThreadContext context, IRubyObject y) {
        RubyInteger x = this;
        if (y instanceof RubyInteger && !((RubyInteger) y).zero_p(context).isTrue()) {
            IRubyObject gcd = gcd(context, y);
            if (!((RubyInteger) gcd).zero_p(context).isTrue()) {
                x = (RubyInteger) div(context, gcd);
                y = ((RubyInteger) y).div(context, gcd);
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
        other = RubyInteger.intValue(context, other);
        return context.runtime.newArray(f_gcd(context, this, other), f_lcm(context, this, other));
    }

    static IRubyObject intValue(ThreadContext context, IRubyObject num) {
        IRubyObject i;
        if (( i = RubyInteger.toInteger(context, num) ) == null) {
            throw context.runtime.newTypeError("not an integer");
        }
        return i;
    }

    static IRubyObject toInteger(ThreadContext context, IRubyObject num) {
        if (num instanceof RubyInteger) return num;
        if (num instanceof RubyNumeric && !integer_p(context).call(context, num, num).isTrue()) { // num.integer?
            return null;
        }
        if (num instanceof RubyString) return null; // do not want String#to_i
        return num.checkCallMethod(context, sites(context).to_i_checked);
    }

    @JRubyMethod(name = "digits")
    public RubyArray digits(ThreadContext context) {
        return digits(context, RubyFixnum.newFixnum(context.getRuntime(), 10));
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

    @JRubyMethod(name = "to_s")
    @Override
    public abstract RubyString to_s();

    @JRubyMethod(name = "to_s")
    public abstract RubyString to_s(IRubyObject x);

    @JRubyMethod(name = "-@")
    public abstract IRubyObject op_uminus(ThreadContext context);

    @JRubyMethod(name = "+")
    public abstract IRubyObject op_plus(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "-")
    public abstract IRubyObject op_minus(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "*")
    public abstract IRubyObject op_mul(ThreadContext context, IRubyObject other);

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

    @JRubyMethod(name = "**")
    public abstract IRubyObject op_pow(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "abs")
    public abstract IRubyObject abs(ThreadContext context);

    @JRubyMethod(name = "magnitude")
    @Override
    public IRubyObject magnitude(ThreadContext context) {
        return abs(context);
    }

    @JRubyMethod(name = "==")
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

    @JRubyMethod(name = ">>")
    public abstract IRubyObject op_rshift(ThreadContext context, IRubyObject other);

    @JRubyMethod(name = "to_f")
    public abstract IRubyObject to_f(ThreadContext context);

    @JRubyMethod(name = "size")
    public abstract IRubyObject size(ThreadContext context);

    @JRubyMethod(name = "zero?")
    public abstract IRubyObject zero_p(ThreadContext context);

    @JRubyMethod(name = "bit_length")
    public abstract IRubyObject bit_length(ThreadContext context);

    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_gt(context, this, other);
    }

    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_lt(context, this, other);
    }

    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_ge(context, this, other);
    }

    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        return RubyComparable.op_le(context, this, other);
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

    public IRubyObject op_lshift(IRubyObject other) {
        return op_lshift(getRuntime().getCurrentContext(), other);
    }

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
            throw recv.getRuntime().newTypeError(
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
