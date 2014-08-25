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

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

public class DispatchHeadNode extends Node {

    public static enum MissingBehavior {
        RETURN_MISSING,
        CALL_METHOD_MISSING
    }

    public static enum DispatchAction {
        CALL,
        RESPOND
    }

    public static final Object MISSING = new Object();

    private final RubyContext context;
    private final boolean ignoreVisibility;
    private final String cachedMethodName;
    private final MissingBehavior missingBehavior;

    @Child protected DispatchNode first;

    public DispatchHeadNode(RubyContext context, String name, MissingBehavior missingBehavior) {
        this(context, false, name, missingBehavior);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, String cachedMethodName, MissingBehavior missingBehavior) {
        this.context = context;
        this.ignoreVisibility = ignoreVisibility;
        this.cachedMethodName = cachedMethodName;
        this.missingBehavior = missingBehavior;
        first = new UnresolvedDispatchNode(context, ignoreVisibility, missingBehavior);
    }

    public DispatchHeadNode(RubyContext context, boolean ignoreVisibility, MissingBehavior missingBehavior) {
        this(context, ignoreVisibility, null, missingBehavior);
    }

    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, NilPlaceholder.INSTANCE, RubyArguments.getSelf(frame.getArguments()), receiverObject, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, NilPlaceholder.INSTANCE, callingSelf, receiverObject, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, methodReceiverObject, callingSelf, receiverObject, cachedMethodName, blockObject, argumentsObjects);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, String methodName, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, DispatchAction.CALL);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, RubySymbol methodName, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, DispatchAction.CALL);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, RubyString methodName, RubyProc blockObject, Object... argumentsObjects) {
        return dispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, DispatchAction.CALL);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject) {
        return (boolean) dispatch(frame, NilPlaceholder.INSTANCE, RubyArguments.getSelf(frame.getArguments()), receiverObject, cachedMethodName, null, null, DispatchAction.RESPOND);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object callingSelf, String methodName, Object receiverObject) {
        return (boolean) dispatch(frame, NilPlaceholder.INSTANCE, callingSelf, receiverObject, methodName, null, null, DispatchAction.RESPOND);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object callingSelf, RubySymbol methodName, Object receiverObject) {
        return (boolean) dispatch(frame, NilPlaceholder.INSTANCE, callingSelf, receiverObject, methodName, null, null, DispatchAction.RESPOND);
    }

    public boolean doesRespondTo(VirtualFrame frame, Object callingSelf, RubyString methodName, Object receiverObject) {
        return (boolean) dispatch(frame, NilPlaceholder.INSTANCE, callingSelf, receiverObject, methodName, null, null, DispatchAction.RESPOND);
    }

    public Object dispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return first.executeDispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }

    public void reset(String reason) {
        first.replace(new UnresolvedDispatchNode(context, ignoreVisibility, missingBehavior), reason);
    }

    public DispatchNode getFirstDispatchNode() {
        return first;
    }

}
