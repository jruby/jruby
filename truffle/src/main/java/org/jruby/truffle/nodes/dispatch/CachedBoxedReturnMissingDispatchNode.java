/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.RubyContext;
import com.oracle.truffle.api.object.DynamicObject;

public class CachedBoxedReturnMissingDispatchNode extends CachedDispatchNode {

    private final DynamicObject expectedClass;
    private final Assumption unmodifiedAssumption;

    public CachedBoxedReturnMissingDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            DynamicObject expectedClass,
            boolean indirect,
            DispatchAction dispatchAction) {
        super(context, cachedName, next, indirect, dispatchAction);
        assert RubyGuards.isRubyClass(expectedClass);
        this.expectedClass = expectedClass;
        unmodifiedAssumption = ModuleNodes.getModel(expectedClass).getUnmodifiedAssumption();
        this.next = next;
    }

    @Override
    protected boolean guard(Object methodName, Object receiver) {
        return guardName(methodName) &&
                (receiver instanceof DynamicObject) &&
                BasicObjectNodes.getMetaClass(((DynamicObject) receiver)) == expectedClass;
    }

    @Override
    public Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (!guard(methodName, receiverObject)) {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects);
        }

        // Check the class has not been modified

        try {
            unmodifiedAssumption.check();
        } catch (InvalidAssumptionException e) {
            return resetAndDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    (DynamicObject) blockObject,
                    argumentsObjects,
                    "class modified");
        }

        switch (getDispatchAction()) {
            case CALL_METHOD:
                return MISSING;

            case RESPOND_TO_METHOD:
                return false;

            default:
                throw new UnsupportedOperationException();
        }
    }

}