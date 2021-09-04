package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 20, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class FixnumCreationBenchmark {

    private static final int INVOCATIONS = 1_000_000;

    private static final Ruby RUBY = Ruby.newInstance();

    private static final RubyFixnum ONE = RubyFixnum.newFixnum(RUBY, 1L);

    private static final RubyFixnum TWO = RubyFixnum.newFixnum(RUBY, 2L);

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchFixnumCreation(final Blackhole blackhole) {
        final long time = System.currentTimeMillis();
        final Ruby ruby = RUBY;
        for (int i = 0; i < INVOCATIONS; i++) {
            blackhole.consume(RubyFixnum.newFixnum(ruby, time));
        }
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void benchStaticFib(final Blackhole blackhole) {
        final Ruby ruby = RUBY;
        final ThreadContext context = ruby.getCurrentContext();
        blackhole.consume(fib(context, RubyFixnum.newFixnum(ruby, 30L)));
    }

    private static IRubyObject fib(final ThreadContext context, final IRubyObject object) {
        final RubyFixnum value = (RubyFixnum) object;
        if (value.op_lt(context, TWO).isTrue()) {
            return value;
        }
        return ((RubyFixnum) fib(context, value.op_minus(context, TWO)))
            .op_plus(context, fib(context, value.op_minus(context, ONE)));
    }
}
