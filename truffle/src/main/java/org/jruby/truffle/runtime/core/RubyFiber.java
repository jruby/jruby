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

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ProcNodes;
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

    public class FiberFields {
        public final RubyThread rubyThread;
        public String name;
        public final boolean isRootFiber;
        // we need 2 slots when the safepoint manager sends the kill message and there is another message unprocessed
        public final BlockingQueue<FiberNodes.FiberMessage> messageQueue = new LinkedBlockingQueue<>(2);
        public RubyFiber lastResumedByFiber = null;
        public boolean alive = true;
        public volatile Thread thread;

        public FiberFields(RubyThread rubyThread, boolean isRootFiber) {
            this.rubyThread = rubyThread;
            this.isRootFiber = isRootFiber;
        }
    }

    private final FiberFields fields;

    public RubyFiber(RubyThread parent, RubyClass rubyClass, String name) {
        this(parent, parent.getFiberManager(), parent.getThreadManager(), rubyClass, name, false);
    }

    public static RubyFiber newRootFiber(RubyThread thread, FiberManager fiberManager, ThreadManager threadManager) {
        RubyContext context = thread.getContext();
        return new RubyFiber(thread, fiberManager, threadManager, context.getCoreLibrary().getFiberClass(), "root Fiber for Thread", true);
    }

    private RubyFiber(RubyThread parent, FiberManager fiberManager, ThreadManager threadManager, RubyClass rubyClass, String name, boolean isRootFiber) {
        super(rubyClass);
        fields = new FiberFields(parent, isRootFiber);
        fields.name = name;
    }

    public void initialize(final RubyBasicObject block) {
        assert RubyGuards.isRubyProc(block);
        fields.name = "Ruby Fiber@" + ProcNodes.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleFiberExceptions(block);
            }
        });
        thread.setName(fields.name);
        thread.start();
    }

    private void handleFiberExceptions(final RubyBasicObject block) {
        assert RubyGuards.isRubyProc(block);

        run(new Runnable() {
            @Override
            public void run() {
                try {
                    final Object[] args = waitForResume();
                    final Object result = ProcNodes.rootCall(block, args);
                    resume(fields.lastResumedByFiber, true, result);
                } catch (FiberNodes.FiberExitException e) {
                    assert !fields.isRootFiber;
                    // Naturally exit the Java thread on catching this
                } catch (ReturnException e) {
                    sendMessageTo(fields.lastResumedByFiber, new FiberNodes.FiberExceptionMessage(getContext().getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    sendMessageTo(fields.lastResumedByFiber, new FiberNodes.FiberExceptionMessage((RubyBasicObject) e.getRubyException()));
                }
            }
        });
    }

    protected void run(final Runnable task) {
        start();
        try {
            task.run();
        } finally {
            cleanup();
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void start() {
        fields.thread = Thread.currentThread();
        fields.rubyThread.getFiberManager().registerFiber(this);
        getContext().getSafepointManager().enterThread();
        getContext().getThreadManager().enterGlobalLock(fields.rubyThread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public void cleanup() {
        fields.alive = false;
        getContext().getThreadManager().leaveGlobalLock();
        getContext().getSafepointManager().leaveThread();
        fields.rubyThread.getFiberManager().unregisterFiber(this);
        fields.thread = null;
    }

    public Thread getJavaThread() {
        return fields.thread;
    }

    public RubyThread getRubyThread() {
        return fields.rubyThread;
    }

    private void sendMessageTo(RubyFiber fiber, FiberNodes.FiberMessage message) {
        fiber.fields.messageQueue.add(message);
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     */
    private Object[] waitForResume() {
        final FiberNodes.FiberMessage message = getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<FiberNodes.FiberMessage>() {
            @Override
            public FiberNodes.FiberMessage block() throws InterruptedException {
                return fields.messageQueue.take();
            }
        });

        fields.rubyThread.getFiberManager().setCurrentFiber(this);

        if (message instanceof FiberNodes.FiberExitMessage) {
            throw new FiberNodes.FiberExitException();
        } else if (message instanceof FiberNodes.FiberExceptionMessage) {
            throw new RaiseException(((FiberNodes.FiberExceptionMessage) message).getException());
        } else if (message instanceof FiberNodes.FiberResumeMessage) {
            final FiberNodes.FiberResumeMessage resumeMessage = (FiberNodes.FiberResumeMessage) message;
            assert getContext().getThreadManager().getCurrentThread() == resumeMessage.getSendingFiber().getRubyThread();
            if (!(resumeMessage.isYield())) {
                fields.lastResumedByFiber = resumeMessage.getSendingFiber();
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
        sendMessageTo(fiber, new FiberNodes.FiberResumeMessage(yield, this, args));
    }

    public Object[] transferControlTo(RubyFiber fiber, boolean yield, Object[] args) {
        resume(fiber, yield, args);

        return waitForResume();
    }

    public void shutdown() {
        assert !fields.isRootFiber;
        sendMessageTo(this, new FiberNodes.FiberExitMessage());
    }

    public boolean isAlive() {
        // TODO CS 2-Feb-15 race conditions (but everything in JRuby+Truffle is currently a race condition)
        // TODO CS 2-Feb-15 should just be alive?
        return fields.alive || !fields.messageQueue.isEmpty();
    }

    public RubyFiber getLastResumedByFiber() {
        return fields.lastResumedByFiber;
    }

    public boolean isRootFiber() {
        return fields.isRootFiber;
    }

    public String getName() {
        return fields.name;
    }

}
