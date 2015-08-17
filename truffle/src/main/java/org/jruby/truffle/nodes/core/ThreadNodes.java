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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyThread.Status;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ReturnException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.layouts.ThreadLayout;
import org.jruby.truffle.runtime.layouts.ThreadLayoutImpl;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.SafepointAction;
import org.jruby.truffle.runtime.subsystems.ThreadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    public static final ThreadLayout THREAD_LAYOUT = ThreadLayoutImpl.INSTANCE;

    public static DynamicObject createRubyThread(DynamicObject rubyClass, ThreadManager manager) {
        final DynamicObject objectClass = BasicObjectNodes.getContext(rubyClass).getCoreLibrary().getObjectClass();
        final ThreadFields fields = new ThreadNodes.ThreadFields(manager, null, ClassNodes.CLASS_LAYOUT.getInstanceFactory(objectClass).newInstance());
        final DynamicObject object = THREAD_LAYOUT.createThread(ClassNodes.CLASS_LAYOUT.getInstanceFactory(rubyClass), fields);
        fields.fiberManager = new FiberManager(object, manager);
        return object;
    }

    public static void initialize(final DynamicObject thread, RubyContext context, Node currentNode, final Object[] arguments, final DynamicObject block) {
        assert RubyGuards.isRubyThread(thread);
        assert RubyGuards.isRubyProc(block);
        String info = ProcNodes.PROC_LAYOUT.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        initialize(thread, context, currentNode, info, new Runnable() {
            @Override
            public void run() {
                THREAD_LAYOUT.getFields(thread).value = ProcNodes.rootCall(block, arguments);
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
    }

    public static void run(DynamicObject thread, final RubyContext context, Node currentNode, String info, Runnable task) {
        assert RubyGuards.isRubyThread(thread);

        THREAD_LAYOUT.getFields(thread).name = "Ruby Thread@" + info;
        Thread.currentThread().setName(THREAD_LAYOUT.getFields(thread).name);

        start(thread);
        try {
            DynamicObject fiber = getRootFiber(thread);
            FiberNodes.run(fiber, task);
        } catch (ThreadExitException e) {
            THREAD_LAYOUT.getFields(thread).value = context.getCoreLibrary().getNilObject();
            return;
        } catch (RaiseException e) {
            THREAD_LAYOUT.getFields(thread).exception = e.getRubyException();
        } catch (ReturnException e) {
            THREAD_LAYOUT.getFields(thread).exception = context.getCoreLibrary().unexpectedReturn(currentNode);
        } finally {
            cleanup(thread);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void start(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).thread = Thread.currentThread();
        THREAD_LAYOUT.getFields(thread).manager.registerThread(thread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void cleanup(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);

        THREAD_LAYOUT.getFields(thread).status = Status.ABORTING;
        THREAD_LAYOUT.getFields(thread).manager.unregisterThread(thread);

        THREAD_LAYOUT.getFields(thread).status = Status.DEAD;
        THREAD_LAYOUT.getFields(thread).thread = null;
        releaseOwnedLocks(thread);
        THREAD_LAYOUT.getFields(thread).finished.countDown();
    }

    public static void shutdown(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).fiberManager.shutdown();
        throw new ThreadExitException();
    }

    public static Thread getRootFiberJavaThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).thread;
    }

    public static Thread getCurrentFiberJavaThread(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return FiberNodes.FIBER_LAYOUT.getFields((THREAD_LAYOUT.getFields(thread).fiberManager.getCurrentFiber())).thread;
    }

    public static void join(final DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(((DynamicObject) thread)).manager.runUntilResult(new ThreadManager.BlockingAction<Boolean>() {
            @Override
            public Boolean block() throws InterruptedException {
                THREAD_LAYOUT.getFields(thread).finished.await();
                return SUCCESS;
            }
        });

        if (THREAD_LAYOUT.getFields(thread).exception != null) {
            throw new RaiseException(THREAD_LAYOUT.getFields(thread).exception);
        }
    }

    public static boolean join(final DynamicObject thread, final int timeoutInMillis) {
        assert RubyGuards.isRubyThread(thread);
        final long start = System.currentTimeMillis();
        final boolean joined = THREAD_LAYOUT.getFields(((DynamicObject) thread)).manager.runUntilResult(new ThreadManager.BlockingAction<Boolean>() {
            @Override
            public Boolean block() throws InterruptedException {
                long now = System.currentTimeMillis();
                long waited = now - start;
                if (waited >= timeoutInMillis) {
                    // We need to know whether countDown() was called and we do not want to block.
                    return THREAD_LAYOUT.getFields(thread).finished.getCount() == 0;
                }
                return THREAD_LAYOUT.getFields(thread).finished.await(timeoutInMillis - waited, TimeUnit.MILLISECONDS);
            }
        });

        if (joined && THREAD_LAYOUT.getFields(thread).exception != null) {
            throw new RaiseException(THREAD_LAYOUT.getFields(thread).exception);
        }

        return joined;
    }

    public static void wakeup(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).wakeUp.set(true);
        Thread t = THREAD_LAYOUT.getFields(thread).thread;
        if (t != null) {
            t.interrupt();
        }
    }

    public static Object getValue(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).value;
    }

    public static Object getException(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).exception;
    }

    public static String getName(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).name;
    }

    public static void setName(DynamicObject thread, String name) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).name = name;
    }

    public static ThreadManager getThreadManager(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).manager;
    }

    public static FiberManager getFiberManager(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).fiberManager;
    }

    public static DynamicObject getRootFiber(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).fiberManager.getRootFiber();
    }

    public static boolean isAbortOnException(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).abortOnException;
    }

    public static void setAbortOnException(DynamicObject thread, boolean abortOnException) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).abortOnException = abortOnException;
    }

    public static InterruptMode getInterruptMode(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).interruptMode;
    }

    public static void setInterruptMode(DynamicObject thread, InterruptMode interruptMode) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).interruptMode = interruptMode;
    }

    /** Return whether Thread#{run,wakeup} was called and clears the wakeup flag.
     * @param thread*/
    public static boolean shouldWakeUp(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).wakeUp.getAndSet(false);
    }

    public static void acquiredLock(DynamicObject thread, Lock lock) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).ownedLocks.add(lock);
    }

    public static void releasedLock(DynamicObject thread, Lock lock) {
        assert RubyGuards.isRubyThread(thread);
        // TODO: this is O(ownedLocks.length).
        THREAD_LAYOUT.getFields(thread).ownedLocks.remove(lock);
    }

    public static void releaseOwnedLocks(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        for (Lock lock : THREAD_LAYOUT.getFields(thread).ownedLocks) {
            lock.unlock();
        }
    }

    public static Status getStatus(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).status;
    }

    public static void setStatus(DynamicObject thread, Status status) {
        assert RubyGuards.isRubyThread(thread);
        THREAD_LAYOUT.getFields(thread).status = status;
    }

    public static DynamicObject getThreadLocals(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        return THREAD_LAYOUT.getFields(thread).threadLocals;
    }

    public enum InterruptMode {
        IMMEDIATE, ON_BLOCKING, NEVER
    }

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        public AliveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean alive(DynamicObject thread) {
            return getStatus(thread) != Status.ABORTING && getStatus(thread) != Status.DEAD;
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

    @CoreMethod(names = { "kill", "exit", "terminate" })
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject kill(final DynamicObject rubyThread) {
            final Thread toKill = getRootFiberJavaThread(rubyThread);

            getContext().getSafepointManager().pauseThreadAndExecuteLater(toKill, this, new SafepointAction() {
                @Override
                public void run(DynamicObject currentThread, Node currentNode) {
                    shutdown(currentThread);
                }
            });

            return rubyThread;
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "handle_interrupt", required = 2, needsBlock = true, visibility = Visibility.PRIVATE)
    public abstract static class HandleInterruptNode extends YieldingCoreMethodNode {

        private final DynamicObject immediateSymbol = getContext().getSymbol("immediate");
        private final DynamicObject onBlockingSymbol = getContext().getSymbol("on_blocking");
        private final DynamicObject neverSymbol = getContext().getSymbol("never");

        public HandleInterruptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubyClass(exceptionClass)", "isRubySymbol(timing)", "isRubyProc(block)"})
        public Object handle_interrupt(VirtualFrame frame, DynamicObject self, DynamicObject exceptionClass, DynamicObject timing, DynamicObject block) {
            // TODO (eregon, 12 July 2015): should we consider exceptionClass?
            final InterruptMode newInterruptMode = symbolToInterruptMode(timing);

            final InterruptMode oldInterruptMode = getInterruptMode(self);
            setInterruptMode(self, newInterruptMode);
            try {
                return yield(frame, block);
            } finally {
                setInterruptMode(self, oldInterruptMode);
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
                throw new RaiseException(getContext().getCoreLibrary().argumentError("invalid timing symbol", this));
            }
        }

    }

    @CoreMethod(names = "initialize", rest = true, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyProc(block)")
        public DynamicObject initialize(DynamicObject thread, Object[] arguments, DynamicObject block) {
            ThreadNodes.initialize(thread, getContext(), this, arguments, block);
            return nil();
        }

    }

    @CoreMethod(names = "join", optional = 1)
    public abstract static class JoinNode extends CoreMethodArrayArgumentsNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject join(DynamicObject thread, NotProvided timeout) {
            ThreadNodes.join(thread);
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
            assert RubyGuards.isRubyThread(self);

            if (ThreadNodes.join(self, timeoutInMillis)) {
                return self;
            } else {
                return nil();
            }
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

    @CoreMethod(names = "pass", onSingleton = true)
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

    @CoreMethod(names = "status")
    public abstract static class StatusNode extends CoreMethodArrayArgumentsNode {

        public StatusNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object status(DynamicObject self) {
            // TODO: slightly hackish
            if (getStatus(self) == Status.DEAD) {
                if (getException(self) != null) {
                    return nil();
                } else {
                    return false;
                }
            }

            return createString(getStatus(self).bytes);
        }

    }

    @CoreMethod(names = "stop?")
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        public StopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean stop(DynamicObject self) {
            return getStatus(self) == Status.DEAD || getStatus(self) == Status.SLEEP;
        }

    }

    @CoreMethod(names = "value")
    public abstract static class ValueNode extends CoreMethodArrayArgumentsNode {

        public ValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object value(DynamicObject self) {
            join(self);
            return getValue(self);
        }

    }

    @CoreMethod(names = { "wakeup", "run" })
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        public WakeupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject wakeup(final DynamicObject thread) {
            if (getStatus(thread) == Status.DEAD) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("killed thread", this));
            }

            // TODO: should only interrupt sleep
            ThreadNodes.wakeup(thread);

            return thread;
        }

    }

    @CoreMethod(names = "abort_on_exception")
    public abstract static class AbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public AbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean abortOnException(DynamicObject self) {
            return isAbortOnException(self);
        }

    }

    @CoreMethod(names = "abort_on_exception=", required = 1)
    public abstract static class SetAbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public SetAbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject setAbortOnException(DynamicObject self, boolean abortOnException) {
            ThreadNodes.setAbortOnException(self, abortOnException);
            return nil();
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createRubyThread(rubyClass, getContext().getThreadManager());
        }

    }

    public static class ThreadFields {
        public final ThreadManager manager;

        public FiberManager fiberManager;

        public String name;

        /**
         * We use this instead of {@link Thread#join()} since we don't always have a reference
         * to the {@link Thread} and we want to handle cases where the Thread did not start yet.
         */
        public final CountDownLatch finished = new CountDownLatch(1);

        public volatile Thread thread;
        public volatile Status status = Status.RUN;
        public volatile AtomicBoolean wakeUp = new AtomicBoolean(false);

        public volatile Object exception;
        public volatile Object value;

        public final DynamicObject threadLocals;

        public final List<Lock> ownedLocks = new ArrayList<>(); // Always accessed by the same underlying Java thread.

        public boolean abortOnException = false;
        public volatile int priority = 0;

        public ThreadNodes.InterruptMode interruptMode = ThreadNodes.InterruptMode.IMMEDIATE;

        public ThreadFields(ThreadManager manager, FiberManager fiberManager, DynamicObject threadLocals) {
            this.manager = manager;
            this.fiberManager = fiberManager;
            this.threadLocals = threadLocals;
        }
    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        public ListNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject list() {
            final DynamicObject[] threads = getContext().getThreadManager().getThreads();
            return createArray(threads, threads.length);
        }
    }

}
