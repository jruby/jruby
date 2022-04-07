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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.Random;
import org.jruby.util.TypeConverter;

import static org.jruby.util.TypeConverter.toFloat;

/**
 * Implementation of the Random class.
 */
@JRubyClass(name = "Random")
public class RubyRandom extends RubyObject {

    /**
     * Internal API, subject to change.
     */
    public static final class RandomType {
        private final RubyInteger seed;
        private final Random impl;

        // RandomType(Ruby runtime) { this(randomSeed(runtime)); }

        // c: rand_init
        RandomType(IRubyObject seed) {
            this.seed = seed.convertToInteger();
            if (this.seed instanceof RubyFixnum) {
                this.impl = randomFromFixnum((RubyFixnum) this.seed);
            } else if (this.seed instanceof RubyBignum) {
                this.impl = randomFromBignum((RubyBignum) this.seed);
            } else {
                throw seed.getRuntime().newTypeError(
                        String.format("failed to convert %s into Integer", seed.getMetaClass().getName()));
            }
        }

        public static Random randomFromFixnum(RubyFixnum seed) {
            return randomFromLong(RubyNumeric.num2long(seed));
        }

        public static Random randomFromLong(long seed) {
            long v = Math.abs(seed);
            if (v == (v & 0xffffffffL)) {
                return new Random((int) v);
            } else {
                int[] ints = new int[2];
                ints[0] = (int) v;
                ints[1] = (int) (v >> 32);
                return new Random(ints);
            }
        }

        public static Random randomFromBignum(RubyBignum seed) {
            BigInteger big = seed.getBigIntegerValue();
            return randomFromBigInteger(big);
        }

        public static Random randomFromBigInteger(BigInteger big) {
            if (big.signum() < 0) {
                big = big.abs();
            }
            byte[] buf = big.toByteArray();
            int buflen = buf.length;
            if (buf[0] == 0) {
                buflen -= 1;
            }
            int len = Math.min((buflen + 3) / 4, Random.N);
            int[] ints = bigEndianToInts(buf, len);
            if (len <= 1) {
                return new Random(ints[0]);
            } else {
                return new Random(ints);
            }
        }

        RandomType(IRubyObject vseed, RubyBignum state, int left) {
            this.seed = vseed.convertToInteger();
            byte[] bytes = state.getBigIntegerValue().toByteArray();
            int[] ints = new int[bytes.length / 4];
            for (int i = 0; i < ints.length; ++i) {
                ints[i] = getIntBigIntegerBuffer(bytes, i);
            }
            this.impl = new Random(ints, left);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RandomType)) return false;
            RandomType rhs = (RandomType) obj;
            return seed.op_equal(seed.getRuntime().getCurrentContext(), rhs.seed).isTrue() && impl.equals(rhs.impl);
        }

        @Override
        public int hashCode() {
            // Using 17 as the initializer, 37 as the multiplier.
            return (629 + seed.hashCode()) * 37 + impl.hashCode();
        }

        RandomType(RandomType orig) {
            this.seed = orig.seed;
            this.impl = new Random(orig.impl);
        }

        int genrandInt32() {
            return impl.genrandInt32();
        }

        double genrandReal() {
            return impl.genrandReal();
        }

        double genrandReal(boolean excl) {
            if (excl) {
                return impl.genrandReal();
            } else {
                return impl.genrandReal2();
            }
        }

        double genrandReal2() {
            return impl.genrandReal2();
        }

        RubyInteger getSeed() {
            return seed;
        }

        RubyBignum getState() {
            int[] ints = impl.getState();
            byte[] bytes = new byte[ints.length * 4];
            for (int idx = 0; idx < ints.length; ++idx) {
                setIntBigIntegerBuffer(bytes, idx, ints[idx]);
            }
            return RubyBignum.newBignum(seed.getRuntime(), new BigInteger(bytes));
        }

        int getLeft() {
            return impl.getLeft();
        }

        // big endian of bytes to reversed ints
        private static int[] bigEndianToInts(byte[] buf, int initKeyLen) {
            int[] initKey = new int[initKeyLen];
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

    private static final int DEFAULT_SEED_CNT = 4;

    public static BigInteger randomSeedBigInteger(java.util.Random random) {
        byte[] seed = new byte[DEFAULT_SEED_CNT * 4];
        random.nextBytes(seed);
        return new BigInteger(seed).abs();
    }

    // c: random_seed
    public static RubyBignum randomSeed(Ruby runtime) {
        return RubyBignum.newBignum(runtime, randomSeedBigInteger(runtime.random));
    }

    @SuppressWarnings("deprecation")
    public static RubyClass createRandomClass(Ruby runtime) {
        RubyClass randomClass = runtime
                .defineClass("Random", runtime.getObject(), RubyRandom::new);

        randomClass.defineAnnotatedMethods(RubyRandom.class);
        randomClass.defineConstant("DEFAULT", randomClass);
        randomClass.deprecateConstant(runtime, "DEFAULT");

        runtime.setDefaultRandom(newRandom(runtime, randomClass, randomSeed(runtime)));

        RubyModule formatterModule = randomClass.defineModuleUnder("Formatter");
        randomClass.includeModule(formatterModule);
        formatterModule.extend_object(randomClass);
        formatterModule.defineAnnotatedMethods(RandomFormatter.class);

        return randomClass;
    }

    public static RubyRandom newRandom(Ruby runtime, RubyClass randomClass, IRubyObject seed) {
        RubyRandom random = new RubyRandom(runtime, randomClass, new RandomType(seed));

        return random;
    }

    private RandomType random = null;

    public RandomType getRandomType() {
        return random;
    }

    RubyRandom(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    RubyRandom(Ruby runtime, RubyClass rubyClass, RandomType randomType) {
        super(runtime, rubyClass);

        this.random = randomType;
    }

    @JRubyMethod(visibility = PRIVATE, optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        checkFrozen();

        random = new RandomType((args.length == 0) ? randomSeed(context.runtime) : args[0]);

        return this;
    }

    @JRubyMethod
    public IRubyObject seed(ThreadContext context) {
        return random.getSeed();
    }

    @JRubyMethod(meta = true)
    public static IRubyObject seed(ThreadContext context, IRubyObject self) {
        return getDefaultRand(context).getSeed();
    }

    @JRubyMethod(name = "initialize_copy", required = 1, visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        if (!(orig instanceof RubyRandom)) {
            throw getRuntime().newTypeError(String.format(
                    "wrong argument type %s (expected %s)", orig.getMetaClass().getName(), getMetaClass().getName())
            );
        }
        checkFrozen();
        random = new RandomType(((RubyRandom) orig).random);
        return this;
    }

    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv) {
        RandomType random = getDefaultRand(context);
        return randFloat(context, random);
    }

    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RandomType random = getDefaultRand(context);
        return randomRand(context, recv, arg, random);
    }

    static IRubyObject randKernel(ThreadContext context, IRubyObject self, IRubyObject arg) {
        RandomType random = getDefaultRand(context);
        if (arg == context.nil) {
            return randFloat(context, random);
        }

        if (arg instanceof RubyRange) {
            return randomRand(context, self, arg, random);
        }

        RubyInteger max = arg.convertToInteger();
        if (max.isZero()) {
            return randFloat(context, random);
        }
        IRubyObject r = randInt(context, random, max, false);
        return (r == context.nil) ? randFloat(context, random) : r;
    }

    @Deprecated
    public IRubyObject randObj(ThreadContext context, IRubyObject[] args) {
        return (args.length == 0) ? rand(context) : rand(context, args[0]);
    }

    @JRubyMethod(name = "rand")
    public IRubyObject rand(ThreadContext context) {
        return randFloat(context, random);
    }

    @JRubyMethod(name = "rand")
    public IRubyObject rand(ThreadContext context, IRubyObject arg) {
        IRubyObject v = randomRand(context, this, arg, random);
        checkRandomNumber(context, v, arg);
        return v;
    }

    @JRubyMethod(name = "default", meta = true)
    public static IRubyObject rbDefault(ThreadContext context, IRubyObject self) {
        return context.runtime.getDefaultRandom();
    }

    // c: rand_int
    private static IRubyObject randInt(ThreadContext context, RandomType random, RubyInteger vmax,
            boolean restrictive) {
        if (vmax instanceof RubyFixnum) {
            long max = RubyNumeric.fix2long(vmax);
            if (max == 0) return context.nil;
            if (max < 0) {
                if (restrictive) return context.nil;
                max = -max;
            }
            return randLimitedFixnum(context, random, max - 1);
        } else {
            BigInteger big = vmax.getBigIntegerValue();
            if (big.equals(BigInteger.ZERO)) {
                return context.nil;
            }
            if (big.signum() < 0) {
                if (restrictive) return context.nil;
                big = big.abs();
            }
            big = big.subtract(BigInteger.ONE);
            return randLimitedBignum(context, random, big);
        }
    }

    public static RubyFloat randFloat(ThreadContext context) {
        return randFloat(context, getDefaultRand(context));
    }

    public static RubyFloat randFloat(ThreadContext context, RandomType random) {
        return context.runtime.newFloat(random.genrandReal());
    }

    public static RubyInteger randLimited(ThreadContext context, long limit) {
        return randLimitedFixnum(context, getDefaultRand(context), limit);
    }

    // c: limited_rand
    // limited_rand gets/returns ulong but we do this in signed long only.
    private static RubyInteger randLimitedFixnum(ThreadContext context, RandomType random, long limit) {
        Ruby runtime = context.runtime;

        if (limit == 0) return RubyFixnum.zero(runtime);
        Random impl;
        if (random == null) {
            impl = new Random();
        } else {
            impl = random.impl;
        }
        return RubyFixnum.newFixnum(runtime, randLimitedFixnumInner(impl, limit));
    }

    public static long randLimitedFixnumInner(Random random, long limit) {
        long val;
        if (limit == 0) {
            val = 0;
        } else {
            long mask = makeMask(limit);
            // take care before code cleanup; it might break random sequence compatibility
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
        return val;
    }

    public static RubyInteger randLimited(ThreadContext context, BigInteger limit) {
        return randLimitedBignum(context, getDefaultRand(context), limit);
    }

    // c: limited_big_rand
    private static RubyInteger randLimitedBignum(ThreadContext context, RandomType random, BigInteger limit) {
        byte[] buf = limit.toByteArray();
        byte[] bytes = new byte[buf.length];
        int len = (buf.length + 3) / 4;
        // take care before code cleanup; it might break random sequence compatibility
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
        return context.runtime.getDefaultRandom().random;
    }

    private static RubyRandom getDefaultRandom(Ruby runtime) {
        return runtime.getDefaultRandom();
    }

    // c: rand_random
    private static IRubyObject randomRand(ThreadContext context, IRubyObject self, IRubyObject vmax, RandomType random) {
        IRubyObject v = randRandom(context, self, random, vmax);
        return v;
    }

    private static void checkRandomNumber(ThreadContext context, IRubyObject v, IRubyObject arg0) {
        if (v == context.fals) {
            arg0.convertToInteger();
        } else if (v == context.nil) {
            invalidArgument(context, arg0);
        }
    }

    private static void invalidArgument(ThreadContext context, IRubyObject arg0) {
        throw context.runtime.newArgumentError("invalid argument - " + arg0);
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RandomType rnd) {
        return RubyFloat.newFloat(context.runtime, randomReal(context, obj, rnd, true));
    }

    // c: rand_random
    static IRubyObject randRandom(ThreadContext context, IRubyObject obj, RandomType rnd, IRubyObject vmax) {
        IRubyObject v;

        if (vmax.isNil()) return vmax;
        if (!(vmax instanceof RubyFloat)) {
            v = TypeConverter.checkIntegerType(context, vmax);
            if (!v.isNil()) return randInt(context, rnd, (RubyInteger) v, true);
        }
        Ruby runtime = context.runtime;
        v = TypeConverter.checkFloatType(runtime, vmax);
        if (!v.isNil()) {
            double max = floatValue(context, v);
            if (max < 0.0) {
                return context.nil;
            } else {
                double r = randomReal(context, obj, rnd, true);
                if (max > 0.0) r *= max;
                return RubyFloat.newFloat(runtime, r);
            }
        }

        return randRange(context, vmax, rnd);
    }

    private static double floatValue(ThreadContext context, IRubyObject v) {
        double value = v.convertToFloat().getDoubleValue();
        if (!Double.isFinite(value)) {
            domainError(context);
        }
        return value;
    }

    private static void domainError(ThreadContext context) {
        throw context.runtime.newErrnoEDOMError();
    }

    private static IRubyObject randRange(ThreadContext context, IRubyObject vmax, RandomType random) {
        IRubyObject v;
        IRubyObject nil = context.nil;
        IRubyObject beg;
        IRubyObject end;
        boolean excl;

        RubyBoolean fals = context.fals;
        RubyRange.RangeLike rangeLike = rangeValues(context, vmax);
        if (rangeLike == null) {
            return fals;
        }

        v = vmax = rangeLike.getRange(context);

        if (v.isNil()) domainError(context);

        Ruby runtime = context.runtime;

        beg = rangeLike.begin;
        end = rangeLike.end;
        excl = rangeLike.excl;

        if ((v = checkMaxInt(context, vmax)) != null) {
            do {
                if (v instanceof RubyFixnum) {
                    long max = ((RubyFixnum) v).value;
                    if (excl) {
                        max -= 1;
                    }
                    if (max >= 0) {
                        v = randLimitedFixnum(context, random, max);
                    } else {
                        v = nil;
                    }
                } else if (v instanceof RubyBignum) {
                    BigInteger big = ((RubyBignum) v).value;
                    if (big.signum() > 0) {
                        if (excl) {
                            big = big.subtract(BigInteger.ONE);
                        }
                        v = randLimitedBignum(context, random, big);
                    } else {
                        v = nil;
                    }
                } else {
                    v = nil;
                }
            } while (false);
        } else if ((v = TypeConverter.checkFloatType(runtime, vmax)) != nil) {
            int scale = 1;
            double max = ((RubyFloat) v).value;
            double mid = 0.5;
            double r;
            if (Double.isInfinite(max)) {
                double min = floatValue(context, toFloat(runtime, beg)) / 2.0;
                max = floatValue(context, toFloat(runtime, end)) / 2.0;
                scale = 2;
                mid = max + min;
                max -= min;
            } else {
                checkFloatValue(context, max); // v
            }
            v = nil;
            if (max > 0.0) {
                if (excl) {
                    r = random.genrandReal();
                } else {
                    r = random.genrandReal2();
                }
                if (scale > 1) {
                    return runtime.newFloat(+(+(+(r - 0.5) * max) * scale) + mid);
                }
                v = runtime.newFloat(r * max);
            } else if (max == 0.0 && !excl) {
                v = runtime.newFloat(0.0);
            }
        }

        if (beg instanceof RubyFixnum && v instanceof RubyFixnum) {
            long x = RubyNumeric.fix2long(beg) + RubyNumeric.fix2long(v);
            return RubyFixnum.newFixnum(runtime, x);
        }
        if (v == nil) {
            return v;
        } else if (v instanceof RubyBignum) {
            return ((RubyBignum) v).op_plus(context, beg);
        } else if (v instanceof RubyFloat) {
            IRubyObject f = TypeConverter.checkFloatType(runtime, beg);
            if (!f.isNil()) {
                return RubyFloat.newFloat(runtime, ((RubyFloat) v).getValue() + ((RubyFloat) f).getValue());
            }
        }
        return Helpers.invoke(context, beg, "+", v);
    }

    // c: float_value
    private static double floatValue(ThreadContext context, RubyFloat v) {
        final double x = v.value;
        checkFloatValue(context, x);
        return x;
    }

    private static void checkFloatValue(ThreadContext context, double x) {
        if (Double.isInfinite(x) || Double.isNaN(x)) {
            domainError(context);
        }
    }

    private static IRubyObject checkMaxInt(ThreadContext context, IRubyObject vmax) {
        if (!(vmax instanceof RubyFloat)) {
            IRubyObject v = TypeConverter.checkIntegerType(context, vmax);
            if (v != context.nil) return v;
        }
        return null;
    }

    static RubyRange.RangeLike rangeValues(ThreadContext context, IRubyObject range) {
        RubyRange.RangeLike like = RubyRange.rangeValues(context, range);
        if (like == null) return null;
        if (like.begin.isNil() || like.end.isNil()) domainError(context);
        return like;
    }

    @JRubyMethod(meta = true)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return srandCommon(context, recv);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject seed) {
        return srandCommon(context, recv, seed);
    }

    // c: rb_f_srand
    public static IRubyObject srandCommon(ThreadContext context, IRubyObject recv) {
        return srandCommon(context, recv, randomSeed(context.runtime));
    }

    // c: rb_f_srand
    public static IRubyObject srandCommon(ThreadContext context, IRubyObject recv, IRubyObject newSeed) {
        Ruby runtime = context.runtime;

        RubyRandom defaultRandom = getDefaultRandom(runtime);
        RubyInteger previousSeed = defaultRandom.getRandomType().getSeed();

        defaultRandom = newRandom(runtime, runtime.getRandomClass(), newSeed);
        context.runtime.setDefaultRandom(defaultRandom);

        return previousSeed;
    }

    @Deprecated
    public IRubyObject op_equal_19(ThreadContext context, IRubyObject obj) {
        return op_equal(context, obj);
    }

    // c: random_equal
    @Override
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (!getType().equals(obj.getType())) {
            return context.fals;
        }
        return RubyBoolean.newBoolean(context, random.equals(((RubyRandom) obj).random));
    }

    // c: random_state
    @JRubyMethod(name = "state", visibility = PRIVATE)
    public IRubyObject stateObj(ThreadContext context) {
        return random.getState();
    }

    // c: random_left
    @JRubyMethod(name = "left", visibility = PRIVATE)
    public IRubyObject leftObj(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, random.getLeft());
    }

    // c: random_s_state
    @JRubyMethod(name = "state", meta = true, visibility = PRIVATE)
    public static IRubyObject state(ThreadContext context, IRubyObject recv) {
        return getDefaultRand(context).getState();
    }

    // c: random_s_left
    @JRubyMethod(name = "left", meta = true, visibility = PRIVATE)
    public static IRubyObject left(ThreadContext context, IRubyObject recv) {
        return RubyNumeric.int2fix(context.runtime, getDefaultRand(context).getLeft());
    }

    // c: random_dump
    @JRubyMethod(name = "marshal_dump")
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyBignum state = random.getState();
        RubyInteger left = RubyFixnum.newFixnum(context.runtime, (long) random.getLeft());
        RubyArray dump = RubyArray.newArray(context.runtime, state, left, random.getSeed());
        if (hasVariables()) {
            dump.syncVariables(this);
        }
        return dump;
    }

    // c: marshal_load
    @JRubyMethod()
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

        checkFrozen();

        random = new RandomType(seed, state, left);
        if (load.hasVariables()) {
            syncVariables((IRubyObject) load);
        }

        setFrozen(true);

        return this;
    }

    // c: random_s_bytes
    @JRubyMethod(meta = true)
    public static IRubyObject bytes(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return bytesCommon(context, getDefaultRand(context), arg);
    }

    // c: rb_random_bytes
    @JRubyMethod(name = "bytes")
    public IRubyObject bytes(ThreadContext context, IRubyObject arg) {
        return bytesCommon(context, random, arg);
    }

    private static IRubyObject bytesCommon(ThreadContext context, RandomType random, IRubyObject arg) {
        int n = RubyNumeric.num2int(arg);
        byte[] bytes = objRandomBytes(random, n);
        return context.runtime.newString(new ByteList(bytes));
    }

    // MRI: obj_random_bytes
    static byte[] objRandomBytes(RandomType random, int n) {
        byte[] bytes = new byte[n];
        int idx = 0;
        for (; n >= 4; n -= 4) {
            int r = random.genrandInt32();
            for (int i = 0; i < 4; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 8;
            }
        }
        if (n > 0) {
            int r = random.genrandInt32();
            for (int i = 0; i < n; ++i) {
                bytes[idx++] = (byte) (r & 0xff);
                r >>>= 8;
            }
        }
        return bytes;
    }

    private static RandomType tryGetRandomType(ThreadContext context, IRubyObject obj) {
        if (obj.equals(context.runtime.getRandomClass())) return getDefaultRand(context);
        if (obj instanceof RubyRandom) return ((RubyRandom) obj).random;
        return null;
    }

    // rb_random_ulong_limited
    public static long randomLongLimited(ThreadContext context, IRubyObject obj, long limit) {
        RandomType rnd = tryGetRandomType(context, obj);

        if (rnd == null) {
            RubyInteger v = Helpers.invokePublic(context, obj, "rand", context.runtime.newFixnum(limit + 1)).convertToInteger();
            long r = RubyNumeric.num2long(v);
            if (r < 0) throw context.runtime.newRangeError("random number too small " + r);
            if (r > limit) throw context.runtime.newRangeError("random number too big " + r);

            return r;
        }

        return randLimitedFixnumInner(rnd.impl, limit);
    }

    // c: rb_random_real
    public static double randomReal(ThreadContext context, IRubyObject obj, boolean excl) {
        RandomType random = tryGetRandomType(context, obj);

        return randomReal(context, obj, random, excl);
    }

    private static double randomReal(ThreadContext context, IRubyObject obj, RandomType random, boolean excl) {
        if (random == null) random = getDefaultRand(context);

        return random.genrandReal(excl);
    }

    @JRubyMethod(name = "new_seed", meta = true)
    public static IRubyObject newSeed(ThreadContext context, IRubyObject recv) {
        return randomSeed(context.runtime);
    }

    @JRubyMethod(name = "urandom", meta = true)
    public static IRubyObject urandom(ThreadContext context, IRubyObject recv, IRubyObject num) {
        Ruby runtime = context.runtime;
        int n = num.convertToInteger().getIntValue();

        if (n < 0) throw runtime.newArgumentError("negative string size (or size too big)");

        if (n == 0) return runtime.newString();

        byte[] seed = new byte[n];
        runtime.random.nextBytes(seed);

        return RubyString.newString(runtime, seed);
    }

    public static class RandomFormatter {
        @JRubyMethod(name = {"rand", "random_number"})
        public static IRubyObject randomNumber(ThreadContext context, IRubyObject self) {
            RandomType rnd = tryGetRnd(context, self);
            IRubyObject v = randRandom(context, self, rnd);
//            else if (!v) invalid_argument(argv[0]);
            return v;
        }

        @JRubyMethod(name = {"rand", "random_number"})
        public static IRubyObject randomNumber(ThreadContext context, IRubyObject self, IRubyObject arg0) {
            RandomType rnd = tryGetRandomType(context, self);
            IRubyObject v = randRandom(context, self, rnd, arg0);
            if (v.isNil()) v = randRandom(context, self, rnd);
//            else if (!v) invalid_argument(argv[0]);
            return v;
        }

        // MRI: try_get_rnd
        static RandomType tryGetRnd(ThreadContext context, IRubyObject obj) {
            if (obj == context.runtime.getRandomClass()) {
                return getDefaultRand(context);
            }
            if (!(obj instanceof RubyRandom)) return null;
            return ((RubyRandom) obj).getRandomType();
        }
    }

    @Deprecated // not-used
    public static IRubyObject randCommon19(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return randKernel(context, args);
    }

    @Deprecated
    static IRubyObject randKernel(ThreadContext context, IRubyObject[] args) {
        RandomType random = getDefaultRand(context);
        if (args.length == 0) {
            return randFloat(context, random);
        }

        IRubyObject arg = args[0];
        return randKernel(context, context.runtime.getRandomClass(), arg);
    }

    @Deprecated
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return randDefault(context, recv);
            case 1:
                return randDefault(context, recv, args[0]);
            default:
                throw context.runtime.newArgumentError(args.length, 0, 1);
        }
    }
}
