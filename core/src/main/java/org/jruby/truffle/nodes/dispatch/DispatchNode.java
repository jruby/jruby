/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
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

@NodeChildren({
        @NodeChild(value="methodReceiverObject", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="callingSelf", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="receiver", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="methodName", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="blockObject", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="arguments", type=DispatchNode.NeverExecuteRubyNode.class),
        @NodeChild(value="action", type=DispatchNode.NeverExecuteRubyNode.class)})
public abstract class DispatchNode extends RubyNode {

    public DispatchNode(RubyContext context) {
        super(context, null);
    }

    public DispatchNode(DispatchNode prev) {
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

    public abstract Object executeDispatch(VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, Object blockObject, Object argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction);

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
            if (dispatchAction == DispatchHeadNode.DispatchAction.CALL) {
                throw new RaiseException(getContext().getCoreLibrary().noMethodError(name, receiverBasicObject.toString(), this));
            } else if (dispatchAction == DispatchHeadNode.DispatchAction.RESPOND) {
                throw new UseMethodMissingException();
            } else {
                throw new UnsupportedOperationException();
            }
        }

        return method;
    }

    protected Object resetAndDispatch(String reason, VirtualFrame frame, Object methodReceiverObject, Object callingSelf, Object receiverObject, Object methodName, RubyProc blockObject, Object[] argumentsObjects, DispatchHeadNode.DispatchAction dispatchAction) {
        final DispatchHeadNode head = getHeadNode();
        head.reset(reason);
        return head.dispatch(frame, methodReceiverObject, callingSelf, receiverObject, methodName, blockObject, argumentsObjects, dispatchAction);
    }

    protected DispatchHeadNode getHeadNode() {
        return NodeUtil.findParent(this, DispatchHeadNode.class);
    }

}
