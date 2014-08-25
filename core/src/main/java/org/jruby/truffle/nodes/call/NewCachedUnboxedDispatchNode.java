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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class NewCachedUnboxedDispatchNode extends NewCachedDispatchNode {

    private final Class expectedClass;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;

    public NewCachedUnboxedDispatchNode(RubyContext context, Object cachedName, NewDispatchNode next, Class expectedClass, Assumption unmodifiedAssumption, RubyMethod method) {
        super(context, cachedName, next);
        assert expectedClass != null;
        assert unmodifiedAssumption != null;
        assert method != null;

        this.expectedClass = expectedClass;
        this.unmodifiedAssumption = unmodifiedAssumption;
        this.method = method;

        this.callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedUnboxedDispatchNode(NewCachedUnboxedDispatchNode prev) {
        super(prev);
        expectedClass = prev.expectedClass;
        unmodifiedAssumption = prev.unmodifiedAssumption;
        method = prev.method;
        callNode = prev.callNode;
    }



    @Specialization(guards = {"isDispatch", "isPrimitive", "guardName"})
    public Object dispatch(VirtualFrame frame, NilPlaceholder methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        // Check the class is what we expect

        if (receiverObject.getClass() != expectedClass) {
            return next.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }
        return doDispatch(frame, methodReceiverObject, callingSelf, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }


    @Fallback
    public Object dispatchGeneric(VirtualFrame frame, Object methodReceiverObject, Object boxedCallingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return doNext(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }

    private Object doDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method, method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
    }

    private Object doNext(VirtualFrame frame, Object methodReceiverObject, Object boxedCallingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return next.executeDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }
}
