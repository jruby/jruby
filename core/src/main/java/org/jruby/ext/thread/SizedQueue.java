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
import java.util.concurrent.locks.Condition;

/**
 * The "SizedQueue" class from the 'thread' library.
 */
@JRubyClass(name = "SizedQueue", parent = "Queue")
public class SizedQueue extends Queue {
    protected final Condition pushCond = lock.newCondition();
    protected volatile int max;

    protected SizedQueue(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public SizedQueue(Ruby runtime, RubyClass type, int max) {
        super(runtime, type);

        initialize(runtime.getCurrentContext(), runtime.newFixnum(max));
    }

    public static void setup(Ruby runtime) {
        RubyClass cSizedQueue = runtime.defineClass("SizedQueue", runtime.getClass("Queue"), new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new SizedQueue(runtime, klass);
            }
        });
        cSizedQueue.setReifiedClass(SizedQueue.class);
        cSizedQueue.defineAnnotatedMethods(SizedQueue.class);
    }

    @JRubyMethod
    @Override
    public IRubyObject clear(ThreadContext context) {
        lock.lock();
        try {
            getQue().clear();

            pushCond.signalAll();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }

        return this;
    }

    @JRubyMethod
    public RubyNumeric max(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, max);
    }

    @JRubyMethod(name = "max=")
    public synchronized IRubyObject max_set(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        int max = RubyNumeric.num2int(arg), diff = 0;

        if (max <= 0) {
            throw runtime.newArgumentError("queue size must be positive");
        }

        lock.lock();
        try {
            if (max > this.max) {
                diff = max - this.max;
            }
            this.max = max;
            while (diff-- > 0) {
                pushCond.signal();
            }
            return arg;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public synchronized IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        que = new ArrayList<>();

        max_set(context, arg);

        return this;
    }

    @JRubyMethod(name = {"push", "<<", "enq"}, required = 1, optional = 1)
    public IRubyObject push(ThreadContext context, final IRubyObject[] argv) {
        boolean should_block = shouldBlock(context, argv);

        try {
            return context.getThread().executeTask(context, argv[0], should_block ? blockingPushTask : nonblockingPushTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#push");
        }
    }

    private final RubyThread.Task<IRubyObject, IRubyObject> blockingPushTask = new RubyThread.Task<IRubyObject, IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, IRubyObject value) throws InterruptedException {
            lock.lock();
            try {
                while (getQue().size() >= max) {
                    if (closed) {
                        return raiseClosedError(context);
                    }
                    else {
                        pushCond.await();
                    }
                }

                if (closed) {
                    raiseClosedError(context);
                }

                return push(context, value);
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        }

        @Override
        public void wakeup(RubyThread thread, IRubyObject value) {
            thread.getNativeThread().interrupt();
        }
    };

    private final RubyThread.Task<IRubyObject, IRubyObject> nonblockingPushTask = new RubyThread.Task<IRubyObject, IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, IRubyObject value) {
            lock.lock();
            try {
                if (getQue().size() >= max) {
                    throw context.runtime.newThreadError("queue full");
                }

                if (closed) {
                    raiseClosedError(context);
                }

                return push(context, value);
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
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

    @Override
    protected IRubyObject doClose(ThreadContext context) throws InterruptedException {
        if (!closed) {
            closed = true;

            if (lock.hasWaiters(popCond)) {
                popCond.signalAll();
            }

            if (lock.hasWaiters(pushCond)) {
                pushCond.signalAll();
            }
        }

        return this;
    }

    @Override
    protected IRubyObject popInternal(ThreadContext context, boolean should_block) throws InterruptedException {
        lock.lock();
        try {
            IRubyObject result = should_block ? popBlocking(context) : popNonblocking(context);

            pushCond.signal();

            return result;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
