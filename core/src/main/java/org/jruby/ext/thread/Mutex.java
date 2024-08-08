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

import java.util.concurrent.locks.Condition;
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
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

/**
 * The "Mutex" class from the 'thread' library.
 */
@JRubyClass(name = "Mutex")
public class Mutex extends RubyObject implements DataType {
    final ReentrantLock lock = new ReentrantLock();
    RubyThread heldBy;

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static Mutex newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Mutex result = new Mutex(context.runtime, (RubyClass) recv);
        result.callInit(context, args, block);
        return result;
    }

    public Mutex(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static RubyClass setup(RubyClass threadClass, RubyClass objectClass) {
        RubyClass cMutex = threadClass.defineClassUnder("Mutex", objectClass, Mutex::new);

        cMutex.setReifiedClass(Mutex.class);
        cMutex.defineAnnotatedMethods(Mutex.class);

        objectClass.setConstant("Mutex", cMutex);

        return cMutex;
    }

    @JRubyMethod(name = "locked?")
    public RubyBoolean locked_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isLocked());
    }

    public boolean isLocked() {
        return lock.isLocked();
    }

    @JRubyMethod
    public RubyBoolean try_lock(ThreadContext context) {
        return RubyBoolean.newBoolean(context, tryLock(context));
    }

    public boolean tryLock(ThreadContext context) {
        if (lock.isHeldByCurrentThread()) {
            return false;
        }
        return context.getThread().tryLock(lock);
    }

    @JRubyMethod
    public IRubyObject lock(ThreadContext context) {
        RubyThread thread = context.getThread();

        checkRelocking(context);

        // try locking without sleep status to avoid looking like blocking
        if (!thread.tryLock(lock)) {
            for (;;) {
                try {
                    context.getThread().lockInterruptibly(lock);
                    return this;
                } catch (InterruptedException ex) {
                    /// ignore, check thread events and try again!
                    context.pollThreadEvents();
                }
            }
        }

        heldBy = context.getFiberCurrentThread();

        return this;
    }

    @JRubyMethod
    public IRubyObject unlock(ThreadContext context) {
        if (!isLocked()) {
            throw context.runtime.newThreadError("Mutex is not locked");
        }
        if (!isHeldByCurrentThread(context)) {
            throw context.runtime.newThreadError("Mutex is not owned by calling thread");
        }

        heldBy = null;

        boolean hasQueued = lock.hasQueuedThreads();
        context.getThread().unlock(lock);
        return hasQueued ? context.nil : this;
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context) {
        return sleep(context, context.nil);
    }

    @JRubyMethod
    public IRubyObject sleep(ThreadContext context, IRubyObject timeout) {
        Ruby runtime = context.runtime;

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
            throw runtime.newThreadError("Attempt to unlock a mutex which is not locked");
        } catch (InterruptedException ex) {
            context.pollThreadEvents();
        }

        return runtime.newFixnum((System.currentTimeMillis() - beg) / 1000);
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
        return RubyBoolean.newBoolean(context, lock.isHeldByCurrentThread());
    }

    private void checkRelocking(ThreadContext context) {
        if (isHeldByCurrentThread(context)) {
            throw context.runtime.newThreadError("Mutex relocking by same thread");
        }
    }

    private boolean isHeldByCurrentThread(ThreadContext context) {
        return context.getFiberCurrentThread() == heldBy;
    }

}
