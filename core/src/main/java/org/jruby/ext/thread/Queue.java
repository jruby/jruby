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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyMarshal;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;
import org.jruby.util.TypeConverter;

import static org.jruby.api.Access.arrayClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toDouble;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.typeError;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * The "Queue" class from the 'thread' library.
 *
 * The implementation is a kernel of Doug Lea's LinkedBlockingQueue with
 * Ruby related tweaks: closeable (blocks out producers, no effect on consumers)
 * and capacity adjustable (Ruby allows sized_queue.max = 123 post construction).
 * Relevant changes noted in comments below.
 *
 * An optionally-bounded {@linkplain BlockingQueue blocking queue} based on
 * linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * Linked queues typically have higher throughput than array-based queues but
 * less predictable performance in most concurrent applications.
 *
 * <p>The optional capacity bound constructor argument serves as a
 * way to prevent excessive queue expansion. The capacity, if unspecified,
 * is equal to {@link Integer#MAX_VALUE}.  Linked nodes are
 * dynamically created upon each insertion unless this would bring the
 * queue above capacity.
 *
 * @since 1.5
 * @author Doug Lea
 */
@JRubyClass(name = "Queue")
public class Queue extends RubyObject implements DataType {

    /*
     * A variant of the "two lock queue" algorithm.  The putLock gates
     * entry to put (and offer), and has an associated condition for
     * waiting puts.  Similarly for the takeLock.  The "count" field
     * that they both rely on is maintained as an atomic to avoid
     * needing to get both locks in most cases. Also, to minimize need
     * for puts to get takeLock and vice-versa, cascading notifies are
     * used. When a put notices that it has enabled at least one takeInternal,
     * it signals taker. That taker in turn signals others if more
     * items have been entered since the signal. And symmetrically for
     * takes signalling puts. Operations such as remove(Object) and
     * iterators acquire both locks.
     *
     * Visibility between writers and readers is provided as follows:
     *
     * Whenever an element is enqueued, the putLock is acquired and
     * count updated.  A subsequent reader guarantees visibility to the
     * enqueued Node by either acquiring the putLock (via fullyLock)
     * or by acquiring the takeLock, and then reading n = count.get();
     * this gives visibility to the first n items.
     *
     * To implement weakly consistent iterators, it appears we need to
     * keep all Nodes GC-reachable from a predecessor dequeued Node.
     * That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.next.
     */


    /**
     * Linked list node class
     */
    static class Node {
        IRubyObject item;

        /**
         * One of:
         * - the real successor Node
         * - this Node, meaning the successor is head.next
         * - null, meaning there is no successor (this is the last node)
         */
        Node next;

        Node(IRubyObject x) { item = x; }
    }

    protected volatile boolean closed = false;

    /** The capacity bound, or Integer.MAX_VALUE if none */
    // LinkedBlockingQueue diffs:
    // Having this volatile allows for lock-free & non-blocking push() for sized queues.
    // The capacity is also no longer final because of SizedQueue#max=.
    protected volatile int capacity;

    /** Current number of elements */
    protected final AtomicInteger count = new AtomicInteger();

    /**
     * Head of linked list.
     * Invariant: head.item == null
     */
    transient Node head;

    /**
     * Tail of linked list.
     * Invariant: last.next == null
     */
    protected transient Node last;

    /** Lock held by takeInternal, poll, etc */
    protected final ReentrantLock takeLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    protected final Condition notEmpty = takeLock.newCondition();

    /** Lock held by put, offer, etc */
    protected final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting puts */
    protected final Condition notFull = putLock.newCondition();

    /**
     * Signals a waiting takeInternal. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    protected void signalNotEmpty() {
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting put. Called only from take/poll.
     */
    protected void signalNotFull() {
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            notFull.signal();
        } finally {
            putLock.unlock();
        }
    }

    protected void enqueue(Node node) {
        last = last.next = node;
    }

    protected IRubyObject dequeue() {
        Node h = head;
        Node first = h.next;
        h.next = h; // help GC
        head = first;
        IRubyObject x = first.item;
        first.item = null;
        return x;
    }

    /**
     * Locks to prevent both puts and takes.
     */
    protected void fullyLock() {
        putLock.lock();
        takeLock.lock();
    }

    /**
     * Unlocks to allow both puts and takes.
     */
    protected void fullyUnlock() {
        takeLock.unlock();
        putLock.unlock();
    }

    @Deprecated(since = "10.0")
    protected void initializedCheck() {
        initializedCheck(getCurrentContext());
    }

    protected void initializedCheck(ThreadContext context) {
        // Ruby initialized check seems to be a genius way to make all methods slower :),
        // here we piggy back on capacity not being allowed to equal 0.
        if (capacity == 0) throw typeError(context, this + " not initialized");
    }

    public Queue(Ruby runtime, RubyClass type) {
        super(runtime, type);
        // LinkedBlockingQueue diff: leaving capacity setup to initialize().
        last = head = new Node(null);
    }

    public static RubyClass setup(ThreadContext context, RubyClass Thread, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "Queue",
                Thread.defineClassUnder(context, "Queue", Object, Queue::new).
                        reifiedClass(Queue.class).defineMethods(context, Queue.class).
                        undefMethods(context, "initialize_copy"));
    }

    public static RubyClass setupError(ThreadContext context, RubyClass Queue, RubyClass StopIteration, RubyClass Object) {
        return (RubyClass) Object.setConstant(context, "ClosedQueueError",
                Queue.defineClassUnder(context, "ClosedQueueError", StopIteration, StopIteration.getAllocator()));
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        this.capacity = Integer.MAX_VALUE;
        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject items) {
        this.capacity = Integer.MAX_VALUE;
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck(context, items, arrayClass(context), context.sites.TypeConverter.to_a_checked);
        if (tmp.isNil()) throw typeError(context, "can't convert ", items, " into Array");

        RubyArray array = (RubyArray) tmp;
        for (int i = 0; i < array.getLength(); i++) {
            push(context, array.eltOk(i));
        }

        return this;
    }

    /**
     * Atomically removes all of the elements from this queue.
     * The queue will be empty after this call returns.
     */
    @JRubyMethod
    public IRubyObject clear(ThreadContext context) {
        initializedCheck(context);
        try {
            clearInternal();
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "clear");
        }

        return this;
    }

    protected void clearInternal() throws InterruptedException {
        final ReentrantLock putLock = this.putLock;
        final ReentrantLock takeLock = this.takeLock;
        // LinkedBlockingQueue diff: lock acquisition is interruptible
        putLock.lockInterruptibly();
        try {
            takeLock.lockInterruptibly();
            try {
                for (Node p, h = head; (p = h.next) != null; h = p) {
                    h.next = h;
                    p.item = null;
                }
                head = last;
                // assert head.item == null && head.next == null;
                if (count.getAndSet(0) == capacity)
                    notFull.signal();
            } finally {
                takeLock.unlock();
            }
        } finally {
            putLock.unlock();
        }
    }

    @JRubyMethod(name = "freeze")
    @Override
    public final IRubyObject freeze(ThreadContext context) {
        throw typeError(context, "cannot freeze " + this);
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        initializedCheck(context);
        return asBoolean(context, count.get() == 0);
    }

    @JRubyMethod(name = {"length", "size"})
    public RubyNumeric length(ThreadContext context) {
        initializedCheck(context);
        return asFixnum(context, count.get());
    }

    @JRubyMethod
    public RubyNumeric num_waiting(ThreadContext context) {
        initializedCheck(context);
        final ReentrantLock takeLock = this.takeLock;
        try {
            takeLock.lockInterruptibly();
            try {
                return asFixnum(context, takeLock.getWaitQueueLength(notEmpty));
            } finally {
                takeLock.unlock();
            }
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "num_waiting");
        }
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context) {
        initializedCheck(context);
        try {
            return context.getThread().executeTaskBlocking(context, this, BLOCKING_TAKE_TASK);
        } catch (InterruptedException ie) {
            // FIXME: is this the right thing to do?
            throw createInterruptedError(context, "pop");
        }
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context, IRubyObject nonblockOrOpts) {
        initializedCheck(context);

        boolean nonblock = false;
        long timeoutNS = 0;

        RubyHash opts = ArgsUtil.extractKeywords(nonblockOrOpts);

        if (opts != null) {
            IRubyObject _timeout = ArgsUtil.extractKeywordArg(context, "timeout", opts);

            if (!_timeout.isNil()) {
                timeoutNS = queueTimeoutToNanos(context, _timeout);

                if (timeoutNS == 0 && count.get() == 0) {
                    return context.nil;
                }
            }
        } else {
            nonblock = nonblockOrOpts.isTrue();
        }

        return popCommon(context, nonblock, timeoutNS);
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context, IRubyObject _nonblock, IRubyObject _opts) {
        initializedCheck(context);

        boolean nonblock = _nonblock.isTrue();
        long timeoutNS = 0;

        IRubyObject _timeout = ArgsUtil.extractKeywordArg(context, "timeout", _opts);
        if (!_timeout.isNil()) {
            if (nonblock) throw argumentError(context, "can't set a timeout if non_block is enabled");

            timeoutNS = queueTimeoutToNanos(context, _timeout);

            if (timeoutNS == 0 && count.get() == 0) return context.nil;
        }

        return popCommon(context, nonblock, timeoutNS);
    }

    private IRubyObject popCommon(ThreadContext context, boolean nonblock, long timeoutNS) {
        try {
            RubyThread.Task<Queue, IRubyObject> task;

            if (nonblock) {
                IRubyObject result = pollInternal();
                if (result == null) {
                    throw context.runtime.newThreadError("queue empty");
                }
                return result;
            }

            if (timeoutNS != 0) {
                task = new BlockingPollTask(timeoutNS);
            } else {
                task = BLOCKING_TAKE_TASK;
            }

            return context.getThread().executeTaskBlocking(context, this, task);
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "pop");
        }
    }

    protected static long queueTimeoutToNanos(ThreadContext context, IRubyObject _timeout) {
        if (_timeout.isNil()) return 0;

        return _timeout instanceof RubyFixnum fixnum ?
                TimeUnit.NANOSECONDS.convert(fixnum.getValue(), TimeUnit.SECONDS) :
                (long) (toDouble(context, _timeout) * 1_000_000_000);
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public IRubyObject push(ThreadContext context, IRubyObject value) {
        initializedCheck(context);
        try {
            putInternal(context, value);
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "push");
        }

        return this;
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary for space to become available.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    protected void putInternal(ThreadContext context, IRubyObject e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        // Note: convention in all put/takeInternal/etc is to preset local var
        // holding count negative to indicate failure unless set.
        int c;
        Node node = new Node(e);
        final ReentrantLock putLock = this.putLock;
        final AtomicInteger count = this.count;
        putLock.lockInterruptibly();
        try {
            boolean isClosed;
            /*
             * Note that count is used in wait guard even though it is
             * not protected by lock. This works because count can
             * only decrease at this point (all other puts are shut
             * out by lock), and we (or some other waiting put) are
             * signalled if it ever changes from capacity. Similarly
             * for all other uses of count in other wait guards.
             */
            // LinkedBlockingQueue diff:
            // * newly check the closed flag,
            // * count.get() == capacity is now a ">=" check, otherwise
            //   when the queue (if SizedQueue) gets full (ie some threads
            //   will sleep/spin on the following loop) and then capacity
            //   is shrunk (via SizedQueue#max=) blocked producers would
            //   think they can go on, when in fact the queue is still full.
            while (!(isClosed = closed) && count.get() >= capacity) {
                notFull.await();
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

            enqueue(node);
            c = count.getAndIncrement();

            if (c + 1 < capacity)
                notFull.signal();
        } finally {
            putLock.unlock();
        }

        if (c == 0)
            signalNotEmpty();
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        return RubyMarshal.undumpable(context, this);
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        initializedCheck(context);
        try {
            closeInternal();
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "close");
        }

        return this;
    }

    protected void closeInternal() throws InterruptedException {
        final ReentrantLock putLock = this.putLock;
        final ReentrantLock takeLock = this.takeLock;
        final AtomicInteger count = this.count;
        int c;
        putLock.lockInterruptibly();
        try {
            takeLock.lockInterruptibly();
            try {
                if (closed) {
                    return;
                }

                closed = true;

                c = count.get();
                // queue's item count can exceed capacity because of SizedQueue#max=
                if (c >= capacity) {
                    // any blocked producers are now free to error out, wake the first
                    // in line
                    notFull.signal();
                } else if (c == 0) {
                    // wake the first blocked consumer
                    notEmpty.signal();
                }
            } finally {
                takeLock.unlock();
            }
        } finally {
            putLock.unlock();
        }
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p(ThreadContext context) {
        initializedCheck(context);
        return asBoolean(context, closed);
    }

    public synchronized void shutdown() throws InterruptedException {
        closeInternal();
    }

    public boolean isShutdown() {
        return closed;
    }

    @Deprecated(since = "10.0")
    public synchronized void checkShutdown() {
        if (isShutdown()) {
            Ruby runtime = getCurrentContext().runtime;
            throw RaiseException.from(runtime, runtime.getThreadError(), "queue shut down");
        }
    }

    protected long java_length() {
        return count.get();
    }

    protected IRubyObject takeInternal(ThreadContext context) throws InterruptedException {
        IRubyObject x;
        int c = -1;
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        boolean isClosed;
        boolean notFullSignalNeeded = false;

        takeLock.lockInterruptibly();
        try {
            // LinkedBlockingQueue diff: newly checking closed flag
            while (!(isClosed = closed) && count.get() == 0) {
                notEmpty.await();
            }

            // LinkedBlockingQueue diff: dequeue conditionally (if there
            // are values to deque.)
            boolean canDequeue = !isClosed || count.get() != 0;
            if (canDequeue) {
                x = dequeue();
                c = count.getAndDecrement();
            } else {
                x = context.nil;
            }

            // LinkedBlockingQueue diff: wake the next in line consumer
            // for closed queue as well
            if (c > 1 || isClosed)
                notEmpty.signal();

            if (canDequeue) {
                // LinkedBlockingQueue diff: moved this check into locked
                // section because of SizedQueue.max=, this might not be
                // necessary (just be being overly cautious).
                notFullSignalNeeded = c == capacity;
            }
        } finally {
            takeLock.unlock();
        }

        if (notFullSignalNeeded)
            signalNotFull();

        return x;
    }

    public IRubyObject pollInternal() throws InterruptedException {
        final AtomicInteger count = this.count;
        if (count.get() == 0)
            return null;
        final IRubyObject x;
        final int c;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lock();
        try {
            if (count.get() == 0)
                return null;
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    public IRubyObject pollInternal(long timeout, TimeUnit unit) throws InterruptedException {
        final IRubyObject x;
        final int c;
        long nanos = unit.toNanos(timeout);
        final AtomicInteger count = this.count;
        final ReentrantLock takeLock = this.takeLock;
        takeLock.lockInterruptibly();
        try {
            while (count.get() == 0) {
                if (nanos <= 0L)
                    return null;
                nanos = notEmpty.awaitNanos(nanos);
            }
            x = dequeue();
            c = count.getAndDecrement();
            if (c > 1)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
        if (c == capacity)
            signalNotFull();
        return x;
    }

    private static final RubyThread.Task<Queue, IRubyObject> BLOCKING_TAKE_TASK = new RubyThread.Task<Queue, IRubyObject>() {
        public IRubyObject run(ThreadContext context, Queue queue) throws InterruptedException {
            while (true) {
                try {
                    return queue.takeInternal(context);
                } catch (InterruptedException ie) {
                    // only thread event can interrupt us
                    context.blockingThreadPoll();
                }
            }
        }
        public void wakeup(RubyThread thread, Queue queue) {
            thread.getNativeThread().interrupt();
        }
    };

    public IRubyObject raiseClosedError(ThreadContext context) {
        throw context.runtime.newRaiseException(context.runtime.getClosedQueueError(), "queue closed");
    }

    protected RaiseException createInterruptedError(ThreadContext context, String methodName) {
        return context.runtime.newThreadError("interrupted in " + getMetaClass().getName(context) + "#" + methodName);
    }

    private static class BlockingPollTask implements RubyThread.Task<Queue, IRubyObject> {
        private final long timeoutNS;

        public BlockingPollTask(long timeoutNS) {
            this.timeoutNS = timeoutNS;
        }

        @Override
        public IRubyObject run(ThreadContext context, Queue queue) throws InterruptedException {
                IRubyObject result = timeoutNS == 0 ? queue.pollInternal() : queue.pollInternal(timeoutNS, TimeUnit.NANOSECONDS);
                if (result == null && timeoutNS == 0) {
                    throw context.runtime.newThreadError("queue empty");
                } else {
                    return Helpers.nullToNil(result, context.nil);
                }
            }

        @Override
        public void wakeup(RubyThread thread, Queue queue) {
            thread.getNativeThread().interrupt();
        }
    }
}
