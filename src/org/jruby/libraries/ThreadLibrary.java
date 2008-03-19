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

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyBoolean;
import org.jruby.RubyThread;
import org.jruby.RubyInteger;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:mental@rydia.net">MenTaLguY</a>
 */
public class ThreadLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) throws IOException {
        Mutex.setup(runtime);
        ConditionVariable.setup(runtime);
        Queue.setup(runtime);
        SizedQueue.setup(runtime);
    }

    static boolean wait_timeout(IRubyObject o, Double timeout) throws InterruptedException {
        if ( timeout != null ) {
            long delay_ns = (long)(timeout * 1000000000.0);
            long start_ns = System.nanoTime();
            if (delay_ns > 0) {
                long delay_ms = delay_ns / 1000000;
                int delay_ns_remainder = (int)( delay_ns % 1000000 );
                o.wait(delay_ms, delay_ns_remainder);
            }
            long end_ns = System.nanoTime();
            return ( end_ns - start_ns ) <= delay_ns;
        } else {
            o.wait();
            return true;
        }
    }

    @JRubyClass(name="Mutex")
    public static class Mutex extends RubyObject {
        private RubyThread owner = null;

        public static Mutex newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            Mutex result = new Mutex(recv.getRuntime(), (RubyClass)recv);
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
            CallbackFactory cb = runtime.callbackFactory(Mutex.class);
            cMutex.getMetaClass().defineMethod("new", cb.getOptSingletonMethod("newInstance"));
            cMutex.defineFastMethod("locked?", cb.getFastMethod("locked_p"));
            cMutex.defineFastMethod("try_lock", cb.getFastMethod("try_lock"));
            cMutex.defineFastMethod("lock", cb.getFastMethod("lock"));
            cMutex.defineFastMethod("unlock", cb.getFastMethod("unlock"));
            cMutex.defineMethod("synchronize", cb.getMethod("synchronize"));
        }

        public synchronized RubyBoolean locked_p() {
            return ( owner != null ? getRuntime().getTrue() : getRuntime().getFalse() );
        }

        public RubyBoolean try_lock() throws InterruptedException {
            //if (Thread.interrupted()) {
            //    throw new InterruptedException();
            //}
            synchronized (this) {
                if ( owner != null ) {
                    return getRuntime().getFalse();
                }
                lock();
            }
            return getRuntime().getTrue();
        }

        public IRubyObject lock() throws InterruptedException {
            //if (Thread.interrupted()) {
            //    throw new InterruptedException();
            //}
            synchronized (this) {
                try {
                    while ( owner != null ) {
                        wait();
                    }
                    owner = getRuntime().getCurrentContext().getThread();
                } catch (InterruptedException ex) {
                    if ( owner == null ) {
                        notify();
                    }
                    throw ex;
                }
            }
            return this;
        }

        public synchronized RubyBoolean unlock() {
            if ( owner != null ) {
                owner = null;
                notify();
                return getRuntime().getTrue();
            } else {
                return getRuntime().getFalse();
            }
        }

        public IRubyObject synchronize(Block block) throws InterruptedException {
            try {
                lock();
                return block.yield(getRuntime().getCurrentContext(), null);
            } finally {
                unlock();
            }
        }
    }

    @JRubyClass(name="ConditionVariable")
    public static class ConditionVariable extends RubyObject {
        public static ConditionVariable newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            ConditionVariable result = new ConditionVariable(recv.getRuntime(), (RubyClass)recv);
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
            CallbackFactory cb = runtime.callbackFactory(ConditionVariable.class);
            cConditionVariable.getMetaClass().defineMethod("new", cb.getOptSingletonMethod("newInstance"));
            cConditionVariable.defineFastMethod("wait", cb.getFastOptMethod("wait_ruby"));
            cConditionVariable.defineFastMethod("broadcast", cb.getFastMethod("broadcast"));
            cConditionVariable.defineFastMethod("signal", cb.getFastMethod("signal"));
        }

        public IRubyObject wait_ruby(IRubyObject args[]) throws InterruptedException {
            if ( args.length < 1 ) {
                throw getRuntime().newArgumentError(args.length, 1);
            }
            if ( args.length > 2 ) {
                throw getRuntime().newArgumentError(args.length, 2);
            }

            if (!( args[0] instanceof Mutex )) {
                throw getRuntime().newTypeError(args[0], getRuntime().fastGetClass("Mutex"));
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
                    mutex.unlock();
                    try {
                        success = ThreadLibrary.wait_timeout(this, timeout);
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
                mutex.lock();
            }
            if (timeout != null) {
                return getRuntime().newBoolean(success);
            } else {
                // backwards-compatibility
                return getRuntime().getNil();
            }
        }

        public synchronized IRubyObject broadcast() {
            notifyAll();
            return getRuntime().getNil();
        }

        public synchronized IRubyObject signal() {
            notify();
            return getRuntime().getNil();
        }
    }

    @JRubyClass(name="Queue")
    public static class Queue extends RubyObject {
        private LinkedList entries;

        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            Queue result = new Queue(recv.getRuntime(), (RubyClass)recv);
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
            CallbackFactory cb = runtime.callbackFactory(Queue.class);
            cQueue.getMetaClass().defineMethod("new", cb.getOptSingletonMethod("newInstance"));

            cQueue.defineFastMethod("clear", cb.getFastMethod("clear"));
            cQueue.defineFastMethod("empty?", cb.getFastMethod("empty_p"));
            cQueue.defineFastMethod("length", cb.getFastMethod("length"));
            cQueue.defineFastMethod("num_waiting", cb.getFastMethod("num_waiting"));
            cQueue.defineFastMethod("pop", cb.getFastOptMethod("pop"));
            cQueue.defineFastMethod("push", cb.getFastMethod("push", IRubyObject.class));
            
            cQueue.defineAlias("<<", "push");
            cQueue.defineAlias("deq", "pop");
            cQueue.defineAlias("shift", "pop");
            cQueue.defineAlias("size", "length");
            cQueue.defineAlias("enq", "push");
        }

        public synchronized IRubyObject clear() {
            entries.clear();
            return getRuntime().getNil();
        }

        public synchronized RubyBoolean empty_p() {
            return ( entries.size() == 0 ? getRuntime().getTrue() : getRuntime().getFalse() );
        }

        public synchronized RubyNumeric length() {
            return RubyNumeric.int2fix(getRuntime(), entries.size());
        }

        public RubyNumeric num_waiting() { return getRuntime().newFixnum(0); }

        public synchronized IRubyObject pop(IRubyObject[] args) {
            boolean should_block = true;
            if ( Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 1 ) {
                should_block = !args[0].isTrue();
            }
            if ( !should_block && entries.size() == 0 ) {
                throw new RaiseException(getRuntime(), getRuntime().fastGetClass("ThreadError"), "queue empty", false);
            }
            while ( entries.size() == 0 ) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            return (IRubyObject)entries.removeFirst();
        }

        public synchronized IRubyObject push(IRubyObject value) {
            entries.addLast(value);
            notify();
            return getRuntime().getNil();
        }
    }


    @JRubyClass(name="SizedQueue", parent="Queue")
    public static class SizedQueue extends Queue {
        private int capacity;

        public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            SizedQueue result = new SizedQueue(recv.getRuntime(), (RubyClass)recv);
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
            CallbackFactory cb = runtime.callbackFactory(SizedQueue.class);
            cSizedQueue.getMetaClass().defineMethod("new", cb.getOptSingletonMethod("newInstance"));

            cSizedQueue.defineFastMethod("initialize", cb.getFastMethod("max_set", RubyInteger.class));

            cSizedQueue.defineFastMethod("clear", cb.getFastMethod("clear"));
            cSizedQueue.defineFastMethod("max", cb.getFastMethod("max"));
            cSizedQueue.defineFastMethod("max=", cb.getFastMethod("max_set", RubyInteger.class));
            cSizedQueue.defineFastMethod("pop", cb.getFastOptMethod("pop"));
            cSizedQueue.defineFastMethod("push", cb.getFastMethod("push", IRubyObject.class));

            cSizedQueue.defineAlias("<<", "push");
            cSizedQueue.defineAlias("deq", "pop");
            cSizedQueue.defineAlias("shift", "pop");
        }

        public synchronized IRubyObject clear() {
            super.clear();
            notifyAll();
            return getRuntime().getNil();
        }

        public synchronized RubyNumeric max() {
            return RubyNumeric.int2fix(getRuntime(), capacity);
        }

        public synchronized IRubyObject max_set(RubyInteger arg) {
            int new_capacity = RubyNumeric.fix2int(arg);
            if ( new_capacity <= 0 ) {
                getRuntime().newArgumentError("queue size must be positive");
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
            return getRuntime().getNil();
        }

        public synchronized IRubyObject pop(IRubyObject args[]) {
            IRubyObject result = super.pop(args);
            notifyAll();
            return result;
        }

        public synchronized IRubyObject push(IRubyObject value) {
            while ( RubyNumeric.fix2int(length()) >= capacity ) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            super.push(value);
            notifyAll();
            return getRuntime().getNil();
        }
    }
}
