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

import java.util.concurrent.locks.ReentrantLock;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The "Mutex" class from the 'thread' library.
 */
@JRubyClass(name = "Mutex")
public class Mutex extends RubyObject {
    ReentrantLock lock = new ReentrantLock();

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static Mutex newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Mutex result = new Mutex(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public Mutex(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static void setup(Ruby runtime) {
        RubyClass cMutex = runtime.defineClass("Mutex", runtime.getObject(), new ObjectAllocator() {

            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new Mutex(runtime, klass);
            }
        });
        cMutex.setReifiedClass(Mutex.class);
        cMutex.defineAnnotatedMethods(Mutex.class);
    }

    @JRubyMethod(name = "locked?")
    public synchronized RubyBoolean locked_p(ThreadContext context) {
        return context.runtime.newBoolean(lock.isLocked());
    }

    @JRubyMethod
    public RubyBoolean try_lock(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            return context.runtime.getFalse();
        }
        return context.runtime.newBoolean(context.getThread().tryLock(lock));
    }

    @JRubyMethod
    public IRubyObject lock(ThreadContext context) {
        try {
            context.getThread().enterSleep();
            try {
                checkRelocking(context);
                context.getThread().lockInterruptibly(lock);
            } catch (InterruptedException ex) {
                context.pollThreadEvents();
                throw context.runtime.newConcurrencyError("interrupted waiting for mutex: " + ex.getMessage());
            }
        } finally {
            context.getThread().exitSleep();
        }
        return this;
    }

    @JRubyMethod
    public synchronized IRubyObject unlock(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (!lock.isLocked()) {
            throw runtime.newThreadError("Mutex is not locked");
        }
        if (!lock.isHeldByCurrentThread()) {
            throw runtime.newThreadError("Mutex is not owned by calling thread");
        }
        
        boolean hasQueued = lock.hasQueuedThreads();
        context.getThread().unlock(lock);
        return hasQueued ? context.nil : this;
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject sleep(ThreadContext context) {
        long beg = System.currentTimeMillis();
        try {
            unlock(context);
            context.getThread().sleep(-1);
        } catch (InterruptedException ex) {
            // ignore interrupted
        } finally {
            lock(context);
        }
        return context.runtime.newFixnum((System.currentTimeMillis() - beg) / 1000);
    }

    @JRubyMethod(compat = CompatVersion.RUBY1_9)
    public IRubyObject sleep(ThreadContext context, IRubyObject timeout) {
        long beg = System.currentTimeMillis();
        try {
            unlock(context);
            context.getThread().sleep((long) (timeout.convertToFloat().getDoubleValue() * 1000));
        } catch (InterruptedException ex) {
            // ignore interrupted
        } finally {
            lock(context);
        }
        return context.runtime.newFixnum((System.currentTimeMillis() - beg) / 1000);
    }

    @JRubyMethod
    public IRubyObject synchronize(ThreadContext context, Block block) {
        lock(context);
        try {
            return block.yield(context, null);
        } finally {
            unlock(context);
        }
    }

    private void checkRelocking(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            throw context.runtime.newThreadError("Mutex relocking by same thread");
        }
    }
    
}
