package org.jruby.bench;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.Binding;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BenchEventHook {
    private static Ruby runtime;
    private static Ruby interpRuntime;
    private static Ruby tracedRuntime;
    private static Ruby hookedRuntime1;
    private static Ruby hookedRuntime2;

    private static final String BOOT_SCRIPT = "def fib(a) \n if a.send :<, \n 2 \n a \n else \n fib(a.send :-, 1).send :+, \n fib(a.send :-, 2) \n end \n end";
    private static final String RUN_SCRIPT = "fib(30)";

    public static void main(String[] args) {
        RubyInstanceConfig config = new RubyInstanceConfig();

        RubyInstanceConfig interpConfig = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);

        RubyInstanceConfig tracedConfig = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        config.setObjectSpaceEnabled(true);
        RubyInstanceConfig.FULL_TRACE_ENABLED = true;

        runtime = Ruby.newInstance(config);
        runtime.evalScriptlet(BOOT_SCRIPT);
        interpRuntime = Ruby.newInstance(interpConfig);
        interpRuntime.evalScriptlet(BOOT_SCRIPT);
        tracedRuntime = Ruby.newInstance(tracedConfig);
        tracedRuntime.evalScriptlet(BOOT_SCRIPT);
        hookedRuntime1 = Ruby.newInstance(tracedConfig);
        hookedRuntime1.evalScriptlet(BOOT_SCRIPT);
        hookedRuntime2 = Ruby.newInstance(tracedConfig);
        hookedRuntime2.evalScriptlet(BOOT_SCRIPT);

        hookedRuntime1.addEventHook(new EventHook() {
            @Override
            public void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
                // do nothing
            }

            @Override
            public boolean isInterestedInEvent(RubyEvent event) {
                // want everything
                return true;
            }
        });

        hookedRuntime2.addEventHook(new EventHook() {
            @Override
            public void eventHandler(ThreadContext context, String eventName, String file, int line, String name, IRubyObject type) {
                // get binding
                Binding binding = context.currentBinding();
            }

            @Override
            public boolean isInterestedInEvent(RubyEvent event) {
                // want everything
                return true;
            }
        });

        for (int i = 0; i < 10; i++) {
            benchControl();
            benchInterp();
            benchTraced();
            benchHooked1();
            benchHooked2();
        }
    }

    public static void benchControl() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            runtime.evalScriptlet(RUN_SCRIPT);
        }
        System.out.println("control: " + (System.currentTimeMillis() - start));
    }

    public static void benchInterp() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            interpRuntime.evalScriptlet(RUN_SCRIPT);
        }
        System.out.println("interp: " + (System.currentTimeMillis() - start));
    }

    public static void benchTraced() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            tracedRuntime.evalScriptlet(RUN_SCRIPT);
        }
        System.out.println("traced: " + (System.currentTimeMillis() - start));
    }

    public static void benchHooked1() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            hookedRuntime1.evalScriptlet(RUN_SCRIPT);
        }
        System.out.println("hooked (no binding): " + (System.currentTimeMillis() - start));
    }

    public static void benchHooked2() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            hookedRuntime2.evalScriptlet(RUN_SCRIPT);
        }
        System.out.println("hooked (binding): " + (System.currentTimeMillis() - start));
    }
}
