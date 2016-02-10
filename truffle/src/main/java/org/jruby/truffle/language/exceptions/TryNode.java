/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.exceptions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.RetryException;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;

/**
 * Represents a block of code run with exception handlers. There's no {@code try} keyword in Ruby -
 * it's implicit - but it's similar to a try statement in any other language.
 */
public class TryNode extends RubyNode {

    @Child private ExceptionTranslatingNode tryPart;
    @Children final RescueNode[] rescueParts;
    @Child private RubyNode elsePart;

    private final BranchProfile elseProfile = BranchProfile.create();
    private final BranchProfile controlFlowProfile = BranchProfile.create();
    private final BranchProfile raiseExceptionProfile = BranchProfile.create();

    public TryNode(RubyContext context, SourceSection sourceSection, ExceptionTranslatingNode tryPart, RescueNode[] rescueParts, RubyNode elsePart) {
        super(context, sourceSection);
        this.tryPart = tryPart;
        this.rescueParts = rescueParts;
        this.elsePart = elsePart;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        while (true) {
            Object result;

            try {
                result = tryPart.execute(frame);
            } catch (RaiseException exception) {
                raiseExceptionProfile.enter();

                try {
                    return handleException(frame, exception);
                } catch (RetryException e) {
                    getContext().getSafepointManager().poll(this);
                    continue;
                }
            } catch (ControlFlowException exception) {
                controlFlowProfile.enter();
                throw exception;
            }

            elseProfile.enter();

            if (elsePart != null) {
                result = elsePart.execute(frame);
            }

            return result;
        }
    }

    @ExplodeLoop
    private Object handleException(VirtualFrame frame, RaiseException exception) {
        for (RescueNode rescue : rescueParts) {
            if (rescue.canHandle(frame, exception.getRubyException())) {
                return setLastExceptionAndRunRescue(frame, exception, rescue);
            }
        }

        // Not handled by any of the rescue
        throw exception;
    }

    private Object setLastExceptionAndRunRescue(VirtualFrame frame, RaiseException exception, RescueNode rescue) {
        final DynamicObject threadLocals = Layouts.THREAD.getThreadLocals(getContext().getThreadManager().getCurrentThread());

        final Object lastException = threadLocals.get("$!", nil());
        threadLocals.set("$!", exception.getRubyException());
        try {
            return rescue.execute(frame);
        } finally {
            threadLocals.set("$!", lastException);
        }
    }

}
