/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.mutex;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.core.thread.ThreadManager.BlockingAction;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    @CoreMethod(unsafeNeedsAudit = true, names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode;

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            allocateNode = AllocateObjectNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, new ReentrantLock());
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "lock")
    public abstract static class LockNode extends UnaryCoreMethodNode {

        public LockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject lock(DynamicObject mutex) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            lock(lock, thread, this);

            return mutex;
        }

        @TruffleBoundary
        protected static void lock(final ReentrantLock lock, final DynamicObject thread, RubyNode currentNode) {
            assert RubyGuards.isRubyThread(thread);

            final RubyContext context = currentNode.getContext();

            if (lock.isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(context.getCoreLibrary().threadError("deadlock; recursive locking", currentNode));
            }

            context.getThreadManager().runUntilResult(currentNode, new BlockingAction<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    Layouts.THREAD.getOwnedLocks(thread).add(lock);
                    return SUCCESS;
                }
            });
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "locked?")
    public abstract static class IsLockedNode extends UnaryCoreMethodNode {

        public IsLockedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isLocked(DynamicObject mutex) {
            return Layouts.MUTEX.getLock(mutex).isLocked();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "owned?")
    public abstract static class IsOwnedNode extends UnaryCoreMethodNode {

        public IsOwnedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isOwned(DynamicObject mutex) {
            return Layouts.MUTEX.getLock(mutex).isHeldByCurrentThread();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "try_lock")
    public abstract static class TryLockNode extends UnaryCoreMethodNode {

        public TryLockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean tryLock(DynamicObject mutex) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);

            if (lock.isHeldByCurrentThread()) {
                return false;
            }

            return doTryLock(lock);
        }

        @TruffleBoundary
        private boolean doTryLock(final ReentrantLock lock) {
            if (lock.tryLock()) {
                final DynamicObject thread = getContext().getThreadManager().getCurrentThread();
                Layouts.THREAD.getOwnedLocks(thread).add(lock);
                return true;
            } else {
                return false;
            }
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "unlock")
    public abstract static class UnlockNode extends UnaryCoreMethodNode {

        public UnlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject unlock(DynamicObject mutex) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            unlock(lock, thread, this);

            return mutex;
        }

        @TruffleBoundary
        protected static void unlock(ReentrantLock lock, DynamicObject thread, RubyNode currentNode) {
            assert RubyGuards.isRubyThread(thread);

            final RubyContext context = currentNode.getContext();

            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                if (!lock.isLocked()) {
                    throw new RaiseException(context.getCoreLibrary().threadError("Attempt to unlock a mutex which is not locked", currentNode));
                } else {
                    throw new RaiseException(context.getCoreLibrary().threadError("Attempt to unlock a mutex which is locked by another thread", currentNode));
                }
            }

            Layouts.THREAD.getOwnedLocks(thread).remove(lock);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "sleep", optional = 1)
    public abstract static class SleepNode extends CoreMethodArrayArgumentsNode {

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long sleep(DynamicObject mutex, NotProvided duration) {
            return doSleepMillis(mutex, Long.MAX_VALUE);
        }

        @Specialization(guards = "isNil(duration)")
        public long sleep(DynamicObject mutex, DynamicObject duration) {
            return sleep(mutex, NotProvided.INSTANCE);
        }

        @Specialization
        public long sleep(DynamicObject mutex, long duration) {
            return doSleepMillis(mutex, duration * 1000);
        }

        @Specialization
        public long sleep(DynamicObject mutex, double duration) {
            return doSleepMillis(mutex, (long) (duration * 1000.0));
        }

        public long doSleepMillis(DynamicObject mutex, long durationInMillis) {
            if (durationInMillis < 0) {
                throw new RaiseException(coreLibrary().argumentError("time interval must be positive", this));
            }

            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            // Clear the wakeUp flag, following Ruby semantics:
            // it should only be considered if we are inside the sleep when Thread#{run,wakeup} is called.
            // Here we do it before unlocking for providing nice semantics for
            // thread1: mutex.sleep
            // thread2: mutex.synchronize { <ensured that thread1 is sleeping and thread1.wakeup will wake it up> }
            Layouts.THREAD.getWakeUp(thread).set(false);

            UnlockNode.unlock(lock, thread, this);
            try {
                return KernelNodes.SleepNode.sleepFor(this, getContext(), durationInMillis);
            } finally {
                LockNode.lock(lock, thread, this);
            }
        }

    }

}
