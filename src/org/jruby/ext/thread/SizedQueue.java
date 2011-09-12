/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.thread;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The "SizedQueue" class from the 'thread' library.
 */
@JRubyClass(name = "SizedQueue", parent = "Queue")
public class SizedQueue extends Queue {
    private int capacity;

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        SizedQueue result = new SizedQueue(context.getRuntime(), (RubyClass) recv);
        result.callInit(args, block);
        return result;
    }

    public SizedQueue(Ruby runtime, RubyClass type) {
        super(runtime, type);
        capacity = 1;
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
    public synchronized IRubyObject clear(ThreadContext context) {
        super.clear(context);
        notifyAll();
        return context.getRuntime().getNil();
    }

    @JRubyMethod
    public synchronized RubyNumeric max(ThreadContext context) {
        return RubyNumeric.int2fix(context.getRuntime(), capacity);
    }

    @JRubyMethod(name = {"max=", "initialize"})
    public synchronized IRubyObject max_set(ThreadContext context, IRubyObject arg) {
        int new_capacity = RubyNumeric.fix2int(arg);
        if (new_capacity <= 0) {
            context.getRuntime().newArgumentError("queue size must be positive");
        }
        int difference;
        if (new_capacity > capacity) {
            difference = new_capacity - capacity;
        } else {
            difference = 0;
        }
        capacity = new_capacity;
        if (difference > 0) {
            notifyAll();
        }
        return context.getRuntime().getNil();
    }

    @JRubyMethod(name = {"pop", "deq", "shift"}, optional = 1)
    @Override
    public synchronized IRubyObject pop(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = super.pop(context, args);
        notifyAll();
        return result;
    }

    @JRubyMethod(name = {"push", "<<"})
    @Override
    public synchronized IRubyObject push(ThreadContext context, IRubyObject value) {
        checkShutdown(context);
        if (java_length() >= capacity) {
            numWaiting++;
            try {
                while (java_length() >= capacity) {
                    try {
                        context.getThread().wait_timeout(this, null);
                    } catch (InterruptedException e) {
                    }
                    checkShutdown(context);
                }
            } finally {
                numWaiting--;
            }
        }
        super.push(context, value);
        notifyAll();
        return context.getRuntime().getNil();
    }
    
}
