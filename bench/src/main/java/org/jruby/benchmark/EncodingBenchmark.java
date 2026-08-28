package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.RubyEncoding;
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
public class EncodingBenchmark {

    private static final int INVOCATIONS = 1_000_000;

    private static final Ruby RUBY = Ruby.newInstance();

    private static final String SHORT_STR = "1234567890";

    private static final String LONG_STR = "1234567890_abcdefghijklmnoprstuvxyz-ABCDEFGHIJKLMNOPQRSTUVXYZ";

    private static final String VERY_LONG_STR;
    static {
        StringBuilder str = new StringBuilder(1025);
        for (int i = 0; i < 21; i++) {
            str.append("ABCDEFGHIJKLMNOPQRSTUVXYZ"); // 25 chars * 21 == 1025
        }
        VERY_LONG_STR = str.toString();
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchShortRubyStringNew(final Blackhole blackhole) {
        final String STR = SHORT_STR;
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchShortRubyStringNewCharSequence(final Blackhole blackhole) {
        final CharSequence STR = new StringBuilder(SHORT_STR);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongRubyStringNew(final Blackhole blackhole) {
        final String STR = LONG_STR;
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongRubyStringNewCharSequence(final Blackhole blackhole) {
        final CharSequence STR = new StringBuilder(LONG_STR);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongRubyStringNewCharSequence2(final Blackhole blackhole) {
        final CharSequence STR = java.nio.CharBuffer.wrap(LONG_STR);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchLongRubyStringNewCharSequence3(final Blackhole blackhole) {
        final CharSequence STR = LONG_STR.substring(0);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchVeryLongRubyStringNew(final Blackhole blackhole) {
        final String STR = VERY_LONG_STR;
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchVeryLongRubyStringNewCharSequence(final Blackhole blackhole) {
        final CharSequence STR = new StringBuilder(VERY_LONG_STR);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchVeryLongRubyStringNewCharSequence2(final Blackhole blackhole) {
        final CharSequence STR = java.nio.CharBuffer.wrap(VERY_LONG_STR);
        for (int i = 0; i < INVOCATIONS; i++) consume(RubyString.newString(RUBY, STR), blackhole);
    }

    private static void consume(final RubyString str, final Blackhole blackhole) {
        blackhole.consume( str );
    }

}
