/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.util.cli.Options;

public final class UnresolvedDispatchNode extends DispatchNode {

    private final boolean ignoreVisibility;
    private final Dispatch.MissingBehavior missingBehavior;

    public UnresolvedDispatchNode(RubyContext context, boolean ignoreVisibility,
                                  Dispatch.MissingBehavior missingBehavior) {
        super(context);
        this.ignoreVisibility = ignoreVisibility;
        this.missingBehavior = missingBehavior;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            Object callingSelf,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        if (getDepth() == Options.TRUFFLE_DISPATCH_POLYMORPHIC_MAX.load()) {
            return getHeadNode().getFirstDispatchNode()
                    .replace(GenericDispatchNodeFactory.create(getContext(), ignoreVisibility,
                            null, null, null, null, null, null, null))
                    .executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject,
                            methodName, blockObject, argumentsObjects, dispatchAction);
        }

        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        final RubyBasicObject boxedCallingSelf = getContext().getCoreLibrary().box(callingSelf);
        final RubyBasicObject boxedReceiverObject = getContext().getCoreLibrary().box(receiverObject);

        if (callingSelf instanceof RubyBasicObject && receiverObject instanceof RubyBasicObject) {
            final RubyMethod method;

            try {
                method = lookup(boxedCallingSelf, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                        dispatchAction);
            } catch (UseMethodMissingException e) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedCallingSelf,
                        boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            final DispatchNode newDispatch;

            if (receiverObject instanceof RubySymbol && RubySymbol.globalSymbolLookupNodeAssumption.isValid()) {
                newDispatch = CachedBoxedSymbolDispatchNodeFactory.create(getContext(), methodName, first, method, null,
                        null, null, null, null, null, null);
            } else {
                newDispatch = CachedBoxedDispatchNodeFactory.create(getContext(), methodName, first,
                        boxedReceiverObject.getLookupNode(), method, null, null, null, null, null, null, null);
            }

            first.replace(newDispatch);
            return newDispatch.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject,
                    methodName, blockObject, argumentsObjects, dispatchAction);
        } else {
            final RubyMethod method;

            try {
                method = lookup(boxedCallingSelf, boxedReceiverObject, methodName.toString(), ignoreVisibility,
                        dispatchAction);
            } catch (UseMethodMissingException e) {
                final DispatchNode newDispatch = createMethodMissingNode(methodName, boxedCallingSelf,
                        boxedReceiverObject, dispatchAction);
                return newDispatch.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, boxedReceiverObject,
                        methodName, blockObject, argumentsObjects, dispatchAction);
            }

            if (receiverObject instanceof  Boolean) {
                try {
                    final Assumption falseUnmodifiedAssumption =
                            getContext().getCoreLibrary().getFalseClass().getUnmodifiedAssumption();

                    final RubyMethod falseMethod =
                            lookup(boxedCallingSelf, getContext().getCoreLibrary().box(false), methodName.toString(),
                                    ignoreVisibility, dispatchAction);

                    final Assumption trueUnmodifiedAssumption =
                            getContext().getCoreLibrary().getTrueClass().getUnmodifiedAssumption();

                    final RubyMethod trueMethod =
                            lookup(boxedCallingSelf, getContext().getCoreLibrary().box(true), methodName.toString(),
                                    ignoreVisibility, dispatchAction);

                    final CachedBooleanDispatchNode newDispatch = CachedBooleanDispatchNodeFactory.create(getContext(),
                            methodName, first, falseUnmodifiedAssumption, falseMethod, trueUnmodifiedAssumption,
                            trueMethod, null, null, null, null, null, null, null);

                    first.replace(newDispatch);

                    return newDispatch.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject,
                            methodName, blockObject, argumentsObjects, dispatchAction);
                } catch (UseMethodMissingException e) {
                    throw new UnsupportedOperationException();
                }
            } else {
                final CachedUnboxedDispatchNode newDispatch = CachedUnboxedDispatchNodeFactory.create(getContext(),
                        methodName, first, receiverObject.getClass(),
                        boxedReceiverObject.getRubyClass().getUnmodifiedAssumption(), method, null, null, null, null,
                        null, null, null);

                first.replace(newDispatch);

                return newDispatch.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName,
                        blockObject, argumentsObjects, dispatchAction);
            }
        }
    }

    private DispatchNode createMethodMissingNode(
            Object methodName,
            RubyBasicObject callingSelf,
            RubyBasicObject receiverObject,
            Dispatch.DispatchAction dispatchAction) {
        final DispatchNode first = getHeadNode().getFirstDispatchNode();

        switch (missingBehavior) {
            case RETURN_MISSING: {
                return first.replace(CachedBoxedReturnMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getLookupNode(), null, null, null, null, null, null, null));
            }

            case CALL_METHOD_MISSING: {
                final RubyMethod method;

                try {
                    method = lookup(callingSelf, receiverObject, "method_missing", ignoreVisibility, dispatchAction);
                } catch (UseMethodMissingException e2) {
                    throw new RaiseException(getContext().getCoreLibrary().runtimeError(
                            receiverObject.toString() + " didn't have a #method_missing", this));
                }

                return first.replace(CachedBoxedMethodMissingDispatchNodeFactory.create(getContext(), methodName, first,
                        receiverObject.getLookupNode(), method, null, null, null, null, null, null, null));
            }

            default: {
                throw new UnsupportedOperationException(missingBehavior.toString());
            }
        }
    }

    private int getDepth() {
        final DispatchHeadNode head = getHeadNode();
        Node parent = getParent();

        int depth = 1;

        while (parent != head) {
            depth++;
            parent = parent.getParent();
        }

        return depth;
    }

}
