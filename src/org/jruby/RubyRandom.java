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
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import static org.jruby.CompatVersion.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.javasupport.util.RuntimeHelpers.invokedynamic;
import static org.jruby.runtime.MethodIndex.OP_EQUAL;

/**
 * Implementation of the Random class.
 */
@JRubyClass(name = "Random")
public class RubyRandom extends RubyObject {

    public static Random globalRandom = new Random();
    private static IRubyObject globalSeed;

    private Random random = new Random();
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

    @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize(ThreadContext context) {
        Ruby runtime = context.getRuntime();
        long seedLong = random.nextLong();
        seed = RubyBignum.newBignum(runtime, seedLong);
        random.setSeed(seedLong);
        return this;
    }

    @JRubyMethod(visibility = PRIVATE, compat = RUBY1_9)
    public IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        long seedLong;
        if (arg instanceof RubyFloat) {
            seed = RubyBignum.num2fix(((RubyFloat) arg).truncate());
            seedLong = RubyNumeric.num2long(seed);
        } else if (arg instanceof RubyBignum) {
            seed = arg;
            seedLong = (long)RubyBignum.big2dbl((RubyBignum)arg);
        } else {
            seed = arg.convertToInteger();
            seedLong = RubyNumeric.num2long(seed);
        }
        random.setSeed(seedLong);

        return this;
    }

    @JRubyMethod(name = "seed", compat = RUBY1_9)
    public IRubyObject seed(ThreadContext context) {
        return seed;
    }

    @JRubyMethod(name = "rand", meta = true, compat = RUBY1_9)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv) {
        return randCommon(context, context.nil, globalRandom, false);
    }

    @JRubyMethod(name = "rand", meta = true, compat = RUBY1_9)
    public static IRubyObject rand(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return randCommon(context, arg0, globalRandom, false);
    }

    @JRubyMethod(name = "rand", compat = RUBY1_9)
    public IRubyObject randObj(ThreadContext context) {
        return randCommon(context, context.nil, random, true);
    }

    @JRubyMethod(name = "rand", compat = RUBY1_9)
    public IRubyObject randObj(ThreadContext context, IRubyObject arg0) {
        return randCommon(context, arg0, random, true);
    }

    private static IRubyObject randCommon(ThreadContext context, IRubyObject arg, Random random, boolean raiseArgError) {
        Ruby runtime = context.getRuntime();
        if (arg.isNil()) {  // NO ARGUMENTS
            return runtime.newFloat(random.nextFloat());
        } else if (arg instanceof RubyRange) { // RANGE ARGUMENT
            RubyRange range = (RubyRange) arg;
            IRubyObject first = range.first();
            IRubyObject last  = range.last();
            
            boolean returnFloat = first instanceof RubyFloat || last instanceof RubyFloat;
            if (returnFloat) {
                first = first.convertToFloat();
                last  = last.convertToFloat();
            }

            if (range.include_p19(context, last).isTrue() && (!returnFloat)) {
                last = last.callMethod(context, "+", runtime.newFixnum(1));
            }

            if (!first.respondsTo("-") || !first.respondsTo("+") ||
                    !last.respondsTo("-") || !last.respondsTo("+")) {
                throw runtime.newArgumentError("invalid argument - " + arg.toString());
            }

            IRubyObject difference = last.callMethod(context, "-", first);
            if (returnFloat) {
                double max = (double) RubyNumeric.num2dbl(difference);
                double rand = random.nextDouble() * ((RubyFloat) difference).getDoubleValue();
                return RubyFloat.newFloat(runtime, ((RubyFloat) first).getDoubleValue() + rand);
                
            } else {
                int max = (int) RubyNumeric.num2long(difference);

                int rand = random.nextInt(max);

                return RubyNumeric.num2fix(first.callMethod(context, "+", runtime.newFixnum(rand)));
            }
        } else if (arg instanceof RubyFloat) { // FLOAT ARGUMENT
            double max = RubyNumeric.num2dbl(arg);
            if (max <= 0 && raiseArgError) {
                throw runtime.newArgumentError("invalid argument - " + arg.toString());
            }
            return runtime.newFloat(random.nextFloat() * max);
        } else { // OTHERWISE
            int max = 0;
            if (arg instanceof RubyBignum) {
                max = (int)RubyBignum.big2dbl((RubyBignum)arg);
            } else {
                if (arg.respondsTo("to_i")) {
                    arg = arg.callMethod(context, "to_i");
                }
                max = (int)RubyNumeric.num2long(arg);
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

    @JRubyMethod(meta = true, compat = RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv) {
        return srand(context, recv, context.getRuntime().getNil());
    }

    @JRubyMethod(meta = true, compat = RUBY1_9)
    public static IRubyObject srand(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        return srandCommon(context, recv, arg, false);
    }

    public static IRubyObject srandCommon(ThreadContext context, IRubyObject recv, IRubyObject arg, boolean acceptZero) {
        Ruby runtime = context.getRuntime();
        IRubyObject newSeed = arg;
        IRubyObject previousSeed = globalSeed;

        long seedArg = 0;
        if (arg instanceof RubyBignum) {
            seedArg = ((RubyBignum)arg).getValue().longValue();
        } else if (!arg.isNil()) {
            seedArg = RubyNumeric.num2long(arg);
        }

        if (arg.isNil() || (!acceptZero && seedArg == 0)) {
            newSeed = RubyNumeric.int2fix(runtime, System.currentTimeMillis() ^
                recv.hashCode() ^ runtime.incrementRandomSeedSequence() ^
                runtime.getRandom().nextInt(Math.max(1, Math.abs((int)runtime.getRandomSeed()))));
            seedArg = RubyNumeric.fix2long(newSeed);
        }

        globalSeed = newSeed;
        globalRandom.setSeed(seedArg);

        return previousSeed;
    }

    @Override
    @JRubyMethod(name = "==", required = 1, compat = RUBY1_9)
    public IRubyObject op_equal_19(ThreadContext context, IRubyObject obj) {
        Ruby runtime = context.getRuntime();
        // TODO: mri also compares the "state"

        if (!(obj instanceof RubyRandom)) {
            return runtime.getFalse();
        } else {
            RubyRandom r2 = (RubyRandom) obj;
            return invokedynamic(context, this.seed(context), OP_EQUAL, r2.seed(context));
        }
    }

    @JRubyMethod(name = "marshal_dump", backtrace = true, compat = RUBY1_9)
    public IRubyObject marshal_dump(ThreadContext context) {
        RubyArray dump = context.getRuntime().newArray(this, seed);

        if (hasVariables()) dump.syncVariables(this);
        return dump;
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject marshal_load(ThreadContext context, IRubyObject arg) {
        RubyArray load = arg.convertToArray();
        if (load.size() > 0) {
            RubyRandom rand = (RubyRandom) load.eltInternal(0);
            seed =  load.eltInternal(1);
            random.setSeed(seed.convertToInteger().getLongValue());
        }
        if (load.hasVariables()) syncVariables((IRubyObject)load);
        return this;
    }

    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject bytes(ThreadContext context, IRubyObject arg) {
        int size = RubyNumeric.num2int(arg);

        byte[] bytes = new byte[size];
        random.nextBytes(bytes);

        return context.getRuntime().newString(new ByteList(bytes));
    }

    public static double randomReal(ThreadContext context, IRubyObject obj) {
        Random random = null;
        if (obj.equals(context.runtime.getRandomClass())) {
            random = globalRandom;
        }
        if (obj instanceof RubyRandom) {
            random = ((RubyRandom) obj).random;
        }
        if (random != null) {
            return random.nextDouble();
        }
        double d = RubyNumeric.num2dbl(RuntimeHelpers.invoke(context, obj, "rand"));
        if (d < 0.0 || d >= 1.0) {
            throw context.runtime.newRangeError("random number too big: " + d);
        }
        return d;
    }
    
    @JRubyMethod(name = "new_seed", meta = true, compat = RUBY1_9)
    public static IRubyObject newSeed(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        globalRandom = new Random();
        long rand = globalRandom.nextLong();
        globalRandom.setSeed(rand);
        return RubyBignum.newBignum(runtime, rand);
    }
}
