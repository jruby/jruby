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
import com.oracle.truffle.api.dsl.Generic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.RubyMethod;

public abstract class NewCachedBoxedSymbolDispatchNode extends NewCachedDispatchNode {

    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;


    public NewCachedBoxedSymbolDispatchNode(RubyContext context, NewDispatchNode next, RubyMethod method) {
        super(context, next);
        unmodifiedAssumption = context.getCoreLibrary().getSymbolClass().getUnmodifiedAssumption();
        this.method = method;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    public NewCachedBoxedSymbolDispatchNode(NewCachedBoxedSymbolDispatchNode prev) {
        this(prev.getContext(), prev.next, prev.method);
    }

    @Specialization
    public Object dispatch(VirtualFrame frame, Object boxedCallingSelf, RubySymbol receiverObject, Object blockObject, Object argumentsObjects) {
        return doDispatch(frame, receiverObject, CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false), CompilerDirectives.unsafeCast(argumentsObjects, Object[].class, true, true));
    }

    private Object doDispatch(VirtualFrame frame, RubySymbol receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // Check no symbols have had their lookup modified

        try {
            RubySymbol.globalSymbolLookupNodeAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("symbol lookup modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
    }

    @Generic
    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects) {
        return next.executeDispatch(frame, callingSelf, receiverObject, blockObject, argumentsObjects);
    }

}