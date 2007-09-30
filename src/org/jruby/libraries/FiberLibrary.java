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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.libraries;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A basic implementation of Ruby 1.9 Fiber library.
 */
public class FiberLibrary implements Library {
    public void load(final Ruby runtime) throws IOException {
        Fiber.setup(runtime);
    }

    public static class Fiber extends RubyObject {
        private Block block;
        private Object yieldLock = new Object();
        private IRubyObject result;
        private Thread thread;
        private boolean alive = false;
        
        public static Fiber newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
            Fiber result = new Fiber(recv.getRuntime(), (RubyClass)recv);
            result.initialize(args, block);
            return result;
        }
        
        public IRubyObject initialize(final IRubyObject[] args, Block block) {
            this.block = block;
            final Ruby runtime = getRuntime();
            this.result = runtime.getNil();
            this.thread = new Thread() {
                public void run() {
                    synchronized (yieldLock) {
                        alive = true;
                        ThreadContext context = runtime.getCurrentContext();
                        context.setFiber(Fiber.this);
                        try {
                            result = Fiber.this.block.yield(runtime.getCurrentContext(), result, null, null, true);
                        } finally {
                            yieldLock.notify();
                        }
                    }
                }
            };
            // FIXME: Is this appropriate? Should still-running fibers just die on exit?
            this.thread.setDaemon(true);
            return this;
        }

        public Fiber(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        public static void setup(Ruby runtime) {
            RubyClass cFiber = runtime.defineClass("Fiber", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
            CallbackFactory cb = runtime.callbackFactory(Fiber.class);
            cFiber.getMetaClass().defineMethod("new", cb.getOptSingletonMethod("newInstance"));
            cFiber.defineFastMethod("resume", cb.getFastOptMethod("resume"));
            // FIXME: Not sure what the semantics of transfer are
            //cFiber.defineFastMethod("transfer", cb.getFastOptMethod("transfer"));
            cFiber.defineFastMethod("alive?", cb.getFastMethod("alive_p"));
            cFiber.getMetaClass().defineFastMethod("yield", cb.getFastSingletonMethod("yield", IRubyObject.class));
            cFiber.getMetaClass().defineFastMethod("current", cb.getFastSingletonMethod("current"));
        }

        public IRubyObject resume(IRubyObject[] args) throws InterruptedException {
            synchronized (yieldLock) {
                result = getRuntime().newArrayNoCopyLight(args);
                if (!alive) {
                    thread.start();
                    yieldLock.wait();
                } else {
                    yieldLock.notify();
                    yieldLock.wait();
                }
            }
            return result;
        }

        public IRubyObject transfer(IRubyObject[] args) throws InterruptedException {
            synchronized (yieldLock) {
                yieldLock.notify();
                yieldLock.wait();
            }
            return result;
        }

        public IRubyObject alive_p() {
            return getRuntime().newBoolean(alive);
        }

        public static IRubyObject yield(IRubyObject recv, IRubyObject value) throws InterruptedException {
            Fiber fiber = recv.getRuntime().getCurrentContext().getFiber();
            fiber.result = value;
            synchronized (fiber.yieldLock) {
                fiber.yieldLock.notify();
                fiber.yieldLock.wait();
            }
            return recv.getRuntime().getNil();
        }

        public static IRubyObject current(IRubyObject recv) {
            return recv.getRuntime().getCurrentContext().getFiber();
        }
    }
}
