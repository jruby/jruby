/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core;

import java.util.concurrent.*;

import org.jruby.RubyThread.Status;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.subsystems.*;

/**
 * Represents the Ruby {@code Thread} class. Implemented using Java threads, but note that there is
 * not a one-to-one mapping between Ruby threads and Java threads - specifically in combination with
 * fibers as they are currently implemented as their own Java threads.
 */
public class RubyThread extends RubyObject {

    public Object getValue() {
        return value;
    }

    public RubyException getException() {
        return exception;
    }

    public void shutdown() {
    }

    /**
     * The class from which we create the object that is {@code Thread}. A subclass of
     * {@link RubyClass} so that we can override {@link RubyClass#newInstance} and allocate a
     * {@link RubyThread} rather than a normal {@link RubyBasicObject}.
     */
    public static class RubyThreadClass extends RubyClass {

        public RubyThreadClass(RubyContext context, RubyClass objectClass) {
            super(context, objectClass, objectClass, "Thread");
        }

        @Override
        public RubyBasicObject newInstance(RubyNode currentNode) {
            return new RubyThread(this, getContext().getThreadManager());
        }

    }

    private final ThreadManager manager;

    private final CountDownLatch finished = new CountDownLatch(1);

    private Status status = Status.RUN;

    private RubyException exception;
    private Object value;

    public RubyThread(RubyClass rubyClass, ThreadManager manager) {
        super(rubyClass);
        this.manager = manager;
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

        new Thread(new Runnable() {

            @Override
            public void run() {
                finalThread.manager.registerThread(finalThread);
                finalThread.manager.enterGlobalLock(finalThread);
                context.getSafepointManager().enterThread();

                try {
                    finalRunnable.run();
                } catch (ThreadExitException e) {
                    return;
                } catch (RaiseException e) {
                    exception = (RubyException) e.getRubyException();
                } catch (ReturnException e) {
                    exception = getContext().getCoreLibrary().unexpectedReturn(currentNode);
                } finally {
                    finalThread.manager.leaveGlobalLock();
                    finalThread.manager.unregisterThread(finalThread);
                    finalThread.finished.countDown();
                    context.getSafepointManager().leaveThread();
                    status = Status.DEAD;
                }
            }

        }).start();
    }

    public void join() {
        final RubyThread runningThread = getContext().getThreadManager().leaveGlobalLock();

        try {
            while (true) {
                try {
                    finished.await();
                    break;
                } catch (InterruptedException e) {
                    // Await again
                }
            }
        } finally {
            getContext().getThreadManager().enterGlobalLock(runningThread);
        }

        if (exception != null) {
            throw new RaiseException(exception);
        }
    }

    public Status getStatus() {
        return status;
    }

}
