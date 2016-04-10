/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyThread.Status;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.InterruptMode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.RubiniusOnly;
import org.jruby.truffle.core.YieldingCoreMethodNode;
import org.jruby.truffle.core.exception.ExceptionOperations;
import org.jruby.truffle.core.fiber.FiberManager;
import org.jruby.truffle.core.fiber.FiberNodes;
import org.jruby.truffle.core.proc.ProcOperations;
import org.jruby.truffle.core.rubinius.ThreadPrimitiveNodes.ThreadRaisePrimitiveNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SafepointAction;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.ReturnException;
import org.jruby.truffle.language.control.ThreadExitException;
import org.jruby.truffle.platform.UnsafeGroup;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    public static DynamicObject createRubyThread(RubyContext context, DynamicObject rubyClass) {
        final DynamicObject threadLocals = createThreadLocals(context);
        final DynamicObject object = Layouts.THREAD.createThread(
                Layouts.CLASS.getInstanceFactory(rubyClass),
                threadLocals,
                InterruptMode.IMMEDIATE,
                Status.RUN,
                new ArrayList<Lock>(),
                null,
                new CountDownLatch(1),
                getGlobalAbortOnException(context),
                null,
                null,
                null,
                new AtomicBoolean(false),
                0);
        Layouts.THREAD.setFiberManagerUnsafe(object, new FiberManager(context, object)); // Because it is cyclic
        return object;
    }

    public static boolean getGlobalAbortOnException(RubyContext context) {
        final DynamicObject threadClass = context.getCoreLibrary().getThreadClass();
        return (boolean) threadClass.get("@abort_on_exception");
    }

    private static DynamicObject createThreadLocals(RubyContext context) {
        final DynamicObjectFactory instanceFactory = Layouts.CLASS.getInstanceFactory(context.getCoreLibrary().getObjectClass());
        final DynamicObject threadLocals = Layouts.BASIC_OBJECT.createBasicObject(instanceFactory);
        threadLocals.define("$!", context.getCoreLibrary().getNilObject(), 0);
        threadLocals.define("$~", context.getCoreLibrary().getNilObject(), 0);
        threadLocals.define("$?", context.getCoreLibrary().getNilObject(), 0);
        return threadLocals;
    }

    public static void initialize(final DynamicObject thread, RubyContext context, Node currentNode, final Object[] arguments, final DynamicObject block) {
        String info = Layouts.PROC.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        initialize(thread, context, currentNode, info, new Runnable() {
            @Override
            public void run() {
                final Object value = ProcOperations.rootCall(block, arguments);
                Layouts.THREAD.setValue(thread, value);
            }
        });
    }

    public static void initialize(final DynamicObject thread, final RubyContext context, final Node currentNode, final String info, final Runnable task) {
        assert RubyGuards.isRubyThread(thread);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ThreadNodes.run(thread, context, currentNode, info, task);
            }
        }).start();

        FiberNodes.waitForInitialization(context, Layouts.THREAD.getFiberManager(thread).getRootFiber(), currentNode);
    }

    public static void run(DynamicObject thread, final RubyContext context, Node currentNode, String info, Runnable task) {
        assert RubyGuards.isRubyThread(thread);

        final String name = "Ruby Thread@" + info;
        Thread.currentThread().setName(name);
        DynamicObject fiber = Layouts.THREAD.getFiberManager(thread).getRootFiber();

        start(context, thread);
        FiberNodes.start(context, fiber);
        try {
            task.run();
        } catch (ThreadExitException e) {
            Layouts.THREAD.setValue(thread, context.getCoreLibrary().getNilObject());
            return;
        } catch (RaiseException e) {
            setException(context, thread, e.getException(), currentNode);
        } catch (ReturnException e) {
            setException(context, thread, context.getCoreLibrary().unexpectedReturn(currentNode), currentNode);
        } finally {
            FiberNodes.cleanup(context, fiber);
            cleanup(context, thread);
        }
    }

    private static void setException(RubyContext context, DynamicObject thread, DynamicObject exception, Node currentNode) {
        final DynamicObject mainThread = context.getThreadManager().getRootThread();
        final boolean isSystemExit = Layouts.BASIC_OBJECT.getLogicalClass(exception) == context.getCoreLibrary().getSystemExitClass();
        if (thread != mainThread && (isSystemExit || Layouts.THREAD.getAbortOnException(thread))) {
            ThreadRaisePrimitiveNode.raiseInThread(context, mainThread, exception, currentNode);
        }
        Layouts.THREAD.setException(thread, exception);
    }

    public static void start(RubyContext context, DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.setThread(thread, Thread.currentThread());
        context.getThreadManager().registerThread(thread);
    }

    public static void cleanup(RubyContext context, DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);

        Layouts.THREAD.setStatus(thread, Status.ABORTING);
        context.getThreadManager().unregisterThread(thread);

        Layouts.THREAD.setStatus(thread, Status.DEAD);
        Layouts.THREAD.setThread(thread, null);
        assert RubyGuards.isRubyThread(thread);
        for (Lock lock : Layouts.THREAD.getOwnedLocks(thread)) {
            lock.unlock();
        }
        Layouts.THREAD.getFinishedLatch(thread).countDown();
    }

    public static void shutdown(RubyContext context, DynamicObject thread, Node currentNode) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.getFiberManager(thread).shutdown();

        if (thread == context.getThreadManager().getRootThread()) {
            throw new RaiseException(context.getCoreLibrary().systemExit(0, currentNode));
        } else {
            throw new ThreadExitException();
        }
    }

    @CoreMethod(names = "alive?", unsafe = UnsafeGroup.THREADS)
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        public AliveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean alive(DynamicObject thread) {
            final Status status = Layouts.THREAD.getStatus(thread);
            return status != Status.ABORTING && status != Status.DEAD;
        }

    }

    @CoreMethod(names = "backtrace", unsafe = UnsafeGroup.THREADS)
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        public BacktraceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject backtrace(DynamicObject rubyThread) {
            final Thread thread = Layouts.FIBER.getThread(Layouts.THREAD.getFiberManager(rubyThread).getCurrentFiber());

            final DynamicObject[] result = new DynamicObject[1];

            getContext().getSafepointManager().pauseThreadAndExecute(thread, this, new SafepointAction() {
                @Override
                public void run(DynamicObject thread, Node currentNode) {
                    final Backtrace backtrace = getContext().getCallStack().getBacktrace(currentNode);
                    result[0] = ExceptionOperations.backtraceAsRubyStringArray(getContext(), null, backtrace);
                }
            });

            // if the thread id dead or aborting the SafepointAction will not run
            if (result[0] != null) {
                return result[0];
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodArrayArgumentsNode {

        public CurrentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject current() {
            return getContext().getThreadManager().getCurrentThread();
        }

    }

    @CoreMethod(names = { "kill", "exit", "terminate" }, unsafe = UnsafeGroup.THREADS)
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject kill(final DynamicObject rubyThread) {
            final Thread toKill = Layouts.THREAD.getThread(rubyThread);
            if (toKill == null) {
                // Already dead
                return rubyThread;
            }

            getContext().getSafepointManager().pauseThreadAndExecuteLater(toKill, this, new SafepointAction() {
                @Override
                public void run(DynamicObject currentThread, Node currentNode) {
                    shutdown(getContext(), currentThread, currentNode);
                }
            });

            return rubyThread;
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "handle_interrupt", required = 2, needsBlock = true, visibility = Visibility.PRIVATE, unsafe = UnsafeGroup.THREADS)
    public abstract static class HandleInterruptNode extends YieldingCoreMethodNode {

        private final DynamicObject immediateSymbol = getContext().getSymbolTable().getSymbol("immediate");
        private final DynamicObject onBlockingSymbol = getContext().getSymbolTable().getSymbol("on_blocking");
        private final DynamicObject neverSymbol = getContext().getSymbolTable().getSymbol("never");

        public HandleInterruptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = { "isRubyClass(exceptionClass)", "isRubySymbol(timing)" })
        public Object handle_interrupt(VirtualFrame frame, DynamicObject self, DynamicObject exceptionClass, DynamicObject timing, DynamicObject block) {
            // TODO (eregon, 12 July 2015): should we consider exceptionClass?
            final InterruptMode newInterruptMode = symbolToInterruptMode(timing);

            final InterruptMode oldInterruptMode = Layouts.THREAD.getInterruptMode(self);
            Layouts.THREAD.setInterruptMode(self, newInterruptMode);
            try {
                return yield(frame, block);
            } finally {
                Layouts.THREAD.setInterruptMode(self, oldInterruptMode);
            }
        }

        private InterruptMode symbolToInterruptMode(DynamicObject symbol) {
            if (symbol == immediateSymbol) {
                return InterruptMode.IMMEDIATE;
            } else if (symbol == onBlockingSymbol) {
                return InterruptMode.ON_BLOCKING;
            } else if (symbol == neverSymbol) {
                return InterruptMode.NEVER;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentError("invalid timing symbol", this));
            }
        }

    }

    @CoreMethod(names = "initialize", rest = true, needsBlock = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject initialize(DynamicObject thread, Object[] arguments, DynamicObject block) {
            ThreadNodes.initialize(thread, getContext(), this, arguments, block);
            return nil();
        }

    }

    @CoreMethod(names = "join", optional = 1, unsafe = UnsafeGroup.THREADS)
    public abstract static class JoinNode extends CoreMethodArrayArgumentsNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject join(DynamicObject thread, NotProvided timeout) {
            doJoin(this, thread);
            return thread;
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject join(DynamicObject thread, Object nil) {
            return join(thread, NotProvided.INSTANCE);
        }

        @Specialization
        public Object join(DynamicObject thread, int timeout) {
            return joinMillis(thread, timeout * 1000);
        }

        @Specialization
        public Object join(DynamicObject thread, double timeout) {
            return joinMillis(thread, (int) (timeout * 1000.0));
        }

        private Object joinMillis(DynamicObject self, int timeoutInMillis) {
            if (doJoinMillis(self, timeoutInMillis)) {
                return self;
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        public static void doJoin(RubyNode currentNode, final DynamicObject thread) {
            currentNode.getContext().getThreadManager().runUntilResult(currentNode, new ThreadManager.BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    Layouts.THREAD.getFinishedLatch(thread).await();
                    return SUCCESS;
                }
            });

            if (Layouts.THREAD.getException(thread) != null) {
                throw new RaiseException(Layouts.THREAD.getException(thread));
            }
        }

        @TruffleBoundary
        private boolean doJoinMillis(final DynamicObject thread, final int timeoutInMillis) {
            final long start = System.currentTimeMillis();
            final boolean joined = getContext().getThreadManager().runUntilResult(this, new ThreadManager.BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    long now = System.currentTimeMillis();
                    long waited = now - start;
                    if (waited >= timeoutInMillis) {
                        // We need to know whether countDown() was called and we do not want to block.
                        return Layouts.THREAD.getFinishedLatch(thread).getCount() == 0;
                    }
                    return Layouts.THREAD.getFinishedLatch(thread).await(timeoutInMillis - waited, TimeUnit.MILLISECONDS);
                }
            });

            if (joined && Layouts.THREAD.getException(thread) != null) {
                throw new RaiseException(Layouts.THREAD.getException(thread));
            }

            return joined;
        }

    }

    @CoreMethod(names = "main", onSingleton = true)
    public abstract static class MainNode extends CoreMethodArrayArgumentsNode {

        public MainNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject main() {
            return getContext().getThreadManager().getRootThread();
        }

    }

    @CoreMethod(names = "pass", onSingleton = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class PassNode extends CoreMethodArrayArgumentsNode {

        public PassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject pass(VirtualFrame frame) {
            Thread.yield();
            return nil();
        }

    }

    @CoreMethod(names = "status", unsafe = UnsafeGroup.THREADS)
    public abstract static class StatusNode extends CoreMethodArrayArgumentsNode {

        public StatusNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object status(DynamicObject self) {
            // TODO: slightly hackish
            final Status status = Layouts.THREAD.getStatus(self);
            if (status == Status.DEAD) {
                if (Layouts.THREAD.getException(self) != null) {
                    return nil();
                } else {
                    return false;
                }
            }

            return createString(status.bytes);
        }

    }

    @CoreMethod(names = "stop?", unsafe = UnsafeGroup.THREADS)
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        public StopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean stop(DynamicObject self) {
            final Status status = Layouts.THREAD.getStatus(self);
            return status == Status.DEAD || status == Status.SLEEP;
        }

    }

    @CoreMethod(names = "value", unsafe = UnsafeGroup.THREADS)
    public abstract static class ValueNode extends CoreMethodArrayArgumentsNode {

        public ValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object value(DynamicObject self) {
            JoinNode.doJoin(this, self);
            return Layouts.THREAD.getValue(self);
        }

    }

    @CoreMethod(names = { "wakeup", "run" }, unsafe = UnsafeGroup.THREADS)
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        public WakeupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject wakeup(final DynamicObject thread) {
            if (Layouts.THREAD.getStatus(thread) == Status.DEAD) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().threadError("killed thread", this));
            }

            // TODO: should only interrupt sleep
            Layouts.THREAD.getWakeUp(thread).set(true);
            Thread t = Layouts.THREAD.getThread(thread);
            if (t != null) {
                t.interrupt();
            }

            return thread;
        }

    }

    @CoreMethod(names = "abort_on_exception", unsafe = UnsafeGroup.THREADS)
    public abstract static class AbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public AbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean abortOnException(DynamicObject self) {
            return Layouts.THREAD.getAbortOnException(self);
        }

    }

    @CoreMethod(names = "abort_on_exception=", required = 1, unsafe = UnsafeGroup.THREADS)
    public abstract static class SetAbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public SetAbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject setAbortOnException(DynamicObject self, boolean abortOnException) {
            Layouts.THREAD.setAbortOnException(self, abortOnException);
            return nil();
        }

    }

    @CoreMethod(names = "allocate", constructor = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        // TODO (eregon, 13/10/2015): Thread is not allocatable in MRI
        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createRubyThread(getContext(), rubyClass);
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject list() {
            final Object[] threads = getContext().getThreadManager().getThreadList();
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), threads, threads.length);
        }
    }

}
