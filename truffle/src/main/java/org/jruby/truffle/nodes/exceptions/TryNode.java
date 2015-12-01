/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.exceptions;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.methods.ExceptionTranslatingNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RetryException;
import org.jruby.truffle.runtime.layouts.Layouts;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

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
            } catch (ControlFlowException exception) {
                controlFlowProfile.enter();
                throw exception;
            } catch (RaiseException exception) {
                raiseExceptionProfile.enter();

                try {
                    return handleException(frame, exception);
                } catch (RetryException e) {
                    getContext().getSafepointManager().poll(this);
                    continue;
                }
            }

            elseProfile.enter();

            if (elsePart != null) {
                result = elsePart.execute(frame);
            }

            return result;
        }
    }

    private Object handleException(VirtualFrame frame, RaiseException exception) {
        CompilerDirectives.transferToInterpreter();

        for (RescueNode rescue : rescueParts) {
            if (rescue.canHandle(frame, (DynamicObject) exception.getRubyException())) {
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
