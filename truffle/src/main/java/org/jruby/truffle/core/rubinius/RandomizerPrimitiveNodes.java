/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyRandom;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.RopeOperations;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public abstract class RandomizerPrimitiveNodes {

    @Primitive(name = "randomizer_allocate", needsSelf = false)
    public static abstract class RandomizerAllocatePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject randomizerAllocate() {
            return Layouts.RANDOMIZER.createRandomizer(coreLibrary().getRandomizerFactory(), new org.jruby.util.Random());
        }

    }

    @Primitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyBignum(seed)")
        public DynamicObject randomizerSeed(DynamicObject randomizer, DynamicObject seed) {
            Layouts.RANDOMIZER.setRandom(randomizer, randomFromBigInteger(Layouts.BIGNUM.getValue(seed)));
            return randomizer;
        }

        @Specialization
        public DynamicObject randomizerSeed(DynamicObject randomizer, long seed) {
            Layouts.RANDOMIZER.setRandom(randomizer, randomFromLong(seed));
            return randomizer;
        }

        @TruffleBoundary
        protected static org.jruby.util.Random randomFromBigInteger(BigInteger seed) {
            return RubyRandom.RandomType.randomFromBigInteger(seed);
        }

        @TruffleBoundary
        protected static org.jruby.util.Random randomFromLong(long seed) {
            return RubyRandom.RandomType.randomFromLong(seed);
        }

    }

    @Primitive(name = "randomizer_rand_float")
    public static abstract class RandomizerRandFloatPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public double randomizerRandFloat(DynamicObject randomizer) {
            // Logic copied from org.jruby.util.Random
            final org.jruby.util.Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            final int a = randomInt(r) >>> 5;
            final int b = randomInt(r) >>> 6;
            return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0);
        }

    }

    @Primitive(name = "randomizer_rand_int")
    public static abstract class RandomizerRandIntPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public int randomizerRandInt(DynamicObject randomizer, int limit) {
            final org.jruby.util.Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return (int) randInt(r, (long) limit);
        }

        @Specialization
        public long randomizerRandInt(DynamicObject randomizer, long limit) {
            final org.jruby.util.Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return randInt(r, limit);
        }

        @TruffleBoundary
        protected static long randInt(org.jruby.util.Random r, long limit) {
            return RubyRandom.randLimitedFixnumInner(r, limit);
        }

    }

    @Primitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends PrimitiveArrayArgumentsNode {

        // Single instance of Random per host VM
        private static final Random RANDOM = new SecureRandom();

        @TruffleBoundary
        @Specialization
        public DynamicObject randomizerGenSeed(DynamicObject randomizerClass) {
            final BigInteger seed = RubyRandom.randomSeedBigInteger(RANDOM);
            return createBignum(seed);
        }

    }

    @Primitive(name = "randomizer_bytes", lowerFixnum = 1)
    public static abstract class RandomizerBytesPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject genRandBytes(DynamicObject randomizer, int length) {
            final org.jruby.util.Random random = Layouts.RANDOMIZER.getRandom(randomizer);
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
    private static int randomInt(org.jruby.util.Random random) {
        return random.genrandInt32();
    }

}
