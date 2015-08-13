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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNodeGen;
import org.jruby.truffle.nodes.core.FiberNodesFactory.FiberTransferNodeFactory;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.ThreadManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@CoreClass(name = "Fiber")
public abstract class FiberNodes {

    @Layout
    public interface FiberLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createFiberShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createFiber(DynamicObjectFactory factory, FiberFields fields);

        boolean isFiber(DynamicObject object);

        FiberFields getFields(DynamicObject object);

    }

    public static final FiberLayout FIBER_LAYOUT = FiberLayoutImpl.INSTANCE;

    public static FiberFields getFields(DynamicObject fiber) {
        return FIBER_LAYOUT.getFields(fiber);
    }

    public static DynamicObject newRootFiber(DynamicObject thread, FiberManager fiberManager, ThreadManager threadManager) {
        assert RubyGuards.isRubyThread(thread);
        RubyContext context = BasicObjectNodes.getContext(thread);
        return createRubyFiber(thread, fiberManager, threadManager, context.getCoreLibrary().getFiberClass(), "root Fiber for Thread", true);
    }

    public static void initialize(final DynamicObject fiber, final DynamicObject block) {
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

    private static void handleFiberExceptions(final DynamicObject fiber, final DynamicObject block) {
        assert RubyGuards.isRubyFiber(fiber);
        assert RubyGuards.isRubyProc(block);

        run(fiber, new Runnable() {
            @Override
            public void run() {
                try {
                    final Object[] args = waitForResume(fiber);
                    final Object result;
                    try {
                        result = ProcNodes.rootCall(block, args);
                    } finally {
                        // Make sure that other fibers notice we are dead before they gain control back
                        getFields(fiber).alive = false;
                    }
                    resume(fiber, getFields(fiber).lastResumedByFiber, true, result);
                } catch (FiberExitException e) {
                    assert !getFields(fiber).isRootFiber;
                    // Naturally exit the Java thread on catching this
                } catch (ReturnException e) {
                    sendMessageTo(getFields(fiber).lastResumedByFiber, new FiberExceptionMessage(BasicObjectNodes.getContext(fiber).getCoreLibrary().unexpectedReturn(null)));
                } catch (RaiseException e) {
                    sendMessageTo(getFields(fiber).lastResumedByFiber, new FiberExceptionMessage((DynamicObject) e.getRubyException()));
                }
            }
        });
    }

    public static void run(DynamicObject fiber, final Runnable task) {
        assert RubyGuards.isRubyFiber(fiber);

        start(fiber);
        try {
            task.run();
        } finally {
            cleanup(fiber);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void start(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).thread = Thread.currentThread();
        BasicObjectNodes.getContext(fiber).getThreadManager().initializeCurrentThread(getFields(fiber).rubyThread);
        ThreadNodes.getFiberManager(getFields(fiber).rubyThread).registerFiber(fiber);
        BasicObjectNodes.getContext(fiber).getSafepointManager().enterThread();
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void cleanup(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).alive = false;
        BasicObjectNodes.getContext(fiber).getSafepointManager().leaveThread();
        ThreadNodes.getFiberManager(getFields(fiber).rubyThread).unregisterFiber(fiber);
        getFields(fiber).thread = null;
    }

    private static void sendMessageTo(DynamicObject fiber, FiberMessage message) {
        assert RubyGuards.isRubyFiber(fiber);
        getFields(fiber).messageQueue.add(message);
    }

    /**
     * Send the Java thread that represents this fiber to sleep until it receives a resume or exit
     * message.
     * @param fiber
     */
    private static Object[] waitForResume(final DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);

        final FiberMessage message = BasicObjectNodes.getContext(fiber).getThreadManager().runUntilResult(new ThreadManager.BlockingAction<FiberMessage>() {
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
            assert BasicObjectNodes.getContext(fiber).getThreadManager().getCurrentThread() == getFields(resumeMessage.getSendingFiber()).rubyThread;
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
    private static void resume(DynamicObject fromFiber, DynamicObject fiber, boolean yield, Object... args) {
        assert RubyGuards.isRubyFiber(fromFiber);
        assert RubyGuards.isRubyFiber(fiber);

        sendMessageTo(fiber, new FiberResumeMessage(yield, fromFiber, args));
    }

    public static Object[] transferControlTo(DynamicObject fromFiber, DynamicObject fiber, boolean yield, Object[] args) {
        assert RubyGuards.isRubyFiber(fromFiber);
        assert RubyGuards.isRubyFiber(fiber);

        resume(fromFiber, fiber, yield, args);

        return waitForResume(fromFiber);
    }

    public static void shutdown(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        assert !getFields(fiber).isRootFiber;
        sendMessageTo(fiber, new FiberExitMessage());
    }

    public static boolean isAlive(DynamicObject fiber) {
        assert RubyGuards.isRubyFiber(fiber);
        return getFields(fiber).alive;
    }

    public static DynamicObject createRubyFiber(DynamicObject parent, DynamicObject rubyClass, String name) {
        final FiberFields fields = new FiberNodes.FiberFields(parent, false);
        fields.name = name;
        return FIBER_LAYOUT.createFiber(ClassNodes.CLASS_LAYOUT.getInstanceFactory(rubyClass), fields);
    }

    public static DynamicObject createRubyFiber(DynamicObject parent, FiberManager fiberManager, ThreadManager threadManager, DynamicObject rubyClass, String name, boolean isRootFiber) {
        final FiberFields fields = new FiberNodes.FiberFields(parent, isRootFiber);
        fields.name = name;
        return FIBER_LAYOUT.createFiber(ClassNodes.CLASS_LAYOUT.getInstanceFactory(rubyClass), fields);
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

        public abstract Object executeTransferControlTo(VirtualFrame frame, DynamicObject fiber, boolean isYield, Object[] args);

        @Specialization(guards = "isRubyFiber(fiber)")
        protected Object transfer(VirtualFrame frame, DynamicObject fiber, boolean isYield, Object[] args) {
            CompilerDirectives.transferToInterpreter();

            if (!isAlive(fiber)) {
                throw new RaiseException(getContext().getCoreLibrary().deadFiberCalledError(this));
            }

            DynamicObject currentThread = getContext().getThreadManager().getCurrentThread();
            if (getFields(fiber).rubyThread != currentThread) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().fiberError("fiber called across threads", this));
            }

            final DynamicObject sendingFiber = ThreadNodes.getFiberManager(currentThread).getCurrentFiber();

            return singleValue(frame, transferControlTo(sendingFiber, fiber, isYield, args));
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public DynamicObject initialize(DynamicObject fiber, DynamicObject block) {
            CompilerDirectives.transferToInterpreter();

            FiberNodes.initialize(fiber, block);
            return nil();
        }

    }

    @CoreMethod(names = "resume", rest = true)
    public abstract static class ResumeNode extends CoreMethodArrayArgumentsNode {

        @Child FiberTransferNode fiberTransferNode;

        public ResumeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        @Specialization
        public Object resume(VirtualFrame frame, DynamicObject fiberBeingResumed, Object[] args) {
            return fiberTransferNode.executeTransferControlTo(frame, fiberBeingResumed, false, args);
        }

    }

    @CoreMethod(names = "yield", onSingleton = true, rest = true)
    public abstract static class YieldNode extends CoreMethodArrayArgumentsNode {

        @Child FiberTransferNode fiberTransferNode;

        public YieldNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        @Specialization
        public Object yield(VirtualFrame frame, Object[] args) {
            final DynamicObject currentThread = getContext().getThreadManager().getCurrentThread();
            final DynamicObject yieldingFiber = ThreadNodes.getFiberManager(currentThread).getCurrentFiber();
            final DynamicObject fiberYieldedTo = getFields(yieldingFiber).lastResumedByFiber;

            if (getFields(yieldingFiber).isRootFiber || fiberYieldedTo == null) {
                throw new RaiseException(getContext().getCoreLibrary().yieldFromRootFiberError(this));
            }

            return fiberTransferNode.executeTransferControlTo(frame, fiberYieldedTo, true, args);
        }

    }

    public static class FiberResumeMessage implements FiberMessage {

        private final boolean yield;
        private final DynamicObject sendingFiber;
        private final Object[] args;

        public FiberResumeMessage(boolean yield, DynamicObject sendingFiber, Object[] args) {
            assert RubyGuards.isRubyFiber(sendingFiber);
            this.yield = yield;
            this.sendingFiber = sendingFiber;
            this.args = args;
        }

        public boolean isYield() {
            return yield;
        }

        public DynamicObject getSendingFiber() {
            return sendingFiber;
        }

        public Object[] getArgs() {
            return args;
        }

    }

    public static class FiberExitMessage implements FiberMessage {
    }

    public static class FiberExceptionMessage implements FiberMessage {

        private final DynamicObject exception;

        public FiberExceptionMessage(DynamicObject exception) {
            this.exception = exception;
        }

        public DynamicObject getException() {
            return exception;
        }

    }

    public static class FiberExitException extends ControlFlowException {
        private static final long serialVersionUID = 1522270454305076317L;
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            DynamicObject parent = getContext().getThreadManager().getCurrentThread();
            return createRubyFiber(parent, rubyClass, null);
        }

    }

    public static class FiberFields {
        public final DynamicObject rubyThread;
        @CompilerDirectives.CompilationFinal
        public String name;
        public final boolean isRootFiber;
        // we need 2 slots when the FiberManager sends the kill message and there is another message unprocessed
        public final BlockingQueue<FiberNodes.FiberMessage> messageQueue = new LinkedBlockingQueue<>(2);
        public volatile DynamicObject lastResumedByFiber = null;
        public volatile boolean alive = true;
        public volatile Thread thread;

        public FiberFields(DynamicObject rubyThread, boolean isRootFiber) {
            assert RubyGuards.isRubyThread(rubyThread);
            this.rubyThread = rubyThread;
            this.isRootFiber = isRootFiber;
        }
    }

}
