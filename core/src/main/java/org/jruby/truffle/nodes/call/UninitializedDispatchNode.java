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
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * The uninitialized dispatch node. Only reached when the method is not expected by any node in the
 * dispatch chain, and only creates new nodes or modifies the existing chain.
 */
@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedDispatchNode extends BoxedDispatchNode {

    private static final int MAX_DISPATCHES = 4;
    private static final int MAX_DEPTH = MAX_DISPATCHES + 2; // MAX_DISPATCHES + BoxingDispatchNode + UninitializedDispatchNode

    private final String name;
    private final DispatchHeadNode.MissingBehavior missingBehavior;

    public UninitializedDispatchNode(RubyContext context, String name, DispatchHeadNode.MissingBehavior missingBehavior) {
        super(context);
        assert name != null;
        this.name = name;
        this.missingBehavior = missingBehavior;
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

            final GeneralDispatchNode newGeneralDispatch = new GeneralDispatchNode(getContext(), name);
            final BoxingDispatchNode newBoxing = new BoxingDispatchNode(getContext(), newGeneralDispatch);

            dispatchHead.getDispatch().replace(newBoxing);
            return newBoxing.dispatch(frame, receiverObject, blockObject, argumentsObjects);
        }

        final RubyBasicObject boxedCallingSelf = getContext().getCoreLibrary().box(RubyArguments.getSelf(frame.getArguments()));

        RubyMethod method;

        try {
            method = lookup(boxedCallingSelf, receiverObject, name);
        } catch (UseMethodMissingException e) {
            switch (missingBehavior) {
                case RETURN_MISSING: {
                    BoxedDispatchNode newDispatch = new CachedBoxedReturnMissingDispatchNode(getContext(), receiverObject.getLookupNode(), this);
                    replace(newDispatch, "appending new boxed return nil dispatch node to chain");
                    return newDispatch.dispatch(frame, receiverObject, blockObject, argumentsObjects);
                }

                case CALL_METHOD_MISSING: {
                    try {
                        method = lookup(boxedCallingSelf, receiverObject, "method_missing");
                    } catch (UseMethodMissingException e2) {
                        throw new RaiseException(context.getCoreLibrary().runtimeError(receiverObject.toString() + " didn't have a #method_missing"));
                    }

                    BoxedDispatchNode newDispatch = new CachedBoxedMethodMissingDispatchNode(getContext(), receiverObject.getLookupNode(), method, name, this);
                    replace(newDispatch, "appending new boxed method missing dispatch node to chain");
                    return newDispatch.dispatch(frame, receiverObject, blockObject, argumentsObjects);
                }

                default:
                    throw new UnsupportedOperationException(missingBehavior.toString());
            }
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
                    final RubyMethod falseMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(false), name);
                    final Assumption trueUnmodifiedAssumption = context.getCoreLibrary().getTrueClass().getUnmodifiedAssumption();
                    final RubyMethod trueMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(true), name);

                    final BooleanDispatchNode newDispatch = new BooleanDispatchNode(getContext(), falseUnmodifiedAssumption, falseMethod, trueUnmodifiedAssumption, trueMethod, null);
                    firstDispatch.replace(newDispatch, "prepending new unboxed dispatch node to chain");
                    newDispatch.setNext(firstDispatch);
                    return newDispatch.dispatch(frame, receiverUnboxed, blockObject, argumentsObjects);
                } catch (UseMethodMissingException e) {
                    throw new UnsupportedOperationException();
                }
            }

            final UnboxedDispatchNode newDispatch = new CachedUnboxedDispatchNode(getContext(), receiverUnboxed.getClass(), receiverObject.getRubyClass().getUnmodifiedAssumption(), method, null);
            firstDispatch.replace(newDispatch, "prepending new unboxed dispatch node to chain");
            newDispatch.setNext(firstDispatch);
            return newDispatch.dispatch(frame, receiverUnboxed, blockObject, argumentsObjects);
        }

        /*
         * Boxed dispatch nodes are appended to the chain of dispatch nodes, so they're after
         * the point where receivers are guaranteed to be boxed.
        */

        final UninitializedDispatchNode newUninitializedDispatch = new UninitializedDispatchNode(getContext(), name, missingBehavior);
        final BoxedDispatchNode newDispatch = new CachedBoxedDispatchNode(getContext(), receiverObject.getLookupNode(), method, newUninitializedDispatch);
        replace(newDispatch, "appending new boxed dispatch node to chain");
        return newDispatch.dispatch(frame, receiverObject, blockObject, argumentsObjects);
    }
}
