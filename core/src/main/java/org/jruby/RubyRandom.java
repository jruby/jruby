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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Convert.*;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.Random;

/**
 * Implementation of the Random class.
 */
@JRubyClass(name = "Random")
public class RubyRandom extends RubyRandomBase {

    /**
     * Internal API, subject to change.
     */
    static final class RandomType {
        final RubyInteger seed;
        final Random impl;

        // RandomType(Ruby runtime) { this(randomSeed(runtime)); }

        // c: rand_init
        RandomType(IRubyObject seed) {
            this.seed = seed.convertToInteger();
            if (this.seed instanceof RubyFixnum) {
                this.impl = randomFromFixnum((RubyFixnum) this.seed);
            } else if (this.seed instanceof RubyBignum) {
                this.impl = randomFromBignum((RubyBignum) this.seed);
            } else {
                throw typeError(seed.getRuntime().getCurrentContext(), "failed to convert ",  seed, " into Integer");
            }
        }

        public static Random randomFromFixnum(RubyFixnum seed) {
            return randomFromLong(numericToLong(seed.getRuntime().getCurrentContext(), seed));
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
    public static RubyClass createRandomClass(ThreadContext context, RubyClass Object) {
        RubyClass RandomBase = RubyClass.newClass(context.runtime, Object).
                allocator(ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR).
                baseName("Base").
                defineMethods(context, RubyRandomBase.class);

        RubyClass Random = defineClass(context, "Random", RandomBase, RubyRandom::new).
                defineConstant(context, "Base", RandomBase).
                defineMethods(context, RubyRandom.class);

        var RandomFormatter = Random.defineModuleUnder(context, "Formatter").defineMethods(context, RandomFormatter.class);

        RandomBase.include(context, RandomFormatter);
        RandomFormatter.extend_object(context, RandomBase);

        return Random;
    }

    public static RubyRandom newRandom(Ruby runtime, RubyClass randomClass, IRubyObject seed) {
        RubyRandom random = new RubyRandom(runtime, randomClass, new RandomType(seed));

        return random;
    }

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

    @JRubyMethod(meta = true)
    public static IRubyObject seed(ThreadContext context, IRubyObject self) {
        return getDefaultRand(context).getSeed();
    }

    @JRubyMethod(name = "initialize_copy", visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject orig) {
        if (orig instanceof RubyRandom rand) {
            checkFrozen();
            random = new RandomType(rand.random);
            return this;
        }
        throw typeError(getRuntime().getCurrentContext(), orig, "Random");
    }

    // MRI: random_s_rand
    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv) {
        RandomType random = getDefaultRand(context);
        return randFloat(context, random);
    }

    // MRI: random_s_rand
    @JRubyMethod(name = "rand", meta = true)
    public static IRubyObject randDefault(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        RandomType random = getDefaultRand(context);
        IRubyObject v = randRandom(context, recv, random, arg);
        checkRandomNumber(context, v, arg);
        return v;
    }

    static IRubyObject randKernel(ThreadContext context, IRubyObject self, IRubyObject arg) {
        RandomType random = getDefaultRand(context);
        if (arg == context.nil) {
            return randFloat(context, random);
        }

        if (arg instanceof RubyRange) {
            IRubyObject v = randRandom(context, self, random, arg);
            return v;
        }

        RubyInteger max = arg.convertToInteger();
        if (max.isZero()) {
            return randFloat(context, random);
        }
        IRubyObject r = randInt(context, self, random, max, false);
        return (r == context.nil) ? randFloat(context, random) : r;
    }

    @JRubyMethod(name = "default", meta = true)
    public static IRubyObject rbDefault(ThreadContext context, IRubyObject self) {
        return context.runtime.getDefaultRandom();
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

    // c: random_equal
    @Override
    @JRubyMethod(name = "==")
    public IRubyObject op_equal(ThreadContext context, IRubyObject obj) {
        if (!getType().equals(obj.getType())) {
            return context.fals;
        }
        return asBoolean(context, random.equals(((RubyRandom) obj).random));
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
        RubyInteger left = asFixnum(context, (long) random.getLeft());
        var dump = newArray(context, state, left, random.getSeed());
        if (hasVariables()) dump.syncVariables(this);
        return dump;
    }

    // c: marshal_load
    @JRubyMethod()
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        if (load.size() != 3) throw argumentError(context, "wrong dump data");

        RubyBignum state = castAsBignum(context, load.eltInternal(0));
        int left = RubyNumeric.num2int(load.eltInternal(1));
        IRubyObject seed = load.eltInternal(2);

        checkFrozen();

        random = new RandomType(seed, state, left);
        if (load.hasVariables()) syncVariables((IRubyObject) load);

        setFrozen(true);

        return this;
    }

    // c: random_s_bytes
    @JRubyMethod(meta = true)
    public static IRubyObject bytes(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return bytesCommon(context, getDefaultRand(context), arg);
    }

    @JRubyMethod(name = "new_seed", meta = true)
    public static IRubyObject newSeed(ThreadContext context, IRubyObject recv) {
        return randomSeed(context.runtime);
    }

    @JRubyMethod(name = "urandom", meta = true)
    public static IRubyObject urandom(ThreadContext context, IRubyObject recv, IRubyObject num) {
        int n = num.convertToInteger().getIntValue();

        if (n < 0) throw argumentError(context, "negative string size (or size too big)");

        if (n == 0) return context.runtime.newString();

        byte[] seed = new byte[n];
        context.runtime.random.nextBytes(seed);

        return RubyString.newString(context.runtime, seed);
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
            else if (v == context.fals) invalidArgument(context, arg0);
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
        return switch (args.length) {
            case 0 -> randDefault(context, recv);
            case 1 -> randDefault(context, recv, args[0]);
            default -> throw argumentError(context, args.length, 0, 1);
        };
    }

    @Deprecated
    public IRubyObject randObj(ThreadContext context, IRubyObject[] args) {
        return (args.length == 0) ? rand(context) : rand(context, args[0]);
    }
}
