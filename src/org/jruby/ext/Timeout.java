package org.jruby.ext;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.threading.DaemonThreadFactory;

public class Timeout implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule timeout = runtime.defineModule("Timeout");
        RubyClass timeoutError = runtime.defineClassUnder("Error", runtime.getInterrupt(), runtime.getInterrupt().getAllocator(), timeout);
        runtime.defineClassUnder("ExitException", runtime.getException(), runtime.getInterrupt().getAllocator(), timeout);

        // These are not really used by timeout, but exposed for compatibility
        timeout.defineConstant("THIS_FILE", RubyRegexp.newRegexp(runtime, "timeout\\.rb", 0));
        timeout.defineConstant("CALLER_OFFSET", RubyFixnum.newFixnum(runtime, 0));

        // Timeout module methods
        timeout.defineAnnotatedMethods(Timeout.class);

        // Toplevel defines
        runtime.getObject().defineConstant("TimeoutError", timeoutError);
        runtime.getObject().defineAnnotatedMethods(TimeoutToplevel.class);
    }

    private static ExecutorService timeoutExecutor = Executors.newCachedThreadPool(new DaemonThreadFactory());

    public static class TimeoutToplevel {
        @JRubyMethod(required = 1, optional = 1)
        public static IRubyObject timeout(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            RubyModule timeout = context.getRuntime().getModule("Timeout");
            
            switch (args.length) {
            case 1:
                return Timeout.timeout(context, timeout, args[0], block);
            case 2:
                return Timeout.timeout(context, timeout, args[0], args[1], block);
            default:
                Arity.raiseArgumentError(context.getRuntime(), args.length, 1, 2);
                return context.getRuntime().getNil();
            }
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject timeout, IRubyObject seconds, Block block) {
        // No seconds, just yield
        if (seconds.isNil() || RuntimeHelpers.invoke(context, seconds, "zero?").isTrue()) {
            return block.yieldSpecific(context);
        }

        final Ruby runtime = context.getRuntime();

        // No timeout in critical section
        if (runtime.getThreadService().getCritical()) {
            return raiseBecauseCritical(context);
        }

        final long waitTime = 1000 * seconds.convertToInteger().getLongValue();
        final RubyThread currentThread = context.getThread();
        final Object monitor = new Object();
        final AtomicBoolean doRaise = new AtomicBoolean(true);
        
        Runnable timeoutRunnable = prepareRunnable(monitor, waitTime, doRaise, currentThread, runtime);
        Future timeoutFuture = null;

        try {
            timeoutFuture = timeoutExecutor.submit(timeoutRunnable);

            return block.yield(context, seconds);
        } catch (RaiseException re) {
            if (re.getException().getMetaClass() == runtime.getClassFromPath("Timeout::ExitException")) {
                return raiseTimeoutError(context, re);
            } else {
                throw re;
            }
        } finally {
            killTimeoutThread(context, timeoutFuture, monitor, doRaise);
        }
    }

    @JRubyMethod(module = true)
    public static IRubyObject timeout(final ThreadContext context, IRubyObject timeout, IRubyObject seconds, IRubyObject exceptionType, Block block) {
        // No seconds, just yield
        if (seconds.isNil() || RuntimeHelpers.invoke(context, seconds, "zero?").isTrue()) {
            return block.yieldSpecific(context);
        }

        final Ruby runtime = context.getRuntime();

        // No timeout in critical section
        if (runtime.getThreadService().getCritical()) {
            return raiseBecauseCritical(context);
        }

        final IRubyObject exception = exceptionType.isNil() ? runtime.getClassFromPath("Timeout::ExitException") : exceptionType;
        final long waitTime = 1000 * seconds.convertToInteger().getLongValue();
        final RubyThread currentThread = context.getThread();
        final Object monitor = new Object();
        final AtomicBoolean doRaise = new AtomicBoolean(true);

        Runnable timeoutRunnable = prepareRunnableWithException(monitor, waitTime, doRaise, currentThread, exception, runtime);
        Future timeoutFuture = null;

        try {
            timeoutFuture = timeoutExecutor.submit(timeoutRunnable);

            return block.yield(context, seconds);
        } catch (RaiseException re) {
            // if it's the exception we're expecting
            if (re.getException().getMetaClass() == exception) {
                // and we were given a specific exception
                if (exceptionType.isNil()) {
                    return raiseTimeoutError(context, re);
                }
            }

            // otherwise, rethrow
            throw re;
        } finally {
            killTimeoutThread(context, timeoutFuture, monitor, doRaise);
        }
    }

    private static Runnable prepareRunnable(final Object monitor, final long waitTime, final AtomicBoolean doRaise, final RubyThread currentThread, final Ruby runtime) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                synchronized (monitor) {
                    doTimedWait(waitTime, doRaise, monitor);
                    if (doRaise.get()) {
                        raiseInThread(runtime, currentThread, runtime.getClassFromPath("Timeout::ExitException"));
                    }
                }
            }
        };
        return timeoutRunnable;
    }

    private static Runnable prepareRunnableWithException(final Object monitor, final long waitTime, final AtomicBoolean doRaise, final RubyThread currentThread, final IRubyObject exception, final Ruby runtime) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                synchronized (monitor) {
                    doTimedWait(waitTime, doRaise, monitor);
                    if (doRaise.get()) {
                        raiseInThread(runtime, currentThread, exception);
                    }
                }
            }
        };
        return timeoutRunnable;
    }

    private static void killTimeoutThread(ThreadContext context, Future timeoutFuture, Object monitor, AtomicBoolean doRaise) {
        synchronized (monitor) {
            doRaise.set(false);
            timeoutFuture.cancel(true);
            context.pollThreadEvents();
        }
    }

    private static void doTimedWait(long waitTime, AtomicBoolean doRaise, Object monitor) {
        try {
            long end = System.currentTimeMillis() + waitTime;
            while (doRaise.get()) {
                long remain = end - System.currentTimeMillis();
                if (remain <= 0) {
                    break;
                }
                monitor.wait(remain);
            }
        } catch (InterruptedException ie) {
            doRaise.set(false);
        }
    }

    private static void raiseInThread(Ruby runtime, RubyThread currentThread, IRubyObject exception) {
        if (currentThread.alive_p().isTrue()) {
            currentThread.internalRaise(new IRubyObject[]{runtime.getClassFromPath("Timeout::ExitException"), runtime.newString("execution expired")});
        }
    }

    private static IRubyObject raiseBecauseCritical(ThreadContext context) {
        Ruby runtime = context.getRuntime();

        return RubyKernel.raise(context, runtime.getKernel(), new IRubyObject[]{runtime.getThreadError(), runtime.newString("timeout within critical section")}, Block.NULL_BLOCK);
    }

    private static IRubyObject raiseTimeoutError(ThreadContext context, RaiseException re) {
        Ruby runtime = context.getRuntime();

        return RubyKernel.raise(
                context,
                runtime.getKernel(),
                new IRubyObject[]{
                    runtime.getClassFromPath("Timeout::Error"),
                    re.getException().callMethod(context, "message"),
                    re.getException().callMethod(context, "backtrace")},
                Block.NULL_BLOCK);
    }
}
