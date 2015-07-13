/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.RubyThread.Status;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyThread;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manages Ruby {@code Thread} objects.
 */
public class ThreadManager {

    private final RubyContext context;

    private final ReentrantLock globalLock = new ReentrantLock();

    private final RubyThread rootThread;
    private RubyThread currentThread;

    private final Set<RubyThread> runningRubyThreads = Collections.newSetFromMap(new ConcurrentHashMap<RubyThread, Boolean>());

    public ThreadManager(RubyContext context) {
        this.context = context;
        this.rootThread = new RubyThread(context.getCoreLibrary().getThreadClass(), this);
        rootThread.setName("main");
    }

    public void initialize() {
        registerThread(rootThread);
        rootThread.start();
        rootThread.getRootFiber().start();
    }

    public RubyThread getRootThread() {
        return rootThread;
    }

    /**
     * Enters the global lock. Reentrant, but be aware that Ruby threads are not one-to-one with
     * Java threads. Needs to be told which Ruby thread is becoming active as it can't work this out
     * from the current Java thread. Remember to call {@link #leaveGlobalLock} again before
     * blocking.
     */
    @TruffleBoundary
    public void enterGlobalLock(RubyThread thread) {
        globalLock.lock();
        currentThread = thread;
    }

    /**
     * Leaves the global lock, returning the Ruby thread which has just stopped being the current
     * thread. Remember to call {@link #enterGlobalLock} again with that returned thread before
     * executing any Ruby code. You probably want to use this with a {@code finally} statement to
     * make sure that happens
     */
    @TruffleBoundary
    public RubyThread leaveGlobalLock() {
        if (!globalLock.isHeldByCurrentThread()) {
            throw new RuntimeException("You don't own this lock!");
        }

        final RubyThread result = currentThread;
        globalLock.unlock();
        return result;
    }

    public static interface BlockingActionWithoutGlobalLock<T> {
        public static boolean SUCCESS = true;

        T block() throws InterruptedException;
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
    public <T> T runUntilResult(BlockingActionWithoutGlobalLock<T> action) {
        T result = null;

        do {
            result = runOnce(action);
        } while (result == null);

        return result;
    }

    /**
     * Runs {@code action} once.
     * The action might be {@link Thread#interrupted()}, for instance by
     * the {@link SafepointManager}, in which case null will be returned.
     *
     * @param action must not touch any Ruby state
     * @return the return value from {@code action} or null if interrupted
     */
    @TruffleBoundary
    public <T> T runOnce(BlockingActionWithoutGlobalLock<T> action) {
        T result = null;
        final RubyThread runningThread = leaveGlobalLock();
        runningThread.setStatus(Status.SLEEP);

        try {
            try {
                result = action.block();
            } finally {
                runningThread.setStatus(Status.RUN);
                // We need to enter the global lock before anything else!
                enterGlobalLock(runningThread);
            }
        } catch (InterruptedException e) {
            // We were interrupted, possibly by the SafepointManager.
            context.getSafepointManager().pollFromBlockingCall(null);
        }
        return result;
    }

    public RubyThread getCurrentThread() {
        assert globalLock.isHeldByCurrentThread() : "getCurrentThread() is only correct if holding the global lock";
        return currentThread;
    }

    public synchronized void registerThread(RubyThread thread) {
        runningRubyThreads.add(thread);
    }

    public synchronized void unregisterThread(RubyThread thread) {
        runningRubyThreads.remove(thread);
    }

    public void shutdown() {
        try {
            killOtherThreads();
        } finally {
            rootThread.getFiberManager().shutdown();
            rootThread.getRootFiber().cleanup();
            rootThread.cleanup();
        }
    }

    private void killOtherThreads() {
        while (true) {
            try {
                context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {
                    @Override
                    public synchronized void run(RubyThread thread, Node currentNode) {
                        if (thread != rootThread && Thread.currentThread() == thread.getRootFiberJavaThread()) {
                            thread.shutdown();
                        }
                    }
                });
                break; // Successfully executed the safepoint and sent the exceptions.
            } catch (RaiseException e) {
                final RubyException rubyException = e.getRubyException();

                for (String line : Backtrace.DISPLAY_FORMATTER.format(e.getRubyException().getContext(), rubyException, rubyException.getBacktrace())) {
                    System.err.println(line);
                }
            }
        }
    }

}
