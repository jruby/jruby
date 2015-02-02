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

import com.oracle.truffle.api.nodes.ControlFlowException;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Represents the Ruby {@code Fiber} class. The current implementation uses Java threads and message
 * passing. Note that the relationship between Java threads, Ruby threads and Ruby fibers is
 * complex. A Java thread might be running a fiber that on difference resumptions is representing
 * different Ruby threads. Take note of the lock contracts on {@link #waitForResume} and
 * {@link #resume}.
 */
public class RubyFiber extends RubyBasicObject {

    private interface FiberMessage {
    }

    private static class FiberResumeMessage implements FiberMessage {

        private final boolean yield;
        private final RubyThread thread;
        private final RubyFiber sendingFiber;
        private final Object arg;

        public FiberResumeMessage(boolean yield, RubyThread thread, RubyFiber sendingFiber, Object arg) {
            this.yield = yield;
            this.thread = thread;
            this.sendingFiber = sendingFiber;
            this.arg = arg;
        }

        public boolean isYield() {
            return yield;
        }

        public RubyThread getThread() {
            return thread;
        }

        public RubyFiber getSendingFiber() {
            return sendingFiber;
        }

        public Object getArg() {
            return arg;
        }

    }

    private static class FiberExitMessage implements FiberMessage {
    }

    private static class FiberExceptionMessage implements FiberMessage {

        public RubyThread thread;
        public RubyException exception;

        public FiberExceptionMessage(RubyThread thread, RubyException exception) {
            this.thread = thread;
            this.exception = exception;
        }

        public RubyThread getThread() {
            return thread;
        }

        public RubyException getException() {
            return exception;
        }

    }

    public static class FiberExitException extends ControlFlowException {

        private static final long serialVersionUID = 1522270454305076317L;

    }

    private final FiberManager fiberManager;
    private final ThreadManager threadManager;

    private String name;
    private boolean topLevel;
    private BlockingQueue<FiberMessage> messageQueue = new ArrayBlockingQueue<>(1);
    private RubyFiber lastResumedByFiber = null;
    private boolean alive = true;

    public RubyFiber(RubyClass rubyClass, FiberManager fiberManager, ThreadManager threadManager, String name, boolean topLevel) {
        super(rubyClass);
        this.fiberManager = fiberManager;
        this.threadManager = threadManager;
        this.name = name;
        this.topLevel = topLevel;
    }

    public void initialize(RubyProc block) {
        RubyNode.notDesignedForCompilation();

        name = "Ruby Fiber@" + block.getSharedMethodInfo().getSourceSection().getShortDescription();

        final RubyThread rubyThread = threadManager.getCurrentThread();
        final RubyFiber finalFiber = this;
        final RubyProc finalBlock = block;

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                fiberManager.registerFiber(finalFiber);
                finalFiber.getContext().getSafepointManager().enterThread();
                threadManager.enterGlobalLock(rubyThread);

                try {
                    final Object arg = finalFiber.waitForResume();
                    final Object result = finalBlock.rootCall(arg);
                    finalFiber.lastResumedByFiber.resume(true, finalFiber, result);
                } catch (FiberExitException e) {
                    // Naturally exit the thread on catching this
                } catch (ReturnException e) {
                    finalFiber.lastResumedByFiber.messageQueue.add(new FiberExceptionMessage(rubyThread, finalFiber.getContext().getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    finalFiber.lastResumedByFiber.messageQueue.add(new FiberExceptionMessage(rubyThread, e.getRubyException()));
                } finally {
                    alive = false;
                    threadManager.leaveGlobalLock();
                    finalFiber.getContext().getSafepointManager().leaveThread();
                    fiberManager.unregisterFiber(finalFiber);
                }
            }

        });
        thread.setName(name);
        thread.start();
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     */
    public Object waitForResume() {
        RubyNode.notDesignedForCompilation();

        final FiberMessage message;

        message = getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<FiberMessage>() {
            @Override
            public FiberMessage block() throws InterruptedException {
                // TODO (CS 30-Jan-15) this timeout isn't ideal - we already handle interrupts for safepoints
                return messageQueue.poll(1, TimeUnit.SECONDS);
            }
        });

        fiberManager.setCurrentFiber(this);

        if (message instanceof FiberExitMessage) {
            // TODO CS 2-Feb-15 what do we do about entering the global lock here?
            throw new FiberExitException();
        } else if (message instanceof FiberExceptionMessage) {
            throw new RaiseException(((FiberExceptionMessage) message).getException());
        } else if (message instanceof FiberResumeMessage) {
            if (!((FiberResumeMessage) message).isYield()) {
                lastResumedByFiber = ((FiberResumeMessage) message).getSendingFiber();
            }
            return ((FiberResumeMessage) message).getArg();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Send a message to a fiber by posting into a message queue. Doesn't explicitly notify the Java
     * thread (although the queue implementation may) and doesn't wait for the message to be
     * received.
     */
    public void resume(boolean yield, RubyFiber sendingFiber, Object... args) {
        RubyNode.notDesignedForCompilation();

        // TODO CS 2-Feb-15 move this logic into the node where we can specialise?

        Object arg;

        if (args.length == 0) {
            arg = getContext().getCoreLibrary().getNilObject();
        } else if (args.length == 1) {
            arg = args[0];
        } else {
            arg = RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), args);
        }

        messageQueue.add(new FiberResumeMessage(yield, threadManager.getCurrentThread(), sendingFiber, arg));
    }

    public void shutdown() {
        RubyNode.notDesignedForCompilation();

        messageQueue.add(new FiberExitMessage());
    }

    public boolean isAlive() {
        // TODO CS 2-Feb-15 race conditions (but everything in JRuby+Truffle is currently a race condition)
        // TODO CS 2-Feb-15 should just be alive?
        return alive || !messageQueue.isEmpty();
    }

    public RubyFiber getLastResumedByFiber() {
        return lastResumedByFiber;
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public String getName() {
        return name;
    }

    public static class FiberAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyFiber(rubyClass, context.getFiberManager(), context.getThreadManager(), null, false);
        }

    }

}
