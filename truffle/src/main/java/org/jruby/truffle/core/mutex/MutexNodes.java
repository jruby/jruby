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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.kernel.KernelNodes;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    @CoreMethod(names = "allocate", constructor = true)
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

    @CoreMethod(names = "lock")
    public abstract static class LockNode extends UnaryCoreMethodNode {

        @Specialization
        public DynamicObject lock(DynamicObject mutex) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();
            MutexOperations.lock(lock, thread, this);
            return mutex;
        }

    }

    @CoreMethod(names = "locked?")
    public abstract static class IsLockedNode extends UnaryCoreMethodNode {

        @Specialization
        public boolean isLocked(DynamicObject mutex) {
            return Layouts.MUTEX.getLock(mutex).isLocked();
        }

    }

    @CoreMethod(names = "owned?")
    public abstract static class IsOwnedNode extends UnaryCoreMethodNode {

        @Specialization
        public boolean isOwned(DynamicObject mutex) {
            return Layouts.MUTEX.getLock(mutex).isHeldByCurrentThread();
        }

    }

    @CoreMethod(names = "try_lock")
    public abstract static class TryLockNode extends UnaryCoreMethodNode {

        @Specialization
        public boolean tryLock(
                DynamicObject mutex,
                @Cached("createBinaryProfile()") ConditionProfile heldByCurrentThreadProfile) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);

            if (heldByCurrentThreadProfile.profile(lock.isHeldByCurrentThread())) {
                return false;
            } else {
                return doTryLock(lock);
            }
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

    @CoreMethod(names = "unlock")
    public abstract static class UnlockNode extends UnaryCoreMethodNode {

        @Specialization
        public DynamicObject unlock(DynamicObject mutex) {
            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();
            MutexOperations.unlock(lock, thread, this);
            return mutex;
        }

    }

    @CoreMethod(names = "sleep", optional = 1)
    public abstract static class SleepNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile durationLessThanZeroProfile = ConditionProfile.createBinaryProfile();

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
            if (durationLessThanZeroProfile.profile(durationInMillis < 0)) {
                throw new RaiseException(coreExceptions().argumentErrorTimeItervalPositive(this));
            }

            final ReentrantLock lock = Layouts.MUTEX.getLock(mutex);
            final DynamicObject thread = getContext().getThreadManager().getCurrentThread();

            /*
             * Clear the wakeUp flag, following Ruby semantics:
             * it should only be considered if we are inside the sleep when Thread#{run,wakeup} is called.
             * Here we do it before unlocking for providing nice semantics for
             * thread1: mutex.sleep
             * thread2: mutex.synchronize { <ensured that thread1 is sleeping and thread1.wakeup will wake it up> }
             */

            Layouts.THREAD.getWakeUp(thread).set(false);

            MutexOperations.unlock(lock, thread, this);

            try {
                return KernelNodes.SleepNode.sleepFor(this, getContext(), durationInMillis);
            } finally {
                MutexOperations.lock(lock, thread, this);
            }
        }

    }

}
