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
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyProc;

public class CallDispatchHeadNode extends DispatchHeadNode {

    @Child private BooleanCastNode booleanCastNode;

    public CallDispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior, LexicalScope lexicalScope) {
        super(context, ignoreVisibility, indirect, missingBehavior, lexicalScope, DispatchAction.CALL_METHOD);
    }

    public Object call(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
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
            RubyProc blockObject,
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
            RubyProc blockObject,
            Object... argumentsObjects) throws UseMethodMissingException {
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (missingBehavior == MissingBehavior.RETURN_MISSING && value == DispatchNode.MISSING) {
            throw new UseMethodMissingException();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                receiverObject, context.getCoreLibrary().getFloatClass(), (String) methodName, value, this));
    }

    public long callLongFixnum(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) throws UseMethodMissingException {
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (missingBehavior == MissingBehavior.RETURN_MISSING && value == DispatchNode.MISSING) {
            throw new UseMethodMissingException();
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                receiverObject, context.getCoreLibrary().getFixnumClass(), (String) methodName, value, this));
    }

}
