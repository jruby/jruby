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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeUtil;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.util.cli.Options;

public abstract class DispatchNode extends RubyNode {

    public static final int DISPATCH_POLYMORPHIC_MAX = Options.TRUFFLE_DISPATCH_POLYMORPHIC_MAX.load();
    public static final boolean DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED = Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_UNCACHED.load();
    public static final boolean DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT = Options.TRUFFLE_DISPATCH_METAPROGRAMMING_ALWAYS_INDIRECT.load();

    private final DispatchAction dispatchAction;

    private static final class Missing {
    }

    public static final Object MISSING = new Missing();

    public DispatchNode(RubyContext context, DispatchAction dispatchAction) {
        super(context, null);
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
            Object blockObject,
            Object argumentsObjects);

    @TruffleBoundary
    protected InternalMethod lookup(
            RubyClass callerClass,
            Object receiver,
            String name,
            boolean ignoreVisibility) {
        InternalMethod method = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), name);

        // If no method was found, use #method_missing

        if (method == null) {
            return null;
        }

        // Check for methods that are explicitly undefined

        if (method.isUndefined()) {
            return null;
        }

        // Check visibility

        if (!ignoreVisibility && !method.isVisibleTo(this, callerClass)) {
            final DispatchAction dispatchAction = getHeadNode().getDispatchAction();

            if (dispatchAction == DispatchAction.CALL_METHOD) {
                throw new RaiseException(getContext().getCoreLibrary().privateMethodError(name, getContext().getCoreLibrary().getLogicalClass(receiver), this));
            } else if (dispatchAction == DispatchAction.RESPOND_TO_METHOD) {
                return null;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return method;
    }

    protected Object resetAndDispatch(
            VirtualFrame frame,
            Object receiverObject,
            Object methodName,
            RubyBasicObject blockObject,
            Object argumentsObjects,
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

    public final Object execute(VirtualFrame frame) {
        throw new IllegalStateException("do not call execute on dispatch nodes");
    }

    public DispatchAction getDispatchAction() {
        return dispatchAction;
    }

    public boolean couldOptimizeKeywordArguments() {
        return false;
    }

}
