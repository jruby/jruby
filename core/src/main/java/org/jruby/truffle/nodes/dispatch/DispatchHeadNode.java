/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyProc;

public class DispatchHeadNode extends Node {

    private final RubyContext context;
    private final boolean ignoreVisibility;
    private final boolean indirect;
    private final boolean rubiniusPrimitive;
    private final Dispatch.MissingBehavior missingBehavior;

    @Child protected DispatchNode first;

    public static DispatchHeadNode onSelf(RubyContext context) {
        return new DispatchHeadNode(context, true, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
    }

    public DispatchHeadNode(RubyContext context) {
        this(context, false, false, false, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility) {
        this(context, ignoreVisibility, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
    }

    public DispatchHeadNode(RubyContext context, Dispatch.MissingBehavior missingBehavior) {
        this(context, false, false, false, missingBehavior);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, Dispatch.MissingBehavior missingBehavior) {
        this(context, ignoreVisibility, false, false, missingBehavior);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, boolean rubiniusPrimitive, Dispatch.MissingBehavior missingBehavior) {
        this.context = context;
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
        this.missingBehavior = missingBehavior;
        this.rubiniusPrimitive = rubiniusPrimitive;
        first = new UnresolvedDispatchNode(context, ignoreVisibility, indirect, missingBehavior);
    }

    public Object call(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) {
        return dispatch(
                frame,
                null, // TODO(eregon): was RubyArguments.getSelf(frame.getArguments()),
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects,
                Dispatch.DispatchAction.CALL_METHOD);
    }

    public double callFloat(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) throws UseMethodMissingException {
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (missingBehavior == Dispatch.MissingBehavior.RETURN_MISSING && value == Dispatch.MISSING) {
            throw new UseMethodMissingException();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreter();

        final String message = String.format("%s (%s#%s gives %s)",
                context.getCoreLibrary().getFloatClass().getName(),
                context.getCoreLibrary().getLogicalClass(receiverObject).getName(),
                methodName,
                context.getCoreLibrary().getLogicalClass(value).getName());

        throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                context.getCoreLibrary().getLogicalClass(receiverObject).getName(),
                message,
                this));
    }

    public long callLongFixnum(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) throws UseMethodMissingException {
        final Object value = call(frame, receiverObject, methodName, blockObject, argumentsObjects);

        if (missingBehavior == Dispatch.MissingBehavior.RETURN_MISSING && value == Dispatch.MISSING) {
            throw new UseMethodMissingException();
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (int) value;
        }

        CompilerDirectives.transferToInterpreter();

        final String message = String.format("%s (%s#%s gives %s)",
                context.getCoreLibrary().getFloatClass().getName(),
                context.getCoreLibrary().getLogicalClass(receiverObject).getName(),
                methodName,
                context.getCoreLibrary().getLogicalClass(value).getName());

        throw new RaiseException(context.getCoreLibrary().typeErrorCantConvertTo(
                context.getCoreLibrary().getLogicalClass(receiverObject).getName(),
                message,
                this));
    }

    /**
     * Check if a specific method is defined on the receiver object.
     * This check is "static" and should only be used in a few VM operations.
     * In many cases, a dynamic call to Ruby's respond_to? should be used instead.
     * Similar to MRI rb_check_funcall().
     */
    public boolean doesRespondTo(
            VirtualFrame frame,
            Object methodName,
            Object receiverObject) {
        return (boolean) dispatch(
                frame,
                null, // TODO(eregon): was RubyArguments.getSelf(frame.getArguments()),
                receiverObject,
                methodName,
                null,
                null,
                Dispatch.DispatchAction.RESPOND_TO_METHOD);
    }

    public Object dispatch(
            VirtualFrame frame,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects,
            Dispatch.DispatchAction dispatchAction) {
        if (rubiniusPrimitive) {
            return first.executeDispatch(
                    frame,
                    lexicalScope,
                    RubyArguments.getSelf(frame.getArguments()),
                    methodName,
                    blockObject,
                    RubyArguments.concatUserArguments(argumentsObjects, frame.getArguments()),
                    dispatchAction);
        } else {
            return first.executeDispatch(
                    frame,
                    lexicalScope,
                    receiverObject,
                    methodName,
                    blockObject,
                    argumentsObjects,
                    dispatchAction);
        }
    }

    public void reset(String reason) {
        first.replace(new UnresolvedDispatchNode(context, ignoreVisibility, indirect, missingBehavior), reason);
    }

    public DispatchNode getFirstDispatchNode() {
        return first;
    }

    public void forceUncached() {
        adoptChildren();
        first.replace(UncachedDispatchNodeFactory.create(context, ignoreVisibility, null, null, null, null, null, null));
    }

}
