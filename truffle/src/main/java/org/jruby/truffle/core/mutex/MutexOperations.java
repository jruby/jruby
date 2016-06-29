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
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

import java.util.concurrent.locks.ReentrantLock;

public abstract class MutexOperations {

    @TruffleBoundary
    protected static void lock(final ReentrantLock lock, final DynamicObject thread, RubyNode currentNode) {
        final RubyContext context = currentNode.getContext();

        if (lock.isHeldByCurrentThread()) {
            throw new RaiseException(context.getCoreExceptions().threadErrorRecursiveLocking(currentNode));
        }

        context.getThreadManager().runUntilResult(currentNode, new ThreadManager.BlockingAction<Boolean>() {

            @Override
            public Boolean block() throws InterruptedException {
                lock.lockInterruptibly();
                Layouts.THREAD.getOwnedLocks(thread).add(lock);
                return SUCCESS;
            }

        });
    }

    @TruffleBoundary
    protected static void unlock(ReentrantLock lock, DynamicObject thread, RubyNode currentNode) {
        final RubyContext context = currentNode.getContext();

        try {
            lock.unlock();
        } catch (IllegalMonitorStateException e) {
            if (!lock.isLocked()) {
                throw new RaiseException(context.getCoreExceptions().threadErrorUnlockNotLocked(currentNode));
            } else {
                throw new RaiseException(context.getCoreExceptions().threadErrorAlreadyLocked(currentNode));
            }
        }

        Layouts.THREAD.getOwnedLocks(thread).remove(lock);
    }

}
