/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * The uninitialized dispatch node. Only reached when the method is not expected by any node in the
 * dispatch chain, and only creates new nodes or modifies the existing chain.
 */
public class UninitializedDispatchNode extends BoxedDispatchNode {

    /*
     * Node at depth 5 is 4 actual dispatches, the boxing dispatch and the final uninitalized
     * dispatch.
     */

    private static final int MAX_DEPTH = 5;

    private final String name;

    public UninitializedDispatchNode(RubyContext context, SourceSection sourceSection, String name) {
        super(context, sourceSection);

        assert name != null;

        this.name = name;
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreter();

        final RubyContext context = getContext();

        final int depth = getDepth();
        final DispatchHeadNode dispatchHead = (DispatchHeadNode) NodeUtil.getNthParent(this, depth);

        if (depth == MAX_DEPTH) {
            /*
             * Replace the chain with DispatchHeadNode -> ExpectBoxedDispatchNode ->
             * GeneralDispatchNode.
             */

            getContext().getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, getSourceSection().getSource().getName(), getSourceSection().getStartLine(), "resorting to a general call node");

            final GeneralBoxedDispatchNode newGeneralDispatch = new GeneralBoxedDispatchNode(getContext(), getSourceSection(), name);
            final BoxingDispatchNode newBoxing = new BoxingDispatchNode(getContext(), getSourceSection(), newGeneralDispatch);

            dispatchHead.getDispatch().replace(newBoxing);
            return newBoxing.dispatch(frame, receiverObject, blockObject, argumentsObjects);
        }


        RubyMethod method;

        try {
            method = lookup(frame, receiverObject, name);
        } catch (UseMethodMissingException e) {
            try {
                method = lookup(frame, receiverObject, "method_missing");
            } catch (UseMethodMissingException e2) {
                throw new RaiseException(context.getCoreLibrary().runtimeError(receiverObject.toString() + " didn't have a #method_missing"));
            }

            final UninitializedDispatchNode newUninitializedDispatch = new UninitializedDispatchNode(getContext(), getSourceSection(), name);

            BoxedDispatchNode newDispatch = new CachedBoxedMethodMissingDispatchNode(getContext(), getSourceSection(), receiverObject.getLookupNode(), method, name, newUninitializedDispatch);
            replace(newDispatch, "appending new boxed method missing dispatch node to chain");
            return newDispatch.dispatch(frame, receiverObject, blockObject, argumentsObjects);
        }

        if (receiverObject instanceof Unboxable) {
            /*
             * Unboxed dispatch nodes are prepended to the chain of dispatch nodes, so they're
             * before the point where receivers will definitely be boxed.
             */

            final Object receiverUnboxed = ((Unboxable) receiverObject).unbox();
            final UnboxedDispatchNode firstDispatch = dispatchHead.getDispatch();

            if (receiverObject instanceof RubyTrueClass || receiverObject instanceof RubyFalseClass) {
                try {
                    final Assumption falseUnmodifiedAssumption = context.getCoreLibrary().getFalseClass().getUnmodifiedAssumption();
                    final RubyMethod falseMethod = lookup(frame, context.getCoreLibrary().box(false), name);
                    final Assumption trueUnmodifiedAssumption = context.getCoreLibrary().getTrueClass().getUnmodifiedAssumption();
                    final RubyMethod trueMethod = lookup(frame, context.getCoreLibrary().box(true), name);

                    final BooleanDispatchNode newDispatch = new BooleanDispatchNode(getContext(), getSourceSection(), falseUnmodifiedAssumption, falseMethod, trueUnmodifiedAssumption, trueMethod, null);
                    firstDispatch.replace(newDispatch, "prepending new unboxed dispatch node to chain");
                    newDispatch.setNext(firstDispatch);
                    return newDispatch.dispatch(frame, receiverUnboxed, blockObject, argumentsObjects);
                } catch (UseMethodMissingException e) {
                    throw new UnsupportedOperationException();
                }
            }

            UnboxedDispatchNode newDispatch;

            if (method.getImplementation() instanceof InlinableMethodImplementation && InlineHeuristic.shouldInline((InlinableMethodImplementation) method.getImplementation())) {
                newDispatch = new InlinedUnboxedDispatchNode(getContext(), getSourceSection(), receiverUnboxed.getClass(), receiverObject.getRubyClass().getUnmodifiedAssumption(),
                        (InlinableMethodImplementation) method.getImplementation(), null);
            } else {
                newDispatch = new CachedUnboxedDispatchNode(getContext(), getSourceSection(), receiverUnboxed.getClass(), receiverObject.getRubyClass().getUnmodifiedAssumption(), method, null);
            }

            firstDispatch.replace(newDispatch, "prepending new unboxed dispatch node to chain");
            newDispatch.setNext(firstDispatch);

            return newDispatch.dispatch(frame, receiverUnboxed, blockObject, argumentsObjects);
        }

        /*
         * Boxed dispatch nodes are appended to the chain of dispatch nodes, so they're after
         * the point where receivers are guaranteed to be boxed.
        */

        final UninitializedDispatchNode newUninitializedDispatch = new UninitializedDispatchNode(getContext(), getSourceSection(), name);

        BoxedDispatchNode newDispatch;

        if (method.getImplementation() instanceof InlinableMethodImplementation && InlineHeuristic.shouldInline((InlinableMethodImplementation) method.getImplementation())) {
            newDispatch = new InlinedBoxedDispatchNode(getContext(), getSourceSection(), receiverObject.getLookupNode(), (InlinableMethodImplementation) method.getImplementation(),
                    newUninitializedDispatch);
        } else {
            newDispatch = new CachedBoxedDispatchNode(getContext(), getSourceSection(), receiverObject.getLookupNode(), method, newUninitializedDispatch);
        }

        replace(newDispatch, "appending new boxed dispatch node to chain");

        return newDispatch.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }
}
