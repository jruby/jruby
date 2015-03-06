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
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.truffle.runtime.subsystems.ObjectSpaceManager.ObjectGraphVisitor;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;

/**
 * Represents the Ruby {@code Thread} class. Implemented using Java threads, but note that there is
 * not a one-to-one mapping between Ruby threads and Java threads - specifically in combination with
 * fibers as they are currently implemented as their own Java threads.
 */
public class RubyThread extends RubyBasicObject {

    private final ThreadManager manager;

    private String name;

    private final CountDownLatch finished = new CountDownLatch(1);

    private volatile Thread thread;
    private volatile Status status = Status.RUN;

    private volatile RubyException exception;
    private volatile Object value;

    private final RubyBasicObject threadLocals;

    private final List<Lock> ownedLocks = new ArrayList<>(); // Always accessed by the same underlying Java thread.

    private final List<Runnable> deferredSafepointActions = new ArrayList<>();

    public RubyThread(RubyClass rubyClass, ThreadManager manager) {
        super(rubyClass);
        this.manager = manager;
        threadLocals = new RubyBasicObject(rubyClass.getContext().getCoreLibrary().getObjectClass());
    }

    public void initialize(RubyContext context, Node currentNode, final RubyProc block) {
        String info = block.getSharedMethodInfo().getSourceSection().getShortDescription();
        initialize(context, currentNode, info, new Runnable() {
            @Override
            public void run() {
                value = block.rootCall();
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
        name = "Ruby Thread@" + info;
        thread = Thread.currentThread();
        thread.setName(name);

        manager.registerThread(this);
        context.getSafepointManager().enterThread();
        manager.enterGlobalLock(this);

        try {
            task.run();
        } catch (ThreadExitException e) {
            value = context.getCoreLibrary().getNilObject();
            return;
        } catch (RaiseException e) {
            exception = e.getRubyException();
        } catch (ReturnException e) {
            exception = getContext().getCoreLibrary().unexpectedReturn(currentNode);
        } finally {
            cleanup(context);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void cleanup(RubyContext context) {
        status = Status.ABORTING;
        context.getThreadManager().leaveGlobalLock();
        context.getSafepointManager().leaveThread();
        manager.unregisterThread(this);

        status = Status.DEAD;
        thread = null;
        releaseOwnedLocks();
        finished.countDown();
    }

    public void setRootThread(Thread thread) {
        this.thread = thread;
    }

    public void join() {
        getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
            @Override
            public Boolean block() throws InterruptedException {
                finished.await();
                return SUCCESS;
            }
        });

        if (exception != null) {
            throw new RaiseException(exception);
        }
    }

    public void interrupt() {
        Thread t = thread;
        if (t != null) {
            t.interrupt();
        }
    }

    public void acquiredLock(Lock lock) {
        ownedLocks.add(lock);
    }

    public void releasedLock(Lock lock) {
        RubyNode.notDesignedForCompilation("475f58dfbb7f4e499a9734e3e63aad97");

        // TODO: this is O(ownedLocks.length).
        ownedLocks.remove(lock);
    }

    protected void releaseOwnedLocks() {
        for (Lock lock : ownedLocks) {
            lock.unlock();
        }
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public RubyBasicObject getThreadLocals() {
        return threadLocals;
    }

    public Object getValue() {
        return value;
    }

    public RubyException getException() {
        return exception;
    }

    public void exit() {
        throw new ThreadExitException();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Runnable> getDeferredSafepointActions() {
        return deferredSafepointActions;
    }

    public static class ThreadAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyThread(rubyClass, context.getThreadManager());
        }

    }

}
