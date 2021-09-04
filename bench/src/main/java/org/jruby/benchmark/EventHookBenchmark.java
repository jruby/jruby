package org.jruby.benchmark;

import java.util.concurrent.TimeUnit;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.Binding;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.RubyEvent;
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

@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 10, time = 3)
@Fork(1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class EventHookBenchmark {

    private static final int INVOCATIONS = 10;

    private static final String BOOT_SCRIPT =
        "def fib(a) \n if a.send :<, \n 2 \n a \n else \n fib(a.send :-, 1).send :+, \n fib(a.send :-, 2) \n end \n end";

    private static final String RUN_SCRIPT = "fib(30)";

    private static final Ruby RUNTIME = initRuntime();
    private static final Ruby INTERP_RUNTIME = initInterpRuntime();
    private static final Ruby TRACED_RUNTIME = initTracedRuntime();
    private static final Ruby HOOKED_RUNTIME_ONE = initHookedRuntime(
        new EventHook() {
            @Override
            public void eventHandler(final ThreadContext context, final String eventName,
                final String file, final int line, final String name, final IRubyObject type) {
                // do nothing
            }

            @Override
            public boolean isInterestedInEvent(final RubyEvent event) {
                // want everything
                return true;
            }
        });
    private static final Ruby HOOKED_RUNTIME_TWO = initHookedRuntime(
        new EventHook() {
            @Override
            public void eventHandler(final ThreadContext context, final String eventName,
                final String file, final int line, final String name, final IRubyObject type) {
                // get binding
                final Binding binding = context.currentBinding();
            }

            @Override
            public boolean isInterestedInEvent(final RubyEvent event) {
                // want everything
                return true;
            }
        }
    );

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchControl() {
        for (int i = 0; i < INVOCATIONS; i++) {
            RUNTIME.evalScriptlet(RUN_SCRIPT);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchInterp() {
        for (int i = 0; i < INVOCATIONS; i++) {
            INTERP_RUNTIME.evalScriptlet(RUN_SCRIPT);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchTraced() {
        for (int i = 0; i < INVOCATIONS; i++) {
            TRACED_RUNTIME.evalScriptlet(RUN_SCRIPT);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchHooked1() {
        for (int i = 0; i < INVOCATIONS; i++) {
            HOOKED_RUNTIME_ONE.evalScriptlet(RUN_SCRIPT);
        }
    }

    @Benchmark
    @OperationsPerInvocation(INVOCATIONS)
    public void benchHooked2() {
        for (int i = 0; i < INVOCATIONS; i++) {
            HOOKED_RUNTIME_TWO.evalScriptlet(RUN_SCRIPT);
        }
    }

    private static Ruby initRuntime() {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        config.setObjectSpaceEnabled(true);
        final Ruby runtime = Ruby.newInstance(config);
        runtime.evalScriptlet(BOOT_SCRIPT);
        return runtime;
    }

    private static Ruby initInterpRuntime() {
        final Ruby runtime = Ruby.newInstance(new RubyInstanceConfig());
        runtime.evalScriptlet(BOOT_SCRIPT);
        return runtime;
    }

    private static Ruby initTracedRuntime() {
        final RubyInstanceConfig config = new RubyInstanceConfig();
        RubyInstanceConfig.FULL_TRACE_ENABLED = true;
        final Ruby runtime = Ruby.newInstance(config);
        runtime.evalScriptlet(BOOT_SCRIPT);
        return runtime;
    }

    private static Ruby initHookedRuntime(final EventHook hook) {
        final Ruby runtime = initTracedRuntime();
        runtime.addEventHook(hook);
        return runtime;
    }
}
