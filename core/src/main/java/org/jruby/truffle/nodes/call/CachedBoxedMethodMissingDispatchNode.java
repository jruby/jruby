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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
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
public class CachedBoxedMethodMissingDispatchNode extends BoxedDispatchNode {

    private final LookupNode expectedLookupNode;
    private final Assumption unmodifiedAssumption;
    private final RubyMethod method;
    private final RubySymbol symbol;

    @Child protected BoxedDispatchNode next;

    public CachedBoxedMethodMissingDispatchNode(RubyContext context, SourceSection sourceSection, LookupNode expectedLookupNode, RubyMethod method, String name, BoxedDispatchNode next) {
        super(context, sourceSection);

        assert expectedLookupNode != null;
        assert method != null;

        this.expectedLookupNode = expectedLookupNode;
        unmodifiedAssumption = expectedLookupNode.getUnmodifiedAssumption();
        this.method = method;
        this.next = adoptChild(next);
        symbol = context.newSymbol(name);
    }

    @Override
    public Object dispatch(VirtualFrame frame, RubyBasicObject receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
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

        return method.call(frame.pack(), receiverObject, blockObject, modifiedArgumentsObjects);
    }

}
