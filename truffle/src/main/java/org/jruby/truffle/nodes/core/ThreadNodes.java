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
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.subsystems.SafepointAction;
import org.jruby.truffle.runtime.util.Consumer;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    @CoreMethod(names = "alive?")
    public abstract static class AliveNode extends CoreMethodNode {

        public AliveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AliveNode(AliveNode prev) {
            super(prev);
        }

        @Specialization
        public boolean alive(RubyThread thread) {
            return thread.getStatus() != Status.ABORTING && thread.getStatus() != Status.DEAD;
        }

    }

    @CoreMethod(names = "current", onSingleton = true)
    public abstract static class CurrentNode extends CoreMethodNode {

        public CurrentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CurrentNode(CurrentNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread current() {
            notDesignedForCompilation();

            return getContext().getThreadManager().getCurrentThread();
        }

    }

    @CoreMethod(names = "exit", onSingleton = true)
    public abstract static class ExitModuleNode extends CoreMethodNode {

        public ExitModuleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitModuleNode(ExitModuleNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass exit() {
            getContext().getThreadManager().getCurrentThread().exit();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = { "kill", "exit", "terminate" })
    public abstract static class KillNode extends CoreMethodNode {

        public KillNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public KillNode(KillNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread kill(final RubyThread thread) {
            getContext().getSafepointManager().pauseAllThreadsAndExecute(this, new SafepointAction() {

                @Override
                public void run(RubyThread currentThread, Node currentNode) {
                    if (currentThread == thread) {
                        currentThread.exit();
                    }
                }

            });

            return thread;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize(RubyThread thread, RubyProc block) {
            notDesignedForCompilation();

            thread.initialize(getContext(), this, block);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "join")
    public abstract static class JoinNode extends CoreMethodNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JoinNode(JoinNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread join(RubyThread self) {
            notDesignedForCompilation();

            self.join();
            return self;
        }

    }

    @CoreMethod(names = "pass", onSingleton = true)
    public abstract static class PassNode extends CoreMethodNode {

        @Child ThreadPassNode threadPassNode;

        public PassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            threadPassNode = new ThreadPassNode(context, sourceSection);
        }

        public PassNode(PassNode prev) {
            super(prev);
            threadPassNode = prev.threadPassNode;
        }

        @Specialization
        public RubyNilClass pass(VirtualFrame frame) {
            threadPassNode.executeVoid(frame);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "raise", required = 1, optional = 1)
    public abstract static class RaiseNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode initialize;

        public RaiseNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            initialize = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public RaiseNode(RaiseNode prev) {
            super(prev);
            initialize = prev.initialize;
        }

        @Specialization
        public RubyNilClass raise(VirtualFrame frame, RubyThread thread, RubyString message, UndefinedPlaceholder undefined) {
            return raise(frame, thread, getContext().getCoreLibrary().getRuntimeErrorClass(), message);
        }

        @Specialization
        public RubyNilClass raise(VirtualFrame frame, RubyThread thread, RubyClass exceptionClass, UndefinedPlaceholder message) {
            return raise(frame, thread, exceptionClass, getContext().makeString(""));
        }

        @Specialization
        public RubyNilClass raise(VirtualFrame frame, final RubyThread thread, RubyClass exceptionClass, RubyString message) {
            final Object exception = exceptionClass.allocate(this);
            initialize.call(frame, exception, "initialize", null, message);

            if (!(exception instanceof RubyException)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeError("exception class/object expected", this));
            }

            final RaiseException exceptionWrapper = new RaiseException((RubyException) exception);

            getContext().getSafepointManager().pauseAllThreadsAndExecute(this, new SafepointAction() {

                @Override
                public void run(RubyThread currentThread, Node currentNode) {
                    if (currentThread == thread) {
                        throw exceptionWrapper;
                    }
                }

            });

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "run")
    public abstract static class RunNode extends CoreMethodNode {

        public RunNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RunNode(RunNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread run(final RubyThread thread) {
            notDesignedForCompilation();

            thread.interrupt();

            return thread;
        }

    }

    @CoreMethod(names = "status")
    public abstract static class StatusNode extends CoreMethodNode {

        public StatusNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StatusNode(StatusNode prev) {
            super(prev);
        }

        @Specialization
        public Object status(RubyThread self) {
            notDesignedForCompilation();

            // TODO: slightly hackish
            if (self.getStatus() == Status.DEAD) {
                if (self.getException() != null) {
                    return getContext().getCoreLibrary().getNilObject();
                } else {
                    return false;
                }
            }

            return new RubyString(getContext().getCoreLibrary().getStringClass(), self.getStatus().bytes);
        }

    }

    @CoreMethod(names = "stop?")
    public abstract static class StopNode extends CoreMethodNode {

        public StopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StopNode(StopNode prev) {
            super(prev);
        }

        @Specialization
        public boolean stop(RubyThread self) {
            notDesignedForCompilation();

            return self.getStatus() == Status.DEAD || self.getStatus() == Status.SLEEP;
        }

    }

    @CoreMethod(names = "value")
    public abstract static class ValueNode extends CoreMethodNode {

        public ValueNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ValueNode(ValueNode prev) {
            super(prev);
        }

        @Specialization
        public Object value(RubyThread self) {
            notDesignedForCompilation();

            self.join();

            return self.getValue();
        }

    }

    @CoreMethod(names = "wakeup")
    public abstract static class WakeupNode extends CoreMethodNode {

        public WakeupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public WakeupNode(WakeupNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread wakeup(final RubyThread thread) {
            notDesignedForCompilation();

            // TODO: should only interrupt sleep
            thread.interrupt();

            return thread;
        }

    }

}
