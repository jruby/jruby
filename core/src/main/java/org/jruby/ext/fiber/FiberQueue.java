/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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
package org.jruby.ext.fiber;

import org.jruby.Ruby;
import org.jruby.RubyThread;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A RubyThread-aware BlockingQueue wrapper used by Fiber for transferring values.
 */
public class FiberQueue {
    protected BlockingQueue<IRubyObject> queue;
    protected final Ruby runtime;

    public FiberQueue(Ruby runtime) {
        this.runtime = runtime;
        this.queue = new ArrayBlockingQueue<>(1, false);
    }

    final RubyThread.Task<FiberQueue, IRubyObject> takeTask = new RubyThread.Task<FiberQueue, IRubyObject>() {
        @Override
        public IRubyObject run(ThreadContext context, FiberQueue queue) throws InterruptedException {
            return queue.getQueueSafe().take();
        }

        @Override
        public void wakeup(RubyThread thread, FiberQueue data) {
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

    public IRubyObject shutdown(ThreadContext context) {
        queue = null;
        return context.nil;
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
            throw new RaiseException(runtime, runtime.getThreadError(), "queue shut down", false);
        }
    }

    public IRubyObject pop(ThreadContext context) {
        return pop(context, true);
    }

    public IRubyObject pop(ThreadContext context, IRubyObject arg0) {
        return pop(context, !arg0.isTrue());
    }

    public void push(ThreadContext context, final IRubyObject[] args) {
        checkShutdown();
        try {
            context.getThread().executeTask(context, args, putTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in FiberQueue.push");
        }
    }

    private IRubyObject pop(ThreadContext context, boolean should_block) {
        final BlockingQueue<IRubyObject> queue = getQueueSafe();
        if (!should_block && queue.size() == 0) {
            throw new RaiseException(context.runtime, context.runtime.getThreadError(), "queue empty", false);
        }
        try {
            return context.getThread().executeTask(context, this, takeTask);
        } catch (InterruptedException ie) {
            throw context.runtime.newThreadError("interrupted in FiberQueue.pop");
        }
    }
    
}
