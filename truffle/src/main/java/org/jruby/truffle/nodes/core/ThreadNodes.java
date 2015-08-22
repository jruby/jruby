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
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.subsystems.FiberManager;
import org.jruby.truffle.runtime.subsystems.SafepointAction;
import org.jruby.truffle.runtime.subsystems.ThreadManager;
import org.jruby.util.StringSupport;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    public static DynamicObject createRubyThread(DynamicObject rubyClass) {
        final DynamicObject objectClass = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getLogicalClass(rubyClass)).getContext().getCoreLibrary().getObjectClass();
        final DynamicObject object = Layouts.THREAD.createThread(Layouts.CLASS.getInstanceFactory(rubyClass), null, null, new CountDownLatch(1), Layouts.BASIC_OBJECT.createBasicObject(Layouts.CLASS.getInstanceFactory(objectClass)), new ArrayList<Lock>(), false,InterruptMode.IMMEDIATE, null, Status.RUN, null, null, new AtomicBoolean(false), 0);
        Layouts.THREAD.setFiberManagerUnsafe(object, new FiberManager(object));
        return object;
    }

    public static void initialize(final DynamicObject thread, RubyContext context, Node currentNode, final Object[] arguments, final DynamicObject block) {
        assert RubyGuards.isRubyThread(thread);
        assert RubyGuards.isRubyProc(block);
        String info = Layouts.PROC.getSharedMethodInfo(block).getSourceSection().getShortDescription();
        initialize(thread, context, currentNode, info, new Runnable() {
            @Override
            public void run() {
                Layouts.THREAD.setValue(thread, ProcNodes.rootCall(block, arguments));
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

        final String name = "Ruby Thread@" + info;
        Layouts.THREAD.setName(thread, name);
        Thread.currentThread().setName(name);

        start(thread);
        try {
            DynamicObject fiber = Layouts.THREAD.getFiberManager(thread).getRootFiber();
            FiberNodes.run(fiber, task);
        } catch (ThreadExitException e) {
            Layouts.THREAD.setValue(thread, context.getCoreLibrary().getNilObject());
            return;
        } catch (RaiseException e) {
            Layouts.THREAD.setException(thread, e.getRubyException());
        } catch (ReturnException e) {
            Layouts.THREAD.setException(thread, context.getCoreLibrary().unexpectedReturn(currentNode));
        } finally {
            cleanup(thread);
        }
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void start(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.setThread(thread, Thread.currentThread());
        Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getMetaClass(thread)).getContext().getThreadManager().registerThread(thread);
    }

    // Only used by the main thread which cannot easily wrap everything inside a try/finally.
    public static void cleanup(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);

        Layouts.THREAD.setStatus(thread, Status.ABORTING);
        Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getMetaClass(thread)).getContext().getThreadManager().unregisterThread(thread);

        Layouts.THREAD.setStatus(thread, Status.DEAD);
        Layouts.THREAD.setThread(thread, null);
        assert RubyGuards.isRubyThread(thread);
        for (Lock lock : Layouts.THREAD.getOwnedLocks(thread)) {
            lock.unlock();
        }
        Layouts.THREAD.getFinishedLatch(thread).countDown();
    }

    public static void shutdown(DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.THREAD.getFiberManager(thread).shutdown();
        throw new ThreadExitException();
    }

    public static void join(final DynamicObject thread) {
        assert RubyGuards.isRubyThread(thread);
        Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getMetaClass(thread)).getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingAction<Boolean>() {
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

    public static boolean join(final DynamicObject thread, final int timeoutInMillis) {
        assert RubyGuards.isRubyThread(thread);
        final long start = System.currentTimeMillis();
        final boolean joined = Layouts.MODULE.getFields(Layouts.BASIC_OBJECT.getMetaClass(thread)).getContext().getThreadManager().runUntilResult(new ThreadManager.BlockingAction<Boolean>() {
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

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        public AliveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean alive(DynamicObject thread) {
            return Layouts.THREAD.getStatus(thread) != Status.ABORTING && Layouts.THREAD.getStatus(thread) != Status.DEAD;
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
            final Thread toKill = Layouts.THREAD.getThread(rubyThread);

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
            if (Layouts.THREAD.getStatus(self) == Status.DEAD) {
                if (Layouts.THREAD.getException(self) != null) {
                    return nil();
                } else {
                    return false;
                }
            }

            return Layouts.STRING.createString(getContext().getCoreLibrary().getStringFactory(), Layouts.THREAD.getStatus(self).bytes, StringSupport.CR_UNKNOWN, null);
        }

    }

    @CoreMethod(names = "stop?")
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        public StopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean stop(DynamicObject self) {
            return Layouts.THREAD.getStatus(self) == Status.DEAD || Layouts.THREAD.getStatus(self) == Status.SLEEP;
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
            return Layouts.THREAD.getValue(self);
        }

    }

    @CoreMethod(names = { "wakeup", "run" })
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        public WakeupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject wakeup(final DynamicObject thread) {
            if (Layouts.THREAD.getStatus(thread) == Status.DEAD) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("killed thread", this));
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

    @CoreMethod(names = "abort_on_exception")
    public abstract static class AbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public AbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean abortOnException(DynamicObject self) {
            return Layouts.THREAD.getAbortOnException(self);
        }

    }

    @CoreMethod(names = "abort_on_exception=", required = 1)
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

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return createRubyThread(rubyClass);
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
            return Layouts.ARRAY.createArray(getContext().getCoreLibrary().getArrayFactory(), threads, threads.length);
        }
    }

}
