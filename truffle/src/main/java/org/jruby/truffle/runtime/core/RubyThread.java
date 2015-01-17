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

import org.jruby.RubyThread.Status;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.concurrent.CountDownLatch;

/**
 * Represents the Ruby {@code Thread} class. Implemented using Java threads, but note that there is
 * not a one-to-one mapping between Ruby threads and Java threads - specifically in combination with
 * fibers as they are currently implemented as their own Java threads.
 */
public class RubyThread extends RubyBasicObject {

    public Object getValue() {
        return value;
    }

    public RubyException getException() {
        return exception;
    }

    public void shutdown() {
    }

    private final ThreadManager manager;

    private final CountDownLatch finished = new CountDownLatch(1);

    private volatile Thread thread;
    private Status status = Status.RUN;

    private RubyException exception;
    private Object value;

    private RubyBasicObject threadLocals;

    public RubyThread(RubyClass rubyClass, ThreadManager manager) {
        super(rubyClass);
        this.manager = manager;
        threadLocals = new RubyBasicObject(rubyClass.getContext().getCoreLibrary().getObjectClass());
    }

    public void ignoreSafepointActions() {
        status = Status.ABORTING;
    }

    public void initialize(RubyContext context, RubyNode currentNode, RubyProc block) {
        final RubyProc finalBlock = block;

        initialize(context, currentNode, new Runnable() {

            @Override
            public void run() {
                value = finalBlock.rootCall();
            }

        });
    }

    public void initialize(final RubyContext context, final RubyNode currentNode, Runnable runnable) {
        final RubyThread finalThread = this;
        final Runnable finalRunnable = runnable;

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                finalThread.manager.registerThread(finalThread);
                context.getSafepointManager().enterThread();
                finalThread.manager.enterGlobalLock(finalThread);

                try {
                    finalRunnable.run();
                } catch (ThreadExitException e) {
                    return;
                } catch (RaiseException e) {
                    exception = e.getRubyException();
                } catch (ReturnException e) {
                    exception = getContext().getCoreLibrary().unexpectedReturn(currentNode);
                } finally {
                    status = Status.ABORTING;
                    context.getThreadManager().leaveGlobalLock();
                    context.getSafepointManager().leaveThread(finalThread);
                    finalThread.manager.unregisterThread(finalThread);

                    finalThread.finished.countDown();
                    status = Status.DEAD;
                    thread = null;
                }
            }

        });
        thread.setDaemon(true);
        thread.start();
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

    public Status getStatus() {
        return status;
    }

    public RubyBasicObject getThreadLocals() {
        return threadLocals;
    }

    public static class ThreadAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyThread(rubyClass, context.getThreadManager());
        }

    }

}
