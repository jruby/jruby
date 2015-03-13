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

import com.oracle.truffle.api.nodes.Node;
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

/**
 * Represents the Ruby {@code Fiber} class. The current implementation uses Java threads and message
 * passing. Note that with fibers, a Ruby thread has multiple Java threads which interleave.
 * A {@code Fiber} is associated with a single (Ruby) {@code Thread}.
 */
public class RubyFiber extends RubyBasicObject {

    private interface FiberMessage {
    }

    private static class FiberResumeMessage implements FiberMessage {

        private final boolean yield;
        private final RubyFiber sendingFiber;
        private final Object[] args;

        public FiberResumeMessage(boolean yield, RubyFiber sendingFiber, Object[] args) {
            this.yield = yield;
            this.sendingFiber = sendingFiber;
            this.args = args;
        }

        public boolean isYield() {
            return yield;
        }

        public RubyFiber getSendingFiber() {
            return sendingFiber;
        }

        public Object[] getArgs() {
            return args;
        }

    }

    private static class FiberExitMessage implements FiberMessage {
    }

    private static class FiberExceptionMessage implements FiberMessage {

        private final RubyException exception;

        public FiberExceptionMessage(RubyException exception) {
            this.exception = exception;
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
    private final RubyThread rubyThread;

    private String name;
    private final boolean topLevel;
    private final BlockingQueue<FiberMessage> messageQueue = new ArrayBlockingQueue<>(1);
    private RubyFiber lastResumedByFiber = null;
    private boolean alive = true;

    public RubyFiber(RubyClass rubyClass, FiberManager fiberManager, ThreadManager threadManager, String name, boolean topLevel) {
        super(rubyClass);
        this.fiberManager = fiberManager;
        this.threadManager = threadManager;
        this.name = name;
        this.topLevel = topLevel;
        this.rubyThread = threadManager.getCurrentThread();
    }

    public void initialize(RubyProc block) {
        RubyNode.notDesignedForCompilation();

        name = "Ruby Fiber@" + block.getSharedMethodInfo().getSourceSection().getShortDescription();

        final RubyFiber finalFiber = this;
        final RubyProc finalBlock = block;

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                fiberManager.registerFiber(finalFiber);
                finalFiber.getContext().getSafepointManager().enterThread();
                threadManager.enterGlobalLock(rubyThread);

                try {
                    final Object[] args = finalFiber.waitForResume();
                    final Object result = finalBlock.rootCall(args);
                    finalFiber.resume(finalFiber.lastResumedByFiber, true, result);
                } catch (FiberExitException e) {
                    // Naturally exit the thread on catching this
                } catch (ReturnException e) {
                    sendMessageTo(finalFiber.lastResumedByFiber, new FiberExceptionMessage(finalFiber.getContext().getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    sendMessageTo(finalFiber.lastResumedByFiber, new FiberExceptionMessage(e.getRubyException()));
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

    public RubyThread getRubyThread() {
        return rubyThread;
    }

    private void sendMessageTo(RubyFiber fiber, FiberMessage message) {
        fiber.messageQueue.add(message);
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     */
    private Object[] waitForResume() {
        RubyNode.notDesignedForCompilation();

        final FiberMessage message = getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<FiberMessage>() {
            @Override
            public FiberMessage block() throws InterruptedException {
                return messageQueue.take();
            }
        });

        fiberManager.setCurrentFiber(this);

        if (message instanceof FiberExitMessage) {
            throw new FiberExitException();
        } else if (message instanceof FiberExceptionMessage) {
            throw new RaiseException(((FiberExceptionMessage) message).getException());
        } else if (message instanceof FiberResumeMessage) {
            final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;
            assert threadManager.getCurrentThread() == resumeMessage.getSendingFiber().getRubyThread();
            if (!(resumeMessage.isYield())) {
                lastResumedByFiber = resumeMessage.getSendingFiber();
            }
            return resumeMessage.getArgs();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Send a resume message to a fiber by posting into its message queue. Doesn't explicitly notify the Java
     * thread (although the queue implementation may) and doesn't wait for the message to be
     * received.
     */
    private void resume(RubyFiber fiber, boolean yield, Object... args) {
        RubyNode.notDesignedForCompilation();

        sendMessageTo(fiber, new FiberResumeMessage(yield, this, args));
    }

    public Object[] transferControlTo(RubyFiber fiber, boolean yield, Object[] args) {
        resume(fiber, yield, args);

        return waitForResume();
    }

    public void shutdown() {
        RubyNode.notDesignedForCompilation();

        sendMessageTo(this, new FiberExitMessage());
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
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyFiber(rubyClass, context.getFiberManager(), context.getThreadManager(), null, false);
        }

    }

}
