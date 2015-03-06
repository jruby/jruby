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
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNode;
import org.jruby.truffle.nodes.cast.SingleValueCastNodeFactory;
import org.jruby.truffle.nodes.core.FiberNodesFactory.FiberTransferNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyFiber;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

@CoreClass(name = "Fiber")
public abstract class FiberNodes {

    public abstract static class FiberTransferNode extends CoreMethodNode {

        @Child SingleValueCastNode singleValueCastNode;

        public FiberTransferNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FiberTransferNode(FiberTransferNode prev) {
            super(prev);
        }

        protected Object singleValue(Object[] args) {
            if (singleValueCastNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                singleValueCastNode = insert(SingleValueCastNodeFactory.create(getContext(), getSourceSection(), null));
            }
            return singleValueCastNode.executeSingleValue(args);
        }

        public abstract Object executeTransferControlTo(RubyFiber fiber, boolean isYield, Object[] args);

        @Specialization
        protected Object transfer(RubyFiber fiber, boolean isYield, Object[] args) {
            notDesignedForCompilation("68918bcd039240528b474822f41fab2b");

            if (!fiber.isAlive()) {
                throw new RaiseException(getContext().getCoreLibrary().deadFiberCalledError(this));
            }

            if (fiber.getRubyThread() != getContext().getThreadManager().getCurrentThread()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().fiberError("fiber called across threads", this));
            }

            final RubyFiber sendingFiber = getContext().getFiberManager().getCurrentFiber();

            return singleValue(sendingFiber.transferControlTo(fiber, isYield, args));
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass initialize(RubyFiber fiber, RubyProc block) {
            notDesignedForCompilation("080c666ff8354c34a322ace130867776");

            fiber.initialize(block);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "resume", argumentsAsArray = true)
    public abstract static class ResumeNode extends CoreMethodNode {

        @Child FiberTransferNode fiberTransferNode;

        public ResumeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        public ResumeNode(ResumeNode prev) {
            super(prev);
            fiberTransferNode = prev.fiberTransferNode;
        }

        @Specialization
        public Object resume(RubyFiber fiberBeingResumed, Object[] args) {
            return fiberTransferNode.executeTransferControlTo(fiberBeingResumed, false, args);
        }

    }

    @CoreMethod(names = "yield", onSingleton = true, argumentsAsArray = true)
    public abstract static class YieldNode extends CoreMethodNode {

        @Child FiberTransferNode fiberTransferNode;

        public YieldNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fiberTransferNode = FiberTransferNodeFactory.create(context, sourceSection, new RubyNode[] { null, null, null });
        }

        public YieldNode(YieldNode prev) {
            super(prev);
            fiberTransferNode = prev.fiberTransferNode;
        }

        @Specialization
        public Object yield(Object[] args) {
            final RubyFiber yieldingFiber = getContext().getFiberManager().getCurrentFiber();
            final RubyFiber fiberYieldedTo = yieldingFiber.getLastResumedByFiber();

            if (yieldingFiber.isTopLevel() || fiberYieldedTo == null) {
                throw new RaiseException(getContext().getCoreLibrary().yieldFromRootFiberError(this));
            }

            return fiberTransferNode.executeTransferControlTo(fiberYieldedTo, true, args);
        }

    }

}
