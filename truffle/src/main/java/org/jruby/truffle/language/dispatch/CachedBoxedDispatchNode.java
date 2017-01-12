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
import com.oracle.truffle.api.object.Shape;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.string.StringUtils;
import org.jruby.truffle.language.methods.InternalMethod;

public class CachedBoxedDispatchNode extends CachedDispatchNode {

    private final Shape expectedShape;
    private final Assumption validShape;
    private final Assumption unmodifiedAssumption;

    private final InternalMethod method;
    @Child private DirectCallNode callNode;

    public CachedBoxedDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            Shape expectedShape,
            DynamicObject expectedClass,
            InternalMethod method,
            DispatchAction dispatchAction) {
        super(cachedName, next, dispatchAction);

        this.expectedShape = expectedShape;
        this.validShape = expectedShape.getValidAssumption();
        this.unmodifiedAssumption = Layouts.MODULE.getFields(expectedClass).getUnmodifiedAssumption();
        this.next = next;
        this.method = method;
        this.callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
        applySplittingInliningStrategy(callNode, method);
    }

    @Override
    public boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) &&
                (receiver instanceof DynamicObject) &&
                ((DynamicObject) receiver).getShape() == expectedShape;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        try {
            validShape.check();
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
                return call(callNode, frame, method, receiverObject, blockObject, argumentsObjects);

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
                expectedShape, expectedShape.hashCode(),
                method == null ? "null" : method.toString());
    }

    public InternalMethod getMethod() {
        return method;
    }

    public Assumption getUnmodifiedAssumption() {
        return unmodifiedAssumption;
    }
}
