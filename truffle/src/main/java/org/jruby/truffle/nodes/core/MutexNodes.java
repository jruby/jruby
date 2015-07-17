/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.object.BasicObjectType;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    private static class MutexType extends BasicObjectType {
    }

    public static final MutexType MUTEX_TYPE = new MutexType();

    private static final HiddenKey LOCK_IDENTIFIER = new HiddenKey("lock");
    private static final Property LOCK_PROPERTY;
    private static final DynamicObjectFactory MUTEX_FACTORY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        LOCK_PROPERTY = Property.create(LOCK_IDENTIFIER, allocator.locationForType(ReentrantLock.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
        Shape shape = RubyBasicObject.LAYOUT.createShape(MUTEX_TYPE).addProperty(LOCK_PROPERTY);
        MUTEX_FACTORY = shape.createFactory();
    }

    public static class MutexAllocator implements Allocator {
        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return new RubyBasicObject(rubyClass, MUTEX_FACTORY.newInstance(new ReentrantLock()));
        }
    }

    protected static ReentrantLock getLock(RubyBasicObject mutex) {
        // mutex has the proper shape since Ruby disallow calling Mutex methods on non-Mutex instances.
        assert mutex.getDynamicObject().getShape().hasProperty(LOCK_IDENTIFIER);
        return (ReentrantLock) LOCK_PROPERTY.get(mutex.getDynamicObject(), true);
    }

    @CoreMethod(names = "lock")
    public abstract static class LockNode extends UnaryCoreMethodNode {

        public LockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject lock(RubyBasicObject mutex) {
            final ReentrantLock lock = getLock(mutex);
            final RubyBasicObject thread = getContext().getThreadManager().getCurrentThread();

            lock(lock, thread, this);

            return mutex;
        }

        protected static void lock(final ReentrantLock lock, final RubyBasicObject thread, RubyNode currentNode) {
            assert RubyGuards.isRubyThread(thread);

            final RubyContext context = currentNode.getContext();

            if (lock.isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(context.getCoreLibrary().threadError("deadlock; recursive locking", currentNode));
            }

            context.getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    ThreadNodes.acquiredLock(thread, lock);
                    return SUCCESS;
                }
            });
        }

    }

    @CoreMethod(names = "locked?")
    public abstract static class IsLockedNode extends UnaryCoreMethodNode {

        public IsLockedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isLocked(RubyBasicObject mutex) {
            return getLock(mutex).isLocked();
        }

    }

    @CoreMethod(names = "owned?")
    public abstract static class IsOwnedNode extends UnaryCoreMethodNode {

        public IsOwnedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean isOwned(RubyBasicObject mutex) {
            return getLock(mutex).isHeldByCurrentThread();
        }

    }

    @CoreMethod(names = "try_lock")
    public abstract static class TryLockNode extends UnaryCoreMethodNode {

        public TryLockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean tryLock(RubyBasicObject mutex) {
            final ReentrantLock lock = getLock(mutex);

            if (lock.isHeldByCurrentThread()) {
                return false;
            }

            if (lock.tryLock()) {
                final RubyBasicObject thread = getContext().getThreadManager().getCurrentThread();
                ThreadNodes.acquiredLock(thread, lock);
                return true;
            } else {
                return false;
            }
        }

    }

    @CoreMethod(names = "unlock")
    public abstract static class UnlockNode extends UnaryCoreMethodNode {

        public UnlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject unlock(RubyBasicObject mutex) {
            final ReentrantLock lock = getLock(mutex);
            final RubyBasicObject thread = getContext().getThreadManager().getCurrentThread();

            unlock(lock, thread, this);

            return mutex;
        }

        protected static void unlock(ReentrantLock lock, RubyBasicObject thread, RubyNode currentNode) {
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

            ThreadNodes.releasedLock(thread, lock);
        }

    }

    @CoreMethod(names = "sleep", optional = 1)
    public abstract static class SleepNode extends CoreMethodArrayArgumentsNode {

        public SleepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public long sleep(RubyBasicObject mutex, NotProvided duration) {
            // TODO: this should actually be "forever".
            return doSleepMillis(mutex, Integer.MAX_VALUE);
        }

        @Specialization(guards = "isNil(duration)")
        public long sleep(RubyBasicObject mutex, RubyBasicObject duration) {
            return sleep(mutex, NotProvided.INSTANCE);
        }

        @Specialization
        public long sleep(RubyBasicObject mutex, long duration) {
            return doSleepMillis(mutex, duration * 1000);
        }

        @Specialization
        public long sleep(RubyBasicObject mutex, double duration) {
            return doSleepMillis(mutex, (long) (duration * 1000.0));
        }

        public long doSleepMillis(RubyBasicObject mutex, long durationInMillis) {
            if (durationInMillis < 0) {
                throw new RaiseException(getContext().getCoreLibrary().argumentError("time interval must be positive", this));
            }

            final ReentrantLock lock = getLock(mutex);
            final RubyBasicObject thread = getContext().getThreadManager().getCurrentThread();

            // Clear the wakeUp flag, following Ruby semantics:
            // it should only be considered if we are inside the sleep when Thread#{run,wakeup} is called.
            // Here we do it before unlocking for providing nice semantics for
            // thread1: mutex.sleep
            // thread2: mutex.synchronize { <ensured that thread1 is sleeping and thread1.wakeup will wake it up> }
            ThreadNodes.shouldWakeUp(thread);

            UnlockNode.unlock(lock, thread, this);
            try {
                return KernelNodes.SleepNode.sleepFor(getContext(), durationInMillis);
            } finally {
                LockNode.lock(lock, thread, this);
            }
        }

    }

}
