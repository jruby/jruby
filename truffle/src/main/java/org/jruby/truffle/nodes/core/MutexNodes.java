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

import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.LocationModifier;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;

import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    private static final HiddenKey LOCK_IDENTIFIER = new HiddenKey("lock");
    private static final Property LOCK_PROPERTY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        LOCK_PROPERTY = Property.create(LOCK_IDENTIFIER, allocator.locationForType(ReentrantLock.class, EnumSet.of(LocationModifier.Final, LocationModifier.NonNull)), 0);
    }

    public static Allocator createMutexAllocator(Shape emptyShape) {
        Shape shape = emptyShape.addProperty(LOCK_PROPERTY);
        final DynamicObjectFactory factory = shape.createFactory();

        return new Allocator() {
            @Override
            public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
                return new RubyBasicObject(rubyClass, factory.newInstance(new ReentrantLock()));
            }
        };
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

            if (lock.isHeldByCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("deadlock; recursive locking", this));
            }

            final RubyThread thread = getContext().getThreadManager().getCurrentThread();

            getContext().getThreadManager().runUntilResult(new BlockingActionWithoutGlobalLock<Boolean>() {
                @Override
                public Boolean block() throws InterruptedException {
                    lock.lockInterruptibly();
                    thread.acquiredLock(lock);
                    return SUCCESS;
                }
            });

            return mutex;
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
                RubyThread thread = getContext().getThreadManager().getCurrentThread();
                thread.acquiredLock(lock);
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

            final RubyThread thread = getContext().getThreadManager().getCurrentThread();

            try {
                lock.unlock();
            } catch (IllegalMonitorStateException e) {
                if (!lock.isLocked()) {
                    throw new RaiseException(getContext().getCoreLibrary().threadError("Attempt to unlock a mutex which is not locked", this));
                } else {
                    throw new RaiseException(getContext().getCoreLibrary().threadError("Attempt to unlock a mutex which is locked by another thread", this));
                }
            }

            thread.releasedLock(lock);

            return mutex;
        }

    }

}
