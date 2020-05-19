package org.jruby.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyRuntimeAdapter;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

public final class EventHookTest extends TestCase {

    public EventHookTest(final String testName) {
        super(testName);
    }

    public void testLineNumbersForNativeTracer() {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.processArguments(new String[]{"--debug"});
        Ruby rt = JavaEmbedUtils.initialize(Collections.<String>emptyList(), config);
        NativeTracer tracer = new NativeTracer();
        rt.addEventHook(tracer);
        RubyRuntimeAdapter evaler = JavaEmbedUtils.newRuntimeAdapter();
        evaler.eval(rt, "sleep 0.01\nsleep 0.01\nsleep 0.01");
        assertEquals("expected tracing", Arrays.asList(1,1,1,2,2,2,3,3,3), tracer.lines);
    }

    private final static class NativeTracer extends EventHook {

        List<Integer> lines;

        NativeTracer() {
            this.lines = new ArrayList<Integer>();
        }

        @Override
        public boolean isInterestedInEvent(RubyEvent event) {
            return true;
        }

        @Override
        public void eventHandler(ThreadContext context, String eventName,
                String file, int line, String name, IRubyObject type) {
            lines.add(line);
        }

    }
}
