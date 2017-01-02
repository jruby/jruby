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
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.language.methods.InternalMethod;

public class CachedBooleanDispatchNode extends CachedDispatchNode {

    private final Assumption falseUnmodifiedAssumption;
    private final InternalMethod falseMethod;
    private final BranchProfile falseProfile = BranchProfile.create();

    @Child private DirectCallNode falseCallDirect;

    private final Assumption trueUnmodifiedAssumption;
    private final InternalMethod trueMethod;
    private final BranchProfile trueProfile = BranchProfile.create();

    @Child private DirectCallNode trueCallDirect;

    public CachedBooleanDispatchNode(
            Object cachedName,
            DispatchNode next,
            Assumption falseUnmodifiedAssumption,
            InternalMethod falseMethod,
            Assumption trueUnmodifiedAssumption,
            InternalMethod trueMethod,
            DispatchAction dispatchAction) {
        super(cachedName, next, dispatchAction);

        this.falseUnmodifiedAssumption = falseUnmodifiedAssumption;
        this.falseMethod = falseMethod;

        if (falseMethod != null) {
            this.falseCallDirect = Truffle.getRuntime().createDirectCallNode(falseMethod.getCallTarget());
            applySplittingInliningStrategy(falseCallDirect, falseMethod);
        }

        this.trueUnmodifiedAssumption = trueUnmodifiedAssumption;
        this.trueMethod = trueMethod;

        if (trueMethod != null) {
            this.trueCallDirect = Truffle.getRuntime().createDirectCallNode(trueMethod.getCallTarget());
            applySplittingInliningStrategy(trueCallDirect, trueMethod);
        }
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) && (receiver instanceof Boolean);
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects) {
        try {
            trueUnmodifiedAssumption.check();
            falseUnmodifiedAssumption.check();
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

        if ((boolean) receiverObject) {
            trueProfile.enter();

            switch (getDispatchAction()) {
                case CALL_METHOD:
                    return call(trueCallDirect, frame, trueMethod, receiverObject, blockObject, argumentsObjects);
                case RESPOND_TO_METHOD:
                    return true;

                default:
                    throw new UnsupportedOperationException();
            }
        } else {
            falseProfile.enter();

            switch (getDispatchAction()) {
                case CALL_METHOD:
                    return call(falseCallDirect, frame, falseMethod, receiverObject, blockObject, argumentsObjects);

                case RESPOND_TO_METHOD:
                    return true;

                default:
                    throw new UnsupportedOperationException();

            }
        }
    }

}
