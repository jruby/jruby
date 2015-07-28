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
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ExceptionNodes;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ruby {@code Thread} objects.
 */
public class ThreadManager {

    private final RubyContext context;

    private final RubyBasicObject rootThread;
    private final ThreadLocal<RubyBasicObject> currentThread = new ThreadLocal<RubyBasicObject>();

    private final Set<RubyBasicObject> runningRubyThreads = Collections.newSetFromMap(new ConcurrentHashMap<RubyBasicObject, Boolean>());

    public ThreadManager(RubyContext context) {
        this.context = context;
        this.rootThread = ThreadNodes.createRubyThread(context.getCoreLibrary().getThreadClass(), this);
        ThreadNodes.setName(rootThread, "main");
    }

    public void initialize() {
        registerThread(rootThread);
        ThreadNodes.start(rootThread);
        FiberNodes.start(ThreadNodes.getRootFiber(rootThread));
    }

    public RubyBasicObject getRootThread() {
        return rootThread;
    }

    /**
     * Enters the global lock. Reentrant, but be aware that Ruby threads are not one-to-one with
     * Java threads. Needs to be told which Ruby thread is becoming active as it can't work this out
     * from the current Java thread. Remember to call {@link #leaveGlobalLock} again before
     * blocking.
     */
    @TruffleBoundary
    public void enterGlobalLock(RubyBasicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        currentThread.set(thread);
    }

    /**
     * Leaves the global lock, returning the Ruby thread which has just stopped being the current
     * thread. Remember to call {@link #enterGlobalLock} again with that returned thread before
     * executing any Ruby code. You probably want to use this with a {@code finally} statement to
     * make sure that happens
     */
    @TruffleBoundary
    public RubyBasicObject leaveGlobalLock() {
        return currentThread.get();
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
            final RubyBasicObject runningThread = leaveGlobalLock();
            ThreadNodes.setStatus(runningThread, Status.SLEEP);

            try {
                try {
                    result = action.block();
                } finally {
                    ThreadNodes.setStatus(runningThread, Status.RUN);
                    // We need to enter the global lock before anything else!
                    enterGlobalLock(runningThread);
                }
            } catch (InterruptedException e) {
                // We were interrupted, possibly by the SafepointManager.
                context.getSafepointManager().pollFromBlockingCall(null);
            }
        } while (result == null);

        return result;
    }

    public RubyBasicObject getCurrentThread() {
        return currentThread.get();
    }

    public synchronized void registerThread(RubyBasicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        runningRubyThreads.add(thread);
    }

    public synchronized void unregisterThread(RubyBasicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        runningRubyThreads.remove(thread);
    }

    public void shutdown() {
        try {
            killOtherThreads();
        } finally {
            ThreadNodes.getFiberManager(rootThread).shutdown();
            FiberNodes.cleanup(ThreadNodes.getRootFiber(rootThread));
            ThreadNodes.cleanup(rootThread);
        }
    }

    private void killOtherThreads() {
        while (true) {
            try {
                context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {
                    @Override
                    public synchronized void run(RubyBasicObject thread, Node currentNode) {
                        if (thread != rootThread && Thread.currentThread() == ThreadNodes.getRootFiberJavaThread(thread)) {
                            ThreadNodes.shutdown(thread);
                        }
                    }
                });
                break; // Successfully executed the safepoint and sent the exceptions.
            } catch (RaiseException e) {
                final Object rubyException = e.getRubyException();

                for (String line : Backtrace.DISPLAY_FORMATTER.format(BasicObjectNodes.getContext(((RubyBasicObject) e.getRubyException())), (RubyBasicObject) rubyException, ExceptionNodes.getBacktrace((RubyBasicObject) rubyException))) {
                    System.err.println(line);
                }
            }
        }
    }

}
