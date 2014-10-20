/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import org.jruby.*;
import org.jruby.RubyThread.Status;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.ThreadExitException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubyThread;

@CoreClass(name = "Thread")
public abstract class ThreadNodes {

    @CoreMethod(names = "alive?", maxArgs = 0)
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

    @CoreMethod(names = "current", onSingleton = true, maxArgs = 0)
    public abstract static class CurrentNode extends CoreMethodNode {

        public CurrentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CurrentNode(AliveNode prev) {
            super(prev);
        }

        @Specialization
        public RubyThread current() {
            notDesignedForCompilation();

            return getContext().getThreadManager().getCurrentThread();
        }

    }

    @CoreMethod(names = "exit", onSingleton = true, maxArgs = 0)
    public abstract static class ExitModuleNode extends CoreMethodNode {

        public ExitModuleNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitModuleNode(ExitModuleNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass exit() {
            throw new ThreadExitException();
        }

    }

    @CoreMethod(names = "exit", needsSelf = false, maxArgs = 0)
    public abstract static class ExitInstanceNode extends CoreMethodNode {

        public ExitInstanceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExitInstanceNode(ExitInstanceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass exit() {
            throw new ThreadExitException();
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, maxArgs = 0)
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

    @CoreMethod(names = "join", maxArgs = 0)
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

    @CoreMethod(names = "pass", onSingleton = true, maxArgs = 0)
    public abstract static class PassNode extends CoreMethodNode {

        public PassNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PassNode(PassNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass pass() {
            getContext().outsideGlobalLock(new Runnable() {
                @Override
                public void run() {
                    Thread.yield();
                }
            });

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "status", maxArgs = 0)
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

            return new RubyString(getContext().getCoreLibrary().getStringClass(), self.getStatus().bytes);
        }

    }

    @CoreMethod(names = "stop?", maxArgs = 0)
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

    @CoreMethod(names = "value", maxArgs = 0)
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

            if (self.getException() != null) {
                throw new RaiseException(self.getException());
            }

            if (self.getValue() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return self.getValue();
            }
        }

    }

}
