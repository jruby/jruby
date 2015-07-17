/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNodeGen;
import org.jruby.truffle.nodes.core.FiberNodesFactory.FiberTransferNodeFactory;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyFiber;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;

@CoreClass(name = "Fiber")
public abstract class FiberNodes {

    public static RubyFiber.FiberFields getFields(RubyFiber fiber) {
        return fiber.fields;
    }

    public static RubyFiber newRootFiber(RubyThread thread, FiberManager fiberManager, ThreadManager threadManager) {
        RubyContext context = thread.getContext();
        return createRubyFiber(thread, fiberManager, threadManager, context.getCoreLibrary().getFiberClass(), "root Fiber for Thread", true);
    }

    public static void initialize(final RubyFiber fiber, final RubyBasicObject block) {
        assert RubyGuards.isRubyProc(block);
        getFields(fiber).name = "Ruby Fiber@" + ProcNodes.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                handleFiberExceptions(fiber, block);
            }
        });
        thread.setName(getFields(fiber).name);
        thread.start();
    }

    private static void handleFiberExceptions(final RubyFiber fiber, final RubyBasicObject block) {
        assert RubyGuards.isRubyProc(block);

        run(fiber, new Runnable() {
            @Override
            public void run() {
                try {
                    final Object[] args = waitForResume(fiber);
                    final Object result = ProcNodes.rootCall(block, args);
                    resume(fiber, getFields(fiber).lastResumedByFiber, true, result);
                } catch (FiberExitException e) {
                    assert !getFields(fiber).isRootFiber;
                    // Naturally exit the Java thread on catching this
                } catch (ReturnException e) {
                    sendMessageTo(getFields(fiber).lastResumedByFiber, new FiberExceptionMessage(fiber.getContext().getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    sendMessageTo(getFields(fiber).lastResumedByFiber, new FiberExceptionMessage((RubyBasicObject) e.getRubyException()));
                }
            }
        });
    }

    public static void run(RubyFiber fiber, final Runnable task) {
        start(fiber);
        try {
            task.run();
        } finally {
            cleanup(fiber);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void start(RubyFiber fiber) {
        getFields(fiber).thread = Thread.currentThread();
        getFields(fiber).rubyThread.getFiberManager().registerFiber(fiber);
        fiber.getContext().getSafepointManager().enterThread();
        fiber.getContext().getThreadManager().enterGlobalLock(getFields(fiber).rubyThread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void cleanup(RubyFiber fiber) {
        getFields(fiber).alive = false;
        fiber.getContext().getThreadManager().leaveGlobalLock();
        fiber.getContext().getSafepointManager().leaveThread();
        getFields(fiber).rubyThread.getFiberManager().unregisterFiber(fiber);
        getFields(fiber).thread = null;
    }

    private static void sendMessageTo(RubyFiber fiber, FiberMessage message) {
        getFields(fiber).messageQueue.add(message);
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     * @param fiber
     */
    private static Object[] waitForResume(final RubyFiber fiber) {
        final FiberMessage message = fiber.getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingActionWithoutGlobalLock<FiberMessage>() {
            @Override
            public FiberMessage block() throws InterruptedException {
                return getFields(fiber).messageQueue.take();
            }
        });

        getFields(fiber).rubyThread.getFiberManager().setCurrentFiber(fiber);

        if (message instanceof FiberExitMessage) {
            throw new FiberExitException();
        } else if (message instanceof FiberExceptionMessage) {
            throw new RaiseException(((FiberExceptionMessage) message).getException());
        } else if (message instanceof FiberResumeMessage) {
            final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;
            assert fiber.getContext().getThreadManager().getCurrentThread() == resumeMessage.getSendingFiber().fields.rubyThread;
            if (!(resumeMessage.isYield())) {
                getFields(fiber).lastResumedByFiber = resumeMessage.getSendingFiber();
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
    private static void resume(RubyFiber fromFiber, RubyFiber fiber, boolean yield, Object... args) {
        sendMessageTo(fiber, new FiberResumeMessage(yield, fromFiber, args));
    }

    public static Object[] transferControlTo(RubyFiber fromFiber, RubyFiber fiber, boolean yield, Object[] args) {
        resume(fromFiber, fiber, yield, args);

        return waitForResume(fromFiber);
    }

    public static void shutdown(RubyFiber fiber) {
        assert !getFields(fiber).isRootFiber;
        sendMessageTo(fiber, new FiberExitMessage());
    }

    public static boolean isAlive(RubyFiber fiber) {
        // TODO CS 2-Feb-15 race conditions (but everything in JRuby+Truffle is currently a race condition)
        // TODO CS 2-Feb-15 should just be alive?
        return getFields(fiber).alive || !getFields(fiber).messageQueue.isEmpty();
    }

    public static RubyFiber createRubyFiber(RubyThread parent, RubyClass rubyClass, String name) {
        return new RubyFiber(parent, rubyClass, name);
    }

    public static RubyFiber createRubyFiber(RubyThread parent, FiberManager fiberManager, ThreadManager threadManager, RubyClass rubyClass, String name, boolean isRootFiber) {
        return new RubyFiber(parent, fiberManager, threadManager, rubyClass, name, isRootFiber);
    }

    public interface FiberMessage {
    }

    public abstract static class FiberTransferNode extends CoreMethodArrayArgumentsNode {

        @Child SingleValueCastNode singleValueCastNode;

        public FiberTransferNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected Object singleValue(VirtualFrame frame, Object[] args) {
            if (singleValueCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singleValueCastNode = insert(SingleValueCastNodeGen.create(getContext(), getSourceSection(), null));
            }
            return singleValueCastNode.executeSingleValue(frame, args);
        }

        public abstract Object executeTransferControlTo(VirtualFrame frame, RubyFiber fiber, boolean isYield, Object[] args);

        @Specialization
        protected Object transfer(VirtualFrame frame, RubyFiber fiber, boolean isYield, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            if (!isAlive(fiber)) {
                throw new RaiseException(getContext().getCoreLibrary().deadFiberCalledError(this));
            }

            RubyThread currentThread = getContext().getThreadManager().getCurrentThread();
            if (fiber.fields.rubyThread != currentThread) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().fiberError("fiber called across threads", this));
            }

            final RubyFiber sendingFiber = currentThread.getFiberManager().getCurrentFiber();

            return singleValue(frame, transferControlTo(sendingFiber, fiber, isYield, args));
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject initialize(RubyFiber fiber, RubyBasicObject block) {
            CompilerDirectives.transferToInterpreter();

            FiberNodes.initialize(fiber, block);
            return nil();
        }

    }

    @CoreMethod(names = "resume", argumentsAsArray = true)
    public abstract static class ResumeNode extends CoreMethodArrayArgumentsNode {

        @Child FiberTransferNode fiberTransferNode;

        public ResumeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        @Specialization
        public Object resume(VirtualFrame frame, RubyFiber fiberBeingResumed, Object[] args) {
            return fiberTransferNode.executeTransferControlTo(frame, fiberBeingResumed, false, args);
        }

    }

    @CoreMethod(names = "yield", onSingleton = true, argumentsAsArray = true)
    public abstract static class YieldNode extends CoreMethodArrayArgumentsNode {

        @Child FiberTransferNode fiberTransferNode;

        public YieldNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        @Specialization
        public Object yield(VirtualFrame frame, Object[] args) {
            RubyThread currentThread = getContext().getThreadManager().getCurrentThread();
            final RubyFiber yieldingFiber = currentThread.getFiberManager().getCurrentFiber();
            final RubyFiber fiberYieldedTo = yieldingFiber.fields.lastResumedByFiber;

            if (yieldingFiber.fields.isRootFiber || fiberYieldedTo == null) {
                throw new RaiseException(getContext().getCoreLibrary().yieldFromRootFiberError(this));
            }

            return fiberTransferNode.executeTransferControlTo(frame, fiberYieldedTo, true, args);
        }

    }

    public static class FiberResumeMessage implements FiberMessage {

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

    public static class FiberExitMessage implements FiberMessage {
    }

    public static class FiberExceptionMessage implements FiberMessage {

        private final RubyBasicObject exception;

        public FiberExceptionMessage(RubyBasicObject exception) {
            this.exception = exception;
        }

        public RubyBasicObject getException() {
            return exception;
        }

    }

    public static class FiberExitException extends ControlFlowException {
        private static final long serialVersionUID = 1522270454305076317L;
    }

    public static class FiberAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            RubyThread parent = context.getThreadManager().getCurrentThread();
            return createRubyFiber(parent, rubyClass, null);
        }

    }
}
