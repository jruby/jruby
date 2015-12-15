/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

/**
 * The "Queue" class from the 'thread' library.
 */
@JRubyClass(name = "Queue")
public class Queue extends RubyObject implements DataType {
    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition popCond = lock.newCondition();
    protected volatile List<IRubyObject> que;
    protected volatile boolean closed;

    public Queue(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static void setup(Ruby runtime) {
        RubyClass cQueue = runtime.defineClass("Queue", runtime.getObject(), new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Queue(runtime, klass);
            }
        });
        cQueue.undefineMethod("initialize_copy");
        cQueue.setReifiedClass(Queue.class);
        cQueue.defineAnnotatedMethods(Queue.class);

        runtime.defineClass("ClosedQueueError", runtime.getStopIteration(), runtime.getStopIteration().getAllocator());
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        que = new ArrayList<>();

        return this;
    }

    @JRubyMethod
    public IRubyObject clear(ThreadContext context) {
        try {
            lock.lockInterruptibly();

            getQue().clear();
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#clear");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }

        return this;
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        return context.runtime.newBoolean(getQue().isEmpty());
    }

    @JRubyMethod(name = {"length", "size"})
    public RubyNumeric length(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, getQue().size());
    }

    @JRubyMethod
    public RubyNumeric num_waiting(ThreadContext context) {
        lock.lock();
        try {
            return context.runtime.newFixnum(lock.getWaitQueueLength(popCond));
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context) {
        try {
            return context.getThread().executeTask(context, this, BLOCKING_POP_TASK);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#pop");
        }
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context, IRubyObject arg0) {
        try {
            return context.getThread().executeTask(context, this, !arg0.isTrue() ? BLOCKING_POP_TASK : NONBLOCKING_POP_TASK);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#pop");
        }
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public IRubyObject push(ThreadContext context, IRubyObject value) {
        if (closed) {
            raiseClosedError(context);
        }
        lock.lock();
        try {
            getQue().add(value);
            popCond.signal();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        return ThreadLibrary.undumpable(context, this);
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        lock.lock();
        try {
            doClose(context);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#pop");
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }

        return this;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p(ThreadContext context) {
        return context.runtime.newBoolean(closed);
    }

    protected IRubyObject doClose(ThreadContext context) throws InterruptedException {
        if (!closed) {
            closed = true;

            if (lock.hasWaiters(popCond)) {
                popCond.signalAll();
            }
        }

        return this;
    }

    public synchronized void shutdown() {
        closed = true;
    }

    public boolean isShutdown() {
        return closed;
    }

    public synchronized void checkShutdown() {
        if (isShutdown()) {
            Ruby runtime = getRuntime();
            throw new RaiseException(runtime, runtime.getThreadError(), "queue shut down", false);
        }
    }

    protected long java_length() {
        return getQue().size();
    }

    protected IRubyObject popInternal(ThreadContext context, boolean should_block) throws InterruptedException {
        lock.lock();
        try {
            return should_block ? popBlocking(context) : popNonblocking(context);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    protected IRubyObject popBlocking(ThreadContext context) throws InterruptedException {
        while (getQue().isEmpty()) {
            if (closed) {
                return context.nil;
            }
            else {
                assert(getQue().size() == 0);
                assert(!closed);

                popCond.await();
            }
        }

        return getQue().remove(0);
    }

    protected IRubyObject popNonblocking(ThreadContext context) throws InterruptedException {
        lock.lock();
        try {
            if (getQue().isEmpty()) {
                throw context.runtime.newThreadError("queue empty");
            }

            return getQue().remove(0);
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private static final RubyThread.Task<Queue, IRubyObject> BLOCKING_POP_TASK = new RubyThread.Task<Queue, IRubyObject>() {
        public IRubyObject run(ThreadContext context, Queue queue) throws InterruptedException {
            return queue.popInternal(context, true);
        }
        public void wakeup(RubyThread thread, Queue queue) {
            thread.getNativeThread().interrupt();
        }
    };

    private static final RubyThread.Task<Queue, IRubyObject> NONBLOCKING_POP_TASK = new RubyThread.Task<Queue, IRubyObject>() {
        public IRubyObject run(ThreadContext context, Queue queue) throws InterruptedException {
            return queue.popInternal(context, false);
        }
        public void wakeup(RubyThread thread, Queue queue) {
            thread.getNativeThread().interrupt();
        }
    };

    public IRubyObject raiseClosedError(ThreadContext context) {
        throw context.runtime.newRaiseException(context.runtime.getClass("ClosedQueueError"), "queue closed");
    }

    protected List<IRubyObject> getQue() {
        List<IRubyObject> que = this.que;

        if (que == null) throw getRuntime().newTypeError(this + " not initialized");

        return que;
    }
}
