/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.call;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;

/**
 * A node in the dispatch chain that transfers to interpreter and then boxes the receiver.
 */
@NodeInfo(cost = NodeCost.UNINITIALIZED)
public class UninitializedBoxingDispatchNode extends UnboxedDispatchNode {

    @Child protected BoxedDispatchNode next;

    public UninitializedBoxingDispatchNode(RubyContext context, boolean ignoreVisibility, BoxedDispatchNode next) {
        super(context, ignoreVisibility);

        this.next = next;
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        return dispatch(frame, RubyArguments.getSelf(frame.getArguments()), receiverObject, blockObject, argumentsObjects);
    }

    @Override
    public Object dispatch(VirtualFrame frame, Object callingSelf, Object receiverObject, RubyProc blockObject, Object[] argumentsObjects) {
        CompilerDirectives.transferToInterpreter();

        /*
         * If the next dispatch node is something other than the uninitialized dispatch node then we
         * need to replace this node because it's now on the fast path. If the receiver was already
         * boxed.
         * 
         * Note that with this scheme it will take a couple of calls for the chain to become fully
         * specialized.
         */

        if (!(next instanceof UninitializedDispatchNode)) {
            this.replace(new BoxingDispatchNode(getContext(), getIgnoreVisibility(), next));
        }

        return next.dispatch(frame, getContext().getCoreLibrary().box(callingSelf), getContext().getCoreLibrary().box(receiverObject), blockObject, argumentsObjects);
    }

    @Override
    public boolean doesRespondTo(VirtualFrame frame, Object receiverObject) {
        CompilerDirectives.transferToInterpreter();

        /*
         * If the next dispatch node is something other than the uninitialized dispatch node then we
         * need to replace this node because it's now on the fast path. If the receiver was already
         * boxed.
         *
         * Note that with this scheme it will take a couple of calls for the chain to become fully
         * specialized.
         */

        if (!(next instanceof UninitializedDispatchNode)) {
            this.replace(new BoxingDispatchNode(getContext(), getIgnoreVisibility(), next));
        }

        return next.doesRespondTo(frame, getContext().getCoreLibrary().box(receiverObject));
    }

}
