/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.control;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.methods.ExceptionTranslatingNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RetryException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Represents a block of code run with exception handlers. There's no {@code try} keyword in Ruby -
 * it's implicit - but it's similar to a try statement in any other language.
 */
public class TryNode extends RubyNode {

    @Child private ExceptionTranslatingNode tryPart;
    @Children final RescueNode[] rescueParts;
    @Child private RubyNode elsePart;
    @Child private WriteInstanceVariableNode clearExceptionVariableNode;

    private final BranchProfile elseProfile = BranchProfile.create();
    private final BranchProfile controlFlowProfile = BranchProfile.create();
    private final BranchProfile raiseExceptionProfile = BranchProfile.create();

    public TryNode(RubyContext context, SourceSection sourceSection, ExceptionTranslatingNode tryPart, RescueNode[] rescueParts, RubyNode elsePart) {
        super(context, sourceSection);
        this.tryPart = tryPart;
        this.rescueParts = rescueParts;
        this.elsePart = elsePart;
        clearExceptionVariableNode = new WriteInstanceVariableNode(context, sourceSection, "$!",
                new ObjectLiteralNode(context, sourceSection, context.getThreadManager().getCurrentThread().getThreadLocals()),
                new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                true);
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
            } finally {
                clearExceptionVariableNode.execute(frame);
            }

            elseProfile.enter();
            elsePart.executeVoid(frame);
            return result;
        }
    }

    @ExplodeLoop
    private Object handleException(VirtualFrame frame, RaiseException exception) {
        CompilerAsserts.neverPartOfCompilation();

        notDesignedForCompilation();

        final RubyBasicObject threadLocals = getContext().getThreadManager().getCurrentThread().getThreadLocals();
        threadLocals.getOperations().setInstanceVariable(threadLocals, "$!", exception.getRubyException());

        for (RescueNode rescue : rescueParts) {
            if (rescue.canHandle(frame, exception.getRubyException())) {
                return rescue.execute(frame);
            }
        }

        throw exception;
    }

}
