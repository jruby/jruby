/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.dispatch;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.control.RaiseException;

public class CallDispatchHeadNode extends DispatchHeadNode {

    @Child private BooleanCastNode booleanCastNode;

    private final BranchProfile errorProfile = BranchProfile.create();

    public static CallDispatchHeadNode createMethodCall() {
        return new CallDispatchHeadNode(
                null,
                false,
                MissingBehavior.CALL_METHOD_MISSING);
    }

    public static CallDispatchHeadNode createMethodCallIgnoreVisibility() {
        return new CallDispatchHeadNode(
                null,
                true,
                MissingBehavior.CALL_METHOD_MISSING);
    }

    public CallDispatchHeadNode(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        super(context, ignoreVisibility, missingBehavior, DispatchAction.CALL_METHOD);
    }

    public Object call(VirtualFrame frame, Object receiver, Object method, Object... arguments) {
        return dispatch(frame, receiver, method, null, arguments);
    }

    public Object callWithBlock(
            VirtualFrame frame,
            Object receiver,
            Object method,
            DynamicObject block,
            Object... arguments) {
        return dispatch(frame, receiver, method, block, arguments);
    }

    public boolean callBoolean(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        if (booleanCastNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            booleanCastNode = insert(BooleanCastNodeGen.create(null));
        }
        return booleanCastNode.executeBoolean(frame,
                dispatch(frame, receiverObject, methodName, blockObject, argumentsObjects));
    }

    public double callFloat(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        final Object value = dispatch(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (value instanceof Double) {
            return (double) value;
        }

        errorProfile.enter();
        if (value == DispatchNode.MISSING) {
            throw new RaiseException(context.getCoreExceptions().typeErrorCantConvertInto(receiverObject, "Float", this));
        } else {
            throw new RaiseException(context.getCoreExceptions().typeErrorCantConvertTo(receiverObject, "Float", (String) methodName, value, this));
        }
    }

    public long callLongFixnum(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        final Object value = dispatch(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        errorProfile.enter();
        if (value == DispatchNode.MISSING) {
            throw new RaiseException(context.getCoreExceptions().typeErrorCantConvertInto(receiverObject, "Fixnum", this));
        } else {
            throw new RaiseException(context.getCoreExceptions().typeErrorCantConvertTo(receiverObject, "Fixnum", (String) methodName, value, this));
        }
    }

}
