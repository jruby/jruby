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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.RubyThread.Status;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ThreadNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.BacktraceFormatter;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Ruby {@code Thread} objects.
 */
public class ThreadManager {

    private final RubyContext context;

    private final DynamicObject rootThread;
    private final ThreadLocal<DynamicObject> currentThread = new ThreadLocal<DynamicObject>();

    private final Set<DynamicObject> runningRubyThreads = Collections.newSetFromMap(new ConcurrentHashMap<DynamicObject, Boolean>());

    public ThreadManager(RubyContext context) {
        this.context = context;
        this.rootThread = ThreadNodes.createRubyThread(context.getCoreLibrary().getThreadClass());
        Layouts.THREAD.setName(rootThread, "main");
    }

    public void initialize() {
        ThreadNodes.start(rootThread);
        FiberNodes.start(Layouts.THREAD.getFiberManager(rootThread).getRootFiber());
    }

    public DynamicObject getRootThread() {
        return rootThread;
    }


    public static interface BlockingAction<T> {
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
    public <T> T runUntilResult(BlockingAction<T> action) {
        T result = null;

        do {
            final DynamicObject runningThread = getCurrentThread();
            Layouts.THREAD.setStatus(runningThread, Status.SLEEP);

            try {
                try {
                    result = action.block();
                } finally {
                    Layouts.THREAD.setStatus(runningThread, Status.RUN);
                }
            } catch (InterruptedException e) {
                // We were interrupted, possibly by the SafepointManager.
                context.getSafepointManager().pollFromBlockingCall(null);
            }
        } while (result == null);

        return result;
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
    }

    public synchronized void unregisterThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        runningRubyThreads.remove(thread);
        currentThread.set(null);
    }

    public void shutdown() {
        try {
            if (runningRubyThreads.size() > 1) {
                killOtherThreads();
            }
        } finally {
            Layouts.THREAD.getFiberManager(rootThread).shutdown();
            FiberNodes.cleanup(Layouts.THREAD.getFiberManager(rootThread).getRootFiber());
            ThreadNodes.cleanup(rootThread);
        }
    }

    public DynamicObject[] getThreads() {
        return runningRubyThreads.toArray(new DynamicObject[runningRubyThreads.size()]);
    }

    private void killOtherThreads() {
        while (true) {
            try {
                context.getSafepointManager().pauseAllThreadsAndExecute(null, false, new SafepointAction() {
                    @Override
                    public synchronized void run(DynamicObject thread, Node currentNode) {
                        if (thread != rootThread && Thread.currentThread() == Layouts.THREAD.getThread(thread)) {
                            ThreadNodes.shutdown(thread);
                        }
                    }
                });
                break; // Successfully executed the safepoint and sent the exceptions.
            } catch (RaiseException e) {
                final Object rubyException = e.getRubyException();

                for (String line : new BacktraceFormatter().format(Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(((DynamicObject) e.getRubyException()))).getContext(), (DynamicObject) rubyException, Layouts.EXCEPTION.getBacktrace((DynamicObject) rubyException))) {
                    System.err.println(line);
                }
            }
        }
    }

}
