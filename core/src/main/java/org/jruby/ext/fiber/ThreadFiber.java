package org.jruby.ext.fiber;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.FiberKill;
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
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.castAsHash;
import static org.jruby.api.Create.newHash;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warnExperimental;

public class ThreadFiber extends RubyObject implements ExecutionContext {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadFiber.class);

    private static final BiConsumer<Ruby, Runnable> FIBER_LAUNCHER;
    private static final MethodHandle VTHREAD_START_METHOD;
    private static final String[] INITIALIZE_KWARGS = {"blocking", "pool", "storage"};

    static {
        BiConsumer<Ruby, Runnable> fiberLauncher = ThreadFiber::nativeThreadLauncher;
        MethodHandle start = null;

        if (Options.FIBER_VTHREADS.load()) {
            try {
                // test that API is available
                Method ofVirtualMethod = Thread.class.getMethod("ofVirtual");
                Object builder = ofVirtualMethod.invoke(null);
                Method startMethod = Class.forName("java.lang.Thread$Builder").getMethod("start", Runnable.class);

                start = MethodHandles.publicLookup().unreflect(startMethod).bindTo(builder);

                fiberLauncher = new VirtualThreadLauncher();
            } catch (Throwable t) {
                // default impl set below
            }
        }

        VTHREAD_START_METHOD = start;
        FIBER_LAUNCHER = fiberLauncher;
    }

    private static void nativeThreadLauncher(Ruby runtime, Runnable runnable) {
        runtime.getFiberExecutor().submit(runnable);
    }

    public boolean isBlocking() {
        return data.blocking;
    }

    private static class VirtualThreadLauncher implements BiConsumer<Ruby, Runnable> {
        @Override
        public void accept(Ruby ruby, Runnable runnable) {
            try {
                VTHREAD_START_METHOD.invokeWithArguments(runnable);
            } catch (Throwable t) {
                Helpers.throwException(t);
            }
        }
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

        rootFiber.data = new FiberData(new FiberQueue(runtime), currentThread, rootFiber, true);
        rootFiber.thread = currentThread;
        context.setRootFiber(rootFiber);
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;

        if (!block.isGiven()) throw argumentError(context, "tried to create Proc object without block");

        inheritFiberStorage(context);

        data = new FiberData(new FiberQueue(runtime), context.getFiberCurrentThread(), this, false);

        FiberData currentFiberData = context.getFiber().data;

        thread = createThread(context, data, currentFiberData.queue, block);

        return context.nil;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject _opts, Block block) {
        if (!block.isGiven()) throw argumentError(context, "tried to create Proc object without block");

        IRubyObject opts = ArgsUtil.getOptionsArg(context, _opts);
        boolean blocking = false;

        if (!opts.isNil()) {
            IRubyObject[] blockingPoolOpt = ArgsUtil.extractKeywordArgs(context, opts, INITIALIZE_KWARGS);

            if (blockingPoolOpt != null) {
                IRubyObject blockingOpt = blockingPoolOpt[0];
                if (blockingOpt != null && !blockingOpt.isNil()) {
                    blocking = blockingOpt.isTrue();
                }

                // TODO: pooling

                IRubyObject storage = blockingPoolOpt[2];

                if (storage == null || storage == context.tru) {
                    inheritFiberStorage(context);
                } else {
                    setStorage(context, storage);
                }
            }
        }

        data = new FiberData(new FiberQueue(context.runtime), context.getFiberCurrentThread(), this, blocking);

        FiberData currentFiberData = context.getFiber().data;

        thread = createThread(context, data, currentFiberData.queue, block);

        return context.nil;
    }

    // MRI: inherit_fiber_storage
    public void inheritFiberStorage(ThreadContext context) {
        RubyHash storage = context.getFiber().storage;
        this.storage = storage == null ? null : (RubyHash) storage.dup(context);
    }
    
    @JRubyMethod(rest = true, keywords = true)
    public IRubyObject resume(ThreadContext context, IRubyObject[] values) {
        Ruby runtime = context.runtime;

        if (!alive()) throw runtime.newFiberError("dead fiber called");

        final FiberData data = this.data;
        FiberData currentFiberData = context.getFiber().data;

        if (currentFiberData == data) throw runtime.newFiberError("attempt to resume the current fiber");
        if (root || data.prev != null || data.transferred) throw runtime.newFiberError("attempt to resume a resuming fiber");
        
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
        
        if (data.parent != context.getFiberCurrentThread()) fiberCalledAcrossThreads(runtime);

        data.prev = context.getFiber();

        FiberRequest result;
        try {
            result = exchangeWithFiber(context, currentFiberData, data, val);
        } finally {
            data.prev = null;
        }

        if (data.blocking) {
            context.getFiberCurrentThread().decrementBlocking();
        }

        if (result.type == RequestType.RAISE) {
            throw (RuntimeException) result.data;
        }

        return processResultData(context, result);
    }

    private static void fiberCalledAcrossThreads(Ruby runtime) {
        throw runtime.newFiberError("fiber called across threads");
    }

    @JRubyMethod(optional = 4, checkArity = false, keywords = true)
    public IRubyObject raise(ThreadContext context, IRubyObject[] args) {
        return raise(context, RubyThread.prepareRaiseException(context, args));
    }

    public enum RequestType {
        DATA,
        RAISE
    }

    public static class FiberRequest {
        final Object data;
        final RequestType type;

        FiberRequest(Object data, RequestType type) {
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
            adjustThreadBlocking(context, currentFiberData, targetFiberData);

            return currentFiberData.queue.pop(context);
        } catch (RaiseException re) {
            handleExceptionDuringExchange(context, currentFiberData, targetFiberData, re);

            // if we get here, we forwarded exception so try once more
            return currentFiberData.queue.pop(context);
        } finally {
            adjustThreadBlocking(context, targetFiberData, currentFiberData);
        }
    }

    private static void adjustThreadBlocking(ThreadContext context, FiberData currentFiberData, FiberData targetFiberData) {
        // if fiber we are leaving is blocking, decrement thread blocking count
        if (currentFiberData.blocking) {
            context.getFiberCurrentThread().decrementBlocking();
        }

        // if fiber we are entering is blocking, increment thread blocking count
        if (targetFiberData.blocking) {
            context.getFiberCurrentThread().incrementBlocking();
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
    public IRubyObject transfer(ThreadContext context, IRubyObject[] values) {
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
        
        if (data.parent != context.getFiberCurrentThread()) fiberCalledAcrossThreads(runtime);

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
            throw (RuntimeException) result.data;
        }

        return processResultData(context, result);
    }

    public IRubyObject raise(ThreadContext context, IRubyObject exception) {
        Ruby runtime = context.runtime;

        if (!alive()) throw runtime.newFiberError("dead fiber called");

        FiberData currentFiberData = context.getFiber().data;

        if (data == currentFiberData) {
            RubyKernel.raise(context, this, exception);
        }

        final FiberData data = this.data;
        if (data.prev != null) throw runtime.newFiberError("double resume");

        if (data.parent != context.getFiberCurrentThread()) fiberCalledAcrossThreads(runtime);

        FiberRequest val = new FiberRequest(((RubyException) exception).toThrowable(), RequestType.RAISE);

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
            throw (RuntimeException) result.data;
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
            throw (RuntimeException) result.data;
        }

        return processResultData(context, result);
    }

    private static IRubyObject processResultData(ThreadContext context, FiberRequest result) {
        IRubyObject data = (IRubyObject) result.data;

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
            throw (RuntimeException) result.data;
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

    @JRubyMethod(name = "alive?")
    public IRubyObject alive_p(ThreadContext context) {
        return asBoolean(context, alive());
    }
    
    @JRubyMethod(meta = true)
    public static IRubyObject current(ThreadContext context, IRubyObject recv) {
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
    
    static RubyThread createThread(ThreadContext context, final FiberData data, final FiberQueue queue, final Block block) {
        final AtomicReference<RubyThread> fiberThread = new AtomicReference();

        // retry with GC once
        boolean retried = false;

        while (!retried) {
            try {
                FIBER_LAUNCHER.accept(context.runtime, () -> {
                    ThreadContext ctxt = context.runtime.getCurrentContext();
                    ctxt.setFiber(data.fiber.get());
                    ctxt.useRecursionGuardsFrom(data.parent.getContext());
                    RubyThread rubyThread = ctxt.getThread();
                    fiberThread.set(rubyThread);
                    rubyThread.setFiberCurrentThread(data.parent);

                    Thread thread = Thread.currentThread();
                    String oldName = thread.getName();
                    thread.setName("Fiber thread for block at: " + block.getBody().getFile() + ":" + block.getBody().getLine());

                    try {
                        FiberRequest init = data.queue.pop(ctxt);

                        try {
                            FiberRequest result;

                            if (init == NEVER) {
                                result = new FiberRequest(block.yieldSpecific(ctxt), RequestType.DATA);
                            } else {
                                // terminated before first resume
                                if (init.type == RequestType.RAISE) {
                                    throw (RuntimeException) init.data;
                                }

                                result = new FiberRequest(block.yieldArray(ctxt, (IRubyObject) init.data, null), RequestType.DATA);
                            }

                            // Clear ThreadFiber's thread since we're on the way out and need to appear non-alive?
                            // Waiting thread can proceed immediately after push below but before we are truly dead.
                            // See https://github.com/jruby/jruby/issues/4838
                            ThreadFiber tf = data.fiber.get();
                            if (tf != null) tf.thread = null;

                            data.prev.data.queue.push(ctxt, result);
                        } finally {
                            // Ensure we do everything for shutdown now
                            data.queue.shutdown();
                            context.runtime.getThreadService().unregisterCurrentThread(ctxt);
                            ThreadFiber tf = data.fiber.get();
                            if (tf != null) tf.thread = null;
                        }
                    } catch (FiberKill fk) {
                        // push a final result and quietly die
                        if (data.prev != null) {
                            data.prev.data.queue.push(ctxt, new FiberRequest(ctxt.nil, RequestType.DATA));
                        }
                    } catch (JumpException.FlowControlException fce) {
                        if (data.prev != null) {
                            data.prev.thread.raise(fce.buildException(context.runtime).getException());
                        }
                    } catch (IRBreakJump bj) {
                        // This is one of the rare cases where IR flow-control jumps
                        // leaks into the runtime impl.
                        if (data.prev != null) {
                            data.prev.thread.raise(((RaiseException) IRException.BREAK_LocalJumpError.getException(context.runtime)).getException());
                        }
                    } catch (IRReturnJump rj) {
                        // This is one of the rare cases where IR flow-control jumps
                        // leaks into the runtime impl.
                        if (data.prev != null) {
                            data.prev.thread.raise(((RaiseException) IRException.RETURN_LocalJumpError.getException(context.runtime)).getException());
                        }
                    } catch (RaiseException re) {
                        if (data.prev != null) {
                            data.prev.data.queue.push(ctxt, new FiberRequest(re.getException().toThrowable(), RequestType.RAISE));
                        }
                    } catch (Throwable t) {
                        if (data.prev != null) {
                            data.prev.thread.raise(JavaUtil.convertJavaToUsableRubyObject(context.runtime, t));
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

    @JRubyMethod(name = "blocking?")
    public IRubyObject blocking_p(ThreadContext context) {
        return asBoolean(context, isBlocking());
    }

    @JRubyMethod(name = "blocking?", meta = true)
    public static IRubyObject blocking_p_s(ThreadContext context, IRubyObject self) {
        boolean blocking = context.getFiber().isBlocking();
        if (!blocking) return context.fals;

        return RubyFixnum.one(context.runtime);
    }

    @JRubyMethod(name = "blocking", meta = true)
    public static IRubyObject blocking(ThreadContext context, IRubyObject self, Block block) {
        ThreadFiber currentFiber = context.getFiber();
        boolean blocking = currentFiber.isBlocking();

        // If we are already blocking, this is essentially a no-op:
        if (currentFiber.isBlocking()) {
            return block.yieldSpecific(context, currentFiber);
        }

        try {
            assert !currentFiber.isBlocking() : "fiber was blocking when it should not have been";

            currentFiber.data.blocking = true;

            // Once the fiber is blocking, and current, we increment the thread blocking state:
            context.getFiberCurrentThread().incrementBlocking();

            return block.yieldSpecific(context, currentFiber);
        } finally {
            // We are no longer blocking:
            currentFiber.data.blocking = false;
            context.getFiberCurrentThread().decrementBlocking();
        }
    }

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context) {
        return backtrace(context, null, null);
    }

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context, IRubyObject level) {
        return backtrace(context, level, null);
    }

    @JRubyMethod(name = "backtrace")
    public IRubyObject backtrace(ThreadContext context, IRubyObject level, IRubyObject length) {
        ThreadFiber threadFiber = data.fiber.get();

        if (threadFiber == null) return context.nil;

        return threadFiber.thread.backtrace(context, level, length);
    }

    @JRubyMethod(name = "backtrace_locations")
    public IRubyObject backtrace_locations(ThreadContext context) {
        return backtrace_locations(context, null, null);
    }

    @JRubyMethod(name = "backtrace_locations")
    public IRubyObject backtrace_locations(ThreadContext context, IRubyObject level) {
        return backtrace_locations(context, level, null);
    }

    @JRubyMethod(name = "backtrace_locations")
    public IRubyObject backtrace_locations(ThreadContext context, IRubyObject level, IRubyObject length) {
        ThreadFiber threadFiber = data.fiber.get();

        if (threadFiber == null) return context.nil;

        return threadFiber.thread.backtrace_locations(context, level, length);
    }

    @JRubyMethod(name = "[]", meta = true)
    public static IRubyObject op_aref(ThreadContext context, IRubyObject recv, IRubyObject key) {
        key = RubySymbol.toSymbol(context, key);

        RubyHash storage = context.getFiber().storage;

        if (storage == null) return context.nil;

        IRubyObject value = storage.op_aref(context, key);

        if (value == null) return context.nil;

        return value;
    }

    @JRubyMethod(name = "[]=", meta = true)
    public static IRubyObject op_aset(ThreadContext context, IRubyObject recv, IRubyObject key, IRubyObject value) {
        key = RubySymbol.toSymbol(context, key);

        ThreadFiber current = context.getFiber();
        boolean nil = value.isNil();
        RubyHash storage = current.storage;

        if (storage == null) {
            if (nil) return context.nil;

            current.storage = storage = newHash(context);
        }

        if (nil) {
            storage.delete(context, key);
        } else {
            storage.op_aset(context, key, value);
        }

        return value;
    }

    @JRubyMethod
    public IRubyObject storage(ThreadContext context) {
        checkSameFiber(context);

        RubyHash storage = this.storage;

        if (storage == null) {
            return context.nil;
        }

        return storage.dup(context);
    }

    private void checkSameFiber(ThreadContext context) {
        if (context.getFiber() != this) {
            throw argumentError(context, "Fiber storage can only be accessed from the Fiber it belongs to");
        }
    }

    @JRubyMethod(name = "storage=")
    public IRubyObject storage_set(ThreadContext context, IRubyObject hash) {
        warnExperimental(context, "Fiber#storage= is experimental and may be removed in the future!");
        checkSameFiber(context);

        setStorage(context, hash);

        return hash;
    }

    private void setStorage(ThreadContext context, IRubyObject hash) {
        validateStorage(context, hash);
        this.storage = hash.isNil() ? null : (RubyHash) hash.dup(context);
    }

    private static void validateStorage(ThreadContext context, IRubyObject hashArg) {
        // nil is an allowed value and will be lazily initialized.
        if (hashArg == context.nil) return;

        var hash = castAsHash(context, hashArg, "storage must be a Hash");
        if (hash.isFrozen()) throw frozenError(context, hash, "storage must not be frozen");

        hash.visitAll(context, (ctx, self, key, value, index) -> {
            if (!(key instanceof RubySymbol)) throw typeError(context, key, "Symbol");
        });
    }

    @JRubyMethod
    public IRubyObject kill(ThreadContext context) {
        if (root) return this;

        if (context.getFiber() == this) {
            throw new FiberKill();
        }

        data.queue.push(context, new FiberRequest(new FiberKill(), RequestType.RAISE));

        return context.nil;
    }

    public static class FiberSchedulerSupport {
        // MRI: rb_fiber_s_schedule_kw and rb_fiber_s_schedule, kw passes on context
        @JRubyMethod(name = "schedule", meta = true, rest = true, keywords = true)
        public static IRubyObject schedule(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            IRubyObject scheduler = context.getThread().getScheduler();
            if (scheduler.isNil()) throw runtimeError(context, "No scheduler is available!");
            return scheduler.callMethod(context, "fiber", args, block);
        }

        // MRI: rb_fiber_s_scheduler
        @JRubyMethod(name = "scheduler", meta = true)
        public static IRubyObject get_scheduler(ThreadContext context, IRubyObject self) {
            return context.getFiberCurrentThread().getScheduler();
        }

        // MRI: rb_fiber_current_scheduler
        @JRubyMethod(name = "current_scheduler", meta = true)
        public static IRubyObject current_scheduler(ThreadContext context, IRubyObject self) {
            return context.getFiberCurrentThread().getSchedulerCurrent();
        }

        // MRI: rb_fiber_set_scheduler
        @JRubyMethod(name = "set_scheduler", meta = true)
        public static IRubyObject set_scheduler(ThreadContext context, IRubyObject self, IRubyObject scheduler) {
            return context.getFiberCurrentThread().setFiberScheduler(context, scheduler);
        }
    }

    public FiberData getData() {
        return data;
    }

    public RubyThread getThread() {
        return thread;
    }
    
    public static class FiberData {
        FiberData(FiberQueue queue, RubyThread parent, ThreadFiber fiber, boolean blocking) {
            this.queue = queue;
            this.parent = parent;
            this.fiber = new WeakReference<ThreadFiber>(fiber);
            this.blocking = blocking;
        }

        public ThreadFiber getPrev() {
            return prev;
        }
        
        final FiberQueue queue;
        volatile ThreadFiber prev;
        final RubyThread parent;
        final WeakReference<ThreadFiber> fiber;
        volatile boolean transferred;
        volatile boolean blocking;
    }
    
    volatile FiberData data;
    volatile RubyThread thread;
    RubyHash storage;
    final boolean root;
}
