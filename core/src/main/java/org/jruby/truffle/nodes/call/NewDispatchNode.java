/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.RubyMethod;

@NodeChildren({@NodeChild(value="methodReceiverObject", type=NewDispatchNode.NeverExecuteRubyNode.class), @NodeChild(value="callingSelf", type=NewDispatchNode.NeverExecuteRubyNode.class), @NodeChild(value="receiver", type=NewDispatchNode.NeverExecuteRubyNode.class), @NodeChild(value="blockObject", type=NewDispatchNode.NeverExecuteRubyNode.class), @NodeChild(value="arguments", type=NewDispatchNode.NeverExecuteRubyNode.class), @NodeChild(value="action", type=NewDispatchNode.NeverExecuteRubyNode.class)})
public abstract class NewDispatchNode extends RubyNode {

    public NewDispatchNode(RubyContext context) {
        super(context, null);
    }

    public NewDispatchNode(NewDispatchNode prev) {
        this(prev.getContext());
    }

    public static class NeverExecuteRubyNode extends  RubyNode {
        public NeverExecuteRubyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public final Object execute(VirtualFrame frame) {
            throw new IllegalStateException("Do not execute this node!");
        }
    }

    public NeverExecuteRubyNode getNeverExecute() {
        return new NeverExecuteRubyNode(getContext(), getSourceSection());
    }

    public final Object execute(VirtualFrame frame) {
        throw new IllegalStateException("do not call execute on dispatch nodes");
    }

    public abstract Object executeDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction);

    protected RubyMethod lookup(RubyBasicObject boxedCallingSelf, RubyBasicObject receiverBasicObject, String name, boolean ignoreVisibility, DispatchHeadNode.DispatchAction dispatchAction) throws UseMethodMissingException {
        CompilerAsserts.neverPartOfCompilation();

        // TODO(CS): why are we using an exception to convey method missing here?

        RubyMethod method = receiverBasicObject.getLookupNode().lookupMethod(name);

        // If no method was found, use #method_missing

        if (method == null) {
            throw new UseMethodMissingException();
        }

        // Check for methods that are explicitly undefined

        if (method.isUndefined()) {
            throw new RaiseException(getContext().getCoreLibrary().noMethodError(name, receiverBasicObject.toString(), this));
        }

        // Check visibility

        if (boxedCallingSelf == receiverBasicObject.getRubyClass()){
            return method;
        }

        if (!ignoreVisibility && !method.isVisibleTo(this, boxedCallingSelf, receiverBasicObject)) {
            if (dispatchAction == DispatchHeadNode.DispatchAction.DISPATCH) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name, receiverBasicObject.toString(), this));
            } else if (dispatchAction == DispatchHeadNode.DispatchAction.RESPOND) {
                throw new UseMethodMissingException();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return method;
    }

    public int getDepth() {
        // TODO: can we use findParent instead?

        int depth = 1;
        Node parent = this.getParent();

        while (!(parent instanceof DispatchHeadNode)) {
            parent = parent.getParent();
            depth++;
        }

        return depth;
    }

    public Object respecialize(String reason, VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object... argumentsObjects) {
        CompilerAsserts.neverPartOfCompilation();

        final int depth = getDepth();
        final DispatchHeadNode head = (DispatchHeadNode) NodeUtil.getNthParent(this, depth);

        return head.respecialize(frame, reason, receiverObject, blockObject, argumentsObjects);
    }

    protected static boolean isDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return dispatchAction == DispatchHeadNode.DispatchAction.DISPATCH;
    }

    protected static boolean isRespond(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        return dispatchAction == DispatchHeadNode.DispatchAction.RESPOND;
    }

}
