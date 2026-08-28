package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;

import org.jruby.Ruby;
import org.jruby.RubyString;
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

@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class StringLoopBenchmark {

    private static final int INVOCATIONS = 1_000_000;

    private static final Ruby RUBY = Ruby.newInstance();

    private static final String SHORT_STR = "1234567890";
    private static final RubyString SHORT = RubyString.newString(RUBY, SHORT_STR);

    private static final String LONG_STR = "1234567890_abcdefghijklmnoprstuvxyz-ABCDEFGHIJKLMNOPQRSTUVXYZ";
    private static final RubyString LONG = RubyString.newString(RUBY, LONG_STR);

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchShortRubyStringLoop(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) equalsRuby(SHORT_STR, SHORT, blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchShortJavaStringLoop(final Blackhole blackhole) {
        final String str = SHORT.decodeString();
        for (int i = 0; i < INVOCATIONS; i++) equalsJava(SHORT_STR, str, blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchShortDecodeStringLoop(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) equalsDecode(SHORT_STR, SHORT, blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongRubyStringLoop(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) equalsRuby(LONG_STR, LONG, blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongJavaStringLoop(final Blackhole blackhole) {
        final String str = LONG.decodeString();
        for (int i = 0; i < INVOCATIONS; i++) equalsJava(LONG_STR, str, blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongDecodeStringLoop(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) equalsDecode(LONG_STR, LONG, blackhole);
    }

    private static void equalsRuby(final String STR, final RubyString str, final Blackhole blackhole) {
        blackhole.consume( STR.contentEquals(str) );
    }

    private static void equalsJava(final String STR, final String str, final Blackhole blackhole) {
        blackhole.consume( STR.contentEquals(str) );
    }

    private static void equalsDecode(final String STR, final RubyString str, final Blackhole blackhole) {
        blackhole.consume( STR.contentEquals(str.decodeString()) );
    }
}
