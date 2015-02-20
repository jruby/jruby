/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import org.jruby.RubyThread.Status;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.util.Consumer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

public class SafepointManager {

    private final RubyContext context;

    private final Set<Thread> runningThreads = Collections.newSetFromMap(new ConcurrentHashMap<Thread, Boolean>());

    @CompilerDirectives.CompilationFinal private Assumption assumption = Truffle.getRuntime().createAssumption();
    private final ReentrantLock lock = new ReentrantLock();
    private final Phaser phaser = new Phaser();
    private volatile Consumer<RubyThread> action;

    public SafepointManager(RubyContext context) {
        this.context = context;

        enterThread();
    }

    public void enterThread() {
        CompilerAsserts.neverPartOfCompilation();

        lock.lock();
        try {
            phaser.register();
            runningThreads.add(Thread.currentThread());
        } finally {
            lock.unlock();
        }
    }

    public void leaveThread() {
        CompilerAsserts.neverPartOfCompilation();

        phaser.arriveAndDeregister();
        runningThreads.remove(Thread.currentThread());
    }

    public void poll() {
        poll(true);
    }

    private void poll(boolean holdsGlobalLock) {
        try {
            assumption.check();
        } catch (InvalidAssumptionException e) {
            assumptionInvalidated(holdsGlobalLock, false);
        }
    }

    private void assumptionInvalidated(boolean holdsGlobalLock, boolean isDrivingThread) {
        RubyThread thread = null;

        if (holdsGlobalLock) {
            thread = context.getThreadManager().leaveGlobalLock();
        }

        try {
            step(thread, isDrivingThread);
        } finally {
            if (holdsGlobalLock) {
                context.getThreadManager().enterGlobalLock(thread);
            }
        }
    }

    private void step(RubyThread thread, boolean isDrivingThread) {
        // wait other threads to reach their safepoint
        phaser.arriveAndAwaitAdvance();

        if (isDrivingThread) {
            assumption = Truffle.getRuntime().createAssumption();
        }

        // wait the assumption to be renewed
        phaser.arriveAndAwaitAdvance();

        try {
            if (thread != null && thread.getStatus() != Status.ABORTING) {
                action.accept(thread);
            }
        } finally {
            // wait other threads to finish their action
            phaser.arriveAndAwaitAdvance();
        }
    }

    public void pauseAllThreadsAndExecute(Consumer<RubyThread> action) {
        pauseAllThreadsAndExecute(true, action);
    }

    public void pauseAllThreadsAndExecuteFromNonRubyThread(Consumer<RubyThread> action) {
        enterThread();
        try {
            pauseAllThreadsAndExecute(false, action);
        } finally {
            leaveThread();
        }
    }

    private void pauseAllThreadsAndExecute(boolean holdsGlobalLock, Consumer<RubyThread> action) {
        CompilerDirectives.transferToInterpreter();

        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Re-entered SafepointManager");
        }

        while (true) {
            try {
                lock.lockInterruptibly();
                break;
            } catch (InterruptedException e) {
                poll(holdsGlobalLock);
            }
        }

        try {
            this.action = action;

            /* this is a potential cause for race conditions,
             * but we need to invalidate first so the interrupted threads
             * see the invalidation in poll() in their catch(InterruptedException) clause
             * and wait on the barrier instead of retrying their blocking action. */
            assumption.invalidate();
            interruptAllThreads();

            assumptionInvalidated(holdsGlobalLock, true);
        } finally {
            lock.unlock();
        }
    }

    private void interruptAllThreads() {
        for (Thread thread : runningThreads) {
            thread.interrupt();
        }
    }

}
