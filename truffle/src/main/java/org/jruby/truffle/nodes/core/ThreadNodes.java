/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyThread.Status;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.core.RubyThread.InterruptMode;
import org.jruby.truffle.runtime.subsystems.SafepointAction;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends CoreMethodArrayArgumentsNode {

        public AliveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean alive(RubyThread thread) {
            return thread.getStatus() != Status.ABORTING && thread.getStatus() != Status.DEAD;
        }

    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodArrayArgumentsNode {

        public CurrentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyThread current() {
            return getContext().getThreadManager().getCurrentThread();
        }

    }

    @CoreMethod(names = { "kill", "exit", "terminate" })
    public abstract static class KillNode extends CoreMethodArrayArgumentsNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyThread kill(final RubyThread rubyThread) {
            final Thread toKill = rubyThread.getRootFiberJavaThread();

            getContext().getSafepointManager().pauseThreadAndExecuteLater(toKill, this, new SafepointAction() {
                @Override
                public void run(RubyThread currentThread, Node currentNode) {
                    currentThread.shutdown();
                }
            });

            return rubyThread;
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "handle_interrupt", required = 2, needsBlock = true, visibility = Visibility.PRIVATE)
    public abstract static class HandleInterruptNode extends YieldingCoreMethodNode {

        private final RubyBasicObject immediateSymbol = getContext().getSymbol("immediate");
        private final RubyBasicObject onBlockingSymbol = getContext().getSymbol("on_blocking");
        private final RubyBasicObject neverSymbol = getContext().getSymbol("never");

        public HandleInterruptNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {"isRubySymbol(timing)", "isRubyProc(block)"})
        public Object handle_interrupt(VirtualFrame frame, RubyThread self, RubyClass exceptionClass, RubyBasicObject timing, RubyBasicObject block) {
            // TODO (eregon, 12 July 2015): should we consider exceptionClass?
            final InterruptMode newInterruptMode = symbolToInterruptMode(timing);

            final InterruptMode oldInterruptMode = self.getInterruptMode();
            self.setInterruptMode(newInterruptMode);
            try {
                return yield(frame, block);
            } finally {
                self.setInterruptMode(oldInterruptMode);
            }
        }

        private InterruptMode symbolToInterruptMode(RubyBasicObject symbol) {
            if (symbol == immediateSymbol) {
                return InterruptMode.IMMEDIATE;
            } else if (symbol == onBlockingSymbol) {
                return InterruptMode.ON_BLOCKING;
            } else if (symbol == neverSymbol) {
                return InterruptMode.NEVER;
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentError("invalid timing symbol", this));
            }
        }

    }

    @CoreMethod(names = "initialize", argumentsAsArray = true, needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyProc(block)")
        public RubyBasicObject initialize(RubyThread thread, Object[] arguments, RubyBasicObject block) {
            thread.initialize(getContext(), this, arguments, block);
            return nil();
        }

    }

    @CoreMethod(names = "join", optional = 1)
    public abstract static class JoinNode extends CoreMethodArrayArgumentsNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyThread join(RubyThread thread, NotProvided timeout) {
            thread.join();
            return thread;
        }

        @Specialization(guards = "isNil(nil)")
        public RubyThread join(RubyThread thread, Object nil) {
            return join(thread, NotProvided.INSTANCE);
        }

        @Specialization
        public Object join(RubyThread thread, int timeout) {
            return joinMillis(thread, timeout * 1000);
        }

        @Specialization
        public Object join(RubyThread thread, double timeout) {
            return joinMillis(thread, (int) (timeout * 1000.0));
        }

        private Object joinMillis(RubyThread self, int timeoutInMillis) {
            if (self.join(timeoutInMillis)) {
                return self;
            } else {
                return nil();
            }
        }

    }

    @CoreMethod(names = "main", onSingleton = true)
    public abstract static class MainNode extends CoreMethodArrayArgumentsNode {

        public MainNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyThread main() {
            return getContext().getThreadManager().getRootThread();
        }

    }

    @CoreMethod(names = "pass", onSingleton = true)
    public abstract static class PassNode extends CoreMethodArrayArgumentsNode {

        @Child ThreadPassNode threadPassNode;

        public PassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            threadPassNode = new ThreadPassNode(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject pass(VirtualFrame frame) {
            threadPassNode.executeVoid(frame);
            return nil();
        }

    }

    @CoreMethod(names = "raise", required = 1, optional = 1)
    public abstract static class RaiseNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode initialize;

        public RaiseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initialize = DispatchHeadNodeFactory.createMethodCallOnSelf(context);
        }

        @Specialization(guards = "isRubyString(message)")
        public RubyBasicObject raise(VirtualFrame frame, RubyThread thread, RubyBasicObject message, NotProvided unused) {
            return raise(frame, thread, getContext().getCoreLibrary().getRuntimeErrorClass(), message);
        }

        @Specialization
        public RubyBasicObject raise(VirtualFrame frame, RubyThread thread, RubyClass exceptionClass, NotProvided message) {
            return raise(frame, thread, exceptionClass, createEmptyString());
        }

        @Specialization(guards = "isRubyString(message)")
        public RubyBasicObject raise(VirtualFrame frame, final RubyThread thread, RubyClass exceptionClass, RubyBasicObject message) {
            final Object exception = exceptionClass.allocate(this);
            initialize.call(frame, exception, "initialize", null, message);

            if (!RubyGuards.isRubyException(exception)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("exception class/object expected", this));
            }

            final RaiseException exceptionWrapper = new RaiseException(exception);

            getContext().getSafepointManager().pauseThreadAndExecuteLater(thread.getCurrentFiberJavaThread(), this, new SafepointAction() {
                @Override
                public void run(RubyThread currentThread, Node currentNode) {
                    throw exceptionWrapper;
                }
            });

            return nil();
        }

    }

    @CoreMethod(names = "status")
    public abstract static class StatusNode extends CoreMethodArrayArgumentsNode {

        public StatusNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object status(RubyThread self) {
            // TODO: slightly hackish
            if (self.getStatus() == Status.DEAD) {
                if (self.getException() != null) {
                    return nil();
                } else {
                    return false;
                }
            }

            return createString(self.getStatus().bytes);
        }

    }

    @CoreMethod(names = "stop?")
    public abstract static class StopNode extends CoreMethodArrayArgumentsNode {

        public StopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean stop(RubyThread self) {
            return self.getStatus() == Status.DEAD || self.getStatus() == Status.SLEEP;
        }

    }

    @CoreMethod(names = "value")
    public abstract static class ValueNode extends CoreMethodArrayArgumentsNode {

        public ValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object value(RubyThread self) {
            self.join();
            return self.getValue();
        }

    }

    @CoreMethod(names = { "wakeup", "run" })
    public abstract static class WakeupNode extends CoreMethodArrayArgumentsNode {

        public WakeupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyThread wakeup(final RubyThread thread) {
            if (thread.getStatus() == Status.DEAD) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().threadError("killed thread", this));
            }

            // TODO: should only interrupt sleep
            thread.wakeup();

            return thread;
        }

    }

    @CoreMethod(names = "abort_on_exception")
    public abstract static class AbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public AbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean abortOnException(RubyThread self) {
            return self.isAbortOnException();
        }

    }

    @CoreMethod(names = "abort_on_exception=", required = 1)
    public abstract static class SetAbortOnExceptionNode extends CoreMethodArrayArgumentsNode {

        public SetAbortOnExceptionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject setAbortOnException(RubyThread self, boolean abortOnException) {
            self.setAbortOnException(abortOnException);
            return nil();
        }

    }

}
