/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.InterruptMode;
import org.jruby.truffle.core.thread.ThreadStatus;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.ReentrantLock;

public class SafepointManager {

    private final RubyContext context;

    private final Set<Thread> runningThreads = Collections.newSetFromMap(new ConcurrentHashMap<Thread, Boolean>());

    private final ReentrantLock lock = new ReentrantLock();
    private final Phaser phaser = new Phaser();

    @CompilationFinal private Assumption assumption = Truffle.getRuntime().createAssumption("SafepointManager");

    private volatile SafepointAction action;
    private volatile boolean deferred;

    public SafepointManager(RubyContext context) {
        this.context = context;
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
        poll(currentNode, false);
    }

    public void pollFromBlockingCall(Node currentNode) {
        poll(currentNode, true);
    }

    private void poll(Node currentNode, boolean fromBlockingCall) {
        try {
            assumption.check();
        } catch (InvalidAssumptionException e) {
            assumptionInvalidated(currentNode, fromBlockingCall);
        }
    }

    @TruffleBoundary
    private void assumptionInvalidated(Node currentNode, boolean fromBlockingCall) {
        final DynamicObject thread = context.getThreadManager().getCurrentThread();
        final InterruptMode interruptMode = Layouts.THREAD.getInterruptMode(thread);

        final boolean interruptible = (interruptMode == InterruptMode.IMMEDIATE) ||
                (fromBlockingCall && interruptMode == InterruptMode.ON_BLOCKING);

        if (!interruptible) {
            Thread.currentThread().interrupt(); // keep the interrupt flag
            return; // interrupt me later
        }

        final SafepointAction deferredAction = step(currentNode, false);

        // We're now running again normally and can run deferred actions
        if (deferredAction != null) {
            deferredAction.accept(thread, currentNode);
        }
    }

    @TruffleBoundary
    private SafepointAction step(Node currentNode, boolean isDrivingThread) {
        final DynamicObject thread = context.getThreadManager().getCurrentThread();

        // Wait for other threads to reach their safepoint
        phaser.arriveAndAwaitAdvance();

        if (isDrivingThread) {
            assumption = Truffle.getRuntime().createAssumption(getClass().getCanonicalName());
        }

        // Wait for the assumption to be renewed
        phaser.arriveAndAwaitAdvance();

        // Read these while in the safepoint
        final SafepointAction deferredAction = deferred ? action : null;

        try {
            if (!deferred && thread != null && Layouts.THREAD.getStatus(thread) != ThreadStatus.ABORTING) {
                action.accept(thread, currentNode);
            }
        } finally {
            // Wait for other threads to finish their action
            phaser.arriveAndAwaitAdvance();
        }

        return deferredAction;
    }

    @TruffleBoundary
    public void pauseAllThreadsAndExecute(Node currentNode, boolean deferred, SafepointAction action) {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Re-entered SafepointManager");
        }

        // Need to lock interruptibly since we are in the registered threads.
        while (!lock.tryLock()) {
            poll(currentNode);
        }

        try {
            pauseAllThreadsAndExecute(currentNode, action, deferred);
        } finally {
            lock.unlock();
        }

        // Run deferred actions after leaving the SafepointManager lock.
        if (deferred) {
            action.accept(context.getThreadManager().getCurrentThread(), currentNode);
        }
    }

    @TruffleBoundary
    public void pauseAllThreadsAndExecuteFromNonRubyThread(boolean deferred, SafepointAction action) {
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException("Re-entered SafepointManager");
        }

        assert !runningThreads.contains(Thread.currentThread());

        // Just wait to grab the lock, since we are not in the registered threads.

        lock.lock();

        try {
            enterThread();
            try {
                pauseAllThreadsAndExecute(null, action, deferred);
            } finally {
                leaveThread();
            }
        } finally {
            lock.unlock();
        }
    }

    // Variants for a single thread

    @TruffleBoundary
    public void pauseThreadAndExecute(final Thread thread, Node currentNode, final SafepointAction action) {
        if (Thread.currentThread() == thread) {
            // fast path if we are already the right thread
            final DynamicObject rubyThread = context.getThreadManager().getCurrentThread();
            action.accept(rubyThread, currentNode);
        } else {
            pauseAllThreadsAndExecute(currentNode, false, (rubyThread, currentNode1) -> {
                if (Thread.currentThread() == thread) {
                    action.accept(rubyThread, currentNode1);
                }
            });
        }
    }

    @TruffleBoundary
    public void pauseThreadAndExecuteLater(final Thread thread, Node currentNode, final SafepointAction action) {
        if (Thread.currentThread() == thread) {
            // fast path if we are already the right thread
            final DynamicObject rubyThread = context.getThreadManager().getCurrentThread();
            action.accept(rubyThread, currentNode);
        } else {
            pauseAllThreadsAndExecute(currentNode, true, (rubyThread, currentNode1) -> {
                if (Thread.currentThread() == thread) {
                    action.accept(rubyThread, currentNode1);
                }
            });
        }
    }

    @TruffleBoundary
    public void pauseThreadAndExecuteLaterFromNonRubyThread(final Thread thread, final SafepointAction action) {
        pauseAllThreadsAndExecuteFromNonRubyThread(true, (rubyThread, currentNode) -> {
            if (Thread.currentThread() == thread) {
                action.accept(rubyThread, currentNode);
            }
        });
    }

    private void pauseAllThreadsAndExecute(Node currentNode, SafepointAction action, boolean deferred) {
        this.action = action;
        this.deferred = deferred;

        /* this is a potential cause for race conditions,
         * but we need to invalidate first so the interrupted threads
         * see the invalidation in poll() in their catch(InterruptedException) clause
         * and wait on the barrier instead of retrying their blocking action. */
        assumption.invalidate();
        interruptOtherThreads();

        step(currentNode, true);
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
