package org.jruby.ext.fiber;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyKernel;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ArraySupport;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class ThreadFiber extends RubyObject implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadFiber.class);

    private static final BiConsumer<Ruby, Runnable> FIBER_LAUNCHER;

    static {
        BiConsumer<Ruby, Runnable> fiberLauncher = null;
        MethodHandle start = null;

        try {
            // test that API is available
            Method ofVirtualMethod = Thread.class.getMethod("ofVirtual");
            Object builder = ofVirtualMethod.invoke(null);
            Method startMethod = Class.forName("java.lang.Thread$Builder").getMethod("start", Runnable.class);

            fiberLauncher = (runtime, runnable) -> {
                try {
                    startMethod.invoke(builder, runnable);
                } catch (Throwable t) {
                    Helpers.throwException(t);
                }
            };
        } catch (Throwable t) {
            fiberLauncher = (runtime, runnable) -> runtime.getFiberExecutor().submit(runnable);
        }

        if (fiberLauncher == null) {
        }

        FIBER_LAUNCHER = fiberLauncher;
    }

    public ThreadFiber(Ruby runtime, RubyClass klass) {
        this(runtime, klass, false);
    }

    public ThreadFiber(Ruby runtime, RubyClass klass, boolean root) {
        super(runtime, klass);

        this.root = root;
    }

    public static void initRootFiber(ThreadContext context, RubyThread currentThread) {
        Ruby runtime = context.runtime;

        ThreadFiber rootFiber = new ThreadFiber(runtime, runtime.getFiber(), true);

        rootFiber.data = new FiberData(new FiberQueue(runtime), currentThread, rootFiber);
        rootFiber.thread = currentThread;
        context.setRootFiber(rootFiber);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        
        if (!block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without block");

        data = new FiberData(new FiberQueue(runtime), context.getFiberCurrentThread(), this);
        
        FiberData currentFiberData = context.getFiber().data;
        
        thread = createThread(runtime, data, currentFiberData.queue, block);
        
        return context.nil;
    }
    
    @JRubyMethod(rest = true, forward = true)
    public IRubyObject resume(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;

        if (!alive()) throw runtime.newFiberError("dead fiber called");

        final FiberData data = this.data;
        if (root || data.prev != null || data.transferred) throw runtime.newFiberError("attempt to resume a resuming fiber");
        
        FiberData currentFiberData = context.getFiber().data;
        
        if (data == currentFiberData) {
            switch (values.length) {
                case 0: return context.nil;
                case 1: return values[0];
                default: return RubyArray.newArrayMayCopy(runtime, values);
            }
        }
        
        FiberRequest val;
        switch (values.length) {
            case 0: val = NEVER; break;
            case 1: val = new FiberRequest(values[0], RequestType.DATA); break;
            default: val = new FiberRequest(RubyArray.newArrayMayCopy(runtime, values), RequestType.DATA);
        }
        
        if (data.parent != context.getFiberCurrentThread()) throw runtime.newFiberError("fiber called across threads");
        
        data.prev = context.getFiber();

        FiberRequest result;
        try {
            result = exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            data.prev = null;
        }

        if (result.type == RequestType.RAISE) {
            throw ((RubyException) result.data).toThrowable();
        }

        return processResultData(context, result);
    }

    @JRubyMethod(rest = true)
    public IRubyObject raise(ThreadContext context, IRubyObject[] args) {
        return raise(context, RubyThread.prepareRaiseException(context, args));
    }

    public enum RequestType {
        DATA,
        RAISE
    }

    public static class FiberRequest {
        final IRubyObject data;
        final RequestType type;

        FiberRequest(IRubyObject data, RequestType type) {
            this.data = data;
            this.type = type;
        }
    }

    private static final FiberRequest NEVER = new FiberRequest(RubyBasicObject.NEVER, RequestType.DATA);

    private static FiberRequest exchangeWithFiber(ThreadContext context, FiberData currentFiberData, FiberData targetFiberData, FiberRequest request) {
        // push should not block and does not check interrupts
        targetFiberData.queue.push(context, request);

        // At this point we consider ourselves "in" the resume, so we need to make an effort to propagate any interrupt
        // raise to the fiber. We forward the exception using the interrupt mechanism and then try once more to let the
        // fiber deal with the exception and either re-raise it or handle it and give us a non-exceptional result. If
        // the second pop is interrupted, either the fiber has propagated the exception back to us or we are being
        // interrupted again and must abandon the fiber.

        try {
            return currentFiberData.queue.pop(context);
        } catch (RaiseException re) {
            handleExceptionDuringExchange(context, currentFiberData, targetFiberData, re);

            // if we get here, we forwarded exception so try once more
            return currentFiberData.queue.pop(context);
        }
    }

    /**
     * Handle exceptions raised while exchanging data with a fiber.
     *
     * The rules work like this:
     *
     * <ul>
     *     <li>If the thread has called Fiber#resume on the fiber and an interrupt is sent to the thread,
     *     forward it to the fiber</li>
     *     <li>If the fiber has called Fiber.yield and an interrupt is sent to the fiber (e.g. Timeout.timeout(x) { Fiber.yield })
     *     forward it to the fiber's parent thread.</li>
     * </ul>
     *
     * @param context
     * @param currentFiberData
     * @param targetFiberData
     * @param re
     */
    private static void handleExceptionDuringExchange(ThreadContext context, FiberData currentFiberData, FiberData targetFiberData, RaiseException re) {
        // If we received a LJC we need to bubble it out
        if (context.runtime.getLocalJumpError().isInstance(re.getException())) {
            throw re;
        }

        // If we were trying to yield but our queue has been shut down,
        // let the exception bubble out and (ideally) kill us.
        if (currentFiberData.queue.isShutdown()) {
            throw re;
        }

        // re-raise if the target fiber has been shut down
        if (targetFiberData.queue.isShutdown()) {
            throw re;
        }

        // Otherwise, we want to forward the exception to the target fiber
        // since it has the ball
        final ThreadFiber fiber = targetFiberData.fiber.get();
        if ( fiber != null && fiber.alive() ) {
            fiber.thread.raise(re.getException());
        } else {
            // target fiber has gone away, it's our ball now
            throw re;
        }
    }

    @JRubyMethod(rest = true)
    public IRubyObject __transfer__(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;

        final FiberData data = this.data;
        if (data.prev != null) throw runtime.newFiberError("double resume");
        
        if (!alive()) throw runtime.newFiberError("dead fiber called");
        
        FiberData currentFiberData = context.getFiber().data;
        
        if (data == currentFiberData) {
            switch (values.length) {
                case 0: return context.nil;
                case 1: return values[0];
                default: return RubyArray.newArrayMayCopy(runtime, values);
            }
        }
        
        FiberRequest val;
        switch (values.length) {
            case 0: val = NEVER; break;
            case 1: val = new FiberRequest(values[0], RequestType.DATA); break;
            default: val = new FiberRequest(RubyArray.newArrayMayCopy(runtime, values), RequestType.DATA);
        }
        
        if (data.parent != context.getFiberCurrentThread()) throw runtime.newFiberError("fiber called across threads");
        
        if (currentFiberData.prev != null) {
            // new fiber should answer to current prev and this fiber is marked as transferred
            data.prev = currentFiberData.prev;
            currentFiberData.prev = null;
            currentFiberData.transferred = true;
        } else {
            data.prev = context.getFiber();
        }

        FiberRequest result;
        try {
            result = exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            data.prev = null;
            currentFiberData.transferred = false;
        }

        if (result.type == RequestType.RAISE) {
            throw ((RubyException) result.data).toThrowable();
        }

        return processResultData(context, result);
    }
    public IRubyObject raise(ThreadContext context, IRubyObject exception) {
        Ruby runtime = context.runtime;

        final FiberData data = this.data;
        if (data.prev != null) throw runtime.newFiberError("double resume");

        if (!alive()) throw runtime.newFiberError("dead fiber called");

        FiberData currentFiberData = context.getFiber().data;

        if (data == currentFiberData) {
            RubyKernel.raise(context, this, exception);
        }

        if (data.parent != context.getFiberCurrentThread()) throw runtime.newFiberError("fiber called across threads");

        FiberRequest val = new FiberRequest(exception, RequestType.RAISE);

        data.prev = context.getFiber();

        FiberRequest result = null;
        try {
            result = exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            if (result == null) {
                // exception raised, mark transfer as completed
                data.prev = null;
                currentFiberData.transferred = false;
            }
        }

        if (result.type == RequestType.RAISE) {
            throw ((RubyException) result.data).toThrowable();
        }

        return processResultData(context, result);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv) {
        return ThreadFiber.yield(context, recv, context.nil);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject value) {
        Ruby runtime = context.runtime;

        FiberData currentFiberData = verifyCurrentFiber(context, runtime);
        FiberData prevFiberData = currentFiberData.prev.data;

        FiberRequest result = exchangeWithFiber(context, currentFiberData, prevFiberData, new FiberRequest(value, RequestType.DATA));

        if (result.type == RequestType.RAISE) {
            throw ((RubyException) result.data).toThrowable();
        }

        return processResultData(context, result);
    }

    private static IRubyObject processResultData(ThreadContext context, FiberRequest result) {
        IRubyObject data = result.data;

        if (data == RubyBasicObject.NEVER) return context.nil;

        return data;
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject[] value) {
        switch (value.length) {
            case 0: return ThreadFiber.yield(context, recv);
            case 1: return ThreadFiber.yield(context, recv, value[0]);
        }

        Ruby runtime = context.runtime;

        FiberData currentFiberData = verifyCurrentFiber(context, runtime);
        FiberData prevFiberData = currentFiberData.prev.data;

        FiberRequest result = exchangeWithFiber(context, currentFiberData, prevFiberData, new FiberRequest(RubyArray.newArrayNoCopy(runtime, value), RequestType.DATA));

        if (result.type == RequestType.RAISE) {
            throw ((RubyException) result.data).toThrowable();
        }

        return processResultData(context, result);
    }

    private static FiberData verifyCurrentFiber(ThreadContext context, Ruby runtime) {
        FiberData currentFiberData = context.getFiber().data;

        if (currentFiberData.parent == null) throw runtime.newFiberError("can't yield from root fiber");

        if (currentFiberData.prev == null)
            throw runtime.newFiberError("BUG: yield occurred with null previous fiber. Report this at http://bugs.jruby.org");

        if (currentFiberData.queue.isShutdown()) throw runtime.newFiberError("dead fiber yielded");
        return currentFiberData;
    }

    @JRubyMethod
    public IRubyObject __alive__(ThreadContext context) {
        return RubyBoolean.newBoolean(context, alive());
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject __current__(ThreadContext context, IRubyObject recv) {
        return context.getFiber();
    }

    @Override
    public Map<Object, IRubyObject> getContextVariables() {
        return thread.getContextVariables();
    }
    
    final boolean alive() {
        RubyThread thread = this.thread;
        if (thread == null || !thread.isAlive() || data.queue.isShutdown()) {
            return false;
        }

        return true;
    }
    
    static RubyThread createThread(final Ruby runtime, final FiberData data, final FiberQueue queue, final Block block) {
        final AtomicReference<RubyThread> fiberThread = new AtomicReference();

        // retry with GC once
        boolean retried = false;

        while (!retried) {
            try {
                FIBER_LAUNCHER.accept(runtime, () -> {
                    ThreadContext context = runtime.getCurrentContext();
                    context.setFiber(data.fiber.get());
                    context.useRecursionGuardsFrom(data.parent.getContext());
                    fiberThread.set(context.getThread());
                    context.getThread().setFiberCurrentThread(data.parent);

                    Thread thread = Thread.currentThread();
                    String oldName = thread.getName();
                    thread.setName("Fiber thread for block at: " + block.getBody().getFile() + ":" + block.getBody().getLine());

                    try {
                        FiberRequest init = data.queue.pop(context);

                        try {
                            FiberRequest result;

                            if (init == NEVER) {
                                result = new FiberRequest(block.yieldSpecific(context), RequestType.DATA);
                            } else {
                                result = new FiberRequest(block.yieldArray(context, init.data, null), RequestType.DATA);
                            }

                            // Clear ThreadFiber's thread since we're on the way out and need to appear non-alive?
                            // Waiting thread can proceed immediately after push below but before we are truly dead.
                            // See https://github.com/jruby/jruby/issues/4838
                            ThreadFiber tf = data.fiber.get();
                            if (tf != null) tf.thread = null;

                            data.prev.data.queue.push(context, result);
                        } finally {
                            // Ensure we do everything for shutdown now
                            data.queue.shutdown();
                            runtime.getThreadService().unregisterCurrentThread(context);
                            ThreadFiber tf = data.fiber.get();
                            if (tf != null) tf.thread = null;
                        }
                    } catch (JumpException.FlowControlException fce) {
                        if (data.prev != null) {
                            data.prev.thread.raise(fce.buildException(runtime).getException());
                        }
                    } catch (IRBreakJump bj) {
                        // This is one of the rare cases where IR flow-control jumps
                        // leaks into the runtime impl.
                        if (data.prev != null) {
                            data.prev.thread.raise(((RaiseException) IRException.BREAK_LocalJumpError.getException(runtime)).getException());
                        }
                    } catch (IRReturnJump rj) {
                        // This is one of the rare cases where IR flow-control jumps
                        // leaks into the runtime impl.
                        if (data.prev != null) {
                            data.prev.thread.raise(((RaiseException) IRException.RETURN_LocalJumpError.getException(runtime)).getException());
                        }
                    } catch (RaiseException re) {
                        if (data.prev != null) {
                            data.prev.data.queue.push(context, new FiberRequest(re.getException(), RequestType.RAISE));
                        }
                    } catch (Throwable t) {
                        if (data.prev != null) {
                            data.prev.thread.raise(JavaUtil.convertJavaToUsableRubyObject(runtime, t));
                        }
                    } finally {
                        thread.setName(oldName);
                    }
                });

                // Successfully submitted to executor, break out of retry loop
                break;
            } catch (OutOfMemoryError oome) {
                oome.printStackTrace();
                String oomeMessage = oome.getMessage();
                if (!retried && oomeMessage != null && oomeMessage.contains("unable to create new native thread")) {
                    // try to clean out stale enumerator threads by forcing GC
                    System.gc();
                    retried = true;
                } else {
                    throw oome;
                }
            }
        }
        
        while (fiberThread.get() == null) { Thread.yield(); }
        
        return fiberThread.get();
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject __finalize__(ThreadContext context) {
        try {
            doFinalize();
        } catch (Exception ignore) { return context.fals; }
        return context.nil;
    }

    private void doFinalize() {
        FiberData data = this.data;
        this.data = null;
        if (data != null) {
            // we never interrupt or shutdown root fibers
            if (data.parent == null) return;

            data.queue.shutdown();
        }

        RubyThread thread = this.thread;
        this.thread = null;
        if (thread != null) {
            thread.dieFromFinalizer();

            // interrupt Ruby thread to break out of queue sleep, blocking IO
            thread.interrupt();

            // null out references to aid GC
            data = null; thread = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            doFinalize();
        } finally {
            super.finalize();
        }
    }

    public FiberData getData() {
        return data;
    }

    public RubyThread getThread() {
        return thread;
    }
    
    public static class FiberData {
        FiberData(FiberQueue queue, RubyThread parent, ThreadFiber fiber) {
            this.queue = queue;
            this.parent = parent;
            this.fiber = new WeakReference<ThreadFiber>(fiber);
        }

        public ThreadFiber getPrev() {
            return prev;
        }
        
        final FiberQueue queue;
        volatile ThreadFiber prev;
        final RubyThread parent;
        final WeakReference<ThreadFiber> fiber;
        volatile boolean transferred;
    }
    
    volatile FiberData data;
    volatile RubyThread thread;
    final boolean root;
}
