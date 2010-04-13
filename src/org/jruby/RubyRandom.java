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

import java.util.Random;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Implementation of the Random class.
 */
@JRubyClass(name = "Random")
public class RubyRandom extends RubyObject {

    private static final Random globalRandom = new Random();
    private static IRubyObject globalSeed;

    private final Random random = new Random();
    private IRubyObject seed;

    public static RubyClass createRandomClass(Ruby runtime) {
        RubyClass randomClass = runtime.defineClass("Random", runtime.getObject(), RANDOM_ALLOCATOR);
        runtime.setRandomClass(randomClass);

        randomClass.defineAnnotatedMethods(RubyRandom.class);
        return randomClass;
    }

    private static ObjectAllocator RANDOM_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyRandom(runtime, klass);
        }
    };

    private RubyRandom(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_9)
    public IRubyObject initialize(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        long seedLong = random.nextLong();
        seed = RubyBignum.newBignum(runtime, seedLong);
        random.setSeed(seedLong);
        return this;
    }

    @JRubyMethod(name = "initialize", required = 1, frame = true, visibility = Visibility.PRIVATE, compat = CompatVersion.RUBY1_9)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        long seedLong;
        if (arg instanceof RubyFloat) {
            seed = RubyBignum.num2fix(((RubyFloat) arg).truncate());
            seedLong = RubyNumeric.num2long(seed);
        } else if (arg instanceof RubyBignum) {
            seed = arg;
            seedLong = new Double(RubyBignum.big2dbl((RubyBignum)arg)).longValue();
        } else {
            seed = arg.convertToInteger();
            seedLong = RubyNumeric.num2long(seed);
        }
        random.setSeed(seedLong);

        return this;
    }

    @JRubyMethod(name = "seed", compat = CompatVersion.RUBY1_9)
    public IRubyObject seed(ThreadContext context) {
        return seed;
    }

    @JRubyMethod(name = "rand", meta = true, optional = 1, compat = CompatVersion.RUBY1_9)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject arg = context.getRuntime().getNil();
        if (args.length > 0) {
            arg = args[0];
        }
        return randCommon(context, arg, globalRandom, false);
    }

    @JRubyMethod(name = "rand", backtrace = true, optional = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject randObj(ThreadContext context, IRubyObject[] args) {
        IRubyObject arg = context.getRuntime().getNil();
        if (args.length > 0) {
            arg = args[0];
        }
        return randCommon(context, arg, random, true);
    }

    private static IRubyObject randCommon(ThreadContext context, IRubyObject arg, Random random, boolean raiseArgError) {
        Ruby runtime = context.getRuntime();
        if (arg.isNil()) {  // NO ARGUMENTS
            return runtime.newFloat(random.nextFloat());
        } else if (arg instanceof RubyRange) { // RANGE ARGUMENT
            RubyRange range = (RubyRange) arg;
            IRubyObject first = range.first();
            IRubyObject last = range.last();
            if (range.include_p19(context, last).isTrue()) {
                last = last.callMethod(context, "+", runtime.newFixnum(1));
            }

            if (!first.respondsTo("-") || !first.respondsTo("+") ||
                    !last.respondsTo("-") || !last.respondsTo("+")) {
                throw runtime.newArgumentError("invalid argument - " + arg.toString());
            }

            IRubyObject difference = last.callMethod(context, "-", first);
            int max = new Long(RubyNumeric.num2long(difference)).intValue();

            int rand = random.nextInt(max);

            return RubyNumeric.num2fix(first.callMethod(context, "+", runtime.newFixnum(rand)));
        } else if (arg instanceof RubyFloat) { // FLOAT ARGUMENT
            double max = RubyNumeric.num2dbl(arg);
            if (max <= 0 && raiseArgError) {
                throw runtime.newArgumentError("invalid argument - " + arg.toString());
            }
            return runtime.newFloat(random.nextFloat() * max);
        } else { // OTHERWISE
            int max = 0;
            if (arg instanceof RubyBignum) {
                max = new Double(RubyBignum.big2dbl((RubyBignum)arg)).intValue();
            } else {
                if (arg.respondsTo("to_i")) {
                    arg = arg.callMethod(context, "to_i");
                }
                max = new Long(RubyNumeric.num2long(arg)).intValue();
            }

            if (max <= 0 && raiseArgError) {
                throw runtime.newArgumentError("invalid argument - " + arg.toString());
            }

            int rand = random.nextInt(max);
            if (arg instanceof RubyBignum) {
                return RubyBignum.newBignum(runtime, rand);
            }

            return runtime.newFixnum(rand);
        }
    }

    @JRubyMethod(name = "srand", frame = true, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return srand(context, recv, context.getRuntime().getNil());
    }

    @JRubyMethod(name = "srand", frame = true, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Ruby runtime = context.getRuntime();
        IRubyObject newSeed = arg;
        IRubyObject previousSeed = globalSeed;

        if (arg.isNil() || RubyNumeric.num2int(arg) == 0) {
            newSeed = RubyNumeric.int2fix(runtime, System.currentTimeMillis() ^
                recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
                runtime.getRandom().nextInt(Math.max(1, Math.abs((int)runtime.getRandomSeed()))));
        }

        globalSeed = newSeed;
        globalRandom.setSeed(RubyNumeric.fix2long(globalSeed));

        return previousSeed;
    }

    @Override
    @JRubyMethod(name = "==", required = 1, compat = CompatVersion.RUBY1_9)
    public IRubyObject op_equal_19(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        // TODO: mri also compares the "state"

        if (!(obj instanceof RubyRandom)) {
            return runtime.getFalse();
        } else {
            RubyRandom r2 = (RubyRandom) obj;
            return this.seed(context).callMethod(context, "==", r2.seed(context));
        }
    }

    @JRubyMethod(name = "marshal_dump", backtrace = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyArray dump = context.getRuntime().newArray(seed);
        if (hasVariables()) dump.syncVariables(getVariableList());
        return dump;
    }

    @JRubyMethod(name = "marshal_load", backtrace = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        if (load.size() > 0) {
            seed =  load.eltInternal(0);
            random.setSeed(seed.convertToInteger().getLongValue());
        }
        if (load.hasVariables()) syncVariables(load.getVariableList());
        return this;
    }

    @JRubyMethod(name = "bytes", frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject bytes(ThreadContext context, IRubyObject arg) {
        throw context.getRuntime().newNotImplementedError("Random#bytes is not implemented yet");
    }

    @JRubyMethod(name = "new_seed", frame = true, meta = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject newSeed(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        long rand = getRandomSeed(runtime, recv);
        return RubyBignum.newBignum(runtime, rand);
    }

    private static long getRandomSeed(Ruby runtime, IRubyObject recv) {
        return System.currentTimeMillis() ^
               recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
               runtime.getRandom().nextInt(Math.max(1, Math.abs((int)runtime.getRandomSeed())));
    }
}
