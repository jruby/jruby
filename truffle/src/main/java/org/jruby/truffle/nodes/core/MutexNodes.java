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

import java.util.concurrent.locks.ReentrantLock;

import com.oracle.truffle.api.object.HiddenKey;
import org.jruby.truffle.nodes.objectstorage.ReadHeadObjectFieldNode;
import org.jruby.truffle.nodes.objectstorage.WriteHeadObjectFieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.subsystems.ThreadManager.BlockingActionWithoutGlobalLock;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

@CoreClass(name = "Mutex")
public abstract class MutexNodes {

    private static final HiddenKey LOCK_IDENTIFIER = new HiddenKey("lock");

    @CoreMethod(names = "initialize")
    public abstract static class InitializeNode extends UnaryCoreMethodNode {

        @Child private WriteHeadObjectFieldNode writeLock;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            writeLock = new WriteHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
            writeLock = prev.writeLock;
        }

        @Specialization
        public RubyBasicObject lock(RubyBasicObject mutex) {
            writeLock.execute(mutex, new ReentrantLock());
            return mutex;
        }

    }

    @CoreMethod(names = "lock")
    public abstract static class LockNode extends UnaryCoreMethodNode {

        @Child private ReadHeadObjectFieldNode readLock;

        public LockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readLock = new ReadHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public LockNode(LockNode prev) {
            super(prev);
            readLock = prev.readLock;
        }

        @Specialization
        public RubyBasicObject lock(RubyBasicObject mutex) {
            final ReentrantLock lock = (ReentrantLock) readLock.execute(mutex);

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

        @Child private ReadHeadObjectFieldNode readLock;

        public IsLockedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readLock = new ReadHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public IsLockedNode(IsLockedNode prev) {
            super(prev);
            readLock = prev.readLock;
        }

        @Specialization
        public boolean isLocked(RubyBasicObject mutex) {
            final ReentrantLock lock = (ReentrantLock) readLock.execute(mutex);
            return lock.isLocked();
        }

    }

    @CoreMethod(names = "owned?")
    public abstract static class IsOwnedNode extends UnaryCoreMethodNode {

        @Child private ReadHeadObjectFieldNode readLock;

        public IsOwnedNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readLock = new ReadHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public IsOwnedNode(IsOwnedNode prev) {
            super(prev);
            readLock = prev.readLock;
        }

        @Specialization
        public boolean isOwned(RubyBasicObject mutex) {
            final ReentrantLock lock = (ReentrantLock) readLock.execute(mutex);
            return lock.isHeldByCurrentThread();
        }

    }

    @CoreMethod(names = "try_lock")
    public abstract static class TryLockNode extends UnaryCoreMethodNode {

        @Child private ReadHeadObjectFieldNode readLock;

        public TryLockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readLock = new ReadHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public TryLockNode(TryLockNode prev) {
            super(prev);
            readLock = prev.readLock;
        }

        @Specialization
        public boolean tryLock(RubyBasicObject mutex) {
            final ReentrantLock lock = (ReentrantLock) readLock.execute(mutex);

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

        @Child private ReadHeadObjectFieldNode readLock;

        public UnlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readLock = new ReadHeadObjectFieldNode(LOCK_IDENTIFIER);
        }

        public UnlockNode(UnlockNode prev) {
            super(prev);
            readLock = prev.readLock;
        }

        @Specialization
        public RubyBasicObject unlock(RubyBasicObject mutex) {
            final ReentrantLock lock = (ReentrantLock) readLock.execute(mutex);

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
