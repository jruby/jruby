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

    private static final int MAX_DEPTH = 8;

    private final boolean ignoreVisibility;
    private final DispatchHeadNode.MissingBehavior missingBehavior;

    public NewUnresolvedDispatchNode(RubyContext context, boolean ignoreVisibility, DispatchHeadNode.MissingBehavior missingBehavior) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        assert methodName != null;
        assert RubyContext.shouldObjectBeVisible(callingSelf);
        assert RubyContext.shouldObjectBeVisible(receiverObject);

        final RubyContext context = getContext();

        if (getDepth() == MAX_DEPTH) {
            return createAndExecuteGeneric(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }

        final DispatchHeadNode dispatchHead = (DispatchHeadNode) NodeUtil.getNthParent(this, getDepth());
        final NewDispatchNode head = dispatchHead.getNewDispatch();

        if (callingSelf instanceof RubyBasicObject && receiverObject instanceof  RubyBasicObject) {
            return doRubyBasicObject(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, context, head, dispatchAction);
        } else {
            return doUnboxed(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, context, head, dispatchAction);
        }
    }

    private Object doUnboxed(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, RubyContext context, NewDispatchNode head, DispatchHeadNode.DispatchAction dispatchAction) {
        RubyBasicObject boxedCallingSelf = context.getCoreLibrary().box(callingSelf);
        RubyBasicObject boxedReceiverObject = context.getCoreLibrary().box(receiverObject);
        RubyMethod method;

        try {
            method = lookup(boxedCallingSelf, boxedReceiverObject, methodName.toString(), ignoreVisibility, dispatchAction);
        } catch (UseMethodMissingException e) {
            NewDispatchNode newDispatch;
            newDispatch = doMissingBehavior(context, methodName, methodReceiverObject, boxedCallingSelf, boxedReceiverObject, head, dispatchAction);
            return newDispatch.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, boxedReceiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }

        if (receiverObject instanceof  Boolean) {
            try {
                final Assumption falseUnmodifiedAssumption = context.getCoreLibrary().getFalseClass().getUnmodifiedAssumption();
                final RubyMethod falseMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(false), methodName.toString(), ignoreVisibility, dispatchAction);
                final Assumption trueUnmodifiedAssumption = context.getCoreLibrary().getTrueClass().getUnmodifiedAssumption();
                final RubyMethod trueMethod = lookup(boxedCallingSelf, context.getCoreLibrary().box(true), methodName.toString(), ignoreVisibility, dispatchAction);

                final NewCachedBooleanDispatchNode newDispatch = NewCachedBooleanDispatchNodeFactory.create(getContext(), methodName, head, falseUnmodifiedAssumption, falseMethod, trueUnmodifiedAssumption, trueMethod, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                head.replace(newDispatch);
                return newDispatch.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
            } catch (UseMethodMissingException e) {
                throw new UnsupportedOperationException();
            }
        } else {
            final NewCachedUnboxedDispatchNode newDispatch = NewCachedUnboxedDispatchNodeFactory.create(getContext(), methodName, head, receiverObject.getClass(), boxedReceiverObject.getRubyClass().getUnmodifiedAssumption(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
            head.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }
    }

    private Object doRubyBasicObject(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, RubyContext context, NewDispatchNode head, DispatchHeadNode.DispatchAction dispatchAction) {
        RubyBasicObject boxedCallingSelf = (RubyBasicObject) callingSelf;
        RubyBasicObject boxedReceiverObject = (RubyBasicObject) receiverObject;
        RubyMethod method;

        try {
            method = lookup(boxedCallingSelf, boxedReceiverObject, methodName.toString(), ignoreVisibility, dispatchAction);
        } catch (UseMethodMissingException e) {
            NewDispatchNode newDispatch;
            newDispatch = doMissingBehavior(context, methodName, methodReceiverObject, boxedCallingSelf, boxedReceiverObject, head, dispatchAction);
            return newDispatch.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, boxedReceiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }
            /*
            * Boxed dispatch nodes are appended to the chain of dispatch nodes, so they're after
            * the point where receivers are guaranteed to be boxed.
            */


        return doRubyBasicObjectWithMethod(receiverObject, methodName, head, boxedReceiverObject, method).executeDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }

    private NewDispatchNode doRubyBasicObjectWithMethod(Object receiverObject, Object methodName, NewDispatchNode head, RubyBasicObject boxedReceiverObject, RubyMethod method) {
        final NewDispatchNode newDispatch;

        if (receiverObject instanceof RubySymbol && RubySymbol.globalSymbolLookupNodeAssumption.isValid()) {
            newDispatch = NewCachedBoxedSymbolDispatchNodeFactory.create(getContext(), methodName, head, method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
        } else {
            newDispatch = NewCachedBoxedDispatchNodeFactory.create(getContext(), methodName, head, boxedReceiverObject.getLookupNode(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
        }

        head.replace(newDispatch);
        return newDispatch;
    }

    private NewDispatchNode doMissingBehavior(RubyContext context, Object methodName, Object methodReceiverObject, RubyBasicObject boxedCallingSelf, RubyBasicObject boxedReceiverObject, NewDispatchNode head, DispatchHeadNode.DispatchAction dispatchAction) {
        NewDispatchNode newDispatch;
        RubyMethod method;
        switch (missingBehavior) {
            case RETURN_MISSING: {
                newDispatch = NewCachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, head, boxedReceiverObject.getLookupNode(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                return head.replace(newDispatch);
            }

            case CALL_METHOD_MISSING: {
                try {
                    method = lookup(boxedCallingSelf, boxedReceiverObject, "method_missing", ignoreVisibility, dispatchAction);
                } catch (UseMethodMissingException e2) {
                    throw new RaiseException(context.getCoreLibrary().runtimeError(boxedReceiverObject.toString() + " didn't have a #method_missing", this));
                }
                newDispatch = NewCachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, head, boxedReceiverObject.getLookupNode(), method, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute());
                return head.replace(newDispatch);
            }

            default:
                throw new UnsupportedOperationException(missingBehavior.toString());
        }
    }

    private Object createAndExecuteGeneric(VirtualFrame frame, Object methodReceiverObject, Object boxedCallingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        final DispatchHeadNode dispatchHead = (DispatchHeadNode) NodeUtil.getNthParent(this, getDepth());
        return dispatchHead.getNewDispatch().replace(NewGenericDispatchNodeFactory.create(getContext(), ignoreVisibility, getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute(), getNeverExecute())).executeDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }
}
