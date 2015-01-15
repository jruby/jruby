/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.util.Consumer;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SafepointManager {

    private final RubyContext context;

    private final Lock lock = new ReentrantLock();
    @CompilerDirectives.CompilationFinal private Assumption assumption = Truffle.getRuntime().createAssumption();
    private CyclicBarrier barrier;
    private int liveThreads = 1;
    private Consumer<RubyThread> action;

    public SafepointManager(RubyContext context) {
        this.context = context;
    }

    public void enterThread() {
        CompilerAsserts.neverPartOfCompilation();

        try {
            lock.lock();
            liveThreads++;
        } finally {
            lock.unlock();
        }
    }

    public void leaveThread() {
        CompilerAsserts.neverPartOfCompilation();

        try {
            lock.lock();

            poll();

            liveThreads--;
        } finally {
            lock.unlock();
        }
    }

    public void poll() {
        try {
            assumption.check();
        } catch (InvalidAssumptionException e) {
            assumptionInvalidated();
        }
    }

    private void assumptionInvalidated() {
        waitOnBarrier();

        try {
            action.accept(context.getThreadManager().getCurrentThread());
        } finally {
            waitOnBarrier();
        }
    }

    public void pauseAllThreadsAndExecute(final Consumer<RubyThread> action) {
        CompilerDirectives.transferToInterpreter();

        try {
            lock.lock();

            SafepointManager.this.action = action;

            barrier = new CyclicBarrier(liveThreads);

            assumption.invalidate();

            // wait for all threads to reach their safepoint
            waitOnBarrier();

            assumption = Truffle.getRuntime().createAssumption();

            try {
                action.accept(context.getThreadManager().getCurrentThread());
            } finally {
                // wait for all threads to execute the action
                waitOnBarrier();
            }
        } finally {
            lock.unlock();
        }
    }

    public void pauseAllThreadsAndExecuteSignalHandler(final Consumer<RubyThread> action) {
        CompilerDirectives.transferToInterpreter();

        // The current (Java) thread is not a Ruby thread, so we do not touch the global lock.

        try {
            lock.lock();

            SafepointManager.this.action = action;

            barrier = new CyclicBarrier(liveThreads + 1);

            assumption.invalidate();

            // wait for all threads to reach their safepoint
            waitOnBarrierNoGlobalLock();

            assumption = Truffle.getRuntime().createAssumption();

            // wait for all Ruby threads to execute the action
            waitOnBarrierNoGlobalLock();
        } finally {
            lock.unlock();
        }
    }

    private void waitOnBarrier() {
        final RubyThread runningThread = context.getThreadManager().leaveGlobalLock();

        try {
            waitOnBarrierNoGlobalLock();
        } finally {
            context.getThreadManager().enterGlobalLock(runningThread);
        }
    }

    private void waitOnBarrierNoGlobalLock() {
        while (true) {
            try {
                barrier.await();
                break;
            } catch (BrokenBarrierException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
            }
        }
    }

}
