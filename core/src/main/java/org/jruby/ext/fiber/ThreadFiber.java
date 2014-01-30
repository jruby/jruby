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
import org.jruby.runtime.builtin.IRubyObject;

public class ThreadFiber extends RubyObject implements ExecutionContext {
    public ThreadFiber(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
    
    public static void initRootFiber(ThreadContext context) {
        Ruby runtime = context.runtime;
        
        ThreadFiber rootFiber = new ThreadFiber(runtime, runtime.getClass("Fiber")); // FIXME: getFiber()
        
        assert runtime.getClass("SizedQueue") != null : "SizedQueue has not been loaded";
        rootFiber.data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue")), null, rootFiber);
        rootFiber.thread = context.getThread();
        context.setRootFiber(rootFiber);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        
        if (!block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without block");
        
        data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue")), context.getFiberCurrentThread(), this);
        
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
        targetFiberData.queue.push(context, val);

        while (true) {
            try {
                IRubyObject result = currentFiberData.queue.pop(context);
                if (result == NEVER) result = context.nil;
                return result;
            } catch (RaiseException re) {
                if (targetFiberData.queue.isShutdown()) {
                    throw re;
                }

                // forward external exception to the fiber and try again
                targetFiberData.fiber.get().thread.raise(re.getException());
            }
        }
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
        
        FiberData prevFiberData = currentFiberData.prev.data;

        return exchangeWithFiber(context, currentFiberData, prevFiberData, value);
    }
    
    @JRubyMethod
    public IRubyObject __alive__(ThreadContext context) {
        return context.runtime.newBoolean(thread != null && thread.isAlive());
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
        return thread != null && thread.isAlive();
    }
    
    static RubyThread createThread(final Ruby runtime, final FiberData data, final SizedQueue queue, final Block block) {
        final AtomicReference<RubyThread> fiberThread = new AtomicReference();
        Thread thread = new Thread() {
            public void run() {
                ThreadContext context = runtime.getCurrentContext();
                context.setFiber(data.fiber.get());
                context.setRootThread(data.parent);
                fiberThread.set(context.getThread());
                
                IRubyObject init = data.queue.pop(context);
                
                try {
                    IRubyObject result;
                    
                    if (init == NEVER) {
                        result = block.yieldSpecific(context);
                    } else {
                        result = block.yieldArray(context, init, null, null);
                    }
                    
                    data.prev.data.queue.push(context, result);
                } catch (JumpException.FlowControlException fce) {
                    if (data.prev != null) {
                        data.prev.thread.raise(fce.buildException(runtime).getException());
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
                    data.queue.shutdown();
                }
            }
        };
        thread.setDaemon(true);
        thread.setName("FiberThread#" + data.fiber.get().id());
        thread.start();
        
        while (fiberThread.get() == null) {}
        
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
    
    private static class FiberData {
        FiberData(SizedQueue queue, RubyThread parent, ThreadFiber fiber) {
            this.queue = queue;
            this.parent = parent;
            this.fiber = new WeakReference<ThreadFiber>(fiber);
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
