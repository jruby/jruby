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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyFiber;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyProc;

@CoreClass(name = "Fiber")
public abstract class FiberNodes {

    @CoreMethod(names = "resume", argumentsAsArray = true)
    public abstract static class ResumeNode extends CoreMethodNode {

        public ResumeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ResumeNode(ResumeNode prev) {
            super(prev);
        }

        @Specialization
        public Object resume(RubyFiber fiberBeingResumed, Object[] args) {
            notDesignedForCompilation();

            if (!fiberBeingResumed.isAlive()) {
                throw new RaiseException(getContext().getCoreLibrary().deadFiberCalledError(this));
            }

            final RubyFiber sendingFiber = getContext().getFiberManager().getCurrentFiber();

            fiberBeingResumed.resume(false, sendingFiber, args);

            return sendingFiber.waitForResume();
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
            notDesignedForCompilation();

            fiber.initialize(block);
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "yield", onSingleton = true, argumentsAsArray = true)
    public abstract static class YieldNode extends CoreMethodNode {

        public YieldNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public YieldNode(YieldNode prev) {
            super(prev);
        }

        @Specialization
        public Object yield(Object[] args) {
            notDesignedForCompilation();

            final RubyFiber yieldingFiber = getContext().getFiberManager().getCurrentFiber();
            final RubyFiber fiberYieldedTo = yieldingFiber.getLastResumedByFiber();

            if (yieldingFiber.isTopLevel() || fiberYieldedTo == null) {
                throw new RaiseException(getContext().getCoreLibrary().yieldFromRootFiberError(this));
            }

            fiberYieldedTo.resume(true, yieldingFiber, args);

            return yieldingFiber.waitForResume();
        }

    }

}
