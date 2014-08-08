/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;

/**
 * Symbols in Ruby are both key for performance and a pain to optimise. The root of the problem is that they're shared,
 * or global objects, with the same object instance for each location they appear in the source code. This is a good
 * interpreter optimization, but it is really hard to optimize as it means the object are always escaping! Because of
 * that, when we call methods on symbols we always have to check the lookup node. This dispatch node ellides that check
 * by keeping a global assumption that no symbol objects have had their lookup nodes modified (for example by using
 * the singleton class). It also checks instanceof RubySymbol to check the node is appropriate for this object in
 * the first place, but that we can do statically when we have a constant RubySymbol.
 */
@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class CachedBoxedSymbolDispatchNode extends BoxedDispatchNode {

    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;

    @Child protected DirectCallNode callNode;
    @Child protected BoxedDispatchNode next;

    public CachedBoxedSymbolDispatchNode(RubyContext context, RubyMethod method, BoxedDispatchNode next) {
        super(context);
        unmodifiedAssumption = context.getCoreLibrary().getSymbolClass().getUnmodifiedAssumption();
        this.method = method;
        this.next = next;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject boxedCallingSelf, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        // Check it is a symbol

        if (!(receiverObject instanceof RubySymbol)) {
            return next.dispatch(frame, boxedCallingSelf, receiverObject, blockObject, argumentsObjects);
        }

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

    @Override
    public boolean doesRespondTo(VirtualFrame frame, RubyBasicObject receiverObject) {
        // Check it is a symbol

        if (!(receiverObject instanceof RubySymbol)) {
            return next.doesRespondTo(frame, receiverObject);
        }

        // Check no symbols have had their lookup modified

        try {
            RubySymbol.globalSymbolLookupNodeAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecializeAndDoesRespondTo("symbol lookup modified", frame, receiverObject);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecializeAndDoesRespondTo("class modified", frame, receiverObject);
        }

        return true;
    }

}
