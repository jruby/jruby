/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyRandom;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.util.Random;

import java.math.BigInteger;

public abstract class RandomizerPrimitiveNodes {

    @RubiniusPrimitive(name = "randomizer_allocate", needsSelf = false)
    public static abstract class RandomizerAllocatePrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerAllocatePrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject randomizerAllocate() {
            return Layouts.RANDOMIZER.createRandomizer(getContext().getCoreLibrary().getRandomizerFactory(), new Random());
        }

    }

    @RubiniusPrimitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
    public static abstract class RandomizerRandFloatPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandFloatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
    public static abstract class RandomizerRandIntPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int randomizerRandInt(DynamicObject randomizer, int limit) {
            final Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return (int) putIntoRange(r, (long) limit);
        }

        @Specialization
        public long randomizerRandInt(DynamicObject randomizer, long limit) {
            final Random r = Layouts.RANDOMIZER.getRandom(randomizer);
            return putIntoRange(r, limit);
        }

        @TruffleBoundary
        protected static long putIntoRange(Random r, long limit) {
            return RubyRandom.randLimitedFixnumInner(r, limit);
        }

    }

    @RubiniusPrimitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerGenSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject randomizerGenSeed(DynamicObject randomizerClass) {
            final BigInteger seed = RubyRandom.randomSeedBigInteger(getContext().getRuntime().getRandom());
            return Layouts.BIGNUM.createBignum(getContext().getCoreLibrary().getBignumFactory(), seed);
        }
    }

    @TruffleBoundary
    private static int randomInt(Random random) {
        return random.genrandInt32();
    }

}
