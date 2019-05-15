package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;

import org.jruby.*;
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
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class NilCheckBenchmark {

    private static final int INVOCATIONS = 10_000_000;

    private static final Ruby RUBY = Ruby.newInstance();
    private static final ThreadContext CTX = RUBY.getCurrentContext();

    private static final IRubyObject NIL = RUBY.getNil();
    private static final RubyBasicObject NIL2 = (RubyBasicObject) NIL;
    private static final IRubyObject OBJ = RubyHash.newHash(RUBY);
    private static final  RubyString STR = RubyString.newString(RUBY, "");

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchIsNilFalse(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(OBJ.isNil());
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchIsNilFalseCast(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(STR.isNil());
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchIsNilTrue(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(NIL.isNil());
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchIsNilTrueCast(final Blackhole blackhole) {
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(NIL2.isNil());
    }


    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchNilCmpTrue(final Blackhole blackhole) {
        final ThreadContext context = CTX;
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(context.nil == NIL);
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchNilCmpFalse(final Blackhole blackhole) {
        final ThreadContext context = CTX;
        for (int i = 0; i < INVOCATIONS; i++) blackhole.consume(context.nil == OBJ);
    }

}
