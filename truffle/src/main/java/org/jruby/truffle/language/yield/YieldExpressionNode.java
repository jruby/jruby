/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.yield;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.core.array.ArrayToObjectArrayNode;
import org.jruby.truffle.core.array.ArrayToObjectArrayNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;

public class YieldExpressionNode extends RubyNode {

    private final boolean unsplat;

    @Children private final RubyNode[] arguments;
    @Child private YieldNode yieldNode;
    @Child private ArrayToObjectArrayNode unsplatNode;

    private final BranchProfile useCapturedBlock = BranchProfile.create();
    private final BranchProfile noCapturedBlock = BranchProfile.create();

    public YieldExpressionNode(boolean unsplat, RubyNode[] arguments) {
        this.unsplat = unsplat;
        this.arguments = arguments;
    }

    @ExplodeLoop
    @Override
    public final Object execute(VirtualFrame frame) {
        Object[] argumentsObjects = new Object[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            argumentsObjects[i] = arguments[i].execute(frame);
        }

        DynamicObject block = RubyArguments.getBlock(frame);

        if (block == null) {
            useCapturedBlock.enter();

            block = RubyArguments.getMethod(frame).getCapturedBlock();

            if (block == null) {
                noCapturedBlock.enter();
                throw new RaiseException(coreExceptions().noBlockToYieldTo(this));
            }
        }

        if (unsplat) {
            argumentsObjects = unsplat(argumentsObjects);
        }

        return getYieldNode().dispatch(frame, block, argumentsObjects);
    }

    private Object[] unsplat(Object[] argumentsObjects) {
        if (unsplatNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            unsplatNode = insert(ArrayToObjectArrayNodeGen.create(null));
        }
        return unsplatNode.unsplat(argumentsObjects);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        if (RubyArguments.getBlock(frame) == null) {
            return nil();
        } else {
            return coreStrings().YIELD.createInstance();
        }
    }

    private YieldNode getYieldNode() {
        if (yieldNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            yieldNode = insert(new YieldNode());
        }

        return yieldNode;
    }

}
