package org.jruby.ext;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.threading.DaemonThreadFactory;

public class Timeout implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule timeout = runtime.defineModule("Timeout");
        RubyClass timeoutError = runtime.defineClassUnder("Error", runtime.getInterrupt(), runtime.getInterrupt().getAllocator(), timeout);
        runtime.defineClassUnder("ExitException", runtime.getException(), runtime.getInterrupt().getAllocator(), timeout);

        // Here we create an "anonymous" exception type used for unrolling the stack.
        // MRI creates a new one for *every call* to timeout, which can be costly.
        // We opt to use a single exception type for all cases to avoid this overhead.
        RubyClass anonEx = runtime.defineClassUnder("AnonymousException", runtime.getException(), runtime.getInterrupt().getAllocator(), timeout);
        anonEx.setBaseName(null); // clear basename so it's anonymous when raising

        // These are not really used by timeout, but exposed for compatibility
        timeout.defineConstant("THIS_FILE", RubyRegexp.newRegexp(runtime, "timeout\\.rb", 0));
        timeout.defineConstant("CALLER_OFFSET", RubyFixnum.newFixnum(runtime, 0));

        // Timeout module methods
        timeout.defineAnnotatedMethods(Timeout.class);

        // Toplevel defines
        runtime.getObject().defineConstant("TimeoutError", timeoutError);
        runtime.getObject().defineAnnotatedMethods(TimeoutToplevel.class);
    }

    private static ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new DaemonThreadFactory());

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

        final RubyThread currentThread = context.getThread();
        
        Runnable timeoutRunnable = prepareRunnable(currentThread, runtime);
        Future timeoutFuture = null;

        try {
            try {
                timeoutFuture = timeoutExecutor.schedule(timeoutRunnable,
                        (long)(seconds.convertToFloat().getDoubleValue() * 1000000), TimeUnit.MICROSECONDS);

                return block.yield(context, seconds);
            } finally {
                killTimeoutThread(context, timeoutFuture);
            }
        } catch (RaiseException re) {
            if (re.getException().getMetaClass() == runtime.getClassFromPath("Timeout::AnonymousException")) {
                return raiseTimeoutError(context, re);
            } else {
                throw re;
            }
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

        final IRubyObject exception = exceptionType.isNil() ? runtime.getClassFromPath("Timeout::AnonymousException") : exceptionType;
        final RubyThread currentThread = context.getThread();
        
        Runnable timeoutRunnable = prepareRunnableWithException(currentThread, exception, runtime);
        Future timeoutFuture = null;

        try {
            try {
                timeoutFuture = timeoutExecutor.schedule(timeoutRunnable,
                        (long)(seconds.convertToFloat().getDoubleValue() * 1000000), TimeUnit.MICROSECONDS);

                return block.yield(context, seconds);
            } finally {
                killTimeoutThread(context, timeoutFuture);
            }
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
        }
    }

    private static Runnable prepareRunnable(final RubyThread currentThread, final Ruby runtime) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                raiseInThread(runtime, currentThread, runtime.getClassFromPath("Timeout::AnonymousException"));
            }
        };
        return timeoutRunnable;
    }

    private static Runnable prepareRunnableWithException(final RubyThread currentThread, final IRubyObject exception, final Ruby runtime) {
        Runnable timeoutRunnable = new Runnable() {
            public void run() {
                raiseInThread(runtime, currentThread, exception);
            }
        };
        return timeoutRunnable;
    }

    private static void killTimeoutThread(ThreadContext context, Future timeoutFuture) {
        boolean cancelled = timeoutFuture.cancel(false);

        //
        // Remove the Executor task now, to avoid cancelled tasks accumulating
        // until an Executor thread can schedule and remove them
        //
        if (cancelled && timeoutExecutor instanceof ScheduledThreadPoolExecutor && timeoutFuture instanceof Runnable) {
            ((ScheduledThreadPoolExecutor) timeoutExecutor).remove((Runnable) timeoutFuture);
        }
        //
        // If the task was scheduled, wait for it to finish before polling
        // for the exception it would have thrown.
        //
        if (!cancelled) {
            try {
                timeoutFuture.get();
            } catch (java.util.concurrent.ExecutionException ex) {
            } catch (InterruptedException ex) {
            }
        }
        context.pollThreadEvents();
    }
    
    private static void raiseInThread(Ruby runtime, RubyThread currentThread, IRubyObject exception) {
        if (currentThread.alive_p().isTrue()) {
            currentThread.internalRaise(new IRubyObject[]{exception, runtime.newString("execution expired")});
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
