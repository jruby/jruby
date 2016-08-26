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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.module.ModuleOperations;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.methods.InternalMethod;

public abstract class DispatchNode extends RubyNode {

    private final DispatchAction dispatchAction;

    private static final class Missing {
    }

    public static final Object MISSING = new Missing();

    public DispatchNode(RubyContext context, DispatchAction dispatchAction) {
        super(context);
        this.dispatchAction = dispatchAction;
        assert dispatchAction != null;
    }

    protected abstract boolean guard(Object methodName, Object receiver);

    protected DispatchNode getNext() {
        return null;
    }

    public abstract Object executeDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects);

    @TruffleBoundary
    protected InternalMethod lookup(
            DynamicObject callerClass,
            Object receiver,
            String name,
            boolean ignoreVisibility) {
        assert ignoreVisibility || RubyGuards.isRubyClass(callerClass);

        final InternalMethod method = ModuleOperations.lookupMethod(coreLibrary().getMetaClass(receiver), name);

        // If no method was found, use #method_missing
        if (method == null) {
            return null;
        }

        // Check for methods that are explicitly undefined
        if (method.isUndefined()) {
            return null;
        }

        // Check visibility
        if (!ignoreVisibility && !method.isVisibleTo(callerClass)) {
            return null;
        }

        return method;
    }

    protected Object resetAndDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            DynamicObject blockObject,
            Object[] argumentsObjects,
            String reason) {
        final DispatchHeadNode head = getHeadNode();
        head.reset(reason);
        return head.dispatch(
                frame,
                receiverObject,
                methodName,
                blockObject,
                argumentsObjects);
    }

    protected DispatchHeadNode getHeadNode() {
        return NodeUtil.findParent(this, DispatchHeadNode.class);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        throw new IllegalStateException("do not call execute on dispatch nodes");
    }

    public DispatchAction getDispatchAction() {
        return dispatchAction;
    }

}
