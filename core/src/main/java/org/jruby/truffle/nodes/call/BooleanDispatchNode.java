/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.*;

/**
 * An unboxed node in the dispatch chain that dispatches if the node is a boolean. In normal unboxed
 * dispatch we look at the Java class of the receiver. However, in Ruby true and false are two
 * separate classes, so in this situation we have to dispatch on the value, as well as the Java
 * class when we are dealing with booleans.
 * <p>
 * TODO(CS): it would be nice if we could {@link RubyNode#executeBoolean} the receiver, but by the
 * time we get to this dispatch node the receiver is already executed.
 */
@NodeInfo(cost = NodeCost.POLYMORPHIC)
public class BooleanDispatchNode extends UnboxedDispatchNode {

    private final Assumption falseUnmodifiedAssumption;
    private final RubyMethod falseMethod;
    private final BranchProfile falseProfile = new BranchProfile();
    @Child protected DirectCallNode falseCall;

    private final Assumption trueUnmodifiedAssumption;
    private final RubyMethod trueMethod;
    private final BranchProfile trueProfile = new BranchProfile();
    @Child protected DirectCallNode trueCall;

    @Child protected UnboxedDispatchNode next;

    public BooleanDispatchNode(RubyContext context, Assumption falseUnmodifiedAssumption, RubyMethod falseMethod, Assumption trueUnmodifiedAssumption,
                               RubyMethod trueMethod, UnboxedDispatchNode next) {
        super(context);

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

        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        RubyNode.notDesignedForCompilation();

        // Check it's a boolean

        if (!(receiverObject instanceof Boolean)) {
            return next.dispatch(frame, receiverObject, blockObject, argumentsObjects);
        }

        if ((boolean) receiverObject) {
            trueProfile.enter();

            try {
                trueUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
            }

            return trueCall.call(frame, RubyArguments.pack(trueMethod.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
        } else {
            falseProfile.enter();

            try {
                falseUnmodifiedAssumption.check();
            } catch (InvalidAssumptionException e) {
                return respecialize("class modified", frame, receiverObject, blockObject, argumentsObjects);
            }

            return falseCall.call(frame, RubyArguments.pack(falseMethod.getDeclarationFrame(), receiverObject, blockObject, argumentsObjects));
        }
    }

    @Override
    public void setNext(UnboxedDispatchNode next) {
        this.next = next;
    }

}
