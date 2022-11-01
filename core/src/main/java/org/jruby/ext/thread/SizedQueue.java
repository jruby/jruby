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
import org.jruby.RubyNumeric;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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

        initialize(runtime.getCurrentContext(), runtime.newFixnum(max));
    }

    public static RubyClass setup(RubyClass threadClass, RubyClass queueClass, RubyClass objectClass) {
        RubyClass cSizedQueue = threadClass.defineClassUnder("SizedQueue", queueClass, SizedQueue::new);

        cSizedQueue.setReifiedClass(SizedQueue.class);
        cSizedQueue.defineAnnotatedMethods(SizedQueue.class);

        objectClass.setConstant("SizedQueue", cSizedQueue);

        return cSizedQueue;
    }

    @JRubyMethod
    public RubyNumeric max(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, capacity);
    }

    @JRubyMethod(name = "max=")
    public synchronized IRubyObject max_set(ThreadContext context, IRubyObject arg) {
        initializedCheck();
        Ruby runtime = context.runtime;
        int max = RubyNumeric.num2int(arg), diff = 0;

        if (max <= 0) {
            throw runtime.newArgumentError("queue size must be positive");
        }

        fullyLock();
        try {
            if (count.get() >= capacity && max > capacity) {
                diff = max - capacity;
            }
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
                    return context.runtime.newFixnum(takeLock.getWaitQueueLength(notEmpty) + putLock.getWaitQueueLength(notFull));
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

    @JRubyMethod(name = {"push", "<<", "enq"}, required = 1, optional = 1)
    public IRubyObject push(ThreadContext context, final IRubyObject[] argv) {
        initializedCheck();

        boolean should_block = shouldBlock(context, argv);

        try {
            if (should_block) {
                return context.getThread().executeTaskBlocking(context, argv[0], blockingPushTask);
            } else {
                return context.getThread().executeTask(context, argv[0], nonblockingPushTask);
            }
        } catch (InterruptedException ie) {
            throw createInterruptedError(context, "push");
        }
    }

    protected boolean offerInternal(ThreadContext context, IRubyObject e) {
        if (e == null) throw new NullPointerException();
        final AtomicInteger count = this.count;
        if (count.get() == capacity)
            return false;
        int c = -1;
        Node node = new Node(e);
        final ReentrantLock putLock = this.putLock;
        putLock.lock();
        try {
            if (closed) {
                raiseClosedError(context);
            }
            if (count.get() < capacity) {
                enqueue(node);
                c = count.getAndIncrement();
                if (c + 1 < capacity)
                    notFull.signal();
            }
        } finally {
            putLock.unlock();
        }
        if (c == 0)
            signalNotEmpty();
        return c >= 0;
    }

    private final RubyThread.Task<IRubyObject, IRubyObject> blockingPushTask = new RubyThread.Task<IRubyObject, IRubyObject>() {
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

    private final RubyThread.Task<IRubyObject, IRubyObject> nonblockingPushTask = new RubyThread.Task<IRubyObject, IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, IRubyObject value) {
            if (!offerInternal(context, value)) {
                throw context.runtime.newThreadError("queue full");
            }
            return SizedQueue.this;
        }

        @Override
        public void wakeup(RubyThread thread, IRubyObject value) {
            thread.getNativeThread().interrupt();
        }
    };

    private static boolean shouldBlock(ThreadContext context, IRubyObject[] argv) {
        boolean should_block = true;
        Arity.checkArgumentCount(context, argv, 1, 2);
        if (argv.length > 1) {
            should_block = !argv[1].isTrue();
        }
        return should_block;
    }
}
