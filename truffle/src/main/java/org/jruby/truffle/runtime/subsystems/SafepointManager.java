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

import com.oracle.truffle.api.nodes.Node;
import org.jruby.RubyThread.Status;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private volatile SafepointAction action;

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

    public void poll(Node currentNode) {
        poll(currentNode, true);
    }

    private void poll(Node currentNode, boolean holdsGlobalLock) {
        try {
            assumption.check();
        } catch (InvalidAssumptionException e) {
            assumptionInvalidated(currentNode, holdsGlobalLock, false);
        }
    }

    private void assumptionInvalidated(Node currentNode, boolean holdsGlobalLock, boolean isDrivingThread) {
        RubyThread thread = null;

        if (holdsGlobalLock) {
            thread = context.getThreadManager().leaveGlobalLock();
        }

        // TODO CS 27-Feb-15 how do we get thread if it wasn't holding the global lock?

        try {
            step(currentNode, thread, isDrivingThread);
        } finally {
            if (holdsGlobalLock) {
                context.getThreadManager().enterGlobalLock(thread);
            }
        }

        // We're now running again normally, with the global lock, and can run deferred actions

        if (thread != null) {
            final List<Runnable> deferredActions = new ArrayList<>(thread.getDeferredSafepointActions());
            thread.getDeferredSafepointActions().clear();

            for (Runnable action : deferredActions) {
                action.run();
            }
        }
    }

    private void step(Node currentNode, RubyThread thread, boolean isDrivingThread) {
        // wait other threads to reach their safepoint
        phaser.arriveAndAwaitAdvance();

        if (isDrivingThread) {
            assumption = Truffle.getRuntime().createAssumption();
        }

        // wait the assumption to be renewed
        phaser.arriveAndAwaitAdvance();

        try {
            if (thread != null && thread.getStatus() != Status.ABORTING) {
                action.run(thread, currentNode);
            }
        } finally {
            // wait other threads to finish their action
            phaser.arriveAndAwaitAdvance();
        }
    }

    public void pauseAllThreadsAndExecute(Node currentNode, SafepointAction action) {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Re-entered SafepointManager");
        }

        // Need to lock interruptibly since we are in the registered threads.
        while (true) {
            try {
                lock.lockInterruptibly();
                break;
            } catch (InterruptedException e) {
                poll(currentNode);
            }
        }

        try {
            pauseAllThreadsAndExecute(currentNode, true, action);
        } finally {
            lock.unlock();
        }
    }

    public void pauseAllThreadsAndExecuteFromNonRubyThread(Node currentNode, SafepointAction action) {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Re-entered SafepointManager");
        }

        assert !runningThreads.contains(Thread.currentThread());
        // Just wait to grab the lock, since we are not in the registered threads.
        lock.lock();
        try {
            enterThread();
            try {
                pauseAllThreadsAndExecute(currentNode, false, action);
            } finally {
                leaveThread();
            }
        } finally {
            lock.unlock();
        }
    }

    private void pauseAllThreadsAndExecute(Node currentNode, boolean holdsGlobalLock, SafepointAction action) {
        this.action = action;

        /* this is a potential cause for race conditions,
         * but we need to invalidate first so the interrupted threads
         * see the invalidation in poll() in their catch(InterruptedException) clause
         * and wait on the barrier instead of retrying their blocking action. */
        assumption.invalidate();
        interruptOtherThreads();

        assumptionInvalidated(currentNode, holdsGlobalLock, true);
    }

    private void interruptOtherThreads() {
        Thread current = Thread.currentThread();
        for (Thread thread : runningThreads) {
            if (thread != current) {
                thread.interrupt();
            }
        }
    }

}
