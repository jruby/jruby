package org.jruby.test;

import org.jruby.RubyThread;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.newString;

public class TestRubyThread extends Base {

    public void testExceptionDoesNotPropagate() throws InterruptedException {
        context.runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) context.runtime.evalScriptlet(
            "Thread.start { sleep(0.01) until $run_thread; raise java.lang.RuntimeException.new('TEST') }"
        );
        assertNull(thread.getExitingException());
        thread.setReportOnException(false);
        thread.setAbortOnException(false);
        context.runtime.evalScriptlet("$run_thread = true");

        Thread.sleep(100);

        assertNotNull(thread.getExitingException());
        assertSame(RuntimeException.class, thread.getExitingException().getClass());

        assertSame(context.nil, thread.status(context));
    }

    public void testJavaErrorDoesPropagate() throws InterruptedException {
        context.runtime.evalScriptlet("$run_thread = false");
        RubyThread thread = (RubyThread) context.runtime.evalScriptlet(
            "Thread.start { sleep(0.01) until $run_thread; raise java.lang.AssertionError.new(42) }"
        );
        assertNull(thread.getExitingException());
        thread.setReportOnException(false);
        thread.setAbortOnException(false);

        final AtomicReference exception = new AtomicReference(null);
        thread.getNativeThread().setUncaughtExceptionHandler((t, uncaught) -> {
            exception.set(uncaught);
        });

        context.runtime.evalScriptlet("$run_thread = true");

        Thread.sleep(100);

        assertTrue(thread.getExitingException() instanceof AssertionError);
        // but bubbles out to Java handler :
        assertNotNull(exception.get());
        assertEquals("java.lang.AssertionError: 42", exception.get().toString());

        assertSame(context.nil, thread.status(context));
    }

    public void testClearLocals() throws InterruptedException {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final AtomicReference<RubyThread> otherThread = new AtomicReference<>();
        final CountDownLatch latch2 = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            context.runtime.evalScriptlet("Thread.current[:foo] = :bar");
            context.runtime.evalScriptlet("Thread.current.thread_variable_set('local', 42)");
            otherThread.set(RubyThread.current(context.runtime.getThread()));

            context.runtime.evalScriptlet("sleep(0.1)");
            latch1.countDown();

            context.runtime.evalScriptlet("sleep(0.1)");
            try {
                latch2.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });

        thread.start();
        latch1.await(3, TimeUnit.SECONDS);

        IRubyObject local;

        local = otherThread.get().op_aref(context, asSymbol(context, "foo"));
        assertEquals("bar", local.toString());

        otherThread.get().clearFiberLocals();

        local = otherThread.get().op_aref(context, asSymbol(context, "foo"));
        assertSame(context.nil, local);
        assertEquals(0, otherThread.get().keys().size());

        local = otherThread.get().thread_variable_p(context, newString(context, "local"));
        assertSame(context.tru, local);

        otherThread.get().clearThreadLocals();

        local = otherThread.get().thread_variable_p(context, newString(context, "local"));
        assertSame(context.fals, local);

        latch2.countDown();
    }
}
