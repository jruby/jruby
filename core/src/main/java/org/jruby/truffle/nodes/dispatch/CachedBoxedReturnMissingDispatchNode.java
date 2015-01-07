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
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyProc;

public abstract class CachedBoxedReturnMissingDispatchNode extends CachedDispatchNode {

    private final RubyClass expectedClass;
    private final Assumption unmodifiedAssumption;

    public CachedBoxedReturnMissingDispatchNode(RubyContext context, Object cachedName, DispatchNode next,
                                                RubyClass expectedClass, boolean indirect, DispatchAction dispatchAction) {
        super(context, cachedName, next, indirect, dispatchAction);
        assert expectedClass != null;
        this.expectedClass = expectedClass;
        unmodifiedAssumption = expectedClass.getUnmodifiedAssumption();
        this.next = next;
    }

    public CachedBoxedReturnMissingDispatchNode(CachedBoxedReturnMissingDispatchNode prev) {
        super(prev);
        expectedClass = prev.expectedClass;
        unmodifiedAssumption = prev.unmodifiedAssumption;
    }

    @Specialization(guards = "guardName")
    public Object dispatch(
            VirtualFrame frame,
            RubyBasicObject receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        // Check the lookup node is what we expect

        if (receiverObject.getMetaClass() != expectedClass) {
            return next.executeDispatch(
                    frame,
                    receiverObject,
                    methodName,
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
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
                    CompilerDirectives.unsafeCast(blockObject, RubyProc.class, true, false),
                    argumentsObjects,
                    "class modified");
        }

        final DispatchAction dispatchAction = getDispatchAction();

        if (dispatchAction == DispatchAction.CALL_METHOD) {
            return MISSING;
        } else if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
            return false;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Fallback
    public Object dispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        return next.executeDispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

}