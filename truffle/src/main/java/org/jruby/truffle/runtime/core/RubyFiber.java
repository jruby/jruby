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
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private final boolean isRootFiber;
    // we need 2 slots when the safepoint manager sends the kill message and there is another message unprocessed
    private final BlockingQueue<FiberMessage> messageQueue = new LinkedBlockingQueue<>(2);
    private RubyFiber lastResumedByFiber = null;
    private boolean alive = true;

    private volatile Thread thread;

    public RubyFiber(RubyThread parent, RubyClass rubyClass, String name) {
        this(parent, parent.getFiberManager(), parent.getThreadManager(), rubyClass, name, false);
    }

    public static RubyFiber newRootFiber(RubyThread thread, FiberManager fiberManager, ThreadManager threadManager) {
        RubyContext context = thread.getContext();
        return new RubyFiber(thread, fiberManager, threadManager, context.getCoreLibrary().getFiberClass(), "root Fiber for Thread", true);
    }

    private RubyFiber(RubyThread parent, FiberManager fiberManager, ThreadManager threadManager, RubyClass rubyClass, String name, boolean isRootFiber) {
        super(rubyClass);
        this.rubyThread = parent;
        this.fiberManager = fiberManager;
        this.threadManager = threadManager;
        this.name = name;
        this.isRootFiber = isRootFiber;
    }

    public void initialize(final RubyProc block) {
        RubyNode.notDesignedForCompilation();

        name = "Ruby Fiber@" + block.getSharedMethodInfo().getSourceSection().getShortDescription();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleFiberExceptions(block);
            }
        });
        thread.setName(name);
        thread.start();
    }

    private void handleFiberExceptions(final RubyProc block) {
        run(new Runnable() {
            @Override
            public void run() {
                try {
                    final Object[] args = waitForResume();
                    final Object result = block.rootCall(args);
                    resume(lastResumedByFiber, true, result);
                } catch (FiberExitException e) {
                    assert !isRootFiber;
                    // Naturally exit the Java thread on catching this
                } catch (ReturnException e) {
                    sendMessageTo(lastResumedByFiber, new FiberExceptionMessage(getContext().getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    sendMessageTo(lastResumedByFiber, new FiberExceptionMessage(e.getRubyException()));
                }
            }
        });
    }

    protected void run(final Runnable task) {
        RubyNode.notDesignedForCompilation();

        start();
        try {
            task.run();
        } finally {
            cleanup();
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void start() {
        thread = Thread.currentThread();
        fiberManager.registerFiber(this);
        getContext().getSafepointManager().enterThread();
        threadManager.enterGlobalLock(rubyThread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void cleanup() {
        alive = false;
        threadManager.leaveGlobalLock();
        getContext().getSafepointManager().leaveThread();
        fiberManager.unregisterFiber(this);
        thread = null;
    }

    public Thread getJavaThread() {
        return thread;
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
        assert !isRootFiber;
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

    public boolean isRootFiber() {
        return isRootFiber;
    }

    public String getName() {
        return name;
    }

    public static class FiberAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            RubyThread parent = context.getThreadManager().getCurrentThread();
            return new RubyFiber(parent, rubyClass, null);
        }

    }

}
