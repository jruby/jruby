package org.jruby.ext.fiber;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.thread.SizedQueue;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.ir.runtime.IRBreakJump;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.ir.operands.IRException;

public class ThreadFiber extends RubyObject implements ExecutionContext {
    public ThreadFiber(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
    
    public static void initRootFiber(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        ThreadFiber rootFiber = new ThreadFiber(runtime, runtime.getClass("Fiber")); // FIXME: getFiber()
        
        assert runtime.getClass("SizedQueue") != null : "SizedQueue has not been loaded";
        rootFiber.data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue"), 1), null, rootFiber);
        rootFiber.thread = context.getThread();
        context.setRootFiber(rootFiber);
    }
    
    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        
        if (!block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without block");

        data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue"), 1), context.getFiberCurrentThread(), this);
        
        FiberData currentFiberData = context.getFiber().data;
        
        thread = createThread(runtime, data, currentFiberData.queue, block);
        
        return context.nil;
    }
    
    @JRubyMethod(rest = true)
    public IRubyObject resume(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;
        
        if (data.prev != null || data.transferred) throw runtime.newFiberError("double resume");
        
        if (!alive()) throw runtime.newFiberError("dead fiber called");
        
        FiberData currentFiberData = context.getFiber().data;
        
        if (this.data == currentFiberData) {
            switch (values.length) {
                case 0: return context.nil;
                case 1: return values[0];
                default: return runtime.newArrayNoCopyLight(values);
            }
        }
        
        IRubyObject val;
        switch (values.length) {
            case 0: val = NEVER; break;
            case 1: val = values[0]; break;
            default: val = runtime.newArrayNoCopyLight(values);
        }
        
        if (data.parent != context.getFiberCurrentThread()) throw runtime.newFiberError("fiber called across threads");
        
        data.prev = context.getFiber();

        try {
            return exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            data.prev = null;
        }
    }

    private static IRubyObject exchangeWithFiber(ThreadContext context, FiberData currentFiberData, FiberData targetFiberData, IRubyObject val) {
        // At this point we consider ourselves "in" the resume, so we need to enforce exception-propagation
        // rules for both the push (to wake up fiber) and pop (to wait for fiber). Failure to do this can
        // cause interrupts destined for the fiber to be caught after the fiber is running but before the
        // resuming thread has started waiting for it, leaving the fiber to run rather than receiving the
        // interrupt, and the parent thread propagates the error.

        // Note: these need to be separate try/catches because of the while loop.
        try {
            targetFiberData.queue.push(context, new IRubyObject[] {val});
        } catch (RaiseException re) {
            handleExceptionDuringExchange(context, currentFiberData, targetFiberData, re);
        }

        while (true) {
            try {
                IRubyObject result = currentFiberData.queue.pop(context);
                if (result == NEVER) result = context.nil;
                return result;
            } catch (RaiseException re) {
                handleExceptionDuringExchange(context, currentFiberData, targetFiberData, re);
            }
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
        targetFiberData.fiber.get().thread.raise(re.getException());
    }

    @JRubyMethod(rest = true)
    public IRubyObject __transfer__(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;
        
        if (data.prev != null) throw runtime.newFiberError("double resume");
        
        if (!alive()) throw runtime.newFiberError("dead fiber called");
        
        FiberData currentFiberData = context.getFiber().data;
        
        if (this.data == currentFiberData) {
            switch (values.length) {
                case 0: return context.nil;
                case 1: return values[0];
                default: return runtime.newArrayNoCopyLight(values);
            }
        }
        
        IRubyObject val;
        switch (values.length) {
            case 0: val = NEVER; break;
            case 1: val = values[0]; break;
            default: val = runtime.newArrayNoCopyLight(values);
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
        
        try {
            return exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            data.prev = null;
            currentFiberData.transferred = false;
        }
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv) {
        return yield(context, recv, context.nil);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject value) {
        Ruby runtime = context.runtime;
        
        FiberData currentFiberData = context.getFiber().data;
        
        if (currentFiberData.parent == null) throw runtime.newFiberError("can't yield from root fiber");

        if (currentFiberData.prev == null) throw runtime.newFiberError("BUG: yield occured with null previous fiber. Report this at http://bugs.jruby.org");

        if (currentFiberData.queue.isShutdown()) throw runtime.newFiberError("dead fiber yielded");
        
        FiberData prevFiberData = currentFiberData.prev.data;

        return exchangeWithFiber(context, currentFiberData, prevFiberData, value);
    }
    
    @JRubyMethod
    public IRubyObject __alive__(ThreadContext context) {
        return context.runtime.newBoolean(alive());
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject __current__(ThreadContext context, IRubyObject recv) {
        return context.getFiber();
    }

    @Override
    public Map<Object, IRubyObject> getContextVariables() {
        return thread.getContextVariables();
    }
    
    boolean alive() {
        return thread != null && thread.isAlive() && !data.queue.isShutdown();
    }
    
    static RubyThread createThread(final Ruby runtime, final FiberData data, final SizedQueue queue, final Block block) {
        final AtomicReference<RubyThread> fiberThread = new AtomicReference();
        runtime.getFiberExecutor().execute(new Runnable() {
            public void run() {
                ThreadContext context = runtime.getCurrentContext();
                context.setFiber(data.fiber.get());
                context.setRootThread(data.parent);
                fiberThread.set(context.getThread());

                try {
                    IRubyObject init = data.queue.pop(context);
                    
                    try {
                        IRubyObject result;

                        if (init == NEVER) {
                            result = block.yieldSpecific(context);
                        } else {
                            result = block.yieldArray(context, init, null);
                        }

                        data.prev.data.queue.push(context, new IRubyObject[] { result });
                    } finally {
                        data.queue.shutdown();
                        runtime.getThreadService().disposeCurrentThread();
                    }
                } catch (JumpException.FlowControlException fce) {
                    if (data.prev != null) {
                        data.prev.thread.raise(fce.buildException(runtime).getException());
                    }
                } catch (IRBreakJump bj) {
                    // This is one of the rare cases where IR flow-control jumps
                    // leaks into the runtime impl.
                    if (data.prev != null) {
                        data.prev.thread.raise(((RaiseException)IRException.BREAK_LocalJumpError.getException(runtime)).getException());
                    }
                } catch (IRReturnJump rj) {
                    // This is one of the rare cases where IR flow-control jumps
                    // leaks into the runtime impl.
                    if (data.prev != null) {
                        data.prev.thread.raise(((RaiseException)IRException.RETURN_LocalJumpError.getException(runtime)).getException());
                    }
                } catch (RaiseException re) {
                    if (data.prev != null) {
                        data.prev.thread.raise(re.getException());
                    }
                } catch (Throwable t) {
                    if (data.prev != null) {
                        data.prev.thread.raise(JavaUtil.convertJavaToUsableRubyObject(runtime, t));
                    }
                } finally {
                    // clear reference to the fiber's thread
                    ThreadFiber tf = data.fiber.get();
                    if (tf != null) tf.thread = null;
                }
            }
        });
        
        while (fiberThread.get() == null) {Thread.yield();}
        
        return fiberThread.get();
    }
    
    protected void finalize() throws Throwable {
        try {
            FiberData data = this.data;
            if (data != null) {
                // we never interrupt or shutdown root fibers
                if (data.parent == null) return;
                
                data.queue.shutdown();
            }

            RubyThread thread = this.thread;
            if (thread != null) {
                thread.dieFromFinalizer();

                // interrupt Ruby thread to break out of queue sleep, blocking IO
                thread.interrupt();

                // null out references to aid GC
                data = null;
                thread = null;
            }
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
        FiberData(SizedQueue queue, RubyThread parent, ThreadFiber fiber) {
            this.queue = queue;
            this.parent = parent;
            this.fiber = new WeakReference<ThreadFiber>(fiber);
        }

        public ThreadFiber getPrev() {
            return prev;
        }
        
        final SizedQueue queue;
        volatile ThreadFiber prev;
        final RubyThread parent;
        final WeakReference<ThreadFiber> fiber;
        volatile boolean transferred;
    }
    
    volatile FiberData data;
    volatile RubyThread thread;
}
