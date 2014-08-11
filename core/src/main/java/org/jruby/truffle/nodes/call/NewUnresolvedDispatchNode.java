/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;

public final class NewUnresolvedDispatchNode extends NewDispatchNode {

    private static final int MAX_DEPTH = 6;

    private final String name;
    private final DispatchHeadNode.MissingBehavior missingBehavior;

    public NewUnresolvedDispatchNode(RubyContext context, String name, DispatchHeadNode.MissingBehavior missingBehavior) {
        super(context);
        assert name != null;
        this.name = name;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final RubyContext context = getContext();



        if (getDepth() == MAX_DEPTH) {
            return createAndExecuteGeneric(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
        }

        final DispatchHeadNode dispatchHead = (DispatchHeadNode) NodeUtil.getNthParent(this, getDepth());
        final NewDispatchNode head = dispatchHead.getNewDispatch();

        if (callingSelf instanceof RubyBasicObject && receiverObject instanceof  RubyBasicObject) {
            RubyBasicObject boxedCallingSelf = (RubyBasicObject) callingSelf;
            RubyBasicObject boxedReceiverObject = (RubyBasicObject) receiverObject;
            RubyMethod method;

            try {
                method = lookup(boxedCallingSelf, boxedReceiverObject, name);
            } catch (UseMethodMissingException e) {
                NewDispatchNode newDispatch;
                newDispatch = doMissingBehavior(context, boxedCallingSelf, boxedReceiverObject, head);
                return newDispatch.executeDispatch(frame, boxedCallingSelf, boxedReceiverObject, blockObject, argumentsObjects);
            }
            /*
            * Boxed dispatch nodes are appended to the chain of dispatch nodes, so they're after
            * the point where receivers are guaranteed to be boxed.
            */


            final NewDispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol && RubySymbol.globalSymbolLookupNodeAssumption.isValid()) {
                newDispatch = NewCachedBoxedSymbolDispatchNodeFactory.create(getContext(), head, method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
            } else {
                newDispatch = NewCachedBoxedDispatchNodeFactory.create(getContext(), head, boxedReceiverObject.getLookupNode(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
            }

            head.replace(newDispatch);
            return newDispatch.executeDispatch(frame, boxedCallingSelf, receiverObject, blockObject, argumentsObjects);
        } else {
            RubyBasicObject boxedCallingSelf = context.getCoreLibrary().box(callingSelf);
            RubyBasicObject boxedReceiverObject = context.getCoreLibrary().box(receiverObject);
            RubyMethod method;

            try {
                method = lookup(boxedCallingSelf, boxedReceiverObject, name);
            } catch (UseMethodMissingException e) {

                NewDispatchNode newDispatch;
                newDispatch = doMissingBehavior(context, boxedCallingSelf, boxedReceiverObject, head);
                return newDispatch.executeDispatch(frame, boxedCallingSelf, boxedReceiverObject, blockObject, argumentsObjects);
            }

            if (receiverObject instanceof  Boolean) {
                try {
                    final Assumption falseUnmodifiedAssumption = context.getCoreLibrary().getFalseClass().getUnmodifiedAssumption();
                    final RubyMethod falseMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(false), name);
                    final Assumption trueUnmodifiedAssumption = context.getCoreLibrary().getTrueClass().getUnmodifiedAssumption();
                    final RubyMethod trueMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(true), name);

                    final NewCachedBooleanDispatchNode newDispatch = NewCachedBooleanDispatchNodeFactory.create(getContext(), head, falseUnmodifiedAssumption, falseMethod, trueUnmodifiedAssumption, trueMethod, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                    head.replace(newDispatch);
                    return newDispatch.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
                } catch (UseMethodMissingException e) {
                    throw new UnsupportedOperationException();
                }
            //} else if (receiverObject instanceof Integer) {
            //    final NewCachedIntegerDispatchNode newDispatch = NewCachedIntegerDispatchNodeFactory.create(getContext(), head, boxedReceiverObject.getRubyClass().getUnmodifiedAssumption(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
            //    head.replace(newDispatch);
            //    return newDispatch.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
            } else {
                final NewCachedUnboxedDispatchNode newDispatch = NewCachedUnboxedDispatchNodeFactory.create(getContext(), head, receiverObject.getClass(), boxedReceiverObject.getRubyClass().getUnmodifiedAssumption(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                head.replace(newDispatch);
                return newDispatch.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
            }
        }
    }

    private NewDispatchNode doMissingBehavior(RubyContext context, RubyBasicObject boxedCallingSelf, RubyBasicObject boxedReceiverObject, NewDispatchNode head) {
        NewDispatchNode newDispatch;
        RubyMethod method;
        switch (missingBehavior) {
            case RETURN_MISSING: {
                newDispatch = NewCachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), head, boxedReceiverObject.getLookupNode(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                return head.replace(newDispatch);
            }

            case CALL_METHOD_MISSING: {
                try {
                    method = lookup(boxedCallingSelf, boxedReceiverObject, "method_missing");
                } catch (UseMethodMissingException e2) {
                    throw new RaiseException(context.getCoreLibrary().runtimeError(boxedReceiverObject.toString() + " didn't have a #method_missing", this));
                }
                newDispatch = NewCachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), head, boxedReceiverObject.getLookupNode(), method, name, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                return head.replace(newDispatch);
            }

            default:
                throw new UnsupportedOperationException(missingBehavior.toString());
        }
    }

    private Object createAndExecuteGeneric(VirtualFrame frame, Object boxedCallingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        final DispatchHeadNode dispatchHead = (DispatchHeadNode) NodeUtil.getNthParent(this, getDepth());
        return dispatchHead.getNewDispatch().replace(NewGenericDispatchNodeFactory.create(getContext(), name, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute())).executeDispatch(frame, boxedCallingSelf, receiverObject, blockObject, argumentsObjects);
    }
}
