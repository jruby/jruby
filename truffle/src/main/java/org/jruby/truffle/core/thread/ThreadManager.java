/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.DefaultNativeTimeval;
import jnr.posix.Timeval;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.core.InterruptMode;
import org.jruby.truffle.core.fiber.FiberManager;
import org.jruby.truffle.core.fiber.FiberNodes;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.SafepointAction;
import org.jruby.truffle.language.SafepointManager;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.ReturnException;
import org.jruby.truffle.language.control.ThreadExitException;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.shared.SharedObjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

public class ThreadManager {

    private final RubyContext context;

    private final DynamicObject rootThread;
    private final ThreadLocal<DynamicObject> currentThread = new ThreadLocal<>();

    private final Set<DynamicObject> runningRubyThreads
            = Collections.newSetFromMap(new ConcurrentHashMap<DynamicObject, Boolean>());

    public ThreadManager(RubyContext context) {
        this.context = context;
        this.rootThread = createRubyThread(context);
    }

    public static final InterruptMode DEFAULT_INTERRUPT_MODE = InterruptMode.IMMEDIATE;
    public static final ThreadStatus DEFAULT_STATUS = ThreadStatus.RUN;

    public static DynamicObject createRubyThread(RubyContext context) {
        final DynamicObject object = Layouts.THREAD.createThread(
                context.getCoreLibrary().getThreadFactory(),
                createThreadLocals(context),
                DEFAULT_INTERRUPT_MODE,
                DEFAULT_STATUS,
                new ArrayList<>(),
                null,
                new CountDownLatch(1),
                getGlobalAbortOnException(context),
                null,
                null,
                null,
                new AtomicBoolean(false),
                0,
                context.getCoreLibrary().getNilObject(),
                context.getCoreLibrary().getNilObject());

        Layouts.THREAD.setFiberManagerUnsafe(object, new FiberManager(context, object)); // Because it is cyclic

        return object;
    }

    public static boolean getGlobalAbortOnException(RubyContext context) {
        final DynamicObject threadClass = context.getCoreLibrary().getThreadClass();
        return (boolean) ReadObjectFieldNode.read(threadClass, "@abort_on_exception", null);
    }

    public static DynamicObject createThreadLocals(RubyContext context) {
        final DynamicObject threadLocals = Layouts.BASIC_OBJECT.createBasicObject(context.getCoreLibrary().getObjectFactory());
        threadLocals.define("$!", context.getCoreLibrary().getNilObject());
        threadLocals.define("$?", context.getCoreLibrary().getNilObject());
        return threadLocals;
    }

    public static void initialize(final DynamicObject thread, RubyContext context, Node currentNode, final Object[] arguments, final DynamicObject block) {
        if (SharedObjects.ENABLED) {
            SharedObjects.shareDeclarationFrame(block);
        }

        final SourceSection sourceSection = Layouts.PROC.getSharedMethodInfo(block).getSourceSection();
        final String info = RubyLanguage.fileLine(sourceSection);
        initialize(thread, context, currentNode, info, () -> {
            final Object value = ProcOperations.rootCall(block, arguments);
            Layouts.THREAD.setValue(thread, value);
        });
    }

    public static void initialize(final DynamicObject thread, final RubyContext context, final Node currentNode, final String info, final Runnable task) {
        assert RubyGuards.isRubyThread(thread);
        new Thread(() -> run(thread, context, currentNode, info, task)).start();

        FiberNodes.waitForInitialization(context, Layouts.THREAD.getFiberManager(thread).getRootFiber(), currentNode);
    }

    public static void run(DynamicObject thread, final RubyContext context, Node currentNode, String info, Runnable task) {
        assert RubyGuards.isRubyThread(thread);

        final String name = "Ruby Thread@" + info;
        Thread.currentThread().setName(name);
        DynamicObject fiber = Layouts.THREAD.getFiberManager(thread).getRootFiber();

        start(context, thread);
        FiberNodes.start(context, fiber);
        try {
            task.run();
        } catch (ThreadExitException e) {
            setThreadValue(thread, context.getCoreLibrary().getNilObject());
            return;
        } catch (RaiseException e) {
            setException(context, thread, e.getException(), currentNode);
        } catch (ReturnException e) {
            setException(context, thread, context.getCoreExceptions().unexpectedReturn(currentNode), currentNode);
        } finally {
            FiberNodes.cleanup(context, fiber);
            cleanup(context, thread);
        }
    }

    private static void setThreadValue(final DynamicObject thread, final Object value) {
        // A Thread is always shared (Thread.list)
        SharedObjects.propagate(thread, value);
        Layouts.THREAD.setValue(thread, value);
    }

    private static void setException(RubyContext context, DynamicObject thread, DynamicObject exception, Node currentNode) {
        final DynamicObject mainThread = context.getThreadManager().getRootThread();
        final boolean isSystemExit = Layouts.BASIC_OBJECT.getLogicalClass(exception) == context.getCoreLibrary().getSystemExitClass();
        if (thread != mainThread && (isSystemExit || Layouts.THREAD.getAbortOnException(thread))) {
            ThreadNodes.ThreadRaisePrimitiveNode.raiseInThread(context, mainThread, exception, currentNode);
        }
        SharedObjects.propagate(thread, exception);
        Layouts.THREAD.setException(thread, exception);
    }

    public static void start(RubyContext context, DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.setThread(thread, Thread.currentThread());
        context.getThreadManager().registerThread(thread);
    }

    public static void cleanup(RubyContext context, DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);

        Layouts.THREAD.setStatus(thread, ThreadStatus.ABORTING);
        context.getThreadManager().unregisterThread(thread);

        Layouts.THREAD.setStatus(thread, ThreadStatus.DEAD);
        Layouts.THREAD.setThread(thread, null);
        assert RubyGuards.isRubyThread(thread);
        for (Lock lock : Layouts.THREAD.getOwnedLocks(thread)) {
            lock.unlock();
        }
        Layouts.THREAD.getFinishedLatch(thread).countDown();
    }

    public static void shutdown(RubyContext context, DynamicObject thread, Node currentNode) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.getFiberManager(thread).shutdown();

        if (thread == context.getThreadManager().getRootThread()) {
            throw new RaiseException(context.getCoreExceptions().systemExit(0, currentNode));
        } else {
            throw new ThreadExitException();
        }
    }

    public void initialize() {
        start(context, rootThread);
        FiberNodes.start(context, Layouts.THREAD.getFiberManager(rootThread).getRootFiber());
    }

    public DynamicObject getRootThread() {
        return rootThread;
    }

    public interface BlockingAction<T> {
        boolean SUCCESS = true;

        T block() throws InterruptedException;
    }

    public interface BlockingTimeoutAction<T> {
        T block(Timeval timeoutToUse) throws InterruptedException;
    }

    /**
     * Runs {@code action} until it returns a non-null value.
     * The action might be {@link Thread#interrupted()}, for instance by
     * the {@link SafepointManager}, in which case it will be run again.
     *
     * @param action must not touch any Ruby state
     * @return the first non-null return value from {@code action}
     */
    @TruffleBoundary
    public <T> T runUntilResult(Node currentNode, BlockingAction<T> action) {
        final DynamicObject runningThread = getCurrentThread();
        T result = null;

        do {
            Layouts.THREAD.setStatus(runningThread, ThreadStatus.SLEEP);

            try {
                try {
                    result = action.block();
                } finally {
                    Layouts.THREAD.setStatus(runningThread, ThreadStatus.RUN);
                }
            } catch (InterruptedException e) {
                // We were interrupted, possibly by the SafepointManager.
                context.getSafepointManager().pollFromBlockingCall(currentNode);
            }
        } while (result == null);

        return result;
    }

    @TruffleBoundary
    public <T> T runUntilSuccessKeepRunStatus(Node currentNode, BlockingAction<T> action) {
        T result = null;

        do {
            try {
                result = action.block();
            } catch (InterruptedException e) {
                // We were interrupted, possibly by the SafepointManager.
                context.getSafepointManager().poll(currentNode);
            }
        } while (result == null);

        return result;
    }

    public interface ResultOrTimeout<T> {
    }

    public static class ResultWithinTime<T> implements ResultOrTimeout<T> {

        private final T value;

        public ResultWithinTime(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

    }

    public static class TimedOut<T> implements ResultOrTimeout<T> {
    }

    public <T> ResultOrTimeout<T> runUntilTimeout(Node currentNode, int timeoutMicros, final BlockingTimeoutAction<T> action) {
        final Timeval timeoutToUse = new DefaultNativeTimeval(jnr.ffi.Runtime.getSystemRuntime());

        if (timeoutMicros == 0) {
            timeoutToUse.setTime(new long[]{0, 0});

            return new ResultWithinTime<>(runUntilResult(currentNode, () -> action.block(timeoutToUse)));
        } else {
            final int pollTime = 500_000_000;
            final long requestedTimeoutAt = System.nanoTime() + timeoutMicros * 1_000L;

            return runUntilResult(currentNode, new BlockingAction<ResultOrTimeout<T>>() {

                @Override
                public ResultOrTimeout<T> block() throws InterruptedException {
                    final long timeUntilRequestedTimeout = requestedTimeoutAt - System.nanoTime();

                    if (timeUntilRequestedTimeout <= 0) {
                        return new TimedOut<>();
                    }

                    final boolean timeoutForPoll = pollTime <= timeUntilRequestedTimeout;
                    final long effectiveTimeout = Math.min(pollTime, timeUntilRequestedTimeout);
                    final long effectiveTimeoutMicros = effectiveTimeout / 1_000;
                    timeoutToUse.setTime(new long[] {
                            effectiveTimeoutMicros / 1_000_000,
                            effectiveTimeoutMicros % 1_000_000
                    });

                    final T result = action.block(timeoutToUse);

                    if (result == null) {
                        if (timeoutForPoll && (requestedTimeoutAt - System.nanoTime()) > 0) {
                            throw new InterruptedException();
                        } else {
                            return new TimedOut<>();
                        }
                    }

                    return new ResultWithinTime<>(result);
                }

            });
        }
    }

    public void initializeCurrentThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        currentThread.set(thread);
    }

    @TruffleBoundary
    public DynamicObject getCurrentThread() {
        return currentThread.get();
    }

    public synchronized void registerThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        initializeCurrentThread(thread);
        runningRubyThreads.add(thread);

        if (SharedObjects.ENABLED && runningRubyThreads.size() > 1) {
            context.getSharedObjects().startSharing();
            SharedObjects.writeBarrier(thread);
        }
    }

    public synchronized void unregisterThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        runningRubyThreads.remove(thread);
        currentThread.set(null);
    }

    @TruffleBoundary
    public void shutdown() {
        try {
            if (runningRubyThreads.size() > 1) {
                killOtherThreads();
            }
        } finally {
            Layouts.THREAD.getFiberManager(rootThread).shutdown();
            FiberNodes.cleanup(context, Layouts.THREAD.getFiberManager(rootThread).getRootFiber());
            cleanup(context, rootThread);
        }
    }

    @TruffleBoundary
    public Object[] getThreadList() {
        return runningRubyThreads.toArray(new Object[runningRubyThreads.size()]);
    }

    @TruffleBoundary
    public Iterable<DynamicObject> iterateThreads() {
        return runningRubyThreads;
    }

    @TruffleBoundary
    private void killOtherThreads() {
        while (true) {
            try {
                context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {
                    @Override
                    public synchronized void accept(DynamicObject thread, Node currentNode) {
                        if (thread != rootThread && Thread.currentThread() == Layouts.THREAD.getThread(thread)) {
                            shutdown(context, thread, currentNode);
                        }
                    }
                });
                break; // Successfully executed the safepoint and sent the exceptions.
            } catch (RaiseException e) {
                final DynamicObject rubyException = e.getException();
                BacktraceFormatter.createDefaultFormatter(context).printBacktrace(context, rubyException, Layouts.EXCEPTION.getBacktrace(rubyException));
            }
        }
    }

}
