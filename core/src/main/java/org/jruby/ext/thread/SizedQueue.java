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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyNumeric;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Error.argumentError;

/**
 * The "SizedQueue" class from the 'thread' library.
 */
@JRubyClass(name = "SizedQueue", parent = "Queue")
public class SizedQueue extends Queue {
    protected SizedQueue(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public SizedQueue(Ruby runtime, RubyClass type, int max) {
        super(runtime, type);

        ThreadContext context = runtime.getCurrentContext();
        initialize(context, asFixnum(context, max));
    }

    public static RubyClass setup(ThreadContext context, RubyClass Thread, RubyClass Queue, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "SizedQueue",
                Thread.defineClassUnder(context, "SizedQueue", Queue, SizedQueue::new).
                        reifiedClass(SizedQueue.class).defineMethods(context, SizedQueue.class));
    }

    @JRubyMethod
    public RubyNumeric max(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, capacity);
    }

    @JRubyMethod(name = "max=")
    public synchronized IRubyObject max_set(ThreadContext context, IRubyObject arg) {
        initializedCheck();

        int max = RubyNumeric.num2int(arg), diff = 0;
        if (max <= 0) throw argumentError(context, "queue size must be positive");

        fullyLock();
        try {
            if (count.get() >= capacity && max > capacity) diff = max - capacity;

            capacity = max;
            while (diff-- > 0) {
                notFull.signal();
            }
            return arg;
        } finally {
            fullyUnlock();
        }
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public synchronized IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        capacity = Integer.MAX_VALUE; // don't trigger initializedCheck() trap in max_set
        max_set(context, arg);
        return this;
    }

    @JRubyMethod
    public RubyNumeric num_waiting(ThreadContext context) {
        initializedCheck();
        final ReentrantLock takeLock = this.takeLock;
        final ReentrantLock putLock = this.putLock;
        try {
            takeLock.lockInterruptibly();
            try {
                putLock.lockInterruptibly();
                try {
                    return asFixnum(context, takeLock.getWaitQueueLength(notEmpty) + putLock.getWaitQueueLength(notFull));
                } finally {
                    putLock.unlock();
                }
            } finally {
                takeLock.unlock();
            }
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "num_waiting");
        }
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public IRubyObject push(ThreadContext context, final IRubyObject arg0) {
        initializedCheck();

        try {
            return context.getThread().executeTaskBlocking(context, arg0, blockingPutTask);
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "push");
        }
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public IRubyObject push(ThreadContext context, final IRubyObject arg0, final IRubyObject nonblockOrOpts) {
        initializedCheck();

        boolean nonblock = false;
        long timeoutNS = 0;

        RubyHash opts = ArgsUtil.extractKeywords(nonblockOrOpts);

        if (opts != null) {
            IRubyObject _timeout = ArgsUtil.extractKeywordArg(context, "timeout", opts);

            if (!_timeout.isNil()) {
                timeoutNS = queueTimeoutToNanos(context, _timeout);

                if (timeoutNS == 0 && count.get() == capacity) {
                    return context.nil;
                }
            }
        } else {
            nonblock = nonblockOrOpts.isTrue();
        }

        return pushCommon(context, arg0, nonblock, timeoutNS);
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public IRubyObject push(ThreadContext context, final IRubyObject arg0, final IRubyObject _nonblock, IRubyObject _opts) {
        initializedCheck();

        boolean nonblock = _nonblock.isTrue();
        long timeoutNS = 0;

        IRubyObject _timeout = ArgsUtil.extractKeywordArg(context, "timeout", _opts);
        if (!_timeout.isNil()) {
            if (nonblock) throw argumentError(context, "can't set a timeout if non_block is enabled");

            timeoutNS = queueTimeoutToNanos(context, _timeout);

            if (timeoutNS == 0 && count.get() == capacity) return context.nil;
        }

        return pushCommon(context, arg0, nonblock, timeoutNS);
    }

    private IRubyObject pushCommon(ThreadContext context, IRubyObject arg0, boolean nonblock, long timeoutNS) {
        try {
            RubyThread thread = context.getThread();
            if (nonblock) {
                if (!offerInternal(context, arg0)) throw context.runtime.newThreadError("queue full");

                return this;
            }

            RubyThread.Task<IRubyObject, IRubyObject> task = timeoutNS != 0 ?
                    new BlockingOfferTask(timeoutNS) : blockingPutTask;

            return thread.executeTaskBlocking(context, arg0, task);
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "push");
        }
    }

    protected boolean offerInternal(ThreadContext context, IRubyObject e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return false;
        final int c;
        final Node node = new Node(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (closed) raiseClosedError(context);
            if (count.get() == capacity) return false;
            enqueue(node);
            c = count.getAndIncrement();
            if (c + 1 < capacity) notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0) signalNotEmpty();
        return true;
    }

    public boolean offerInternal(ThreadContext context, IRubyObject e, long timeout, TimeUnit unit)
            throws InterruptedException {

        if (e == null) throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        final int c;
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            if (closed) {
                raiseClosedError(context);
            }
            /*
            Similar to differences in Queue from LinkedBlockingQueue, this checks for the closed flag,
            propagating a ClosedError along the waiters if we find the queue has been closed while waiting.
             */
            boolean isClosed;
            while (!(isClosed = closed) && count.get() == capacity) {
                if (nanos <= 0L)
                    return false;
                nanos = notFull.awaitNanos(nanos);
            }
            if (isClosed) {
                // wake the next in line
                notFull.signal();
                // note that this is now a new early exit from the method,
                // this doesn't matter because for the closed queues it is
                // not a producer's responsibility to wake the blocked consumers
                // (they wake each other, while the first in line gets notified
                // by the close() caller)
                raiseClosedError(context);
            }
            enqueue(new Node(e));
            c = count.getAndIncrement();
            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }
        if (c == 0) signalNotEmpty();
        return true;
    }

    private final RubyThread.Task<IRubyObject, IRubyObject> blockingPutTask = new RubyThread.Task<>() {
        @Override
        public IRubyObject run(ThreadContext context, IRubyObject value) throws InterruptedException {
            putInternal(context, value);
            return SizedQueue.this;
        }

        @Override
        public void wakeup(RubyThread thread, IRubyObject value) {
            thread.getNativeThread().interrupt();
        }
    };

    private final class BlockingOfferTask implements RubyThread.Task<IRubyObject, IRubyObject> {
        private final long timeoutNS;

        public BlockingOfferTask(long timeoutNS) {
            this.timeoutNS = timeoutNS;
        }

        @Override
        public IRubyObject run(ThreadContext context, IRubyObject value) throws InterruptedException {
            boolean result = offerInternal(context, value, timeoutNS, TimeUnit.NANOSECONDS);
            return !result ? context.nil : SizedQueue.this;
        }

        @Override
        public void wakeup(RubyThread thread, IRubyObject sizedQueue) {
            thread.getNativeThread().interrupt();
        }
    }

    @Deprecated
    public IRubyObject push(ThreadContext context, final IRubyObject[] argv) {
        return switch (argv.length) {
            case 1 -> push(context, argv[0]);
            case 2 -> push(context, argv[0], argv[1]);
            default -> throw argumentError(context, argv.length, 1, 2);
        };
    }
}
