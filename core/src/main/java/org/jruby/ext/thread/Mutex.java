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

import java.util.concurrent.locks.ReentrantLock;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyThread;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;

/**
 * The "Mutex" class from the 'thread' library.
 */
@JRubyClass(name = "Mutex")
public class Mutex extends RubyObject implements DataType {
    final ReentrantLock lock = new ReentrantLock();
    /**
     * The non-fiber thread that currently holds the lock; this will be the same for all fibers associated with that
     * thread.
     */
    volatile RubyThread lockingThread;

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static Mutex newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Mutex result = new Mutex(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public Mutex(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass setup(ThreadContext context, RubyClass Thread, RubyClass Object) {
        return (RubyClass) Object.setConstant("Mutex",
                Thread.defineClassUnder(context, "Mutex", Object, Mutex::new).reifiedClass(Mutex.class).defineMethods(context, Mutex.class));
    }

    @JRubyMethod(name = "locked?")
    public RubyBoolean locked_p(ThreadContext context) {
        return asBoolean(context, isLocked());
    }

    public boolean isLocked() {
        return lock.isLocked();
    }

    @JRubyMethod
    public RubyBoolean try_lock(ThreadContext context) {
        return asBoolean(context, tryLock(context));
    }

    public boolean tryLock(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            return false;
        }
        boolean locked = context.getThread().tryLock(lock);

        if (locked) this.lockingThread = context.getThread();

        return locked;
    }

    @JRubyMethod
    public IRubyObject lock(ThreadContext context) {
        RubyThread thread = context.getThread();
        RubyThread parentThread = context.getFiberCurrentThread();

        checkRelocking(context);

        if (this.lockingThread == parentThread) {
            throw context.runtime.newThreadError("deadlock; lock already owned by another fiber belonging to the same thread");
        }

        // try locking without sleep status to avoid looking like blocking
        if (!thread.tryLock(lock)) {
            for (;;) {
                try {
                    context.getThread().lockInterruptibly(lock);
                    break;
                } catch (InterruptedException ex) {
                    /// ignore, check thread events and try again!
                    context.pollThreadEvents();
                }
            }
        }

        this.lockingThread = thread;

        // always check for thread interrupts after acquiring lock
        thread.pollThreadEvents(context);

        return this;
    }

    @JRubyMethod
    public IRubyObject unlock(ThreadContext context) {
        if (!isLocked()) {
            throw context.runtime.newThreadError("Mutex is not locked");
        }
        if (!lock.isHeldByCurrentThread()) {
            throw context.runtime.newThreadError("Mutex is not owned by calling thread");
        }

        boolean hasQueued = lock.hasQueuedThreads();
        this.lockingThread = null;
        context.getThread().unlock(lock);
        return hasQueued ? context.nil : this;
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context) {
        return sleep(context, context.nil);
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context, IRubyObject timeout) {
        final long beg = System.currentTimeMillis();

        try {
            RubyThread thread = context.getThread();

            if (timeout.isNil()) {
                thread.sleep(lock);
            } else {
                double t = RubyTime.convertTimeInterval(context, timeout);
                long millis = (long) (t * 1000);

                if (Double.compare(t, 0.0d) == 0 || millis == 0) {
                    // wait time is zero or smaller than 1ms, so we just proceed
                } else {
                    thread.sleep(lock, millis);
                }
            }
        } catch (IllegalMonitorStateException imse) {
            throw context.runtime.newThreadError("Attempt to unlock a mutex which is not locked");
        } catch (InterruptedException ex) {
            context.pollThreadEvents();
        }

        return asFixnum(context, (System.currentTimeMillis() - beg) / 1000);
    }

    @JRubyMethod
    public IRubyObject synchronize(ThreadContext context, Block block) {
        lock(context);
        try {
            return block.yieldSpecific(context);
        } finally {
            unlock(context);
        }
    }

    @JRubyMethod(name = "owned?")
    public IRubyObject owned_p(ThreadContext context) {
        return asBoolean(context, lock.isHeldByCurrentThread());
    }

    private void checkRelocking(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            throw context.runtime.newThreadError("Mutex relocking by same thread");
        }
    }

}
