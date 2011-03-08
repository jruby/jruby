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

import java.util.WeakHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyClass;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;

import static org.jruby.runtime.Visibility.*;

/**
 * A basic implementation of Ruby 1.9 Fiber library.
 */
public class FiberLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) {
        RubyClass cFiber = runtime.defineClass("Fiber", runtime.getObject(), new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new Fiber(runtime, klazz);
            }
        });

        cFiber.defineAnnotatedMethods(Fiber.class);
        cFiber.defineAnnotatedMethods(FiberMeta.class);

        if (runtime.getExecutor() != null) {
            executor = runtime.getExecutor();
        } else {
            executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
        }
    }

    private Executor executor;

    public enum FiberState {
        NOT_STARTED,
        STARTED,
        YIELDED,
        RUNNING,
        FINISHED
    }

    @JRubyClass(name="Fiber")
    public class Fiber extends RubyObject implements ExecutionContext {
        private final SynchronousQueue<IRubyObject> yield = new SynchronousQueue<IRubyObject>();
        private final SynchronousQueue<IRubyObject> resume = new SynchronousQueue<IRubyObject>();
        private final Map<Object, IRubyObject> contextVariables = new WeakHashMap<Object, IRubyObject>();
        private Block block;
        private IRubyObject result;
        private RubyThread parent;
        private Runnable runnable;
        private FiberState state = FiberState.NOT_STARTED;

        @JRubyMethod(rest = true, visibility = PRIVATE)
        public IRubyObject initialize(ThreadContext context, final IRubyObject[] args, Block block) {
            final Ruby runtime = context.getRuntime();

            if (block == null || !block.isGiven()) {
                throw runtime.newArgumentError("tried to create Proc object without a block");
            }

            this.block = block;
            this.parent = context.getThread();
            this.result = runtime.getNil();
            this.runnable = new Runnable() {
                public void run() {
                    ThreadContext context = runtime.getCurrentContext();
                    context.setFiber(Fiber.this);
                    try {
                        state = FiberState.STARTED;
                        result = resume.take();
                        state = FiberState.RUNNING;
                        result = Fiber.this.block.yieldArray(context, result, null, null);
                        state = FiberState.FINISHED;
                        yield.put(result);
                    } catch (JumpException.RetryJump rtry) {
                        // FIXME: technically this should happen before the block is executed
                        parent.raise(new IRubyObject[] {runtime.newSyntaxError("Invalid retry").getException()}, Block.NULL_BLOCK);
                        parent.getNativeThread().interrupt();
                    } catch (JumpException.BreakJump brk) {
                        parent.raise(new IRubyObject[] {runtime.newLocalJumpError(Reason.BREAK, runtime.getNil(), "break from proc-closure").getException()}, Block.NULL_BLOCK);
                        parent.getNativeThread().interrupt();
                    } catch (JumpException.ReturnJump ret) {
                        parent.raise(new IRubyObject[] {runtime.newLocalJumpError(Reason.RETURN, runtime.getNil(), "unexpected return").getException()}, Block.NULL_BLOCK);
                        parent.getNativeThread().interrupt();
                    } catch (RaiseException re) {
                        // re-raise exception in parent thread
                        parent.raise(new IRubyObject[] {re.getException()}, Block.NULL_BLOCK);
                        parent.getNativeThread().interrupt();
                    } catch (InterruptedException ie) {
                        context.pollThreadEvents();
                        throw context.getRuntime().newConcurrencyError(ie.getLocalizedMessage());
                    } finally {
                        state = FiberState.FINISHED;
                    }
                }
            };
            // FIXME: Make thread pool threads daemons if necessary
            return this;
        }

        public Fiber(Ruby runtime, RubyClass type) {
            super(runtime, type);
        }

        @JRubyMethod(rest = true, compat = CompatVersion.RUBY1_9)
        public IRubyObject resume(ThreadContext context, IRubyObject[] args) {
            // FIXME: Broken but behaving
            IRubyObject result;
            if (args.length == 0) {
                result = context.getRuntime().getNil();
            } else if (args.length == 1) {
                result = args[0];
            } else {
                result = context.getRuntime().newArrayNoCopyLight(args);
            }
            try {
                switch (state) {
                case NOT_STARTED:
                    executor.execute(runnable);
                case YIELDED:
                    resume.put(result);
                    result = yield.take();
                    context.pollThreadEvents();
                    return result;
                case RUNNING:
                    throw context.getRuntime().newFiberError("double resume");
                case FINISHED:
                    throw context.getRuntime().newFiberError("dead fiber called");
                default:
                    throw context.getRuntime().newFiberError("fiber in an unknown state");
                }
            } catch (OutOfMemoryError oome) {
                if (oome.getMessage().equals("unable to create new native thread")) {
                    throw context.runtime.newThreadError("too many threads, can't create a new Fiber");
                }
                throw oome;
            } catch (InterruptedException ie) {
                context.pollThreadEvents();
                throw context.getRuntime().newConcurrencyError(ie.getLocalizedMessage());
            }
        }

        // This should only be defined after require 'fiber'
        @JRubyMethod(name = "transfer", rest = true, compat = CompatVersion.RUBY1_9)
        public IRubyObject transfer(ThreadContext context, IRubyObject[] args) {
            // FIXME: transfer is resume but with some sort of fiber affinity
            return resume(context, args);
        }

        // This should only be defined after require 'fiber'
        @JRubyMethod(name = "alive?", compat = CompatVersion.RUBY1_9)
        public IRubyObject alive_p(ThreadContext context) {
            return context.getRuntime().newBoolean(state != FiberState.FINISHED);
        }

        public Map<Object, IRubyObject> getContextVariables() {
            return contextVariables;
        }
    }

    public static class FiberMeta {
        @JRubyMethod(compat = CompatVersion.RUBY1_9, rest = true, meta = true)
        public static IRubyObject yield(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
            Fiber fiber = context.getFiber();
            if (fiber == null) {
                throw context.getRuntime().newFiberError("can't yield from root fiber");
            }

            if (args.length == 1) {
                fiber.result = args[0];
            } else if (args.length > 0) {
                fiber.result = context.getRuntime().newArrayNoCopyLight(args);
            }
            try {
                fiber.state = FiberState.YIELDED;
                fiber.yield.put(fiber.result);
                fiber.result = fiber.resume.take();
                context.pollThreadEvents();
                fiber.state = FiberState.RUNNING;
            } catch (InterruptedException ie) {
                context.pollThreadEvents();
                throw context.getRuntime().newConcurrencyError(ie.getLocalizedMessage());
            }
            return fiber.result;
        }
    }
}
