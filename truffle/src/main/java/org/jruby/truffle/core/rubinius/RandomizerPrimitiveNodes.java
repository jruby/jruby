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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyRandom;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.util.Random;

import java.math.BigInteger;

public abstract class RandomizerPrimitiveNodes {

    @RubiniusPrimitive(name = "randomizer_allocate", needsSelf = false)
    public static abstract class RandomizerAllocatePrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject randomizerAllocate() {
            return Layouts.RANDOMIZER.createRandomizer(coreLibrary().getRandomizerFactory(), new Random());
        }

    }

    @RubiniusPrimitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

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
        protected static Random randomFromBigInteger(BigInteger seed) {
            return RubyRandom.RandomType.randomFromBigInteger(seed);
        }

        @TruffleBoundary
        protected static Random randomFromLong(long seed) {
            return RubyRandom.RandomType.randomFromLong(seed);
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_float")
    public static abstract class RandomizerRandFloatPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public double randomizerRandFloat(DynamicObject randomizer) {
            // Logic copied from org.jruby.util.Random
            final Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            final int a = randomInt(r) >>> 5;
            final int b = randomInt(r) >>> 6;
            return (a * 67108864.0 + b) * (1.0 / 9007199254740992.0);
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_int")
    public static abstract class RandomizerRandIntPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @Specialization
        public int randomizerRandInt(DynamicObject randomizer, int limit) {
            final Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return (int) randInt(r, (long) limit);
        }

        @Specialization
        public long randomizerRandInt(DynamicObject randomizer, long limit) {
            final Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return randInt(r, limit);
        }

        @TruffleBoundary
        protected static long randInt(Random r, long limit) {
            return RubyRandom.randLimitedFixnumInner(r, limit);
        }

    }

    @RubiniusPrimitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends RubiniusPrimitiveArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject randomizerGenSeed(DynamicObject randomizerClass) {
            final BigInteger seed = RubyRandom.randomSeedBigInteger(getContext().getJRubyRuntime().getRandom());
            return createBignum(seed);
        }
    }

    @TruffleBoundary
    private static int randomInt(Random random) {
        return random.genrandInt32();
    }

}
