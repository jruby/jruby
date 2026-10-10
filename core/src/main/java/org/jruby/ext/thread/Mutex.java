/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 MenTaLguY <mental@rydia.net>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.thread;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.jruby.FiberScheduler;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFloat;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;

/**
 * The "Mutex" class from the 'thread' library.
 */
@JRubyClass(name = "Mutex")
public class Mutex extends RubyObject implements DataType {
    final ReentrantLock lock = new MutexLock();
    /**
     * The non-fiber thread that currently holds the lock; this will be the same for all fibers associated with that
     * thread.
     */
    volatile RubyThread lockingThread;
    /** The fiber within {@link #lockingThread} that currently holds the lock. */
    volatile IRubyObject lockingFiber;

    /** A fiber waiting through a fiber scheduler, on a Mutex or a ConditionVariable. MRI: sync_waiter */
    record FiberWaiter(IRubyObject scheduler, IRubyObject fiber) implements ConditionVariable.Waiter {}

    /** Fibers blocked in {@link #lock} through a fiber scheduler. MRI: mutex waitq */
    private final ArrayDeque<FiberWaiter> schedulerWaiters = new ArrayDeque<>();

    /** Threads in {@link #sleep}, whose Condition#await releases the lock without waking scheduler waiters. */
    private final AtomicInteger awaitingThreads = new AtomicInteger();

    /** Temporary: fibers poll while threads sleep; to be removed in a follow-up. */
    private static final double AWAITING_RETRY_SECONDS = 0.001;

    /** Wakes a scheduler waiter on every release, including a dying thread's. MRI: rb_mutex_unlock_th */
    private final class MutexLock extends ReentrantLock {
        @Override
        public void unlock() {
            lockingThread = null;
            lockingFiber = null;
            super.unlock();
            wakeupSchedulerWaiter(getRuntime().getCurrentContext());
        }
    }

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static Mutex newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Mutex result = new Mutex(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public Mutex(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass setup(ThreadContext context, RubyClass Thread, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "Mutex",
                Thread.defineClassUnder(context, "Mutex", Object, Mutex::new).reifiedClass(Mutex.class).defineMethods(context, Mutex.class));
    }

    @JRubyMethod(name = "locked?")
    public RubyBoolean locked_p(ThreadContext context) {
        return asBoolean(context, isLocked());
    }

    public boolean isLocked() {
        return lock.isLocked();
    }

    @JRubyMethod
    public RubyBoolean try_lock(ThreadContext context) {
        return asBoolean(context, tryLock(context));
    }

    public boolean tryLock(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            return false;
        }
        boolean locked = context.getThread().tryLock(lock);

        if (locked) {
            this.lockingThread = context.getFiberCurrentThread();
            this.lockingFiber = context.getFiber();
        }

        return locked;
    }

    @JRubyMethod
    public IRubyObject lock(ThreadContext context) {
        RubyThread thread = context.getThread();
        RubyThread parentThread = context.getFiberCurrentThread();

        checkRelocking(context);

        IRubyObject scheduler = FiberScheduler.current(context);

        if (scheduler != null) {
            schedulerLock(context, scheduler);
        } else if (this.lockingThread == parentThread && this.lockingFiber != context.getFiber()) {
            throw context.runtime.newThreadError("deadlock; lock already owned by another fiber belonging to the same thread");
        }

        // schedulerLock has already acquired the lock when there is a scheduler;
        // otherwise try locking without sleep status to avoid looking like blocking
        if (!lock.isHeldByCurrentThread() && !thread.tryLock(lock)) {
            for (;;) {
                try {
                    context.getThread().lockInterruptibly(lock);
                    break;
                } catch (InterruptedException ex) {
                    /// ignore, check thread events and try again!
                    context.pollThreadEvents();
                }
            }
        }

        // always check for thread interrupts after acquiring lock
        try {
            thread.pollThreadEvents(context);
        } catch (Throwable t) {
            // Thread poll triggered an exception event, release locked locks before propagating
            if (lock.isHeldByCurrentThread()) {
                thread.unlock(lock);
            }
            Helpers.throwException(t);
        }

        // set locking thread and fiber once successfully locked with no interrupts
        this.lockingThread = parentThread;
        this.lockingFiber = context.getFiber();

        return this;
    }

    // MRI: do_mutex_lock, which blocks a fiber through the scheduler until the lock is free
    private void schedulerLock(ThreadContext context, IRubyObject scheduler) {
        RubyThread thread = context.getThread();
        FiberWaiter waiter = new FiberWaiter(scheduler, context.getFiber());

        while (!thread.tryLock(lock)) {
            synchronized (schedulerWaiters) {
                // the holder may have unlocked since, and found no one to wake
                if (thread.tryLock(lock)) return;

                schedulerWaiters.add(waiter);
            }

            IRubyObject timeout = awaitingThreads.get() > 0 ?
                    RubyFloat.newFloat(context.runtime, AWAITING_RETRY_SECONDS) : context.nil;

            try {
                FiberScheduler.block(context, scheduler, this, timeout);
            } finally {
                synchronized (schedulerWaiters) {
                    schedulerWaiters.remove(waiter);
                }
            }
        }
    }

    // MRI: mutex_lock_uninterruptible, which leaves pending interrupts to the next poll rather than letting one
    // replace an exception already propagating out of sleep
    private void relock(ThreadContext context, IRubyObject scheduler) {
        schedulerLock(context, scheduler);
        this.lockingThread = context.getFiberCurrentThread();
        this.lockingFiber = context.getFiber();
    }

    // MRI: rb_mutex_unlock_th's wakeup of the next fiber waiting through a scheduler
    private void wakeupSchedulerWaiter(ThreadContext context) {
        FiberWaiter waiter;
        synchronized (schedulerWaiters) {
            waiter = schedulerWaiters.poll();
        }

        if (waiter != null) FiberScheduler.unblock(context, waiter.scheduler(), this, waiter.fiber());
    }

    @JRubyMethod
    public IRubyObject unlock(ThreadContext context) {
        if (!isLocked()) {
            throw context.runtime.newThreadError("Mutex is not locked");
        }
        if (!lock.isHeldByCurrentThread()) {
            throw context.runtime.newThreadError("Mutex is not owned by calling thread");
        }

        boolean hasQueued = lock.hasQueuedThreads();
        context.getThread().unlock(lock);
        return hasQueued ? context.nil : this;
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context) {
        return sleep(context, context.nil);
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context, IRubyObject timeout) {
        final long beg = System.currentTimeMillis();

        // MRI: rb_mutex_sleep, which sleeps through the scheduler and relocks afterwards
        IRubyObject scheduler = FiberScheduler.current(context);
        if (scheduler != null) {
            if (!timeout.isNil()) RubyTime.convertTimeInterval(context, timeout);
            if (!lock.isHeldByCurrentThread()) {
                throw context.runtime.newThreadError("Attempt to unlock a mutex which is not locked");
            }

            unlock(context);
            try {
                FiberScheduler.kernelSleep(context, scheduler, timeout);
            } finally {
                relock(context, scheduler);
            }

            return asFixnum(context, (System.currentTimeMillis() - beg) / 1000);
        }

        awaitingThreads.incrementAndGet();
        try {
            // wake the first fiber waiting through a scheduler so it blocks again on the short timeout, taking the
            // lock once the await below releases it; any others are woken in turn as the lock is released
            wakeupSchedulerWaiter(context);

            RubyThread thread = context.getThread();

            if (timeout.isNil()) {
                thread.sleep(lock);
            } else {
                double t = RubyTime.convertTimeInterval(context, timeout);
                long millis = (long) (t * 1000);

                if (Double.compare(t, 0.0d) == 0 || millis == 0) {
                    // wait time is zero or smaller than 1ms, so we just proceed
                } else {
                    thread.sleep(lock, millis);
                }
            }
        } catch (IllegalMonitorStateException imse) {
            throw context.runtime.newThreadError("Attempt to unlock a mutex which is not locked");
        } catch (InterruptedException ex) {
            context.pollThreadEvents();
        } finally {
            awaitingThreads.decrementAndGet();
        }

        return asFixnum(context, (System.currentTimeMillis() - beg) / 1000);
    }

    @JRubyMethod
    public IRubyObject synchronize(ThreadContext context, Block block) {
        lock(context);
        try {
            return block.yieldSpecific(context);
        } finally {
            unlock(context);
        }
    }

    @JRubyMethod(name = "owned?")
    public IRubyObject owned_p(ThreadContext context) {
        return asBoolean(context, lock.isHeldByCurrentThread());
    }

    private void checkRelocking(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            throw context.runtime.newThreadError("deadlock; recursive locking");
        }
    }

}
