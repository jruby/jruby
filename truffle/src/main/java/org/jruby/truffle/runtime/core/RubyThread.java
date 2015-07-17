/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import com.oracle.truffle.api.nodes.Node;
import org.jruby.RubyThread.Status;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ProcNodes;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * Represents the Ruby {@code Thread} class. Implemented using Java threads, but note that there is
 * not a one-to-one mapping between Ruby threads and Java threads - specifically in combination with
 * fibers as they are currently implemented as their own Java threads.
 */
public class RubyThread extends RubyBasicObject {

    public enum InterruptMode {
        IMMEDIATE, ON_BLOCKING, NEVER
    }

    public static class ThreadFields {
        public final ThreadManager manager;

        public final FiberManager fiberManager;

        public String name;

        /**
         * We use this instead of {@link Thread#join()} since we don't always have a reference
         * to the {@link Thread} and we want to handle cases where the Thread did not start yet.
         */
        public final CountDownLatch finished = new CountDownLatch(1);

        public volatile Thread thread;
        public volatile Status status = Status.RUN;
        public volatile AtomicBoolean wakeUp = new AtomicBoolean(false);

        public volatile Object exception;
        public volatile Object value;

        public final RubyBasicObject threadLocals;

        public final List<Lock> ownedLocks = new ArrayList<>(); // Always accessed by the same underlying Java thread.

        public boolean abortOnException = false;

        public InterruptMode interruptMode = InterruptMode.IMMEDIATE;

        public ThreadFields(ThreadManager manager, FiberManager fiberManager, RubyBasicObject threadLocals) {
            this.manager = manager;
            this.fiberManager = fiberManager;
            this.threadLocals = threadLocals;
        }
    }

    public ThreadFields fields;

    public RubyThread(RubyClass rubyClass, ThreadManager manager) {
        super(rubyClass);
        fields = new ThreadFields(manager, new FiberManager(this, manager), new RubyBasicObject(rubyClass.getContext().getCoreLibrary().getObjectClass()));
    }

    public void initialize(RubyContext context, Node currentNode, final Object[] arguments, final RubyBasicObject block) {
        assert RubyGuards.isRubyProc(block);
        String info = ProcNodes.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        initialize(context, currentNode, info, new Runnable() {
            @Override
            public void run() {
                fields.value = ProcNodes.rootCall(block, arguments);
            }
        });
    }

    public void initialize(final RubyContext context, final Node currentNode, final String info, final Runnable task) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                RubyThread.this.run(context, currentNode, info, task);
            }
        }).start();
    }

    public void run(final RubyContext context, Node currentNode, String info, Runnable task) {
        fields.name = "Ruby Thread@" + info;
        Thread.currentThread().setName(fields.name);

        start();
        try {
            RubyBasicObject fiber = getRootFiber();
            FiberNodes.run(fiber, task);
        } catch (ThreadExitException e) {
            fields.value = context.getCoreLibrary().getNilObject();
            return;
        } catch (RaiseException e) {
            fields.exception = e.getRubyException();
        } catch (ReturnException e) {
            fields.exception = context.getCoreLibrary().unexpectedReturn(currentNode);
        } finally {
            cleanup();
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void start() {
        fields.thread = Thread.currentThread();
        fields.manager.registerThread(this);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void cleanup() {
        fields.status = Status.ABORTING;
        fields.manager.unregisterThread(this);

        fields.status = Status.DEAD;
        fields.thread = null;
        releaseOwnedLocks();
        fields.finished.countDown();
    }

    public void shutdown() {
        fields.fiberManager.shutdown();
        throw new ThreadExitException();
    }

    public Thread getRootFiberJavaThread() {
        return fields.thread;
    }

    public Thread getCurrentFiberJavaThread() {
        return ((RubyFiber) fields.fiberManager.getCurrentFiber()).fields.thread;
    }

    public void join() {
        fields.manager.runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
            @Override
            public Boolean block() throws InterruptedException {
                fields.finished.await();
                return SUCCESS;
            }
        });

        if (fields.exception != null) {
            throw new RaiseException(fields.exception);
        }
    }

    public boolean join(final int timeoutInMillis) {
        final long start = System.currentTimeMillis();
        final boolean joined = fields.manager.runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
            @Override
            public Boolean block() throws InterruptedException {
                long now = System.currentTimeMillis();
                long waited = now - start;
                if (waited >= timeoutInMillis) {
                    // We need to know whether countDown() was called and we do not want to block.
                    return fields.finished.getCount() == 0;
                }
                return fields.finished.await(timeoutInMillis - waited, TimeUnit.MILLISECONDS);
            }
        });

        if (joined && fields.exception != null) {
            throw new RaiseException(fields.exception);
        }

        return joined;
    }

    public void wakeup() {
        fields.wakeUp.set(true);
        Thread t = fields.thread;
        if (t != null) {
            t.interrupt();
        }
    }

    public void acquiredLock(Lock lock) {
        fields.ownedLocks.add(lock);
    }

    public void releasedLock(Lock lock) {
        // TODO: this is O(ownedLocks.length).
        fields.ownedLocks.remove(lock);
    }

    protected void releaseOwnedLocks() {
        for (Lock lock : fields.ownedLocks) {
            lock.unlock();
        }
    }

    public Status getStatus() {
        return fields.status;
    }

    public void setStatus(Status status) {
        fields.status = status;
    }

    public RubyBasicObject getThreadLocals() {
        return fields.threadLocals;
    }

    public Object getValue() {
        return fields.value;
    }

    public Object getException() {
        return fields.exception;
    }

    public String getName() {
        return fields.name;
    }

    public void setName(String name) {
        fields.name = name;
    }

    public ThreadManager getThreadManager() {
        return fields.manager;
    }

    public FiberManager getFiberManager() {
        return fields.fiberManager;
    }

    public RubyBasicObject getRootFiber() {
        return fields.fiberManager.getRootFiber();
    }

    public boolean isAbortOnException() {
        return fields.abortOnException;
    }

    public void setAbortOnException(boolean abortOnException) {
        fields.abortOnException = abortOnException;
    }

    public InterruptMode getInterruptMode() {
        return fields.interruptMode;
    }

    public void setInterruptMode(InterruptMode interruptMode) {
        fields.interruptMode = interruptMode;
    }

    /** Return whether Thread#{run,wakeup} was called and clears the wakeup flag. */
    public boolean shouldWakeUp() {
        return fields.wakeUp.getAndSet(false);
    }

    public static class ThreadAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyThread(rubyClass, context.getThreadManager());
        }

    }

}
