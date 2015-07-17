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

    public static RubyFiber.FiberFields getFields(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        return ((RubyFiber) fiber).fields;
    }

    public static RubyBasicObject newRootFiber(RubyThread thread, FiberManager fiberManager, ThreadManager threadManager) {
        RubyContext context = thread.getContext();
        return createRubyFiber(thread, fiberManager, threadManager, context.getCoreLibrary().getFiberClass(), "root Fiber for Thread", true);
    }

    public static void initialize(final RubyBasicObject fiber, final RubyBasicObject block) {
        assert RubyGuards.isRubyFiber(fiber);
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

    private static void handleFiberExceptions(final RubyBasicObject fiber, final RubyBasicObject block) {
        assert RubyGuards.isRubyFiber(fiber);
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

    public static void run(RubyBasicObject fiber, final Runnable task) {
        assert RubyGuards.isRubyFiber(fiber);

        start(fiber);
        try {
            task.run();
        } finally {
            cleanup(fiber);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void start(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).thread = Thread.currentThread();
        ThreadNodes.getFiberManager(getFields(fiber).rubyThread).registerFiber(fiber);
        fiber.getContext().getSafepointManager().enterThread();
        fiber.getContext().getThreadManager().enterGlobalLock(getFields(fiber).rubyThread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void cleanup(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).alive = false;
        fiber.getContext().getThreadManager().leaveGlobalLock();
        fiber.getContext().getSafepointManager().leaveThread();
        ThreadNodes.getFiberManager(getFields(fiber).rubyThread).unregisterFiber(fiber);
        getFields(fiber).thread = null;
    }

    private static void sendMessageTo(RubyBasicObject fiber, FiberMessage message) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).messageQueue.add(message);
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     * @param fiber
     */
    private static Object[] waitForResume(final RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);

        final FiberMessage message = fiber.getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingActionWithoutGlobalLock<FiberMessage>() {
            @Override
            public FiberMessage block() throws InterruptedException {
                return getFields(fiber).messageQueue.take();
            }
        });

        ThreadNodes.getFiberManager(getFields(fiber).rubyThread).setCurrentFiber(fiber);

        if (message instanceof FiberExitMessage) {
            throw new FiberExitException();
        } else if (message instanceof FiberExceptionMessage) {
            throw new RaiseException(((FiberExceptionMessage) message).getException());
        } else if (message instanceof FiberResumeMessage) {
            final FiberResumeMessage resumeMessage = (FiberResumeMessage) message;
            assert fiber.getContext().getThreadManager().getCurrentThread() == ((RubyFiber) resumeMessage.getSendingFiber()).fields.rubyThread;
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
    private static void resume(RubyBasicObject fromFiber, RubyBasicObject fiber, boolean yield, Object... args) {
        assert RubyGuards.isRubyFiber(fromFiber);
        assert RubyGuards.isRubyFiber(fiber);

        sendMessageTo(fiber, new FiberResumeMessage(yield, fromFiber, args));
    }

    public static Object[] transferControlTo(RubyBasicObject fromFiber, RubyBasicObject fiber, boolean yield, Object[] args) {
        assert RubyGuards.isRubyFiber(fromFiber);
        assert RubyGuards.isRubyFiber(fiber);

        resume(fromFiber, fiber, yield, args);

        return waitForResume(fromFiber);
    }

    public static void shutdown(RubyBasicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        assert !getFields(fiber).isRootFiber;
        sendMessageTo(fiber, new FiberExitMessage());
    }

    public static boolean isAlive(RubyBasicObject fiber) {
        // TODO CS 2-Feb-15 race conditions (but everything in JRuby+Truffle is currently a race condition)
        // TODO CS 2-Feb-15 should just be alive?
        assert RubyGuards.isRubyFiber(fiber);
        return getFields(fiber).alive || !getFields(fiber).messageQueue.isEmpty();
    }

    public static RubyBasicObject createRubyFiber(RubyThread parent, RubyClass rubyClass, String name) {
        return new RubyFiber(parent, rubyClass, name);
    }

    public static RubyBasicObject createRubyFiber(RubyThread parent, FiberManager fiberManager, ThreadManager threadManager, RubyClass rubyClass, String name, boolean isRootFiber) {
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

        public abstract Object executeTransferControlTo(VirtualFrame frame, RubyBasicObject fiber, boolean isYield, Object[] args);

        @Specialization(guards = "isRubyFiber(fiber)")
        protected Object transfer(VirtualFrame frame, RubyBasicObject fiber, boolean isYield, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            if (!isAlive(fiber)) {
                throw new RaiseException(getContext().getCoreLibrary().deadFiberCalledError(this));
            }

            RubyThread currentThread = getContext().getThreadManager().getCurrentThread();
            if (((RubyFiber) fiber).fields.rubyThread != currentThread) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().fiberError("fiber called across threads", this));
            }

            final RubyBasicObject sendingFiber = ThreadNodes.getFiberManager(currentThread).getCurrentFiber();

            return singleValue(frame, transferControlTo(sendingFiber, fiber, isYield, args));
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject initialize(RubyBasicObject fiber, RubyBasicObject block) {
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
        public Object resume(VirtualFrame frame, RubyBasicObject fiberBeingResumed, Object[] args) {
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
            final RubyBasicObject yieldingFiber = ThreadNodes.getFiberManager(currentThread).getCurrentFiber();
            final RubyBasicObject fiberYieldedTo = ((RubyFiber) yieldingFiber).fields.lastResumedByFiber;

            if (((RubyFiber) yieldingFiber).fields.isRootFiber || fiberYieldedTo == null) {
                throw new RaiseException(getContext().getCoreLibrary().yieldFromRootFiberError(this));
            }

            return fiberTransferNode.executeTransferControlTo(frame, fiberYieldedTo, true, args);
        }

    }

    public static class FiberResumeMessage implements FiberMessage {

        private final boolean yield;
        private final RubyBasicObject sendingFiber;
        private final Object[] args;

        public FiberResumeMessage(boolean yield, RubyBasicObject sendingFiber, Object[] args) {
            assert RubyGuards.isRubyFiber(sendingFiber);
            this.yield = yield;
            this.sendingFiber = sendingFiber;
            this.args = args;
        }

        public boolean isYield() {
            return yield;
        }

        public RubyBasicObject getSendingFiber() {
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
