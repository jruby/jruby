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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import com.oracle.truffle.api.object.DynamicObject;

public class CallDispatchHeadNode extends DispatchHeadNode {

    @Child private BooleanCastNode booleanCastNode;

    public CallDispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior) {
        super(context, ignoreVisibility, indirect, missingBehavior, DispatchAction.CALL_METHOD);
    }

    public Object call(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        return dispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

    public boolean callBoolean(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        if (booleanCastNode == null) {
            CompilerDirectives.transferToInterpreter();
            booleanCastNode = insert(BooleanCastNodeGen.create(context, getSourceSection(), null));
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
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreter();
        if (value == DispatchNode.MISSING) {
            throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertInto(
                    receiverObject, context.getCoreLibrary().getFloatClass(), this));
        } else {
            throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                    receiverObject, context.getCoreLibrary().getFloatClass(), (String) methodName, value, this));
        }
    }

    public long callLongFixnum(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object... argumentsObjects) {
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        CompilerDirectives.transferToInterpreter();
        if (value == DispatchNode.MISSING) {
            throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertInto(
                    receiverObject, context.getCoreLibrary().getFixnumClass(), this));
        } else {
            throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                    receiverObject, context.getCoreLibrary().getFixnumClass(), (String) methodName, value, this));
        }
    }

}
