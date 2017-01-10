/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * Copyright the JRuby team.
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.algorithms.Randomizer;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.RopeOperations;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public abstract class RandomizerPrimitiveNodes {

    @Primitive(name = "randomizer_allocate", needsSelf = false)
    public static abstract class RandomizerAllocatePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject randomizerAllocate() {
            return Layouts.RANDOMIZER.createRandomizer(coreLibrary().getRandomizerFactory(), new Randomizer());
        }

    }

    @Primitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyBignum(seed)")
        public DynamicObject randomizerSeed(DynamicObject randomizer, DynamicObject seed) {
            Layouts.RANDOMIZER.setRandomizer(randomizer, randomFromBigInteger(Layouts.BIGNUM.getValue(seed)));
            return randomizer;
        }

        @Specialization
        public DynamicObject randomizerSeed(DynamicObject randomizer, long seed) {
            Layouts.RANDOMIZER.setRandomizer(randomizer, randomFromLong(seed));
            return randomizer;
        }

        @TruffleBoundary
        protected static Randomizer randomFromLong(long seed) {
            return Randomizer.randomFromLong(seed);
        }

        public static int N = 624;

        @TruffleBoundary
        public static Randomizer randomFromBigInteger(BigInteger big) {
            if (big.signum() < 0) {
                big = big.abs();
            }
            byte[] buf = big.toByteArray();
            int buflen = buf.length;
            if (buf[0] == 0) {
                buflen -= 1;
            }
            int len = Math.min((buflen + 3) / 4, N);
            int[] ints = bigEndianToInts(buf, len);
            if (len <= 1) {
                return new Randomizer(ints[0]);
            } else {
                return new Randomizer(ints);
            }
        }

        private static int[] bigEndianToInts(byte[] buf, int initKeyLen) {
            int[] initKey = new int[initKeyLen];
            for (int idx = 0; idx < initKey.length; ++idx) {
                initKey[idx] = getIntBigIntegerBuffer(buf, idx);
            }
            return initKey;
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

    }

    @Primitive(name = "randomizer_rand_float")
    public static abstract class RandomizerRandFloatPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public double randomizerRandFloat(DynamicObject randomizer) {
            // Logic copied from org.jruby.util.Random
            final Randomizer r = Layouts.RANDOMIZER.getRandomizer(randomizer);
            final int a = randomInt(r) >>> 5;
            final int b = randomInt(r) >>> 6;
            return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0);
        }

    }

    @Primitive(name = "randomizer_rand_int")
    public static abstract class RandomizerRandIntPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public int randomizerRandInt(DynamicObject randomizer, int limit) {
            final Randomizer r = Layouts.RANDOMIZER.getRandomizer(randomizer);
            return (int) randInt(r, limit);
        }

        @Specialization
        public long randomizerRandInt(DynamicObject randomizer, long limit) {
            final Randomizer r = Layouts.RANDOMIZER.getRandomizer(randomizer);
            return randInt(r, limit);
        }

        @Specialization(guards = "isRubyBignum(limit)")
        public Object randomizerRandInt(DynamicObject randomizer, DynamicObject limit,
                @Cached("new()") FixnumOrBignumNode fixnumOrBignum) {
            final Randomizer r = Layouts.RANDOMIZER.getRandomizer(randomizer);
            return fixnumOrBignum.fixnumOrBignum(randLimitedBignum(r, Layouts.BIGNUM.getValue(limit)));
        }

        @TruffleBoundary
        protected static long randInt(Randomizer r, long limit) {
            return randLimitedFixnumInner(r, limit);
        }

        public static long randLimitedFixnumInner(Randomizer randomizer, long limit) {
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
                            val |= (randomizer.genrandInt32() & 0xffffffffL) << (i * 32);
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

        private static BigInteger randLimitedBignum(Randomizer randomizer, BigInteger limit) {
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
                        rnd = (randomizer.genrandInt32() & 0xffffffffL) & mask;
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
            return new BigInteger(bytes);
        }

        private static int getIntBigIntegerBuffer(byte[] src, int loc) {
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

        private static void setIntBigIntegerBuffer(byte[] dest, int loc, int value) {
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

        private static long makeMask(long x) {
            x = x | x >>> 1;
            x = x | x >>> 2;
            x = x | x >>> 4;
            x = x | x >>> 8;
            x = x | x >>> 16;
            x = x | x >>> 32;
            return x;
        }

    }

    @Primitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends PrimitiveArrayArgumentsNode {

        // Single instance of Random per host VM
        private static final Random RANDOM;

        static {
            try {
                RANDOM = TruffleOptions.AOT ? SecureRandom.getInstance("SHA1PRNG") : new SecureRandom();
            } catch (NoSuchAlgorithmException e) {
                throw new UnsupportedOperationException(e);
            }
        };

        @TruffleBoundary
        @Specialization
        public DynamicObject randomizerGenSeed(DynamicObject randomizerClass) {
            final BigInteger seed = randomSeedBigInteger(RANDOM);
            return createBignum(seed);
        }

        private static final int DEFAULT_SEED_CNT = 4;

        public static BigInteger randomSeedBigInteger(java.util.Random random) {
            byte[] seed = new byte[DEFAULT_SEED_CNT * 4];
            random.nextBytes(seed);
            return new BigInteger(seed).abs();
        }

    }

    @Primitive(name = "randomizer_bytes", lowerFixnum = 1)
    public static abstract class RandomizerBytesPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject genRandBytes(DynamicObject randomizer, int length) {
            final Randomizer random = Layouts.RANDOMIZER.getRandomizer(randomizer);
            final byte[] bytes = new byte[length];
            int idx = 0;
            for (; length >= 4; length -= 4) {
                int r = random.genrandInt32();
                for (int i = 0; i < 4; ++i) {
                    bytes[idx++] = (byte) (r & 0xff);
                    r >>>= 8;
                }
            }
            if (length > 0) {
                int r = random.genrandInt32();
                for (int i = 0; i < length; ++i) {
                    bytes[idx++] = (byte) (r & 0xff);
                    r >>>= 8;
                }
            }
            return createString(RopeOperations.create(bytes, ASCIIEncoding.INSTANCE, CodeRange.CR_UNKNOWN));
        }
    }

    @TruffleBoundary
    private static int randomInt(Randomizer randomizer) {
        return randomizer.genrandInt32();
    }

}
