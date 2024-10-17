package org.jruby.test;

import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TestRubyThread extends Base {

    public void testExceptionDoesNotPropagate() throws InterruptedException {
        runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) runtime.evalScriptlet(
            "Thread.start { sleep(0.01) until $run_thread; raise java.lang.RuntimeException.new('TEST') }"
        );
        assertNull(thread.getExitingException());
        thread.setReportOnException(false);
        thread.setAbortOnException(false);
        runtime.evalScriptlet("$run_thread = true");

        Thread.sleep(100);

        assertNotNull(thread.getExitingException());
        assertSame(RuntimeException.class, thread.getExitingException().getClass());

        assertSame(runtime.getNil(), thread.status(runtime.getCurrentContext()));
    }

    public void testJavaErrorDoesPropagate() throws InterruptedException {
        runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) runtime.evalScriptlet(
            "Thread.start { sleep(0.01) until $run_thread; raise java.lang.AssertionError.new(42) }"
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

        assertTrue(thread.getExitingException() instanceof AssertionError);
        // but bubbles out to Java handler :
        assertNotNull(exception.get());
        assertEquals("java.lang.AssertionError: 42", exception.get().toString());

        assertSame(runtime.getNil(), thread.status(runtime.getCurrentContext()));
    }

    public void testClearLocals() throws InterruptedException {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final AtomicReference<RubyThread> otherThread = new AtomicReference<>();
        final CountDownLatch latch2 = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            runtime.evalScriptlet("Thread.current[:foo] = :bar");
            runtime.evalScriptlet("Thread.current.thread_variable_set('local', 42)");
            otherThread.set(RubyThread.current(runtime.getThread()));

            runtime.evalScriptlet("sleep(0.1)");
            latch1.countDown();

            runtime.evalScriptlet("sleep(0.1)");
            try {
                latch2.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        thread.start();
        latch1.await(3, TimeUnit.SECONDS);

        final ThreadContext context = runtime.getCurrentContext();
        IRubyObject local;

        local = otherThread.get().op_aref(context, runtime.newSymbol("foo"));
        assertEquals("bar", local.toString());

        otherThread.get().clearFiberLocals();

        local = otherThread.get().op_aref(context, runtime.newSymbol("foo"));
        assertSame(runtime.getNil(), local);
        assertEquals(0, otherThread.get().keys().size());

        local = otherThread.get().thread_variable_p(context, runtime.newString("local"));
        assertSame(runtime.getTrue(), local);

        otherThread.get().clearThreadLocals();

        local = otherThread.get().thread_variable_p(context, runtime.newString("local"));
        assertSame(runtime.getFalse(), local);

        latch2.countDown();
    }
}
