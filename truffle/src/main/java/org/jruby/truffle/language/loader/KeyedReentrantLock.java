package org.jruby.truffle.language.loader;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.thread.ThreadManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Usage:
 * <pre><code>
 *  KeyedReentrantLock<String> fileLocks = new KeyedReentrantLock<String>();
 *  while (true) {
 *      final ReentrantLock lock = fileLocks.getLock(key);
 *      fileLocks.lock(callNode, context, lock);
 *      // Check that the lock is still correct, otherwise start over, required!
 *      if (!fileLocks.correctLock(key, lock)) {
 *          continue;
 *      }
 *      try {
 *          doStuff
 *          return true;
 *      } finally {
 *          fileLocks.unlock(key, lock);
 *      }
 *  }
 * </code></pre>
 */
public class KeyedReentrantLock<K> {

    private final ConcurrentHashMap<K, ReentrantLock> locks = new ConcurrentHashMap<>();

    public KeyedReentrantLock() {
    }

    @TruffleBoundary
    public ReentrantLock getLock(K key) {
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

    @TruffleBoundary
    public boolean ensureCorrectLock(K key, ReentrantLock lock) {
        if (lock == locks.get(key)) {
            return true;
        } else {
            lock.unlock();
            return false;
        }
    }

    @TruffleBoundary
    public void lock(Node currentNode, RubyContext context, final ReentrantLock lock) {
        context.getThreadManager().runUntilResult(
                currentNode,
                new ThreadManager.BlockingAction<Boolean>() {
                    @Override
                    public Boolean block() throws InterruptedException {
                        lock.lockInterruptibly();
                        return SUCCESS;
                    }
                });
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
