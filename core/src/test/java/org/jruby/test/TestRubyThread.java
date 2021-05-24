package org.jruby.test;

import org.jruby.RubyThread;

import java.util.concurrent.atomic.AtomicReference;

public class TestRubyThread extends Base {

    public void testExceptionDoesNotPropagate() throws InterruptedException {
        runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) runtime.evalScriptlet(
            "Thread.start { sleep(0.01) unless $run_thread; raise java.lang.RuntimeException.new('TEST') }"
        );
        assertNull(thread.getExitingException());
        thread.setReportOnException(false);
        thread.setAbortOnException(false);
        runtime.evalScriptlet("$run_thread = true");

        Thread.sleep(100);

        assertNotNull(thread.getExitingException());
        assertSame(RuntimeException.class, thread.getExitingException().getClass());
    }

    public void testJavaErrorDoesPropagate() throws InterruptedException {
        runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) runtime.evalScriptlet(
            "Thread.start { sleep(0.01) unless $run_thread; raise java.lang.AssertionError.new(42) }"
        );
        assertNull(thread.getExitingException());
        thread.setReportOnException(false);
        thread.setAbortOnException(false);

        final AtomicReference exception = new AtomicReference(null);
        thread.getNativeThread().setUncaughtExceptionHandler((t, uncaught) -> {
            exception.set(uncaught);
        });

        runtime.evalScriptlet("$run_thread = true");

        Thread.sleep(100);

        assertNull(thread.getExitingException()); // bubble out to handler :
        assertNotNull(exception.get());
        assertEquals("java.lang.AssertionError: 42", exception.get().toString());
    }

}
