/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.methods.InternalMethod;

/**
 * Like {@link CachedBoxedDispatchNode}, but on singleton objects.
 * Checking identity of the DynamicObject is therefore faster than reading the Shape and checking the Shape identity.
 */
public class CachedSingletonDispatchNode extends CachedDispatchNode {

    private final DynamicObject expectedReceiver;
    private final Assumption unmodifiedAssumption;

    private final InternalMethod method;
    @Child private DirectCallNode callNode;

    public CachedSingletonDispatchNode(
            Object cachedName,
            DispatchNode next,
            DynamicObject expectedReceiver,
            DynamicObject expectedClass,
            InternalMethod method,
            DispatchAction dispatchAction) {
        super(cachedName, next, dispatchAction);

        this.expectedReceiver = expectedReceiver;
        this.unmodifiedAssumption = Layouts.MODULE.getFields(expectedClass).getUnmodifiedAssumption();
        this.next = next;
        this.method = method;
        this.callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
        applySplittingInliningStrategy(callNode, method);
    }

    @Override
    public boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) &&
                receiver == expectedReceiver;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return resetAndDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    "class modified");
        }

        if (!guard(methodName, receiverObject)) {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }

        switch (getDispatchAction()) {
            case CALL_METHOD:
                return call(callNode, frame, method, expectedReceiver, blockObject, argumentsObjects);

            case RESPOND_TO_METHOD:
                return true;

            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    public String toString() {
        return StringUtils.format("CachedBoxedDispatchNode(:%s, %s@%x, %s)",
                getCachedNameAsSymbol().toString(),
                expectedReceiver, expectedReceiver.hashCode(),
                method == null ? "null" : method.toString());
    }

}
