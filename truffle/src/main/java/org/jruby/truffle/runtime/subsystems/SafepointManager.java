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
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReentrantLock;

public class SafepointManager {

    private final RubyContext context;

    private final Set<Thread> runningThreads = Collections.newSetFromMap(new ConcurrentHashMap<Thread, Boolean>());

    @CompilerDirectives.CompilationFinal private Assumption assumption = Truffle.getRuntime().createAssumption();
    private final ReentrantLock lock = new ReentrantLock();
    private CyclicBarrier barrier;
    private Consumer<RubyThread> action;

    public SafepointManager(RubyContext context) {
        this.context = context;
        runningThreads.add(Thread.currentThread());
    }

    public void enterThread() {
        CompilerAsserts.neverPartOfCompilation();

        // Waits for lock to become available
        lock.lock();
        try {
            runningThreads.add(Thread.currentThread());
        } finally {
            lock.unlock();
        }
    }

    public void leaveThread() {
        CompilerAsserts.neverPartOfCompilation();

        // Leave only when there is no more running safepoint action.
        while (!lock.tryLock()) {
            poll(false);
        }
        // SafepointManager lock acquired
        try {
            runningThreads.remove(Thread.currentThread());
        } finally {
            lock.unlock();
        }
    }

    public void poll() {
        poll(true);
    }

    private void poll(boolean holdsGlobalLock) {
        try {
            assumption.check();
        } catch (InvalidAssumptionException e) {
            assumptionInvalidated(holdsGlobalLock);
        }
    }

    private void assumptionInvalidated(boolean holdsGlobalLock) {
        RubyThread thread = null;
        if (holdsGlobalLock) {
            thread = context.getThreadManager().leaveGlobalLock();
        }

        try {
            // clear the interrupted status which may have been set by interruptAllThreads().
            Thread.interrupted();

            // wait other threads to reach their safepoint
            waitOnBarrier();

            if (lock.isHeldByCurrentThread()) {
                assumption = Truffle.getRuntime().createAssumption();
            }

            // wait the assumption to be renewed
            waitOnBarrier();

            try {
                if (holdsGlobalLock && thread.getStatus() != Status.ABORTING) {
                    runAction(thread);
                }
            } finally {
                // wait other threads to finish their action
                waitOnBarrier();
            }
        } finally {
            if (holdsGlobalLock) {
                context.getThreadManager().enterGlobalLock(thread);
            }
        }
    }

    private void runAction(RubyThread thread) {
        context.getThreadManager().enterGlobalLock(thread);
        try {
            action.accept(thread);
        } finally {
            context.getThreadManager().leaveGlobalLock();
        }
    }

    public void pauseAllThreadsAndExecute(Consumer<RubyThread> action) {
        pauseAllThreadsAndExecute(true, true, action);
    }

    public void pauseAllThreadsAndExecuteFromNonRubyThread(Consumer<RubyThread> action) {
        pauseAllThreadsAndExecute(false, false, action);
    }

    private void pauseAllThreadsAndExecute(boolean isRubyThread, boolean holdsGlobalLock, Consumer<RubyThread> action) {
        CompilerDirectives.transferToInterpreter();

        assert !lock.isHeldByCurrentThread() : "reentering pauseAllThreadsAndExecute";
        lock.lock();
        try {
            this.action = action;

            barrier = new CyclicBarrier(isRubyThread ? runningThreads.size() : runningThreads.size() + 1);

            /* this is a potential cause for race conditions,
             * but we need to invalidate first so the interrupted threads
             * see the invalidation in poll() in their catch(InterruptedException) clause
             * and wait on the barrier instead of retrying their blocking action. */
            assumption.invalidate();
            interruptAllThreads();

            assumptionInvalidated(isRubyThread && holdsGlobalLock);
        } finally {
            lock.unlock();
        }
    }

    private void waitOnBarrier() {
        while (true) {
            try {
                barrier.await();
                break;
            } catch (BrokenBarrierException | InterruptedException e) {
                // System.err.println("Safepoint barrier interrupted for thread " + Thread.currentThread());
                if (lock.isHeldByCurrentThread()) {
                    barrier.reset();
                } else {
                    // wait for the lock holder to repair the barrier
                    while (barrier.isBroken()) {
                        Thread.yield();
                    }
                }
            }
        }
    }

    private void interruptAllThreads() {
        for (Thread thread : runningThreads) {
            thread.interrupt();
        }
    }

    public synchronized void registerThread(Thread thread) {
        runningThreads.add(thread);
    }

    public synchronized void unregisterThread(Thread thread) {
        runningThreads.remove(thread);
    }

}
