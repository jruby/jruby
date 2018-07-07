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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.compiler.Constantizable;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.util.ByteList;
import org.jruby.util.ConvertBytes;
import org.jruby.util.Numeric;
import org.jruby.util.cli.Options;

import static org.jruby.util.Numeric.f_odd_p;

/**
 * Implementation of the Fixnum class.
 */
public class RubyFixnum extends RubyInteger implements Constantizable {

    public static RubyClass createFixnumClass(Ruby runtime) {
        RubyClass fixnum = runtime.getInteger();
        runtime.getObject().setConstant("Fixnum", fixnum);
        runtime.getObject().deprecateConstant(runtime, "Fixnum");
        runtime.setFixnum(fixnum);

        for (int i = 0; i < runtime.fixnumCache.length; i++) {
            runtime.fixnumCache[i] = new RubyFixnum(fixnum, i - CACHE_OFFSET);
        }

        return fixnum;
    }

    private final long value;
    private static final int BIT_SIZE = 64;
    public static final long SIGN_BIT = (1L << (BIT_SIZE - 1));
    public static final long MAX = (1L<<(BIT_SIZE - 1)) - 1;
    public static final long MIN = -1 * MAX - 1;
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

    private static IRubyObject fixCoerce(IRubyObject x) {
        do {
            x = x.convertToInteger();
        } while (!(x instanceof RubyFixnum) && !(x instanceof RubyBignum));
        return x;
    }

    public RubyFixnum(Ruby runtime) {
        this(runtime, 0);
    }

    public RubyFixnum(Ruby runtime, long value) {
        super(runtime.getFixnum());
        this.value = value;
        this.flags |= FROZEN_F;
    }

    private RubyFixnum(RubyClass klazz, long value) {
        super(klazz);
        this.value = value;
        this.flags |= FROZEN_F;
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
        long value = this.value;

        if (value < CACHE_OFFSET && value >= -CACHE_OFFSET) {
            Object[] fixnumConstants = getRuntime().fixnumConstants;
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
        return other instanceof RubyFixnum && value == ((RubyFixnum)other).value;
    }

    @Override
    public IRubyObject equal_p(ThreadContext context, IRubyObject obj) {
        return context.runtime.newBoolean(this == obj || eql(obj));
    }

    @Override
    public boolean isImmediate() {
    	return true;
    }

    @Override
    public RubyClass getSingletonClass() {
        throw getRuntime().newTypeError("can't define singleton");
    }

    @Override
    public Class<?> getJavaClass() {
        return long.class;
    }

    @Override
    public double getDoubleValue() {
        return value;
    }

    @Override
    public long getLongValue() {
        return value;
    }

    @Override
    public int getIntValue() { return (int) value; }

    @Override
    public BigInteger getBigIntegerValue() {
        return BigInteger.valueOf(value);
    }

    @Override
    public int signum() { return Long.signum(value); }

    @Override
    public RubyInteger negate() {
        return negate(getRuntime(), value);
    }

    public static RubyFixnum newFixnum(Ruby runtime, long value) {
        if (USE_CACHE && isInCacheRange(value)) {
            return cachedFixnum(runtime, value);
        }
        return new RubyFixnum(runtime, value);
    }

    private static boolean isInCacheRange(long value) {
        return value <= CACHE_OFFSET - 1 && value >= -CACHE_OFFSET;
    }

    private static RubyFixnum cachedFixnum(Ruby runtime, long value) {
        return runtime.fixnumCache[(int) value + CACHE_OFFSET];
    }

    public RubyFixnum newFixnum(long newValue) {
        return newFixnum(getRuntime(), newValue);
    }

    public static RubyFixnum zero(Ruby runtime) {
        return CACHE_OFFSET > 0 ? runtime.fixnumCache[CACHE_OFFSET] : new RubyFixnum(runtime, 0);
    }

    public static RubyFixnum one(Ruby runtime) {
        return CACHE_OFFSET > 1 ? runtime.fixnumCache[CACHE_OFFSET + 1] : new RubyFixnum(runtime, 1);
    }

    public static RubyFixnum two(Ruby runtime) {
        return CACHE_OFFSET > 2 ? runtime.fixnumCache[CACHE_OFFSET + 2] : new RubyFixnum(runtime, 2);
    }

    public static RubyFixnum three(Ruby runtime) {
        return CACHE_OFFSET > 3 ? runtime.fixnumCache[CACHE_OFFSET + 3] : new RubyFixnum(runtime, 3);
    }

    public static RubyFixnum four(Ruby runtime) {
        return CACHE_OFFSET > 4 ? runtime.fixnumCache[CACHE_OFFSET + 4] : new RubyFixnum(runtime, 4);
    }

    public static RubyFixnum five(Ruby runtime) {
        return CACHE_OFFSET > 5 ? runtime.fixnumCache[CACHE_OFFSET + 5] : new RubyFixnum(runtime, 5);
    }

    public static RubyFixnum minus_one(Ruby runtime) {
        return -CACHE_OFFSET <= -1 ? runtime.fixnumCache[CACHE_OFFSET - 1] : new RubyFixnum(runtime, -1);
    }

    @Override
    public RubyFixnum hash() {
        return newFixnum(hashCode());
    }

    @Override
    public final int hashCode() {
        return (int) (value ^ value >>> 32);
    }

    /*  ================
     *  Instance Methods
     *  ================
     */
    @Override
    public IRubyObject times(ThreadContext context, Block block) {
        if (block.isGiven()) {
            final long value = this.value;
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
                final Ruby runtime = context.runtime;
                for (long i = 0; i < value; i++) {
                    block.yield(context, RubyFixnum.newFixnum(runtime, i));
                }
            }
            return this;
        }
        return RubyEnumerator.enumeratorizeWithSize(context, this, "times", timesSizeFn(context.runtime));
    }
    /** rb_fix_ceil
     *
     */
    @Override
    public IRubyObject ceil(ThreadContext context, IRubyObject arg){
        long ndigits = arg.convertToInteger().getLongValue();
        long self = getLongValue();
        if (ndigits >= 0) {
            return this;
        } else {
            long posdigits = Math.abs(ndigits);
            long exp = (long) Math.pow(10, posdigits);
            long mod = (self % exp + exp) % exp;
            long res = self;
            if (mod != 0) {
                res = self + (exp - (mod));
            }
            return newFixnum(context.runtime, res);
        }
    }

    /** rb_fix_floor
     *
     */
    @Override
    public IRubyObject floor(ThreadContext context, IRubyObject arg){
        long ndigits = (arg).convertToInteger().getLongValue();
        long self = getLongValue();
        if (ndigits >= 0) {
            return this;
        } else {
            long posdigits = Math.abs(ndigits);
            long exp = (long) Math.pow(10, posdigits);
            long mod = (self % exp + exp) % exp;
            long res = self - mod;
            return newFixnum(context.runtime, res);
        }
    }

    /** rb_fix_truncate
     *
     */
    @Override
    public IRubyObject truncate(ThreadContext context, IRubyObject arg) {
        long self = getLongValue();
        if (self > 0){
            return floor(context, arg);
        } else if (self < 0){
            return ceil(context, arg);
        } else {
            return this;
        }
    }

    /** rb_fix_digits
     *
     */
    @Override
    public RubyArray digits(ThreadContext context, IRubyObject base) {

        long self = getLongValue();
        Ruby runtime = context.getRuntime();
        if (self < 0) {
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
        if (base instanceof RubyBignum){
            return RubyArray
                    .newArray(context.runtime, 1)
                    .append(newFixnum(runtime, self));
        }
        long longBase = ((RubyFixnum)base).getLongValue();
        if (longBase < 0) {
            throw runtime.newArgumentError("negative radix");
        }
        if (longBase < 2) {
            throw runtime.newArgumentError("invalid radix: " + longBase);
        }

        RubyArray res = RubyArray.newArray(context.runtime, 0);

        if (self == 0) {
            res.append(newFixnum(runtime, 0));
            return res;
        }

        while (self > 0) {
            long q = self % longBase;
            res.append(newFixnum(runtime, q));
            self /= longBase;
        }

        return res;
    }


    /** fix_to_s
     *
     */
    public RubyString to_s(IRubyObject[] args) {
        switch (args.length) {
            case 0: return to_s();
            case 1: return to_s(args[0]);
            default: throw getRuntime().newArgumentError(args.length, 1);
        }
    }

    @Override
    public RubyString to_s() {
        ByteList bytes = ConvertBytes.longToByteList(value, 10);
        return RubyString.newString(getRuntime(), bytes, USASCIIEncoding.INSTANCE);
    }

    @Override
    public RubyString to_s(IRubyObject arg0) {
        int base = num2int(arg0);
        if (base < 2 || base > 36) {
            throw getRuntime().newArgumentError("illegal radix " + base);
        }
        ByteList bytes = ConvertBytes.longToByteList(value, base);
        return RubyString.newString(getRuntime(), bytes, USASCIIEncoding.INSTANCE);
    }

    /** fix_to_sym
     *
     */
    @Deprecated
    public IRubyObject to_sym() {
        RubySymbol symbol = RubySymbol.getSymbolLong(getRuntime(), value);

        return symbol != null ? symbol : getRuntime().getNil();
    }

    /** fix_uminus
     *
     */
    @Override
    public IRubyObject op_uminus(ThreadContext context) {
        return negate(context.runtime, value);
    }

    private static RubyInteger negate(final Ruby runtime, final long value) {
        if (value == MIN) { // a gotcha
            return RubyBignum.newBignum(runtime, BigInteger.valueOf(value).negate());
        }
        return RubyFixnum.newFixnum(runtime, -value);
    }

    /** fix_plus
     *
     */
    @Override
    public IRubyObject op_plus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return addFixnum(context, (RubyFixnum) other);
        }
        return addOther(context, other);
    }

    @Override
    public IRubyObject op_plus(ThreadContext context, long otherValue) {
        try {
            return newFixnum(Math.addExact(value, otherValue));
        } catch (ArithmeticException ae) {
            return addAsBignum(context, otherValue);
        }
    }

    public IRubyObject op_plus_one(ThreadContext context) {
        try {
            return newFixnum(context.runtime, Math.addExact(value, 1));
        } catch (ArithmeticException ae) {
            return addAsBignum(context, 1);
        }
    }

    public IRubyObject op_plus_two(ThreadContext context) {
        long result = value + 2;
    //- if (result == Long.MIN_VALUE + 1) {     //-code
        if (result < value) {                   //+code+patch; maybe use  if (result <= value) {
            return addAsBignum(context, 2);
        }
        return newFixnum(context.runtime, result);
    }

    private RubyInteger addFixnum(ThreadContext context, RubyFixnum other) {
        try {
            return newFixnum(context.runtime, Math.addExact(value, other.value));
        } catch (ArithmeticException ae) {
            return (RubyInteger) addAsBignum(context, other);
        }
    }

    private IRubyObject addAsBignum(ThreadContext context, RubyFixnum other) {
        return RubyBignum.newBignum(context.runtime, value).op_plus(context, other.value);
    }

    private IRubyObject addAsBignum(ThreadContext context, long other) {
        return RubyBignum.newBignum(context.runtime, value).op_plus(context, other);
    }

    private IRubyObject addOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_plus(context, this.value);
        }
        if (other instanceof RubyFloat) {
            return context.runtime.newFloat((double) value + ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, sites(context).op_plus, other);
    }

    /** fix_minus
     *
     */
    @Override
    public IRubyObject op_minus(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return subtractFixnum(context, (RubyFixnum) other);
        }
        return subtractOther(context, other);
    }

    @Override
    public IRubyObject op_minus(ThreadContext context, long otherValue) {
        try {
            return newFixnum(context.runtime, Math.subtractExact(value, otherValue));
        } catch (ArithmeticException ae) {
            return subtractAsBignum(context, otherValue);
        }
    }

    public IRubyObject op_minus_one(ThreadContext context) {
        try {
            return newFixnum(context.runtime, Math.subtractExact(value, 1));
        } catch (ArithmeticException ae) {
            return subtractAsBignum(context, 1);
        }
    }

    public IRubyObject op_minus_two(ThreadContext context) {
        try {
            return newFixnum(context.runtime, Math.subtractExact(value, 2));
        } catch (ArithmeticException ae) {
            return subtractAsBignum(context, 2);
        }
    }

    private IRubyObject subtractFixnum(ThreadContext context, RubyFixnum other) {
        try {
            return newFixnum(context.runtime, Math.subtractExact(value, other.value));
        } catch (ArithmeticException ae) {
            return subtractAsBignum(context, other);
        }
    }

    private IRubyObject subtractAsBignum(ThreadContext context, RubyFixnum other) {
        return RubyBignum.newBignum(context.runtime, value).op_minus(context, other);
    }

    private IRubyObject subtractAsBignum(ThreadContext context, long other) {
        return RubyBignum.newBignum(context.runtime, value).op_minus(context, other);
    }

    private IRubyObject subtractOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBignum.newBignum(context.runtime, value).op_minus(context, other);
        } else if (other instanceof RubyFloat) {
            return context.runtime.newFloat((double) value - ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, sites(context).op_minus, other);
    }

    /** fix_mul
     *
     */
    @Override
    public IRubyObject op_mul(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_mul(context, ((RubyFixnum) other).value);
        }
        return multiplyOther(context, other);
    }

    private IRubyObject multiplyOther(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_mul(context, this.value);
        } else if (other instanceof RubyFloat) {
            return runtime.newFloat((double) value * ((RubyFloat) other).getDoubleValue());
        }
        return coerceBin(context, sites(context).op_times, other);
    }

    @Override
    public IRubyObject op_mul(ThreadContext context, long otherValue) {
        Ruby runtime = context.runtime;
        try {
            return newFixnum(runtime, Math.multiplyExact(value, otherValue));
        } catch (ArithmeticException ae) {
            return RubyBignum.newBignum(runtime, value).op_mul(context, otherValue);
        }
    }

    /** fix_div
     * here is terrible MRI gotcha:
     * 1.div 3.0 -> 0
     * 1 / 3.0   -> 0.3333333333333333
     *
     * MRI is also able to do it in one place by looking at current frame in rb_num_coerce_bin:
     * rb_funcall(x, ruby_frame->orig_func, 1, y);
     *
     * also note that RubyFloat doesn't override Numeric.div
     */
    @Override
    public IRubyObject idiv(ThreadContext context, IRubyObject other) {
        checkZeroDivisionError(context, other);

        return idiv(context, other, sites(context).div);
    }

    @Override
    public IRubyObject idiv(ThreadContext context, long other) {
        return idivLong(context, value, other);
    }

    @Override
    public IRubyObject op_div(ThreadContext context, IRubyObject other) {
        return idiv(context, other, sites(context).op_quo);
    }

    public IRubyObject op_div(ThreadContext context, long other) {
        return idivLong(context, value, other);
    }

    @Override
    public RubyBoolean odd_p(ThreadContext context) {
        return (value & 1) != 0 ? context.tru : context.fals;
    }

    @Override
    public RubyBoolean even_p(ThreadContext context) {
        return (value & 1) == 0 ? context.tru : context.fals;
    }

    public IRubyObject pred(ThreadContext context) {
        return op_minus_one(context);
    }

    @Deprecated
    public IRubyObject idiv(ThreadContext context, IRubyObject other, String method) {
        if (other instanceof RubyFixnum) {
            return idivLong(context, value, ((RubyFixnum) other).value);
        }
        return coerceBin(context, method, other);
    }

    public IRubyObject idiv(ThreadContext context, IRubyObject other, CallSite site) {
        if (other instanceof RubyFixnum) {
            return idivLong(context, value, ((RubyFixnum) other).value);
        }
        return coerceBin(context, site, other);
    }

    @Deprecated
    public IRubyObject idiv(ThreadContext context, long y, String method) {
        long x = value;

        return idivLong(context, x, y);
    }

    private RubyInteger idivLong(ThreadContext context, long x, long y) {
        final Ruby runtime = context.runtime;
        if (y == 0) {
            throw runtime.newZeroDivisionError();
        }
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
            if (x == MIN) {
                return RubyBignum.newBignum(runtime, BigInteger.valueOf(x).negate());
            }
            result = -x;
        } else {
            result = x / y;  // x <= 0, y < 0;
        }
        return runtime.newFixnum(result);
    }

    /** fix_mod
     *
     */
    @Override
    public IRubyObject op_mod(ThreadContext context, IRubyObject other) {
        checkZeroDivisionError(context, other);
        if (other instanceof RubyFixnum) {
            return moduloFixnum(context, (RubyFixnum) other);
        }
        return coerceBin(context, sites(context).op_mod, other);
    }

    @Override
    public IRubyObject op_mod(ThreadContext context, long other) {
        return moduloFixnum(context, other);
    }

    private IRubyObject moduloFixnum(ThreadContext context, RubyFixnum other) {
        return moduloFixnum(context, other.value);
    }

    private IRubyObject moduloFixnum(ThreadContext context, long other) {
        // Java / and % are not the same as ruby
        long x = value;
        long y = other;
        if (y == 0) {
            throw context.runtime.newZeroDivisionError();
        }
        long mod = x % y;
        if (mod < 0 && y > 0 || mod > 0 && y < 0) {
            mod += y;
        }
        return context.runtime.newFixnum(mod);
    }

    /** fix_divmod
     *
     */
    @Override
    public IRubyObject divmod(ThreadContext context, IRubyObject other) {
        checkZeroDivisionError(context, other);
        if (other instanceof RubyFixnum) {
            return divmodFixnum(context, (RubyFixnum) other);
        }
        return coerceBin(context, sites(context).divmod, other);
    }

    private IRubyObject divmodFixnum(ThreadContext context, RubyFixnum other) {
        final Ruby runtime = context.runtime;

        final long x = this.value;
        final long y = other.value;
        if (y == 0) {
            throw runtime.newZeroDivisionError();
        }

        long mod; final RubyInteger integerDiv;
        if (y == -1) {
            if (x == MIN) {
                integerDiv = RubyBignum.newBignum(runtime, BigInteger.valueOf(x).negate());
            } else {
                integerDiv = RubyFixnum.newFixnum(runtime, -x);
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
            integerDiv = RubyFixnum.newFixnum(runtime, div);
        }
        IRubyObject fixMod = RubyFixnum.newFixnum(runtime, mod);
        return RubyArray.newArray(runtime, integerDiv, fixMod);
    }

    /** fix_pow
     *
     */
    @Override
    public IRubyObject op_pow(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyNumeric) {
            double d_other = ((RubyNumeric) other).getDoubleValue();
            if (value < 0 && (d_other != Math.round(d_other))) {
                RubyComplex complex = RubyComplex.newComplexRaw(context.runtime, this);
                return numFuncall(context, complex, sites(context).op_exp_complex, other);
            }
            if (other instanceof RubyFixnum) {
                return powerFixnum(context, (RubyFixnum) other);
            }
        }
        return powerOther(context, other);
    }

    public IRubyObject op_pow(ThreadContext context, long other) {
        return powerFixnum(context, RubyFixnum.newFixnum(context.runtime, other));
    }

    @Deprecated
    public IRubyObject op_pow_19(ThreadContext context, IRubyObject other) {
        return op_pow(context, other);
    }

    private IRubyObject powerOther(ThreadContext context, IRubyObject other) {
        Ruby runtime = context.runtime;
        long a = value;
        if (other instanceof RubyBignum) {
            if (sites(context).op_lt_bignum.call(context, other, other, RubyFixnum.zero(runtime)).isTrue()) {
                RubyRational rational = RubyRational.newRationalRaw(runtime, this);
                return numFuncall(context, rational, sites(context).op_exp_rational, other);
            }
            if (a == 0) return RubyFixnum.zero(runtime);
            if (a == 1) return RubyFixnum.one(runtime);
            if (a == -1) {
                return ((RubyBignum)other).even_p(context).isTrue() ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
            }
            RubyBignum.newBignum(runtime, RubyBignum.fix2big(this)).op_pow(context, other);
        } else if (other instanceof RubyFloat) {
            double b = ((RubyFloat)other).getValue();
            if (b == 0.0 || a == 1) return runtime.newFloat(1.0);
            if (a == 0) return runtime.newFloat(b < 0 ? 1.0 / 0.0 : 0.0);
            return RubyFloat.newFloat(runtime, Math.pow(a, b));
        }
        return coerceBin(context, sites(context).op_exp, other);
    }

    private RubyNumeric powerFixnum(ThreadContext context, RubyFixnum other) {
        Ruby runtime = context.runtime;
        long a = value;
        long b = other.value;
        if (b < 0) {
            RubyRational rational = RubyRational.newRationalRaw(runtime, this);
            return (RubyNumeric) numFuncall(context, rational, sites(context).op_exp_rational, other);
        }
        if (b == 0) {
            return RubyFixnum.one(runtime);
        }
        if (b == 1) {
            return this;
        }
        if (a == 0) {
            return b > 0 ? RubyFixnum.zero(runtime) : RubyNumeric.dbl2ival(runtime, 1.0 / 0.0);
        }
        if (a == 1) {
            return RubyFixnum.one(runtime);
        }
        if (a == -1) {
            return b % 2 == 0 ? RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
        }
        return Numeric.int_pow(context, a, b);
    }

    // MRI: int_pow_tmp1
    protected IRubyObject intPowTmp1(ThreadContext context, RubyInteger y, long mm, boolean negaFlg) {
        Ruby runtime = context.runtime;

        long xx = getLongValue();
        long tmp = 1L;
        long yy;

        for (/*NOP*/; !(y instanceof RubyFixnum); y = (RubyInteger) sites(context).op_rshift.call(context, y, y, RubyFixnum.one(runtime))) {
            if (f_odd_p(context, y)) {
                tmp = (tmp * xx) % mm;
            }
            xx = (xx * xx) % mm;
        }
        for (yy = y.convertToInteger().getLongValue(); yy != 0L; yy >>= 1L) {
            if ((yy & 1L) != 0L) {
                tmp = (tmp * xx) % mm;
            }
            xx = (xx * xx) % mm;
        }

        if (negaFlg && (tmp != 0)) {
            tmp -= mm;
        }
        return runtime.newFixnum(tmp);
    }

    // MRI: int_pow_tmp2
    protected IRubyObject intPowTmp2(ThreadContext context, IRubyObject y, long mm, boolean negaFlg) {
        Ruby runtime = context.runtime;

        long tmp = 1L;
        long yy;

        final IRubyObject m = runtime.newFixnum(mm);
        RubyFixnum tmp2 = runtime.newFixnum(tmp);
        RubyFixnum xx = (RubyFixnum) this;

        for (/*NOP*/; !(y instanceof RubyFixnum); y = sites(context).op_rshift.call(context, y, y, RubyFixnum.one(runtime))) {
            if (f_odd_p(context, y)) {
                tmp2 = mulModulo(context, tmp2, xx, m);
            }
            xx = mulModulo(context, xx, xx, m);
        }
        for (yy = ((RubyFixnum) y).getLongValue(); yy != 0; yy >>= 1L) {
            if ((yy & 1L) != 0) {
                tmp2 = mulModulo(context, tmp2, xx, m);
            }
            xx = mulModulo(context, xx, xx, m);
        }

        tmp = tmp2.getLongValue();
        if (negaFlg && (tmp != 0)) {
            tmp -= mm;
        }
        return runtime.newFixnum(tmp);
    }

    // MRI: MUL_MODULO macro defined within int_pow_tmp2 in numeric.c
    private static RubyFixnum mulModulo(ThreadContext context, RubyFixnum a, RubyFixnum b, IRubyObject c) {
        return (RubyFixnum) ((RubyInteger) a.op_mul(context, b.getLongValue())).modulo(context, c);
    }

    /** fix_abs
     *
     */
    @Override
    public IRubyObject abs(ThreadContext context) {
        if (value < 0) {
            // A gotcha for Long.MIN_VALUE: value = -value
            if (value == Long.MIN_VALUE) {
                return RubyBignum.newBignum(
                        context.runtime, BigInteger.valueOf(value).negate());
            }
            return RubyFixnum.newFixnum(context.runtime, -value);
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
                op_equal(context, ((RubyFixnum) other).value) : op_equalOther(context, other);
    }

    public IRubyObject op_equal(ThreadContext context, long other) {
        return RubyBoolean.newBoolean(context.runtime, value == other);
    }

    public boolean op_equal_boolean(ThreadContext context, long other) {
        return value == other;
    }

    public final boolean fastEqual(RubyFixnum other) {
        return value == other.value;
    }

    private IRubyObject op_equalOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBoolean.newBoolean(context.runtime,
                    BigInteger.valueOf(value).compareTo(((RubyBignum) other).getValue()) == 0);
        }
        if (other instanceof RubyFloat) {
            return RubyBoolean.newBoolean(context.runtime, (double) value == ((RubyFloat) other).getDoubleValue());
        }
        return super.op_num_equal(context, other);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof RubyFixnum) {
            return value == ((RubyFixnum) other).value;
        }
        return false;
    }

    @Override
    public final int compareTo(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long otherValue = ((RubyFixnum)other).value;
            return value == otherValue ? 0 : value > otherValue ? 1 : -1;
        }
        return compareToOther(other);
    }

    private int compareToOther(IRubyObject other) {
        if (other instanceof RubyBignum) return BigInteger.valueOf(value).compareTo(((RubyBignum)other).getValue());
        if (other instanceof RubyFloat) return Double.compare((double)value, ((RubyFloat)other).getDoubleValue());
        ThreadContext context = getRuntime().getCurrentContext();
        return (int) coerceCmp(context, sites(context).op_cmp, other).convertToInteger().getLongValue();
    }

    /** fix_cmp
     *
     */
    @Override
    public IRubyObject op_cmp(ThreadContext context, IRubyObject other) {
        return other instanceof RubyFixnum ?
                op_cmp(context, ((RubyFixnum) other).value) : compareOther(context, other);
    }

    public IRubyObject op_cmp(ThreadContext context, long other) {
        Ruby runtime = context.runtime;
        return value == other ? RubyFixnum.zero(runtime) : value > other ?
                RubyFixnum.one(runtime) : RubyFixnum.minus_one(runtime);
    }

    private IRubyObject compareOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return newFixnum(context.runtime, BigInteger.valueOf(value).compareTo(((RubyBignum)other).getValue()));
        }
        if (other instanceof RubyFloat) {
            return dbl_cmp(context.runtime, (double) value, ((RubyFloat) other).getDoubleValue());
        }
        return coerceCmp(context, sites(context).op_cmp, other);
    }

    /** fix_gt
     *
     */
    @Override
    public IRubyObject op_gt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(context.runtime, value > ((RubyFixnum) other).value);
        }

        return op_gtOther(context, other);
    }

    public IRubyObject op_gt(ThreadContext context, long other) {
        return RubyBoolean.newBoolean(context.runtime, value > other);
    }

    public boolean op_gt_boolean(ThreadContext context, long other) {
        return value > other;
    }

    private IRubyObject op_gtOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBoolean.newBoolean(context.runtime,
                    BigInteger.valueOf(value).compareTo(((RubyBignum) other).getValue()) > 0);
        }
        if (other instanceof RubyFloat) {
            return RubyBoolean.newBoolean(context.runtime, (double) value > ((RubyFloat) other).getDoubleValue());
        }
        return coerceRelOp(context, sites(context).op_gt, other);
    }

    /** fix_ge
     *
     */
    @Override
    public IRubyObject op_ge(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(context.runtime, value >= ((RubyFixnum) other).value);
        }
        return op_geOther(context, other);
    }

    public IRubyObject op_ge(ThreadContext context, long other) {
        return RubyBoolean.newBoolean(context.runtime, value >= other);
    }

    public boolean op_ge_boolean(ThreadContext context, long other) {
        return value >= other;
    }

    private IRubyObject op_geOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBoolean.newBoolean(context.runtime,
                    BigInteger.valueOf(value).compareTo(((RubyBignum) other).getValue()) >= 0);
        }
        if (other instanceof RubyFloat) {
            return RubyBoolean.newBoolean(context.runtime, (double) value >= ((RubyFloat) other).getDoubleValue());
        }
        return coerceRelOp(context, sites(context).op_ge, other);
    }

    /** fix_lt
     *
     */
    @Override
    public IRubyObject op_lt(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return op_lt(context, ((RubyFixnum) other).value);
        }
        return op_ltOther(context, other);
    }

    public IRubyObject op_lt(ThreadContext context, long other) {
        return RubyBoolean.newBoolean(context.runtime, value < other);
    }

    public boolean op_lt_boolean(ThreadContext context, long other) {
        return value < other;
    }

    private IRubyObject op_ltOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBoolean.newBoolean(context.runtime,
                    BigInteger.valueOf(value).compareTo(((RubyBignum) other).getValue()) < 0);
        }
        if (other instanceof RubyFloat) {
            return RubyBoolean.newBoolean(context.runtime, (double) value < ((RubyFloat) other).getDoubleValue());
        }
        return coerceRelOp(context, sites(context).op_lt, other);
    }

    /** fix_le
     *
     */
    @Override
    public IRubyObject op_le(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return RubyBoolean.newBoolean(context.runtime, value <= ((RubyFixnum) other).value);
        }
        return op_leOther(context, other);
    }

    public IRubyObject op_le(ThreadContext context, long other) {
        return RubyBoolean.newBoolean(context.runtime, value <= other);
    }

    public boolean op_le_boolean(ThreadContext context, long other) {
        return value <= other;
    }

    private IRubyObject op_leOther(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyBignum) {
            return RubyBoolean.newBoolean(context.runtime,
                    BigInteger.valueOf(value).compareTo(((RubyBignum) other).getValue()) <= 0);
        }
        if (other instanceof RubyFloat) {
            return RubyBoolean.newBoolean(context.runtime, (double) value <= ((RubyFloat) other).getDoubleValue());
        }
        return coerceRelOp(context, sites(context).op_le, other);
    }

    /** fix_rev
     *
     */
    @Override
    public IRubyObject op_neg(ThreadContext context) {
        return newFixnum(context.runtime, ~value);
    }

    /** fix_and
     *
     */
    @Override
    public IRubyObject op_and(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return context.runtime.newFixnum(value & ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_and(context, this);
        }
        return coerceBit(context, sites(context).checked_op_and, other);
    }

    public IRubyObject op_and(ThreadContext context, long other) {
        return newFixnum(context.runtime, value & other);
    }

    /** fix_or
     *
     */
    @Override
    public IRubyObject op_or(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return context.runtime.newFixnum(value | ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_or(context, this);
        }
        return coerceBit(context, sites(context).checked_op_or, other);
    }

    public IRubyObject op_or(ThreadContext context, long other) {
        return newFixnum(context.runtime, value | other);
    }

    /** fix_xor
     *
     */
    @Override
    public IRubyObject op_xor(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFixnum) {
            return context.runtime.newFixnum(value ^ ((RubyFixnum) other).value);
        }
        if (other instanceof RubyBignum) {
            return ((RubyBignum) other).op_xor(context, this);
        }
        return coerceBit(context, sites(context).checked_op_xor, other);
    }

    public IRubyObject op_xor(ThreadContext context, long other) {
        return newFixnum(context.runtime, value ^ other);
    }

    /** fix_aref
     *
     */
    @Override
    public IRubyObject op_aref(ThreadContext context, IRubyObject other) {
        if(!(other instanceof RubyFixnum) && !((other = fixCoerce(other)) instanceof RubyFixnum)) {
            RubyBignum big = (RubyBignum) other;
            other = RubyBignum.bignorm(context.runtime, big.getValue());
            if (!(other instanceof RubyFixnum)) {
                return big.getValue().signum() == 0 || value >= 0 ? RubyFixnum.zero(context.runtime) : RubyFixnum.one(context.runtime);
            }
        }

        long otherValue = fix2long(other);

        if (otherValue < 0) return RubyFixnum.zero(context.runtime);

        if (BIT_SIZE - 1 < otherValue) {
            return value < 0 ? RubyFixnum.one(context.runtime) : RubyFixnum.zero(context.runtime);
        }

        return (value & (1L << otherValue)) == 0 ? RubyFixnum.zero(context.runtime) : RubyFixnum.one(context.runtime);
    }

    /** fix_lshift
     *
     */
    @Override
    public IRubyObject op_lshift(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyFixnum)) {
            return RubyBignum.newBignum(context.runtime, value).op_lshift(context, other);
        }

        return op_lshift(context, ((RubyFixnum) other).getLongValue());
    }

    @Override
    public RubyInteger op_lshift(ThreadContext context, final long width) {
        return width < 0 ? rshift(context, -width) : lshift(context, width);
    }

    private RubyInteger lshift(ThreadContext context, final long width) {
        if (width > BIT_SIZE - 1 || ((~0L << BIT_SIZE - width - 1) & value) != 0) {
            return RubyBignum.newBignum(context.runtime, value).op_lshift(context, width);
        }
        return RubyFixnum.newFixnum(context.runtime, value << width);
    }

    @Deprecated // no longer used
    public IRubyObject op_lshift(long width) {
        return op_lshift(getRuntime().getCurrentContext(), width);
    }

    /** fix_rshift
     *
     */
    @Override
    public IRubyObject op_rshift(ThreadContext context, IRubyObject other) {
        if (!(other instanceof RubyFixnum)) {
            return RubyBignum.newBignum(context.runtime, value).op_rshift(context, other);
        }

        return op_rshift(context, ((RubyFixnum) other).getLongValue());
    }

    @Override
    public RubyInteger op_rshift(ThreadContext context, final long width) {
        if (width == 0) return this;

        return width < 0 ? lshift(context, -width) : rshift(context, width);
    }

    private RubyFixnum rshift(ThreadContext context, final long width) {
        if (width >= BIT_SIZE - 1) {
            return value < 0 ? RubyFixnum.minus_one(context.runtime) : RubyFixnum.zero(context.runtime);
        }
        return RubyFixnum.newFixnum(context.runtime, value >> width);
    }

    @Deprecated
    public IRubyObject op_rshift(long width) {
        return op_rshift(getRuntime().getCurrentContext(), width);
    }

    /** fix_to_f
     *
     */
    @Override
    public IRubyObject to_f(ThreadContext context) {
        return RubyFloat.newFloat(context.runtime, (double) value);
    }

    @Override
    public IRubyObject to_f() {
        return RubyFloat.newFloat(getRuntime(), (double) value);
    }

    /** fix_size
     *
     */
    @Override
    public IRubyObject size(ThreadContext context) {
        return newFixnum((long) ((BIT_SIZE + 7) / 8));
    }

    @Deprecated
    public IRubyObject zero_p() {
        return zero_p(getRuntime().getCurrentContext());
    }

    /** fix_zero_p
     *
     */
    @Override
    public IRubyObject zero_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context.runtime, value == 0);
    }

    @Override
    public final boolean isZero() {
        return value == 0;
    }

    @Override
    public IRubyObject nonzero_p(ThreadContext context) {
        return isZero() ? context.nil : this;
    }

    @Override
    final boolean isOne() {
        return value == 1;
    }

    @Override
    public IRubyObject succ(ThreadContext context) {
        return ((RubyFixnum) this).op_plus_one(context);
    }

    @Override
    public IRubyObject bit_length(ThreadContext context) {
        long tmpValue = value;
        if (value < 0) {
            tmpValue = ~value;
        }

        return context.runtime.newFixnum(64 - Long.numberOfLeadingZeros(tmpValue));
    }

    @Override
    public IRubyObject id() {
        if (value <= Long.MAX_VALUE / 2 && value >= Long.MIN_VALUE / 2) {
            return newFixnum(2 * value + 1);
        }

        return super.id();
    }

    @Override
    public IRubyObject taint(ThreadContext context) {
        return this;
    }

    // Piece of mri rb_to_id
    @Override
    public String asJavaString() {
        throw getRuntime().newTypeError(inspect().toString() + " is not a symbol");
    }

    public static RubyFixnum unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        return input.getRuntime().newFixnum(input.unmarshalInt());
    }

    private void checkZeroDivisionError(ThreadContext context, IRubyObject other) {
        if (other instanceof RubyFloat && ((RubyFloat) other).getDoubleValue() == 0.0d) {
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
        if (y instanceof RubyFixnum) {
            return context.runtime.newFloat(((double) value) / ((double) fix2long(y)));
        }
        if (y instanceof RubyBignum) {
            return RubyBignum.newBignum(context.runtime, value).fdivDouble(context, (RubyBignum) y);
        }
        if (y instanceof RubyFloat) {
            return context.runtime.newFloat(((double) value) / ((RubyFloat) y).getDoubleValue());
        }
        return coerceBin(context, sites(context).fdiv, y);
    }

    @Override
    public IRubyObject isNegative(ThreadContext context) {
        Ruby runtime = context.runtime;
        CachingCallSite op_lt_site = sites(context).basic_op_lt;
        if (op_lt_site.retrieveCache(metaClass).method.isBuiltin()) {
            return runtime.newBoolean(value < 0);
        }
        return op_lt_site.call(context, this, this, RubyFixnum.zero(runtime));
    }

    @Override
    public IRubyObject isPositive(ThreadContext context) {
        Ruby runtime = context.runtime;
        CachingCallSite op_gt_site = sites(context).basic_op_gt;
        if (op_gt_site.retrieveCache(metaClass).method.isBuiltin()) {
            return runtime.newBoolean(value > 0);
        }
        return op_gt_site.call(context, this, this, RubyFixnum.zero(runtime));
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
    public RubyRational convertToRational() {
        final Ruby runtime = getRuntime();
        return RubyRational.newRationalRaw(runtime, this, one(runtime));
    }

    @Override
    public IRubyObject remainder(ThreadContext context, IRubyObject y) {
        return numRemainder(context, y);
    }

    // MRI: rb_int_s_isqrt, Fixnum portion
    @Override
    public IRubyObject sqrt(ThreadContext context) {
        Ruby runtime = context.runtime;

        if (isNegative()) {
            throw runtime.newMathDomainError("Numerical argument is out of domain - isqrt");
        }

        long n = value;
        // FIXME: this is obviously not super efficient, but floorSqrt(long) did not pass tests
        long sq = floorSqrt(BigInteger.valueOf(n)).longValue();
        
        return RubyFixnum.newFixnum(runtime, sq);
    }

    private static JavaSites.FixnumSites sites(ThreadContext context) {
        return context.sites.Fixnum;
    }

    /** rb_fix_induced_from
     *
     */
    @Deprecated
    public static IRubyObject induced_from(IRubyObject recv, IRubyObject other) {
        return RubyNumeric.num2fix(other);
    }
}
