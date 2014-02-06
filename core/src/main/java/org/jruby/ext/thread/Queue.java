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

import java.util.LinkedList;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The "Queue" class from the 'thread' library.
 */
@JRubyClass(name = "Queue")
public class Queue extends RubyObject {
    private LinkedList entries;
    protected volatile int numWaiting = 0;

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Queue result = new Queue(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public Queue(Ruby runtime, RubyClass type) {
        super(runtime, type);
        entries = new LinkedList();
    }

    public static void setup(Ruby runtime) {
        RubyClass cQueue = runtime.defineClass("Queue", runtime.getObject(), new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Queue(runtime, klass);
            }
        });
        cQueue.setReifiedClass(Queue.class);
        cQueue.defineAnnotatedMethods(Queue.class);
    }

    @JRubyMethod(name = "shutdown!")
    public synchronized IRubyObject shutdown(ThreadContext context) {
        entries = null;
        notifyAll();
        return context.runtime.getNil();
    }
    
    public synchronized void shutdown() {
        entries = null;
        notifyAll();
    }

    public boolean isShutdown() {
        return entries == null;
    }

    public synchronized void checkShutdown(ThreadContext context) {
        if (entries == null) {
            throw new RaiseException(context.runtime, context.runtime.getThreadError(), "queue shut down", false);
        }
    }

    @JRubyMethod
    public synchronized IRubyObject clear(ThreadContext context) {
        checkShutdown(context);
        entries.clear();
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "empty?")
    public synchronized RubyBoolean empty_p(ThreadContext context) {
        checkShutdown(context);
        return context.runtime.newBoolean(entries.size() == 0);
    }

    @JRubyMethod(name = {"length", "size"})
    public synchronized RubyNumeric length(ThreadContext context) {
        checkShutdown(context);
        return RubyNumeric.int2fix(context.runtime, entries.size());
    }

    protected synchronized long java_length() {
        return entries.size();
    }

    @JRubyMethod
    public RubyNumeric num_waiting(ThreadContext context) {
        return context.runtime.newFixnum(numWaiting);
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public synchronized IRubyObject pop(ThreadContext context) {
        return pop(context, true);
    }

    @JRubyMethod(name = {"pop", "deq", "shift"})
    public synchronized IRubyObject pop(ThreadContext context, IRubyObject arg0) {
        return pop(context, !arg0.isTrue());
    }

    @JRubyMethod(name = {"push", "<<", "enq"})
    public synchronized IRubyObject push(ThreadContext context, IRubyObject value) {
        checkShutdown(context);
        entries.addLast(value);
        notify();
        return context.runtime.getNil();
    }

    private synchronized IRubyObject pop(ThreadContext context, boolean should_block) {
        checkShutdown(context);
        if (!should_block && entries.size() == 0) {
            throw new RaiseException(context.runtime, context.runtime.getThreadError(), "queue empty", false);
        }
        numWaiting++;
        try {
            while (java_length() == 0) {
                try {
                    context.getThread().wait_timeout(this, null);
                } catch (InterruptedException e) {
                }
                checkShutdown(context);
            }
        } finally {
            numWaiting--;
        }
        return (IRubyObject) entries.removeFirst();
    }
    
}
