package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyNumeric;
import org.jruby.RubyRandom;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Rubinius primitives associated with the Ruby {@code Random} class.
 */

public abstract class RandomPrimitiveNodes {

    @RubiniusPrimitive(name = "randomizer_seed")
    public static abstract class RandomizerSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandomizerSeedPrimitiveNode(RandomizerSeedPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long randomizerSeed(RubyBasicObject random) {
            notDesignedForCompilation();

            return System.currentTimeMillis();
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_float")
    public static abstract class RandomizerRandFloatPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandFloatPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandomizerRandFloatPrimitiveNode(RandomizerRandFloatPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public double randomizerRandFloat(RubyBasicObject random) {
            notDesignedForCompilation();

            return Math.random();
        }

    }

    @RubiniusPrimitive(name = "randomizer_rand_int")
    public static abstract class RandomizerRandIntPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerRandIntPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandomizerRandIntPrimitiveNode(RandomizerRandIntPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public long randomizerRandInt(RubyBasicObject random, Integer limit) {
            notDesignedForCompilation();

            return RandomPrimitiveHelper.randomInt(getContext().getRuntime().getCurrentContext().getRuntime(), limit);
        }

        @Specialization
        public long randomizerRandInt(RubyBasicObject random, Long limit) {
            notDesignedForCompilation();

            return RandomPrimitiveHelper.randomInt(getContext().getRuntime().getCurrentContext().getRuntime(), limit);
        }
    }

    @RubiniusPrimitive(name = "randomizer_gen_seed")
    public static abstract class RandomizerGenSeedPrimitiveNode extends RubiniusPrimitiveNode {

        public RandomizerGenSeedPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RandomizerGenSeedPrimitiveNode(RandomizerGenSeedPrimitiveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyBignum randomizerGenSeed(RubyBasicObject random) {
            notDesignedForCompilation();

            BigInteger integer = RandomPrimitiveHelper.randomSeed(getContext().getRuntime().getCurrentContext().getRuntime());
            return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), integer);
        }
    }

    static class RandomPrimitiveHelper {

        @CompilerDirectives.TruffleBoundary
        public static BigInteger randomSeed(Ruby context) {
            return RubyRandom.randomSeed(context).getBigIntegerValue();
        }

        @CompilerDirectives.TruffleBoundary
        public static long randomInt(Ruby context, long limit) {
            RubyFixnum fixnum = context.newFixnum(limit);
            return generateRandomInt(context, fixnum);
        }

        @CompilerDirectives.TruffleBoundary
        public static long randomInt(Ruby context, int limit) {
            RubyFixnum fixnum = context.newFixnum(limit);
            return generateRandomInt(context, fixnum);
        }

        public static long generateRandomInt(Ruby context, RubyFixnum limit) {
            IRubyObject params[] = new IRubyObject[1];
            params[0] = limit;
            RubyNumeric num = (RubyNumeric) RubyRandom.randCommon19(context.getCurrentContext(), null, params);
            return num.getLongValue();
        }
    }
}
