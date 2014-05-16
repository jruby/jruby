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
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.lookup.LookupNode;
import org.jruby.truffle.runtime.methods.RubyMethod;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * A node that calls {@code #method_missing} because at the point of lookup no method was found. We have a full dispatch
 * node for this because some frameworks might use {@code #method_missing} as dynamic programming on the fast path.
 */
@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class CachedBoxedMethodMissingDispatchNode extends BoxedDispatchNode {

    private final LookupNode expectedLookupNode;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;
    private final RubySymbol symbol;

    @Child protected BoxedDispatchNode next;
    @Child protected DirectCallNode callNode;

    public CachedBoxedMethodMissingDispatchNode(RubyContext context, LookupNode expectedLookupNode, RubyMethod method, String name, BoxedDispatchNode next) {
        super(context);

        assert expectedLookupNode != null;
        assert method != null;

        this.expectedLookupNode = expectedLookupNode;
        unmodifiedAssumption = expectedLookupNode.getUnmodifiedAssumption();
        this.method = method;
        symbol = context.newSymbol(name);
        this.next = next;

        callNode = Truffle.getRuntime().createDirectCallNode(method.getCallTarget());
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        RubyNode.notDesignedForCompilation();

        // Check the lookup node is what we expect

        if (receiverObject.getLookupNode() != expectedLookupNode) {
            return next.dispatch(frame, receiverObject, blockObject, argumentsObjects);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
        }

        // When calling #method_missing we need to prepend the symbol

        final Object[] modifiedArgumentsObjects = new Object[1 + argumentsObjects.length];
        modifiedArgumentsObjects[0] = symbol;
        System.arraycopy(argumentsObjects, 0, modifiedArgumentsObjects, 1, argumentsObjects.length);

        // Call the method

        return callNode.call(frame, RubyArguments.pack(method.getDeclarationFrame(), receiverObject, blockObject, modifiedArgumentsObjects));
    }

}
