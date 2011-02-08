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

import java.math.BigInteger;
import java.security.SecureRandom;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.CompatVersion.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Random;
import org.jruby.util.TypeConverter;

/**
 * Implementation of the Random class.
 */
@JRubyClass(name = "Random")
public class RubyRandom extends RubyObject {

    public static class RandomType {
        private final IRubyObject seed;
        private final Random mt;

        RandomType(Ruby runtime) {
            this(randomSeed(runtime));
        }

        // c: rand_init
        RandomType(IRubyObject vseed) {
            this.seed = vseed.convertToInteger();
            if (seed instanceof RubyFixnum) {
                int v = (int) (RubyNumeric.num2long(seed) & 0xFFFFFFFFL);
                this.mt = new Random(v);
            } else if (seed instanceof RubyBignum) {
                byte[] buf = ((RubyBignum) seed).getBigIntegerValue().toByteArray();
                this.mt = new Random(bigEndianToInts(buf));
            } else {
                throw vseed.getRuntime().newTypeError(
                        String.format("failed to convert %s into Integer", vseed.getMetaClass()
                                .getName()));
            }
        }

        RandomType(IRubyObject vseed, RubyBignum state, int left) {
            this.seed = vseed.convertToInteger();
            byte[] bytes = state.getBigIntegerValue().toByteArray();
            int[] ints = new int[bytes.length / 4];
            int idx = 0;
            for (int i = 0; i < ints.length; ++i) {
                int r = 0;
                for (int j = 0; j < 4; ++j) {
                    r |= (bytes[idx++] & 0xff) << (j * 4);
                }
                ints[i] = r;
            }
            this.mt = new Random(ints, left);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof RandomType)) {
                return false;
            }
            RandomType rhs = (RandomType) obj;
            return seed.op_equal(seed.getRuntime().getCurrentContext(), rhs.seed).isTrue()
                    && mt.equals(rhs.mt);
        }

        @Override
        public int hashCode() {
            // Using 17 as the initializer, 37 as the multiplier.
            return (629 + seed.hashCode()) * 37 + mt.hashCode();
        }

        RandomType(RandomType orig) {
            this.seed = orig.seed;
            this.mt = new Random(orig.mt);
        }

        int genrandInt32() {
            return mt.genrandInt32();
        }

        double genrandReal() {
            return mt.genrandReal();
        }

        IRubyObject getSeed() {
            return seed;
        }

        RubyBignum getState() {
            int[] ints = mt.getState();
            byte[] bytes = new byte[ints.length * 4];
            int idx = 0;
            for (int r : ints) {
                for (int i = 0; i < 4; ++i) {
                    bytes[idx++] = (byte) (r & 0xff);
                    r >>>= 4;
                }
            }
            return RubyBignum.newBignum(seed.getRuntime(), new BigInteger(bytes));
        }

        int getLeft() {
            return mt.getLeft();
        }

        // big endian of bytes to reversed ints
        private int[] bigEndianToInts(byte[] buf) {
            int[] initKey = new int[(buf.length + 3) / 4];
            for (int idx = 0; idx < initKey.length; ++idx) {
                initKey[idx] = getIntBigIntegerBuffer(buf, idx);
            }
            return initKey;
        }
    }
    
    static int getIntBigIntegerBuffer(byte[] src, int loc) {
        int v = 0;
        int idx = src.length - loc * 4 - 1;
        if (idx >= 0) {
            v |= (src[idx--] & 0xff);
            if (idx >= 0) {
                v |= (src[idx--] & 0xff) << 8;
                if (idx >= 0) {
                    v |= (src[idx--] & 0xff) << 16;
                    if (idx >= 0) {
                        v |= (src[idx--] & 0xff) << 24;
                    }
                }
            }
        }
        return v;
    }

    static void setIntBigIntegerBuffer(byte[] dest, int loc, int value) {
        int idx = dest.length - loc * 4 - 1;
        if (idx >= 0) {
            dest[idx--] = (byte) (value & 0xff);
            if (idx >= 0) {
                dest[idx--] = (byte) ((value >> 8) & 0xff);
                if (idx >= 0) {
                    dest[idx--] = (byte) ((value >> 16) & 0xff);
                    if (idx >= 0) {
                        dest[idx--] = (byte) ((value >> 24) & 0xff);
                    }
                }
            }
        }
    }

    // TODO: Need to have randomSeed for each VM?
    private static SecureRandom randomSeed = new SecureRandom();

    private static final int DEFAULT_SEED_CNT = 4;
    
    // c: random_seed
    public static RubyBignum randomSeed(Ruby runtime) {
        byte[] seed = new byte[DEFAULT_SEED_CNT * 4];
        randomSeed.nextBytes(seed);
        return RubyBignum.newBignum(runtime, (new BigInteger(seed)).abs());
    }

    public static RubyClass createRandomClass(Ruby runtime) {
        RubyClass randomClass = runtime
                .defineClass("Random", runtime.getObject(), RANDOM_ALLOCATOR);
        randomClass.defineAnnotatedMethods(RubyRandom.class);
        RubyRandom defaultRand = new RubyRandom(runtime, randomClass);
        defaultRand.random = new RandomType(randomSeed(runtime));
        randomClass.setConstant("DEFAULT", defaultRand);
        runtime.setDefaultRand(defaultRand.random);
        return randomClass;
    }

    private static ObjectAllocator RANDOM_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyRandom(runtime, klass);
        }
    };

    private RandomType random = null;

    RubyRandom(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(visibility = PRIVATE, optional = 1, compat = RUBY1_9)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        random = new RandomType((args.length == 0) ? randomSeed(context.runtime) : args[0]);
        return this;
    }

    @JRubyMethod(name = "seed", compat = RUBY1_9)
    public IRubyObject seed(ThreadContext context) {
        return random.getSeed();
    }

    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        if (!(orig instanceof RubyRandom)) {
            throw getRuntime().newTypeError(
                    String.format("wrong argument type %s (expected %s)", orig.getMetaClass()
                            .getName(), getMetaClass().getName()));
        }
        random = new RandomType(((RubyRandom) orig).random);
        return this;
    }

    @JRubyMethod(name = "rand", meta = true, optional = 1, compat = RUBY1_9)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return randCommon19(context, recv, args);
    }

    // c: rb_f_rand
    public static IRubyObject randCommon19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RandomType random = getDefaultRand(context);
        if (args.length == 0) {
            return randFloat(context, random);
        }
        IRubyObject arg = args[0];
        if (arg.isNil()) {
            return randFloat(context, random);
        }
        // 1.9 calls rb_to_int
        RubyInteger max = arg.convertToInteger();
        return randCommon(context, random, max);
    }

    // c: rb_f_rand for 1.8
    public static IRubyObject randCommon18(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        RandomType random = getDefaultRand(context);
        if (args.length == 0) {
            return randFloat(context, random);
        }
        IRubyObject arg = args[0];
        if (arg.isNil()) {
            return randFloat(context, random);
        }
        // 1.8 calls rb_Integer()
        RubyInteger max = (RubyInteger) RubyKernel.new_integer(context, recv, arg);
        return randCommon(context, random, max);
    }

    private static IRubyObject randCommon(ThreadContext context, RandomType random, RubyInteger max) {
        if (max.zero_p(context).isTrue()) {
            return randFloat(context, random);
        }
        IRubyObject r = randInt(context, random, max, false);
        if (r.isNil()) {
            return randFloat(context, random);
        }
        return r;
    }
    
    @JRubyMethod(name = "rand", compat = RUBY1_9)
    public IRubyObject randObj(ThreadContext context) {
        return randFloat(context, random);
    }

    @JRubyMethod(name = "rand", compat = RUBY1_9)
    public IRubyObject randObj(ThreadContext context, IRubyObject arg0) {
        return randomRand(context, arg0, random);
    }

    // c: rand_int
    private static IRubyObject randInt(ThreadContext context, RandomType random, RubyInteger vmax,
            boolean restrictive) {
        if (vmax instanceof RubyFixnum) {
            long max = RubyNumeric.fix2long(vmax);
            if (max == 0) {
                return context.nil;
            }
            if (max < 0) {
                if (restrictive) {
                    return context.nil;
                }
                max = -max;
            }
            return randLimitedFixnum(context, random, max - 1);
        } else {
            BigInteger big = vmax.getBigIntegerValue();
            if (big.equals(BigInteger.ZERO)) {
                return context.nil;
            }
            if (big.signum() < 0) {
                if (restrictive) {
                    return context.nil;
                }
                big = big.abs();
            }
            big = big.subtract(BigInteger.ONE);
            return randLimitedBignum(context, random, RubyBignum.newBignum(context.runtime, big));
        }
    }

    public static RubyFloat randFloat(ThreadContext context, RandomType random) {
        return context.runtime.newFloat(random.genrandReal());
    }

    // c: limited_rand
    // limited_rand gets/returns unsigned long but we do this in signed long only.
    private static IRubyObject randLimitedFixnum(ThreadContext context, RandomType random,
            long limit) {
        long val;
        if (limit == 0) {
            val = 0;
        } else {
            long mask = makeMask(limit);
            retry: while (true) {
                val = 0;
                for (int i = 1; 0 <= i; --i) {
                    if (((mask >>> (i * 32)) & 0xffffffffL) != 0) {
                        val |= (random.genrandInt32() & 0xffffffffL) << (i * 32);
                        val &= mask;
                    }
                    if (limit < val) {
                        continue retry;
                    }
                }
                break;
            }
        }
        return context.runtime.newFixnum(val);
    }

    // c: limited_big_rand
    private static IRubyObject randLimitedBignum(ThreadContext context, RandomType random,
            RubyBignum limit) {
        byte[] buf = limit.getBigIntegerValue().toByteArray();
        byte[] bytes = new byte[buf.length];
        int len = (buf.length + 3) / 4;
        retry: while (true) {
            long mask = 0;
            boolean boundary = true;
            for (int idx = len - 1; 0 <= idx; --idx) {
                long lim = getIntBigIntegerBuffer(buf, idx) & 0xffffffffL;
                mask = (mask != 0) ? 0xffffffffL : makeMask(lim);
                long rnd;
                if (mask != 0) {
                    rnd = (random.genrandInt32() & 0xffffffffL) & mask;
                    if (boundary) {
                        if (lim < rnd) {
                            continue retry;
                        }
                        if (rnd < lim) {
                            boundary = false;
                        }
                    }
                } else {
                    rnd = 0;
                }
                setIntBigIntegerBuffer(bytes, idx, (int) rnd);
            }
            break;
        }
        return RubyBignum.newBignum(context.runtime, new BigInteger(bytes));
    }

    private static long makeMask(long x) {
        x = x | x >>> 1;
        x = x | x >>> 2;
        x = x | x >>> 4;
        x = x | x >>> 8;
        x = x | x >>> 16;
        x = x | x >>> 32;
        return x;
    }

    private static RandomType getDefaultRand(ThreadContext context) {
        return context.runtime.getDefaultRand();
    }

    // c: random_rand
    private static IRubyObject randomRand(ThreadContext context, IRubyObject vmax, RandomType random) {
        IRubyObject v;
        if (vmax.isNil()) {
            v = context.nil;
        } else if (!(vmax instanceof RubyFloat)
                && (!(v = TypeConverter.checkIntegerType(context.runtime, vmax, "to_int")).isNil())) {
            v = randInt(context, random, (RubyInteger) v, true);
        } else if (!(v = TypeConverter.checkFloatType(context.runtime, vmax)).isNil()) {
            double max = ((RubyFloat) v).getValue();
            if (max > 0.0) {
                v = context.runtime.newFloat(max * random.genrandReal());
            } else {
                v = context.nil;
            }
        } else if (vmax instanceof RubyRange) { // RANGE ARGUMENT
            // TODO: implement

            RubyRange range = (RubyRange) vmax;
            IRubyObject first = range.first();
            IRubyObject last = range.last();

            boolean returnFloat = first instanceof RubyFloat || last instanceof RubyFloat;
            if (returnFloat) {
                first = first.convertToFloat();
                last = last.convertToFloat();
            }

            if (range.include_p19(context, last).isTrue() && (!returnFloat)) {
                last = last.callMethod(context, "+", context.runtime.newFixnum(1));
            }

            if (!first.respondsTo("-") || !first.respondsTo("+") || !last.respondsTo("-")
                    || !last.respondsTo("+")) {
                throw context.runtime.newArgumentError("invalid argument - " + vmax.toString());
            }
/*
 * IRubyObject difference = last.callMethod(context, "-", first); if
 * (returnFloat) { double max = (double) RubyNumeric.num2dbl(difference); double
 * rand = random.nextDouble() * ((RubyFloat) difference).getDoubleValue();
 * return RubyFloat.newFloat(runtime, ((RubyFloat) first).getDoubleValue() +
 * rand); } else { int max = (int) RubyNumeric.num2long(difference); int rand =
 * random.nextInt(max); return RubyNumeric.num2fix(first.callMethod(context,
 * "+", runtime.newFixnum(rand))); }
 */
        } else {
            v = context.nil;
        }
        if (v.isNil()) {
            throw context.runtime.newArgumentError("invalid argument - " + vmax.toString());
        }
        return v;
        // if (beg == Qundef) return v;

        /*
         * if (FIXNUM_P(beg) && FIXNUM_P(v)) { long x = FIX2LONG(beg) +
         * FIX2LONG(v); return LONG2NUM(x); } switch (TYPE(v)) { case T_BIGNUM:
         * return rb_big_plus(v, beg); case T_FLOAT: { VALUE f =
         * rb_check_to_float(beg); if (!NIL_P(f)) { RFLOAT_VALUE(v) +=
         * RFLOAT_VALUE(f); return v; } } default: return rb_funcall2(beg,
         * id_plus, 1, &v); }
         */

    }

    @JRubyMethod(meta = true, compat = RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return srandCommon(context, recv);
    }

    @JRubyMethod(meta = true, compat = RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject seed) {
        return srandCommon(context, recv, seed);
    }

    // c: rb_f_srand
    public static IRubyObject srandCommon(ThreadContext context, IRubyObject recv) {
        return srandCommon(context, recv, randomSeed(context.runtime));
    }

    // c: rb_f_srand
    public static IRubyObject srandCommon(ThreadContext context, IRubyObject recv,
            IRubyObject newSeed) {
        RandomType defaultRand = getDefaultRand(context);
        IRubyObject previousSeed = defaultRand.getSeed();
        defaultRand = new RandomType(newSeed);
        context.runtime.setDefaultRand(defaultRand);
        if (context.runtime.is1_9()) {
            ((RubyRandom) ((RubyModule) RuntimeHelpers.getConstant(context, "Random"))
                    .getConstant("DEFAULT")).setRandomType(defaultRand);
        }
        return previousSeed;
    }

    // c: random_equal
    @Override
    @JRubyMethod(name = "==", required = 1, compat = RUBY1_9)
    public IRubyObject op_equal_19(ThreadContext context, IRubyObject obj) {
        if (!getType().equals(obj.getType())) {
            return context.runtime.getFalse();
        }
        return context.runtime.newBoolean(random.equals(((RubyRandom) obj).random));
    }

    // c: random_dump
    @JRubyMethod(name = "marshal_dump", backtrace = true, compat = RUBY1_9)
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyBignum state = random.getState();
        RubyInteger left = (RubyInteger) RubyNumeric.int2fix(context.runtime, random.getLeft());
        RubyArray dump = context.getRuntime().newArray(state, left, random.getSeed());
        if (hasVariables()) {
            dump.syncVariables(this);
        }
        return dump;
    }

    // c: marshal_load
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        if (load.size() != 3) {
            throw context.runtime.newArgumentError("wrong dump data");
        }
        if (!(load.eltInternal(0) instanceof RubyBignum)) {
            throw context.runtime.newTypeError(load.eltInternal(0), context.runtime.getBignum());
        }
        RubyBignum state = (RubyBignum) load.eltInternal(0);
        int left = RubyNumeric.num2int(load.eltInternal(1));
        IRubyObject seed = load.eltInternal(2);
        random = new RandomType(seed, state, left);
        if (load.hasVariables()) {
            syncVariables((IRubyObject) load);
        }
        return this;
    }

    // c: rb_random_bytes
    @JRubyMethod(name = "bytes", compat = RUBY1_9)
    public IRubyObject bytes(ThreadContext context, IRubyObject arg) {
        int n = RubyNumeric.num2int(arg);
        byte[] bytes = new byte[n];
        int idx = 0;
        for (; n >= 4; n -= 4) {
            int r = random.genrandInt32();
            for (int i = 0; i < 4; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 4;
            }
        }
        if (n > 0) {
            int r = random.genrandInt32();
            for (int i = 0; i < n; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 4;
            }
        }
        return context.getRuntime().newString(new ByteList(bytes));
    }

    @JRubyMethod(name = "new_seed", meta = true, compat = RUBY1_9)
    public static IRubyObject newSeed(ThreadContext context, IRubyObject recv) {
        return randomSeed(context.runtime);
    }
    
    private void setRandomType(RandomType random) {
        this.random = random;
    }
}
