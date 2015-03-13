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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

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

/**
 * The "Queue" class from the 'thread' library.
 */
@JRubyClass(name = "Queue")
public class Queue extends RubyObject {
    protected BlockingQueue<IRubyObject> queue;
    protected AtomicLong numWaiting = new AtomicLong();

    final RubyThread.Task<Queue, IRubyObject> takeTask = new RubyThread.Task<Queue, IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, Queue queue) throws InterruptedException {
            return queue.getQueueSafe().take();
        }

        @Override
        public void wakeup(RubyThread thread, Queue data) {
            thread.getNativeThread().interrupt();
        }
    };

    final RubyThread.Task<IRubyObject[], IRubyObject> putTask = new RubyThread.Task<IRubyObject[], IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, IRubyObject[] args) throws InterruptedException {
            final BlockingQueue<IRubyObject> queue = getQueueSafe();
            if(args.length == 2 && args[1].isTrue() && queue.remainingCapacity() == 0) {
                throw context.runtime.newThreadError("queue full");
            }
            queue.put(args[0]);
            return context.nil;
        }

        @Override
        public void wakeup(RubyThread thread, IRubyObject[] data) {
            thread.getNativeThread().interrupt();
        }
    };

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
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context) {
        queue = new LinkedBlockingQueue<IRubyObject>();
        return this;
    }

    @JRubyMethod(name = "shutdown!")
    public IRubyObject shutdown(ThreadContext context) {
        queue = null;
        return context.runtime.getNil();
    }
    
    public synchronized void shutdown() {
        queue = null;
    }

    public boolean isShutdown() {
        return queue == null;
    }

    public BlockingQueue<IRubyObject> getQueueSafe() {
        BlockingQueue<IRubyObject> queue = this.queue;
        checkShutdown();
        return queue;
    }

    public synchronized void checkShutdown() {
        if (queue == null) {
            Ruby runtime = getRuntime();
            throw new RaiseException(runtime, runtime.getThreadError(), "queue shut down", false);
        }
    }

    @JRubyMethod
    public synchronized IRubyObject clear(ThreadContext context) {
        BlockingQueue<IRubyObject> queue = getQueueSafe();
        queue.clear();
        return this;
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(ThreadContext context) {
        BlockingQueue<IRubyObject> queue = getQueueSafe();
        return context.runtime.newBoolean(queue.size() == 0);
    }

    @JRubyMethod(name = {"length", "size"})
    public RubyNumeric length(ThreadContext context) {
        checkShutdown();
        return RubyNumeric.int2fix(context.runtime, queue.size());
    }

    protected long java_length() {
        return queue.size();
    }

    @JRubyMethod
    public RubyNumeric num_waiting(ThreadContext context) {
        return context.runtime.newFixnum(numWaiting.longValue());
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context) {
        return pop(context, true);
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public IRubyObject pop(ThreadContext context, IRubyObject arg0) {
        return pop(context, !arg0.isTrue());
    }

    @JRubyMethod(name = {"push", "<<", "enq"}, required = 1, optional = 1)
    public IRubyObject push(ThreadContext context, final IRubyObject[] args) {
        checkShutdown();
        try {
            context.getThread().executeTask(context, args, putTask);
            return this;
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#push");
        }
    }

    @JRubyMethod
    public IRubyObject marshal_dump(ThreadContext context) {
        return ThreadLibrary.undumpable(context, this);
    }

    private IRubyObject pop(ThreadContext context, boolean should_block) {
        final BlockingQueue<IRubyObject> queue = getQueueSafe();
        if (!should_block && queue.size() == 0) {
            throw new RaiseException(context.runtime, context.runtime.getThreadError(), "queue empty", false);
        }
        numWaiting.incrementAndGet();
        try {
            return context.getThread().executeTask(context, this, takeTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in " + getMetaClass().getName() + "#pop");
        } finally {
            numWaiting.decrementAndGet();
        }
    }
    
}
