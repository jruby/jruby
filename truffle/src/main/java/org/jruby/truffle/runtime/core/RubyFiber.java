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

        private final RubyThread thread;
        private final RubyFiber sendingFiber;
        private final Object arg;

        public FiberResumeMessage(RubyThread thread, RubyFiber sendingFiber, Object arg) {
            this.thread = thread;
            this.sendingFiber = sendingFiber;
            this.arg = arg;
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

    public static class FiberExitException extends ControlFlowException {

        private static final long serialVersionUID = 1522270454305076317L;

    }

    private final FiberManager fiberManager;
    private final ThreadManager threadManager;

    private BlockingQueue<FiberMessage> messageQueue = new ArrayBlockingQueue<>(1);
    private RubyFiber lastResumedByFiber = null;
    private boolean alive;

    public RubyFiber(RubyClass rubyClass, FiberManager fiberManager, ThreadManager threadManager) {
        super(rubyClass);
        this.fiberManager = fiberManager;
        this.threadManager = threadManager;
    }

    public void initialize(RubyProc block) {
        RubyNode.notDesignedForCompilation();

        final RubyFiber finalFiber = this;
        final RubyProc finalBlock = block;

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                alive = true;

                finalFiber.getContext().getSafepointManager().enterThread();
                fiberManager.registerFiber(finalFiber);

                try {
                    final Object arg = finalFiber.waitForResume();
                    final Object result = finalBlock.rootCall(arg);
                    finalFiber.lastResumedByFiber.resume(finalFiber, result);
                } catch (FiberExitException e) {
                    // Naturally exit the thread on catching this
                } finally {
                    alive = false;
                    fiberManager.unregisterFiber(finalFiber);
                    finalFiber.getContext().getSafepointManager().leaveThread();
                }
            }

        });
        thread.setName("Ruby Fiber@" + block.getSharedMethodInfo().getSourceSection().getShortDescription());
        thread.start();
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message. On entry, assumes that the GIL is not held. On exit, holding the GIL.
     */
    public Object waitForResume() {
        RubyNode.notDesignedForCompilation();

        FiberMessage message = getContext().getThreadManager().runUntilResult(false, new BlockingActionWithoutGlobalLock<FiberMessage>() {
            @Override
            public FiberMessage block() throws InterruptedException {
                // TODO (CS 30-Jan-15) this timeout isn't ideal - we already handle interrupts for safepoints
                return messageQueue.poll(1, TimeUnit.SECONDS);
            }
        });

        if (message instanceof FiberExitMessage) {
            throw new FiberExitException();
        }

        final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;

        threadManager.enterGlobalLock(resumeMessage.getThread());

        fiberManager.setCurrentFiber(this);

        lastResumedByFiber = resumeMessage.getSendingFiber();
        return resumeMessage.getArg();
    }

    /**
     * Send a message to a fiber by posting into a message queue. Doesn't explicitly notify the Java
     * thread (although the queue implementation may) and doesn't wait for the message to be
     * received. On entry, assumes the the GIL is held. On exit, not holding the GIL.
     */
    public void resume(RubyFiber sendingFiber, Object... args) {
        RubyNode.notDesignedForCompilation();

        Object arg;

        if (args.length == 0) {
            arg = getContext().getCoreLibrary().getNilObject();
        } else if (args.length == 1) {
            arg = args[0];
        } else {
            arg = RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), args);
        }

        final RubyThread runningThread = threadManager.leaveGlobalLock();

        messageQueue.add(new FiberResumeMessage(runningThread, sendingFiber, arg));
    }

    public void shutdown() {
        RubyNode.notDesignedForCompilation();

        messageQueue.add(new FiberExitMessage());
    }

    public boolean isAlive() {
        return alive;
    }

    public RubyFiber getLastResumedByFiber() {
        return lastResumedByFiber;
    }

    public static class FiberAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, RubyNode currentNode) {
            return new RubyFiber(rubyClass, context.getFiberManager(), context.getThreadManager());
        }

    }

}
