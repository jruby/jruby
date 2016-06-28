/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.core.thread.ThreadManager;
import org.jruby.truffle.language.RubyNode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Usage:
 * <pre><code>
 *  ReentrantLockFreeingMap<String> fileLocks = new ReentrantLockFreeingMap<String>();
 *  while (true) {
 *      final ReentrantLock lock = fileLocks.getLock(key);
 *
 *      if (!fileLocks.lock(callNode, context, lock)) {
 *          continue;
 *      }
 *
 *      try {
 *          doStuff
 *          return true;
 *      } finally {
 *          fileLocks.unlock(key, lock);
 *      }
 *  }
 * </code></pre>
 */
public class ReentrantLockFreeingMap<K> {

    private final ConcurrentHashMap<K, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLockFreeingMap() {
    }

    @TruffleBoundary
    public ReentrantLock get(K key) {
        final ReentrantLock currentLock = locks.get(key);
        final ReentrantLock lock;

        if (currentLock == null) {
            ReentrantLock newLock = new ReentrantLock();
            final ReentrantLock wasLock = locks.putIfAbsent(key, newLock);
            lock = (wasLock == null) ? newLock : wasLock;
        } else {
            lock = currentLock;
        }
        return lock;
    }

    public boolean lock(RubyNode currentNode, K key, final ReentrantLock lock) {
        return lock(currentNode, currentNode.getContext().getThreadManager(), key, lock);
    }

    @TruffleBoundary
    public boolean lock(
            Node currentNode,
            ThreadManager threadManager,
            K key,
            final ReentrantLock lock) {

        threadManager.runUntilResult(
                currentNode,
                new ThreadManager.BlockingAction<Boolean>() {
                    @Override
                    public Boolean block() throws InterruptedException {
                        lock.lockInterruptibly();
                        return SUCCESS;
                    }
                });
        // ensure that we are not holding removed lock
        if (lock == locks.get(key)) {
            return true;
        } else {
            lock.unlock();
            return false;
        }
    }

    @TruffleBoundary
    public void unlock(K key, ReentrantLock lock) {
        if (!lock.hasQueuedThreads()) {
            // may remove lock after a thread starts waiting, has to be mitigated by checking
            // correctLock after lock is acquired, if not it has to start over
            locks.remove(key);
        }
        lock.unlock();
    }

}
