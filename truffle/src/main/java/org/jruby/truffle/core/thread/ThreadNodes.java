/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
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
 * Copyright (C) 2002 Jason Voegele <jason@jvoegele.com>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
 */
package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.YieldingCoreMethodNode;
import org.jruby.truffle.collections.Memo;
import org.jruby.truffle.core.InterruptMode;
import org.jruby.truffle.core.exception.ExceptionOperations;
import org.jruby.truffle.core.fiber.FiberManager;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.backtrace.Backtrace;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNode;
import org.jruby.truffle.language.objects.ReadObjectFieldNodeGen;
import org.jruby.truffle.platform.UnsafeGroup;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@CoreClass("Thread")
public abstract class ThreadNodes {

    @CoreMethod(names = "alive?", unsafe = UnsafeGroup.THREADS)
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean alive(DynamicObject thread) {
            final ThreadStatus status = Layouts.THREAD.getStatus(thread);
            return status != ThreadStatus.ABORTING && status != ThreadStatus.DEAD;
        }

    }

    @CoreMethod(names = "backtrace", unsafe = UnsafeGroup.THREADS)
    public abstract static class BacktraceNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject backtrace(DynamicObject rubyThread) {
            final Thread thread = Layouts.FIBER.getThread(Layouts.THREAD.getFiberManager(rubyThread).getCurrentFiber());

            final Memo<DynamicObject> result = new Memo<>(null);

            getContext().getSafepointManager().pauseThreadAndExecute(thread, this, (thread1, currentNode) -> {
                final Backtrace backtrace = getContext().getCallStack().getBacktrace(currentNode);
                result.set(ExceptionOperations.backtraceAsRubyStringArray(getContext(), null, backtrace));
            });

            // If the thread id dead or aborting the SafepointAction will not run

            if (result.get() != null) {
                return result.get();
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject current() {
            return getContext().getThreadManager().getCurrentThread();
        }

    }

    @CoreMethod(names = "group")
    public abstract static class GroupNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject group(DynamicObject thread) {
            return Layouts.THREAD.getThreadGroup(thread);
        }

    }

    @CoreMethod(names = { "kill", "exit", "terminate" }, unsafe = UnsafeGroup.THREADS)
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject kill(final DynamicObject rubyThread) {
            final Thread toKill = Layouts.THREAD.getThread(rubyThread);

            if (toKill == null) {
                // Already dead
                return rubyThread;
            }

            getContext().getSafepointManager().pauseThreadAndExecuteLater(toKill, this, (currentThread, currentNode) -> ThreadManager.shutdown(getContext(), currentThread, currentNode));

            return rubyThread;
        }

    }

    @NonStandard
    @CoreMethod(names = "handle_interrupt", required = 2, needsBlock = true, visibility = Visibility.PRIVATE, unsafe = UnsafeGroup.THREADS)
    public abstract static class HandleInterruptNode extends YieldingCoreMethodNode {

        @CompilationFinal private DynamicObject immediateSymbol;
        @CompilationFinal private DynamicObject onBlockingSymbol;
        @CompilationFinal private DynamicObject neverSymbol;

        private final BranchProfile errorProfile = BranchProfile.create();

        @Specialization(guards = { "isRubyClass(exceptionClass)", "isRubySymbol(timing)" })
        public Object handle_interrupt(VirtualFrame frame, DynamicObject self, DynamicObject exceptionClass, DynamicObject timing, DynamicObject block) {
            // TODO (eregon, 12 July 2015): should we consider exceptionClass?
            final InterruptMode newInterruptMode = symbolToInterruptMode(timing);

            final InterruptMode oldInterruptMode = Layouts.THREAD.getInterruptMode(self);
            Layouts.THREAD.setInterruptMode(self, newInterruptMode);
            try {
                return yield(frame, block);
            } finally {
                Layouts.THREAD.setInterruptMode(self, oldInterruptMode);
            }
        }

        private InterruptMode symbolToInterruptMode(DynamicObject symbol) {
            if (symbol == getImmediateSymbol()) {
                return InterruptMode.IMMEDIATE;
            } else if (symbol == getOnBlockingSymbol()) {
                return InterruptMode.ON_BLOCKING;
            } else if (symbol == getNeverSymbol()) {
                return InterruptMode.NEVER;
            } else {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().argumentError("invalid timing symbol", this));
            }
        }

        private DynamicObject getImmediateSymbol() {
            if (immediateSymbol == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                immediateSymbol = getSymbol("immediate");
            }

            return immediateSymbol;
        }

        private DynamicObject getOnBlockingSymbol() {
            if (onBlockingSymbol == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                onBlockingSymbol = getSymbol("on_blocking");
            }

            return onBlockingSymbol;
        }

        private DynamicObject getNeverSymbol() {
            if (neverSymbol == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                neverSymbol = getSymbol("never");
            }

            return neverSymbol;
        }

    }

    @CoreMethod(names = "initialize", rest = true, needsBlock = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject initialize(DynamicObject thread, Object[] arguments, DynamicObject block) {
            ThreadManager.initialize(thread, getContext(), this, arguments, block);
            return nil();
        }

    }

    @CoreMethod(names = "join", optional = 1, lowerFixnum = 1, unsafe = UnsafeGroup.THREADS)
    public abstract static class JoinNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject join(DynamicObject thread, NotProvided timeout) {
            doJoin(this, thread);
            return thread;
        }

        @Specialization(guards = "isNil(nil)")
        public DynamicObject join(DynamicObject thread, Object nil) {
            return join(thread, NotProvided.INSTANCE);
        }

        @Specialization
        public Object join(DynamicObject thread, int timeout) {
            return joinMillis(thread, timeout * 1000);
        }

        @Specialization
        public Object join(DynamicObject thread, double timeout) {
            return joinMillis(thread, (int) (timeout * 1000.0));
        }

        private Object joinMillis(DynamicObject self, int timeoutInMillis) {
            if (doJoinMillis(self, timeoutInMillis)) {
                return self;
            } else {
                return nil();
            }
        }

        @TruffleBoundary
        public static void doJoin(RubyNode currentNode, final DynamicObject thread) {
            currentNode.getContext().getThreadManager().runUntilResult(currentNode, new ThreadManager.BlockingAction<Boolean>() {

                @Override
                public Boolean block() throws InterruptedException {
                    Layouts.THREAD.getFinishedLatch(thread).await();
                    return SUCCESS;
                }

            });

            if (Layouts.THREAD.getException(thread) != null) {
                throw new RaiseException(Layouts.THREAD.getException(thread));
            }
        }

        @TruffleBoundary
        private boolean doJoinMillis(final DynamicObject thread, final int timeoutInMillis) {
            final long start = System.currentTimeMillis();

            final boolean joined = getContext().getThreadManager().runUntilResult(this, () -> {
                long now = System.currentTimeMillis();
                long waited = now - start;
                if (waited >= timeoutInMillis) {
                    // We need to know whether countDown() was called and we do not want to block.
                    return Layouts.THREAD.getFinishedLatch(thread).getCount() == 0;
                }
                return Layouts.THREAD.getFinishedLatch(thread).await(timeoutInMillis - waited, TimeUnit.MILLISECONDS);
            });

            if (joined && Layouts.THREAD.getException(thread) != null) {
                throw new RaiseException(Layouts.THREAD.getException(thread));
            }

            return joined;
        }

    }

    @CoreMethod(names = "main", onSingleton = true)
    public abstract static class MainNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject main() {
            return getContext().getThreadManager().getRootThread();
        }

    }

    @CoreMethod(names = "pass", onSingleton = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class PassNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject pass() {
            Thread.yield();
            return nil();
        }

    }

    @CoreMethod(names = "status", unsafe = UnsafeGroup.THREADS)
    public abstract static class StatusNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object status(DynamicObject self) {
            // TODO: slightly hackish
            final ThreadStatus status = Layouts.THREAD.getStatus(self);
            if (status == ThreadStatus.DEAD) {
                if (Layouts.THREAD.getException(self) != null) {
                    return nil();
                } else {
                    return false;
                }
            }
            return create7BitString(status.toString().toLowerCase(), USASCIIEncoding.INSTANCE);
        }

    }

    @CoreMethod(names = "stop?", unsafe = UnsafeGroup.THREADS)
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean stop(DynamicObject self) {
            final ThreadStatus status = Layouts.THREAD.getStatus(self);
            return status == ThreadStatus.DEAD || status == ThreadStatus.SLEEP;
        }

    }

    @CoreMethod(names = "value", unsafe = UnsafeGroup.THREADS)
    public abstract static class ValueNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public Object value(DynamicObject self) {
            JoinNode.doJoin(this, self);
            return Layouts.THREAD.getValue(self);
        }

    }

    @CoreMethod(names = { "wakeup", "run" }, unsafe = UnsafeGroup.THREADS)
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject wakeup(final DynamicObject thread) {
            if (Layouts.THREAD.getStatus(thread) == ThreadStatus.DEAD) {
                throw new RaiseException(coreExceptions().threadErrorKilledThread(this));
            }

            Layouts.THREAD.getWakeUp(thread).set(true);

            final Thread toInterrupt = Layouts.THREAD.getThread(thread);

            if (toInterrupt != null) {
                // TODO: should only interrupt sleep
                toInterrupt.interrupt();
            }

            return thread;
        }

    }

    @CoreMethod(names = "abort_on_exception", unsafe = UnsafeGroup.THREADS)
    public abstract static class AbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public boolean abortOnException(DynamicObject self) {
            return Layouts.THREAD.getAbortOnException(self);
        }

    }

    @CoreMethod(names = "abort_on_exception=", required = 1, unsafe = UnsafeGroup.THREADS)
    public abstract static class SetAbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject setAbortOnException(DynamicObject self, boolean abortOnException) {
            Layouts.THREAD.setAbortOnException(self, abortOnException);
            return nil();
        }

    }

    @NonStandard
    @CoreMethod(names = "allocate", constructor = true, unsafe = UnsafeGroup.THREADS)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject allocate(
                DynamicObject rubyClass,
                @Cached("create()") AllocateObjectNode allocateObjectNode,
                @Cached("createReadAbortOnExceptionNode()") ReadObjectFieldNode readAbortOnException ) {
            final DynamicObject currentGroup = Layouts.THREAD.getThreadGroup(getContext().getThreadManager().getCurrentThread());
            final DynamicObject object = allocateObjectNode.allocate(
                    rubyClass,
                    ThreadManager.createThreadLocals(getContext()),
                    new AtomicReference<>(ThreadManager.DEFAULT_INTERRUPT_MODE),
                    new AtomicReference<>(ThreadManager.DEFAULT_STATUS),
                    new ArrayList<>(),
                    null,
                    new CountDownLatch(1),
                    readAbortOnException.execute(getContext().getCoreLibrary().getThreadClass()),
                    new AtomicReference<>(null),
                    new AtomicReference<>(null),
                    new AtomicReference<>(null),
                    new AtomicBoolean(false),
                    new AtomicInteger(0),
                    currentGroup,
                    nil());

            Layouts.THREAD.setFiberManagerUnsafe(object, new FiberManager(getContext(), object)); // Because it is cyclic

            return object;
        }

        protected ReadObjectFieldNode createReadAbortOnExceptionNode() {
            return ReadObjectFieldNodeGen.create("@abort_on_exception", false);
        }

    }

    @CoreMethod(names = "list", onSingleton = true)
    public abstract static class ListNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject list() {
            final Object[] threads = getContext().getThreadManager().getThreadList();
            return createArray(threads, threads.length);
        }
    }

    @Primitive(name = "thread_raise", unsafe = UnsafeGroup.THREADS)
    public static abstract class ThreadRaisePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = { "isRubyThread(thread)", "isRubyException(exception)" })
        public DynamicObject raise(DynamicObject thread, final DynamicObject exception) {
            raiseInThread(getContext(), thread, exception, this);
            return nil();
        }

        @TruffleBoundary
        public static void raiseInThread(final RubyContext context, DynamicObject rubyThread, final DynamicObject exception, Node currentNode) {
            final Thread javaThread = Layouts.FIBER.getThread((Layouts.THREAD.getFiberManager(rubyThread).getCurrentFiber()));

            context.getSafepointManager().pauseThreadAndExecuteLater(javaThread, currentNode, (currentThread, currentNode1) -> {
                if (Layouts.EXCEPTION.getBacktrace(exception) == null) {
                    Backtrace backtrace = context.getCallStack().getBacktrace(currentNode1);
                    Layouts.EXCEPTION.setBacktrace(exception, backtrace);
                }
                throw new RaiseException(exception);
            });
        }

    }

    @Primitive(name = "thread_get_name")
    public static abstract class ThreadGetNamePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyThread(thread)")
        public DynamicObject getName(DynamicObject thread) {
            return Layouts.THREAD.getName(thread);
        }
    }

    @Primitive(name = "thread_set_name")
    public static abstract class ThreadSetNamePrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyThread(thread)")
        public DynamicObject setName(DynamicObject thread, DynamicObject name) {
            Layouts.THREAD.setName(thread, name);
            return name;
        }
    }

    @Primitive(name = "thread_get_priority", unsafe = UnsafeGroup.THREADS)
    public static abstract class ThreadGetPriorityPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyThread(thread)")
        public int getPriority(DynamicObject thread) {
            final Thread javaThread = Layouts.THREAD.getThread(thread);
            if (javaThread != null) {
                int javaPriority = javaThread.getPriority();
                return javaPriorityToRubyPriority(javaPriority);
            } else {
                return Layouts.THREAD.getPriority(thread);
            }
        }

        /*
         * helper methods to translate Java thread priority (1-10) to Ruby thread priority (-3 to 3)
         * using a quadratic polynomial ant its inverse passing by (Ruby,Java): (-3,1), (0,5) and
         * (3,10) i.e., j = r^2/18 + 3*r/2 + 5 r = 3/2*sqrt(8*j + 41) - 27/2
         */
        private static int javaPriorityToRubyPriority(int javaPriority) {
            double d = 1.5 * Math.sqrt(8.0 * javaPriority + 41) - 13.5;
            return Math.round((float) d);
        }
    }

    @Primitive(name = "thread_set_priority", unsafe = UnsafeGroup.THREADS)
    public static abstract class ThreadSetPriorityPrimitiveNode extends PrimitiveArrayArgumentsNode {

        static final int RUBY_MIN_THREAD_PRIORITY = -3;
        static final int RUBY_MAX_THREAD_PRIORITY = 3;

        @Specialization(guards = "isRubyThread(thread)")
        public int getPriority(DynamicObject thread, int rubyPriority) {
            if (rubyPriority < RUBY_MIN_THREAD_PRIORITY) {
                rubyPriority = RUBY_MIN_THREAD_PRIORITY;
            } else if (rubyPriority > RUBY_MAX_THREAD_PRIORITY) {
                rubyPriority = RUBY_MAX_THREAD_PRIORITY;
            }

            int javaPriority = rubyPriorityToJavaPriority(rubyPriority);
            final Thread javaThread = Layouts.THREAD.getThread(thread);
            if (javaThread != null) {
                javaThread.setPriority(javaPriority);
            }
            Layouts.THREAD.setPriority(thread, rubyPriority);
            return rubyPriority;
        }

        private static int rubyPriorityToJavaPriority(int rubyPriority) {
            double d = (rubyPriority * rubyPriority) / 18.0 + 1.5 * rubyPriority + 5;
            return Math.round((float) d);
        }

    }

    @Primitive(name = "thread_set_group")
    public static abstract class ThreadSetGroupPrimitiveNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyThread(thread)")
        public DynamicObject setGroup(DynamicObject thread, DynamicObject threadGroup) {
            Layouts.THREAD.setThreadGroup(thread, threadGroup);
            return threadGroup;
        }
    }

    @Primitive(name = "thread_get_fiber_locals", unsafe = UnsafeGroup.THREADS)
    public static abstract class ThreadGetFiberLocalsNode extends PrimitiveArrayArgumentsNode {

        @Specialization(guards = "isRubyThread(thread)")
        public DynamicObject getFiberLocals(DynamicObject thread) {
            final DynamicObject fiber = Layouts.THREAD.getFiberManager(thread).getCurrentFiber();
            return Layouts.FIBER.getFiberLocals(fiber);
        }
    }

}
