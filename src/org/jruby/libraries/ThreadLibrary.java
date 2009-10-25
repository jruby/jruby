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

/* Portions loosely based on public-domain JSR-166 code by Doug Lea et al. */

package org.jruby.libraries;

import java.io.IOException;
import java.util.LinkedList;

import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyBoolean;
import org.jruby.RubyThread;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:mental@rydia.net">MenTaLguY</a>
 */
public class ThreadLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        runtime.getThread().defineAnnotatedMethods(ThreadMethods.class);
        Mutex.setup(runtime);
        ConditionVariable.setup(runtime);
        Queue.setup(runtime);
        SizedQueue.setup(runtime);
    }

    public static class ThreadMethods {
        @JRubyMethod(name = "exclusive", meta = true, compat = CompatVersion.RUBY1_8)
        public static IRubyObject exclusive(ThreadContext context, IRubyObject receiver, Block block) {
            ThreadService service  = context.getRuntime().getThreadService();
            boolean old = service.getCritical();
            try {
                service.setCritical(true);
                return block.yield(receiver.getRuntime().getCurrentContext(), (IRubyObject) null);
            } finally {
                service.setCritical(old);
            }
        }
    }

    @JRubyClass(name="Mutex")
    public static class Mutex extends RubyObject {
        private RubyThread owner = null;

        @JRubyMethod(name = "new", rest = true, meta = true)
        public static Mutex newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            Mutex result = new Mutex(context.getRuntime(), (RubyClass)recv);
            result.callInit(args, block);
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
            cMutex.defineAnnotatedMethods(Mutex.class);
        }

        @JRubyMethod(name = "locked?")
        public synchronized RubyBoolean locked_p(ThreadContext context) {
            return context.getRuntime().newBoolean(owner != null);
        }

        @JRubyMethod
        public RubyBoolean try_lock(ThreadContext context) throws InterruptedException {
            //if (Thread.interrupted()) {
            //    throw new InterruptedException();
            //}
            synchronized (this) {
                if ( owner != null ) {
                    return context.getRuntime().getFalse();
                }
                lock(context);
            }
            return context.getRuntime().getTrue();
        }

        @JRubyMethod(frame = true)
        public IRubyObject lock(ThreadContext context) throws InterruptedException {
            //if (Thread.interrupted()) {
            //    throw new InterruptedException();
            //}
            try {
                context.getThread().enterSleep();
                synchronized (this) {
                    try {
                        if (owner == context.getThread()) {
                            throw context.getRuntime().newThreadError("Mutex relocking by same thread");
                        }
                        while ( owner != null ) {
                            wait();
                        }
                        owner = context.getThread();
                    } catch (InterruptedException ex) {
                        if ( owner == null ) {
                            notify();
                        }
                        throw ex;
                    }
                }
            } finally {
                context.getThread().exitSleep();
            }
            return this;
        }

        @JRubyMethod
        public synchronized RubyBoolean unlock(ThreadContext context) {
            if ( owner != null ) {
                owner = null;
                notify();
                return context.getRuntime().getTrue();
            } else {
                return context.getRuntime().getFalse();
            }
        }

        @JRubyMethod
        public IRubyObject synchronize(ThreadContext context, Block block) throws InterruptedException {
            try {
                lock(context);
                return block.yield(context, null);
            } finally {
                unlock(context);
            }
        }
    }

    @JRubyClass(name="ConditionVariable")
    public static class ConditionVariable extends RubyObject {
        @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
        public static ConditionVariable newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            ConditionVariable result = new ConditionVariable(context.getRuntime(), (RubyClass)recv);
            result.callInit(args, block);
            return result;
        }

        public ConditionVariable(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        public static void setup(Ruby runtime) {
            RubyClass cConditionVariable = runtime.defineClass("ConditionVariable", runtime.getObject(), new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                    return new ConditionVariable(runtime, klass);
                }
            });
            
            cConditionVariable.defineAnnotatedMethods(ConditionVariable.class);
        }

        @JRubyMethod(name = "wait", required = 1, optional = 1)
        public IRubyObject wait_ruby(ThreadContext context, IRubyObject args[]) throws InterruptedException {
            if ( args.length < 1 ) {
                throw context.getRuntime().newArgumentError(args.length, 1);
            }
            if ( args.length > 2 ) {
                throw context.getRuntime().newArgumentError(args.length, 2);
            }

            if (!( args[0] instanceof Mutex )) {
                throw context.getRuntime().newTypeError(args[0], context.getRuntime().fastGetClass("Mutex"));
            }
            Mutex mutex = (Mutex)args[0];

            Double timeout = null;
            if ( args.length > 1 && !args[1].isNil() ) {
                timeout = args[1].convertToFloat().getDoubleValue();
            }

            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            boolean success = false;
            try {
                synchronized (this) {
                    mutex.unlock(context);
                    try {
                        success = context.getThread().wait_timeout(this, timeout);
                    } finally {
                        // An interrupt or timeout may have caused us to miss
                        // a notify that we consumed, so do another notify in
                        // case someone else is available to pick it up.
                        if (!success) {
                            this.notify();
                        }
                    }
                }
            } finally {
                mutex.lock(context);
            }
            if (timeout != null) {
                return context.getRuntime().newBoolean(success);
            } else {
                return this;
            }
        }

        @JRubyMethod
        public synchronized IRubyObject broadcast(ThreadContext context) {
            notifyAll();
            return this;
        }

        @JRubyMethod
        public synchronized IRubyObject signal(ThreadContext context) {
            notify();
            return this;
        }
    }

    @JRubyClass(name="Queue")
    public static class Queue extends RubyObject {
        private LinkedList entries;
        protected volatile int numWaiting=0;

        @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
        public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            Queue result = new Queue(context.getRuntime(), (RubyClass)recv);
            result.callInit(args, block);
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
            
            cQueue.defineAnnotatedMethods(Queue.class);
        }

        @JRubyMethod(name = "shutdown!")
        public synchronized IRubyObject shutdown(ThreadContext context) {
            entries = null;
            notifyAll();
            return context.getRuntime().getNil();
        }

        public synchronized void checkShutdown(ThreadContext context) {
            if (entries == null) {
                throw new RaiseException(context.getRuntime(), context.getRuntime().getThreadError(), "queue shut down", false);
            }
        }

        @JRubyMethod
        public synchronized IRubyObject clear(ThreadContext context) {
            checkShutdown(context);
            entries.clear();
            return context.getRuntime().getNil();
        }

        @JRubyMethod(name = "empty?")
        public synchronized RubyBoolean empty_p(ThreadContext context) {
            checkShutdown(context);
            return context.getRuntime().newBoolean(entries.size() == 0);
        }

        @JRubyMethod(name = {"length", "size"})
        public synchronized RubyNumeric length(ThreadContext context) {
            checkShutdown(context);
            return RubyNumeric.int2fix(context.getRuntime(), entries.size());
        }

        protected synchronized long java_length() {
            return entries.size();
        }

        @JRubyMethod
        public RubyNumeric num_waiting(ThreadContext context) { return context.getRuntime().newFixnum(numWaiting); }

        @JRubyMethod(name = {"pop", "deq", "shift"}, optional = 1)
        public synchronized IRubyObject pop(ThreadContext context, IRubyObject[] args) {
            checkShutdown(context);
            boolean should_block = true;
            if ( Arity.checkArgumentCount(context.getRuntime(), args, 0, 1) == 1 ) {
                should_block = !args[0].isTrue();
            }
            if ( !should_block && entries.size() == 0 ) {
                throw new RaiseException(context.getRuntime(), context.getRuntime().getThreadError(), "queue empty", false);
            }
            numWaiting++;
            try {
                while ( java_length() == 0 ) {
                    try {
                        context.getThread().wait_timeout(this, null);
                    } catch (InterruptedException e) {
                    }
                    checkShutdown(context);
                }
            } finally {
                numWaiting--;
            }
            return (IRubyObject)entries.removeFirst();
        }

        @JRubyMethod(name = {"push", "<<", "enq"})
        public synchronized IRubyObject push(ThreadContext context, IRubyObject value) {
            checkShutdown(context);
            entries.addLast(value);
            notify();
            return context.getRuntime().getNil();
        }
    }


    @JRubyClass(name="SizedQueue", parent="Queue")
    public static class SizedQueue extends Queue {
        private int capacity;

        @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
        public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
            SizedQueue result = new SizedQueue(context.getRuntime(), (RubyClass)recv);
            result.callInit(args, block);
            return result;
        }

        public SizedQueue(Ruby runtime, RubyClass type) {
            super(runtime, type);
            capacity = 1;
        }

        public static void setup(Ruby runtime) {
            RubyClass cSizedQueue = runtime.defineClass("SizedQueue", runtime.fastGetClass("Queue"), new ObjectAllocator() {
                public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                    return new SizedQueue(runtime, klass);
                }
            });
            
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
            if ( new_capacity <= 0 ) {
                context.getRuntime().newArgumentError("queue size must be positive");
            }
            int difference;
            if ( new_capacity > capacity ) {
                difference = new_capacity - capacity;
            } else {
                difference = 0;
            }
            capacity = new_capacity;
            if ( difference > 0 ) {
                notifyAll();
            }
            return context.getRuntime().getNil();
        }

        @JRubyMethod(name = {"pop", "deq", "shift"}, optional = 1)
        @Override
        public synchronized IRubyObject pop(ThreadContext context, IRubyObject args[]) {
            IRubyObject result = super.pop(context, args);
            notifyAll();
            return result;
        }

        @JRubyMethod(name = {"push", "<<"})
        @Override
        public synchronized IRubyObject push(ThreadContext context, IRubyObject value) {
            checkShutdown(context);
            if ( java_length() >= capacity ) {
                numWaiting++;
                try {
                    while ( java_length() >= capacity ) {
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
}
