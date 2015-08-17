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
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyRandom;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.core.BignumNodes;
import org.jruby.truffle.runtime.RubyContext;

import java.math.BigInteger;

/**
 * Rubinius primitives associated with the Ruby {@code Random} class.
 */

public abstract class RandomPrimitiveNodes {

    @RubiniusPrimitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long randomizerSeed(DynamicObject random) {
            return System.currentTimeMillis();
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_float")
    public static abstract class RandomizerRandFloatPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandFloatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double randomizerRandFloat(DynamicObject random) {
            return Math.random();
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_int")
    public static abstract class RandomizerRandIntPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long randomizerRandInt(DynamicObject random, int limit) {
            return randomizerRandLong(random, (long) limit);
        }

        @Specialization
        public long randomizerRandLong(DynamicObject random, long limit) {
            return RandomPrimitiveHelper.randomInt(getContext().getRuntime(), limit);
        }
    }

    @RubiniusPrimitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerGenSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject randomizerGenSeed(DynamicObject random) {
            BigInteger integer = RandomPrimitiveHelper.randomSeed(getContext().getRuntime());
            return BignumNodes.createRubyBignum(getContext().getCoreLibrary().getBignumClass(), integer);
        }
    }

    static class RandomPrimitiveHelper {

        @TruffleBoundary
        public static BigInteger randomSeed(Ruby context) {
            return RubyRandom.randomSeed(context).getBigIntegerValue();
        }

        @TruffleBoundary
        public static long randomInt(Ruby context, long limit) {
            RubyFixnum fixnum = context.newFixnum(limit);
            return generateRandomInt(context, fixnum);
        }

        public static long generateRandomInt(Ruby context, RubyFixnum limit) {
            IRubyObject params[] = new IRubyObject[] { limit };
            RubyNumeric num = (RubyNumeric) RubyRandom.randCommon19(context.getCurrentContext(), null, params);
            return num.getLongValue();
        }
    }
}
