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
        
        rootFiber.data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue")), null, rootFiber);
        rootFiber.thread = context.getThread();
        context.setRootFiber(rootFiber);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        
        if (!block.isGiven()) throw runtime.newArgumentError("tried to create Proc object without block");
        
        data = new FiberData(new SizedQueue(runtime, runtime.getClass("SizedQueue")), context.getFiberCurrentThread(), this);
        
        ThreadFiber currentFiber = context.getFiber();
        
        thread = createThread(runtime, data, currentFiber.data.queue, block);
        
        return context.nil;
    }
    
    @JRubyMethod(rest = true)
    public IRubyObject resume(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;
        
        if (data.prev != null || data.transferred) throw runtime.newFiberError("double resume");
        
        if (!alive()) throw runtime.newFiberError("dead fiber called");
        
        ThreadFiber currentFiber = context.getFiber();
        
        if (this == currentFiber) {
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
        
        data.prev = currentFiber;
        
        try {
            data.queue.push(context, val);
            return currentFiber.data.queue.pop(context);
        } finally {
            data.prev = null;
        }
    }
    
    @JRubyMethod(rest = true)
    public IRubyObject __transfer__(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;
        
        if (data.prev != null) throw runtime.newFiberError("double resume");
        
        if (!alive()) throw runtime.newFiberError("dead fiber called");
        
        ThreadFiber currentFiber = context.getFiber();
        
        if (this == currentFiber) {
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
        
        if (currentFiber.data.prev != null) {
            // new fiber should answer to current prev and this fiber is marked as transferred
            data.prev = currentFiber.data.prev;
            currentFiber.data.prev = null;
            currentFiber.data.transferred = true;
        } else {
            data.prev = currentFiber;
        }
        
        try {
            data.queue.push(context, val);
            return currentFiber.data.queue.pop(context);
        } finally {
            data.prev = null;
            currentFiber.data.transferred = false;
        }
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv) {
        return yield(context, recv, context.nil);
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject value) {
        Ruby runtime = context.runtime;
        
        ThreadFiber currentFiber = context.getFiber();
        
        if (currentFiber.data.parent == null) throw runtime.newFiberError("can't yield from root fiber");
        
        ThreadFiber prevFiber = currentFiber.data.prev;
        
        prevFiber.data.queue.push(context, value);
        
        
        return currentFiber.data.queue.pop(context);
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
                    t.printStackTrace();
                }
            }
        };
        thread.setDaemon(true);
        thread.setName("FiberThread#" + data.fiber.get().id());
        thread.start();
        
        while (fiberThread.get() == null) {}
        
        return fiberThread.get();
    }
    
    @Override
    public void finalize() throws Throwable {
        super.finalize();
        data.queue.shutdown();
        thread.kill();
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
