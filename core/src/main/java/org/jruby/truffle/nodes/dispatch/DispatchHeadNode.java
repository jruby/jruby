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
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyProc;

public class DispatchHeadNode extends Node {

    private final RubyContext context;
    private final boolean ignoreVisibility;
    private final boolean indirect;
    private final MissingBehavior missingBehavior;
    private final LexicalScope lexicalScope;
    private final DispatchAction dispatchAction;

    @Child private DispatchNode first;

    public static DispatchHeadNode onSelf(RubyContext context) {
        return new DispatchHeadNode(context, true, MissingBehavior.CALL_METHOD_MISSING);
    }

    public DispatchHeadNode(RubyContext context) {
        this(context, false, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility) {
        this(context, ignoreVisibility, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
    }

    public DispatchHeadNode(RubyContext context, MissingBehavior missingBehavior) {
        this(context, false, false, missingBehavior, null, DispatchAction.CALL_METHOD);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        this(context, ignoreVisibility, false, missingBehavior, null, DispatchAction.CALL_METHOD);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior) {
        this(context, ignoreVisibility, indirect, missingBehavior, null, DispatchAction.CALL_METHOD);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, boolean indirect, MissingBehavior missingBehavior, LexicalScope lexicalScope, DispatchAction dispatchAction) {
        this.context = context;
        this.ignoreVisibility = ignoreVisibility;
        this.indirect = indirect;
        this.missingBehavior = missingBehavior;
        this.lexicalScope = lexicalScope;
        this.dispatchAction = dispatchAction;
        first = new UnresolvedDispatchNode(context, ignoreVisibility, indirect, missingBehavior, dispatchAction);
    }

    public Object call(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyProc blockObject,
            Object... argumentsObjects) {
        assert dispatchAction == DispatchAction.CALL_METHOD;

        return dispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
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
        assert dispatchAction == DispatchAction.RESPOND_TO_METHOD;

        // It's ok to cast here as we control what RESPOND_TO_METHOD returns
        return (boolean) dispatch(
                frame,
                receiverObject,
                methodName,
                null,
                null);
    }

    public Object dispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        return first.executeDispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

    public void reset(String reason) {
        first.replace(new UnresolvedDispatchNode(context, ignoreVisibility, indirect, missingBehavior, dispatchAction), reason);
    }

    public DispatchNode getFirstDispatchNode() {
        return first;
    }

    public void forceUncached() {
        adoptChildren();
        first.replace(UncachedDispatchNodeFactory.create(context, ignoreVisibility, null, null, null, null, null));
    }

    public LexicalScope getLexicalScope() {
        return lexicalScope;
    }

    public DispatchAction getDispatchAction() {
        return dispatchAction;
    }
}
