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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Visibility;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The "SizedQueue" class from the 'thread' library.
 */
@JRubyClass(name = "SizedQueue", parent = "Queue")
public class SizedQueue extends Queue {
    public SizedQueue(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public SizedQueue(Ruby runtime, RubyClass type, int size) {
        super(runtime, type);

        this.queue = new ArrayBlockingQueue<IRubyObject>(size, false);
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
        super.clear(context);

        return this;
    }

    @JRubyMethod
    public RubyNumeric max(ThreadContext context) {
        return RubyNumeric.int2fix(context.runtime, queue.size() + queue.remainingCapacity());
    }

    @JRubyMethod(name = "max=")
    public synchronized IRubyObject max_set(ThreadContext context, IRubyObject arg) {
        BlockingQueue<IRubyObject> oldQueue = this.queue;
        initialize(context, arg);
        oldQueue.drainTo(this.queue);
        return arg;
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public synchronized IRubyObject initialize(ThreadContext context, IRubyObject arg) {
        int new_capacity = RubyNumeric.fix2int(arg);

        if (new_capacity <= 0) {
            throw context.runtime.newArgumentError("queue size must be positive");
        }

        this.queue = new ArrayBlockingQueue<IRubyObject>(new_capacity, false);

        return this;
    }
}
