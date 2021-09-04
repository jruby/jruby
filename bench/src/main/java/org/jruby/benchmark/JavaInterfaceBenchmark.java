package org.jruby.benchmark;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyInstanceConfig;
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
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class JavaInterfaceBenchmark {

    private static final int INVOCATIONS = 200_000_000;

    private static final Ruby RUBY = initRuby();

    private static final RubyFixnum LEFT = RUBY.newFixnum(ThreadLocalRandom.current().nextInt());

    private static final RubyFixnum RIGHT = RUBY.newFixnum(ThreadLocalRandom.current().nextInt());

    private static final JavaInterfaceBenchmark.Summation JAVA_SUMMER =
        new JavaInterfaceBenchmark.Summation() {
            @Override
            public IRubyObject sum(final RubyFixnum left, final RubyFixnum right) {
                return left.op_plus(RUBY.getCurrentContext(), right);
            }
        };

    public interface Summation {
        IRubyObject sum(RubyFixnum left, RubyFixnum right);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchHalfRubyVersion(final Blackhole blackhole) {
        blackhole.consume(
            RUBY.executeScript(
                "org.jruby.benchmark.JavaInterfaceBenchmark.doRun(RubySummation.new)"
            ,"benchHalfRubyVersion")
        );
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchJavaVersion(final Blackhole blackhole) {
        blackhole.consume(doRun(JAVA_SUMMER));
    }

    public static IRubyObject doRun(final JavaInterfaceBenchmark.Summation summer) {
        IRubyObject sum = null;
        for (int i = 0; i < INVOCATIONS; ++i) {
            sum = summer.sum(LEFT, RIGHT);
        }
        return sum;
    }

    private static Ruby initRuby() {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.FORCE);
        final Ruby ruby = Ruby.newInstance(config);
        ruby.executeScript(
            new StringBuilder()
                .append("class RubySummation\n")
                .append("\tinclude org.jruby.benchmark.JavaInterfaceBenchmark::Summation\n")
                .append('\n')
                .append("\tdef sum(a, b)\n")
                .append("\t\ta + b\n")
                .append("\tend\n")
                .append("end")
                .toString(), "initRuby"
        );
        return ruby;
    }
}
