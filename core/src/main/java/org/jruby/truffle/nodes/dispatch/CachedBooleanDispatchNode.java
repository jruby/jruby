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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class CachedBooleanDispatchNode extends CachedDispatchNode {

    private final Assumption falseUnmodifiedAssumption;
    private final RubyMethod falseMethod;
    private final BranchProfile falseProfile = new BranchProfile();
    @Child protected DirectCallNode falseCall;

    private final Assumption trueUnmodifiedAssumption;
    private final RubyMethod trueMethod;
    private final BranchProfile trueProfile = new BranchProfile();
    @Child protected DirectCallNode trueCall;



    public CachedBooleanDispatchNode(RubyContext context, Object cachedName, DispatchNode next, Assumption falseUnmodifiedAssumption, RubyMethod falseMethod, Assumption trueUnmodifiedAssumption,
                                     RubyMethod trueMethod) {
        super(context, cachedName, next);
        assert falseUnmodifiedAssumption != null;
        assert falseMethod != null;
        assert trueUnmodifiedAssumption != null;
        assert trueMethod != null;

        this.falseUnmodifiedAssumption = falseUnmodifiedAssumption;
        this.falseMethod = falseMethod;
        falseCall = Truffle.getRuntime().createDirectCallNode(falseMethod.getCallTarget());

        this.trueUnmodifiedAssumption = trueUnmodifiedAssumption;
        this.trueMethod = trueMethod;
        trueCall = Truffle.getRuntime().createDirectCallNode(trueMethod.getCallTarget());
    }

    public CachedBooleanDispatchNode(CachedBooleanDispatchNode prev) {
        super(prev);
        falseUnmodifiedAssumption = prev.falseUnmodifiedAssumption;
        falseMethod = prev.falseMethod;
        falseCall = prev.falseCall;
        trueUnmodifiedAssumption = prev.trueUnmodifiedAssumption;
        trueMethod = prev.trueMethod;
        trueCall = prev.trueCall;
    }


    @Specialization(guards = {"guardName"})
    public Object dispatch(VirtualFrame frame, NilPlaceholder methodReceiverObject, Object boxedCallingSelf, boolean receiverObject, Object methodName, Object blockObject, Object argumentsObjects, Dispatch.DispatchAction dispatchAction) {
        return doDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true), dispatchAction);
    }

    private Object doDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, boolean receiverObject, Object methodName, RubyProc blockObject, Object[] argumentsObjects, Dispatch.DispatchAction dispatchAction) {
        if ((boolean) receiverObject) {
            trueProfile.enter();

            try {
                trueUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return resetAndDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction, "class modified");
            }

            if (dispatchAction == Dispatch.DispatchAction.CALL) {
                return trueCall.call(frame, RubyArguments.pack(trueMethod, trueMethod.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND) {
                return true;
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            falseProfile.enter();

            try {
                falseUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return resetAndDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction, "class modified");
            }

            if (dispatchAction == Dispatch.DispatchAction.CALL) {
                return falseCall.call(frame, RubyArguments.pack(falseMethod, falseMethod.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
            } else if (dispatchAction == Dispatch.DispatchAction.RESPOND) {
                return true;
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Fallback
    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, Dispatch.DispatchAction dispatchAction) {
        return next.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }


}