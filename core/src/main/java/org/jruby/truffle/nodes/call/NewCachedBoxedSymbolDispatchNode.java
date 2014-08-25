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
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class NewCachedBoxedSymbolDispatchNode extends NewCachedDispatchNode {

    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;


    public NewCachedBoxedSymbolDispatchNode(RubyContext context, Object cachedName, NewDispatchNode next, RubyMethod method) {
        super(context, cachedName, next);
        unmodifiedAssumption = context.getCoreLibrary().getSymbolClass().getUnmodifiedAssumption();
        this.method = method;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedBoxedSymbolDispatchNode(NewCachedBoxedSymbolDispatchNode prev) {
        super(prev);
        unmodifiedAssumption = prev.unmodifiedAssumption;
        method = prev.method;
        callNode = prev.callNode;
    }

    @Specialization(guards = {"isDispatch", "guardName"})
    public Object dispatch(VirtualFrame frame, NilPlaceholder methodReceiverObject, Object boxedCallingSelf, RubySymbol receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return doDispatch(frame, methodReceiverObject, boxedCallingSelf, receiverObject, methodName, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true), dispatchAction);
    }

    private Object doDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, RubySymbol receiverObject, Object methodName, RubyProc blockObject, Object[] argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        // Check no symbols have had their lookup modified

        try {
            RubySymbol.globalSymbolLookupNodeAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("symbol lookup modified", frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
        }

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method, method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
    }

    @Fallback
    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return next.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }

}