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
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class CachedBoxedMethodMissingDispatchNode extends CachedDispatchNode {

    private final LookupNode expectedLookupNode;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;

    public CachedBoxedMethodMissingDispatchNode(RubyContext context, Object cachedName, DispatchNode next,
                                                LookupNode expectedLookupNode, RubyMethod method) {
        super(context, cachedName, next);

        this.expectedLookupNode = expectedLookupNode;
        unmodifiedAssumption = expectedLookupNode.getUnmodifiedAssumption();
        this.method = method;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public CachedBoxedMethodMissingDispatchNode(CachedBoxedMethodMissingDispatchNode prev) {
        super(prev);
        expectedLookupNode = prev.expectedLookupNode;
        unmodifiedAssumption = prev.unmodifiedAssumption;
        method = prev.method;
        callNode = prev.callNode;
    }

    @Specialization(guards = "guardName")
    public Object dispatch(
            VirtualFrame frame,
            NilPlaceholder methodReceiverObject,
            Object callingSelf,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        // Check the lookup node is what we expect

        if (receiverObject.getLookupNode() != expectedLookupNode) {
            return next.executeDispatch(
                    frame,
                    methodReceiverObject,
                    callingSelf,
                    receiverObject,
                    methodName,
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                    argumentsObjects,
                    dispatchAction);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return resetAndDispatch(
                    frame,
                    methodReceiverObject,
                    callingSelf,
                    receiverObject,
                    methodName,
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                    argumentsObjects,
                    dispatchAction,
                    "class modified");
        }

        if (dispatchAction == Dispatch.DispatchAction.CALL) {
            // When calling #method_missing we need to prepend the symbol

            final Object[] argumentsObjectsArray = CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true);

            final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjectsArray.length];
            modifiedArgumentsObjects[0] = getContext().newSymbol(methodName.toString());
            System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjectsArray.length);
            return callNode.call(
                    frame,
                    RubyArguments.pack(
                            method,
                            method.getDeclarationFrame(),
                            receiverObject,
                            CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                            modifiedArgumentsObjects));
        } else if (dispatchAction == Dispatch.DispatchAction.RESPOND) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }


    @Fallback
    public Object dispatch(
            VirtualFrame frame,
            Object methodReceiverObject,
            Object callingSelf,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        return next.executeDispatch(
                frame,
                methodReceiverObject,
                callingSelf,
                receiverObject,
                methodName,
                CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                argumentsObjects, dispatchAction);
    }


}